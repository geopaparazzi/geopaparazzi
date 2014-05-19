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
package eu.geopaparazzi.mapsforge.mapsdirmanager.sourcesview;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ToggleButton;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.mapsforge.R;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.spatialite.util.SpatialDataType;

/**
 * Activity for tile source visualization.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SourcesTreeListActivity extends Activity implements OnClickListener {

    SourcesExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    private ToggleButton mapToggleButton;
    private ToggleButton mapurlToggleButton;
    private ToggleButton mbtilesToggleButton;
    private boolean showMaps = true;
    private boolean showMapurls = true;
    private boolean showMbtiles = true;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sources_list);

        mapToggleButton = (ToggleButton) findViewById(R.id.toggleMapButton);
        mapToggleButton.setChecked(true);
        mapToggleButton.setOnClickListener(this);
        mapToggleButton.setTextOn(SpatialDataType.MAP.getTypeName());
        mapToggleButton.setTextOff(SpatialDataType.MAP.getTypeName());
        mapurlToggleButton = (ToggleButton) findViewById(R.id.toggleMapurlButton);
        mapurlToggleButton.setChecked(true);
        mapurlToggleButton.setOnClickListener(this);
        mapurlToggleButton.setTextOn(SpatialDataType.MAPURL.getTypeName());
        mapurlToggleButton.setTextOff(SpatialDataType.MAPURL.getTypeName());
        mbtilesToggleButton = (ToggleButton) findViewById(R.id.toggleMbtilesButton);
        mbtilesToggleButton.setChecked(true);
        mbtilesToggleButton.setOnClickListener(this);
        mbtilesToggleButton.setTextOn(SpatialDataType.MBTILES.getTypeName());
        mbtilesToggleButton.setTextOff(SpatialDataType.MBTILES.getTypeName());

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expandableSourceListView);

        try {
            getData();
        } catch (Exception e) {
            GPLog.error(this, "Problem getting sources.", e); //$NON-NLS-1$
        }
    }

    private void getData() throws Exception {
        LinkedHashMap<String, List<String[]>> fodler2TablesMap = MapsDirManager.getInstance().getFolder2TablesMap();
        final LinkedHashMap<String, List<String[]>> newMap = new LinkedHashMap<String, List<String[]>>();
        for( Entry<String, List<String[]>> item : fodler2TablesMap.entrySet() ) {
            String key = item.getKey();
            ArrayList<String[]> newValues = new ArrayList<String[]>();
            newMap.put(key, newValues);

            List<String[]> values = item.getValue();
            for( String[] value : values ) {
                if (showMaps && value[1].equals(SpatialDataType.MAP.getTypeName())) {
                    newValues.add(value);
                } else if (showMapurls && value[1].equals(SpatialDataType.MAPURL.getTypeName())) {
                    newValues.add(value);
                } else if (showMbtiles && value[1].equals(SpatialDataType.MBTILES.getTypeName())) {
                    newValues.add(value);
                }
            }

        }

        listAdapter = new SourcesExpandableListAdapter(this, newMap);
        expListView.setAdapter(listAdapter);
        expListView.setClickable(true);
        expListView.setFocusable(true);
        expListView.setFocusableInTouchMode(true);
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener(){
            public boolean onChildClick( ExpandableListView parent, View v, int groupPosition, int childPosition, long id ) {
                int index = 0;
                String[] spatialTableDate = null;
                for( Entry<String, List<String[]>> entry : newMap.entrySet() ) {
                    if (groupPosition == index) {
                        List<String[]> value = entry.getValue();
                        spatialTableDate = value.get(childPosition);
                        break;
                    }
                    index++;
                }
                MapsDirManager.getInstance().setSelectedSpatialTable(SourcesTreeListActivity.this, spatialTableDate);
                finish();
                return false;
            }
        });
        int groupCount = listAdapter.getGroupCount();
        for( int i = 0; i < groupCount; i++ ) {
            expListView.expandGroup(i);
        }
    }
    @Override
    public void onClick( View view ) {
        showMaps = mapToggleButton.isChecked();
        showMapurls = mapurlToggleButton.isChecked();
        showMbtiles = mbtilesToggleButton.isChecked();
        try {
            getData();
        } catch (Exception e) {
            GPLog.error(this, "Error getting source data.", e); //$NON-NLS-1$
        }
    }
}