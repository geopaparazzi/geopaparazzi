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
package eu.geopaparazzi.spatialite.database.spatial.activities;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.core.maps.SpatialiteMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.layers.SpatialVectorTableLayer;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;

/**
 * Editable spatialtables listing and choosing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class EditableLayersListActivity extends AppCompatActivity{

    private int index = 0;

    private SpatialiteMap spatialiteMap;

    private int buttonSelectionColor;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_editable_layers_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView mListView = findViewById(R.id.editablelayerslist);

        buttonSelectionColor = Compat.getColor(this, R.color.main_selection);

        final List<SpatialiteMap> editableSpatialiteMaps = new ArrayList<>();
        final HashMap<SpatialiteMap, SpatialVectorTable> spatialVectorTables = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap();
        try {

            for (Map.Entry<SpatialiteMap, SpatialVectorTable> entry : spatialVectorTables.entrySet()) {
                SpatialVectorTable spatialVectorTable = entry.getValue();
                if (spatialVectorTable.isEditable()) {
                    int geomType = spatialVectorTable.getGeomType();
                    GeometryType geometryType = GeometryType.forValue(geomType);
                    switch (geometryType) {
                        // supported types
                        case POLYGON_XY:
                        case POLYGON_XYM:
                        case POLYGON_XYZ:
                        case POLYGON_XYZM:
                        case MULTIPOLYGON_XY:
                        case MULTIPOLYGON_XYM:
                        case MULTIPOLYGON_XYZ:
                        case MULTIPOLYGON_XYZM:
                        case LINESTRING_XY:
                        case LINESTRING_XYM:
                        case LINESTRING_XYZ:
                        case LINESTRING_XYZM:
                        case MULTILINESTRING_XY:
                        case MULTILINESTRING_XYM:
                        case MULTILINESTRING_XYZ:
                        case MULTILINESTRING_XYZM:
                        case POINT_XY:
                        case POINT_XYM:
                        case POINT_XYZ:
                        case POINT_XYZM:
                        case MULTIPOINT_XY:
                        case MULTIPOINT_XYM:
                        case MULTIPOINT_XYZ:
                        case MULTIPOINT_XYZM:
                            editableSpatialiteMaps.add(entry.getKey());
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }

        if (editableSpatialiteMaps.size() == 0) {
            GPDialogs.warningDialog(this, "No editable layers found", new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
            return;
        }

        Collections.sort(editableSpatialiteMaps, new Comparator<SpatialiteMap>() {
            @Override
            public int compare(SpatialiteMap lhs, SpatialiteMap rhs) {
                return lhs.tableName.compareToIgnoreCase(rhs.tableName);
            }
        });

        final ILayer editLayer = EditManager.INSTANCE.getEditLayer();


        if (editLayer instanceof SpatialVectorTableLayer) {
            SpatialVectorTableLayer layer = (SpatialVectorTableLayer) editLayer;
            SpatialVectorTable spatialVectorTable = layer.getSpatialVectorTable();

            int tmpIndex = 0;
            for (Map.Entry<SpatialiteMap, SpatialVectorTable> entry : spatialVectorTables.entrySet()) {
                SpatialVectorTable tmp = entry.getValue();
                if (tmp.equals(spatialVectorTable)) {
                    index = tmpIndex;
                    spatialiteMap = entry.getKey();
                    break;
                }
                tmpIndex++;
            }
        }

        ArrayAdapter<SpatialiteMap> arrayAdapter = new ArrayAdapter<SpatialiteMap>(this, R.layout.editablelayers_row,
                editableSpatialiteMaps) {
            private ImageButton currentEditable = null;

            @NonNull
            @Override
            public View getView(final int position, View cView, @NonNull ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.editablelayers_row, null);
                try {
                    final SpatialiteMap item = editableSpatialiteMaps.get(position);

                    TextView nameView = rowView.findViewById(R.id.name);
                    TextView descriptionView = rowView.findViewById(R.id.description);

                    final ImageButton editableButton = rowView.findViewById(R.id.editableButton);
                    editableButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (currentEditable != null) {
                                currentEditable.setImageDrawable(Compat.getDrawable(EditableLayersListActivity.this, R.drawable.ic_layer_visible));
                            }
                            editableButton.setImageDrawable(Compat.getDrawable(EditableLayersListActivity.this, R.drawable.ic_layer_editable));
                            currentEditable = editableButton;

                            SpatialiteMap spatialiteMap = editableSpatialiteMaps.get(position);
                            SpatialVectorTable spatialVectorTable = spatialVectorTables.get(spatialiteMap);
                            ILayer layer = new SpatialVectorTableLayer(spatialVectorTable);
                            EditManager.INSTANCE.setEditLayer(layer);

//                            finish();
                        }
                    });
                    editableButton.setEnabled(true);
                    if (spatialiteMap != null && spatialiteMap == item) {
                        editableButton.setImageDrawable(Compat.getDrawable(EditableLayersListActivity.this, R.drawable.ic_layer_editable));
                        currentEditable = editableButton;
                    } else if (item.isVisible) {
                        editableButton.setImageDrawable(Compat.getDrawable(EditableLayersListActivity.this, R.drawable.ic_layer_visible));
                    } else {
                        editableButton.setEnabled(false);
                    }

                    nameView.setText(item.tableName);

                    String dbName = item.databasePath;

                    descriptionView.setText(item.geometryType + "\n" + "database: "
                            + dbName);

                } catch (Exception e1) {
                    GPLog.error(EditableLayersListActivity.this, null, e1);
                }

                return rowView;
            }

        };
        mListView.setAdapter(arrayAdapter);

        if (index != -1) {
            // move to the right position (this does not actually select the item)
            mListView.setSelection(index);
        }

    }


}
