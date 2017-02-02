/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.spatialite.database.spatial.activities.databasesview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.dialogs.ColorStrokeDialogFragment;
import eu.geopaparazzi.library.core.dialogs.LabelDialogFragment;
import eu.geopaparazzi.library.core.dialogs.StrokeDashDialogFragment;
import eu.geopaparazzi.library.core.dialogs.ZoomlevelDialogFragment;
import eu.geopaparazzi.library.core.maps.SpatialiteMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.style.ColorStrokeObject;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.style.LabelObject;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivityStarter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.types.ESpatialDataSources;
import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.TableTypes;

/**
 * Activity for tile source visualisation.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteDatabasesTreeListActivity extends AppCompatActivity implements IActivityStarter,
        LabelDialogFragment.ILabelPropertiesChangeListener, ColorStrokeDialogFragment.IColorStrokePropertiesChangeListener,
        StrokeDashDialogFragment.IDashStrokePropertiesChangeListener, ZoomlevelDialogFragment.IZoomlevelPropertiesChangeListener {
    public static final int PICKFILE_REQUEST_CODE = 666;
    public static final int PICKFOLDER_REQUEST_CODE = 667;

    public static final String SHOW_TABLES = "showTables";
    public static final String SHOW_VIEWS = "showViews";

    private ExpandableListView mExpListView;
    private EditText mFilterText;
    private String mTextToFilter = "";
    private SharedPreferences mPreferences;
    private boolean[] mCheckedValues;
    private List<String> mTypeNames;
    private final LinkedHashMap<String, List<SpatialiteMap>> newMap = new LinkedHashMap<>();
    private SpatialiteDatabasesExpandableListAdapter expandableListAdapter;
    private StringAsyncTask loadDataTask;
    private StringAsyncTask addNewSourceTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spatialitedatabases_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mFilterText = (EditText) findViewById(R.id.search_box);
        mFilterText.addTextChangedListener(filterTextWatcher);

        boolean showTables = mPreferences.getBoolean(SHOW_TABLES, true);
        boolean showViews = mPreferences.getBoolean(SHOW_VIEWS, true);

        String tableTypeName = TableTypes.SPATIALTABLE.getDescription();
        String viewTypeName = TableTypes.SPATIALVIEW.getDescription();
        mTypeNames = new ArrayList<>();
        mTypeNames.add(tableTypeName);
        mTypeNames.add(viewTypeName);
        mCheckedValues = new boolean[mTypeNames.size()];
        mCheckedValues[0] = showTables;
        mCheckedValues[1] = showViews;

        // get the listview
        mExpListView = (ExpandableListView) findViewById(R.id.expandableSourceListView);

        if (ProfilesHandler.INSTANCE.getActiveProfile() != null) {
            RelativeLayout mainView = (RelativeLayout) findViewById(R.id.sources_list_mainview);
            int color = ColorUtilities.toColor(ProfilesHandler.INSTANCE.getActiveProfile().color);
            mainView.setBackgroundColor(color);

            FloatingActionButton addSourceButton = (FloatingActionButton) findViewById(R.id.addSourceButton);
            addSourceButton.hide();
        }

        loadDataTask = new StringAsyncTask(this) {
            List<SpatialiteMap> spatialiteMaps;

            protected String doBackgroundWork() {
                spatialiteMaps = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps();
                return "";
            }

            protected void doUiPostWork(String response) {
                dispose();
                try {
                    refreshData(spatialiteMaps);
                } catch (Exception e) {
                    GPLog.error(this, "Problem getting databases.", e);
                }
            }
        };
        loadDataTask.setProgressDialog("", getString(R.string.loading_databases), false, null);
        loadDataTask.execute();
    }

    @Override
    protected void onStart() {
        super.onStart();


    }

    @Override
    protected void onDestroy() {
        if (loadDataTask != null) loadDataTask.dispose();
        if (addNewSourceTask != null) addNewSourceTask.dispose();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        mFilterText.removeTextChangedListener(filterTextWatcher);

        // save changes to preferences also
        SpatialiteSourcesManager.INSTANCE.saveCurrentSpatialiteMapsToPreferences();
        super.onStop();
    }

    public void add(View view) {
        try {
            String title = getString(R.string.select_spatialite_database);
            String[] supportedExtensions = ESpatialDataSources.getSupportedVectorExtensions();
            AppsUtilities.pickFile(this, PICKFILE_REQUEST_CODE, title, supportedExtensions, null);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(this, e, null);
        }
    }

    public void addFolder(View view) {
        try {
            String title = getString(R.string.select_spatialite_database_folder);
            AppsUtilities.pickFolder(this, PICKFOLDER_REQUEST_CODE, title, null);
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
                        final File file = new File(filePath);
                        if (file.exists()) {
                            Utilities.setLastFilePath(this, filePath);
                            // add basemap to list and in mPreferences
                            addNewSourceTask = new StringAsyncTask(this) {
                                public List<SpatialiteMap> spatialiteMaps;

                                protected String doBackgroundWork() {
                                    try {
                                        // add basemap to list and in mPreferences
                                        if (SpatialiteSourcesManager.INSTANCE.addSpatialiteMapFromFile(file)) {
                                            spatialiteMaps = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps();
                                        } else {
                                            return getString(R.string.selected_file_no_vector_data) + file.getAbsolutePath();
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
                                        GPDialogs.warningDialog(SpatialiteDatabasesTreeListActivity.this, response, null);
                                    } else {
                                        try {
                                            refreshData(spatialiteMaps);
                                        } catch (Exception e) {
                                            GPLog.error(this, null, e);
                                        }
                                    }
                                }
                            };
                            addNewSourceTask.setProgressDialog("", getString(R.string.adding_new_source), false, null);
                            addNewSourceTask.execute();
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
                            String[] supportedExtensions = ESpatialDataSources.getSupportedVectorExtensions();
                            FileUtilities.searchDirectoryRecursive(folder, supportedExtensions, foundFiles);
                            // add basemap to list and in mPreferences
                            addNewSourceTask = new StringAsyncTask(this) {
                                public List<SpatialiteMap> spatialiteMaps;

                                protected String doBackgroundWork() {
                                    try {
                                        for (int i = 0; i < foundFiles.size(); i++) {
                                            File file = foundFiles.get(i);
                                            try {
                                                // add basemap to list and in mPreferences
                                                SpatialiteSourcesManager.INSTANCE.addSpatialiteMapFromFile(file);
                                            } catch (Exception e) {
                                                // ignore
                                            } finally {
                                                onProgressUpdate(i + 1);
                                            }
                                        }
                                        spatialiteMaps = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps();
                                        if (spatialiteMaps.size() == 0) {
                                            return getString(R.string.selected_file_no_vector_data) + folder.getAbsolutePath();
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
                                        GPDialogs.warningDialog(SpatialiteDatabasesTreeListActivity.this, response, null);
                                    } else {
                                        try {
                                            refreshData(spatialiteMaps);
                                        } catch (Exception e) {
                                            GPLog.error(this, null, e);
                                        }
                                    }
                                }
                            };
                            addNewSourceTask.setProgressDialog("", getString(R.string.adding_new_source), false, foundFiles.size());
                            addNewSourceTask.execute();
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
        getMenuInflater().inflate(R.menu.menu_spatialitedatabases, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.select_type_item) {
            TableTypesChoiceDialog dialog = new TableTypesChoiceDialog();
            dialog.open(getString(R.string.select_type), SpatialiteDatabasesTreeListActivity.this, mTypeNames, mCheckedValues);
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshData(List<SpatialiteMap> spatialiteMaps) throws Exception {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(SHOW_TABLES, mCheckedValues[0]);
        editor.putBoolean(SHOW_VIEWS, mCheckedValues[1]);
        editor.apply();

        boolean log = GPLog.LOG;
        if (log) {
            GPLog.addLogEntry(this, "Available spatialitemaps:");
            for (SpatialiteMap tmpSpatialiteMap : spatialiteMaps) {
                GPLog.addLogEntry(this, tmpSpatialiteMap.toString());
            }
        }

        newMap.clear();
        for (SpatialiteMap spatialiteMap : spatialiteMaps) {
            String key = spatialiteMap.databasePath;
            List<SpatialiteMap> newValues = newMap.get(key);
            if (newValues == null) {
                newValues = new ArrayList<>();
                newMap.put(key, newValues);
            }

            boolean doAdd = false;
            String tableType = spatialiteMap.tableType;
            if (tableType == null) {
                doAdd = true;
            } else if (mCheckedValues[0] && tableType.equals(TableTypes.SPATIALTABLE.getDescription())) {
                doAdd = true;
            } else if (mCheckedValues[1] && tableType.equals(TableTypes.SPATIALVIEW.getDescription())) {
                doAdd = true;
            }
            if (log) {
                GPLog.addLogEntry(this, "doAdd: " + doAdd + " spatialiteMap: " + spatialiteMap);
            }

            if (mTextToFilter.length() > 0) {
                // filter text
                String filterString = mTextToFilter.toLowerCase();
                String valueString = spatialiteMap.databasePath.toLowerCase();
                if (!valueString.contains(filterString)) {
                    valueString = spatialiteMap.tableName.toLowerCase();
                    if (!valueString.contains(filterString)) {
                        doAdd = false;
                    }
                }
            }
            if (doAdd) {
                newValues.add(spatialiteMap);
                if (log) {
                    GPLog.addLogEntry(this, "Added: " + spatialiteMap.toString());
                }
            }
            if (newValues.size() == 0) {
                newMap.remove(key);
            }

        }

        expandableListAdapter = new SpatialiteDatabasesExpandableListAdapter(this, newMap);
        mExpListView.setAdapter(expandableListAdapter);
        mExpListView.setClickable(true);
        mExpListView.setFocusable(true);
        mExpListView.setFocusableInTouchMode(true);
//        mExpListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
//            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
//                int index = 0;
//                SpatialiteMap selectedSpatialiteMap = null;
//                for (Entry<String, List<SpatialiteMap>> entry : newMap.entrySet()) {
//                    if (groupPosition == index) {
//                        List<SpatialiteMap> value = entry.getValue();
//                        selectedSpatialiteMap = value.get(childPosition);
//                        break;
//                    }
//                    index++;
//                }
//                try {
//                    SpatialiteSourcesManager.INSTANCE.setSelectedBaseMap(selectedSpatialiteMap);
//                } catch (jsqlite.Exception e) {
//                    GPLog.error(SourcesTreeListActivity.this, "ERROR", e);
//                }
//                finish();
//                return false;
//            }
//        });

        if (ProfilesHandler.INSTANCE.getActiveProfile() == null)
            mExpListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                        int groupPosition = ExpandableListView.getPackedPositionGroup(id);

                        int index = 0;
                        for (final String group : newMap.keySet()) {
                            if (index == groupPosition) {

                                GPDialogs.yesNoMessageDialog(SpatialiteDatabasesTreeListActivity.this, String.format(getString(R.string.remove_from_list), group), new Runnable() {
                                    @Override
                                    public void run() {
                                        List<SpatialiteMap> spatialiteMapList = newMap.get(group);
                                        try {
                                            SpatialiteSourcesManager.INSTANCE.removeSpatialiteMaps(spatialiteMapList);
                                        } catch (Exception e) {
                                            GPLog.error(this, null, e);
                                        }
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    refreshData(SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps());
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
//                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
//                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);
//                    int childPosition = ExpandableListView.getPackedPositionChild(id);
//
//                    int index = 0;
//                    for (String group : newMap.keySet()) {
//                        if (index == groupPosition) {
//                            List<SpatialiteMap> spatialiteMapList = newMap.get(group);
//
//
//                            final SpatialiteMap spatialiteMap = spatialiteMapList.get(childPosition);
//
//                            GPDialogs.yesNoMessageDialog(SpatialiteDatabasesTreeListActivity.this, String.format(getString(R.string.remove_from_list), spatialiteMap.title), new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        SpatialiteSourcesManager.INSTANCE.removeSpatialiteMap(spatialiteMap);
//                                    } catch (JSONException e) {
//                                        GPLog.error(this, null, e);
//                                    }
//
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            try {
//                                                refreshData(SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps());
//                                            } catch (Exception e) {
//                                                GPLog.error(this, null, e);
//                                            }
//                                        }
//                                    });
//
//                                }
//                            }, null);
//
//                            return true;
//                        }
//                        index++;
//                    }
//                    return true;
//                }
                    return false;
                }
            });

        int groupCount = expandableListAdapter.getGroupCount();
        for (int i = 0; i < groupCount; i++) {
            mExpListView.expandGroup(i);
        }
    }

    public void onPropertiesChanged(ColorStrokeObject newColorStrokeObject) {
        if (expandableListAdapter != null)
            expandableListAdapter.onPropertiesChanged(newColorStrokeObject);
    }

    public void onPropertiesChanged(LabelObject newLabelObject) {
        if (expandableListAdapter != null)
            expandableListAdapter.onPropertiesChanged(newLabelObject);
    }

    @Override
    public void onDashChanged(float[] dash, float shift) {
        if (expandableListAdapter != null)
            expandableListAdapter.onDashChanged(dash, shift);
    }

    @Override
    public void onPropertiesChanged(int minZoomlevel, int maxZoomlevel) {
        if (expandableListAdapter != null)
            expandableListAdapter.onPropertiesChanged(minZoomlevel, maxZoomlevel);
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
                refreshData(SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps());
            } catch (Exception e) {
                GPLog.error(SpatialiteDatabasesTreeListActivity.this, "ERROR", e);
            }
        }
    };

    @Override
    public Context getContext() {
        return this;
    }


}
