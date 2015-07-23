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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Switch;
import android.widget.ToggleButton;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.MultipleChoiceDialog;
import eu.geopaparazzi.mapsforge.R;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.SPL_Rasterlite;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;

/**
 * Activity for tile source visualisation.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SourcesTreeListActivity extends Activity implements OnClickListener {

    public static final String SHOW_MAPS = "showMaps";
    public static final String SHOW_MAPURLS = "showMapurls";
    public static final String SHOW_MBTILES = "showMbtiles";
    public static final String SHOW_RASTER_LITE_2 = "showRasterLite2";
    SourcesExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    private Button mapTypeToggleButton;
    private boolean showMaps = true;
    private boolean showMapurls = true;
    private boolean showMbtiles = true;
    private boolean showRasterLite2 = true;
    private EditText filterText;
    private String textToFilter = "";
    private SharedPreferences preferences;
    private boolean[] checkedValues;
    private boolean hasRL2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sources_list);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);


        filterText = (EditText) findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);

        hasRL2 = SPL_Rasterlite.hasRasterLiteSupport();

        showMaps = preferences.getBoolean(SHOW_MAPS, true);
        showMapurls = preferences.getBoolean(SHOW_MAPURLS, true);
        showMbtiles = preferences.getBoolean(SHOW_MBTILES, true);
        showRasterLite2 = preferences.getBoolean(SHOW_RASTER_LITE_2, true);

        String mapTypeName = SpatialDataType.MAP.getTypeName();
        String mapurlTypeName = SpatialDataType.MAPURL.getTypeName();
        String mbtilesTypeName = SpatialDataType.MBTILES.getTypeName();
        final List<String> typeNames = new ArrayList<String>();
        typeNames.add(mapTypeName);
        typeNames.add(mapurlTypeName);
        typeNames.add(mbtilesTypeName);
        if (hasRL2) {
            String rasterLiteTypeName = SpatialDataType.RASTERLITE2.getTypeName();
            typeNames.add(rasterLiteTypeName);
        } else {
            showRasterLite2 = false;
        }
        checkedValues = new boolean[typeNames.size()];
        checkedValues[0] = showMaps;
        checkedValues[1] = showMapurls;
        checkedValues[2] = showMbtiles;
        if (hasRL2)
            checkedValues[3] = showRasterLite2;

        mapTypeToggleButton = (Button) findViewById(R.id.toggleTypesButton);
        mapTypeToggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MapTypesChoiceDialog dialog = new MapTypesChoiceDialog();
                dialog.open(mapTypeToggleButton.getText().toString(), SourcesTreeListActivity.this, typeNames, checkedValues);
            }
        });

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

    public void refreshData() throws Exception {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_MAPS, checkedValues[0]);
        editor.putBoolean(SHOW_MAPURLS, checkedValues[1]);
        editor.putBoolean(SHOW_MBTILES, checkedValues[2]);
        if (hasRL2)
            editor.putBoolean(SHOW_RASTER_LITE_2, checkedValues[3]);
        editor.apply();

        LinkedHashMap<String, List<String[]>> folder2TablesMap = MapsDirManager.getInstance().getFolder2TablesMap();
        final LinkedHashMap<String, List<String[]>> newMap = new LinkedHashMap<String, List<String[]>>();
        for (Entry<String, List<String[]>> item : folder2TablesMap.entrySet()) {
            String key = item.getKey();
            ArrayList<String[]> newValues = new ArrayList<String[]>();

            List<String[]> values = item.getValue();
            for (String[] value : values) {
                boolean doAdd = false;
                if (checkedValues[0] && value[1].equals(SpatialDataType.MAP.getTypeName())) {
                    doAdd = true;
                } else if (checkedValues[1] && value[1].equals(SpatialDataType.MAPURL.getTypeName())) {
                    doAdd = true;
                } else if (checkedValues[2] && value[1].equals(SpatialDataType.MBTILES.getTypeName())) {
                    doAdd = true;
                } else if (hasRL2 && checkedValues[3] && value[1].equals(SpatialDataType.RASTERLITE2.getTypeName())) {
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
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                int index = 0;
                String[] spatialTableDate = null;
                for (Entry<String, List<String[]>> entry : newMap.entrySet()) {
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
        for (int i = 0; i < groupCount; i++) {
            expListView.expandGroup(i);
        }
    }

    private TextWatcher filterTextWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
            // ignore
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // ignore
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            textToFilter = s.toString();
            try {
                refreshData();
            } catch (Exception e) {
                GPLog.error(SourcesTreeListActivity.this, "ERROR", e);
            }
        }
    };


    @Override
    public void onClick(View view) {

    }
}
