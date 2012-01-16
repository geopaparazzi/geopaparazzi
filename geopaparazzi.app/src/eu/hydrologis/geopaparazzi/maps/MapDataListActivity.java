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
package eu.hydrologis.geopaparazzi.maps;

import java.io.IOException;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * Maps listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapDataListActivity extends ListActivity {
    private static final int SELECTALL = 1;
    private static final int UNSELECTALL = 2;

    private MapItem[] mapsItems;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.mapslist);

        refreshList();
    }

    private void refreshList() {
        if (Debug.D) Logger.d(this, "refreshing maps list"); //$NON-NLS-1$
        mapsItems = new MapItem[0];
        try {
            List<MapItem> mapsList = DaoMaps.getMaps(this);
            mapsItems = (MapItem[]) mapsList.toArray(new MapItem[mapsList.size()]);
        } catch (IOException e1) {
            Logger.e(this, e1.getLocalizedMessage(), e1);
            e1.printStackTrace();
        }

        ArrayAdapter<MapItem> arrayAdapter = new ArrayAdapter<MapItem>(this, R.layout.maps_row, mapsItems){

            @Override
            public View getView( int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.maps_row, null);

                TextView nameView = (TextView) rowView.findViewById(R.id.filename);
                CheckBox visibleView = (CheckBox) rowView.findViewById(R.id.visible);

                final MapItem currentItem = mapsItems[position];
                // Logger.d("MAPSDATALISTACTIVITY", currentItem.getName() + "/" +
                // currentItem.isVisible() + "/" + currentItem.getId());
                rowView.setBackgroundColor(Color.parseColor(currentItem.getColor()));
                nameView.setText(currentItem.getName());

                visibleView.setChecked(currentItem.isVisible());
                visibleView.setOnCheckedChangeListener(new OnCheckedChangeListener(){
                    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                        boolean visible = currentItem.isVisible();

                        if (visible != isChecked) {
                            currentItem.setVisible(isChecked);
                            currentItem.setDirty(true);
                            try {
                                DaoMaps.updateMapProperties(MapDataListActivity.this, currentItem.getId(),
                                        currentItem.getColor(), currentItem.getWidth(), currentItem.isVisible(), null);
                            } catch (IOException e) {
                                Logger.e(this, e.getLocalizedMessage(), e);
                                e.printStackTrace();
                            }
                        }

                    }
                });

                return rowView;
            }

        };
        setListAdapter(arrayAdapter);
    }

    protected void onListItemClick( ListView parent, View v, int position, long id ) {
        Intent intent = new Intent(Constants.MAPDATAPROPERTIES);
        intent.putExtra(Constants.PREFS_KEY_MAP4PROPERTIES, mapsItems[position]);
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, SELECTALL, 1, R.string.select_all).setIcon(R.drawable.ic_menu_select);
        menu.add(Menu.NONE, UNSELECTALL, 2, R.string.unselect_all).setIcon(R.drawable.ic_menu_unselect);
        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case SELECTALL:
            try {
                DaoMaps.setMapsVisibility(this, true);
                refreshList();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        case UNSELECTALL:
            try {
                DaoMaps.setMapsVisibility(this, false);
                refreshList();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    protected void onPause() {
        boolean oneVisible = false;
        for( MapItem item : mapsItems ) {
            if (!oneVisible && item.isVisible()) {
                oneVisible = true;
            }
        }
        DataManager.getInstance().setMapsVisible(oneVisible);
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        refreshList();
    }

}
