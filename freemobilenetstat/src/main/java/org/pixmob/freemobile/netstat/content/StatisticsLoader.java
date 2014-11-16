package org.pixmob.freemobile.netstat.content;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.SystemClock;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;

import org.pixmob.freemobile.netstat.Event;
import org.pixmob.freemobile.netstat.MobileOperator;
import org.pixmob.freemobile.netstat.NetworkClass;
import org.pixmob.freemobile.netstat.content.NetstatContract.Events;

import java.util.Calendar;
import java.util.Date;

import static org.pixmob.freemobile.netstat.BuildConfig.DEBUG;
import static org.pixmob.freemobile.netstat.Constants.INTERVAL_ONE_MONTH;
import static org.pixmob.freemobile.netstat.Constants.INTERVAL_ONE_WEEK;
import static org.pixmob.freemobile.netstat.Constants.INTERVAL_TODAY;
import static org.pixmob.freemobile.netstat.Constants.SP_KEY_TIME_INTERVAL;
import static org.pixmob.freemobile.netstat.Constants.SP_NAME;
import static org.pixmob.freemobile.netstat.Constants.TAG;

/**
 * {@link Loader} implementation for loading events from the database, and
 * computing statistics.
 * @author Pixmob
 */
public class StatisticsLoader extends AsyncTaskLoader<Statistics> {
    public StatisticsLoader(final Context context) {
        super(context);

        if (DEBUG) {
            Log.d(TAG, "New StatisticsLoader");
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();

        if (DEBUG) {
            Log.d(TAG, "StatisticsLoader.onStartLoading()");
        }
    }

    @Override
    public Statistics loadInBackground() {
        if (DEBUG) {
            Log.d(TAG, "StatisticsLoader.loadInBackground()");
        }

        final long now = System.currentTimeMillis();

        final SharedPreferences prefs = getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        final int interval = prefs.getInt(SP_KEY_TIME_INTERVAL, 0);
        final long fromTimestamp;
        if (interval == INTERVAL_ONE_MONTH) {
            final Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
            cal.add(Calendar.MONTH, -1);
            fromTimestamp = cal.getTimeInMillis();
        } else if (interval == INTERVAL_ONE_WEEK) {
            final Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
            cal.add(Calendar.DATE, -7);
            fromTimestamp = cal.getTimeInMillis();
        } else if (interval == INTERVAL_TODAY) {
            // Get the date at midnight today.
            final Time t = new Time();
            t.set(now);
            t.hour = 0;
            t.minute = 0;
            t.second = 0;
            fromTimestamp = t.toMillis(false);
        } else {
            fromTimestamp = now - SystemClock.elapsedRealtime();
        }

        Log.i(TAG, "Loading statistics from " + new Date(fromTimestamp) + " to now");

        final Statistics s = new Statistics();

        final TelephonyManager tm = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        s.mobileOperatorCode = tm.getNetworkOperator();
        s.mobileOperator = MobileOperator.fromString(s.mobileOperatorCode);
        if (s.mobileOperator == null) {
            s.mobileOperatorCode = null;
        }

        long connectionTimestamp = 0;

        Cursor c = null;
        try {
            c = getContext().getContentResolver().query(
                    Events.CONTENT_URI,
                    new String[] { Events.TIMESTAMP, Events.SCREEN_ON, Events.WIFI_CONNECTED,
                            Events.MOBILE_CONNECTED, Events.MOBILE_NETWORK_TYPE, Events.MOBILE_OPERATOR,
                            Events.BATTERY_LEVEL, Events.POWER_ON, Events.FEMTOCELL }, Events.TIMESTAMP + ">?",
                    new String[] { String.valueOf(fromTimestamp) }, Events.TIMESTAMP + " ASC");
            final int rowCount = c.getCount();
            s.events = new Event[rowCount];
            for (int i = 0; c.moveToNext(); ++i) {
                final Event e = new Event();
                e.read(c);
                s.events[i] = e;

                if (i > 0) {
                    final Event e0 = s.events[i - 1];
                    if (e.powerOn && !e0.powerOn) {
                        continue;
                    }
                    final long dt = e.timestamp - e0.timestamp;

                    final MobileOperator op = MobileOperator.fromString(e.mobileOperator);
                    final MobileOperator op0 = MobileOperator.fromString(e0.mobileOperator);
                	final NetworkClass nc = NetworkClass.getNetworkClass(e.mobileNetworkType);
                    if (op != null && op.equals(op0)) {
                        if (MobileOperator.ORANGE.equals(op)) {
                            s.orangeTime += dt;
                            if (NetworkClass.NC_2G.equals(nc)) {
                            	s.orange2GTime += dt;
                            } else if (NetworkClass.NC_3G.equals(nc)) {
                            	s.orange3GTime += dt;
                            }
                        } else if (MobileOperator.FREE_MOBILE.equals(op)) {
                            s.freeMobileTime += dt;
                            if (NetworkClass.NC_3G.equals(nc)) {
                            	s.freeMobile3GTime += dt;
                            } else if (NetworkClass.NC_4G.equals(nc)) {
                            	s.freeMobile4GTime += dt;
                            }
                        }
                    }
                    if (e.mobileConnected && !e0.mobileConnected) {
                        connectionTimestamp = e.timestamp;
                    }
                    if (!e.mobileConnected) {
                        connectionTimestamp = 0;
                    }
                    if (e.wifiConnected && e0.wifiConnected) {
                        s.wifiOnTime += dt;
                    }
                    if (e.screenOn && e0.screenOn) {
                        s.screenOnTime += dt;
                    }
                    if (e.femtocell && e0.femtocell) {
                        s.femtocellTime += dt;
                    }
                }
            }

            if (s.events.length > 0) {
                s.battery = s.events[s.events.length - 1].batteryLevel;
            }

            final double sTime = s.orangeTime + s.freeMobileTime;                
            s.freeMobileUsePercent = (int) Math.round(s.freeMobileTime / sTime * 100d);
            s.orangeUsePercent = (int) Math.round(s.orangeTime / sTime * 100d);
            s.freeMobile4GUsePercent =
            		s.freeMobileUsePercent == 0 || s.freeMobileTime == 0 ?
            				0 : (int) Math.round((double)s.freeMobile4GTime / s.freeMobileTime * 100);
            s.freeMobile3GUsePercent = 100 - s.freeMobile4GUsePercent;
            s.orange3GUsePercent =
            		s.orangeUsePercent == 0 || s.orangeTime == 0 ?
            				0 : (int) Math.round((double)s.orange3GTime / s.orangeTime * 100);
            s.orange2GUsePercent = 100 - s.orange3GUsePercent;
            s.connectionTime = now - connectionTimestamp;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to load statistics", e);
            s.events = new Event[0];
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception ignore) {
            }
        }

        if (DEBUG) {
            final long end = System.currentTimeMillis();
            Log.d(TAG, "Statistics loaded in " + (end - now) + " ms");
        }

        return s;
    }
}