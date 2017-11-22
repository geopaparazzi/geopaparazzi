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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import eu.geopaparazzi.library.core.activities.GeocodeActivity;
import eu.geopaparazzi.library.core.dialogs.ColorStrokeDialogFragment;
import eu.geopaparazzi.library.core.dialogs.InsertCoordinatesDialogFragment;
import eu.geopaparazzi.library.core.dialogs.LabelDialogFragment;
import eu.geopaparazzi.library.core.dialogs.StrokeDashDialogFragment;
import eu.geopaparazzi.library.core.dialogs.ZoomlevelDialogFragment;
import eu.geopaparazzi.library.core.maps.SpatialiteMap;
import eu.geopaparazzi.library.core.maps.SpatialiteMapOrderComparator;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorStrokeObject;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.style.LabelObject;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.library.style.Style;
import jsqlite.Exception;

/**
 * Expandable list for tile sources.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteDatabasesExpandableListAdapter extends BaseExpandableListAdapter {

    private Activity activity;
    private List<String> folderList;
    private List<List<SpatialiteMap>> tablesList;
    private int count = 0;
    private final String[] orderArray;
    private final HashMap<SpatialiteMap, ViewHolder> spatialiteMaps2Viewholders = new HashMap<>();
    private List<SpatialiteMap> allSpatialiteMaps = new ArrayList<>();

    private volatile boolean ignoreSpinnerEvents = false;
    private SpatialiteMap currentPropertiesEditedSpatialiteMap;

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
            List<SpatialiteMap> spatialiteMaps = entry.getValue();
            tablesList.add(spatialiteMaps);
            allSpatialiteMaps.addAll(spatialiteMaps);
        }
        for (SpatialiteMap spatialiteMap : allSpatialiteMaps) {
            count++;
        }

        orderArray = new String[count];
        for (int i = 0; i < count; i++) {
            orderArray[i] = String.valueOf(i);
        }

        orderSpatialiteMaps();

    }

    private void orderSpatialiteMaps() {

        Collections.sort(allSpatialiteMaps, new SpatialiteMapOrderComparator());
        for (int i = 0; i < allSpatialiteMaps.size(); i++) {
            allSpatialiteMaps.get(i).order = i;
        }


    }

    public Object getChild(int groupPosition, int childPosititon) {
        List<SpatialiteMap> list = tablesList.get(groupPosition);
        SpatialiteMap spatialiteMap = list.get(childPosititon);
        return spatialiteMap;
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    private static class ViewHolder {
        Spinner orderSpinner;
        CheckBox isVibileCheckbox;
        TextView tableNameView;
        TextView tableTypeView;
        ImageButton moreButton;
        View view;
    }

    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        final SpatialiteMap spatialiteMap = (SpatialiteMap) getChild(groupPosition, childPosition);
        ViewHolder viewHolder = spatialiteMaps2Viewholders.get(spatialiteMap);

        if (viewHolder == null) {
            viewHolder = new ViewHolder();
            LayoutInflater infalInflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            viewHolder.view = infalInflater.inflate(R.layout.spatialitedatabases_list_item, null);
            spatialiteMaps2Viewholders.put(spatialiteMap, viewHolder);
        }

        convertView = viewHolder.view;

        viewHolder.orderSpinner = convertView.findViewById(R.id.orderSpinner);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this.activity, android.R.layout.simple_spinner_item, orderArray);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        viewHolder.orderSpinner.setAdapter(spinnerArrayAdapter);
        viewHolder.orderSpinner.setSelection((int) spatialiteMap.order);
        final Spinner orderSpinner = viewHolder.orderSpinner;
        viewHolder.orderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (ignoreSpinnerEvents) return;
                String item = orderSpinner.getSelectedItem().toString();
                double newOrder = count + 1;
                try {
                    newOrder = Double.parseDouble(item);
                } catch (NumberFormatException e) {
                    GPLog.error(this, null, e);
                }

                if (spatialiteMap.order == newOrder) {
                    return;
                } else if (spatialiteMap.order < newOrder) {
                    newOrder = newOrder + 0.1;
                } else {
                    newOrder = newOrder - 0.1;
                }
                spatialiteMap.order = newOrder;

                // reorder
                orderSpatialiteMaps();

                ignoreSpinnerEvents = true;
                // fix combos
                for (Entry<SpatialiteMap, ViewHolder> entry : spatialiteMaps2Viewholders.entrySet()) {
                    int order = (int) entry.getKey().order;
                    entry.getValue().orderSpinner.setSelection(order);
                }
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ignoreSpinnerEvents = false;
                    }
                }).start();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        viewHolder.isVibileCheckbox = convertView.findViewById(R.id.isVisibleCheckbox);
        viewHolder.isVibileCheckbox.setChecked(spatialiteMap.isVisible);
        viewHolder.isVibileCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                spatialiteMap.isVisible = isChecked;
            }
        });
        viewHolder.tableNameView = convertView.findViewById(R.id.source_header_titletext);
        viewHolder.tableNameView.setText(spatialiteMap.tableName);
        viewHolder.tableTypeView = convertView.findViewById(R.id.source_header_descriptiontext);
        viewHolder.tableTypeView.setText("[" + spatialiteMap.geometryType + "]");

        viewHolder.moreButton = convertView.findViewById(R.id.moreButton);
        final ViewHolder finalViewHolder = viewHolder;
        viewHolder.moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMoreMenu(finalViewHolder.moreButton, spatialiteMap);
            }
        });


        return viewHolder.view;
    }

    private void openMoreMenu(ImageButton button, final SpatialiteMap spatialiteMap) {

        final String zoomToTitle = "Zoom to";
        final String labellingTitle = "Labelling";
        final String propertiesTitle = "Properties";
        final String extrasTitle = "Extras";

        PopupMenu popup = new PopupMenu(this.activity, button);
        popup.getMenu().add(zoomToTitle);
        popup.getMenu().add(labellingTitle);
        popup.getMenu().add(propertiesTitle);
        popup.getMenu().add(extrasTitle);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                String actionName = item.getTitle().toString();
                try {
                    if (actionName.equals(zoomToTitle)) {
                        zoomTo(spatialiteMap);
                    } else if (actionName.equals(labellingTitle)) {
                        labelling(spatialiteMap);
                    } else if (actionName.equals(propertiesTitle)) {
                        properties(spatialiteMap);
                    } else if (actionName.equals(extrasTitle)) {
                        extras(spatialiteMap);
                    }
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                }
                return true;
            }


        });
        popup.show();
    }

    private void extras(final SpatialiteMap spatialiteMap) {

        String[] items = new String[]{"Stroke Dash", "Zoomlevel visibility"};//, "Decimation"};

        new AlertDialog.Builder(activity).setSingleChoiceItems(items, 0, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();

                        currentPropertiesEditedSpatialiteMap = spatialiteMap;
                        SpatialVectorTable spatialVectorTable = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap().get(spatialiteMap);
                        Style style = spatialVectorTable.getStyle();
                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        if (selectedPosition == 0) {

                            float[] dash = Style.dashFromString(style.dashPattern);
                            StrokeDashDialogFragment strokeDashDialogFragment;
                            if (dash != null && dash.length > 2) {
                                float[] dashPart = Style.getDashOnly(dash);
                                strokeDashDialogFragment = StrokeDashDialogFragment.newInstance(dashPart, Style.getDashShift(dash));
                            } else {
                                strokeDashDialogFragment = StrokeDashDialogFragment.newInstance(null, 0);
                            }
                            strokeDashDialogFragment.show(((AppCompatActivity) activity).getSupportFragmentManager(), "Stroke Dash Dialog");
                        } else if (selectedPosition == 1) {
                            int[] minMaxZoomlevel = {style.minZoom, style.maxZoom};
                            ZoomlevelDialogFragment zoomlevelDialogFragment = ZoomlevelDialogFragment.newInstance(minMaxZoomlevel);
                            zoomlevelDialogFragment.show(((AppCompatActivity) activity).getSupportFragmentManager(), "Zoomlevel Dialog");
                        } else if (selectedPosition == 2) {
                            // TODO  decimation
                        }

                    }
                }).show();


    }

    private void properties(SpatialiteMap spatialiteMap) {
        currentPropertiesEditedSpatialiteMap = spatialiteMap;
        SpatialVectorTable spatialVectorTable = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap().get(spatialiteMap);
        Style style = spatialVectorTable.getStyle();
        ColorStrokeObject colorStrokeObject = new ColorStrokeObject();
        boolean isPoint = spatialVectorTable.isPoint();
        boolean isLine = spatialVectorTable.isLine();
        boolean isPolygon = spatialVectorTable.isPolygon();
        if (isPolygon || isPoint) {
            colorStrokeObject.hasFill = true;
            colorStrokeObject.fillColor = ColorUtilities.toColor(style.fillcolor);
            colorStrokeObject.fillAlpha = (int) (style.fillalpha * 255);
        }
        if (isPolygon || isLine || isPoint) {
            colorStrokeObject.hasStroke = true;
            colorStrokeObject.strokeColor = ColorUtilities.toColor(style.strokecolor);
            colorStrokeObject.strokeAlpha = (int) (style.strokealpha * 255);

            colorStrokeObject.hasStrokeWidth = true;
            colorStrokeObject.strokeWidth = (int) style.width;
        }
        if (isPoint) {
            colorStrokeObject.hasShape = true;
            colorStrokeObject.shapeWKT = style.shape;
            colorStrokeObject.shapeSize = (int) style.size;
        }
        ColorStrokeDialogFragment colorStrokeDialogFragment = ColorStrokeDialogFragment.newInstance(colorStrokeObject);
        colorStrokeDialogFragment.show(((AppCompatActivity) activity).getSupportFragmentManager(), "Color Stroke Dialog");

    }

    public void onPropertiesChanged(ColorStrokeObject newColorStrokeObject) {
        if (currentPropertiesEditedSpatialiteMap != null) {
            SpatialVectorTable spatialVectorTable = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap().get(currentPropertiesEditedSpatialiteMap);
            Style style = spatialVectorTable.getStyle();
            boolean isPoint = spatialVectorTable.isPoint();
            boolean isLine = spatialVectorTable.isLine();
            boolean isPolygon = spatialVectorTable.isPolygon();
            if (isPolygon || isPoint) {
                style.fillcolor = ColorUtilities.getHex(newColorStrokeObject.fillColor);
                style.fillalpha = newColorStrokeObject.fillAlpha / 255f;
            }
            if (isPolygon || isLine || isPoint) {
                style.strokecolor = ColorUtilities.getHex(newColorStrokeObject.strokeColor);
                style.strokealpha = newColorStrokeObject.strokeAlpha / 255f;
                style.width = newColorStrokeObject.strokeWidth;
            }
            if (isPoint) {
                style.shape = newColorStrokeObject.shapeWKT;
                style.size = newColorStrokeObject.shapeSize;
            }

            HashMap<SpatialiteMap, SpatialiteDatabaseHandler> spatialiteMaps2DbHandlersMap = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2DbHandlersMap();
            SpatialiteDatabaseHandler spatialiteDatabaseHandler = spatialiteMaps2DbHandlersMap.get(currentPropertiesEditedSpatialiteMap);
            try {
                spatialiteDatabaseHandler.updateStyle(style);
            } catch (jsqlite.Exception e) {
                GPLog.error(this, null, e);
            }
        }

    }

    private void labelling(SpatialiteMap spatialiteMap) {
        if (spatialiteMap != null) {
            currentPropertiesEditedSpatialiteMap = spatialiteMap;
            SpatialVectorTable spatialVectorTable = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap().get(spatialiteMap);

            Style style = spatialVectorTable.getStyle();

            LabelObject labelObject = new LabelObject();
            labelObject.hasLabel = style.labelvisible == 1;
            labelObject.labelFieldsList = spatialVectorTable.getTableFieldNamesList();
            labelObject.label = style.labelfield;
            labelObject.labelSize = (int) style.labelsize;

            LabelDialogFragment labelDialogFragment = LabelDialogFragment.newInstance(labelObject);
            labelDialogFragment.show(((AppCompatActivity) activity).getSupportFragmentManager(), "Label Dialog");
        }
    }

    public void onPropertiesChanged(LabelObject newLabelObject) {
        if (currentPropertiesEditedSpatialiteMap != null) {
            SpatialVectorTable spatialVectorTable = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap().get(currentPropertiesEditedSpatialiteMap);
            Style style = spatialVectorTable.getStyle();

            style.labelvisible = newLabelObject.hasLabel ? 1 : 0;
            style.labelfield = newLabelObject.label;
            style.labelsize = newLabelObject.labelSize;

            HashMap<SpatialiteMap, SpatialiteDatabaseHandler> spatialiteMaps2DbHandlersMap = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2DbHandlersMap();
            SpatialiteDatabaseHandler spatialiteDatabaseHandler = spatialiteMaps2DbHandlersMap.get(currentPropertiesEditedSpatialiteMap);
            try {
                spatialiteDatabaseHandler.updateStyle(style);
            } catch (jsqlite.Exception e) {
                GPLog.error(this, null, e);
            }
        }
    }

    public void onPropertiesChanged(int minZoomlevel, int maxZoomlevel) {
        if (currentPropertiesEditedSpatialiteMap != null) {
            SpatialVectorTable spatialVectorTable = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap().get(currentPropertiesEditedSpatialiteMap);
            Style style = spatialVectorTable.getStyle();

            style.minZoom = minZoomlevel;
            style.maxZoom = maxZoomlevel;

            HashMap<SpatialiteMap, SpatialiteDatabaseHandler> spatialiteMaps2DbHandlersMap = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2DbHandlersMap();
            SpatialiteDatabaseHandler spatialiteDatabaseHandler = spatialiteMaps2DbHandlersMap.get(currentPropertiesEditedSpatialiteMap);
            try {
                spatialiteDatabaseHandler.updateStyle(style);
            } catch (jsqlite.Exception e) {
                GPLog.error(this, null, e);
            }
        }
    }


    public void onDashChanged(float[] dash, float shift) {
        if (currentPropertiesEditedSpatialiteMap != null) {
            SpatialVectorTable spatialVectorTable = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap().get(currentPropertiesEditedSpatialiteMap);
            Style style = spatialVectorTable.getStyle();

            style.dashPattern = Style.dashToString(dash, shift);

            HashMap<SpatialiteMap, SpatialiteDatabaseHandler> spatialiteMaps2DbHandlersMap = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2DbHandlersMap();
            SpatialiteDatabaseHandler spatialiteDatabaseHandler = spatialiteMaps2DbHandlersMap.get(currentPropertiesEditedSpatialiteMap);
            try {
                spatialiteDatabaseHandler.updateStyle(style);
            } catch (jsqlite.Exception e) {
                GPLog.error(this, null, e);
            }
        }
    }

    private void zoomTo(SpatialiteMap spatialiteMap) throws Exception {
        HashMap<SpatialiteMap, SpatialiteDatabaseHandler> spatialiteMaps2DbHandlersMap = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2DbHandlersMap();
        SpatialiteDatabaseHandler spatialiteDatabaseHandler = spatialiteMaps2DbHandlersMap.get(spatialiteMap);
        HashMap<SpatialiteMap, SpatialVectorTable> spatialiteMaps2TablesMap = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap();
        SpatialVectorTable spatialVectorTable = spatialiteMaps2TablesMap.get(spatialiteMap);
        if (spatialiteDatabaseHandler != null) {
            float[] tableBounds = spatialiteDatabaseHandler.getTableBounds(spatialVectorTable);
            double lat = tableBounds[1] + (tableBounds[0] - tableBounds[1]) / 2.0;
            double lon = tableBounds[3] + (tableBounds[2] - tableBounds[3]) / 2.0;
            Intent intent = activity.getIntent();
            intent.putExtra(LibraryConstants.LATITUDE, lat);
            intent.putExtra(LibraryConstants.LONGITUDE, lon);
            intent.putExtra(LibraryConstants.ZOOMLEVEL, 16);
            activity.setResult(Activity.RESULT_OK, intent);
            activity.finish();
        }
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

        File dbFile = new File(folder);

        TextView dbName = convertView.findViewById(R.id.sources_header_nametext);
        dbName.setTypeface(null, Typeface.BOLD);
        dbName.setText(dbFile.getName());
        TextView folderName = convertView.findViewById(R.id.sources_header_pathtext);
        folderName.setText(dbFile.getParent());

        return convertView;
    }

    public boolean hasStableIds() {
        return false;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}
