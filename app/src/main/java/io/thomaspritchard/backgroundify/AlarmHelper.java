package io.thomaspritchard.backgroundify;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by thomas on 4/7/17.
 */

public class AlarmHelper {

    static final String URL = "io.thomaspritchard.backgroundify.URL";
    public static boolean pageFinished = false;
    public static Context context = null;
    public static boolean firstFire = false;
    //Updates alarm according to context's preference settings.
    public static void updateAlarm(Context newContext) {
        context = newContext;
        boolean delayed = false;
        setAlarm(delayed);
    }

    public static void updateAlarm(boolean delayed) {
        setAlarm(delayed);
    }

    @TargetApi(19)
    private static void setAlarm(boolean delayed) {

        if(context == null) {
            return;
        }

        String updateFrequency = "";
        String newUrl = "";
        Boolean enabled = false;
        long updateFreqAsLong = 0;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        updateFrequency = sharedPreferences.getString(context.getResources().getString(R.string.pref_freq_key), context.getResources().getString(R.string.pref_freq_once_value));
        newUrl = sharedPreferences.getString(context.getResources().getString(R.string.pref_url_key), context.getResources().getString(R.string.pref_url_default));
        enabled = sharedPreferences.getBoolean(context.getResources().getString(R.string.pref_enable_key), false);
        updateFreqAsLong = Long.parseLong(updateFrequency);

        PendingIntent exactPendingIntent = null;
        Log.d("ALARM PARAMS", "Enabled: " + enabled + " URL: " + newUrl + " Frequency: " + updateFrequency);
        Intent updateBackground = new Intent("io.thomaspritchard.backgroundify.UPDATE_BACKGROUND");
        updateBackground.putExtra(URL, newUrl);
        exactPendingIntent = PendingIntent.getBroadcast(context, 0, updateBackground, PendingIntent.FLAG_UPDATE_CURRENT);

        if(enabled) {
            if(!delayed) {
                //Requesting alarm to be set to trigger immediately.
                Log.d("ALARM START", "Starting Undelayed Exact Alarm");
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime(),
                        exactPendingIntent);
            }
            else {
                //Requesting alarm to be set according to preferences.
                if(!updateFrequency.equals(context.getResources().getString(R.string.pref_freq_once_value))) {
                    if(android.os.Build.VERSION.SDK_INT >= 19) {
                        //Set next alarm for preferred interval.
                        Log.d("Testing", "Starting delayed alarm, delayed by " + updateFrequency + " minutes");
                        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME,
                                SystemClock.elapsedRealtime() + (1000 * 60 * updateFreqAsLong),
                                exactPendingIntent);
                    }
                    else { //setExact is api 19 or greater, set behaves as setExact in api 19 or less.
                        //Set next alarm for preferred interval.
                        Log.d("Testing", "Starting delayed alarm, delayed by " + updateFrequency + " minutes");
                        alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                                SystemClock.elapsedRealtime() + (1000 * 60 * updateFreqAsLong),
                                exactPendingIntent);
                    }

                    //Enable boot up alarm starting
                    ComponentName receiver = new ComponentName(context, OnBootReceiver.class);
                    PackageManager pm = context.getPackageManager();

                    pm.setComponentEnabledSetting(receiver,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                }
                else {
                    //do not set alarm, page just needed to be updated once.
                    Log.d("Testing", "Page loaded once not restarting alarm.");

                    //Disable boot up alarm starting
                    ComponentName receiver = new ComponentName(context, OnBootReceiver.class);
                    PackageManager pm = context.getPackageManager();

                    pm.setComponentEnabledSetting(receiver,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
            }
        }
        else {
            //Cancel the alarm
            Log.d("ALARM STOP", "Stopping Alarm");
            alarmManager.cancel(exactPendingIntent);

            //Disable boot up alarm starting
            ComponentName receiver = new ComponentName(context, OnBootReceiver.class);
            PackageManager pm = context.getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
