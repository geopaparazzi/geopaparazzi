/*
 * Copyright 2014 Magnus Woxblom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.geopaparazzi.map.gui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;

import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.style.Style;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.map.GPMapThemes;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.layers.ELayerTypes;
import eu.geopaparazzi.map.layers.LayerManager;
import eu.geopaparazzi.map.layers.interfaces.IGpLayer;
import eu.geopaparazzi.map.layers.utils.SpatialiteColorStrokeDialogFragment;
import eu.geopaparazzi.map.layers.utils.SpatialiteColorStrokeObject;
import eu.geopaparazzi.map.layers.utils.SpatialiteConnectionsHandler;
import eu.geopaparazzi.map.utils.MapUtilities;

class MapLayerAdapter extends DragItemAdapter<MapLayerItem, MapLayerAdapter.ViewHolder> {

    private MapLayerListFragment mapLayerListFragment;
    private int mLayoutId;
    private int mGrabHandleId;
    private boolean mDragOnLongPress;

    private String remove_layer;
    private String toggle3d;
    private String toggleLabels;
    private String setAlpha;
    private String setStyle;
    private String enableEditing;
    private String disableEditing;
    private String setTheme;


    MapLayerAdapter(MapLayerListFragment mapLayerListFragment, ArrayList<MapLayerItem> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
        this.mapLayerListFragment = mapLayerListFragment;
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;

        FragmentActivity activity = mapLayerListFragment.getActivity();
        remove_layer = activity.getString(R.string.menu_remove_layer);
        toggle3d = activity.getString(R.string.menu_toggle_3d);
        toggleLabels = activity.getString(R.string.menu_toggle_labels);
        setAlpha = activity.getString(R.string.menu_set_opacity);
        setStyle = activity.getString(R.string.menu_set_style);
        enableEditing = activity.getString(R.string.menu_enable_editing);
        disableEditing = activity.getString(R.string.menu_disable_editing);
        setTheme = activity.getString(R.string.menu_select_theme);

        setItemList(list);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        MapLayerItem item = mItemList.get(position);
        holder.nameView.setText(item.name);
        holder.pathView.setText(item.url != null ? item.url : item.path);
        updateEditingColor(holder, item.isEditing);
        holder.enableCheckbox.setChecked(item.enabled);
        holder.enableCheckbox.setOnCheckedChangeListener((e, i) -> {
            item.enabled = holder.enableCheckbox.isChecked();
            LayerManager.INSTANCE.setEnabled(item.isSystem, item.position, item.enabled);
        });
        if (item.isSystem) {
            holder.moreButton.setVisibility(View.INVISIBLE);
        } else {
            holder.moreButton.setVisibility(View.VISIBLE);
            holder.moreButton.setOnClickListener(e -> {
                PopupMenu popup = new PopupMenu(mapLayerListFragment.getActivity(), holder.moreButton);

                List<MapLayerItem> itemList = getItemList();
                MapLayerItem selMapLayerItem_ = null;
                int selIndex = 0;
                for (int i = 0; i < itemList.size(); i++) {
                    MapLayerItem mapLayerItem = itemList.get(i);
                    if (mapLayerItem.name.equals(holder.nameView.getText().toString())) {
                        selMapLayerItem_ = mapLayerItem;
                        selIndex = i;
                        break;
                    }
                }
                MapLayerItem selMapLayerItem = selMapLayerItem_;
                if (selMapLayerItem != null) {
                    try {
                        ELayerTypes layerType = ELayerTypes.fromType(selMapLayerItem.type);
                        if (layerType != null) {
                            switch (layerType) {
                                case MAPSFORGE: {
                                    popup.getMenu().add(toggle3d);
                                    popup.getMenu().add(toggleLabels);
                                    popup.getMenu().add(setTheme);
                                    break;
                                }
                                case MBTILES: {
                                    popup.getMenu().add(setAlpha);
                                    break;
                                }
                                case VECTORTILESSERVICE: {
                                    break;
                                }
                                case BITMAPTILESERVICE: {
                                    popup.getMenu().add(setAlpha);
                                    break;
                                }
                                case SPATIALITE: {
                                    popup.getMenu().add(setStyle);
                                    if (selMapLayerItem.isEditing) {
                                        popup.getMenu().add(disableEditing);
                                    } else {
                                        popup.getMenu().add(enableEditing);
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Exception e1) {
                        GPLog.error(this, null, e1);
                    }

                    if (!selMapLayerItem.isSystem) {
                        popup.getMenu().add(remove_layer);
                    }

                    int finalSelIndex = selIndex;
                    popup.setOnMenuItemClickListener(selectedItem -> {
                        String actionName = selectedItem.getTitle().toString();
                        try {
                            if (actionName.equals(remove_layer)) {
                                mapLayerListFragment.removeItemAtIndex(0, finalSelIndex);
                                List<JSONObject> userLayersDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                                userLayersDefinitions.remove(finalSelIndex);
                                notifyDataSetChanged();
                            } else if (actionName.equals(toggle3d)) {
                                List<JSONObject> userLayersDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                                JSONObject jsonObject = userLayersDefinitions.get(finalSelIndex);
                                if (jsonObject.has(IGpLayer.LAYERDO3D_TAG)) {
                                    boolean do3D = jsonObject.getBoolean(IGpLayer.LAYERDO3D_TAG);
                                    jsonObject.put(IGpLayer.LAYERDO3D_TAG, !do3D);

                                } else {
                                    jsonObject.put(IGpLayer.LAYERDO3D_TAG, false);
                                }
                            } else if (actionName.equals(toggleLabels)) {
                                List<JSONObject> userLayersDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                                JSONObject jsonObject = userLayersDefinitions.get(finalSelIndex);
                                if (jsonObject.has(IGpLayer.LAYERDOLABELS_TAG)) {
                                    boolean doLabels = jsonObject.getBoolean(IGpLayer.LAYERDOLABELS_TAG);
                                    jsonObject.put(IGpLayer.LAYERDOLABELS_TAG, !doLabels);
                                } else {
                                    jsonObject.put(IGpLayer.LAYERDOLABELS_TAG, false);
                                }
                            } else if (actionName.equals(setAlpha)) {
                                List<JSONObject> userLayersDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                                JSONObject jsonObject = userLayersDefinitions.get(finalSelIndex);
                                int def = 100;
                                if (jsonObject.has(IGpLayer.LAYERALPHA_TAG))
                                    def = (int) (jsonObject.getDouble(IGpLayer.LAYERALPHA_TAG) * 100.0);

                                int[] opacityLevels = ELayerTypes.OPACITY_LEVELS;
                                String[] alphas = new String[opacityLevels.length];
                                boolean[] selAlphas = new boolean[opacityLevels.length];
                                boolean oneTrue = false;
                                for (int i = 0; i < opacityLevels.length; i++) {
                                    alphas[i] = opacityLevels[i] + "%";
                                    boolean isThat = def == opacityLevels[i];
                                    selAlphas[i] = isThat;
                                    if (isThat) oneTrue = true;
                                }
                                if (!oneTrue)
                                    // if none selected set 100% opaque
                                    selAlphas[selAlphas.length - 1] = true;
                                GPDialogs.singleOptionDialog(mapLayerListFragment.getActivity(), alphas, selAlphas, () -> {
                                    for (int i = 0; i < selAlphas.length; i++) {
                                        if (selAlphas[i]) {
                                            String alpha = alphas[i].replace("%", "");
                                            int alphaInt = Integer.parseInt(alpha);
                                            double a = alphaInt / 100.0;
                                            try {
                                                jsonObject.put(IGpLayer.LAYERALPHA_TAG, a);
                                            } catch (JSONException e1) {
                                                GPLog.error(this, null, e1);
                                            }
                                            break;
                                        }
                                    }
                                });

                            } else if (actionName.equals(setStyle)) {
                                List<JSONObject> userLayersDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                                JSONObject jsonObject = userLayersDefinitions.get(finalSelIndex);
                                String tableName = jsonObject.getString(IGpLayer.LAYERNAME_TAG);
                                String dbPath = jsonObject.getString(IGpLayer.LAYERPATH_TAG);

                                Style style = SpatialiteConnectionsHandler.INSTANCE.getStyleForTable(dbPath, tableName, null);
                                SpatialiteColorStrokeObject colorStrokeObject = new SpatialiteColorStrokeObject();
                                colorStrokeObject.dbPath = dbPath;
                                colorStrokeObject.tableName = tableName;

                                EGeometryType geometryType = SpatialiteConnectionsHandler.INSTANCE.getGeometryType(dbPath, tableName);

                                boolean isPoint = geometryType == EGeometryType.POINT || geometryType == EGeometryType.MULTIPOINT;
                                boolean isLine = geometryType == EGeometryType.LINESTRING || geometryType == EGeometryType.MULTILINESTRING;
                                boolean isPolygon = geometryType == EGeometryType.POLYGON || geometryType == EGeometryType.MULTIPOLYGON;
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
                                SpatialiteColorStrokeDialogFragment colorStrokeDialogFragment = SpatialiteColorStrokeDialogFragment.newInstance(colorStrokeObject);
                                colorStrokeDialogFragment.show(mapLayerListFragment.getSupportFragmentManager(), "Color Stroke Dialog");//NON-NLS
                            } else if (actionName.equals(setTheme)) {
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GPApplication.getInstance());
                                String themeLabel = preferences.getString(MapUtilities.PREFERENCES_KEY_THEME, GPMapThemes.DEFAULT.getThemeLabel());
                                GPMapThemes[] themes = GPMapThemes.values();
                                String[] themeNames = new String[themes.length];
                                boolean[] selThemeNames = new boolean[themes.length];
                                for (int i = 0; i < themeNames.length; i++) {
                                    themeNames[i] = themes[i].getThemeLabel();
                                    if (themeNames[i].equals(themeLabel)) {
                                        selThemeNames[i] = true;
                                    }
                                }
                                GPDialogs.singleOptionDialog(mapLayerListFragment.getActivity(), themeNames, selThemeNames, () -> {
                                    for (int i = 0; i < selThemeNames.length; i++) {
                                        if (selThemeNames[i]) {
                                            SharedPreferences.Editor editor = preferences.edit();
                                            editor.putString(MapUtilities.PREFERENCES_KEY_THEME, GPMapThemes.fromLabel(themeNames[i]).getThemeLabel());
                                            editor.apply();
                                            break;
                                        }
                                    }
                                });

                            } else if (actionName.equals(disableEditing)) {
                                List<JSONObject> userLayersDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                                JSONObject jsonObject = userLayersDefinitions.get(finalSelIndex);
                                jsonObject.put(IGpLayer.LAYEREDITING_TAG, false);
                                selMapLayerItem.isEditing = false;

                                notifyDataSetChanged();
                            } else if (actionName.equals(enableEditing)) {
                                List<JSONObject> userLayersDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                                List<MapLayerItem> mapItemList = getItemList();
                                for (int i = 0; i < userLayersDefinitions.size(); i++) {
                                    JSONObject jsonObject = userLayersDefinitions.get(i);
                                    MapLayerItem tmpItem = mapItemList.get(i);
                                    if (i == finalSelIndex) {
                                        jsonObject.put(IGpLayer.LAYEREDITING_TAG, true);
                                        selMapLayerItem.isEditing = true;
                                    } else {
                                        jsonObject.put(IGpLayer.LAYEREDITING_TAG, false);
                                        tmpItem.isEditing = false;
                                    }
                                }
                                notifyDataSetChanged();
                            }
                        } catch (Exception e1) {
                            GPLog.error(this, null, e1);
                        }
                        return true;
                    });
                    popup.show();
                }
            });
        }
        holder.itemView.setTag(mItemList.get(position));
    }

    private void updateEditingColor(@NonNull ViewHolder holder, boolean isEditing) {
        Context context = mapLayerListFragment.getContext();
        if (context != null) {
            if (isEditing) {
                holder.itemView.setBackgroundColor(Compat.getColor(context, R.color.main_selection));
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.itemView.setBackgroundColor(Compat.getColor(context, R.color.main_background));
                }
            }
        }
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).position;
    }

    class ViewHolder extends DragItemAdapter.ViewHolder {

        TextView nameView;
        TextView pathView;
        CheckBox enableCheckbox;
        ImageButton moreButton;

        ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);
            nameView = itemView.findViewById(R.id.nameView);
            pathView = itemView.findViewById(R.id.pathView);
            enableCheckbox = itemView.findViewById(R.id.enableCheckbox);
            moreButton = itemView.findViewById(R.id.morebutton);
        }

        @Override
        public void onItemClicked(View view) {
//            Toast.makeText(view.getContext(), "Item clicked" + nameView.getText(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onItemLongClicked(View view) {
//            Toast.makeText(view.getContext(), "Item long clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}