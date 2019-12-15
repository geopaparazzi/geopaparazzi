package eu.geopaparazzi.map.layers.userlayers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.LongSparseArray;
import android.util.TypedValue;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.android.GPGeopackageDb;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.algorithm.InteriorPointArea;
import org.locationtech.jts.algorithm.InteriorPointLine;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.oscim.backend.canvas.Paint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Layers;

import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.features.Feature;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.ILabeledLayer;
import eu.geopaparazzi.map.layers.interfaces.IVectorDbLayer;
import eu.geopaparazzi.map.layers.layerobjects.GPLineDrawable;
import eu.geopaparazzi.map.layers.layerobjects.GPPointDrawable;
import eu.geopaparazzi.map.layers.layerobjects.GPPolygonDrawable;
import eu.geopaparazzi.map.layers.layerobjects.IGPDrawable;
import eu.geopaparazzi.map.layers.utils.GeopackageConnectionsHandler;
import eu.geopaparazzi.map.proj.OverlayViewProjection;
import eu.geopaparazzi.map.utils.MapUtilities;

public class GeopackageTableLayer extends VectorLayer implements IVectorDbLayer, ILabeledLayer {

    private GPMapView mapView;
    private final String dbPath;
    private final String tableName;
    private boolean isEditing;
    private EGeometryType geometryType;
    private GeometryColumn gCol;

    private Style pointStyle = null;
    private Style lineStyle = null;
    private Style polygonStyle = null;

    private LongSparseArray<IGPDrawable> drawablesMap = null;
    private ASpatialDb db;
    private eu.geopaparazzi.library.style.Style gpStyle;
    private int labelColor;

    private android.graphics.Paint labelsBackgroundPaint = new android.graphics.Paint();


    public GeopackageTableLayer(GPMapView mapView, String dbPath, String tableName, boolean isEditing) {
        super(mapView.map());
        this.mapView = mapView;
        this.dbPath = dbPath;
        this.tableName = tableName;
        this.isEditing = isEditing;

        labelsBackgroundPaint.setAntiAlias(true);
        labelsBackgroundPaint.setColor(Color.WHITE);
        labelsBackgroundPaint.setAlpha(170);
        labelsBackgroundPaint.setStyle(android.graphics.Paint.Style.FILL);
    }

