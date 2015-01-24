package org.pixmob.freemobile.netstat;

import android.annotation.TargetApi;
import android.os.Build;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import java.util.List;

import static org.pixmob.freemobile.netstat.Constants.TAG;

/**
 * Describe a cell
 * @author gilbsgilbs
 */
public class CellInfoWrapper {
    public enum CellType { CT_GSM, CT_WCDMA, CT_LTE }
    public final CellType type;
    public final Integer cid;
    public final Integer lac;
    public final Integer mcc;
    public final Integer mnc;
    public final Integer psc;
    public final Integer pci;
    public final Integer tac;


    /**
     * Constructs a CellInfo based on the given TelephonyManager. Tries both GSM, WCDMA and LTE methods with old and new APIs.
     * @param tm the TelephonyManager
     */
    public CellInfoWrapper(TelephonyManager tm) {
        CellType tmp_type = null;
        Integer tmp_cid = null, tmp_lac = null, tmp_mcc = null, tmp_mnc = null, tmp_psc = null, tmp_pci = null, tmp_tac = null;

        if (tm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                //get the cell list
                List<android.telephony.CellInfo> cellInfos = tm.getAllCellInfo();
                if (cellInfos != null) {
                    for (android.telephony.CellInfo cellInfo : cellInfos) {
                        if (cellInfo.isRegistered()) { //we use only registered cells
                            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                                    && (cellInfo instanceof CellInfoWcdma)) { //manage the wcdma cell case
                                Log.d(TAG, "We got a WCDMA cell");
                                CellIdentityWcdma ciw = ((CellInfoWcdma) cellInfo).getCellIdentity();
                                if (ciw != null) {
                                    tmp_type = CellType.CT_WCDMA;
                                    tmp_cid = ciw.getCid() == Integer.MAX_VALUE ? null : ciw.getCid();
                                    tmp_lac = ciw.getLac() == Integer.MAX_VALUE ? null : ciw.getLac();
                                    tmp_mcc = ciw.getMcc() == Integer.MAX_VALUE ? null : ciw.getMcc();
                                    tmp_mnc = ciw.getMnc() == Integer.MAX_VALUE ? null : ciw.getMnc();
                                    tmp_psc = ciw.getPsc() == Integer.MAX_VALUE ? null : ciw.getPsc();
                                    Log.d(TAG, "We got the cell information - exit loop");
                                    break;
                                }
                            } else if (cellInfo instanceof CellInfoGsm) { //test the gsm case
                                CellIdentityGsm ci = ((CellInfoGsm) cellInfo).getCellIdentity();
                                Log.d(TAG, "We got a GSM cell");
                                if (ci != null) {
                                    tmp_type = CellType.CT_GSM;
                                    tmp_cid = ci.getCid() == Integer.MAX_VALUE ? null : ci.getCid();
                                    tmp_lac = ci.getLac() == Integer.MAX_VALUE ? null : ci.getLac();
                                    tmp_mcc = ci.getMcc() == Integer.MAX_VALUE ? null : ci.getMcc();
                                    tmp_mnc = ci.getMnc() == Integer.MAX_VALUE ? null : ci.getMnc();
                                    Log.d(TAG, "We got the cell information - exit loop");
                                    break;
                                }
                            } else if (cellInfo instanceof CellInfoLte) { // test the lte case
                                CellIdentityLte ci = ((CellInfoLte) cellInfo).getCellIdentity();
                                Log.d(TAG, "We got a GSM cell");
                                if (ci != null) {
                                    tmp_type = CellType.CT_LTE;
                                    tmp_cid = ci.getCi() == Integer.MAX_VALUE ? null : ci.getCi();
                                    tmp_mcc = ci.getMcc() == Integer.MAX_VALUE ? null : ci.getMcc();
                                    tmp_mnc = ci.getMnc() == Integer.MAX_VALUE ? null : ci.getMnc();
                                    tmp_pci = ci.getPci() == Integer.MAX_VALUE ? null : ci.getPci();
                                    tmp_tac = ci.getTac() == Integer.MAX_VALUE ? null : ci.getTac();
                                    Log.d(TAG, "We got the cell information - exit loop");
                                    break;
                                }
                            }

                        } else
                            Log.d(TAG, "Unregistered cell - skipping");
                    }
                } else
                    Log.d(TAG, "No cell info were available with getAllCellInfo(). Trying with old API.");

            }
            if (tmp_type == null) { // use old API if LAC was not found with the new method (useful for buggy devices such as Samsung Galaxy S5) or if SDK is too old
                CellLocation cellLocation = tm.getCellLocation(); //cell location might be null... handle with care
                if ((cellLocation != null) && (cellLocation instanceof GsmCellLocation)) {
                    GsmCellLocation ci = (GsmCellLocation) cellLocation;
                    Log.d(TAG, "We got a old GSM cell with information");
                    tmp_type = CellType.CT_GSM;
                    tmp_cid = ci.getCid() == -1 ? null : ci.getCid();
                    tmp_lac = ci.getLac() == -1 ? null : ci.getLac();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
                        tmp_psc = ci.getPsc() == -1 ? null : ci.getPsc();
                }
            }
        }

        this.type = tmp_type;
        this.cid = tmp_cid;
        this.lac = tmp_lac;
        this.mcc = tmp_mcc;
        this.mnc = tmp_mnc;
        this.psc = tmp_psc;
        this.pci = tmp_pci;
        this.tac = tmp_tac;
    }
}
