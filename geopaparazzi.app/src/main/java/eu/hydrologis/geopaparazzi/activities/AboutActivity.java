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
package eu.hydrologis.geopaparazzi.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.fragments.AboutFragment;
import eu.hydrologis.geopaparazzi.R;

/**
 * The about activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AboutActivity extends AppCompatActivity {

    @SuppressWarnings("nls")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(eu.hydrologis.geopaparazzi.R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(eu.hydrologis.geopaparazzi.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AboutFragment aboutFragment = new AboutFragment();
        Bundle bundle = new Bundle();
        bundle.putString(LibraryConstants.PREFS_KEY_TEXT, "eu.hydrologis.geopaparazzi");
        aboutFragment.setArguments(bundle);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragmentAboutContainer, aboutFragment);
        transaction.commit();
    }


}
