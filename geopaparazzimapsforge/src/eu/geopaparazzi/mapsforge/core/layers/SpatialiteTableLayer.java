package eu.geopaparazzi.mapsforge.core.layers;

import android.graphics.PointF;

import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.spatialite.ESpatialiteGeometryType;
import org.locationtech.jts.android.PointTransformation;
import org.locationtech.jts.android.ShapeWriter;
import org.locationtech.jts.android.geom.DrawableShape;
import org.locationtech.jts.android.geom.PathShape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.util.MapViewProjection;

import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.core.maps.SpatialiteMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.Style;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.mapsforge.core.proj.MapsforgePointTransformation;
import eu.geopaparazzi.spatialite.GeopaparazziDatabaseProperties;
import eu.geopaparazzi.spatialite.ISpatialiteTableAndFieldsNames;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryIterator;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;

public class SpatialiteTableLayer extends Layer implements ISpatialiteTableAndFieldsNames {

    private final MapViewProjection prj;
    private final ASpatialDb db;
    private final String tableName;
    private final Style style;
    private final Paint fill;
    private final Paint stroke;
    private final boolean isPoint;
    private final int tileSize;

    public SpatialiteTableLayer(MapView mapView, ASpatialDb db, String tableName) throws java.lang.Exception {
        prj = mapView.getMapViewProjection();
        tileSize = mapView.getModel().displayModel.getTileSize();
        this.db = db;
        this.tableName = tableName;

        style = GeopaparazziDatabaseProperties.getStyle4Table(db, tableName, null);
        fill = GeopaparazziDatabaseProperties.getFillPaint4Style(style);
        stroke = GeopaparazziDatabaseProperties.getStrokePaint4Style(style);

        GeometryColumn gc = db.getGeometryColumnsForTable(tableName);

        int type = gc.geometryType;

        ESpatialiteGeometryType geomType = ESpatialiteGeometryType.forValue(type);
        isPoint = geomType.isPoint();
    }


    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        /*
         * draw from spatialite
         */
        double n = boundingBox.maxLatitude;
        double w = boundingBox.minLongitude;
        double s = boundingBox.minLatitude;
        double e = boundingBox.maxLongitude;
        Envelope canvasEnvelope = new Envelope(w, e, s, n);
        try {
            List<Geometry> geoms = db.getGeometriesIn(tableName, canvasEnvelope, null);

            if (!isVisible()) {
                return;
            }

            if (zoomLevel < style.minZoom || zoomLevel > style.maxZoom) {
                // we do not draw outside of the zoom levels
                return;
            }

//                    Paint stroke = null;
//                    if (style.themeField == null) {
//                        if (style.fillcolor != null && style.fillcolor.trim().length() > 0)
//                            fill = spatialTable.getFillPaint4Style(style);
//                        if (style.strokecolor != null && style.strokecolor.trim().length() > 0)
//                            stroke = spatialTable.getStrokePaint4Style(style);
//                    }
            PointTransformation pointTransformer = new MapsforgePointTransformation(prj, topLeftPoint,
                    zoomLevel, tileSize);
            ShapeWriter shapeWriter;
            ShapeWriter shape_writer_point = null;
            if (isPoint) {
                shapeWriter = new ShapeWriter(pointTransformer, style.shape,
                        style.size);
                shape_writer_point = new ShapeWriter(pointTransformer, style.shape,
                        style.size);
            } else {
                shapeWriter = new ShapeWriter(pointTransformer);
//                        if (spatialTable.isGeometryCollection()) {
//                            shape_writer_point = new ShapeWriter(pointTransformer, style.shape,
//                                    style.size);
//                        }
            }
            shapeWriter.setRemoveDuplicatePoints(true);
            shapeWriter.setDecimation(style.decimationFactor);
            for (Geometry geom : geoms) {
                if (geom != null) {
                    if (!canvasEnvelope.intersects(geom.getEnvelopeInternal())) {
                        continue;
                    }

                    // TODO unlock themes
//                            if (style.themeField != null) {
//                                // set paint
//                                String themeFieldValue = geometryIterator.getThemeFieldValue();
//                                Style themeStyle = style.themeMap.get(themeFieldValue);
//                                if (themeStyle != null) {
//                                    if (themeStyle.fillcolor != null && themeStyle.fillcolor.trim().length() > 0)
//                                        fill = spatialTable.getFillPaint4Theme(themeFieldValue, themeStyle);
//                                    if (themeStyle.strokecolor != null && themeStyle.strokecolor.trim().length() > 0)
//                                        stroke = spatialTable.getStrokePaint4Theme(themeFieldValue, themeStyle);
//                                    if (spatialTable.isPoint())
//                                        shape_writer_point = new ShapeWriter(pointTransformer, themeStyle.shape, themeStyle.size);
//                                }
//                            }
                    int geometriesCount = geom.getNumGeometries();
                    for (int j = 0; j < geometriesCount; j++) {
                        Geometry geom_collect = geom.getGeometryN(j);
                        if (geom_collect != null) {
                            String geometryType = geom_collect.getGeometryType();
                            if (geometryType.toUpperCase().contains("POINT")) {
                                drawGeometry(geom_collect, canvas, shape_writer_point, fill, stroke);
                            } else {
                                drawGeometry(geom_collect, canvas, shapeWriter, fill, stroke);
                            }
                        }
                    }
                }
            }


        } catch (Exception cme) {
            GPLog.error(this, "Error while looping on spatialite maps, skipped rendering.", cme);
            return;
        }


