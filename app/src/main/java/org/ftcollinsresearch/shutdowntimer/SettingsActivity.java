/**
 * Created by jeff boehmer on 2/27/15.
 */

package org.ftcollinsresearch.shutdowntimer;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * This class retrieves and saves the Preference settings for the applicaton
 */
public class SettingsActivity extends PreferenceActivity {

    public String TEST_MODE = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TEST_MODE = getResources().getString(R.string.pref_test_mode);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(TEST_MODE)) {
            Preference pref = findPreference(key);
            pref.setSummary(sharedPreferences.getString(key, ""));
        }
    }
}
