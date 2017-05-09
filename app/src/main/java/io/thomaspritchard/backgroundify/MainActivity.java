package io.thomaspritchard.backgroundify;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.view.View.GONE;
import static io.thomaspritchard.backgroundify.IabHelper.BILLING_RESPONSE_RESULT_ERROR;
import static io.thomaspritchard.backgroundify.IabHelper.BILLING_RESPONSE_RESULT_OK;
import static io.thomaspritchard.backgroundify.IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static boolean mBackgroundifyPro = false;

//    private class Delay extends AsyncTask<Void, Void, Integer> {
//        protected Integer doInBackground(Void... params) {
//            SystemClock.sleep(15000);
//            return 1;
//        }
//
//        protected void onProgressUpdate() {
//        }
//
//        protected void onPostExecute(Integer integer) {
//            Log.d("Testing", "Delay over, taking updating text.");
//            mButtonBackgroundify.setText(getString(R.string.button_complete));
//        }
//    }

    public static final String URL = "url";
    SharedPreferences mSharedPreferences = null;
    String mUrl = null;
    WebView mWebView = null;
    Button mButtonPreview = null;
    Button mButtonBackgroundify = null;
    Button mButtonUpgrade = null;
    Bundle mSavedInstanceState = null;
    IInAppBillingService mService;

    private BroadcastReceiver mBackgroundUpdatedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Testing", "Received background updated notification");
            if(intent.getAction().equals(context.getResources().getString(R.string.broadcast_background_updated))) {
                mButtonBackgroundify.setText(getString(R.string.button_complete));
            }
        }
    };

    private BroadcastReceiver mBackgroundProgressReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Testing", "Received background progress notification");
            if(intent.getAction().equals(context.getResources().getString(R.string.broadcast_background_progress))) {
                mButtonBackgroundify.setText(Integer.toString(intent.getExtras().getInt("PROGRESS")));
            }
        }
    };

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);

            try {
                Bundle purchases = mService.getPurchases(3, getPackageName(), "inapp", "");

                if(purchases.getInt("RESPONSE_CODE", BILLING_RESPONSE_RESULT_ERROR) == BILLING_RESPONSE_RESULT_OK){
                    //purchases was successfully received.
                    ArrayList<String> purchasesArray = purchases.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                    if(purchasesArray != null) {
                        if(purchasesArray.contains("backgroundify.pro")){
                            mBackgroundifyPro = true;
                        }
                    }
                }
            }
            catch(RemoteException e) {
                e.printStackTrace();
            }

            if(mBackgroundifyPro) {
                mButtonUpgrade.setVisibility(GONE);
                handleSuccessfulUpgradeActions();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSavedInstanceState = savedInstanceState;
        setContentView(R.layout.activity_main);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        mUrl = mSharedPreferences.getString(getString(R.string.pref_url_key), getString(R.string.pref_url_default));
        mWebView = (WebView) findViewById(R.id.preview_webview);
        mButtonBackgroundify = (Button) findViewById(R.id.button_backgroundify);
        mButtonUpgrade = (Button) findViewById(R.id.button_upgrade);
        mButtonBackgroundify.setOnClickListener(this);
        mButtonUpgrade.setOnClickListener(this);

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

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.broadcast_background_updated));
        registerReceiver(mBackgroundUpdatedReceiver, filter);

        IntentFilter otherFilter = new IntentFilter();
        otherFilter.addAction(getString(R.string.broadcast_background_progress));
        registerReceiver(mBackgroundProgressReceiver, otherFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mWebView != null) {
            mWebView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mWebView != null) {
            mWebView.onResume();
        }
    }

    private void searchWebPage() {
        Log.d("Testing", "Preview Called!");
        mUrl = mSharedPreferences.getString(getString(R.string.pref_url_key), getString(R.string.pref_url_default));
        mWebView.loadUrl(mUrl);
    }

    private void backgroundify() {
        Log.d("Testing", "Backgroundify Called!");
        AlarmHelper alarmHelper = new AlarmHelper();
        alarmHelper.setAlarm(this, false);
        mButtonBackgroundify.setText(getString(R.string.button_loading));
    }

    private void upgrade() {
        Log.d("Testing", "Upgrade!!!!!");
        Bundle buyIntentBundle = null;
        try {
            buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                    "backgroundify.pro", "inapp", "");
        } catch(RemoteException e) {
            e.printStackTrace();
        }

        if(buyIntentBundle.getInt("RESPONSE_CODE", BILLING_RESPONSE_RESULT_USER_CANCELED) == BILLING_RESPONSE_RESULT_OK) {
            //pro selection was success.
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            if(pendingIntent != null) {
                try {
                    startIntentSenderForResult(pendingIntent.getIntentSender(),
                            1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                            Integer.valueOf(0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton("Okay!", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });

            AlertDialog alert = builder.create();
            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    Log.d("Testing", "Sku = " + sku);
                    alert.setTitle("Congratulations!");
                    alert.setMessage("You have bought Backgroundify Pro!");
                    alert.show();
                }
                catch (JSONException e) {
                    alert.setTitle("Uh-oh!");
                    alert.setMessage("Failed to parse purchase data.");
                    alert.show();
                    e.printStackTrace();
                }
                mBackgroundifyPro = true;
                handleSuccessfulUpgradeActions();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.button_backgroundify) {
            backgroundify();
        }
        else if(id == R.id.button_upgrade) {
            if(!mBackgroundifyPro) {
                Log.d("Testing", "Upgrade to pro!!!");
                upgrade();
            }
        }
    }

    private void handleSuccessfulUpgradeActions() {
        mBackgroundifyPro = true;
        //Display the fragment as the main content.
        SettingsFragment sf = (SettingsFragment) getFragmentManager().findFragmentById(R.id.settings_fragment);
        sf.upgrade();
        mButtonUpgrade.setVisibility(GONE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(getString(R.string.pref_url_key))) {
            searchWebPage();
        }

        Button backgroundify = (Button) findViewById(R.id.button_backgroundify);
        backgroundify.setText(getString(R.string.button_accept));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(mServiceConn);
        }

        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(mBackgroundUpdatedReceiver);
        unregisterReceiver(mBackgroundProgressReceiver);
    }

    @TargetApi(android.os.Build.VERSION_CODES.N)
    private void displayFaq() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Okay!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        AlertDialog alert = builder.create();
        alert.setTitle(getString(R.string.faq_title));
        alert.setMessage(getString(R.string.faq_message));
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();
        if (itemThatWasClickedId == R.id.action_faq) {
            displayFaq();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
