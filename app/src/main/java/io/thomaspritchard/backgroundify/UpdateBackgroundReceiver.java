package io.thomaspritchard.backgroundify;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;

/**
 * Created by thomas on 4/7/17.
 */

public class UpdateBackgroundReceiver extends BroadcastReceiver {
    Context mContext = null;
    @Override
    public void onReceive(Context context, Intent intent) {
        String newUrl = "";
        int height = 0;
        int width = 0;
        mContext = context;
        Log.d("Background Receiver", "Received update background signal");
        if (intent.getAction().equals("io.thomaspritchard.backgroundify.UPDATE_BACKGROUND")) {
            Log.d("Background Receicver", "Received update background action");
            newUrl = intent.getExtras().getString(AlarmHelper.URL);
            Log.d("Background Receicver", "URL " + newUrl);
            updateBackground(newUrl);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean updateBackground(String url) {
        //generate a service to run based on preferences.
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        WebView w = new WebView(mContext);
        w.layout(0, 0, width, height);
        w.getSettings().setJavaScriptEnabled(true);
        w.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        w.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        w.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        w.setVisibility(View.GONE);

        w.setWebViewClient(new WebViewClient() {

            boolean timeout = true;
            boolean timedout = false;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //If the page doesn't load in 10 seconds
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if(timeout) {
                            timedout = true;
                            // do what you want
                            Log.d("Testing", "Timed out");
                            //Update alarm asap page didn't load try again.
                            AlarmHelper.updateAlarm(false);
                        }
                    }
                }).start();
            }

            @Override
            @TargetApi(android.os.Build.VERSION_CODES.N)
            public void onPageFinished(WebView view, String url) {
                timeout = false;
                if(timedout) {
                    timedout = false;
                    Log.d("Testing", "Page loaded too slow timed out and restarted page load.");
                }
                else {
                    Log.d("Testing", "Page finished");
                    AlarmHelper.updateAlarm(true);
                    super.onPageFinished(view, url);
                    Bitmap newBackground = takePicture(view);

                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {//at least version 24
                            wallpaperManager.setBitmap(newBackground, null, true, WallpaperManager.FLAG_SYSTEM);
                            //TODO: If premium do wallpaper.
                            wallpaperManager.setBitmap(newBackground, null, true, WallpaperManager.FLAG_LOCK);
                        } else {
                            wallpaperManager.setBitmap(newBackground);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Log.d("LOAD", "About to load URL");
        CookieManager.getInstance().setAcceptCookie(true);
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//at least version 24
            CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
            CookieManager.getInstance().setAcceptFileSchemeCookies(true);
        }
        w.loadUrl(url);
        String freq = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext()).getString(mContext.getResources().getString(R.string.pref_freq_key), mContext.getResources().getString(R.string.pref_freq_once_value));
        Log.d("Testing", "Frequency is " + freq);
        return true;
    }

    private Bitmap takePicture(WebView view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

}
