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
package eu.hydrologis.geopaparazzi.maptools;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.Collections;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.hydrologis.geopaparazzi.maptools.resourceviews.ResourceBrowser;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maptools.tools.LineOnSelectionToolGroup;
import eu.hydrologis.geopaparazzi.maptools.tools.PointOnSelectionToolGroup;
import eu.hydrologis.geopaparazzi.maptools.tools.PolygonOnSelectionToolGroup;
import eu.hydrologis.geopaparazzi.mapview.MapsSupportService;
import jsqlite.Database;
import jsqlite.Exception;

/**
 * The activity to page features.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
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

        Toolbar toolbar = (Toolbar) findViewById(eu.geopaparazzi.mapsforge.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        featuresList = extras.getParcelableArrayList(FeatureUtilities.KEY_FEATURESLIST);
        isReadOnly = extras.getBoolean(FeatureUtilities.KEY_READONLY);

        selectedFeature = featuresList.get(0);
        PagerAdapter featureAdapter = new FeaturePageAdapter(this, featuresList, isReadOnly, getSupportFragmentManager());

        ViewPager featuresPager = (ViewPager) findViewById(R.id.featurePager);
        // ViewPager viewPager = new ViewPager(this);
        featuresPager.setAdapter(featureAdapter);
        featuresPager.addOnPageChangeListener(this);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        tableNameView = (TextView) findViewById(R.id.tableNameView);
        dbNameView = (TextView) findViewById(R.id.databaseNameView);
        featureCounterView = (TextView) findViewById(R.id.featureCounterView);

        onPageSelected(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isReadOnly) {
            getMenuInflater().inflate(R.menu.menu_featurepager, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_goto) {
            gotoFeature();
        }
        else if (item.getItemId() == R.id.action_image_browser) {
            Intent intent = new Intent(this, ResourceBrowser.class);
            String tableName = selectedFeature.getTableName();
            SpatialVectorTable table = SpatialiteSourcesManager.INSTANCE.getTableFromFeature(selectedFeature);
            intent.putExtra(TABLENAME_EXTRA_MESSAGE, tableName);
            intent.putExtra(DBPATH_EXTRA_MESSAGE, selectedFeature.getDatabasePath());
            intent.putExtra(ROWID_EXTRA_MESSAGE, selectedFeature.getId());
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.action_save) {
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
                    } catch (Exception e) {
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
                            (ArrayList<? extends Parcelable>) featuresList);
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

    private void saveData() throws Exception {
        for (Feature feature : featuresList) {
            if (feature.isDirty()) {
                SpatialVectorTable table = SpatialiteSourcesManager.INSTANCE.getTableFromFeature(feature);
                SpatialiteDatabaseHandler spatialiteDatabaseHandler = SpatialiteSourcesManager.INSTANCE.getExistingDatabaseHandlerByTable(table);
                Database database = spatialiteDatabaseHandler.getDatabase();
                DaoSpatialite.updateFeatureAlphanumericAttributes(database, feature);
            }
        }
    }


    private void gotoFeature() {
        try {
            Geometry geometry = FeatureUtilities.getGeometry(selectedFeature);
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
