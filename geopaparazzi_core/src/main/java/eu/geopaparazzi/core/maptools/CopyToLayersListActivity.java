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
package eu.geopaparazzi.core.maptools;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.vividsolutions.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.util.comparators.SpatialTableNameComparator;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.mapview.MapsSupportService;

/**
 * An activity that proposes a list of compatible layer to copy features over and then performs the copy.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CopyToLayersListActivity extends AppCompatActivity implements OnTouchListener {

    private SpatialVectorTable spatialVectorTable;

    private int buttonSelectionColor;
    private ArrayList<Feature> featuresList;
    private String fromTableSrid;
    private ListView mListView;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(eu.geopaparazzi.spatialite.R.layout.data_list);

        Toolbar toolbar = (Toolbar) findViewById(eu.geopaparazzi.mapsforge.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        EditText filterText = (EditText) findViewById(eu.geopaparazzi.spatialite.R.id.search_box);
        filterText.setVisibility(View.GONE);
        LinearLayout toggleButtonsView = (LinearLayout) findViewById(eu.geopaparazzi.spatialite.R.id.sourceTypeToggleButtonsView);
        toggleButtonsView.setVisibility(View.GONE);

        mListView = (ListView) findViewById(R.id.dataList);


        Bundle extras = getIntent().getExtras();
        featuresList = extras.getParcelableArrayList(FeatureUtilities.KEY_FEATURESLIST);
        try {
            SpatialVectorTable vectorTable = FeatureUtilities.getTableFromFeature(featuresList.get(0));
            fromTableSrid = vectorTable.getSrid();
            int fromTableGeomTypeInt = vectorTable.getGeomType();
            GeometryType fromTableGeometryType = GeometryType.forValue(fromTableGeomTypeInt);

            buttonSelectionColor = Compat.getColor(this, eu.geopaparazzi.spatialite.R.color.main_selection);

            final List<SpatialVectorTable> compatibleSpatialVectorTables = new ArrayList<>();
            Collection<SpatialVectorTable> spatialVectorTables = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap().values();
            for (SpatialVectorTable spatialVectorTable : spatialVectorTables) {
                if (spatialVectorTable.isEditable()) {
                    int geomType = spatialVectorTable.getGeomType();
                    GeometryType geometryType = GeometryType.forValue(geomType);
                    if (fromTableGeometryType.isGeometryTypeCompatible(geometryType)) {
                        compatibleSpatialVectorTables.add(spatialVectorTable);
                    }
                }
            }

            if (compatibleSpatialVectorTables.size() == 0) {
                GPDialogs.warningDialog(this, getString(eu.geopaparazzi.core.R.string.no_compatible_layers_found), new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
                return;
            }

            Collections.sort(compatibleSpatialVectorTables, new SpatialTableNameComparator());


            ArrayAdapter<SpatialVectorTable> arrayAdapter = new ArrayAdapter<SpatialVectorTable>(this, eu.geopaparazzi.spatialite.R.layout.editablelayers_row,
                    compatibleSpatialVectorTables) {
                @Override
                public View getView(final int position, View cView, ViewGroup parent) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View rowView = cView;
                    if (rowView == null)
                        rowView = inflater.inflate(eu.geopaparazzi.spatialite.R.layout.editablelayers_row, null);
                    try {
                        final SpatialVectorTable item = compatibleSpatialVectorTables.get(position);
                        TextView nameView = (TextView) rowView.findViewById(eu.geopaparazzi.spatialite.R.id.name);
                        TextView descriptionView = (TextView) rowView.findViewById(eu.geopaparazzi.spatialite.R.id.description);

                        final ImageButton editableButton = (ImageButton) rowView.findViewById(eu.geopaparazzi.spatialite.R.id.editableButton);
                        editableButton.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                /*
                                 * copy feature geometry over to layer
                                 */
                                SpatialVectorTable spatialVectorTable = compatibleSpatialVectorTables.get(position);

                                int count = 0;
                                for (Feature feature : featuresList) {
                                    try {
                                        Geometry geometry = FeatureUtilities.getGeometry(feature);
                                        DaoSpatialite.addNewFeatureByGeometry(geometry, LibraryConstants.SRID_WGS84_4326, spatialVectorTable);
                                        count++;
                                    } catch (Exception e) {
                                        GPLog.error(this, null, e);
                                        GPDialogs.errorDialog(editableButton.getContext(), e, null);
                                        return;
                                    }
                                }

                                try {
                                    // reset mapview
                                    Context context = v.getContext();
                                    Intent intent = new Intent(context, MapsSupportService.class);
                                    intent.putExtra(MapsSupportService.REREAD_MAP_REQUEST, true);
                                    context.startService(intent);
                                } catch (Exception e) {
                                    GPLog.error(this, null, e);
                                    GPDialogs.errorDialog(editableButton.getContext(), e, null);
                                    return;
                                }

                                if (count > 0)
                                    GPDialogs.toast(editableButton.getContext(), String.format(getString(eu.geopaparazzi.core.R.string.copied_features_to_layer), count, spatialVectorTable.getTableName()), Toast.LENGTH_SHORT);

                                finish();
                            }
                        });
                        editableButton.setOnTouchListener(CopyToLayersListActivity.this);
                        editableButton.setEnabled(true);
                        editableButton.setBackground(Compat.getDrawable(getContext(), eu.geopaparazzi.spatialite.R.drawable.ic_layer_visible));

                        nameView.setText(item.getTableName());

                        String dbName = item.getFileName();

//                        if (tableHandler != null) {
//                            String databasePath = tableHandler.getFile().getAbsolutePath();
//                            if (databasePath.startsWith(mapsDirPath)) {
//                                dbName = databasePath.replaceFirst(mapsDirPath, "");
//                                if (dbName.startsWith(File.separator)) {
//                                    dbName = dbName.substring(1);
//                                }
//                            }
//                        }

                        descriptionView.setText(item.getGeomName() + ": " + item.getTableTypeDescription() + "\n" + "database: "
                                + dbName);

                    } catch (Exception e1) {
                        GPLog.error(CopyToLayersListActivity.this, null, e1);
                    }

                    return rowView;
                }

            };
            mListView.setAdapter(arrayAdapter);

        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                v.getBackground().setColorFilter(buttonSelectionColor, Mode.SRC_OVER);
                v.invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                v.getBackground().clearColorFilter();
                v.invalidate();
                break;
            }
        }
        return false;
    }

}