    public void load() {
        Layers layers = mapView.map().layers();
        layers.add(GeopackageTableLayer.this, LayerGroups.GROUP_MAPLAYERS.getGroupId());
        try {
            new StringAsyncTask(mapView.getContext()) {
                @Override
                protected String doBackgroundWork() {
                    try {
                        reloadData();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void doUiPostWork(String response) {
                }
            }.execute();
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }

    }

    @Override
    public void reloadData() throws Exception {
        mDrawables.clear();
        tmpDrawables.clear();

        GeopackageConnectionsHandler.INSTANCE.openTable(dbPath, tableName);

        db = GeopackageConnectionsHandler.INSTANCE.getDb(dbPath);
        gCol = db.getGeometryColumnsForTable(tableName);
        geometryType = GeopackageConnectionsHandler.INSTANCE.getGeometryType(dbPath, tableName);
        gpStyle = GeopackageConnectionsHandler.INSTANCE.getStyleForTable(dbPath, tableName, null);

        if (gpStyle.strokecolor != null) {
            labelColor = ColorUtilities.toColor(gpStyle.strokecolor);
        } else if (gpStyle.fillcolor != null) {
            labelColor = ColorUtilities.toColor(gpStyle.fillcolor);
        } else {
            labelColor = Color.BLACK;
        }

        List<Feature> features = getFeatures(null);
        drawablesMap = new LongSparseArray<>(features.size());

        for (Feature feature : features) {
            Geometry geom = feature.getDefaultGeometry();
            long id = feature.getIdFieldValue();

            eu.geopaparazzi.library.style.Style themeStyle = null;
            if (gpStyle.themeField != null) {
                String userData = geom.getUserData().toString();
                String[] split = userData.split(GeopackageConnectionsHandler.LABEL_THEME_SEPARATOR);
//                    String label = split[0];
                String themeFieldValue = split[1];
                themeStyle = gpStyle.themeMap.get(themeFieldValue);
            }

            IGPDrawable drawable = null;
            if (geometryType == EGeometryType.POINT || geometryType == EGeometryType.MULTIPOINT) {
                if (pointStyle == null) {
                    pointStyle = Style.builder()
                            .buffer(gpStyle.size)
                            .strokeWidth(gpStyle.width)
                            .strokeColor(ColorUtilities.toColor(gpStyle.strokecolor))
                            .fillColor(ColorUtilities.toColor(gpStyle.fillcolor))
                            .fillAlpha(gpStyle.fillalpha)
                            .scaleZoomLevel(19)
                            .build();
                }
                if (geom != null) {
                    int numGeometries = geom.getNumGeometries();
                    for (int i = 0; i < numGeometries; i++) {
                        Geometry geometryN = geom.getGeometryN(i);
                        Coordinate c = geometryN.getCoordinate();
                        if (themeStyle != null) {
                            Style pointThemeStyle = Style.builder()
                                    .buffer(themeStyle.size)
                                    .strokeWidth(themeStyle.width)
                                    .strokeColor(ColorUtilities.toColor(themeStyle.strokecolor))
                                    .fillColor(ColorUtilities.toColor(themeStyle.fillcolor))
                                    .fillAlpha(themeStyle.fillalpha)
                                    .scaleZoomLevel(19)
                                    .build();
                            drawable = new GPPointDrawable(c.y, c.x, pointThemeStyle, id);
                            add((Drawable) drawable);
                        } else {
                            drawable = new GPPointDrawable(c.y, c.x, pointStyle, id);
                            add((Drawable) drawable);
                        }
                    }
                }
            } else if (geometryType == EGeometryType.LINESTRING || geometryType == EGeometryType.MULTILINESTRING) {
                if (lineStyle == null) {
                    lineStyle = Style.builder()
                            .strokeColor(ColorUtilities.toColor(gpStyle.strokecolor))
                            .strokeWidth(gpStyle.width)
                            .cap(Paint.Cap.ROUND)
                            .build();
                }
                if (geom != null) {
                    int numGeometries = geom.getNumGeometries();
                    for (int i = 0; i < numGeometries; i++) {
                        Geometry geometryN = geom.getGeometryN(i);
                        if (themeStyle != null) {
                            Style lineThemeStyle = Style.builder()
                                    .strokeColor(ColorUtilities.toColor(themeStyle.strokecolor))
                                    .strokeWidth(themeStyle.width)
                                    .cap(Paint.Cap.ROUND)
                                    .build();
                            drawable = new GPLineDrawable(geometryN, lineThemeStyle, id);
                            add((Drawable) drawable);
                        } else {
                            drawable = new GPLineDrawable(geometryN, lineStyle, id);
                            add((Drawable) drawable);
                        }
                    }
                }
            } else if (geometryType == EGeometryType.POLYGON || geometryType == EGeometryType.MULTIPOLYGON) {
                if (polygonStyle == null) {
                    polygonStyle = Style.builder()
                            .strokeColor(ColorUtilities.toColor(gpStyle.strokecolor))
                            .strokeWidth(gpStyle.width)
                            .fillColor(ColorUtilities.toColor(gpStyle.fillcolor))
                            .fillAlpha(gpStyle.fillalpha)
                            .cap(Paint.Cap.ROUND)
                            .build();
                }
                if (geom != null) {
                    int numGeometries = geom.getNumGeometries();
                    for (int i = 0; i < numGeometries; i++) {
                        Geometry geometryN = geom.getGeometryN(i);
                        if (themeStyle != null) {
                            Style polygonThemeStyle = Style.builder()
                                    .strokeColor(ColorUtilities.toColor(themeStyle.strokecolor))
                                    .strokeWidth(themeStyle.width)
                                    .fillColor(ColorUtilities.toColor(themeStyle.fillcolor))
                                    .fillAlpha(themeStyle.fillalpha)
                                    .cap(Paint.Cap.ROUND)
                                    .build();
                            drawable = new GPPolygonDrawable(geometryN, polygonThemeStyle, id);
                            add((Drawable) drawable);
                        } else {
                            drawable = new GPPolygonDrawable(geometryN, polygonStyle, id);
                            add((Drawable) drawable);
                        }
                    }
                }
            }
            if (drawable != null) {
                drawablesMap.put(id, drawable);
            }
        }
        update();
    }

    @Override
    public EGeometryType getGeometryType() {
        return geometryType;
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getName() {
        return tableName;
    }

    @Override
    public GPMapView getMapView() {
        return mapView;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject jo = toDefaultJson();
        jo.put(LAYERPATH_TAG, dbPath);
        jo.put(LAYEREDITING_TAG, isEditing);
        return jo;
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isInEditingMode() {
        return isEditing;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public List<Feature> getFeatures(Envelope env) throws Exception {
        QueryResult queryResult = db.getTableRecordsMapIn(getName(), env, -1, -1, null); // only 4326 are supported

        return MapUtilities.fromQueryResult(getName(), getDbPath(), queryResult);
    }


    @Override
    public String getDbPath() {
        return dbPath;
    }

    private long insertGeometry(ASpatialDb db, String tableName, Geometry geometry) throws Exception {
        int epsg = 4326;
        String pk = ((GPGeopackageDb) db).getPrimaryKey(tableName);
        long nextId = db.getLong("select max(" + pk + ") from " + tableName) + 1;

        IGeometryParser gp = db.getType().getGeometryParser();
        geometry.setSRID(epsg);
        Object obj = gp.toSqlObject(geometry);
        if (obj instanceof byte[]) {
            byte[] objBytes = (byte[]) obj;
            GeometryColumn gc = db.getGeometryColumnsForTable(tableName);
            String sql = "INSERT INTO " + tableName + " (" + pk + "," + gc.geometryColumnName + ") VALUES (?, ?)";

            db.execOnConnection(connection -> {
                try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                    pStmt.setLong(1, nextId);
                    pStmt.setBytes(2, objBytes);
                    pStmt.executeUpdate();
                }
                return null;
            });

            Envelope env = geometry.getEnvelopeInternal();
            double minX = env.getMinX();
            double maxX = env.getMaxX();
            double minY = env.getMinY();
            double maxY = env.getMaxY();

            try {
                // also update rtree index, since it is not supported
                String sqlTree = "INSERT OR REPLACE INTO rtree_" + tableName + "_" + gc.geometryColumnName +
                        " VALUES (" + nextId + "," + minX + ", " + maxX + "," + minY + ", " + maxY + ");";
                db.executeInsertUpdateDeleteSql(sqlTree);
            } catch (Exception e) {
                GPLog.error(this, "ERROR on rtree", e);
            }

            return nextId;
        }

        throw new IllegalArgumentException("Geometry object is not byte array.");
    }

    private void updateGeometry(ASpatialDb db, String tableName, long id, Geometry geometry) throws Exception {
        int epsg = 4326;
        String pk = ((GPGeopackageDb) db).getPrimaryKey(tableName);

        IGeometryParser gp = db.getType().getGeometryParser();
        geometry.setSRID(epsg);
        Object obj = gp.toSqlObject(geometry);
        if (obj instanceof byte[]) {
            byte[] objBytes = (byte[]) obj;
            GeometryColumn gc = db.getGeometryColumnsForTable(tableName);
            String sql = "update " + tableName + " set " + gc.geometryColumnName + "=? where " + pk + "=" + id;

            db.execOnConnection(connection -> {
                try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                    pStmt.setBytes(1, objBytes);
                    pStmt.executeUpdate();
                }
                return null;
            });

            Envelope env = geometry.getEnvelopeInternal();
            double minX = env.getMinX();
            double maxX = env.getMaxX();
            double minY = env.getMinY();
            double maxY = env.getMaxY();

            try {
                // also update rtree index, since it is not supported
                String sqlTree = "INSERT OR REPLACE INTO rtree_" + tableName + "_" + gc.geometryColumnName +
                        " VALUES (" + id + "," + minX + ", " + maxX + "," + minY + ", " + maxY + ");";
                db.executeInsertUpdateDeleteSql(sqlTree);
            } catch (Exception e) {
                GPLog.error(this, "ERROR on rtree", e);
            }

        } else {
            throw new IllegalArgumentException("Geometry object is not byte array.");
        }
    }


    @Override
    public void addNewFeatureByGeometry(Geometry geometry, int geometrySrid)
            throws Exception {
        int srid = gCol.srid;
        if (srid != GeopackageCommonDb.WGS84LL_SRID) {
            // not supported
            return;
        }
        long newId = insertGeometry(db, gCol.tableName, geometry);
        /*
         * if everything went well, add also geometry to the layer
         */
        addNewGeometry(geometry, newId);
        update();
    }

    private void addNewGeometry(Geometry geometry, long id) {
        if (geometryType == EGeometryType.POINT || geometryType == EGeometryType.MULTIPOINT) {
            int numGeometries = geometry.getNumGeometries();
            for (int i = 0; i < numGeometries; i++) {
                Geometry geometryN = geometry.getGeometryN(i);
                Coordinate c = geometryN.getCoordinate();

                GPPointDrawable drawable = new GPPointDrawable(c.y, c.x, pointStyle, id);
                add(drawable);
                drawablesMap.put(id, drawable);

            }
        } else if (geometryType == EGeometryType.LINESTRING || geometryType == EGeometryType.MULTILINESTRING) {
            int numGeometries = geometry.getNumGeometries();
            for (int i = 0; i < numGeometries; i++) {
                Geometry geometryN = geometry.getGeometryN(i);
                GPLineDrawable drawable = new GPLineDrawable(geometryN, lineStyle, id);
                add(drawable);
                drawablesMap.put(id, drawable);
            }
        } else if (geometryType == EGeometryType.POLYGON || geometryType == EGeometryType.MULTIPOLYGON) {
            int numGeometries = geometry.getNumGeometries();
            for (int i = 0; i < numGeometries; i++) {
                Geometry geometryN = geometry.getGeometryN(i);
                GPPolygonDrawable drawable = new GPPolygonDrawable(geometryN, polygonStyle, id);
                add(drawable);
                drawablesMap.put(id, drawable);
            }
        }
    }

    @Override
    public void updateFeatureGeometry(Feature feature, Geometry geometry, int geometrySrid)
            throws Exception {
        updateGeometry(db, gCol.tableName, feature.getIdFieldValue(), geometry);

        long id = feature.getIdFieldValue();
        IGPDrawable drawable = drawablesMap.get(id);
        if (drawable != null)
            remove((Drawable) drawable);
        drawablesMap.remove(id);

        addNewGeometry(geometry, id);
        update();
    }

    public void deleteFeatures(List<Feature> features) throws Exception {
        if (features.size() == 0) return;
        Feature firstFeature = features.get(0);
        ASpatialDb db = GeopackageConnectionsHandler.INSTANCE.getDb(firstFeature.getDatabasePath());
        String tableName = firstFeature.getTableName();

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("delete from \"").append(tableName);//NON-NLS
        sbIn.append("\" where ");//NON-NLS

        int idIndex = firstFeature.getIdIndex();
        String indexName = firstFeature.getAttributeNames().get(idIndex);

        StringBuilder sb = new StringBuilder();
        for (Feature feature : features) {
            sb.append(" OR ");//NON-NLS
            sb.append(indexName).append("=");
            sb.append(feature.getAttributeValues().get(idIndex));
        }
        String valuesPart = sb.substring(4);

        sbIn.append(valuesPart);

        String updateQuery = sbIn.toString();
        db.executeInsertUpdateDeleteSql(updateQuery);

        for (Feature feature : features) {
            long id = feature.getIdFieldValue();
            IGPDrawable drawable = drawablesMap.get(id);
            if (drawable != null)
                remove((Drawable) drawable);
            drawablesMap.remove(id);
        }
        update();
    }

    @Override
    public void drawLabels(Canvas canvas, OverlayViewProjection prj) throws Exception {
        if (gpStyle != null && gpStyle.labelvisible == 1) {
            android.graphics.Point drawPoint = new android.graphics.Point();
            DisplayMetrics displayMetrics = getMapView().getContext().getResources().getDisplayMetrics();
            int pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, gpStyle.labelsize, displayMetrics);

            android.graphics.Paint labelPaint = new android.graphics.Paint();
            labelPaint.setAntiAlias(true);
            labelPaint.setTextSize(pixel);
            labelPaint.setColor(labelColor);
            labelPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

            Rect rect = new Rect();

            Rect bounds = canvas.getClipBounds();
            Coordinate ll = prj.fromPixels(bounds.left, bounds.bottom);
            Coordinate ur = prj.fromPixels(bounds.right, bounds.top);
            Envelope env = new Envelope(ll, ur);

            Quadtree labelTree = new Quadtree();

            List<Feature> features = getFeatures(env);
            for (Feature f : features) {
                Object attribute = f.getAttribute(gpStyle.labelfield);
                if (attribute != null) {
                    String txt = attribute.toString();
                    if (txt.length() > 0) {
                        labelPaint.getTextBounds(txt, 0, txt.length(), rect);
                        int textWidth = rect.width();
                        int textHeight = rect.height();

                        if (geometryType == EGeometryType.POINT || geometryType == EGeometryType.MULTIPOINT) {
                            Point p = f.getDefaultGeometry().getCentroid();
                            prj.toPixels(p.getCoordinate(), drawPoint);

                            int x = drawPoint.x - textWidth / 2;
                            int y = drawPoint.y - textHeight;

                            int indent = 10;
                            RectF backRect = new RectF();
                            backRect.left = x - indent;
                            backRect.right = x + textWidth + indent * 2;
                            backRect.bottom = y + indent * 2;
                            backRect.top = y - textHeight - indent * 2;

                            Envelope lenv = new Envelope(backRect.left, backRect.right, backRect.bottom, backRect.top);
                            List<Envelope> res = labelTree.query(lenv);
                            boolean inters = false;
                            for (Envelope e : res) {
                                if (lenv.intersects(e)) {
                                    inters = true;
                                    break;
                                }
                            }
                            if (!inters) {
                                canvas.drawRoundRect(backRect, indent, indent, labelsBackgroundPaint);
                                canvas.drawText(txt, x, y, labelPaint);
                                labelTree.insert(lenv, lenv);
                            }
                        } else if (geometryType == EGeometryType.POLYGON || geometryType == EGeometryType.MULTIPOLYGON) {
                            Geometry geometry = f.getDefaultGeometry();
                            int numGeometries = geometry.getNumGeometries();
                            for (int i = 0; i < numGeometries; i++) {
                                Polygon polygon = (Polygon) geometry.getGeometryN(i);

                                Coordinate interiorPoint = new InteriorPointArea(polygon).getInteriorPoint();
                                prj.toPixels(interiorPoint, drawPoint);

                                int x = drawPoint.x - textWidth / 2;
                                int y = drawPoint.y;

                                int indent = 10;
                                RectF backRect = new RectF();
                                backRect.left = x - indent;
                                backRect.right = x + textWidth + indent * 2;
                                backRect.bottom = y + indent * 2;
                                backRect.top = y - textHeight - indent * 2;

                                Envelope lenv = new Envelope(backRect.left, backRect.right, backRect.bottom, backRect.top);
                                List<Envelope> res = labelTree.query(lenv);
                                boolean inters = false;
                                for (Envelope e : res) {
                                    if (lenv.intersects(e)) {
                                        inters = true;
                                        break;
                                    }
                                }
                                if (!inters) {
                                    canvas.drawRoundRect(backRect, indent, indent, labelsBackgroundPaint);
                                    canvas.drawText(txt, x, y, labelPaint);
                                    labelTree.insert(lenv, lenv);
                                }
                            }
                        } else if (geometryType == EGeometryType.LINESTRING || geometryType == EGeometryType.MULTILINESTRING) {
                            Geometry geometry = f.getDefaultGeometry();
                            int numGeometries = geometry.getNumGeometries();
                            for (int i = 0; i < numGeometries; i++) {
                                LineString polygon = (LineString) geometry.getGeometryN(i);

                                Coordinate interiorPoint = new InteriorPointLine(polygon).getInteriorPoint();
                                prj.toPixels(interiorPoint, drawPoint);

                                int x = drawPoint.x - textWidth / 2;
                                int y = drawPoint.y;

                                int indent = 10;
                                RectF backRect = new RectF();
                                backRect.left = x - indent;
                                backRect.right = x + textWidth + indent * 2;
                                backRect.bottom = y + indent * 2;
                                backRect.top = y - textHeight - indent * 2;

                                Envelope lenv = new Envelope(backRect.left, backRect.right, backRect.bottom, backRect.top);
                                List<Envelope> res = labelTree.query(lenv);
                                boolean inters = false;
                                for (Envelope e : res) {
                                    if (lenv.intersects(e)) {
                                        inters = true;
                                        break;
                                    }
                                }
                                if (!inters) {
                                    canvas.drawRoundRect(backRect, indent, indent, labelsBackgroundPaint);
                                    canvas.drawText(txt, x, y, labelPaint);
                                    labelTree.insert(lenv, lenv);
                                }
                            }


                        }

                    }
                }
            }


        }
    }
}
