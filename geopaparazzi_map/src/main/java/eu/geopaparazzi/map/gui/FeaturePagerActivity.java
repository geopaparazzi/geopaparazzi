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
package eu.geopaparazzi.map.gui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.Collections;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.map.MapsSupportService;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.features.Feature;
import eu.geopaparazzi.map.features.FeatureUtilities;
import eu.geopaparazzi.map.features.editing.EditManager;
import eu.geopaparazzi.map.features.tools.impl.LineOnSelectionToolGroup;
import eu.geopaparazzi.map.features.tools.impl.PointOnSelectionToolGroup;
import eu.geopaparazzi.map.features.tools.impl.PolygonOnSelectionToolGroup;
import eu.geopaparazzi.map.features.tools.interfaces.ToolGroup;
import eu.geopaparazzi.map.layers.utils.SpatialiteConnectionsHandler;
import eu.geopaparazzi.map.layers.utils.SpatialiteUtilities;

/**
 * The activity to page features.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("ALL")
public class FeaturePagerActivity extends AppCompatActivity implements OnPageChangeListener {

    private TextView tableNameView;
    private TextView featureCounterView;
    private ArrayList<Feature> featuresList;
    private TextView dbNameView;
    private Feature selectedFeature;
    private boolean isReadOnly;
    public final static String TABLENAME_EXTRA_MESSAGE = "eu.hydrologis.geopaparazzi.maptools.TABLEVIEW";
    public final static String DBPATH_EXTRA_MESSAGE = "eu.hydrologis.geopaparazzi.maptools.DBPATH";
    public final static String ROWID_EXTRA_MESSAGE = "eu.hydrologis.geopaparazzi.maptools.ROWID";
    private StringAsyncTask saveDataTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_featurepager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        featuresList = extras.getParcelableArrayList(FeatureUtilities.KEY_FEATURESLIST);
        isReadOnly = extras.getBoolean(FeatureUtilities.KEY_READONLY);

        selectedFeature = featuresList.get(0);
        PagerAdapter featureAdapter = new FeaturePageAdapter(this, featuresList, isReadOnly, getSupportFragmentManager());

        ViewPager featuresPager = findViewById(R.id.featurePager);
        // ViewPager viewPager = new ViewPager(this);
        featuresPager.setAdapter(featureAdapter);
        featuresPager.addOnPageChangeListener(this);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        tableNameView = findViewById(R.id.tableNameView);
        dbNameView = findViewById(R.id.databaseNameView);
        featureCounterView = findViewById(R.id.featureCounterView);

        onPageSelected(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isReadOnly) {
            getMenuInflater().inflate(R.menu.menu_readonlyfeaturepager, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_featurepager, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_goto) {
            gotoFeature();
        } else if (item.getItemId() == R.id.action_image_browser) {
            Intent intent = new Intent(this, ResourceBrowserActivity.class);
            String tableName = selectedFeature.getTableName();
            intent.putExtra(TABLENAME_EXTRA_MESSAGE, tableName);
            intent.putExtra(DBPATH_EXTRA_MESSAGE, selectedFeature.getDatabasePath());
            intent.putExtra(ROWID_EXTRA_MESSAGE, selectedFeature.getIdFieldValue());
            intent.putExtra(FeatureUtilities.KEY_READONLY, isReadOnly);
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_save) {
            int dirtyCount = 0;
            for (Feature feature : featuresList) {
                if (feature.isDirty()) {
                    dirtyCount++;
                }
            }
            if (dirtyCount == 0) {
                finish();
            }

            saveDataTask = new StringAsyncTask(this) {
                private Exception ex;

                @Override
                protected String doBackgroundWork() {
                    try {
                        saveData();
                    } catch (java.lang.Exception e) {
                        ex = e;
                    }
                    return "";
                }

                @Override
                protected void doUiPostWork(String response) {
                    if (ex != null) {
                        GPLog.error(this, "ERROR", ex);
                        GPDialogs.errorDialog(FeaturePagerActivity.this, ex, null);
                    }
                    Intent result = new Intent();
                    result.putParcelableArrayListExtra(FeatureUtilities.KEY_FEATURESLIST,
                            featuresList);
                    FeaturePagerActivity.this.setResult(RESULT_OK, result);
                    finish();
                }

            };
            saveDataTask.setProgressDialog(null, getString(R.string.saving_to_database), false, null);
            saveDataTask.execute();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (saveDataTask != null) saveDataTask.dispose();
        super.onDestroy();
    }

    private void saveData() throws java.lang.Exception {
        for (Feature feature : featuresList) {
            if (feature.isDirty()) {
                ASpatialDb db = SpatialiteConnectionsHandler.INSTANCE.getDb(feature.getDatabasePath());
                SpatialiteUtilities.updateFeatureAlphanumericAttributes(db, feature);
            }
        }
    }


    private void gotoFeature() {
        try {
            Geometry geometry = selectedFeature.getDefaultGeometry();
            Coordinate centroid = geometry.getCentroid().getCoordinate();
            Intent intent = new Intent(this, MapsSupportService.class);
            intent.putExtra(MapsSupportService.CENTER_ON_POSITION_REQUEST, true);
            intent.putExtra(LibraryConstants.LONGITUDE, centroid.x);
            intent.putExtra(LibraryConstants.LATITUDE, centroid.y);
            this.startService(intent);

            ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
            if (activeToolGroup instanceof PolygonOnSelectionToolGroup) {
                PolygonOnSelectionToolGroup toolGroup = (PolygonOnSelectionToolGroup) activeToolGroup;
                toolGroup.setSelectedFeatures(Collections.singletonList(selectedFeature));
            } else if (activeToolGroup instanceof LineOnSelectionToolGroup) {
                LineOnSelectionToolGroup toolGroup = (LineOnSelectionToolGroup) activeToolGroup;
                toolGroup.setSelectedFeatures(Collections.singletonList(selectedFeature));
            } else if (activeToolGroup instanceof PointOnSelectionToolGroup) {
                PointOnSelectionToolGroup toolGroup = (PointOnSelectionToolGroup) activeToolGroup;
                toolGroup.setSelectedFeatures(Collections.singletonList(selectedFeature));
            }

            finish();
        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }

    }

    public void onPageScrollStateChanged(int arg0) {
        // TODO Auto-generated method stub

    }

    public void onPageScrolled(int arg0, float arg1, int arg2) {
        // TODO Auto-generated method stub

    }

    public void onPageSelected(int state) {
        selectedFeature = featuresList.get(state);
        String tableName = selectedFeature.getTableName();
        tableNameView.setText(tableName);
        int count = state + 1;
        featureCounterView.setText(count + "/" + featuresList.size());
        dbNameView.setText(selectedFeature.getDatabasePath());
    }

}
