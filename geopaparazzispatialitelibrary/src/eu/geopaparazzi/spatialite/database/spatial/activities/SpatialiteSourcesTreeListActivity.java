///*
//* Geopaparazzi - Digital field mapping on Android based devices
//* Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
//*
//* This program is free software: you can redistribute it and/or modify
//* it under the terms of the GNU General Public License as published by
//* the Free Software Foundation, either version 3 of the License, or
//* (at your option) any later version.
//*
//* This program is distributed in the hope that it will be useful,
//* but WITHOUT ANY WARRANTY; without even the implied warranty of
//* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//* GNU General Public License for more details.
//*
//* You should have received a copy of the GNU General Public License
//* along with this program.  If not, see <http://www.gnu.org/licenses/>.
//*/
//package eu.geopaparazzi.spatialite.database.spatial.activities;
//
//import android.app.Activity;
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ExpandableListView;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map.Entry;
//
//import eu.geopaparazzi.library.database.GPLog;
//import eu.geopaparazzi.spatialite.R;
//import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
//import eu.geopaparazzi.spatialite.database.spatial.core.daos.SPL_Rasterlite;
//import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
//import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;
//import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
//import eu.geopaparazzi.spatialite.database.spatial.util.comparators.OrderComparator;
//
///**
//* Activity for tile source visualisation.
//*
//* @author Andrea Antonello (www.hydrologis.com)
//*
//*/
//public class SpatialiteSourcesTreeListActivity extends Activity implements OnClickListener {
//
//    SpatialiteSourcesExpandableListAdapter listAdapter;
//    ExpandableListView expListView;
//    private Button toggleTablesButton;
//    private Button toggleViewsButton;
//    private boolean showTables = true;
//    private boolean showViews = true;
//    private EditText filterText;
//    private String textToFilter = "";
//
//    @Override
//    protected void onCreate( Bundle savedInstanceState ) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.spatialite_sources_list);
//
//        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//
//        filterText = (EditText) findViewById(R.id.search_box);
//        filterText.addTextChangedListener(filterTextWatcher);
//
//        toggleTablesButton = (Button) findViewById(R.id.toggleTablesButton);
//        toggleTablesButton.setOnClickListener(this);
//        toggleTablesButton.setText(SpatialDataType.MAP.getTypeName());
//        toggleTablesButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable_selected));
//
//        toggleViewsButton = (Button) findViewById(R.id.toggleViewsButton);
//        toggleViewsButton.setOnClickListener(this);
//        toggleViewsButton.setText(SpatialDataType.MAPURL.getTypeName());
//        toggleViewsButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable_selected));
//
//        // get the listview
//        expListView = (ExpandableListView) findViewById(R.id.expandableSourceListView);
//
//        try {
//            refreshData();
//        } catch (Exception e) {
//            GPLog.error(this, "Problem getting sources.", e); //$NON-NLS-1$
//        }
//    }
//
//    protected void onDestroy() {
//        super.onDestroy();
//        filterText.removeTextChangedListener(filterTextWatcher);
//    }
//
//    void refreshData() throws Exception {
//        final LinkedHashMap<String, List<SpatialVectorTable>> newMap = new LinkedHashMap<String, List<SpatialVectorTable>>();
//        List<AbstractSpatialDatabaseHandler> spatialDatabaseHandlers = SpatialDatabasesManager.getInstance().getSpatialDatabaseHandlers();
//        for (AbstractSpatialDatabaseHandler spatialDatabaseHandler : spatialDatabaseHandlers) {
//            String databasePath = spatialDatabaseHandler.getDatabasePath();
//
//            List<SpatialVectorTable> spatialVectorTables = spatialDatabaseHandler.getSpatialVectorTables(false);
//            List<SpatialVectorTable> acceptedSpatialVectorTables = new ArrayList<SpatialVectorTable>();
//            for (SpatialVectorTable spatialVectorTable : spatialVectorTables) {
//                boolean isView = !spatialVectorTable.isEditable(); // TODO this needs to be made better
//
//                boolean doAdd = false;
//                if (showTables && !isView) {
//                    doAdd = true;
//                } else if (showViews && isView) {
//                    doAdd = true;
//                }
//
//                if (textToFilter.length() > 0) {
//                    // filter text
//                    String tableNameString = spatialVectorTable.getTableName().toLowerCase();
//                    String filterString = textToFilter.toLowerCase();
//                    if (!tableNameString.contains(filterString)) {
//                        doAdd = false;
//                    }
//                }
//
//                if (doAdd){
//                    acceptedSpatialVectorTables.add(spatialVectorTable);
//                }
//            }
//            if (acceptedSpatialVectorTables.size()>0){
//                Collections.sort(acceptedSpatialVectorTables, new OrderComparator());
//                newMap.put(databasePath, acceptedSpatialVectorTables);
//            }
//        }
//
//
//        listAdapter = new SpatialiteSourcesExpandableListAdapter(this, newMap);
//        expListView.setAdapter(listAdapter);
//        expListView.setClickable(true);
//        expListView.setFocusable(true);
//        expListView.setFocusableInTouchMode(true);
//        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener(){
//            public boolean onChildClick( ExpandableListView parent, View v, int groupPosition, int childPosition, long id ) {
//                int index = 0;
//                SpatialVectorTable spatialTableDate = null;
//                for( Entry<String, List<SpatialVectorTable>> entry : newMap.entrySet() ) {
//                    if (groupPosition == index) {
//                        List<SpatialVectorTable> value = entry.getValue();
//                        spatialTableDate = value.get(childPosition);
//                        break;
//                    }
//                    index++;
//                }
//                return false;
//            }
//        });
//        int groupCount = listAdapter.getGroupCount();
//        for( int i = 0; i < groupCount; i++ ) {
//            expListView.expandGroup(i);
//        }
//    }
//    @Override
//    public void onClick( View view ) {
//        if (view == toggleTablesButton) {
//            if (!showTables) {
//                toggleTablesButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable_selected));
//            } else {
//                toggleTablesButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable));
//            }
//            showTables = !showTables;
//        }
//        if (view == toggleViewsButton) {
//            if (!showViews) {
//                toggleViewsButton.setBackgroundDrawable(getResources().getDrawable(
//                        R.drawable.button_background_drawable_selected));
//            } else {
//                toggleViewsButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable));
//            }
//            showViews = !showViews;
//        }
//        try {
//            refreshData();
//        } catch (Exception e) {
//            GPLog.error(this, "Error getting source data.", e); //$NON-NLS-1$
//        }
//    }
//
//    private TextWatcher filterTextWatcher = new TextWatcher(){
//
//        public void afterTextChanged( Editable s ) {
//            // ignore
//        }
//
//        public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
//            // ignore
//        }
//
//        public void onTextChanged( CharSequence s, int start, int before, int count ) {
//            textToFilter = s.toString();
//            try {
//                refreshData();
//            } catch (Exception e) {
//                GPLog.error(SpatialiteSourcesTreeListActivity.this, "ERROR", e);
//            }
//        }
//    };
//}
