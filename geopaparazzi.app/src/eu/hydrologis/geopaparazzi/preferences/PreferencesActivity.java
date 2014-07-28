/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.preferences;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import eu.hydrologis.geopaparazzi.R;

/**
 * Prefs activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class PreferencesActivity extends PreferenceActivity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.my_preferences);

        // TODO make them all white
        // getWindow().setBackgroundDrawableResource(R.drawable.background);
        //
        // PreferenceScreen b = (PreferenceScreen)
        // findPreference("pref_second_preferencescreen_key");
        // b.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        //
        // @Override
        // public boolean onPreferenceClick(Preference preference) {
        // PreferenceScreen a = (PreferenceScreen) preference;
        // a.getDialog().getWindow().setBackgroundDrawableResource(R.drawable.background);
        // return false;
        // }
        // });

    }

}
