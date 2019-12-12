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
package eu.geopaparazzi.map.features.tools.impl;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.style.ToolColors;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.map.GPMapPosition;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.features.FeatureUtilities;
import eu.geopaparazzi.map.features.editing.EditManager;
import eu.geopaparazzi.map.features.editing.EditingView;
import eu.geopaparazzi.map.features.tools.interfaces.Tool;
import eu.geopaparazzi.map.features.tools.interfaces.ToolGroup;
import eu.geopaparazzi.map.jts.JtsUtilities;
import eu.geopaparazzi.map.jts.MapviewPointTransformation;
import eu.geopaparazzi.map.jts.android.PointTransformation;
import eu.geopaparazzi.map.jts.android.ShapeWriter;
import eu.geopaparazzi.map.layers.interfaces.IEditableLayer;
import eu.geopaparazzi.map.proj.OverlayViewProjection;

import static java.lang.Math.round;

/**
 * The group of tools active when a selection has been done.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PolygonCreateFeatureToolGroup implements ToolGroup, OnClickListener, OnTouchListener, View.OnLongClickListener {

    private final GPMapView mapView;

    private int buttonSelectionColor;

    private List<Coordinate> coordinatesList = new ArrayList<>();
    private ImageButton addVertexButton;
    private OverlayViewProjection editingViewProjection;

    private final Paint createdGeometryPaintHaloStroke = new Paint();
    private final Paint createdGeometryPaintStroke = new Paint();
    private final Paint createdGeometryPaintFill = new Paint();

    /**
     * Stores the top-left map position at which the redraw should happen.
     */
    private final Point point;

    /**
     * Stores the map position after drawing is finished.
     */
    private Point positionBeforeDraw;
    private ImageButton gpsStreamButton;

    private ImageButton commitButton;

    private ImageButton undoButton;

    private Geometry polygonGeometry;

    private boolean firstInvalid = true;

    private boolean gpsStreamActive = false;

    private ImageButton addVertexByTapButton;
    private boolean addVertexByTapActive = false;
    private Coordinate lastKnownGpsCoordinate;

    /**
     * Constructor.
     *
     * @param mapView the map view.
     */
    public PolygonCreateFeatureToolGroup(GPMapView mapView) {
        this.mapView = mapView;

        EditingView editingView = EditManager.INSTANCE.getEditingView();
        editingViewProjection = new OverlayViewProjection(mapView, editingView);
        buttonSelectionColor = Compat.getColor(editingView.getContext(), R.color.main_selection);

        int selectionStroke = ColorUtilities.toColor(ToolColors.selection_stroke.getHex());
        createdGeometryPaintFill.setAntiAlias(true);
        createdGeometryPaintFill.setColor(selectionStroke);
        createdGeometryPaintFill.setAlpha(180);
        createdGeometryPaintFill.setStyle(Paint.Style.FILL);

        createdGeometryPaintHaloStroke.setAntiAlias(true);
        createdGeometryPaintHaloStroke.setStrokeWidth(7f);
        createdGeometryPaintHaloStroke.setColor(Color.BLACK);
        createdGeometryPaintHaloStroke.setStyle(Paint.Style.STROKE);

        createdGeometryPaintStroke.setAntiAlias(true);
        createdGeometryPaintStroke.setStrokeWidth(5f);
        createdGeometryPaintStroke.setColor(selectionStroke);
        createdGeometryPaintStroke.setStyle(Paint.Style.STROKE);

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
        IEditableLayer editLayer = EditManager.INSTANCE.getEditLayer();
        int padding = 2;

        if (editLayer != null) {
            gpsStreamButton = new ImageButton(context);
            gpsStreamButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            gpsStreamButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_gps_stream_24dp));
            gpsStreamButton.setPadding(0, padding, 0, padding);
            gpsStreamButton.setOnTouchListener(this);
            gpsStreamButton.setOnLongClickListener(this);
            gpsStreamButton.setOnClickListener(this);
            parent.addView(gpsStreamButton);

            addVertexButton = new ImageButton(context);
            addVertexButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            addVertexButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_add_vertex_24dp));
            addVertexButton.setPadding(0, padding, 0, padding);
            addVertexButton.setOnTouchListener(this);
            addVertexButton.setOnClickListener(this);
            parent.addView(addVertexButton);

            addVertexByTapButton = new ImageButton(context);
            addVertexByTapButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            addVertexByTapButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_add_vertex_tap_24dp));
            addVertexByTapButton.setPadding(0, padding, 0, padding);
            addVertexByTapButton.setOnTouchListener(this);
            addVertexByTapButton.setOnClickListener(this);
            parent.addView(addVertexByTapButton);

            undoButton = new ImageButton(context);
            undoButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            undoButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_undo_24dp));
            undoButton.setPadding(0, padding, 0, padding);
            undoButton.setOnTouchListener(this);
            undoButton.setOnClickListener(this);
            parent.addView(undoButton);

            commitButton = new ImageButton(context);
            commitButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            commitButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_commit_24dp));
            commitButton.setPadding(0, padding, 0, padding);
            commitButton.setOnTouchListener(this);
            commitButton.setOnClickListener(this);
            commitButton.setVisibility(View.GONE);
            parent.addView(commitButton);
        }
    }

    public void disable() {
        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        if (parent != null)
            parent.removeAllViews();
        parent = null;
        gpsStreamActive = false;
        addVertexByTapActive = false;
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == gpsStreamButton) {
            gpsStreamActive = !gpsStreamActive;
            addVertexByTapActive = false;
        }
        EditManager.INSTANCE.invalidateEditingView();
        handleToolIcons(v);
        return true;
    }

    public void onClick(View v) {
        if (v == addVertexButton) {
            // adds point in map center
            gpsStreamActive = false;
            addVertexByTapActive = false;
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GPApplication.getInstance());
            double[] mapCenter = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);

            Coordinate coordinate = new Coordinate(mapCenter[0], mapCenter[1]);

            if (addVertex(v.getContext(), coordinate)) {
                return;
            }
        } else if (v == gpsStreamButton) {
            addVertexByTapActive = false;
            addGpsCoordinate();
        } else if (v == addVertexByTapButton) {
            addVertexByTapActive = !addVertexByTapActive;
            gpsStreamActive = false;
        } else if (v == commitButton) {
            if (coordinatesList.size() > 2) {
                List<Geometry> geomsList = new ArrayList<>();
                Polygon polygonGeometry = JtsUtilities.createPolygon(coordinatesList);
                if (polygonGeometry.isValid()) {
                    geomsList.add(polygonGeometry);
                } else {
                    try {
                        Geometry polygonSplit = FeatureUtilities.invalidPolygonSplit(polygonGeometry);
                        for (int i = 0; i < polygonSplit.getNumGeometries(); i++) {
                            geomsList.add(polygonSplit.getGeometryN(i));
                        }
                    } catch (Exception e) {
                        GPLog.error(this, null, e);
                        // just clean it up through buffer
                        Geometry buffer = polygonGeometry.buffer(0);
                        geomsList.add(buffer);
                    }
                }

                IEditableLayer editLayer = EditManager.INSTANCE.getEditLayer();
                try {
                    for (Geometry geometry : geomsList) {
                        editLayer.addNewFeatureByGeometry(geometry, LibraryConstants.SRID_WGS84_4326);
                    }
                    GPDialogs.toast(commitButton.getContext(), commitButton.getContext().getString(R.string.geometry_saved), Toast.LENGTH_SHORT);
                    coordinatesList.clear();

                    /*
                     * TODO open the table view to insert data
                     */

                } catch (Exception e) {
                    if (e.getMessage().contains("UNIQUE constraint failed")) { //NON-NLS
                        GPDialogs.warningDialog(commitButton.getContext(), commitButton.getContext().getString(R.string.unique_constraint_violation_message), null);
                        coordinatesList.clear();
                        this.polygonGeometry = null;
                    } else {
                        GPLog.error(this, null, e);
                    }
                }
            }
        } else if (v == undoButton) {
            if (coordinatesList.size() == 0) {
                EditManager.INSTANCE.setActiveToolGroup(new PolygonMainEditingToolGroup(mapView));
                EditManager.INSTANCE.setActiveTool(null);
                return;
            } else if (coordinatesList.size() > 0) {
                int size = coordinatesList.size() - 1;
                coordinatesList.remove(size);
                // commitButton.setVisibility(View.VISIBLE);
                reCreateGeometry(v.getContext());
            }
        }
        if (coordinatesList.size() > 2) {
            commitButton.setVisibility(View.VISIBLE);
        } else {
            commitButton.setVisibility(View.GONE);
        }
        EditManager.INSTANCE.invalidateEditingView();
        handleToolIcons(v);
    }

    private boolean addVertex(Context context, Coordinate coordinate) {
        coordinatesList.add(coordinate);
        int coordinatesCount = coordinatesList.size();
        if (coordinatesCount == 0) {
            return true;
        }
        reCreateGeometry(context);
        return false;
    }

    private void reCreateGeometry(Context context) {
        polygonGeometry = null;
        if (coordinatesList.size() > 2) {
            polygonGeometry = JtsUtilities.createPolygon(coordinatesList);
            if (!polygonGeometry.isValid() && firstInvalid) {
                IEditableLayer editLayer = EditManager.INSTANCE.getEditLayer();
                EGeometryType geometryType = editLayer.getGeometryType();
                if (!geometryType.toSpatialiteGeometryType().isGeometryCompatible(polygonGeometry)) {
                    GPDialogs.warningDialog(context, context.getString(R.string.selfintersection_message), null);
                }
                firstInvalid = false;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleToolIcons(View activeToolButton) {
        Context context = activeToolButton.getContext();
        if (gpsStreamButton != null)
            if (gpsStreamActive) {
                gpsStreamButton
                        .setBackground(Compat.getDrawable(context, R.drawable.ic_editing_gps_stream_active_24dp));
            } else {
                gpsStreamButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_gps_stream_24dp));
                gpsStreamButton.getBackground().clearColorFilter();
            }
        if (addVertexByTapButton != null)
            if (addVertexByTapActive) {
                addVertexByTapButton.setBackground(Compat.getDrawable(context,
                        R.drawable.ic_editing_add_vertex_tap_active_24dp));
            } else {
                addVertexByTapButton.setBackground(Compat.getDrawable(context,
                        R.drawable.ic_editing_add_vertex_tap_24dp));
            }

    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
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

    public void onToolFinished(Tool tool) {
        // nothing
    }

    public void onToolDraw(Canvas canvas) {
        try {

            OverlayViewProjection projection = editingViewProjection;

            byte zoomLevelBeforeDraw;
            synchronized (mapView) {
                zoomLevelBeforeDraw = (byte) mapView.getMapPosition().getZoomLevel();
                positionBeforeDraw = projection.toPoint(mapView.getMapPosition().getCoordinate(), positionBeforeDraw,
                        zoomLevelBeforeDraw);
            }

            // calculate the top-left point of the visible rectangle
            point.x = positionBeforeDraw.x - (canvas.getWidth() >> 1);
            point.y = positionBeforeDraw.y - (canvas.getHeight() >> 1);

            GPMapPosition mapPosition = mapView.getMapPosition();
            byte zoomLevel = (byte) mapPosition.getZoomLevel();

            PointTransformation pointTransformer = new MapviewPointTransformation(projection, point, zoomLevel);
            ShapeWriter shapeWriter = new ShapeWriter(pointTransformer);
            shapeWriter.setRemoveDuplicatePoints(true);
            // shapeWriter.setDecimation(spatialTable.getStyle().decimationFactor);

            // draw features
            if (polygonGeometry != null) {
                FeatureUtilities.drawGeometry(polygonGeometry, canvas, shapeWriter, createdGeometryPaintFill,
                        createdGeometryPaintHaloStroke);
                FeatureUtilities.drawGeometry(polygonGeometry, canvas, shapeWriter, null, createdGeometryPaintStroke);
            }

            final PointF vertexPoint = new PointF();
            if (coordinatesList.size() == 2) {
                final PointF vertexPoint2 = new PointF();
                pointTransformer.transform(coordinatesList.get(0), vertexPoint);
                pointTransformer.transform(coordinatesList.get(1), vertexPoint2);
                canvas.drawLine(vertexPoint.x, vertexPoint.y, vertexPoint2.x, vertexPoint2.y, createdGeometryPaintHaloStroke);
                canvas.drawLine(vertexPoint.x, vertexPoint.y, vertexPoint2.x, vertexPoint2.y, createdGeometryPaintStroke);
            }

            for (Coordinate vertexCoordinate : coordinatesList) {
                pointTransformer.transform(vertexCoordinate, vertexPoint);
                canvas.drawCircle(vertexPoint.x, vertexPoint.y, 10f, createdGeometryPaintHaloStroke);
                canvas.drawCircle(vertexPoint.x, vertexPoint.y, 10f, createdGeometryPaintStroke);
            }

        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    public boolean onToolTouchEvent(MotionEvent event) {
        if (addVertexByTapActive) {
            if (mapView == null) {
                return false;
            }

            float currentX = event.getX();
            float currentY = event.getY();

            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                Coordinate coordinate = editingViewProjection.fromPixels(round(currentX), round(currentY));
                addVertex(mapView.getContext(), coordinate);
                if (coordinatesList.size() > 2) {
                    commitButton.setVisibility(View.VISIBLE);
                } else {
                    commitButton.setVisibility(View.GONE);
                }
                EditManager.INSTANCE.invalidateEditingView();
                return true;
            }
        }
        return false;
    }

    public void onGpsUpdate(double lon, double lat) {
        lastKnownGpsCoordinate = new Coordinate(lon, lat);
        if (gpsStreamActive) {
            addGpsCoordinate();
        }
    }

    private void addGpsCoordinate() {
        if (lastKnownGpsCoordinate != null) {
            addVertex(mapView.getContext(), lastKnownGpsCoordinate);

            if (coordinatesList.size() > 2) {
                commitButton.setVisibility(View.VISIBLE);
            } else {
                commitButton.setVisibility(View.GONE);
            }
            EditManager.INSTANCE.invalidateEditingView();
        } else {
            EditingView editingView = EditManager.INSTANCE.getEditingView();
            Context context = editingView.getContext();
            GPDialogs.warningDialog(context, context.getString(R.string.no_gps_coordinate_acquired_yet), null);
        }
    }


}