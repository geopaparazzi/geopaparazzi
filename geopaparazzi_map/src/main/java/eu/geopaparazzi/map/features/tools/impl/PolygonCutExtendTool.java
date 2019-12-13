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

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.widget.Toast;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.style.ToolColors;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.map.GPMapPosition;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.features.Feature;
import eu.geopaparazzi.map.features.FeatureUtilities;
import eu.geopaparazzi.map.features.editing.EditManager;
import eu.geopaparazzi.map.features.tools.MapTool;
import eu.geopaparazzi.map.features.tools.interfaces.ToolGroup;
import eu.geopaparazzi.map.jts.MapviewPointTransformation;
import eu.geopaparazzi.map.jts.android.PointTransformation;
import eu.geopaparazzi.map.jts.android.ShapeWriter;
import eu.geopaparazzi.map.layers.ELayerTypes;
import eu.geopaparazzi.map.layers.interfaces.IEditableLayer;
import eu.geopaparazzi.map.layers.interfaces.IVectorDbLayer;
import eu.geopaparazzi.map.layers.utils.SpatialiteConnectionsHandler;
import eu.geopaparazzi.map.proj.OverlayViewProjection;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * A tool to cut or extend features.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PolygonCutExtendTool extends MapTool {

    private final Paint drawingPaintStroke = new Paint();
    private final Paint drawingPaintFill = new Paint();
    private final Paint selectedPreviewGeometryPaintStroke = new Paint();
    private final Paint selectedPreviewGeometryPaintFill = new Paint();

    private float currentX = 0;
    private float currentY = 0;
    private float lastX = -1;
    private float lastY = -1;

    private final Point tmpP = new Point();
    private final Point startP = new Point();
    private final Point endP = new Point();

    private Path drawingPath = new Path();

    private Coordinate startGeoPoint;
    private Geometry previewGeometry = null;

    /**
     * Stores the top-left map position at which the redraw should happen.
     */
    private final Point point;

    /**
     * Stores the map position after drawing is finished.
     */
    private Point positionBeforeDraw;

    // private ProgressDialog infoProgressDialog;

    private OverlayViewProjection editingViewProjection;
    private Feature startFeature;
    private Feature endFeature;
    private boolean doCut;

    /**
     * Constructor.
     *
     * @param mapView the mapview reference.
     * @param doCut   if <code>true</code>, do cut as opposed to extend.
     */
    public PolygonCutExtendTool(GPMapView mapView, boolean doCut) {
        super(mapView);
        this.doCut = doCut;
        editingViewProjection = new OverlayViewProjection(mapView, EditManager.INSTANCE.getEditingView());

        point = new Point();
        positionBeforeDraw = new Point();

        // Context context = GeopaparazziApplication.getInstance().getApplicationContext();
        // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        drawingPaintFill.setAntiAlias(true);
        drawingPaintFill.setColor(Color.RED);
        // drawingPaintFill.setAlpha(80);
        drawingPaintFill.setStyle(Paint.Style.FILL);
        drawingPaintStroke.setAntiAlias(true);
        drawingPaintStroke.setStrokeWidth(5f);
        drawingPaintStroke.setColor(Color.RED);
        drawingPaintStroke.setStyle(Paint.Style.STROKE);

        int previewStroke = ColorUtilities.toColor(ToolColors.preview_stroke.getHex());
        int previewFill = ColorUtilities.toColor(ToolColors.preview_fill.getHex());
        selectedPreviewGeometryPaintFill.setAntiAlias(true);
        selectedPreviewGeometryPaintFill.setColor(previewFill);
        selectedPreviewGeometryPaintFill.setAlpha(180);
        selectedPreviewGeometryPaintFill.setStyle(Paint.Style.FILL);
        selectedPreviewGeometryPaintStroke.setAntiAlias(true);
        selectedPreviewGeometryPaintStroke.setStrokeWidth(5f);
        selectedPreviewGeometryPaintStroke.setColor(previewStroke);
        selectedPreviewGeometryPaintStroke.setStyle(Paint.Style.STROKE);

    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(false);
    }

    public void onToolDraw(Canvas canvas) {
        if (startP.x == 0 && startP.y == 0) return;
        canvas.drawCircle(startP.x, startP.y, 15, drawingPaintFill);
        canvas.drawPath(drawingPath, drawingPaintStroke);
        canvas.drawCircle(endP.x, endP.y, 15, drawingPaintFill);

        if (previewGeometry != null) {
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

            FeatureUtilities.drawGeometry(previewGeometry, canvas, shapeWriter, selectedPreviewGeometryPaintFill, selectedPreviewGeometryPaintStroke);
        }
    }

    public boolean onToolTouchEvent(MotionEvent event) {
        if (mapView == null || mapView.isClickable()) {
            return false;
        }
        OverlayViewProjection pj = editingViewProjection;

        // handle drawing
        currentX = event.getX();
        currentY = event.getY();

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                pj.toPixels(startGeoPoint, startP);
                endP.set(startP.x, startP.y);

                drawingPath.reset();
                drawingPath.moveTo(startP.x, startP.y);

                lastX = currentX;
                lastY = currentY;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = currentX - lastX;
                float dy = currentY - lastY;
                if (abs(dx) < 1 && abs(dy) < 1) {
                    lastX = currentX;
                    lastY = currentY;
                    return true;
                }
                Coordinate currentGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                pj.toPixels(currentGeoPoint, tmpP);
                drawingPath.lineTo(tmpP.x, tmpP.y);
                endP.set(tmpP.x, tmpP.y);

                EditManager.INSTANCE.invalidateEditingView();
                break;
            case MotionEvent.ACTION_UP:

                Coordinate endGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                GeometryFactory gf = new GeometryFactory();
                Coordinate startCoord = new Coordinate(startGeoPoint.x, startGeoPoint.y);
                org.locationtech.jts.geom.Point startPoint = gf.createPoint(startCoord);
                Coordinate endCoord = new Coordinate(endGeoPoint.x, endGeoPoint.y);
                org.locationtech.jts.geom.Point endPoint = gf.createPoint(endCoord);
                Envelope env = new Envelope(startCoord, endCoord);
                select(env.getMaxY(), env.getMinX(), env.getMinY(), env.getMaxX(), startPoint, endPoint);
                //            EditManager.INSTANCE.invalidateEditingView();
                break;
        }

        return true;
    }

    public void disable() {
        if (mapView != null) {
            mapView.setClickable(true);
            mapView = null;
        }
        previewGeometry = null;
        startFeature = null;
        endFeature = null;
    }

    private void select(final double n, final double w, final double s, final double e,//
                        final org.locationtech.jts.geom.Point startPoint, final org.locationtech.jts.geom.Point endPoint) {

        IEditableLayer editLayer = EditManager.INSTANCE.getEditLayer();

        final Context context = EditManager.INSTANCE.getEditingView().getContext();
        final ProgressDialog infoProgressDialog = new ProgressDialog(context);
        infoProgressDialog.setCancelable(true);
        infoProgressDialog.setTitle(context.getString(R.string.select_title));
        infoProgressDialog.setMessage(context.getString(R.string.selecting_features));
        infoProgressDialog.setCancelable(false);
        infoProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        infoProgressDialog.setIndeterminate(true);
        infoProgressDialog.show();

        // TODO make this Asynktask right
        new AsyncTask<String, Integer, String>() {

            protected String doInBackground(String... params) {
                try {
                    double north = n;
                    double south = s;
                    if (n - s == 0) {
                        south = n - 1;
                    }
                    double west = w;
                    double east = e;
                    if (e - w == 0) {
                        west = e - 1;
                    }

                    Envelope env = new Envelope(west, east, south, north);
                    if (editLayer instanceof IVectorDbLayer) {
                        IVectorDbLayer vectorDbLayer = (IVectorDbLayer) editLayer;
                        ELayerTypes layerType = ELayerTypes.fromFileExt(vectorDbLayer.getDbPath());
                        if(layerType==ELayerTypes.SPATIALITE) {
                            ASpatialDb db = SpatialiteConnectionsHandler.INSTANCE.getDb(vectorDbLayer.getDbPath());
                            int mapSrid = LibraryConstants.SRID_WGS84_4326;
                            GeometryColumn gcol = db.getGeometryColumnsForTable(vectorDbLayer.getName());
                            env = db.reproject(env, mapSrid, gcol.srid);
                        }
                    }

                    List<Feature> features = editLayer.getFeatures(env);
                    Geometry startGeometry = null;
                    Geometry endGeometry = null;
                    for (Feature feature : features) {
                        if (startGeometry != null && endGeometry != null) break;
                        Geometry geometry = feature.getDefaultGeometry();
                        if (startGeometry == null && geometry != null && geometry.intersects(startPoint)) {
                            startGeometry = geometry;
                            startFeature = feature;
                        } else if (endGeometry == null && geometry != null && geometry.intersects(endPoint)) {
                            endGeometry = geometry;
                            endFeature = feature;
                        }
                    }

                    if (startGeometry != null && endGeometry != null) {
                        if (!doCut) {
                            previewGeometry = startGeometry.union(endGeometry);
                        } else {
                            previewGeometry = startGeometry.difference(endGeometry);
                        }
                        return "";
                    } else {
                        return "ERROR: no start or end geometry touched.";//NON-NLS
                    }
                } catch (Exception e) {
                    GPLog.error(this, null, e); //$NON-NLS-1$
                    return "ERROR: " + e.getLocalizedMessage();//NON-NLS
                }

            }

            protected void onProgressUpdate(Integer... progress) { // on UI thread!
                if (infoProgressDialog.isShowing())
                    infoProgressDialog.incrementProgressBy(progress[0]);
            }

            protected void onPostExecute(String response) { // on UI thread!
                GPDialogs.dismissProgressDialog(infoProgressDialog);
                if (response.startsWith("ERROR")) {//NON-NLS
                    GPDialogs.warningDialog(context, response, null);
                    disable();
                } else {
                    GPDialogs.toast(context, context.getString(R.string.preview_mode_save_warning), Toast.LENGTH_SHORT);

                    EditManager.INSTANCE.invalidateEditingView();

                    ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
                    if (activeToolGroup != null) {
                        activeToolGroup.onToolFinished(PolygonCutExtendTool.this);
                    }
                }
            }

        }.execute((String) null);

    }

    /**
     * Get the features as processed by the cut or extend.
     * <p/>
     * <p>The first feature is assured to have the right id and geometry to be used
     * for the db update.</p>
     *
     * @return the processed feature and the one to remove.
     */
    Feature[] getProcessedFeatures() {

        try {
            startFeature.getAttributeValues().set(startFeature.getGeometryIndex(), previewGeometry);
            return new Feature[]{startFeature, endFeature}; // new geom feature + feature to remove
        } catch (Exception e) {
            final Context context = EditManager.INSTANCE.getEditingView().getContext();
            String msg = context.getString(R.string.unable_write_geometry) + (previewGeometry == null ? "." : ": " + previewGeometry.toText());
            GPLog.error(this, msg, e);
            GPDialogs.warningDialog(context, msg, null);
            return null;
        }
    }

    @Override
    public void onViewChanged() {
        // ignore
    }
}
