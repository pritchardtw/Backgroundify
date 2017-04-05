package io.thomaspritchard.backgroundify;


import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;


public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    Context mContext; //Parent context because fragments don't have context.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_backgroundify);
        Preference pref = findPreference(getString(R.string.pref_url_key));
        EditTextPreference etPref = (EditTextPreference) pref;
        etPref.setSummary(etPref.getText());

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Figure out which preference was changed
        Preference preference = findPreference(key);
        if (null != preference) {
            String newUrl = sharedPreferences.getString(preference.getKey(), "");
            preference.setSummary(newUrl);
            updateBackground(newUrl);
        }
    }

    @Override
    public void onAttach(Context parentContext) {
        super.onAttach(parentContext);
        Log.d("ON_ATTACH", "On attach called");
        this.mContext = parentContext;
    }

    private boolean updateBackground(String url) {
        WebView w = new WebView(mContext);
        w.getSettings().setJavaScriptEnabled(true);
        w.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        w.setWebViewClient(new WebViewClient() {
            @Override
            @TargetApi(android.os.Build.VERSION_CODES.N)
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(view.getProgress() == 100) {
                    Bitmap newBackground = takePicture(view);
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                    try {
                        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {//at least version 24
                            wallpaperManager.setBitmap(newBackground, null, true, WallpaperManager.FLAG_SYSTEM);
                            wallpaperManager.setBitmap(newBackground, null, true, WallpaperManager.FLAG_LOCK);
                        }
                        else {
                            wallpaperManager.setBitmap(newBackground);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Log.d("LOAD", "About to load URL");
        w.setVisibility(View.INVISIBLE);
        int height = getActivity().getWindow().getDecorView().getHeight();
        int width = getActivity().getWindow().getDecorView().getWidth();
        w.layout(0, 0, width, height);
        w.loadUrl(url);
        return true;
    }

    private Bitmap takePicture(WebView view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}