package android.telephony;

import static android.telephony.ServiceState.DUPLEX_MODE_FDD;
import static android.telephony.ServiceState.DUPLEX_MODE_TDD;
import static android.telephony.ServiceState.DUPLEX_MODE_UNKNOWN;

import android.telephony.AccessNetworkConstants.EutranBand;
import android.telephony.AccessNetworkConstants.GeranBand;
import android.telephony.AccessNetworkConstants.UtranBand;
import android.telephony.ServiceState.DuplexMode;

import java.util.Arrays;

/**
 * Utilities to map between radio constants.
 *
 * @hide
 */
public class AccessNetworkUtils {

    // do not instantiate
    private AccessNetworkUtils() {}

    public static final int INVALID_BAND = -1;

    /** ISO country code of Japan. */
    private static final String JAPAN_ISO_COUNTRY_CODE = "jp";

    /**
     * Gets the duplex mode for the given EUTRAN operating band.
     *
     * <p>See 3GPP 36.101 sec 5.5-1 for calculation
     *
     * @param band The EUTRAN band number
     * @return The duplex mode of the given EUTRAN band
     */
    @DuplexMode
    public static int getDuplexModeForEutranBand(int band) {
        if (band == INVALID_BAND) {
            return DUPLEX_MODE_UNKNOWN;
        }

        if (band > EutranBand.BAND_88) {
            return DUPLEX_MODE_UNKNOWN;
        } else if (band >= EutranBand.BAND_65) {
            return DUPLEX_MODE_FDD;
        } else if (band >= EutranBand.BAND_33) {
            return DUPLEX_MODE_TDD;
        } else if (band >= EutranBand.BAND_1) {
            return DUPLEX_MODE_FDD;
        }

        return DUPLEX_MODE_UNKNOWN;
    }

    /**
     * Gets the EUTRAN Operating band for a given downlink EARFCN.
     *
     * <p>See 3GPP TS 36.101 clause 5.7.3-1 for calculation.
     *
     * @param earfcn The downlink EARFCN
     * @return Operating band number, or {@link #INVALID_BAND} if no corresponding band exists
     */
    public static int getOperatingBandForEarfcn(int earfcn) {
        if (earfcn > 70645) {
            return INVALID_BAND;
        } else if (earfcn >= 70596) {
            return EutranBand.BAND_88;
        } else if (earfcn >= 70546) {
            return EutranBand.BAND_87;
        } else if (earfcn >= 70366) {
            return EutranBand.BAND_85;
        } else if (earfcn > 69465) {
            return INVALID_BAND;
        } else if (earfcn >= 69036) {
            return EutranBand.BAND_74;
        } else if (earfcn >= 68986) {
            return EutranBand.BAND_73;
        } else if (earfcn >= 68936) {
            return EutranBand.BAND_72;
        } else if (earfcn >= 68586) {
            return EutranBand.BAND_71;
        } else if (earfcn >= 68336) {
            return EutranBand.BAND_70;
        } else if (earfcn > 67835) {
            return INVALID_BAND;
        } else if (earfcn >= 67536) {
            return EutranBand.BAND_68;
        } else if (earfcn >= 67366) {
            return INVALID_BAND; // band 67 only for CarrierAgg
        } else if (earfcn >= 66436) {
            return EutranBand.BAND_66;
        } else if (earfcn >= 65536) {
            return EutranBand.BAND_65;
        } else if (earfcn > 60254) {
            return INVALID_BAND;
        } else if (earfcn >= 60140) {
            return EutranBand.BAND_53;
        } else if (earfcn >= 59140) {
            return EutranBand.BAND_52;
        } else if (earfcn >= 59090) {
            return EutranBand.BAND_51;
        } else if (earfcn >= 58240) {
            return EutranBand.BAND_50;
        } else if (earfcn >= 56740) {
            return EutranBand.BAND_49;
        } else if (earfcn >= 55240) {
            return EutranBand.BAND_48;
        } else if (earfcn >= 54540) {
            return EutranBand.BAND_47;
        } else if (earfcn >= 46790) {
            return EutranBand.BAND_46;
        } else if (earfcn >= 46590) {
            return EutranBand.BAND_45;
        } else if (earfcn >= 45590) {
            return EutranBand.BAND_44;
        } else if (earfcn >= 43590) {
            return EutranBand.BAND_43;
        } else if (earfcn >= 41590) {
            return EutranBand.BAND_42;
        } else if (earfcn >= 39650) {
            return EutranBand.BAND_41;
        } else if (earfcn >= 38650) {
            return EutranBand.BAND_40;
        } else if (earfcn >= 38250) {
            return EutranBand.BAND_39;
        } else if (earfcn >= 37750) {
            return EutranBand.BAND_38;
        } else if (earfcn >= 37550) {
            return EutranBand.BAND_37;
        } else if (earfcn >= 36950) {
            return EutranBand.BAND_36;
        } else if (earfcn >= 36350) {
            return EutranBand.BAND_35;
        } else if (earfcn >= 36200) {
            return EutranBand.BAND_34;
        } else if (earfcn >= 36000) {
            return EutranBand.BAND_33;
        } else if (earfcn > 10359) {
            return INVALID_BAND;
        } else if (earfcn >= 9920) {
            return INVALID_BAND; // band 32 only for CarrierAgg
        } else if (earfcn >= 9870) {
            return EutranBand.BAND_31;
        } else if (earfcn >= 9770) {
            return EutranBand.BAND_30;
        } else if (earfcn >= 9660) {
            return INVALID_BAND; // band 29 only for CarrierAgg
        } else if (earfcn >= 9210) {
            return EutranBand.BAND_28;
        } else if (earfcn >= 9040) {
            return EutranBand.BAND_27;
        } else if (earfcn >= 8690) {
            return EutranBand.BAND_26;
        } else if (earfcn >= 8040) {
            return EutranBand.BAND_25;
        } else if (earfcn >= 7700) {
            return EutranBand.BAND_24;
        } else if (earfcn >= 7500) {
            return EutranBand.BAND_23;
        } else if (earfcn >= 6600) {
            return EutranBand.BAND_22;
        } else if (earfcn >= 6450) {
            return EutranBand.BAND_21;
        } else if (earfcn >= 6150) {
            return EutranBand.BAND_20;
        } else if (earfcn >= 6000) {
            return EutranBand.BAND_19;
        } else if (earfcn >= 5850) {
            return EutranBand.BAND_18;
        } else if (earfcn >= 5730) {
            return EutranBand.BAND_17;
        } else if (earfcn > 5379) {
            return INVALID_BAND;
        } else if (earfcn >= 5280) {
            return EutranBand.BAND_14;
        } else if (earfcn >= 5180) {
            return EutranBand.BAND_13;
        } else if (earfcn >= 5010) {
            return EutranBand.BAND_12;
        } else if (earfcn >= 4750) {
            return EutranBand.BAND_11;
        } else if (earfcn >= 4150) {
            return EutranBand.BAND_10;
        } else if (earfcn >= 3800) {
            return EutranBand.BAND_9;
        } else if (earfcn >= 3450) {
            return EutranBand.BAND_8;
        } else if (earfcn >= 2750) {
            return EutranBand.BAND_7;
        } else if (earfcn >= 2650) {
            return EutranBand.BAND_6;
        } else if (earfcn >= 2400) {
            return EutranBand.BAND_5;
        } else if (earfcn >= 1950) {
            return EutranBand.BAND_4;
        } else if (earfcn >= 1200) {
            return EutranBand.BAND_3;
        } else if (earfcn >= 600) {
            return EutranBand.BAND_2;
        } else if (earfcn >= 0) {
            return EutranBand.BAND_1;
        }

        return INVALID_BAND;
    }

