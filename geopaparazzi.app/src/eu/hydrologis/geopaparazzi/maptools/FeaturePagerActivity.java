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
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;

/**
 * The activity to page features.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeaturePagerActivity extends Activity implements OnPageChangeListener {

    private TextView tableNameView;
    private TextView featureCounterView;
    private ArrayList<Feature> featuresList;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.features_viewer);

        Bundle extras = getIntent().getExtras();
        featuresList = extras.getParcelableArrayList(FeatureUtilities.KEY_FEATURESLIST);
        boolean isReadOnly = extras.getBoolean(FeatureUtilities.KEY_READONLY);

        PagerAdapter featureAdapter = new FeaturePageAdapter(this, featuresList, isReadOnly);

        ViewPager featuresPager = (ViewPager) findViewById(R.id.featurePager);
        // ViewPager viewPager = new ViewPager(this);
        featuresPager.setAdapter(featureAdapter);
        featuresPager.setOnPageChangeListener(this);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        tableNameView = (TextView) findViewById(R.id.tableNameView);
        featureCounterView = (TextView) findViewById(R.id.featureCounterView);

        if (isReadOnly) {
            Button saveButton = (Button) findViewById(R.id.saveButton);
            saveButton.setVisibility(View.GONE);
            Button editButton = (Button) findViewById(R.id.editButton);
            editButton.setVisibility(View.GONE);
        }

        onPageSelected(0);
    }

    /**
     * Cancel button action.
     * 
     * @param view the parent view.
     */
    public void onCancel( View view ) {
        finish();
    }

    public void onPageScrollStateChanged( int arg0 ) {
        // TODO Auto-generated method stub

    }

    public void onPageScrolled( int arg0, float arg1, int arg2 ) {
        // TODO Auto-generated method stub

    }

    public void onPageSelected( int state ) {
        Feature feature = featuresList.get(state);
        tableNameView.setText(feature.tableName);
        int count = state + 1;
        featureCounterView.setText(count + "/" + featuresList.size());
    }
}
