/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.location;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Geofence;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;

import com.android.server.FgThread;
import com.android.server.PendingIntentUtils;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class GeofenceManager implements LocationListener, PendingIntent.OnFinished {
    private static final String TAG = "GeofenceManager";
    private static final boolean D = Log.isLoggable(TAG, Log.DEBUG);

    private static final int MSG_UPDATE_FENCES = 1;

    /**
     * Assume a maximum land speed, as a heuristic to throttle location updates.
     * (Air travel should result in an airplane mode toggle which will
     * force a new location update anyway).
     */
    private static final int MAX_SPEED_M_S = 100;  // 360 km/hr (high speed train)

    /**
     * Maximum age after which a location is no longer considered fresh enough to use.
     */
    private static final long MAX_AGE_NANOS = 5 * 60 * 1000000000L; // five minutes


    /**
     * Least frequent update interval allowed.
     */
    private static final long MAX_INTERVAL_MS = 2 * 60 * 60 * 1000; // two hours

    private final Context mContext;
    private final GeofenceHandler mHandler;

    private final LocationManager mLocationManager;
    private final AppOpsManager mAppOps;
    private final PowerManager.WakeLock mWakeLock;

    private final SettingsHelper mSettingsStore;

    private final Object mLock = new Object();

    // access to members below is synchronized on mLock
    /**
     * A list containing all registered geofences.
     */
    private List<GeofenceState> mFences = new LinkedList<>();

    /**
     * This is set true when we have an active request for {@link Location} updates via
     * {@link LocationManager#requestLocationUpdates(LocationRequest, LocationListener,
     * android.os.Looper).
     */
    private boolean mReceivingLocationUpdates;

    /**
     * The update interval component of the current active {@link Location} update request.
     */
    private long mLocationUpdateInterval;

    /**
     * The {@link Location} most recently received via {@link #onLocationChanged(Location)}.
     */
    private Location mLastLocationUpdate;

    /**
     * This is set true when a {@link Location} is received via
     * {@link #onLocationChanged(Location)} or {@link #scheduleUpdateFencesLocked()}, and cleared
     * when that Location has been processed via {@link #updateFences()}
     */
    private boolean mPendingUpdate;

    public GeofenceManager(Context context, SettingsHelper settingsStore) {
        mContext = context;
        mHandler = new GeofenceHandler(FgThread.getHandler().getLooper());

        mLocationManager = mContext.getSystemService(LocationManager.class);
        mAppOps = mContext.getSystemService(AppOpsManager.class);

        mWakeLock = mContext.getSystemService(PowerManager.class).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mSettingsStore = settingsStore;
    }

    public void addFence(LocationRequest request, Geofence geofence, PendingIntent intent,
            CallerIdentity identity) {
        GeofenceState state = new GeofenceState(geofence,
                request.getExpirationRealtimeMs(SystemClock.elapsedRealtime()),
                identity, intent);
        synchronized (mLock) {
            // first make sure it doesn't already exist
            for (int i = mFences.size() - 1; i >= 0; i--) {
                GeofenceState w = mFences.get(i);
                if (geofence.equals(w.mFence) && intent.equals(w.mIntent)) {
                    // already exists, remove the old one
                    mFences.remove(i);
                    break;
                }
            }
            mFences.add(state);
            scheduleUpdateFencesLocked();
        }
    }

    public void removeFence(Geofence fence, PendingIntent intent) {
        if (D) {
            Slog.d(TAG, "removeFence: fence=" + fence + ", intent=" + intent);
        }

        synchronized (mLock) {
            Iterator<GeofenceState> iter = mFences.iterator();
            while (iter.hasNext()) {
                GeofenceState state = iter.next();
                if (state.mIntent.equals(intent)) {

                    if (fence == null) {
                        // always remove
                        iter.remove();
                    } else {
                        // just remove matching fences
                        if (fence.equals(state.mFence)) {
                            iter.remove();
                        }
                    }
                }
            }
            scheduleUpdateFencesLocked();
        }
    }

    public void removeFence(String packageName) {
        if (D) {
            Slog.d(TAG, "removeFence: packageName=" + packageName);
        }

        synchronized (mLock) {
            mFences.removeIf(state -> state.mIdentity.packageName.equals(packageName));
            scheduleUpdateFencesLocked();
        }
    }

    private void removeExpiredFencesLocked() {
        long time = SystemClock.elapsedRealtime();
        mFences.removeIf(state -> state.mExpireAt < time);
    }

    private void scheduleUpdateFencesLocked() {
        if (!mPendingUpdate) {
            mPendingUpdate = true;
            mHandler.sendEmptyMessage(MSG_UPDATE_FENCES);
        }
    }

    /**
     * Returns the location received most recently from {@link #onLocationChanged(Location)},
     * or consult {@link LocationManager#getLastLocation()} if none has arrived. Does not return
     * either if the location would be too stale to be useful.
     *
     * @return a fresh, valid Location, or null if none is available
     */
    private Location getFreshLocationLocked() {
        // Prefer mLastLocationUpdate to LocationManager.getLastLocation().
        Location location = mReceivingLocationUpdates ? mLastLocationUpdate : null;
        if (location == null && !mFences.isEmpty()) {
            location = mLocationManager.getLastLocation();
        }

        // Early out for null location.
        if (location == null) {
            return null;
        }

        // Early out for stale location.
        long now = SystemClock.elapsedRealtimeNanos();
        if (now - location.getElapsedRealtimeNanos() > MAX_AGE_NANOS) {
            return null;
        }

        // Made it this far? Return our fresh, valid location.
        return location;
    }

    /**
     * The geofence update loop. This function removes expired fences, then tests the most
     * recently-received {@link Location} against each registered {@link GeofenceState}, sending
     * {@link Intent}s for geofences that have been tripped. It also adjusts the active location
     * update request with {@link LocationManager} as appropriate for any active geofences.
     */
    // Runs on the handler.
    private void updateFences() {
        List<PendingIntent> enterIntents = new LinkedList<>();
        List<PendingIntent> exitIntents = new LinkedList<>();

        synchronized (mLock) {
            mPendingUpdate = false;

            // Remove expired fences.
            removeExpiredFencesLocked();

            // Get a location to work with, either received via onLocationChanged() or
            // via LocationManager.getLastLocation().
            Location location = getFreshLocationLocked();

            // Update all fences.
            // Keep track of the distance to the nearest fence.
            double minFenceDistance = Double.MAX_VALUE;
            boolean needUpdates = false;
            for (GeofenceState state : mFences) {
                CallerIdentity identity = state.mIdentity;
                if (mSettingsStore.isLocationPackageBlacklisted(identity.userId,
                        identity.packageName)) {
                    continue;
                }

                int op = CallerIdentity.asAppOp(identity.permissionLevel);
                if (op >= 0) {
                    if (mAppOps.noteOpNoThrow(AppOpsManager.OP_FINE_LOCATION, identity.uid,
                            identity.packageName, identity.featureId, null)
                            != AppOpsManager.MODE_ALLOWED) {
                        continue;
                    }
                }

                needUpdates = true;
                if (location != null) {
                    int event = state.processLocation(location);
                    if ((event & GeofenceState.FLAG_ENTER) != 0) {
                        enterIntents.add(state.mIntent);
                    }
                    if ((event & GeofenceState.FLAG_EXIT) != 0) {
                        exitIntents.add(state.mIntent);
                    }

                    // FIXME: Ideally this code should take into account the accuracy of the
                    // location fix that was used to calculate the distance in the first place.
                    double fenceDistance = state.getDistanceToBoundary(); // MAX_VALUE if unknown
                    if (fenceDistance < minFenceDistance) {
                        minFenceDistance = fenceDistance;
                    }
                }
            }

            // Request or cancel location updates if needed.
            if (needUpdates) {
                // Request location updates.
                // Compute a location update interval based on the distance to the nearest fence.
                long intervalMs;
                if (location != null && Double.compare(minFenceDistance, Double.MAX_VALUE) != 0) {
                    intervalMs = (long) Math.min(MAX_INTERVAL_MS, Math.max(
                            mSettingsStore.getBackgroundThrottleProximityAlertIntervalMs(),
                            minFenceDistance * 1000 / MAX_SPEED_M_S));
                } else {
                    intervalMs = mSettingsStore.getBackgroundThrottleProximityAlertIntervalMs();
                }
                if (!mReceivingLocationUpdates || mLocationUpdateInterval != intervalMs) {
                    mReceivingLocationUpdates = true;
                    mLocationUpdateInterval = intervalMs;
                    mLastLocationUpdate = location;

                    LocationRequest request = new LocationRequest();
                    request.setInterval(intervalMs).setFastestInterval(0);
                    mLocationManager.requestLocationUpdates(request, this, mHandler.getLooper());
                }
            } else {
                // Cancel location updates.
                if (mReceivingLocationUpdates) {
                    mReceivingLocationUpdates = false;
                    mLocationUpdateInterval = 0;
                    mLastLocationUpdate = null;

                    mLocationManager.removeUpdates(this);
                }
            }

            if (D) {
                Slog.d(TAG, "updateFences: location=" + location
                        + ", mFences.size()=" + mFences.size()
                        + ", mReceivingLocationUpdates=" + mReceivingLocationUpdates
                        + ", mLocationUpdateInterval=" + mLocationUpdateInterval
                        + ", mLastLocationUpdate=" + mLastLocationUpdate);
            }
        }

        // release lock before sending intents
        for (PendingIntent intent : exitIntents) {
            sendIntentExit(intent);
        }
        for (PendingIntent intent : enterIntents) {
            sendIntentEnter(intent);
        }
    }

    private void sendIntentEnter(PendingIntent pendingIntent) {
        if (D) {
            Slog.d(TAG, "sendIntentEnter: pendingIntent=" + pendingIntent);
        }

        Intent intent = new Intent();
        intent.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
        sendIntent(pendingIntent, intent);
    }

    private void sendIntentExit(PendingIntent pendingIntent) {
        if (D) {
            Slog.d(TAG, "sendIntentExit: pendingIntent=" + pendingIntent);
        }

        Intent intent = new Intent();
        intent.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
        sendIntent(pendingIntent, intent);
    }

    private void sendIntent(PendingIntent pendingIntent, Intent intent) {
        mWakeLock.acquire();
        try {
            pendingIntent.send(mContext, 0, intent, this, null,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    PendingIntentUtils.createDontSendToRestrictedAppsBundle(null));
        } catch (PendingIntent.CanceledException e) {
            removeFence(null, pendingIntent);
            mWakeLock.release();
        }
        // ...otherwise, mWakeLock.release() gets called by onSendFinished()
    }

    // Runs on the handler (which was passed into LocationManager.requestLocationUpdates())
    @Override
    public void onLocationChanged(Location location) {
        synchronized (mLock) {
            if (mReceivingLocationUpdates) {
                mLastLocationUpdate = location;
            }

            // Update the fences immediately before returning in
            // case the caller is holding a wakelock.
            if (mPendingUpdate) {
                mHandler.removeMessages(MSG_UPDATE_FENCES);
            } else {
                mPendingUpdate = true;
            }
        }
        updateFences();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode,
            String resultData, Bundle resultExtras) {
        mWakeLock.release();
    }

    public void dump(PrintWriter pw) {
        for (GeofenceState state : mFences) {
            pw.println(state.mIdentity + " " + state.mFence);
        }
    }

    private final class GeofenceHandler extends Handler {
        private GeofenceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_FENCES) {
                updateFences();
            }
        }
    }
}
