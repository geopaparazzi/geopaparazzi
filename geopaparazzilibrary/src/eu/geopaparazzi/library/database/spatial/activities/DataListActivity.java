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
package eu.geopaparazzi.library.database.spatial.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.library.database.spatial.core.OrderComparator;
import eu.geopaparazzi.library.database.spatial.core.SpatialTable;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * Data listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DataListActivity extends ListActivity {
    private static final int MOVE_UP = 1;
    private static final int MOVE_DOWN = 2;

    private static final int DATAPROPERTIES_RETURN_CODE = 668;

    private List<SpatialTable> spatialTables = new ArrayList<SpatialTable>();;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.data_list);

        refreshList(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList(true);
    }

    private void refreshList( boolean doReread ) {
        if (Debug.D)
            Logger.d(this, "refreshing data list"); //$NON-NLS-1$

        try {
            if (doReread)
                spatialTables = SpatialDatabasesManager.getInstance().getSpatialTables(doReread);
        } catch (Exception e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        ArrayAdapter<SpatialTable> arrayAdapter = new ArrayAdapter<SpatialTable>(this, R.layout.data_row, spatialTables){
            @Override
            public View getView( int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.data_row, null);

                TextView nameView = (TextView) rowView.findViewById(R.id.name);
                CheckBox visibleView = (CheckBox) rowView.findViewById(R.id.visible);

                final SpatialTable item = spatialTables.get(position);
                // rowView.setBackgroundColor(Color.parseColor(item.getColor()));
                nameView.setText(item.name);

                visibleView.setChecked(item.style.enabled != 0);
                visibleView.setOnCheckedChangeListener(new OnCheckedChangeListener(){
                    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                        item.style.enabled = isChecked ? 1 : 0;
                    }
                });
                return rowView;
            }

        };
        setListAdapter(arrayAdapter);
    }

    @Override
    protected void onListItemClick( ListView parent, View v, int position, long id ) {
        // Intent intent = new Intent(this, DataPropertiesActivity.class);
        // intent.putExtra(Constants.PREFS_KEY_GPSLOG4PROPERTIES, gpslogItems[position]);
        // startActivityForResult(intent, GPSDATAPROPERTIES_RETURN_CODE);
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if (Debug.D) {
            Logger.d(this, "Activity returned"); //$NON-NLS-1$
        }
        super.onActivityResult(requestCode, resultCode, data);
        switch( requestCode ) {
        case (DATAPROPERTIES_RETURN_CODE): {
            if (resultCode == Activity.RESULT_OK) {
                double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0d);
                double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0d);
                Intent intent = getIntent();
                intent.putExtra(LibraryConstants.LATITUDE, lat);
                intent.putExtra(LibraryConstants.LONGITUDE, lon);
                setResult(Activity.RESULT_OK, intent);
            }
            break;
        }
        }
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.FIRST, MOVE_UP, 1, "Move UP").setIcon(android.R.drawable.ic_menu_add);
        menu.add(Menu.FIRST, MOVE_DOWN, 2, "Move DOWN").setIcon(android.R.drawable.ic_menu_delete);
        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        SpatialTable selectedTable = null;
        SpatialTable beforeSelectedTable = null;
        SpatialTable afterSelectedTable = null;
        for( int i = 0; i < spatialTables.size(); i++ ) {
            SpatialTable spatialTable = spatialTables.get(i);
            if (spatialTable.style.enabled != 0) {
                // pick the first enabled
                selectedTable = spatialTable;
                if (i > 0) {
                    beforeSelectedTable = spatialTables.get(i - 1);
                }
                if (i < spatialTables.size() - 1) {
                    afterSelectedTable = spatialTables.get(i + 1);
                }
                break;
            }
        }

        switch( item.getItemId() ) {
        case MOVE_UP:
            if (selectedTable != null) {
                if (beforeSelectedTable != null) {
                    int tmp1 = beforeSelectedTable.style.order;
                    int tmp2 = selectedTable.style.order;
                    selectedTable.style.order = tmp1;
                    beforeSelectedTable.style.order = tmp2;
                    Collections.sort(spatialTables, new OrderComparator());
                    refreshList(false);
                }
            }
            return true;
        case MOVE_DOWN:
            if (selectedTable != null) {
                if (afterSelectedTable != null) {
                    int tmp1 = afterSelectedTable.style.order;
                    int tmp2 = selectedTable.style.order;
                    selectedTable.style.order = tmp1;
                    afterSelectedTable.style.order = tmp2;
                    Collections.sort(spatialTables, new OrderComparator());
                    refreshList(false);
                }
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onPause() {
        try {
            SpatialDatabasesManager.getInstance().updateStyles();
        } catch (Exception e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        super.onPause();
    }

}
