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
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import eu.geopaparazzi.library.core.maps.SpatialiteMap;
import eu.geopaparazzi.spatialite.R;

/**
 * Expandable list for tile sources.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteDatabasesExpandableListAdapter extends BaseExpandableListAdapter {

    private Activity activity;
    private List<String> folderList;
    private List<List<SpatialiteMap>> tablesList;

    /**
     * @param activity         activity to use.
     * @param folder2TablesMap the folder and table map.
     */
    public SpatialiteDatabasesExpandableListAdapter(Activity activity, LinkedHashMap<String, List<SpatialiteMap>> folder2TablesMap) {
        this.activity = activity;
        folderList = new ArrayList<String>();
        tablesList = new ArrayList<>();
        for (Entry<String, List<SpatialiteMap>> entry : folder2TablesMap.entrySet()) {
            folderList.add(entry.getKey());
            tablesList.add(entry.getValue());
        }

    }

    public Object getChild(int groupPosition, int childPosititon) {
        List<SpatialiteMap> list = tablesList.get(groupPosition);
        SpatialiteMap spatialTable = list.get(childPosititon);
        return spatialTable;
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        final SpatialiteMap table = (SpatialiteMap) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.spatialitedatabases_list_item, null);
        }

        CheckBox isVibileCheckbox = (CheckBox) convertView.findViewById(R.id.isVisibleCheckbox);
        isVibileCheckbox.setChecked(table.isVisible);
        TextView tableNameView = (TextView) convertView.findViewById(R.id.source_header_titletext);
        tableNameView.setText(table.title);
        TextView tableTypeView = (TextView) convertView.findViewById(R.id.source_header_descriptiontext);
        tableTypeView.setText("[" + table.tableType + " -> " + table.geometryType + "]");

        return convertView;
    }

    public int getChildrenCount(int groupPosition) {
        List<SpatialiteMap> list = tablesList.get(groupPosition);
        return list.size();
    }

    public Object getGroup(int groupPosition) {
        return folderList.get(groupPosition);
    }

    public int getGroupCount() {
        return folderList.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String folder = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.spatialitedatabases_list_header, null);
        }

        TextView folderName = (TextView) convertView.findViewById(R.id.sources_header_descriptiontext);
        folderName.setTypeface(null, Typeface.BOLD);
        folderName.setText(folder);

        return convertView;
    }

    public boolean hasStableIds() {
        return false;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}
