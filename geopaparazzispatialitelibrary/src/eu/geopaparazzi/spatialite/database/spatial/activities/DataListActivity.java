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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.TextView;
import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.OrderComparator;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.geopaparazzi.spatialite.util.SpatialiteLibraryConstants;

/**
 * Data listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DataListActivity extends ListActivity {

    private List<SpatialVectorTable> spatialTables = new ArrayList<SpatialVectorTable>();;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.data_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList(true);
    }

    private void refreshList( boolean doReread ) {
        // if (Debug.D)
        //            Logger.d(this, "refreshing data list"); //$NON-NLS-1$

        try {
            if (doReread)
                spatialTables = SpatialDatabasesManager.getInstance().getSpatialVectorTables(doReread);
        } catch (Exception e) {
            // Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        ArrayAdapter<SpatialVectorTable> arrayAdapter = new ArrayAdapter<SpatialVectorTable>(this, R.layout.data_row, spatialTables){
            @Override
            public View getView( final int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.data_row, null);
                final SpatialVectorTable item = spatialTables.get(position);

                TextView nameView = (TextView) rowView.findViewById(R.id.name);
                CheckBox visibleView = (CheckBox) rowView.findViewById(R.id.visible);
                ImageButton listUpButton = (ImageButton) rowView.findViewById(R.id.upButton);
                listUpButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        if (position > 0) {
                            SpatialVectorTable before = spatialTables.get(position - 1);
                            int tmp1 = before.getStyle().order;
                            int tmp2 = item.getStyle().order;
                            item.getStyle().order = tmp1;
                            before.getStyle().order = tmp2;
                            Collections.sort(spatialTables, new OrderComparator());
                            refreshList(false);
                        }
                    }
                });

                ImageButton listDownButton = (ImageButton) rowView.findViewById(R.id.downButton);
                listDownButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        if (position < spatialTables.size() - 1) {
                            SpatialVectorTable after = spatialTables.get(position + 1);
                            int tmp1 = after.getStyle().order;
                            int tmp2 = item.getStyle().order;
                            item.getStyle().order = tmp1;
                            after.getStyle().order = tmp2;
                            Collections.sort(spatialTables, new OrderComparator());
                            refreshList(false);
                        }
                    }
                });

                ImageButton propertiesButton = (ImageButton) rowView.findViewById(R.id.propertiesButton);
                propertiesButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        Intent intent = null;
                        if (item.isLine()) {
                            intent = new Intent(DataListActivity.this, LinesDataPropertiesActivity.class);
                        } else if (item.isPolygon()) {
                            intent = new Intent(DataListActivity.this, PolygonsDataPropertiesActivity.class);
                        } else if (item.isPoint()) {
                            intent = new Intent(DataListActivity.this, PointsDataPropertiesActivity.class);
                        }
                        intent.putExtra(SpatialiteLibraryConstants.PREFS_KEY_TEXT, item.getName());
                        startActivity(intent);

                    }
                });

                ImageButton zoomtoButton = (ImageButton) rowView.findViewById(R.id.zoomtoButton);
                zoomtoButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        try {
                            float[] tableBounds = SpatialDatabasesManager.getInstance().getVectorHandler(item)
                                    .getTableBounds(item, "4326");
                            double lat = tableBounds[1] + (tableBounds[0] - tableBounds[1]) / 2.0;
                            double lon = tableBounds[3] + (tableBounds[2] - tableBounds[3]) / 2.0;

                            Intent intent = getIntent();
                            intent.putExtra(SpatialiteLibraryConstants.LATITUDE, lat);
                            intent.putExtra(SpatialiteLibraryConstants.LONGITUDE, lon);
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        } catch (jsqlite.Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                // rowView.setBackgroundColor(Color.parseColor(item.getColor()));
                nameView.setText(item.getName());

                visibleView.setChecked(item.getStyle().enabled != 0);
                visibleView.setOnCheckedChangeListener(new OnCheckedChangeListener(){
                    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                        item.getStyle().enabled = isChecked ? 1 : 0;
                    }
                });
                return rowView;
            }

        };
        setListAdapter(arrayAdapter);

    }

    // public boolean onMenuItemSelected( int featureId, MenuItem item ) {
    // SpatialTable selectedTable = null;
    // SpatialTable beforeSelectedTable = null;
    // SpatialTable afterSelectedTable = null;
    // for( int i = 0; i < spatialTables.size(); i++ ) {
    // SpatialTable spatialTable = spatialTables.get(i);
    // if (spatialTable.getStyle().enabled != 0) {
    // // pick the first enabled
    // selectedTable = spatialTable;
    // if (i > 0) {
    // beforeSelectedTable = spatialTables.get(i - 1);
    // }
    // if (i < spatialTables.size() - 1) {
    // afterSelectedTable = spatialTables.get(i + 1);
    // }
    // break;
    // }
    // }
    //
    // switch( item.getItemId() ) {
    // case MOVE_TOP:
    // if (selectedTable != null) {
    // SpatialTable first = spatialTables.get(0);
    // int tmp1 = first.getStyle().order;
    // int tmp2 = selectedTable.getStyle().order;
    // selectedTable.getStyle().order = tmp1;
    // first.getStyle().order = tmp2;
    // Collections.sort(spatialTables, new OrderComparator());
    // refreshList(false);
    // }
    // return true;
    // case MOVE_UP:
    // if (selectedTable != null) {
    // if (beforeSelectedTable != null) {
    // int tmp1 = beforeSelectedTable.getStyle().order;
    // int tmp2 = selectedTable.getStyle().order;
    // selectedTable.getStyle().order = tmp1;
    // beforeSelectedTable.getStyle().order = tmp2;
    // Collections.sort(spatialTables, new OrderComparator());
    // refreshList(false);
    // }
    // }
    // return true;
    // case MOVE_DOWN:
    // if (selectedTable != null) {
    // if (afterSelectedTable != null) {
    // int tmp1 = afterSelectedTable.getStyle().order;
    // int tmp2 = selectedTable.getStyle().order;
    // selectedTable.getStyle().order = tmp1;
    // afterSelectedTable.getStyle().order = tmp2;
    // Collections.sort(spatialTables, new OrderComparator());
    // refreshList(false);
    // }
    // }
    // return true;
    // case MOVE_BOTTOM:
    // if (selectedTable != null) {
    // if (selectedTable != null) {
    // SpatialTable last = spatialTables.get(spatialTables.size() - 1);
    // int tmp1 = last.getStyle().order;
    // int tmp2 = selectedTable.getStyle().order;
    // selectedTable.getStyle().order = tmp1;
    // last.getStyle().order = tmp2;
    // Collections.sort(spatialTables, new OrderComparator());
    // refreshList(false);
    // }
    // }
    // return true;
    // }
    // return super.onMenuItemSelected(featureId, item);
    // }

    @Override
    protected void onPause() {
        try {
            for( int i = 0; i < spatialTables.size(); i++ ) {
                SpatialVectorTable spatialTable = spatialTables.get(i);
                SpatialDatabasesManager.getInstance().updateStyle(spatialTable);
            }
            SpatialDatabasesManager.getInstance().getSpatialVectorTables(true);
        } catch (Exception e) {
            // Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        super.onPause();
    }

}
