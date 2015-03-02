package org.ftcollinsresearch.shutdowntimer;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by jeff boehmer on 2/27/15.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }
}
