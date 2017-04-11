package io.thomaspritchard.backgroundify;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    SharedPreferences mSharedPreferences = null;
    String mUrl = null;
    WebView mWebView = null;
    Button mButtonPreview = null;
    Button mButtonBackgroundify = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        mUrl = mSharedPreferences.getString(getString(R.string.pref_url_key), getString(R.string.pref_url_default));
        mWebView = (WebView) findViewById(R.id.preview_webview);
        mButtonPreview = (Button)findViewById(R.id.button_preview);
        mButtonBackgroundify = (Button)findViewById(R.id.button_backgroundify);

        mButtonPreview.setOnClickListener(this);
        mButtonBackgroundify.setOnClickListener(this);

        mUrl = mSharedPreferences.getString(getString(R.string.pref_url_key), getString(R.string.pref_url_default));
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        mWebView.layout(0, 0, width, height);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl(mUrl);


        // Display the fragment as the main content.
//        getFragmentManager().beginTransaction()
//                .replace(android.R.id.content, new SettingsFragment())
//                .commit();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.button_preview) {
            Log.d("Testing", "Preview Called!");
            mUrl = mSharedPreferences.getString(getString(R.string.pref_url_key), getString(R.string.pref_url_default));
            mWebView.loadUrl(mUrl);
        }
        else if(id == R.id.button_backgroundify) {
            Log.d("Testing", "Backgroundify Called!");
            AlarmHelper.updateAlarm(this);
        }
    }
}
