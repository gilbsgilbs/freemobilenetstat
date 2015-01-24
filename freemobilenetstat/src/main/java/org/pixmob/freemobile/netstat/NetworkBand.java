package org.pixmob.freemobile.netstat;

import org.pixmob.freemobile.netstat.util.SearchUtils;

/**
 * Network Band (3G@900mhz, 3G@2100mhz, …).
 */
public enum NetworkBand {
	NB_UMTS_900, NB_UMTS_2100, NB_LTE_1800, NB_LTE_2600;

    /**
     * Converts a Free Mobile CID to a Network Band
     */
    public static NetworkBand fromLCID(int cid, final NetworkClass nc) {

        if (NetworkClass.NC_3G.equals(nc)) {
            cid %= 65536; // UMTS cid

            final int[][] FM_NB3G900_intervals =
                    {
                            {91, 93}, {4187, 4189}, {8283, 8285},
                            {12379, 12381}, {16475, 16477}, {20571, 20573},
                            {24667, 24669}, {28763, 28765}, {24667, 24669}
                    };
            if (SearchUtils.binarySearchInIntervals(cid, FM_NB3G900_intervals) != -1)
                return NB_UMTS_900;

            final int[][] FM_NB3G2100_intervals =
                    {
                            {21, 23}, {32789, 32791}, {36885, 36887},
                            {40981, 40983}, {45077, 45079}, {49173, 49175},
                            {53269, 53271}, {57365, 57367}, {61461, 61463}
                    };
            if (SearchUtils.binarySearchInIntervals(cid, FM_NB3G2100_intervals) != -1)
                return NB_UMTS_2100;

            if (cid < 32768)
                return NB_UMTS_2100;

            return NB_UMTS_900;
        }

        return null;
    }
}
