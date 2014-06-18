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
import eu.geopaparazzi.library.features.ToolGroup;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maps.SliderDrawView;
import eu.hydrologis.geopaparazzi.maptools.core.MapTool;

/**
 * The main editing tool, which just shows the tool palette.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MainEditingToolGroup implements ToolGroup, OnClickListener, OnTouchListener {

    private LinearLayout parent;
    private ImageButton selectAllButton;
    private SliderDrawView sliderDrawView;
    private MapView mapView;

    private MapTool activeTool = null;
    private ImageButton selectEditableButton;
    private int selectionColor;

    /**
     * Constructor.
     * 
     * @param parent the view into which to place the UI parts.
     * @param sliderDrawView the draw view.
     * @param mapView the map view.
     */
    public MainEditingToolGroup( LinearLayout parent, SliderDrawView sliderDrawView, MapView mapView ) {
        this.parent = parent;
        this.sliderDrawView = sliderDrawView;
        this.mapView = mapView;

        selectionColor = parent.getContext().getResources().getColor(R.color.main_selection);
    }

    public void setToolUI() {

        Context context = parent.getContext();
        ILayer editLayer = EditManager.INSTANCE.getEditLayer();

        if (editLayer != null) {
            ImageButton cutButton = new ImageButton(context);
            cutButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            cutButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_cut));
            parent.addView(cutButton);

            ImageButton extendButton = new ImageButton(context);
            extendButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            extendButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_extend));
            parent.addView(extendButton);

            ImageButton createFeatureButton = new ImageButton(context);
            createFeatureButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            createFeatureButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_create_polygon));
            parent.addView(createFeatureButton);

            selectEditableButton = new ImageButton(context);
            selectEditableButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            selectEditableButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_select_editable));
            selectEditableButton.setOnClickListener(this);
            parent.addView(selectEditableButton);
        }

        selectAllButton = new ImageButton(context);
        selectAllButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        selectAllButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_select_all));
        selectAllButton.setOnClickListener(this);
        selectAllButton.setOnTouchListener(this);
        parent.addView(selectAllButton);

        if (editLayer != null) {
            ImageButton undoButton = new ImageButton(context);
            undoButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            undoButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_undo));
            parent.addView(undoButton);

            ImageButton commitButton = new ImageButton(context);
            commitButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            commitButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_commit));
            parent.addView(commitButton);
        }
    }

    public void disableTools() {
        if (activeTool != null) {
            sliderDrawView.disableTool();
            activeTool.disable();
            activeTool = null;
        }
    }

    public void disable() {
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
                    Utilities.messageDialog(parent.getContext(), R.string.no_queriable_layer_is_visible, null);
                    return;
                }
            } catch (jsqlite.Exception e) {
                GPLog.error(this, null, e);
            }

            activeTool = new InfoTool(sliderDrawView, mapView);
            sliderDrawView.enableTool(activeTool);
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
}
