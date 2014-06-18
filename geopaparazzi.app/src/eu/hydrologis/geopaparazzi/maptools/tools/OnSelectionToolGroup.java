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
import com.vividsolutions.jts.android.geom.DrawableShape;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.library.features.Tool;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryType;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maps.SliderDrawView;
import eu.hydrologis.geopaparazzi.maps.overlays.MapsforgePointTransformation;
import eu.hydrologis.geopaparazzi.maps.overlays.SliderDrawProjection;
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
    private int buttonSelectionColor;
    private List<Feature> selectedFeatures;
    private ImageButton deleteFeatureButton;
    private SliderDrawProjection sliderDrawProjection;

    private final Paint selectedGeometryPaintStroke = new Paint();
    private final Paint selectedGeometryPaintFill = new Paint();

    private WKBReader wkbReader = new WKBReader();

    /**
     * Stores the top-left map position at which the redraw should happen.
     */
    private final Point point;

    /**
     * Stores the map position after drawing is finished.
     */
    private Point positionBeforeDraw;

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

        sliderDrawProjection = new SliderDrawProjection(mapView, sliderDrawView);
        buttonSelectionColor = parent.getContext().getResources().getColor(R.color.main_selection);

        selectedGeometryPaintFill.setAntiAlias(true);
        selectedGeometryPaintFill.setColor(Color.RED);
        selectedGeometryPaintFill.setStyle(Paint.Style.FILL);
        selectedGeometryPaintStroke.setAntiAlias(true);
        selectedGeometryPaintStroke.setStrokeWidth(3f);
        selectedGeometryPaintStroke.setColor(Color.YELLOW);
        selectedGeometryPaintStroke.setStyle(Paint.Style.STROKE);

        point = new Point();
        positionBeforeDraw = new Point();
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
            deleteFeatureButton.setOnTouchListener(this);
            parent.addView(deleteFeatureButton);

            ImageButton editAttributesButton = new ImageButton(context);
            editAttributesButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            editAttributesButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_view_attributes));
            editAttributesButton.setPadding(0, padding, 0, padding);
            editAttributesButton.setOnTouchListener(this);
            parent.addView(editAttributesButton);

            ImageButton undoButton = new ImageButton(context);
            undoButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            undoButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_undo));
            undoButton.setPadding(0, padding, 0, padding);
            undoButton.setOnTouchListener(this);
            parent.addView(undoButton);

            ImageButton commitButton = new ImageButton(context);
            commitButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            commitButton.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_editing_commit));
            commitButton.setPadding(0, padding, 0, padding);
            commitButton.setOnTouchListener(this);
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

            activeTool = new InfoTool(this, sliderDrawView, mapView);
            sliderDrawView.enableTool(activeTool);
        }
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
            if (selectedFeatures != null && selectedFeatures.size() > 0) {
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
                // GeoPoint mapCenter = mapPosition.getMapCenter();

                PointTransformation pointTransformer = new MapsforgePointTransformation(projection, point, zoomLevel);
                ShapeWriter shapeWriter = new ShapeWriter(pointTransformer);
                shapeWriter.setRemoveDuplicatePoints(true);
                // shapeWriter.setDecimation(spatialTable.getStyle().decimationFactor);

                // draw features
                for( Feature feature : selectedFeatures ) {
                    byte[] defaultGeometry = feature.getDefaultGeometry();
                    Geometry geometry = wkbReader.read(defaultGeometry);
                    drawGeometry(geometry, canvas, shapeWriter);
                }
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    private void drawGeometry( Geometry geom, Canvas canvas, ShapeWriter shapeWriter ) {
        String geometryTypeStr = geom.getGeometryType();
        int geometryTypeInt = GeometryType.forValue(geometryTypeStr);
        GeometryType geometryType = GeometryType.forValue(geometryTypeInt);
        DrawableShape shape = shapeWriter.toShape(geom);
        switch( geometryType ) {
        // case POINT_XY:
        // case POINT_XYM:
        // case POINT_XYZ:
        // case POINT_XYZM:
        // case MULTIPOINT_XY:
        // case MULTIPOINT_XYM:
        // case MULTIPOINT_XYZ:
        // case MULTIPOINT_XYZM: {
        // if (selectedGeometryPaintFill != null)
        // shape.fill(canvas, selectedGeometryPaintFill);
        // if (selectedGeometryPaintStroke != null)
        // shape.draw(canvas, selectedGeometryPaintStroke);
        // //
        // GPLog.androidLog(-1,"GeopaparazziOverlay.drawGeometry geometry_type["+s_geometry_type+"]: ["+i_geometry_type+"]");
        // }
        // break;
        // case LINESTRING_XY:
        // case LINESTRING_XYM:
        // case LINESTRING_XYZ:
        // case LINESTRING_XYZM:
        // case MULTILINESTRING_XY:
        // case MULTILINESTRING_XYM:
        // case MULTILINESTRING_XYZ:
        // case MULTILINESTRING_XYZM: {
        // if (selectedGeometryPaintStroke != null)
        // shape.draw(canvas, selectedGeometryPaintStroke);
        // }
        // break;
        case POLYGON_XY:
        case POLYGON_XYM:
        case POLYGON_XYZ:
        case POLYGON_XYZM:
        case MULTIPOLYGON_XY:
        case MULTIPOLYGON_XYM:
        case MULTIPOLYGON_XYZ:
        case MULTIPOLYGON_XYZM: {
            if (selectedGeometryPaintFill != null)
                shape.fill(canvas, selectedGeometryPaintFill);
            if (selectedGeometryPaintStroke != null)
                shape.draw(canvas, selectedGeometryPaintStroke);
        }
            break;
        default:
            break;
        }
    }

    public boolean onToolTouchEvent( MotionEvent event ) {
        // TODO Auto-generated method stub
        return false;
    }

}
