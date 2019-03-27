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
package eu.geopaparazzi.core.maptools.tools;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.widget.Toast;

import org.locationtech.jts.android.PointTransformation;
import org.locationtech.jts.android.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.IMapViewPosition;

import java.util.List;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.maptools.FeatureUtilities;
import eu.geopaparazzi.core.maptools.MapTool;
import eu.geopaparazzi.mapsforge.core.proj.MapsforgePointTransformation;
import eu.geopaparazzi.mapsforge.core.proj.SliderDrawProjection;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.core.features.EditManager;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.core.features.ILayer;
import eu.geopaparazzi.core.features.ToolGroup;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.style.ToolColors;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.spatialite.database.spatial.core.layers.SpatialVectorTableLayer;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteUtilities;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * A tool to cut or extend features.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PolygonCutExtendTool extends MapTool {

    private final Paint drawingPaintStroke = AndroidGraphicFactory.INSTANCE.createPaint();
    private final Paint drawingPaintFill = AndroidGraphicFactory.INSTANCE.createPaint();
    private final Paint selectedPreviewGeometryPaintStroke = AndroidGraphicFactory.INSTANCE.createPaint();
    private final Paint selectedPreviewGeometryPaintFill = AndroidGraphicFactory.INSTANCE.createPaint();

    private float currentX = 0;
    private float currentY = 0;
    private float lastX = -1;
    private float lastY = -1;

    private Point tmpP = new Point(0, 0);
    private Point startP = new Point(0, 0);
    private Point endP = new Point(0, 0);

    private Path drawingPath = AndroidGraphicFactory.INSTANCE.createPath();

    private LatLong startGeoPoint;
    private Geometry previewGeometry = null;


    // private ProgressDialog infoProgressDialog;

    private SliderDrawProjection editingViewProjection;
    private Feature startFeature;
    private Feature endFeature;
    private boolean doCut;

    /**
     * Constructor.
     *
     * @param mapView the mapview reference.
     * @param doCut   if <code>true</code>, do cut as opposed to extend.
     */
    public PolygonCutExtendTool(MapView mapView, boolean doCut) {
        super(mapView);
        this.doCut = doCut;
        editingViewProjection = new SliderDrawProjection(mapView, EditManager.INSTANCE.getEditingView());

        // Context context = GeopaparazziApplication.getInstance().getApplicationContext();
        // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//        drawingPaintFill.setAntiAlias(true);
        drawingPaintFill.setColor(Color.RED);
        // drawingPaintFill.setAlpha(80);
        drawingPaintFill.setStyle(Style.FILL);
//        drawingPaintStroke.setAntiAlias(true);
        drawingPaintStroke.setStrokeWidth(5f);
        drawingPaintStroke.setColor(Color.RED);
        drawingPaintStroke.setStyle(Style.STROKE);

        int previewStroke = ColorUtilities.toColor(ToolColors.preview_stroke.getHex());
        int previewFill = ColorUtilities.toColor(ToolColors.preview_fill.getHex());
//        selectedPreviewGeometryPaintFill.setAntiAlias(true);
        selectedPreviewGeometryPaintFill.setColor(previewFill);
//        selectedPreviewGeometryPaintFill.setAlpha(180);
        selectedPreviewGeometryPaintFill.setStyle(Style.FILL);
//        selectedPreviewGeometryPaintStroke.setAntiAlias(true);
        selectedPreviewGeometryPaintStroke.setStrokeWidth(5f);
        selectedPreviewGeometryPaintStroke.setColor(previewStroke);
        selectedPreviewGeometryPaintStroke.setStyle(Style.STROKE);

    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(false);
    }

    public void onToolDraw(Canvas canvas) {
        if (startP.x == 0 && startP.y == 0) return;
        canvas.drawCircle((int) startP.x, (int) startP.y, 15, drawingPaintFill);
        canvas.drawPath(drawingPath, drawingPaintStroke);
        canvas.drawCircle((int) endP.x, (int) endP.y, 15, drawingPaintFill);

        if (previewGeometry != null) {
            SliderDrawProjection projection = editingViewProjection;
            IMapViewPosition mapPosition = this.mapView.getModel().mapViewPosition;

            byte zoomLevelBeforeDraw = mapPosition.getZoomLevel();
            Point positionBeforeDraw = projection.toPoint(mapPosition.getCenter(),
                    zoomLevelBeforeDraw);
            // calculate the top-left point of the visible rectangle
            double x = positionBeforeDraw.x - (canvas.getWidth() >> 1);
            double y = positionBeforeDraw.y - (canvas.getHeight() >> 1);
            Point point = new Point(x, y);

            byte zoomLevel = mapPosition.getZoomLevel();

            PointTransformation pointTransformer = new MapsforgePointTransformation(projection, point, zoomLevel, mapView.getModel().displayModel.getTileSize());
            ShapeWriter shapeWriter = new ShapeWriter(pointTransformer);
            shapeWriter.setRemoveDuplicatePoints(true);

            FeatureUtilities.drawGeometry(previewGeometry, canvas, shapeWriter, selectedPreviewGeometryPaintFill, selectedPreviewGeometryPaintStroke);
        }
    }

    public boolean onToolTouchEvent(MotionEvent event) {
        if (mapView == null || mapView.isClickable()) {
            return false;
        }
        SliderDrawProjection pj = editingViewProjection;

        // handle drawing
        currentX = event.getX();
        currentY = event.getY();

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                startP = pj.toPixels(startGeoPoint);
                endP = new Point(startP.x, startP.y);

//                drawingPath.reset();
                drawingPath.moveTo((float) startP.x, (float) startP.y);

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
                LatLong currentGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                tmpP = pj.toPixels(currentGeoPoint);
                drawingPath.lineTo((float) tmpP.x, (float) tmpP.y);
                endP = new Point(tmpP.x, tmpP.y);

                EditManager.INSTANCE.invalidateEditingView();
                break;
            case MotionEvent.ACTION_UP:

                LatLong endGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                GeometryFactory gf = new GeometryFactory();
                Coordinate startCoord = new Coordinate(startGeoPoint.getLongitude(), startGeoPoint.getLatitude());
                org.locationtech.jts.geom.Point startPoint = gf.createPoint(startCoord);
                Coordinate endCoord = new Coordinate(endGeoPoint.getLongitude(), endGeoPoint.getLatitude());
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

                    String query =
                            SpatialiteUtilities.getBboxIntersectingFeaturesQuery(LibraryConstants.SRID_WGS84_4326,
                                    spatialVectorTable, north, south, east, west);
                    List<Feature> features = FeatureUtilities.buildFeatures(query, spatialVectorTable);
                    Geometry startGeometry = null;
                    Geometry endGeometry = null;
                    for (Feature feature : features) {
                        if (startGeometry != null && endGeometry != null) break;
                        Geometry geometry = FeatureUtilities.getGeometry(feature);
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
                        return "ERROR: no start or end geometry touched.";
                    }
                } catch (Exception e) {
                    GPLog.error(this, null, e); //$NON-NLS-1$
                    return "ERROR: " + e.getLocalizedMessage();
                }

            }

            protected void onProgressUpdate(Integer... progress) { // on UI thread!
                if (infoProgressDialog.isShowing())
                    infoProgressDialog.incrementProgressBy(progress[0]);
            }

            protected void onPostExecute(String response) { // on UI thread!
                GPDialogs.dismissProgressDialog(infoProgressDialog);
                if (response.startsWith("ERROR")) {
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
    public Feature[] getProcessedFeatures() {

        try {
            byte[] geomBytes = FeatureUtilities.WKBWRITER.write(previewGeometry);
            Feature feature = new Feature(startFeature.getTableName(), startFeature.getDatabasePath(), startFeature.getId(), geomBytes);
            return new Feature[]{feature, endFeature}; // new geom feature + feature to remove
        } catch (Exception e) {
            String msg = "Unable to write geometry" + (previewGeometry == null ? "." : ": " + previewGeometry.toText());
            GPLog.error(this, msg, e);
            final Context context = EditManager.INSTANCE.getEditingView().getContext();
            GPDialogs.warningDialog(context, msg, null);
            return null;
        }
    }

    @Override
    public void onViewChanged() {
        // ignore
    }
}
