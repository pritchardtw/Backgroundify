package io.thomaspritchard.backgroundify;

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

    //Updates alarm according to context's preference settings.
    public static void updateAlarm(Context context) {

        String updateFrequency = "";
        String newUrl = "";
        Boolean enabled = false;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        updateFrequency = sharedPreferences.getString(context.getResources().getString(R.string.pref_freq_key), context.getResources().getString(R.string.pref_freq_once_value));
        newUrl = sharedPreferences.getString(context.getResources().getString(R.string.pref_url_key), context.getResources().getString(R.string.pref_url_default));
        enabled = sharedPreferences.getBoolean(context.getResources().getString(R.string.pref_enable_key), false);
        int updateFreqAsInt = Integer.parseInt(updateFrequency);
        PendingIntent pendingIntent = null;
        Log.d("ALARM PARAMS", "Enabled: " + enabled + " URL: " + newUrl + " Frequency: " + updateFrequency);
        Intent updateBackground = new Intent("io.thomaspritchard.backgroundify.UPDATE_BACKGROUND");
        updateBackground.putExtra(URL, newUrl);
        pendingIntent = PendingIntent.getBroadcast(context, 0, updateBackground, PendingIntent.FLAG_UPDATE_CURRENT);

        if(enabled) {

            if(!(updateFrequency.equals(context.getResources().getString(R.string.pref_freq_once_value)))) {
                //START THE ALARM
                Log.d("ALARM START", "Starting Repeating Alarm");
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime(),
                        (60 * 1000 * updateFreqAsInt),
                        pendingIntent);
            }
            else {
                //This will run the background updater itent once right when the settings are changed if enabled.
                Log.d("ALARM START", "Starting Initial Alarm");
                alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime(),
                        pendingIntent);
            }

            //Enable boot up alarm starting
            ComponentName receiver = new ComponentName(context, OnBootReceiver.class);
            PackageManager pm = context.getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
        else {
            //Cancel the alarm
            Log.d("ALARM STOP", "Stopping Alarm");
            alarmManager.cancel(pendingIntent);
            //Disable boot up alarm starting
            ComponentName receiver = new ComponentName(context, OnBootReceiver.class);
            PackageManager pm = context.getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
