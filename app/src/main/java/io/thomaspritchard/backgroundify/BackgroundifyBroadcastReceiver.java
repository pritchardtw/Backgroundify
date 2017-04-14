package io.thomaspritchard.backgroundify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by thomas on 4/11/17.
 */

public class BackgroundifyBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Tetsing", "onReceive Called for Broadcast Receiver");
        if (intent.getAction().equals(context.getResources().getString(R.string.broadcast_update_background))) {
            Log.d("Background Receiver", "Received update background action");
            String newUrl = intent.getExtras().getString(MainActivity.URL);
            Log.d("Background Receiver", "URL " + newUrl);

            //Update Background
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            BackgroundUpdater backgroundUpdater = new BackgroundUpdater();
            backgroundUpdater.setContext(context);
            backgroundUpdater.setHeight(height);
            backgroundUpdater.setWidth(width);
            backgroundUpdater.setUrl(newUrl);
            backgroundUpdater.updateBackground();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

            String updateFrequency = sharedPreferences.getString(context.getResources().getString(R.string.pref_freq_key), context.getResources().getString(R.string.pref_freq_once_value));
            if(!(updateFrequency.equals(context.getResources().getString(R.string.pref_freq_once_value)))) {
                Log.d("Testing", "Setting Next Alarm");
                //Set next Alarm
                AlarmHelper alarmHelper = new AlarmHelper();
                alarmHelper.setAlarm(context, true);
            }
            else {
                Log.d("Testing", "Not starting next alarm because we're only supposed to update once.");
            }
        }
        else if(intent.getAction().equals(context.getResources().getString(R.string.broadcast_boot_completed))) {
            AlarmHelper alarmHelper = new AlarmHelper();
            alarmHelper.setAlarm(context, false);
        }
    }
}