    /**
     * Gets the GERAN Operating band for a given ARFCN.
     *
     * <p>See 3GPP TS 45.005 clause 2 for calculation.
     *
     * @param arfcn The ARFCN
     * @return Operating band number, or {@link #INVALID_BAND} if no corresponding band exists
     */
    public static int getOperatingBandForArfcn(int arfcn) {
        if (arfcn >= 0 && arfcn <= 124) {
            return GeranBand.BAND_E900;
        } else if (arfcn >= 128 && arfcn <= 251) {
            return GeranBand.BAND_850;
        } else if (arfcn >= 259 && arfcn <= 293) {
            return GeranBand.BAND_450;
        } else if (arfcn >= 306 && arfcn <= 340) {
            return GeranBand.BAND_480;
        } else if (arfcn >= 438 && arfcn <= 511) {
            return GeranBand.BAND_750;
        } else if (arfcn >= 512 && arfcn <= 885) {
            // ARFCN between 512 and 810 are also part of BAND_PCS1900.
            // Returning BAND_DCS1800 in both cases.
            return GeranBand.BAND_DCS1800;
        } else if (arfcn >= 940 && arfcn <= 974) {
            return GeranBand.BAND_ER900;
        } else if (arfcn >= 975 && arfcn <= 1023) {
            return GeranBand.BAND_E900;
        }
        return INVALID_BAND;
    }

