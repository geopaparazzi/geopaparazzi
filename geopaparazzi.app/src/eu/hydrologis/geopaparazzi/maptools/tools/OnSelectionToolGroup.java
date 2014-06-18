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
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maps.SliderDrawView;
import eu.hydrologis.geopaparazzi.maptools.core.MapTool;

/**
 * The group of tools active when a selection has been done.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OnSelectionToolGroup implements ToolGroup, OnClickListener, OnTouchListener {

    private LinearLayout parent;
    private SliderDrawView sliderDrawView;
    private MapView mapView;

    private MapTool activeTool = null;
    private int selectionColor;
    private List<Feature> selectedFeatures;
    private ImageButton deleteFeatureButton;

    /**
     * Constructor.
     * 
     * @param parent the view into which to place the UI parts.
     * @param sliderDrawView the draw view.
     * @param mapView the map view.
     * @param selectedFeatures the set of selected features.
     */
    public OnSelectionToolGroup( LinearLayout parent, SliderDrawView sliderDrawView, MapView mapView,
            List<Feature> selectedFeatures ) {
        this.parent = parent;
        this.sliderDrawView = sliderDrawView;
        this.mapView = mapView;
        this.selectedFeatures = selectedFeatures;

        selectionColor = parent.getContext().getResources().getColor(R.color.main_selection);
    }

    public void setToolUI() {
        parent.removeAllViews();

        Context context = parent.getContext();
        ILayer editLayer = EditManager.INSTANCE.getEditLayer();
        int padding = 2;

        if (editLayer != null) {
            deleteFeatureButton = new ImageButton(context);
            deleteFeatureButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            deleteFeatureButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_delete_feature));
            deleteFeatureButton.setPadding(0, padding, 0, padding);
            parent.addView(deleteFeatureButton);

            ImageButton editAttributesButton = new ImageButton(context);
            editAttributesButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            editAttributesButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_view_attributes));
            editAttributesButton.setPadding(0, padding, 0, padding);
            parent.addView(editAttributesButton);

            ImageButton undoButton = new ImageButton(context);
            undoButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            undoButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_undo));
            undoButton.setPadding(0, padding, 0, padding);
            parent.addView(undoButton);

            ImageButton commitButton = new ImageButton(context);
            commitButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            commitButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_commit));
            commitButton.setPadding(0, padding, 0, padding);
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
        if (v == deleteFeatureButton) {

            // TODO
            System.out.println(selectedFeatures);

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
