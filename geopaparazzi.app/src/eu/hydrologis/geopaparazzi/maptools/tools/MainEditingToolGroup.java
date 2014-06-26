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
package eu.hydrologis.geopaparazzi.maptools.tools;

import java.util.List;

import org.mapsforge.android.maps.MapView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.library.features.Tool;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.hydrologis.geopaparazzi.R;

/**
 * The main editing tool, which just shows the tool palette.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MainEditingToolGroup implements ToolGroup, OnClickListener, OnTouchListener {

    private ImageButton selectAllButton;
    private MapView mapView;

    private ImageButton selectEditableButton;
    private int selectionColor;
    private ImageButton createFeatureButton;
    private ImageButton cutButton;
    private ImageButton extendButton;

    /**
     * Constructor.
     * 
     * @param mapView the map view.
     */
    public MainEditingToolGroup( MapView mapView ) {
        this.mapView = mapView;

        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        selectionColor = parent.getContext().getResources().getColor(R.color.main_selection);
    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(true);
    }

    public void initUI() {

        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        Context context = parent.getContext();
        ILayer editLayer = EditManager.INSTANCE.getEditLayer();
        int padding = 2;

        if (editLayer != null) {
            cutButton = new ImageButton(context);
            cutButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            cutButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_cut));
            cutButton.setPadding(0, padding, 0, padding);
            cutButton.setOnClickListener(this);
            cutButton.setOnTouchListener(this);
            parent.addView(cutButton);

            extendButton = new ImageButton(context);
            extendButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            extendButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_extend));
            extendButton.setPadding(0, padding, 0, padding);
            extendButton.setOnClickListener(this);
            extendButton.setOnTouchListener(this);
            parent.addView(extendButton);

            createFeatureButton = new ImageButton(context);
            createFeatureButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            createFeatureButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_create_polygon));
            createFeatureButton.setPadding(0, padding, 0, padding);
            createFeatureButton.setOnClickListener(this);
            createFeatureButton.setOnTouchListener(this);
            parent.addView(createFeatureButton);

            selectEditableButton = new ImageButton(context);
            selectEditableButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            selectEditableButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_select_editable));
            selectEditableButton.setPadding(0, padding, 0, padding);
            selectEditableButton.setOnClickListener(this);
            selectEditableButton.setOnTouchListener(this);
            parent.addView(selectEditableButton);
        }

        selectAllButton = new ImageButton(context);
        selectAllButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        selectAllButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_select_all));
        selectAllButton.setPadding(0, padding, 0, padding);
        selectAllButton.setOnClickListener(this);
        selectAllButton.setOnTouchListener(this);
        parent.addView(selectAllButton);

    }

    public void disable() {
        EditManager.INSTANCE.setActiveTool(null);
        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        if (parent != null)
            parent.removeAllViews();
        parent = null;
    }

    public void onClick( View v ) {
        if (v == selectAllButton) {
            // check maps enablement
            try {
                final SpatialDatabasesManager sdbManager = SpatialDatabasesManager.getInstance();
                final List<SpatialVectorTable> spatialTables = sdbManager.getSpatialVectorTables(false);
                boolean atLeastOneEnabled = false;
                for( SpatialVectorTable spatialVectorTable : spatialTables ) {
                    if (spatialVectorTable.getStyle().enabled == 1) {
                        atLeastOneEnabled = true;
                        break;
                    }
                }
                if (!atLeastOneEnabled) {
                    LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
                    if (parent != null)
                        Utilities.messageDialog(parent.getContext(), R.string.no_queriable_layer_is_visible, null);
                    return;
                }
            } catch (jsqlite.Exception e) {
                GPLog.error(this, null, e);
            }

            Tool activeTool = new InfoTool(this, mapView);
            EditManager.INSTANCE.setActiveTool(activeTool);
        } else if (v == selectEditableButton) {
            Tool activeTool = new SelectionTool(mapView);
            EditManager.INSTANCE.setActiveTool(activeTool);
        } else if (v == createFeatureButton) {
            ToolGroup createFeatureToolGroup = new CreateFeatureToolGroup(mapView);
            EditManager.INSTANCE.setActiveToolGroup(createFeatureToolGroup);
        } else if (v == cutButton) {
            Tool activeTool = new CutExtendTool(mapView, true);
            EditManager.INSTANCE.setActiveTool(activeTool);
        } else if (v == extendButton) {
            Tool activeTool = new CutExtendTool(mapView, false);
            EditManager.INSTANCE.setActiveTool(activeTool);
        }
        handleToolIcons(v);
    }

    @SuppressWarnings("deprecation")
    private void handleToolIcons( View activeToolButton ) {
        Context context = activeToolButton.getContext();
        if (activeToolButton == selectEditableButton) {
            selectEditableButton.setBackgroundDrawable(context.getResources().getDrawable(
                    R.drawable.ic_editing_select_editable_active));
        } else {
            selectEditableButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_select_editable));
        }
        if (activeToolButton == selectAllButton) {
            selectAllButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_select_all_active));
        } else {
            selectAllButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_select_all));
        }
        if (activeToolButton == cutButton) {
            cutButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_cut_active));
        } else {
            cutButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_cut));
        }
        if (activeToolButton == extendButton) {
            extendButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_extend_active));
        } else {
            extendButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_extend));
        }

    }

    public boolean onTouch( View v, MotionEvent event ) {
        switch( event.getAction() ) {
        case MotionEvent.ACTION_DOWN: {
            v.getBackground().setColorFilter(selectionColor, Mode.SRC_ATOP);
            v.invalidate();
            break;
        }
        case MotionEvent.ACTION_UP: {
            v.getBackground().clearColorFilter();
            v.invalidate();
            break;
        }
        }
        return false;
    }

    public void onToolFinished( Tool tool ) {
        // if (activeTool == null) {
        // return;
        // }
        // if (tool == activeTool) {
        // sliderDrawView.disableTool();
        // activeTool.disable();
        // activeTool = null;
        // }
    }

    public void onToolDraw( Canvas canvas ) {
        // nothing to draw
    }

    public boolean onToolTouchEvent( MotionEvent event ) {
        return false;
    }
}