    /**
     * Gets the UTRAN Operating band for a given downlink UARFCN.
     *
     * <p>See 3GPP TS 25.101 clause 5.4.4 for calculation.
     *
     * @param uarfcn The downlink UARFCN
     * @return Operating band number, or {@link #INVALID_BAND} if no corresponding band exists
     */
    public static int getOperatingBandForUarfcn(int uarfcn) {
        // List of additional bands defined in TS 25.101.
        int[] addlBand2 = {412, 437, 462, 487, 512, 537, 562, 587, 612, 637, 662, 687};
        int[] addlBand4 = {1887, 1912, 1937, 1962, 1987, 2012, 2037, 2062, 2087};
        int[] addlBand5 = {1007, 1012, 1032, 1037, 1062, 1087};
        int[] addlBand6 = {1037, 1062};
        int[] addlBand7 =
                {2587, 2612, 2637, 2662, 2687, 2712, 2737, 2762, 2787, 2812, 2837, 2862,
                2887, 2912};
        int[] addlBand10 =
                {3412, 3437, 3462, 3487, 3512, 3537, 3562, 3587, 3612, 3637, 3662, 3687};
        int[] addlBand12 = {3932, 3957, 3962, 3987, 3992};
        int[] addlBand13 = {4067, 4092};
        int[] addlBand14 = {4167, 4192};
        int[] addlBand19 = {787, 812, 837};
        int[] addlBand25 =
                {6292, 6317, 6342, 6367, 6392, 6417, 6442, 6467, 6492, 6517, 6542, 6567, 6592};
        int[] addlBand26 = {5937, 5962, 5987, 5992, 6012, 6017, 6037, 6042, 6062, 6067, 6087};

        if (uarfcn >= 10562 && uarfcn <= 10838) {
            return UtranBand.BAND_1;
        } else if ((uarfcn >= 9662 && uarfcn <= 9938)
                || Arrays.binarySearch(addlBand2, uarfcn) >= 0) {
            return UtranBand.BAND_2;
        } else if (uarfcn >= 1162 && uarfcn <= 1513) {
            return UtranBand.BAND_3;
        } else if ((uarfcn >= 1537 && uarfcn <= 1738)
                || Arrays.binarySearch(addlBand4, uarfcn) >= 0) {
            return UtranBand.BAND_4;
        } else if (uarfcn >= 4387 && uarfcn <= 4413) {
            // Band 6 is a subset of band 5. Only Japan uses band 6 and Japan does not have band 5.
            String country = TelephonyManager.getDefault().getNetworkCountryIso();
            if (JAPAN_ISO_COUNTRY_CODE.compareToIgnoreCase(country) == 0) {
                return UtranBand.BAND_6;
            } else {
                return UtranBand.BAND_5;
            }
        } else if ((uarfcn >= 4357 && uarfcn <= 4458)
                || Arrays.binarySearch(addlBand5, uarfcn) >= 0) {
            return UtranBand.BAND_5;
        } else if (Arrays.binarySearch(addlBand6, uarfcn) >= 0) {
            return UtranBand.BAND_6;
        } else if ((uarfcn >= 2237 && uarfcn <= 2563)
                || Arrays.binarySearch(addlBand7, uarfcn) >= 0) {
            return UtranBand.BAND_7;
        } else if (uarfcn >= 2937 && uarfcn <= 3088) {
            return UtranBand.BAND_8;
        } else if (uarfcn >= 9237 && uarfcn <= 9387) {
            return UtranBand.BAND_9;
        } else if ((uarfcn >= 3112 && uarfcn <= 3388)
                || Arrays.binarySearch(addlBand10, uarfcn) >= 0) {
            return UtranBand.BAND_10;
        } else if (uarfcn >= 3712 && uarfcn <= 3787) {
            return UtranBand.BAND_11;
        } else if ((uarfcn >= 3842 && uarfcn <= 3903)
                || Arrays.binarySearch(addlBand12, uarfcn) >= 0) {
            return UtranBand.BAND_12;
        } else if ((uarfcn >= 4017 && uarfcn <= 4043)
                || Arrays.binarySearch(addlBand13, uarfcn) >= 0) {
            return UtranBand.BAND_13;
        } else if ((uarfcn >= 4117 && uarfcn <= 4143)
                || Arrays.binarySearch(addlBand14, uarfcn) >= 0) {
            return UtranBand.BAND_14;
        } else if ((uarfcn >= 712 && uarfcn <= 763)
                || Arrays.binarySearch(addlBand19, uarfcn) >= 0) {
            return UtranBand.BAND_19;
        } else if (uarfcn >= 4512 && uarfcn <= 4638) {
            return UtranBand.BAND_20;
        } else if (uarfcn >= 862 && uarfcn <= 912) {
            return UtranBand.BAND_21;
        } else if (uarfcn >= 4662 && uarfcn <= 5038) {
            return UtranBand.BAND_22;
        } else if ((uarfcn >= 5112 && uarfcn <= 5413)
                || Arrays.binarySearch(addlBand25, uarfcn) >= 0) {
            return UtranBand.BAND_25;
        } else if ((uarfcn >= 5762 && uarfcn <= 5913)
                || Arrays.binarySearch(addlBand26, uarfcn) >= 0) {
            return UtranBand.BAND_26;
        }
        return INVALID_BAND;
    }
}