        /*
         * draw labels
         */
//            for (Map.Entry<SpatialiteMap, SpatialiteDatabaseHandler> entry : spatialiteMaps2DbHandlersMap.entrySet()) {
//                if (stopDrawing()) {
//                    // stop working
//                    return;
//                }
//                SpatialiteMap spatialiteMap = entry.getKey();
//                if (!spatialiteMap.isVisible) {
//                    continue;
//                }
//
//                SpatialiteDatabaseHandler spatialDatabaseHandler = entry.getValue();
//
//                SpatialVectorTable spatialTable = spatialiteMaps2TablesMap.get(spatialiteMap);
//                Style style = spatialTable.getStyle();
//
//                if (style.labelvisible == 0) {
//                    continue;
//                }
//                if (drawZoomLevel < style.minZoom || drawZoomLevel > style.maxZoom) {
//                    // we do not draw outside of the zoom levels
//                    continue;
//                }
//
//                float delta = style.size / 2f;
//                if (delta < 2) {
//                    delta = 2;
//                }
//
//                Paint dbTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//                dbTextPaint.setStyle(Paint.Style.FILL);
//                dbTextPaint.setColor(Color.BLACK);
//                dbTextPaint.setTextSize(style.labelsize);
//                Paint dbTextHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//                dbTextHaloPaint.setStyle(Paint.Style.STROKE);
//                dbTextHaloPaint.setStrokeWidth(3);
//                dbTextHaloPaint.setColor(Color.WHITE);
//                dbTextHaloPaint.setTextSize(style.labelsize);
//
//                GeometryIterator geometryIterator = null;
//                try {
//                    PointTransformation pointTransformer = new MapsforgePointTransformation(projection, drawPosition,
//                            drawZoomLevel);
//                    ShapeWriter linesWriter = null;
//                    if (spatialTable.isLine()) {
//                        linesWriter = new ShapeWriter(pointTransformer, spatialTable.getStyle().shape,
//                                spatialTable.getStyle().size);
//                        dbTextHaloPaint.setTextAlign(Paint.Align.CENTER);
//                        dbTextPaint.setTextAlign(Paint.Align.CENTER);
//                    } else {
//                        dbTextHaloPaint.setTextAlign(Paint.Align.LEFT);
//                        dbTextPaint.setTextAlign(Paint.Align.LEFT);
//                    }
//
//                    if (spatialDatabaseHandler.isOpen()) {
//                        geometryIterator = spatialDatabaseHandler.getGeometryIteratorInBounds(
//                                LibraryConstants.SRID_WGS84_4326, spatialTable, n, s, e, w);
//                        while (geometryIterator.hasNext()) {
//                            Geometry geom = geometryIterator.next();
//                            if (geom != null) {
//                                if (!canvasEnvelope.intersects(geom.getEnvelopeInternal())) {
//                                    // TODO check the performance impact of this
//                                    continue;
//                                }
//                                String labelText = geometryIterator.getLabelText();
//                                if (labelText == null || labelText.length() == 0) {
//                                    continue;
//                                }
//                                if (spatialTable.isGeometryCollection()) {
//                                    int geometriesCount = geom.getNumGeometries();
//                                    for (int j = 0; j < geometriesCount; j++) {
//                                        Geometry geom_collect = geom.getGeometryN(j);
//                                        if (geom_collect != null) {
//                                            drawLabel(pointTransformer, geom_collect, labelText, canvas, dbTextPaint,
//                                                    dbTextHaloPaint, delta, linesWriter);
//                                            if (stopDrawing()) { // stop working
//                                                return;
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    drawLabel(pointTransformer, geom, labelText, canvas, dbTextPaint, dbTextHaloPaint, delta,
//                                            linesWriter);
//                                    if (stopDrawing()) { // stop working
//                                        return;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                } finally {
//                    if (geometryIterator != null)
//                        geometryIterator.close();
//                }
//            }

    }

