package io.thomaspritchard.backgroundify;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Toast;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;


public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    Context mContext; //Parent context because fragments don't have context.
    SharedPreferences mSharedPreferences = null;
    PreferenceScreen mPrefScreen = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        if(MainActivity.mBackgroundifyPro) {
            addPreferencesFromResource(R.xml.pref_backgroundify_pro);
        }
        else {
            addPreferencesFromResource(R.xml.pref_backgroundify);
        }

        updateAllPreferenceSummaries();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Preference preference = findPreference(getString(R.string.pref_url_key));
        preference.setOnPreferenceChangeListener(this);
    }

    public void upgrade() {
        String url = mSharedPreferences.getString(getString(R.string.pref_url_key), getString(R.string.pref_url_default));
        Boolean enabled = mSharedPreferences.getBoolean(getString(R.string.pref_enable_key), false);
        mPrefScreen.removeAll();
        addPreferencesFromResource(R.xml.pref_backgroundify_pro);
        mSharedPreferences = mPrefScreen.getSharedPreferences();

        if (!(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)) {
            //Remove enable lock screen preference for sdks under 24.
            int count = mPrefScreen.getPreferenceCount();

            for (int i = 0; i < count; i++) {
                Preference p = mPrefScreen.getPreference(i);

                if (p.getKey() == getString(R.string.pref_enable_lock_key)) {
                    mPrefScreen.removePreference(p);
                }
            }
        }

        SharedPreferences.Editor sPE = mSharedPreferences.edit();
        sPE.putString(getString(R.string.pref_url_key), url);
        sPE.putBoolean(getString(R.string.pref_enable_key), enabled);
        sPE.commit();
        updateAllPreferenceSummaries();
        Preference preference = findPreference(getString(R.string.pref_url_key));
        preference.setOnPreferenceChangeListener(this);
    }

    private void updateAllPreferenceSummaries() {
        mSharedPreferences = getPreferenceScreen().getSharedPreferences();
        mPrefScreen = getPreferenceScreen();
        int count = mPrefScreen.getPreferenceCount();

        // Go through all of the preferences, and set up their preference summary.
        for (int i = 0; i < count; i++) {
            Preference p = mPrefScreen.getPreference(i);
            // You don't need to set up preference summaries for checkbox preferences because
            // they are already set up in xml using summaryOff and summary On
            if (!(p instanceof CheckBoxPreference) && !(p instanceof SwitchPreference)) {
                String value = mSharedPreferences.getString(p.getKey(), "");
                setPreferenceSummary(p, value);
            }
        }
    }

    private void setPreferenceSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            // For list preferences, figure out the label of the selected value
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if (prefIndex >= 0) {
                // Set the summary to that label
                listPreference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (preference instanceof EditTextPreference) {
            // For EditTextPreferences, set the summary to the value's simple string representation.
            preference.setSummary(value);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Figure out which preference was changed

        Preference preference = findPreference(key);
        if(preference == null) {
            return;
        }
        else if (!(preference instanceof CheckBoxPreference) && !(preference instanceof SwitchPreference)) {
            String value = sharedPreferences.getString(key, "");
            setPreferenceSummary(preference, value);

            if(preference.getKey().equals(getString(R.string.pref_url_key))) {
                EditTextPreference etp = (EditTextPreference) preference;
                etp.setText(mSharedPreferences.getString(getString(R.string.pref_url_key), getString(R.string.pref_url_default)));
            }
        }
    }

    @Override
    public void onAttach(Context parentContext) {
        super.onAttach(parentContext);
        Log.d("ON_ATTACH", "On attach called");
        this.mContext = parentContext;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // In this context, we're using the onPreferenceChange listener for manipulating the url
        Log.d("Testing", "On preference change called");
        // Double check that the preference is the size preference
        String urlKey = getString(R.string.pref_url_key);
        if (preference.getKey().equals(urlKey)) {
            String url = (String) newValue;
            Log.d("Testing", "Url = " + url);
            if (!url.startsWith("www.") && !url.startsWith("http://") && !url.startsWith("https://")) {
                url = "www." + url;
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            Log.d("Testing", "Url After = " + url);

            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(getString(R.string.pref_url_key), url);
            editor.apply();

            return false;
        }

        return true;
    }
}