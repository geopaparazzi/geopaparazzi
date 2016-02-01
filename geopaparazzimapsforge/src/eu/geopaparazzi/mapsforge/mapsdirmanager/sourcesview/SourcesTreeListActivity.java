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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ExpandableListView;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.maps.BaseMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.mapsforge.R;
import eu.geopaparazzi.mapsforge.mapsdirmanager.BaseMapSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.SPL_Rasterlite;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;

/**
 * Activity for tile source visualisation.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SourcesTreeListActivity extends AppCompatActivity implements OnClickListener {
    public static final int PICKFILE_REQUEST_CODE = 666;

    public static final String SHOW_MAPS = "showMaps";
    public static final String SHOW_MAPURLS = "showMapurls";
    public static final String SHOW_MBTILES = "showMbtiles";
    public static final String SHOW_RASTER_LITE_2 = "showRasterLite2";
    SourcesExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    private boolean showMaps = true;
    private boolean showMapurls = true;
    private boolean showMbtiles = true;
    private boolean showRasterLite2 = true;
    private EditText filterText;
    private String textToFilter = "";
    private SharedPreferences preferences;
    private boolean[] checkedValues;
    private boolean hasRL2;
    private List<String> typeNames;
    private List<BaseMap> baseMaps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sources_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
        typeNames = new ArrayList<>();
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

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expandableSourceListView);

        try {
            refreshData();
        } catch (Exception e) {
            GPLog.error(this, "Problem getting sources.", e); //$NON-NLS-1$
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        filterText.removeTextChangedListener(filterTextWatcher);
    }

    public void add(View view) {
        try {
            String title = "Select basemap source to add";
            String mimeType = "*/*";
            Uri uri = Uri.parse(ResourcesManager.getInstance(this).getMapsDir().getAbsolutePath());
            AppsUtilities.pickFile(this, PICKFILE_REQUEST_CODE, title, mimeType, uri);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(this, e, null);
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (PICKFILE_REQUEST_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String filePath = data.getDataString();
                        File file = new File(new URL(filePath).toURI());
                        if (file.exists()) {
                            // add basemap to list and in preferences
                            BaseMapSourcesManager.getInstance().addBaseMapFromFile(file);
                            refreshData();
                        }
                    } catch (Exception e) {
                        GPDialogs.errorDialog(this, e, null);
                    }
                }
                break;
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sources, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.select_type_item) {
            MapTypesChoiceDialog dialog = new MapTypesChoiceDialog();
            dialog.open(getString(R.string.select_type), SourcesTreeListActivity.this, typeNames, checkedValues);
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshData() throws Exception {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_MAPS, checkedValues[0]);
        editor.putBoolean(SHOW_MAPURLS, checkedValues[1]);
        editor.putBoolean(SHOW_MBTILES, checkedValues[2]);
        if (hasRL2)
            editor.putBoolean(SHOW_RASTER_LITE_2, checkedValues[3]);
        editor.apply();


        baseMaps = BaseMapSourcesManager.getInstance().getBaseMaps();
        final LinkedHashMap<String, List<BaseMap>> newMap = new LinkedHashMap<>();
        for (BaseMap baseMap : baseMaps) {
            String key = baseMap.parentFolder;
            List<BaseMap> newValues = newMap.get(key);
            if (newValues == null) {
                newValues = new ArrayList<>();
                newMap.put(key, newValues);
            }

            boolean doAdd = false;
            String mapType = baseMap.mapType;
            if (checkedValues[0] && mapType.equals(SpatialDataType.MAP.getTypeName())) {
                doAdd = true;
            } else if (checkedValues[1] && mapType.equals(SpatialDataType.MAPURL.getTypeName())) {
                doAdd = true;
            } else if (checkedValues[2] && mapType.equals(SpatialDataType.MBTILES.getTypeName())) {
                doAdd = true;
            } else if (hasRL2 && checkedValues[3] && mapType.equals(SpatialDataType.RASTERLITE2.getTypeName())) {
                doAdd = true;
            }

            if (textToFilter.length() > 0) {
                // filter text
                String filterString = textToFilter.toLowerCase();
                String valueString = baseMap.databasePath.toLowerCase();
                if (!valueString.contains(filterString)) {
                    valueString = baseMap.title.toLowerCase();
                    if (!valueString.contains(filterString)) {
                        doAdd = false;
                    }
                }
            }
            if (doAdd)
                newValues.add(baseMap);

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
                BaseMap selectedBaseMap = null;
                for (Entry<String, List<BaseMap>> entry : newMap.entrySet()) {
                    if (groupPosition == index) {
                        List<BaseMap> value = entry.getValue();
                        selectedBaseMap = value.get(childPosition);
                        break;
                    }
                    index++;
                }
                try {
                    BaseMapSourcesManager.getInstance().setSelectedBaseMap(selectedBaseMap);
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
