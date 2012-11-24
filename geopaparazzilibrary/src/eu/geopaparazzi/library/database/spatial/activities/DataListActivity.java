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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.library.database.spatial.core.OrderComparator;
import eu.geopaparazzi.library.database.spatial.core.SpatialTable;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * Data listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DataListActivity extends ListActivity {
    private static final int MOVE_TOP = 1;
    private static final int MOVE_UP = 2;
    private static final int MOVE_DOWN = 3;
    private static final int MOVE_BOTTOM = 4;

    private List<SpatialTable> spatialTables = new ArrayList<SpatialTable>();;

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
            public View getView( final int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.data_row, null);
                final SpatialTable item = spatialTables.get(position);

                TextView nameView = (TextView) rowView.findViewById(R.id.name);
                CheckBox visibleView = (CheckBox) rowView.findViewById(R.id.visible);
                ImageButton listUpButton = (ImageButton) rowView.findViewById(R.id.upButton);
                listUpButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        if (position > 0) {
                            SpatialTable before = spatialTables.get(position - 1);
                            int tmp1 = before.style.order;
                            int tmp2 = item.style.order;
                            item.style.order = tmp1;
                            before.style.order = tmp2;
                            Collections.sort(spatialTables, new OrderComparator());
                            refreshList(false);
                        }
                    }
                });

                ImageButton listDownButton = (ImageButton) rowView.findViewById(R.id.downButton);
                listDownButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        if (position < spatialTables.size() - 1) {
                            SpatialTable after = spatialTables.get(position + 1);
                            int tmp1 = after.style.order;
                            int tmp2 = item.style.order;
                            item.style.order = tmp1;
                            after.style.order = tmp2;
                            Collections.sort(spatialTables, new OrderComparator());
                            refreshList(false);
                        }
                    }
                });

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

        ListView listView = getListView();
        // Then you can create a listener like so:
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            public boolean onItemLongClick( AdapterView< ? > arg0, View arg1, int pos, long arg3 ) {
                final SpatialTable item = spatialTables.get(pos);

                Utilities.yesNoMessageDialog(DataListActivity.this, "Zoom to the selected layer?", new Runnable(){
                    public void run() {
                        try {
                            float[] tableBounds = SpatialDatabasesManager.getInstance().getHandler(item)
                                    .getTableBounds(item, "4326");
                            double lat = tableBounds[1] + (tableBounds[0] - tableBounds[1]) / 2.0;
                            double lon = tableBounds[3] + (tableBounds[2] - tableBounds[3]) / 2.0;

                            Intent intent = getIntent();
                            intent.putExtra(LibraryConstants.LATITUDE, lat);
                            intent.putExtra(LibraryConstants.LONGITUDE, lon);
                            setResult(Activity.RESULT_OK, intent);
                            finish();

                        } catch (jsqlite.Exception e) {
                            e.printStackTrace();
                        }

                    }
                }, null);

                return true;
            }
        });
    }

    @Override
    protected void onListItemClick( ListView parent, View v, int position, long id ) {
        final SpatialTable spTable = spatialTables.get(position);

        Intent intent = null;
        if (spTable.isLine()) {
            intent = new Intent(this, LinesDataPropertiesActivity.class);
        } else if (spTable.isPolygon()) {
            intent = new Intent(this, PolygonsDataPropertiesActivity.class);
        } else if (spTable.isPoint()) {
            intent = new Intent(this, PointsDataPropertiesActivity.class);
        }
        intent.putExtra(LibraryConstants.PREFS_KEY_TEXT, spTable.name);
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.FIRST, MOVE_BOTTOM, 1, "Move to BOTTOM");
        menu.add(Menu.FIRST, MOVE_DOWN, 2, "Move DOWN");
        menu.add(Menu.FIRST, MOVE_UP, 3, "Move UP");
        menu.add(Menu.FIRST, MOVE_TOP, 4, "Move to TOP");
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
        case MOVE_TOP:
            if (selectedTable != null) {
                SpatialTable first = spatialTables.get(0);
                int tmp1 = first.style.order;
                int tmp2 = selectedTable.style.order;
                selectedTable.style.order = tmp1;
                first.style.order = tmp2;
                Collections.sort(spatialTables, new OrderComparator());
                refreshList(false);
            }
            return true;
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
        case MOVE_BOTTOM:
            if (selectedTable != null) {
                if (selectedTable != null) {
                    SpatialTable last = spatialTables.get(spatialTables.size() - 1);
                    int tmp1 = last.style.order;
                    int tmp2 = selectedTable.style.order;
                    selectedTable.style.order = tmp1;
                    last.style.order = tmp2;
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
            for( int i = 0; i < spatialTables.size(); i++ ) {
                SpatialTable spatialTable = spatialTables.get(i);
                SpatialDatabasesManager.getInstance().updateStyle(spatialTable);
            }
            SpatialDatabasesManager.getInstance().getSpatialTables(true);
        } catch (Exception e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        super.onPause();
    }

}
