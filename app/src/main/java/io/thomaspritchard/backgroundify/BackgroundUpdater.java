package io.thomaspritchard.backgroundify;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.net.http.SslError;
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
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.io.IOException;

/**
 * Created by thomas on 4/7/17.
 */

public class BackgroundUpdater {

    Context mContext = null;
    private int mHeight = 0;
    private int mWidth = 0;
    private String mUrl = "";

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean updateBackground() {

        WebView w = new WebView(mContext);
        w.layout(0, 0, mWidth, mHeight);
        w.getSettings().setJavaScriptEnabled(true);
        w.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        w.setVisibility(View.GONE);

        w.setWebViewClient(new WebViewClient() {

            boolean timeout = true;
            boolean timedout = false;
            boolean firstTimeout = true;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
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
                            if(firstTimeout) {
                                firstTimeout = false;
                                Log.d("Testing", "First timeout re-issuing broadcast");
                                AlarmHelper alarmHelper = new AlarmHelper();
                                alarmHelper.cancelLastAlarm(mContext);
                                alarmHelper.setAlarm(mContext, false);
                            }
                            // do what you want
                            Log.d("Testing", "Timed out");
                        }
                    }
                }).start();
            }

            @Override
            @TargetApi(android.os.Build.VERSION_CODES.N)
            public void onPageFinished(WebView view, String url) {
                timeout = false;
                super.onPageFinished(view, url);
                if(timedout) {
                    timedout = false;
                    Log.d("Testing", "Page loaded too slow timed out and restarted page load.");
                }
                else {
                    Log.d("Testing", "Page finished");
                    Bitmap newBackground = takePicture(view);
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {//at least version 24
                            wallpaperManager.setBitmap(newBackground, null, true, WallpaperManager.FLAG_SYSTEM);
                            //TODO: If premium do wallpaper.
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
                            Boolean updateLock = sharedPreferences.getBoolean(mContext.getResources().getString(R.string.pref_enable_lock_key), false);
                            if(updateLock) {
                                wallpaperManager.setBitmap(newBackground, null, true, WallpaperManager.FLAG_LOCK);
                            }
                        } else {
                            wallpaperManager.setBitmap(newBackground);
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
                            Boolean updateLock = sharedPreferences.getBoolean(mContext.getResources().getString(R.string.pref_enable_lock_key), false);
                            if(updateLock) {
                                Log.d("Testing", "Would update lock if api > 24");
                            }
                            else {
                                Log.d("Testing", "Would not update lock if api > 24");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Log.d("LOAD", "About to load URL");
        CookieManager.getInstance().setAcceptCookie(true);
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//at least version lollipop
            CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
            CookieManager.getInstance().setAcceptFileSchemeCookies(true);
        }
        w.loadUrl(mUrl);
        return true;
    }

    private Bitmap takePicture(WebView view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

}