    private static void drawGeometry(Geometry geom, Canvas canvas, ShapeWriter shape_writer, Paint fill, Paint stroke) {
        String s_geometry_type = geom.getGeometryType();
        int i_geometry_type = GeometryType.forValue(s_geometry_type);
        GeometryType geometry_type = GeometryType.forValue(i_geometry_type);
        DrawableShape shape = shape_writer.toShape(geom);
        switch (geometry_type) {
            case POINT_XY:
            case POINT_XYM:
            case POINT_XYZ:
            case POINT_XYZM:
            case MULTIPOINT_XY:
            case MULTIPOINT_XYM:
            case MULTIPOINT_XYZ:
            case MULTIPOINT_XYZM: {
                if (fill != null)
                    shape.fill(canvas, fill);
                if (stroke != null)
                    shape.draw(canvas, stroke);
                // GPLog.androidLog(-1,"GeopaparazziOverlay.drawGeometry geometry_type["+s_geometry_type+"]: ["+i_geometry_type+"]");
            }
            break;
            case LINESTRING_XY:
            case LINESTRING_XYM:
            case LINESTRING_XYZ:
            case LINESTRING_XYZM:
            case MULTILINESTRING_XY:
            case MULTILINESTRING_XYM:
            case MULTILINESTRING_XYZ:
            case MULTILINESTRING_XYZM: {
                if (stroke != null)
                    shape.draw(canvas, stroke);
            }
            break;
            case POLYGON_XY:
            case POLYGON_XYM:
            case POLYGON_XYZ:
            case POLYGON_XYZM:
            case MULTIPOLYGON_XY:
            case MULTIPOLYGON_XYM:
            case MULTIPOLYGON_XYZ:
            case MULTIPOLYGON_XYZM: {
                if (fill != null)
                    shape.fill(canvas, fill);
                if (stroke != null)
                    shape.draw(canvas, stroke);
            }
            break;
            default:
                break;
        }
    }

    private static void drawLabel(PointTransformation pointTransformer, Geometry geom, String label, android.graphics.Canvas canvas,
                                  Paint dbTextPaint, Paint dbTextHaloPaint, float delta, ShapeWriter linesWriter) {

//        if (linesWriter == null) {
//            /*
//             * for points and polygons for now just use the centroid
//             */
//            org.locationtech.jts.geom.Point centroid = geom.getCentroid();
//            Coordinate coordinate = centroid.getCoordinate();
//            PointF dest = new PointF();
//            pointTransformer.transform(coordinate, dest);
//            float x = dest.x + delta;
//            float y = dest.y - delta;
//            // if (doNotesTextHalo)
//            canvas.drawText(label, x, y, dbTextHaloPaint);
//            canvas.drawText(label, x, y, dbTextPaint);
//        } else {
//            DrawableShape shape = linesWriter.toShape(geom);
//            if (shape instanceof PathShape) {
//                PathShape lineShape = (PathShape) shape;
//                Path linePath = lineShape.getPath();
//                // if (doNotesTextHalo)
//                int hOffset = 15;
//                int vOffset = -5;
//                canvas.drawTextOnPath(label, linePath, hOffset, vOffset, dbTextHaloPaint);
//                canvas.drawTextOnPath(label, linePath, hOffset, vOffset, dbTextPaint);
//            }
//        }
    }


}
