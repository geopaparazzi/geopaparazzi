///*
// * Geopaparazzi - Digital field mapping on Android based devices
// * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package eu.geopaparazzi.spatialite.database.spatial.activities;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Typeface;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseExpandableListAdapter;
//import android.widget.CheckBox;
//import android.widget.CompoundButton;
//import android.widget.ImageButton;
//import android.widget.TextView;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map.Entry;
//
//import eu.geopaparazzi.library.database.GPLog;
//import eu.geopaparazzi.library.util.FileUtilities;
//import eu.geopaparazzi.library.util.ResourcesManager;
//import eu.geopaparazzi.spatialite.R;
//import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
//import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
//import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
//import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteLibraryConstants;
//import eu.geopaparazzi.spatialite.database.spatial.util.comparators.OrderComparator;
//
///**
// * Expandable list for tile sources.
// *
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class SpatialiteSourcesExpandableListAdapter extends BaseExpandableListAdapter {
//
//    private SpatialiteSourcesTreeListActivity activity;
//    private List<String> folderList;
//    private List<List<SpatialVectorTable>> tablesList;
//    private String mapsParentPath;
//
//    /**
//     * @param activity activity to use.
//     * @param databasepath2TablesMap the database and table map.
//     */
//    public SpatialiteSourcesExpandableListAdapter(SpatialiteSourcesTreeListActivity activity, LinkedHashMap<String, List<SpatialVectorTable>> databasepath2TablesMap) {
//        this.activity = activity;
//        try {
//            File mapsDir = ResourcesManager.getInstance(activity).getMapsDir();
//            mapsParentPath = mapsDir.getParent() + "/"; //$NON-NLS-1$
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        folderList = new ArrayList<String>();
//        tablesList = new ArrayList<List<SpatialVectorTable>>();
//        for( Entry<String, List<SpatialVectorTable>> entry : databasepath2TablesMap.entrySet() ) {
//            folderList.add(entry.getKey());
//            tablesList.add(entry.getValue());
//        }
//
//    }
//
//    public Object getChild( int groupPosition, int childPosititon ) {
//        List<SpatialVectorTable> list = tablesList.get(groupPosition);
//        SpatialVectorTable spatialTable = list.get(childPosititon);
//        return spatialTable;
//    }
//
//    public long getChildId( int groupPosition, int childPosition ) {
//        return childPosition;
//    }
//
//    public View getChildView( final int groupPosition, final int position, boolean isLastChild, View rowView, ViewGroup parent ) {
//
//        final SpatialVectorTable item = (SpatialVectorTable) getChild(groupPosition, position);
//        final List<SpatialVectorTable> allVectorTables = tablesList.get(groupPosition);
//
//        if (rowView == null) {
//            LayoutInflater infalInflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            rowView = infalInflater.inflate(R.layout.data_row, null);
//        }
//
//        AbstractSpatialDatabaseHandler tableHandler = null;
//        try {
//            tableHandler = SpatialDatabasesManager.getInstance().getVectorHandler(item);
//        } catch (jsqlite.Exception e1) {
//            e1.printStackTrace();
//        }
//
//        TextView nameView = (TextView) rowView.findViewById(R.id.name);
//        TextView descriptionView = (TextView) rowView.findViewById(R.id.description);
//
//        CheckBox visibleView = (CheckBox) rowView.findViewById(R.id.visible);
//        ImageButton listUpButton = (ImageButton) rowView.findViewById(R.id.upButton);
//        listUpButton.setOnClickListener(new View.OnClickListener(){
//            public void onClick( View v ) {
//                if (position > 0) {
//                    List<SpatialVectorTable> spatialTables = tablesList.get(groupPosition);
//                    SpatialVectorTable before = spatialTables.get(position - 1);
//                    int tmp1 = before.getStyle().order;
//                    int tmp2 = item.getStyle().order;
//                    item.getStyle().order = tmp1;
//                    before.getStyle().order = tmp2;
//                    Collections.sort(spatialTables, new OrderComparator());
//                    try {
//                        activity.refreshData();
//                    } catch (Exception e) {
//                        GPLog.error(this, null, e);
//                    }
//                }
//            }
//        });
//
//        ImageButton listDownButton = (ImageButton) rowView.findViewById(R.id.downButton);
//        listDownButton.setOnClickListener(new View.OnClickListener(){
//            public void onClick( View v ) {
//                List<SpatialVectorTable> spatialTables = tablesList.get(groupPosition);
//                if (position < spatialTables.size() - 1) {
//                    SpatialVectorTable after = spatialTables.get(position + 1);
//                    int tmp1 = after.getStyle().order;
//                    int tmp2 = item.getStyle().order;
//                    item.getStyle().order = tmp1;
//                    after.getStyle().order = tmp2;
//                    Collections.sort(spatialTables, new OrderComparator());
//                    try {
//                        activity.refreshData();
//                    } catch (Exception e) {
//                        GPLog.error(this, null, e);
//                    }
//                }
//            }
//        });
//
//        ImageButton propertiesButton = (ImageButton) rowView.findViewById(R.id.propertiesButton);
//        propertiesButton.setOnClickListener(new View.OnClickListener(){
//            public void onClick( View v ) {
//                Intent intent = null;
//                if (item.isLine()) {
//                    intent = new Intent(v.getContext(), LinesDataPropertiesActivity.class);
//                } else if (item.isPolygon()) {
//                    intent = new Intent(v.getContext(), PolygonsDataPropertiesActivity.class);
//                } else if ((item.isPoint()) || (item.isGeometryCollection())) {
//                    intent = new Intent(v.getContext(), PointsDataPropertiesActivity.class);
//                }
//                intent.putExtra(SpatialiteLibraryConstants.PREFS_KEY_TEXT, item.getUniqueNameBasedOnDbFilePath());
//                v.getContext().startActivity(intent);
//
//            }
//        });
//
//        ImageButton zoomtoButton = (ImageButton) rowView.findViewById(R.id.zoomtoButton);
//        zoomtoButton.setOnClickListener(new View.OnClickListener(){
//            public void onClick( View v ) {
//                try {
//                    float[] tableBounds = SpatialDatabasesManager.getInstance().getVectorHandler(item)
//                            .getTableBounds(item);
//                    double lat = tableBounds[1] + (tableBounds[0] - tableBounds[1]) / 2.0;
//                    double lon = tableBounds[3] + (tableBounds[2] - tableBounds[3]) / 2.0;
//
//                    Intent intent = activity.getIntent();
//                    intent.putExtra(SpatialiteLibraryConstants.LATITUDE, lat);
//                    intent.putExtra(SpatialiteLibraryConstants.LONGITUDE, lon);
//                    activity.setResult(Activity.RESULT_OK, intent);
//                    activity.finish();
//                } catch (jsqlite.Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        ImageButton labelsButton = (ImageButton) rowView.findViewById(R.id.labelsButton);
//        labelsButton.setOnClickListener(new View.OnClickListener(){
//            public void onClick( View v ) {
//                Intent intent = new Intent(v.getContext(), LabelPropertiesActivity.class);
//                intent.putExtra(SpatialiteLibraryConstants.PREFS_KEY_TEXT, item.getUniqueNameBasedOnDbFilePath());
//                v.getContext().startActivity(intent);
//            }
//        });
//
//        // rowView.setBackgroundColor(ColorUtilities.toColor(item.getColor()));
//        // mj10777: some tables may have more than one column, thus the column name will
//        // also be shown item.getUniqueName()
//        nameView.setText(item.getTableName());
//
//        String dbName = item.getFileName();
//
//        descriptionView.setText(item.getGeomName() + ": " + item.getLayerTypeDescription() + ", db: " + dbName);
//
//        visibleView.setChecked(item.getStyle().enabled != 0);
//        visibleView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
//            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
//                item.getStyle().enabled = isChecked ? 1 : 0;
//                try {
//                    SpatialDatabasesManager.getInstance().updateStyle(item);
//                } catch (jsqlite.Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        return rowView;
//    }
//
//    public int getChildrenCount( int groupPosition ) {
//        List<SpatialVectorTable> list = tablesList.get(groupPosition);
//        return list.size();
//    }
//
//    public Object getGroup( int groupPosition ) {
//        return folderList.get(groupPosition);
//    }
//
//    public int getGroupCount() {
//        return folderList.size();
//    }
//
//    public long getGroupId( int groupPosition ) {
//        return groupPosition;
//    }
//
//    public View getGroupView( int groupPosition, boolean isExpanded, View convertView, ViewGroup parent ) {
//        String folder = (String) getGroup(groupPosition);
//        folder = folder.replaceFirst(mapsParentPath, ""); //$NON-NLS-1$
//        if (convertView == null) {
//            LayoutInflater infalInflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            convertView = infalInflater.inflate(R.layout.spatialite_sources_list_header, null);
//        }
//
//        TextView folderName = (TextView) convertView.findViewById(R.id.sources_header_descriptiontext);
//        folderName.setTypeface(null, Typeface.BOLD);
//        folderName.setText(folder);
//
//        return convertView;
//    }
//    public boolean hasStableIds() {
//        return false;
//    }
//
//    public boolean isChildSelectable( int groupPosition, int childPosition ) {
//        return true;
//    }
//
//}
