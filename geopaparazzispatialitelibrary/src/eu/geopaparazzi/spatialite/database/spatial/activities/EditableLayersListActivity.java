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

import android.app.ListActivity;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.layers.SpatialVectorTableLayer;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.util.comparators.SpatialTableNameComparator;

/**
 * Editable spatialtables listing and choosing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class EditableLayersListActivity extends ListActivity implements OnTouchListener {

    private String mapsDirPath;

    private int index = 0;

    private SpatialVectorTable spatialVectorTable;

    private int buttonSelectionColor;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.data_list);

        EditText filterText = (EditText) findViewById(R.id.search_box);
        filterText.setVisibility(View.GONE);
        LinearLayout toggleButtonsView = (LinearLayout) findViewById(R.id.sourceTypeToggleButtonsView);
        toggleButtonsView.setVisibility(View.GONE);

        buttonSelectionColor = getResources().getColor(R.color.main_selection);

        try {
            mapsDirPath = ResourcesManager.getInstance(this).getMapsDir().getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final List<SpatialVectorTable> editableSpatialVectorTables = new ArrayList<SpatialVectorTable>();
        final List<String> editableSpatialVectorTablesNames = new ArrayList<String>();
        try {
            List<SpatialVectorTable> spatialVectorTables = SpatialDatabasesManager.getInstance().getSpatialVectorTables(false);
            for( SpatialVectorTable spatialVectorTable : spatialVectorTables ) {
                if (spatialVectorTable.isEditable()) {
                    int geomType = spatialVectorTable.getGeomType();
                    GeometryType geometryType = GeometryType.forValue(geomType);
                    switch( geometryType ) {
                    case POLYGON_XY:
                    case POLYGON_XYM:
                    case POLYGON_XYZ:
                    case POLYGON_XYZM:
                    case MULTIPOLYGON_XY:
                    case MULTIPOLYGON_XYM:
                    case MULTIPOLYGON_XYZ:
                    case MULTIPOLYGON_XYZM:
                        editableSpatialVectorTables.add(spatialVectorTable);
                        editableSpatialVectorTablesNames.add(spatialVectorTable.getTableName());
                        break;
                    default:
                        break;
                    }
                }
            }
        } catch (jsqlite.Exception e) {
            e.printStackTrace();
        }

        if (editableSpatialVectorTables.size()==0){
            Utilities.messageDialog(this, "No editable layers found", new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
            return;
        }

        Collections.sort(editableSpatialVectorTables, new SpatialTableNameComparator());
        Collections.sort(editableSpatialVectorTablesNames);

        final ILayer editLayer = EditManager.INSTANCE.getEditLayer();

        if (editLayer instanceof SpatialVectorTableLayer) {
            SpatialVectorTableLayer layer = (SpatialVectorTableLayer) editLayer;
            spatialVectorTable = layer.getSpatialVectorTable();
            int indexOf = editableSpatialVectorTables.indexOf(spatialVectorTable);
            if (indexOf != -1) {
                index = indexOf;
            }
        }

        ArrayAdapter<SpatialVectorTable> arrayAdapter = new ArrayAdapter<SpatialVectorTable>(this, R.layout.editablelayers_row,
                editableSpatialVectorTables){
            @SuppressWarnings("nls")
            @Override
            public View getView( final int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.editablelayers_row, null);
                try {
                    final SpatialVectorTable item = editableSpatialVectorTables.get(position);
                    AbstractSpatialDatabaseHandler tableHandler = null;
                    tableHandler = SpatialDatabasesManager.getInstance().getVectorHandler(item);

                    TextView nameView = (TextView) rowView.findViewById(R.id.name);
                    TextView descriptionView = (TextView) rowView.findViewById(R.id.description);

                    ImageButton editableButton = (ImageButton) rowView.findViewById(R.id.editableButton);
                    editableButton.setOnClickListener(new View.OnClickListener(){
                        public void onClick( View v ) {
                            SpatialVectorTable spatialVectorTable = editableSpatialVectorTables.get(position);
                            ILayer layer = new SpatialVectorTableLayer(spatialVectorTable);
                            EditManager.INSTANCE.setEditLayer(layer);

                            finish();
                        }
                    });
                    editableButton.setOnTouchListener(EditableLayersListActivity.this);
                    editableButton.setEnabled(true);
                    if (spatialVectorTable != null && spatialVectorTable == item) {
                        editableButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_layer_editable));
                    } else if (item.isTableEnabled() == 1) {
                        editableButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_layer_visible));
                    } else {
                        editableButton.setEnabled(false);
                    }

                    // rowView.setBackgroundColor(ColorUtilities.toColor(item.getColor()));
                    // mj10777: some tables may have more than one column, thus the column name will
                    // also be shown item.getUniqueName()
                    nameView.setText(item.getTableName());

                    String dbName = item.getFileName();

                    if (mapsDirPath != null && tableHandler != null) {
                        String databasePath = tableHandler.getFile().getAbsolutePath();
                        if (databasePath.startsWith(mapsDirPath)) {
                            dbName = databasePath.replaceFirst(mapsDirPath, "");
                            if (dbName.startsWith(File.separator)) {
                                dbName = dbName.substring(1);
                            }
                        }
                    }

                    descriptionView.setText(item.getGeomName() + ": " + item.getLayerTypeDescription() + "\n" + "database: "
                            + dbName);

                } catch (jsqlite.Exception e1) {
                    GPLog.error(EditableLayersListActivity.this, null, e1);
                }

                return rowView;
            }

        };
        setListAdapter(arrayAdapter);

        if (index != -1) {
            // move to the right position (this does not actually select the item)
            ListView listView = getListView();
            listView.setSelection(index);
        }

    }

    public boolean onTouch( View v, MotionEvent event ) {
        switch( event.getAction() ) {
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
