/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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

// MainActivity.java
// Hosts the GeopaparazziActivityFragment on a phone and both the
// GeopaparazziActivityFragment and SettingsActivityFragment on a tablet
package eu.geopaparazzi.library.core.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.core.fragments.DatabaseListFragment;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * Database list activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DatabaseListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_databaselist);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String sql = extras.getString(LibraryConstants.PREFS_KEY_QUERY);

            DatabaseListFragment databaseListFragment = new DatabaseListFragment();
            Bundle bundle = new Bundle();
            bundle.putString(LibraryConstants.PREFS_KEY_QUERY, sql);
            databaseListFragment.setArguments(bundle);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.databaseListContainer, databaseListFragment);
            transaction.commit();
        } else {
            throw new IllegalArgumentException("No sql supplied to query.");
        }

    }


}
