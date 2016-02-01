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
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ExpandableListView;

import org.json.JSONException;

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
public class SourcesTreeListActivity extends AppCompatActivity {
    public static final int PICKFILE_REQUEST_CODE = 666;

    public static final String SHOW_MAPS = "showMaps";
    public static final String SHOW_MAPURLS = "showMapurls";
    public static final String SHOW_MBTILES = "showMbtiles";
    public static final String SHOW_RASTER_LITE_2 = "showRasterLite2";

    private ExpandableListView mExpListView;
    private EditText mFilterText;
    private String mTextToFilter = "";
    private SharedPreferences mPreferences;
    private boolean[] mCheckedValues;
    private boolean mHasRL2;
    private List<String> mTypeNames;
    private final LinkedHashMap<String, List<BaseMap>> newMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sources_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mFilterText = (EditText) findViewById(R.id.search_box);
        mFilterText.addTextChangedListener(filterTextWatcher);

        mHasRL2 = SPL_Rasterlite.hasRasterLiteSupport();

        boolean showMaps = mPreferences.getBoolean(SHOW_MAPS, true);
        boolean showMapurls = mPreferences.getBoolean(SHOW_MAPURLS, true);
        boolean showMbtiles = mPreferences.getBoolean(SHOW_MBTILES, true);
        boolean showRasterLite2 = mPreferences.getBoolean(SHOW_RASTER_LITE_2, true);

        String mapTypeName = SpatialDataType.MAP.getTypeName();
        String mapurlTypeName = SpatialDataType.MAPURL.getTypeName();
        String mbtilesTypeName = SpatialDataType.MBTILES.getTypeName();
        mTypeNames = new ArrayList<>();
        mTypeNames.add(mapTypeName);
        mTypeNames.add(mapurlTypeName);
        mTypeNames.add(mbtilesTypeName);
        if (mHasRL2) {
            String rasterLiteTypeName = SpatialDataType.RASTERLITE2.getTypeName();
            mTypeNames.add(rasterLiteTypeName);
        } else {
            showRasterLite2 = false;
        }
        mCheckedValues = new boolean[mTypeNames.size()];
        mCheckedValues[0] = showMaps;
        mCheckedValues[1] = showMapurls;
        mCheckedValues[2] = showMbtiles;
        if (mHasRL2)
            mCheckedValues[3] = showRasterLite2;

        // get the listview
        mExpListView = (ExpandableListView) findViewById(R.id.expandableSourceListView);

        try {
            refreshData();
        } catch (Exception e) {
            GPLog.error(this, "Problem getting sources.", e); //$NON-NLS-1$
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFilterText.removeTextChangedListener(filterTextWatcher);
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
                            // add basemap to list and in mPreferences
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
            dialog.open(getString(R.string.select_type), SourcesTreeListActivity.this, mTypeNames, mCheckedValues);
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshData() throws Exception {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(SHOW_MAPS, mCheckedValues[0]);
        editor.putBoolean(SHOW_MAPURLS, mCheckedValues[1]);
        editor.putBoolean(SHOW_MBTILES, mCheckedValues[2]);
        if (mHasRL2)
            editor.putBoolean(SHOW_RASTER_LITE_2, mCheckedValues[3]);
        editor.apply();

        newMap.clear();
        List<BaseMap> baseMaps = BaseMapSourcesManager.getInstance().getBaseMaps();
        for (BaseMap baseMap : baseMaps) {
            String key = baseMap.parentFolder;
            List<BaseMap> newValues = newMap.get(key);
            if (newValues == null) {
                newValues = new ArrayList<>();
                newMap.put(key, newValues);
            }

            boolean doAdd = false;
            String mapType = baseMap.mapType;
            if (mCheckedValues[0] && mapType.equals(SpatialDataType.MAP.getTypeName())) {
                doAdd = true;
            } else if (mCheckedValues[1] && mapType.equals(SpatialDataType.MAPURL.getTypeName())) {
                doAdd = true;
            } else if (mCheckedValues[2] && mapType.equals(SpatialDataType.MBTILES.getTypeName())) {
                doAdd = true;
            } else if (mHasRL2 && mCheckedValues[3] && mapType.equals(SpatialDataType.RASTERLITE2.getTypeName())) {
                doAdd = true;
            }

            if (mTextToFilter.length() > 0) {
                // filter text
                String filterString = mTextToFilter.toLowerCase();
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

        SourcesExpandableListAdapter listAdapter = new SourcesExpandableListAdapter(this, newMap);
        mExpListView.setAdapter(listAdapter);
        mExpListView.setClickable(true);
        mExpListView.setFocusable(true);
        mExpListView.setFocusableInTouchMode(true);
        mExpListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
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

        mExpListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // When clicked on child, function longClick is executed
                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                    int childPosition = ExpandableListView.getPackedPositionChild(id);

                    int index = 0;
                    for (String group : newMap.keySet()) {
                        if (index == groupPosition) {
                            List<BaseMap> baseMapList = newMap.get(group);
                            final BaseMap baseMap = baseMapList.get(childPosition);

                            GPDialogs.yesNoMessageDialog(SourcesTreeListActivity.this, String.format(getString(R.string.remove_from_list), baseMap.title), new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        BaseMapSourcesManager.getInstance().removeBaseMap(baseMap);
                                    } catch (JSONException e) {
                                        GPLog.error(this, null, e);
                                    }

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                refreshData();
                                            } catch (Exception e) {
                                                GPLog.error(this, null, e);
                                            }
                                        }
                                    });

                                }
                            }, null);

                            return true;
                        }
                        index++;
                    }
                    return true;
                }
                return false;
            }
        });

        int groupCount = listAdapter.getGroupCount();
        for (int i = 0; i < groupCount; i++) {
            mExpListView.expandGroup(i);
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
            mTextToFilter = s.toString();
            try {
                refreshData();
            } catch (Exception e) {
                GPLog.error(SourcesTreeListActivity.this, "ERROR", e);
            }
        }
    };

}
