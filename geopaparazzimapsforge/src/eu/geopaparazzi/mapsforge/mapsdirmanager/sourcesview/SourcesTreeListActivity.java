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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.mapsforge.R;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.SPL_Rasterlite;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;

/**
 * Activity for tile source visualisation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SourcesTreeListActivity extends Activity implements OnClickListener {

    SourcesExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    private Button mapToggleButton;
    private Button mapurlToggleButton;
    private Button mbtilesToggleButton;
    private Button rasterLite2ToggleButton;
    private boolean showMaps = true;
    private boolean showMapurls = true;
    private boolean showMbtiles = true;
    private boolean showRasterLite2 = true;
    private EditText filterText;
    private String textToFilter = "";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sources_list);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        filterText = (EditText) findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);

        mapToggleButton = (Button) findViewById(R.id.toggleMapButton);
        mapToggleButton.setOnClickListener(this);
        mapToggleButton.setText(SpatialDataType.MAP.getTypeName());
        mapToggleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable_selected));

        mapurlToggleButton = (Button) findViewById(R.id.toggleMapurlButton);
        mapurlToggleButton.setOnClickListener(this);
        mapurlToggleButton.setText(SpatialDataType.MAPURL.getTypeName());
        mapurlToggleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable_selected));

        mbtilesToggleButton = (Button) findViewById(R.id.toggleMbtilesButton);
        mbtilesToggleButton.setOnClickListener(this);
        mbtilesToggleButton.setText(SpatialDataType.MBTILES.getTypeName());
        mbtilesToggleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable_selected));

        rasterLite2ToggleButton = (Button) findViewById(R.id.toggleRasterLite2Button);
        if (!SPL_Rasterlite.Rasterlite2Version_CPU.equals("")) { //$NON-NLS-1$
            // show this only if the driver is installed and active
            rasterLite2ToggleButton.setOnClickListener(this);
            rasterLite2ToggleButton.setText(SpatialDataType.RASTERLITE2.getTypeName());
            rasterLite2ToggleButton.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.button_background_drawable_selected));
        } else {
            // hide R.id.toggleRasterLite2Button ?
            rasterLite2ToggleButton.setVisibility(View.GONE);
            showRasterLite2 = false;
        }

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expandableSourceListView);

        try {
            refreshData();
        } catch (Exception e) {
            GPLog.error(this, "Problem getting sources.", e); //$NON-NLS-1$
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        filterText.removeTextChangedListener(filterTextWatcher);
    }

    private void refreshData() throws Exception {
        LinkedHashMap<String, List<String[]>> folder2TablesMap = MapsDirManager.getInstance().getFolder2TablesMap();
        final LinkedHashMap<String, List<String[]>> newMap = new LinkedHashMap<String, List<String[]>>();
        for( Entry<String, List<String[]>> item : folder2TablesMap.entrySet() ) {
            String key = item.getKey();
            ArrayList<String[]> newValues = new ArrayList<String[]>();

            List<String[]> values = item.getValue();
            for( String[] value : values ) {
                boolean doAdd = false;
                if (showMaps && value[1].equals(SpatialDataType.MAP.getTypeName())) {
                    doAdd = true;
                } else if (showMapurls && value[1].equals(SpatialDataType.MAPURL.getTypeName())) {
                    doAdd = true;
                } else if (showMbtiles && value[1].equals(SpatialDataType.MBTILES.getTypeName())) {
                    doAdd = true;
                } else if (showRasterLite2 && value[1].equals(SpatialDataType.RASTERLITE2.getTypeName())) {
                    doAdd = true;
                }

                if (textToFilter.length() > 0) {
                    // filter text
                    String filterString = textToFilter.toLowerCase();
                    String valueString = value[0].toLowerCase();
                    if (!valueString.contains(filterString)) {
                        valueString = value[2].toLowerCase();
                        if (!valueString.contains(filterString)) {
                            doAdd = false;
                        }
                    }
                }
                if (doAdd)
                    newValues.add(value);
            }

            if (newValues.size() > 0) {
                newMap.put(key, newValues);
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
                try {
                    MapsDirManager.getInstance().setSelectedSpatialTable(SourcesTreeListActivity.this, spatialTableDate);
                } catch (jsqlite.Exception e) {
                    GPLog.error(SourcesTreeListActivity.this, "ERROR", e);
                }
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
        if (view == mapToggleButton) {
            if (!showMaps) {
                mapToggleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable_selected));
            } else {
                mapToggleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable));
            }
            showMaps = !showMaps;
        }
        if (view == mapurlToggleButton) {
            if (!showMapurls) {
                mapurlToggleButton.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.button_background_drawable_selected));
            } else {
                mapurlToggleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable));
            }
            showMapurls = !showMapurls;
        }
        if (view == mbtilesToggleButton) {
            if (!showMbtiles) {
                mbtilesToggleButton.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.button_background_drawable_selected));
            } else {
                mbtilesToggleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable));
            }
            showMbtiles = !showMbtiles;
        }

        if (!SPL_Rasterlite.Rasterlite2Version_CPU.equals(""))
            if (view == rasterLite2ToggleButton) {
                if (!showRasterLite2) {
                    rasterLite2ToggleButton.setBackgroundDrawable(getResources().getDrawable(
                            R.drawable.button_background_drawable_selected));
                } else {
                    rasterLite2ToggleButton.setBackgroundDrawable(getResources().getDrawable(
                            R.drawable.button_background_drawable));
                }
                showRasterLite2 = !showRasterLite2;
            }
        try {
            refreshData();
        } catch (Exception e) {
            GPLog.error(this, "Error getting source data.", e); //$NON-NLS-1$
        }
    }

    private TextWatcher filterTextWatcher = new TextWatcher(){

        public void afterTextChanged( Editable s ) {
            // ignore
        }

        public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            // ignore
        }

        public void onTextChanged( CharSequence s, int start, int before, int count ) {
            textToFilter = s.toString();
            try {
                refreshData();
            } catch (Exception e) {
                GPLog.error(SourcesTreeListActivity.this, "ERROR", e);
            }
        }
    };
}
