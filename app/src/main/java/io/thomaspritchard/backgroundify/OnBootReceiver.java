package io.thomaspritchard.backgroundify;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by thomas on 4/7/17.
 */

public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BOOT RECEIVER", "Received boot up signal");
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d("BOOT RECEIVER", "Received boot up signal");
            AlarmHelper.updateAlarm(context);
        }
    }
}
