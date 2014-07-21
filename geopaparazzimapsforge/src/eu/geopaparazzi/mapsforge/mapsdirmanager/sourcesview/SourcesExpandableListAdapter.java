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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.mapsforge.R;

/**
 * Expandable list for tile sources.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SourcesExpandableListAdapter extends BaseExpandableListAdapter {

    private Activity activity;
    private List<String> folderList;
    private List<List<String[]>> tablesList;
    private String mapsParentPath;

    /**
     * @param activity activity to use.
     * @param folder2TablesMap the folder and table map.
     */
    public SourcesExpandableListAdapter( Activity activity, LinkedHashMap<String, List<String[]>> folder2TablesMap ) {
        this.activity = activity;
        try {
            File mapsDir = ResourcesManager.getInstance(activity).getMapsDir();
            mapsParentPath = mapsDir.getParent() + "/"; //$NON-NLS-1$
        } catch (Exception e) {
            e.printStackTrace();
        }
        folderList = new ArrayList<String>();
        tablesList = new ArrayList<List<String[]>>();
        for( Entry<String, List<String[]>> entry : folder2TablesMap.entrySet() ) {
            folderList.add(entry.getKey());
            tablesList.add(entry.getValue());
        }

    }

    public Object getChild( int groupPosition, int childPosititon ) {
        List<String[]> list = tablesList.get(groupPosition);
        String[] spatialTable = list.get(childPosititon);
        return spatialTable;
    }

    public long getChildId( int groupPosition, int childPosition ) {
        return childPosition;
    }

    public View getChildView( int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent ) {

        final String[] spatialTableData = (String[]) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.sources_list_item, null);
        }

        // convertView.setOnClickListener(new View.OnClickListener(){
        // public void onClick( View v ) {
        //
        // MapsDirManager.getInstance().setSelectedSpatialTable(activity, spatialTableData);
        // activity.finish();
        // }
        // });

        TextView tableNameView = (TextView) convertView.findViewById(R.id.source_header_titletext);
        tableNameView.setTypeface(null, Typeface.BOLD);
        File dbFile = new File(spatialTableData[0]);
        String name = FileUtilities.getNameWithoutExtention(dbFile);
        tableNameView.setText(name);

        TextView tableTypeView = (TextView) convertView.findViewById(R.id.source_header_descriptiontext);
        tableTypeView.setText("[" + spatialTableData[1] + "] ["+spatialTableData[2]+"]");

        return convertView;
    }

    public int getChildrenCount( int groupPosition ) {
        List<String[]> list = tablesList.get(groupPosition);
        return list.size();
    }

    public Object getGroup( int groupPosition ) {
        return folderList.get(groupPosition);
    }

    public int getGroupCount() {
        return folderList.size();
    }

    public long getGroupId( int groupPosition ) {
        return groupPosition;
    }

    public View getGroupView( int groupPosition, boolean isExpanded, View convertView, ViewGroup parent ) {
        String folder = (String) getGroup(groupPosition);
        folder = folder.replaceFirst(mapsParentPath, ""); //$NON-NLS-1$
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.sources_list_header, null);
        }

        TextView folderName = (TextView) convertView.findViewById(R.id.sources_header_descriptiontext);
        folderName.setTypeface(null, Typeface.BOLD);
        folderName.setText(folder);

        return convertView;
    }
    public boolean hasStableIds() {
        return false;
    }

    public boolean isChildSelectable( int groupPosition, int childPosition ) {
        return true;
    }

}
