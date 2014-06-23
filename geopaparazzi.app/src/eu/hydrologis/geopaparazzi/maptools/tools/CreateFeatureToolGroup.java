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

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewPosition;
import org.mapsforge.android.maps.Projection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.vividsolutions.jts.android.PointTransformation;
import com.vividsolutions.jts.android.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.EditingView;
import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.library.features.Tool;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maps.overlays.MapsforgePointTransformation;
import eu.hydrologis.geopaparazzi.maps.overlays.SliderDrawProjection;
import eu.hydrologis.geopaparazzi.maptools.FeatureUtilities;

/**
 * The group of tools active when a selection has been done.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CreateFeatureToolGroup implements ToolGroup, OnClickListener, OnTouchListener {

    private MapView mapView;

    private int buttonSelectionColor;

    private Geometry buildingGeometry;
    private ImageButton addVertexButton;
    private SliderDrawProjection sliderDrawProjection;

    private final Paint selectedGeometryPaintStroke = new Paint();
    private final Paint selectedGeometryPaintFill = new Paint();

    /**
     * Stores the top-left map position at which the redraw should happen.
     */
    private final Point point;

    /**
     * Stores the map position after drawing is finished.
     */
    private Point positionBeforeDraw;
    private ImageButton gpsStreamButton;

    /**
     * Constructor.
     * 
     * @param mapView the map view.
     */
    public CreateFeatureToolGroup( MapView mapView ) {
        this.mapView = mapView;

        EditingView editingView = EditManager.INSTANCE.getEditingView();
        sliderDrawProjection = new SliderDrawProjection(mapView, editingView);
        buttonSelectionColor = editingView.getContext().getResources().getColor(R.color.main_selection);

        selectedGeometryPaintFill.setAntiAlias(true);
        selectedGeometryPaintFill.setColor(Color.RED);
        selectedGeometryPaintFill.setAlpha(180);
        selectedGeometryPaintFill.setStyle(Paint.Style.FILL);
        selectedGeometryPaintStroke.setAntiAlias(true);
        selectedGeometryPaintStroke.setStrokeWidth(5f);
        selectedGeometryPaintStroke.setColor(Color.YELLOW);
        selectedGeometryPaintStroke.setStyle(Paint.Style.STROKE);

        point = new Point();
        positionBeforeDraw = new Point();
    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(true);
    }

    public void initUI() {
        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        parent.removeAllViews();

        Context context = parent.getContext();
        ILayer editLayer = EditManager.INSTANCE.getEditLayer();
        int padding = 2;

        if (editLayer != null) {
            addVertexButton = new ImageButton(context);
            addVertexButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            addVertexButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_add_vertex));
            addVertexButton.setPadding(0, padding, 0, padding);
            addVertexButton.setOnTouchListener(this);
            addVertexButton.setOnClickListener(this);
            parent.addView(addVertexButton);

            gpsStreamButton = new ImageButton(context);
            gpsStreamButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            gpsStreamButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_gps_stream));
            gpsStreamButton.setPadding(0, padding, 0, padding);
            gpsStreamButton.setOnTouchListener(this);
            gpsStreamButton.setOnClickListener(this);
            parent.addView(gpsStreamButton);

            ImageButton undoButton = new ImageButton(context);
            undoButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            undoButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_undo));
            undoButton.setPadding(0, padding, 0, padding);
            undoButton.setOnTouchListener(this);
            undoButton.setOnClickListener(this);
            parent.addView(undoButton);

            ImageButton commitButton = new ImageButton(context);
            commitButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            commitButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_commit));
            commitButton.setPadding(0, padding, 0, padding);
            commitButton.setOnTouchListener(this);
            commitButton.setOnClickListener(this);
            parent.addView(commitButton);
        }
    }

    public void disable() {
        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        if (parent != null)
            parent.removeAllViews();
        parent = null;
    }

    public void onClick( View v ) {
        // if (v == gpsStreamButton) {
        // if (selectedFeatures.size() > 0) {
        // Context context = v.getContext();
        // Intent intent = new Intent(context, FeaturePagerActivity.class);
        // intent.putParcelableArrayListExtra(FeatureUtilities.KEY_FEATURESLIST,
        // (ArrayList< ? extends Parcelable>) selectedFeatures);
        // intent.putExtra(FeatureUtilities.KEY_READONLY, false);
        // context.startActivity(intent);
        // }
        // }
    }

    public boolean onTouch( View v, MotionEvent event ) {
        switch( event.getAction() ) {
        case MotionEvent.ACTION_DOWN: {
            v.getBackground().setColorFilter(buttonSelectionColor, Mode.SRC_ATOP);
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
        // nothing
    }

    public void onToolDraw( Canvas canvas ) {
        try {
            if (buildingGeometry != null) {
                // int centerX = canvas.getWidth() / 2;
                // int centerY = canvas.getHeight() / 2;
                // Point drawPosition = new Point(centerX, centerY);
                Projection projection = sliderDrawProjection;

                byte zoomLevelBeforeDraw;
                synchronized (mapView) {
                    zoomLevelBeforeDraw = mapView.getMapPosition().getZoomLevel();
                    positionBeforeDraw = projection.toPoint(mapView.getMapPosition().getMapCenter(), positionBeforeDraw,
                            zoomLevelBeforeDraw);
                }

                // calculate the top-left point of the visible rectangle
                point.x = positionBeforeDraw.x - (canvas.getWidth() >> 1);
                point.y = positionBeforeDraw.y - (canvas.getHeight() >> 1);

                MapViewPosition mapPosition = mapView.getMapPosition();
                byte zoomLevel = mapPosition.getZoomLevel();

                PointTransformation pointTransformer = new MapsforgePointTransformation(projection, point, zoomLevel);
                ShapeWriter shapeWriter = new ShapeWriter(pointTransformer);
                shapeWriter.setRemoveDuplicatePoints(true);
                // shapeWriter.setDecimation(spatialTable.getStyle().decimationFactor);

                // draw features
                FeatureUtilities.drawGeometry(buildingGeometry, canvas, shapeWriter, selectedGeometryPaintFill,
                        selectedGeometryPaintStroke);
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    public boolean onToolTouchEvent( MotionEvent event ) {
        return false;
    }

}
