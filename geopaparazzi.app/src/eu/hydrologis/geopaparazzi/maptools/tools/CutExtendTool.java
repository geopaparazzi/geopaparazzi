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

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.AsyncTask;
import android.view.MotionEvent;

import com.vividsolutions.jts.android.PointTransformation;
import com.vividsolutions.jts.android.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewPosition;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import java.util.List;

import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.layers.SpatialVectorTableLayer;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteUtilities;
import eu.hydrologis.geopaparazzi.maps.overlays.MapsforgePointTransformation;
import eu.hydrologis.geopaparazzi.maps.overlays.SliderDrawProjection;
import eu.hydrologis.geopaparazzi.maptools.FeatureUtilities;
import eu.hydrologis.geopaparazzi.maptools.core.MapTool;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * A tool to cut or extend features.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CutExtendTool extends MapTool {

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
    private final Point endP =  new Point();

    private Path drawingPath = new Path();

    private GeoPoint startGeoPoint;
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

    private SliderDrawProjection editingViewProjection;
    private Feature startFeature;
    private Feature endFeature;
    private boolean doCut;

    /**
     * Constructor.
     * 
     * @param mapView the mapview reference.
     * @param doCut if <code>true</code>, do cut as opposed to extend.
     */
    public CutExtendTool( MapView mapView, boolean doCut ) {
        super(mapView);
        this.doCut = doCut;
        editingViewProjection = new SliderDrawProjection(mapView, EditManager.INSTANCE.getEditingView());

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

        selectedPreviewGeometryPaintFill.setAntiAlias(true);
        selectedPreviewGeometryPaintFill.setColor(Color.GRAY);
        selectedPreviewGeometryPaintFill.setAlpha(180);
        selectedPreviewGeometryPaintFill.setStyle(Paint.Style.FILL);
        selectedPreviewGeometryPaintStroke.setAntiAlias(true);
        selectedPreviewGeometryPaintStroke.setStrokeWidth(5f);
        selectedPreviewGeometryPaintStroke.setColor(Color.DKGRAY);
        selectedPreviewGeometryPaintStroke.setStyle(Paint.Style.STROKE);

    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(false);
    }

    public void onToolDraw( Canvas canvas ) {
        if (startP.x == 0 && startP.y == 0) return;
        canvas.drawCircle(startP.x, startP.y, 15, drawingPaintFill);
        canvas.drawPath(drawingPath, drawingPaintStroke);
        canvas.drawCircle(endP.x, endP.y, 15, drawingPaintFill);

        if (previewGeometry!=null) {
            Projection projection = editingViewProjection;

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

            FeatureUtilities.drawGeometry(previewGeometry, canvas, shapeWriter, selectedPreviewGeometryPaintFill, selectedPreviewGeometryPaintStroke);
        }
    }

    public boolean onToolTouchEvent( MotionEvent event ) {
        if (mapView == null || mapView.isClickable()) {
            return false;
        }
        Projection pj = editingViewProjection;

        // handle drawing
        currentX = event.getX();
        currentY = event.getY();

        int action = event.getAction();
        switch( action ) {
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
            GeoPoint currentGeoPoint = pj.fromPixels(round(currentX), round(currentY));
            pj.toPixels(currentGeoPoint, tmpP);
            drawingPath.lineTo(tmpP.x, tmpP.y);
            endP.set(tmpP.x, tmpP.y);

            EditManager.INSTANCE.invalidateEditingView();
            break;
        case MotionEvent.ACTION_UP:

            GeoPoint endGeoPoint = pj.fromPixels(round(currentX), round(currentY));
            GeometryFactory gf = new GeometryFactory();
            Coordinate startCoord = new Coordinate(startGeoPoint.getLongitude(), startGeoPoint.getLatitude());
            com.vividsolutions.jts.geom.Point startPoint = gf.createPoint(startCoord);
            Coordinate endCoord = new Coordinate(endGeoPoint.getLongitude(), endGeoPoint.getLatitude());
            com.vividsolutions.jts.geom.Point endPoint = gf.createPoint(endCoord);
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
     final com.vividsolutions.jts.geom.Point startPoint, final com.vividsolutions.jts.geom.Point endPoint) {

        ILayer editLayer = EditManager.INSTANCE.getEditLayer();
        SpatialVectorTableLayer layer = (SpatialVectorTableLayer) editLayer;
        final SpatialVectorTable spatialVectorTable = layer.getSpatialVectorTable();

        final Context context = EditManager.INSTANCE.getEditingView().getContext();
        final ProgressDialog infoProgressDialog = new ProgressDialog(context);
        infoProgressDialog.setCancelable(true);
        infoProgressDialog.setTitle("SELECT");
        infoProgressDialog.setMessage("Selecting features...");
        infoProgressDialog.setCancelable(false);
        infoProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        infoProgressDialog.setIndeterminate(true);
        infoProgressDialog.show();

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

                    String query =
                            SpatialiteUtilities.getBboxIntersectingFeaturesQuery(LibraryConstants.SRID_WGS84_4326,
                                    spatialVectorTable, north, south, east, west);
                    List<Feature> features = FeatureUtilities.buildFeatures(query, spatialVectorTable);
                    Geometry startGeometry = null;
                    Geometry endGeometry = null;
                    for (Feature feature: features) {
                        if (startGeometry != null && endGeometry != null) break;
                        Geometry geometry = FeatureUtilities.getGeometry(feature);
                        if (startGeometry == null && geometry.intersects(startPoint)) {
                            startGeometry = geometry;
                            startFeature  = feature;
                        } else if (endGeometry == null && geometry.intersects(endPoint)) {
                            endGeometry = geometry;
                            endFeature = feature;
                        }
                    }

                    if (!doCut) {
                        previewGeometry = startGeometry.union(endGeometry);
                    }else{
                        previewGeometry = startGeometry.difference(endGeometry);
                    }
                    return "";
                } catch (Exception e) {
                    return "ERROR: " + e.getLocalizedMessage();
                }

            }

            protected void onProgressUpdate(Integer... progress) { // on UI thread!
                if (infoProgressDialog != null && infoProgressDialog.isShowing())
                    infoProgressDialog.incrementProgressBy(progress[0]);
            }

            protected void onPostExecute(String response) { // on UI thread!
                Utilities.dismissProgressDialog(infoProgressDialog);
                if (response.startsWith("ERROR")) {
                    Utilities.messageDialog(context, response, null);
                } else {
                    EditManager.INSTANCE.invalidateEditingView();

                    ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
                    if (activeToolGroup != null) {
                        activeToolGroup.onToolFinished(CutExtendTool.this);
                    }
                }
            }

        }.execute((String) null);

    }

    /**
     * Get the features as processed by the cut or extend.
     *
     * <p>The first feature is assured to have the right id and geometry to be used
     * for the db update.</p>
     *
     * @return the processed feature and the one to remove.
     */
    public Feature[] getProcessedFeatures() {
        byte[] geomBytes = FeatureUtilities.WKBWRITER.write(previewGeometry);
        Feature feature = new Feature(startFeature.getTableName(),startFeature.getUniqueTableName(), startFeature.getId(),  geomBytes);
        return new Feature[]{feature, endFeature}; // new geom feature + feature to remove
    }
}
