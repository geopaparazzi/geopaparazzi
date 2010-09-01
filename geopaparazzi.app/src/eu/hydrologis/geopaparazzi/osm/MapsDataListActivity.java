/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.geopaparazzi.osm;

import java.io.IOException;
import java.util.Collection;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * Maps listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsDataListActivity extends ListActivity {
    private MapItem[] mapsItems;
    private ArrayAdapter<MapItem> arrayAdapter;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.mapslist);

        Collection<MapItem> logsList = null;
        try {
            logsList = DaoMaps.getMaps();
            mapsItems = (MapItem[]) logsList.toArray(new MapItem[logsList.size()]);
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }

        arrayAdapter = new ArrayAdapter<MapItem>(this, R.layout.maps_row, mapsItems){

            @Override
            public View getView( int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.maps_row, null);

                TextView nameView = (TextView) rowView.findViewById(R.id.filename);
                CheckBox visibleView = (CheckBox) rowView.findViewById(R.id.visible);

                final MapItem currentItem = mapsItems[position];
                // Log.d("MAPSDATALISTACTIVITY", currentItem.getName() + "/" +
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
                                DaoMaps.updateMapProperties(currentItem.getId(), currentItem.getColor(), currentItem.getWidth(),
                                        currentItem.isVisible(), null);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                });

                return rowView;
            }

        };
        setListAdapter(arrayAdapter);
        // getListView().setTextFilterEnabled(true);
    }

    @Override
    protected void onListItemClick( ListView parent, View v, int position, long id ) {
        Intent intent = new Intent(Constants.MAPDATAPROPERTIES);
        intent.putExtra(Constants.PREFS_KEY_MAP4PROPERTIES, mapsItems[position]);
        startActivity(intent);
    }

    // @Override
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

    // @Override
    // protected void onResume() {
    // arrayAdapter.notifyDataSetChanged();
    // arrayAdapter.notifyDataSetInvalidated();
    // getListView().invalidateViews();
    //
    // super.onResume();
    // }

}
