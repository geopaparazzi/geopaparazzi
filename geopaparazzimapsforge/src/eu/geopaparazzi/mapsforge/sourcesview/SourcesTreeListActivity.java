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
package eu.geopaparazzi.mapsforge.sourcesview;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.RelativeLayout;

import org.json.JSONException;

import eu.geopaparazzi.library.core.maps.BaseMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.R;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import eu.geopaparazzi.library.util.types.ESpatialDataSources;

/**
 * Activity for tile source visualisation.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SourcesTreeListActivity extends AppCompatActivity implements IActivitySupporter {
    public static final int PICKFILE_REQUEST_CODE = 666;
    public static final int PICKFOLDER_REQUEST_CODE = 667;

    public static final String SHOW_MAPS = "showMaps";
    public static final String SHOW_MAPURLS = "showMapurls";
    public static final String SHOW_MBTILES = "showMbtiles";
    public static final String SHOW_RASTER_LITE_2 = "showRasterLite2";

    private ExpandableListView mExpListView;
    private EditText mFilterText;
    private String mTextToFilter = "";
    private SharedPreferences mPreferences;
    private boolean[] mCheckedValues;
    private List<String> mTypeNames;
    private final LinkedHashMap<String, List<BaseMap>> newMap = new LinkedHashMap<>();
    private StringAsyncTask loadTask;
    private StringAsyncTask addNewSourcesTask;

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

        boolean showMaps = mPreferences.getBoolean(SHOW_MAPS, true);
        boolean showMapurls = mPreferences.getBoolean(SHOW_MAPURLS, true);
        boolean showMbtiles = mPreferences.getBoolean(SHOW_MBTILES, true);
        boolean showRasterLite2 = mPreferences.getBoolean(SHOW_RASTER_LITE_2, true);

        String mapTypeName = ESpatialDataSources.MAP.getTypeName();
        String mapurlTypeName = ESpatialDataSources.MAPURL.getTypeName();
        String mbtilesTypeName = ESpatialDataSources.MBTILES.getTypeName();
        String rasterLiteTypeName = ESpatialDataSources.RASTERLITE2.getTypeName();
        mTypeNames = new ArrayList<>();
        mTypeNames.add(mapTypeName);
        mTypeNames.add(mapurlTypeName);
        mTypeNames.add(mbtilesTypeName);
        mTypeNames.add(rasterLiteTypeName);
        mCheckedValues = new boolean[mTypeNames.size()];
        mCheckedValues[0] = showMaps;
        mCheckedValues[1] = showMapurls;
        mCheckedValues[2] = showMbtiles;
        mCheckedValues[3] = showRasterLite2;

        // get the listview
        mExpListView = (ExpandableListView) findViewById(R.id.expandableSourceListView);

        if (ProfilesHandler.INSTANCE.getActiveProfile() != null) {
            RelativeLayout mainView = (RelativeLayout) findViewById(R.id.sources_list_mainview);
            int color = ColorUtilities.toColor(ProfilesHandler.INSTANCE.getActiveProfile().color);
            mainView.setBackgroundColor(color);

            FloatingActionButton addSourceButton = (FloatingActionButton) findViewById(R.id.addSourceButton);
            addSourceButton.hide();
        }

        loadTask = new StringAsyncTask(this) {
            List<BaseMap> baseMaps;

            protected String doBackgroundWork() {
                baseMaps = BaseMapSourcesManager.INSTANCE.getBaseMaps();
                return "";
            }

            protected void doUiPostWork(String response) {
                dispose();
                try {
                    refreshData(baseMaps);
                } catch (Exception e) {
                    GPLog.error(this, "Problem getting sources.", e);
                }
            }
        };
        loadTask.setProgressDialog("", getString(R.string.loading_sources), false, null);
        loadTask.execute();

    }

    @Override
    protected void onStart() {
        super.onStart();


    }

    @Override
    protected void onDestroy() {
        if (loadTask != null) loadTask.dispose();
        if (addNewSourcesTask != null) addNewSourcesTask.dispose();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        mFilterText.removeTextChangedListener(filterTextWatcher);
        super.onStop();
    }

    public void add(View view) {
        try {
            String title = getString(R.string.select_basemap_source);
            String[] supportedExtensions = ESpatialDataSources.getSupportedTileSourcesExtensions();
            AppsUtilities.pickFile(this, PICKFILE_REQUEST_CODE, title, supportedExtensions, null);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(this, e, null);
        }
    }

    public void addFolder(View view) {
        try {
            String title = getString(R.string.select_basemap_source_folder);
            String[] supportedExtensions = ESpatialDataSources.getSupportedTileSourcesExtensions();
            AppsUtilities.pickFolder(this, PICKFOLDER_REQUEST_CODE, title, null, supportedExtensions);
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
                        String filePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                        File file = new File(filePath);
                        if (file.exists()) {
                            Utilities.setLastFilePath(this, filePath);
                            final File finalFile = file;
                            // add basemap to list and in mPreferences
                            addNewSourcesTask = new StringAsyncTask(this) {
                                public List<BaseMap> baseMaps;

                                protected String doBackgroundWork() {
                                    try {
                                        // add basemap to list and in mPreferences
                                        if (BaseMapSourcesManager.INSTANCE.addBaseMapsFromFile(finalFile).size() != 0) {
                                            baseMaps = BaseMapSourcesManager.INSTANCE.getBaseMaps();
                                        } else {
                                            return getString(R.string.selected_file_no_basemap) + finalFile;
                                        }
                                    } catch (Exception e) {
                                        GPLog.error(this, "Problem getting sources.", e);
                                        return "ERROR: " + e.getLocalizedMessage();
                                    }
                                    return "";
                                }

                                protected void doUiPostWork(String response) {
                                    dispose();
                                    if (response.length() > 0) {
                                        GPDialogs.warningDialog(SourcesTreeListActivity.this, response, null);
                                    } else {
                                        try {
                                            refreshData(baseMaps);
                                        } catch (Exception e) {
                                            GPLog.error(this, null, e);
                                        }
                                    }
                                }
                            };
                            addNewSourcesTask.setProgressDialog("", getString(R.string.adding_new_source), false, null);
                            addNewSourcesTask.execute();
                        }
                    } catch (Exception e) {
                        GPDialogs.errorDialog(this, e, null);
                    }
                }
                break;
            }
            case (PICKFOLDER_REQUEST_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String folderPath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                        final File folder = new File(folderPath);
                        if (folder.exists()) {
                            Utilities.setLastFilePath(this, folderPath);
                            final List<File> foundFiles = new ArrayList<>();
                            // get all supported files
                            String[] supportedExtensions = ESpatialDataSources.getSupportedTileSourcesExtensions();
                            FileUtilities.searchDirectoryRecursive(folder, supportedExtensions, foundFiles);
                            // add basemap to list and in mPreferences
                            addNewSourcesTask = new StringAsyncTask(this) {
                                public List<BaseMap> baseMaps = new ArrayList<>();

                                protected String doBackgroundWork() {
                                    try {

                                        for (int i = 0; i < foundFiles.size(); i++) {
                                            File file = foundFiles.get(i);
                                            try {
                                                // add basemap to list and in mPreferences
                                                BaseMapSourcesManager.INSTANCE.addBaseMapsFromFile(file);
                                            } catch (Exception e) {
                                                // ignore
                                            } finally {
                                                onProgressUpdate(i + 1);
                                            }
                                        }

                                        baseMaps = BaseMapSourcesManager.INSTANCE.getBaseMaps();
                                        if (baseMaps.size() == 0) {
                                            return getString(R.string.selected_file_no_basemap) + folder;
                                        }
                                    } catch (Exception e) {
                                        GPLog.error(this, "Problem getting sources.", e);
                                        return "ERROR: " + e.getLocalizedMessage();
                                    }
                                    return "";
                                }

                                protected void doUiPostWork(String response) {
                                    dispose();
                                    if (response.length() > 0) {
                                        GPDialogs.warningDialog(SourcesTreeListActivity.this, response, null);
                                    } else {
                                        try {
                                            refreshData(baseMaps);
                                        } catch (Exception e) {
                                            GPLog.error(this, null, e);
                                        }
                                    }
                                }
                            };
                            addNewSourcesTask.setProgressDialog("", getString(R.string.adding_new_source), false, foundFiles.size());
                            addNewSourcesTask.execute();
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

    public void refreshData(List<BaseMap> baseMaps) throws Exception {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(SHOW_MAPS, mCheckedValues[0]);
        editor.putBoolean(SHOW_MAPURLS, mCheckedValues[1]);
        editor.putBoolean(SHOW_MBTILES, mCheckedValues[2]);
        editor.putBoolean(SHOW_RASTER_LITE_2, mCheckedValues[3]);
        editor.apply();

        boolean log = GPLog.LOG;
        if (log) {
            GPLog.addLogEntry(this, "Available baseMaps:");
            for (BaseMap basemap : baseMaps) {
                GPLog.addLogEntry(this, basemap.toString());
            }
        }

        newMap.clear();
        for (BaseMap baseMap : baseMaps) {
            String key = baseMap.parentFolder;
            List<BaseMap> newValues = newMap.get(key);
            if (newValues == null) {
                newValues = new ArrayList<>();
                newMap.put(key, newValues);
            }

            boolean doAdd = false;
            String mapType = baseMap.mapType;
            if (mCheckedValues[0] && mapType.equals(ESpatialDataSources.MAP.getTypeName())) {
                doAdd = true;
            } else if (mCheckedValues[1] && mapType.equals(ESpatialDataSources.MAPURL.getTypeName())) {
                doAdd = true;
            } else if (mCheckedValues[2] && mapType.equals(ESpatialDataSources.MBTILES.getTypeName())) {
                doAdd = true;
            } else if (mCheckedValues[3] && mapType.equals(ESpatialDataSources.RASTERLITE2.getTypeName())) {
                doAdd = true;
            }
            if (log) {
                GPLog.addLogEntry(this, "doAdd: " + doAdd + " baseMap: " + baseMap);
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
            if (doAdd) {
                newValues.add(baseMap);
                if (log) {
                    GPLog.addLogEntry(this, "Added: " + baseMap.toString());
                }
            }
            if (newValues.size() == 0) {
                newMap.remove(key);
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
                    BaseMapSourcesManager.INSTANCE.setSelectedBaseMap(selectedBaseMap);
                } catch (jsqlite.Exception e) {
                    GPLog.error(SourcesTreeListActivity.this, "ERROR", e);
                }
                finish();
                return false;
            }
        });

        if (ProfilesHandler.INSTANCE.getActiveProfile() == null)
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
                                            BaseMapSourcesManager.INSTANCE.removeBaseMap(baseMap);
                                        } catch (JSONException e) {
                                            GPLog.error(this, null, e);
                                        }

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    refreshData(BaseMapSourcesManager.INSTANCE.getBaseMaps());
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
                refreshData(BaseMapSourcesManager.INSTANCE.getBaseMaps());
            } catch (Exception e) {
                GPLog.error(SourcesTreeListActivity.this, "ERROR", e);
            }
        }
    };

    @Override
    public Context getContext() {
        return this;
    }
}
