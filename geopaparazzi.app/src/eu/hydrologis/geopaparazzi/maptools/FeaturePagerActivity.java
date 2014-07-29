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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maps.MapsSupportService;
import eu.hydrologis.geopaparazzi.maptools.tools.OnSelectionToolGroup;
import jsqlite.Database;
import jsqlite.Exception;

/**
 * The activity to page features.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeaturePagerActivity extends Activity implements OnPageChangeListener {

    private TextView tableNameView;
    private TextView featureCounterView;
    private ArrayList<Feature> featuresList;
    private TextView dbNameView;
    private Feature selectedFeature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.features_viewer);

        Bundle extras = getIntent().getExtras();
        featuresList = extras.getParcelableArrayList(FeatureUtilities.KEY_FEATURESLIST);
        boolean isReadOnly = extras.getBoolean(FeatureUtilities.KEY_READONLY);

        selectedFeature = featuresList.get(0);
        PagerAdapter featureAdapter = new FeaturePageAdapter(this, featuresList, isReadOnly);

        ViewPager featuresPager = (ViewPager) findViewById(R.id.featurePager);
        // ViewPager viewPager = new ViewPager(this);
        featuresPager.setAdapter(featureAdapter);
        featuresPager.setOnPageChangeListener(this);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        tableNameView = (TextView) findViewById(R.id.tableNameView);
        dbNameView = (TextView) findViewById(R.id.databaseNameView);
        featureCounterView = (TextView) findViewById(R.id.featureCounterView);

        if (isReadOnly) {
            Button saveButton = (Button) findViewById(R.id.saveButton);
            saveButton.setVisibility(View.GONE);
            Button gotoButton = (Button) findViewById(R.id.gotoButton);
            gotoButton.setVisibility(View.GONE);
        }

        onPageSelected(0);
    }

    /**
     * Cancel button action.
     *
     * @param view the parent view.
     */
    public void onCancel(View view) {
        finish();
    }

    /**
     * Save button action.
     *
     * @param view the parent view.
     */
    public void onSave(View view) {
        int dirtyCount = 0;
        for (Feature feature : featuresList) {
            if (feature.isDirty()) {
                dirtyCount++;
            }
        }
        if (dirtyCount == 0) {
            finish();
            return;
        }

        StringAsyncTask saveDataTask = new StringAsyncTask(this) {
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
                    Utilities.errorDialog(FeaturePagerActivity.this, ex, null);
                }
                finish();
            }

        };
        saveDataTask.startProgressDialog("SAVE", "Saving data to database...", false, null);
        saveDataTask.execute();

    }

    private void saveData() throws Exception {
        List<SpatialVectorTable> spatialVectorTables = SpatialDatabasesManager.getInstance().getSpatialVectorTables(false);
        for (Feature feature : featuresList) {
            if (feature.isDirty()) {
                String tableName = feature.getUniqueTableName();

                for (SpatialVectorTable spatialVectorTable : spatialVectorTables) {
                    String uniqueNameBasedOnDbFilePath = spatialVectorTable.getUniqueNameBasedOnDbFilePath();
                    if (tableName.equals(uniqueNameBasedOnDbFilePath)) {
                        AbstractSpatialDatabaseHandler vectorHandler = SpatialDatabasesManager.getInstance().getVectorHandler(
                                spatialVectorTable);
                        if (vectorHandler instanceof SpatialiteDatabaseHandler) {
                            SpatialiteDatabaseHandler spatialiteDatabaseHandler = (SpatialiteDatabaseHandler) vectorHandler;
                            Database database = spatialiteDatabaseHandler.getDatabase();
                            DaoSpatialite.updateFeatureAlphanumericAttributes(database, feature);
                        }
                    }
                }

            }
        }
    }


    public void onGotoFeature(View view) {
        try {
            Geometry geometry = FeatureUtilities.getGeometry(selectedFeature);
            Coordinate centroid = geometry.getCentroid().getCoordinate();
            Context context = view.getContext();
            Intent intent = new Intent(context, MapsSupportService.class);
            intent.putExtra(MapsSupportService.CENTER_ON_POSITION_REQUEST, true);
            intent.putExtra(LibraryConstants.LONGITUDE, centroid.x);
            intent.putExtra(LibraryConstants.LATITUDE, centroid.y);
            context.startService(intent);

            ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
            if (activeToolGroup instanceof OnSelectionToolGroup) {
                OnSelectionToolGroup toolGroup = (OnSelectionToolGroup) activeToolGroup;
                toolGroup.setSelectedFeatures(Arrays.asList(selectedFeature));
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
        tableNameView.setText(selectedFeature.getTableName());
        int count = state + 1;
        featureCounterView.setText(count + "/" + featuresList.size());

        try {
            List<SpatialVectorTable> spatialVectorTables = SpatialDatabasesManager.getInstance().getSpatialVectorTables(false);
            String tableName = selectedFeature.getUniqueTableName();

            for (SpatialVectorTable spatialVectorTable : spatialVectorTables) {
                String uniqueNameBasedOnDbFilePath = spatialVectorTable.getUniqueNameBasedOnDbFilePath();
                if (tableName.equals(uniqueNameBasedOnDbFilePath)) {
                    AbstractSpatialDatabaseHandler vectorHandler = SpatialDatabasesManager.getInstance().getVectorHandler(
                            spatialVectorTable);
                    if (vectorHandler instanceof SpatialiteDatabaseHandler) {
                        SpatialiteDatabaseHandler spatialiteDatabaseHandler = (SpatialiteDatabaseHandler) vectorHandler;
                        String dbName = spatialiteDatabaseHandler.getName();
                        dbNameView.setText(dbName);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            GPLog.error(this, null, e);
            dbNameView.setText("");
        }


    }

}
