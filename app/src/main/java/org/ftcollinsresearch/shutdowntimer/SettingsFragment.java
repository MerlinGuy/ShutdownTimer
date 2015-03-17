/**
 * Created by jeff boehmer on 2/27/15.
 *
 * This program is free software and covered under the Apache License, Version 2.0 license
 *
 */
package org.ftcollinsresearch.shutdowntimer;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }
}
