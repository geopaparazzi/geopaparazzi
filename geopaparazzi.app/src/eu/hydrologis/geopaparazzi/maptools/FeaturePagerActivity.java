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
package eu.hydrologis.geopaparazzi.maptools;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.WindowManager;
import eu.hydrologis.geopaparazzi.R;

/**
 * The activity to page features.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeaturePagerActivity extends Activity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.features_viewer);

        Bundle extras = getIntent().getExtras();
        ArrayList<Feature> featuresList = extras.getParcelableArrayList("FEATURESLIST");

        PagerAdapter featureAdapter = new FeaturePageAdapter(this, featuresList, true);

        ViewPager featuresPager = (ViewPager) findViewById(R.id.featurePager);
        // ViewPager viewPager = new ViewPager(this);
        featuresPager.setAdapter(featureAdapter);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
}
