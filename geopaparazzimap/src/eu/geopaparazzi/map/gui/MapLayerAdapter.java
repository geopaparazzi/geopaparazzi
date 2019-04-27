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

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.ANote;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.layers.ELayerTypes;
import eu.geopaparazzi.map.layers.LayerManager;
import eu.geopaparazzi.map.layers.interfaces.IGpLayer;

class MapLayerAdapter extends DragItemAdapter<MapLayerItem, MapLayerAdapter.ViewHolder> {

    private MapLayerListFragment mapLayerListFragment;
    private int mLayoutId;
    private int mGrabHandleId;
    private boolean mDragOnLongPress;

    MapLayerAdapter(MapLayerListFragment mapLayerListFragment, ArrayList<MapLayerItem> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
        this.mapLayerListFragment = mapLayerListFragment;
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
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
        item.position = position;
        holder.nameView.setText(item.name);
        holder.pathView.setText(item.url != null ? item.url : item.path);
        holder.enableCheckbox.setChecked(item.enabled);
        holder.enableCheckbox.setOnCheckedChangeListener((e, i) -> {
            item.enabled = holder.enableCheckbox.isChecked();
            LayerManager.INSTANCE.setEnabled(item.isSystem, item.position, item.enabled);
        });
        holder.moreButton.setOnClickListener(e -> {
            PopupMenu popup = new PopupMenu(mapLayerListFragment.getActivity(), holder.moreButton);
            String remove_layer = "Remove layer";
            String toggle3d = "Toggle 3D";
            String toggleLabels = "Toggle Labels";
            String setAlpha = "Set transparency";

            MapLayerItem mapLayerItem = getItemList().get(item.position);

            try {
                ELayerTypes layerType = ELayerTypes.fromType(mapLayerItem.type);
                switch (layerType) {
                    case MAPSFORGE: {
                        popup.getMenu().add(toggle3d);
                        popup.getMenu().add(toggleLabels);
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
                }
            } catch (Exception e1) {
                GPLog.error(this, null, e1);
            }

            if (!mapLayerItem.isSystem) {
                popup.getMenu().add(remove_layer);
            }

            popup.setOnMenuItemClickListener(selectedItem -> {
                String actionName = selectedItem.getTitle().toString();
                try {
                    if (actionName.equals(remove_layer)) {
                        mapLayerListFragment.removeItemAtIndex(0, item.position);
                        List<JSONObject> userLayersDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                        userLayersDefinitions.remove(item.position);
                        notifyDataSetChanged();
                    } else if (actionName.equals(toggle3d)) {
                        List<JSONObject> userLayersDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                        JSONObject jsonObject = userLayersDefinitions.get(item.position);
                        if (jsonObject.has(IGpLayer.LAYERDO3D_TAG)) {
                            boolean do3D = jsonObject.getBoolean(IGpLayer.LAYERDO3D_TAG);
                            jsonObject.put(IGpLayer.LAYERDO3D_TAG, !do3D);

                        } else {
                            jsonObject.put(IGpLayer.LAYERDO3D_TAG, false);
                        }
                    } else if (actionName.equals(toggleLabels)) {
                        List<JSONObject> userLayersDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                        JSONObject jsonObject = userLayersDefinitions.get(item.position);
                        if (jsonObject.has(IGpLayer.LAYERDOLABELS_TAG)) {
                            boolean doLabels = jsonObject.getBoolean(IGpLayer.LAYERDOLABELS_TAG);
                            jsonObject.put(IGpLayer.LAYERDOLABELS_TAG, !doLabels);
                        } else {
                            jsonObject.put(IGpLayer.LAYERDOLABELS_TAG, false);
                        }
                    }
                } catch (JSONException e1) {
                    GPLog.error(this, null, e1);
                }
                return true;
            });
            popup.show();
        });
        holder.itemView.setTag(mItemList.get(position));
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