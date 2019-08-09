package eu.geopaparazzi.map.layers.userlayers;

import android.util.LongSparseArray;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.datatypes.EDataType;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.datatypes.ESpatialiteGeometryType;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.oscim.backend.canvas.Paint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Layers;

import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.features.Feature;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.IVectorDbLayer;
import eu.geopaparazzi.map.layers.layerobjects.GPLineDrawable;
import eu.geopaparazzi.map.layers.layerobjects.GPPointDrawable;
import eu.geopaparazzi.map.layers.layerobjects.GPPolygonDrawable;
import eu.geopaparazzi.map.layers.layerobjects.IGPDrawable;
import eu.geopaparazzi.map.layers.utils.SpatialiteConnectionsHandler;
import eu.geopaparazzi.map.layers.utils.SpatialiteUtilities;
import eu.geopaparazzi.map.utils.MapUtilities;

public class SpatialiteTableLayer extends VectorLayer implements IVectorDbLayer {

    private GPMapView mapView;
    private final String dbPath;
    private final String tableName;
    private boolean isEditing;
    private EGeometryType geometryType;
    private GeometryColumn gCol;
    private List<String[]> tableColumnInfos;

    private Style pointStyle = null;
    private Style lineStyle = null;
    private Style polygonStyle = null;

    private LongSparseArray<IGPDrawable> drawablesMap = null;

    public SpatialiteTableLayer(GPMapView mapView, String dbPath, String tableName, boolean isEditing) {
        super(mapView.map());
        this.mapView = mapView;
        this.dbPath = dbPath;
        this.tableName = tableName;
        this.isEditing = isEditing;
    }

    public void load() {
        try {
            reloadData();

            Layers layers = mapView.map().layers();
            layers.add(this, LayerGroups.GROUP_MAPLAYERS.getGroupId());
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    @Override
    public void reloadData() throws Exception {
        mDrawables.clear();
        tmpDrawables.clear();

        SpatialiteConnectionsHandler.INSTANCE.openTable(dbPath, tableName);

        ASpatialDb db = SpatialiteConnectionsHandler.INSTANCE.getDb(dbPath);
        gCol = db.getGeometryColumnsForTable(tableName);
        tableColumnInfos = db.getTableColumns(tableName);
        geometryType = SpatialiteConnectionsHandler.INSTANCE.getGeometryType(dbPath, tableName);
        eu.geopaparazzi.library.style.Style gpStyle = SpatialiteConnectionsHandler.INSTANCE.getStyleForTable(dbPath, tableName, null);

        List<Feature> features = getFeatures(null);
        drawablesMap = new LongSparseArray<>(features.size());

        for (Feature feature : features) {
            Geometry geom = feature.getDefaultGeometry();
            long id = feature.getIdFieldValue();

            eu.geopaparazzi.library.style.Style themeStyle = null;
            if (gpStyle.themeField != null) {
                String userData = geom.getUserData().toString();
                String[] split = userData.split(SpatialiteUtilities.LABEL_THEME_SEPARATOR);
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
        try {
            SpatialiteConnectionsHandler.INSTANCE.disposeTable(dbPath, tableName);
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
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
        ASpatialDb db = SpatialiteConnectionsHandler.INSTANCE.getDb(getDbPath());
        QueryResult queryResult = db.getTableRecordsMapIn(getName(), env, -1, LibraryConstants.SRID_WGS84_4326, null);

        return MapUtilities.fromQueryResult(getName(), getDbPath(), queryResult);


//        ASpatialDb db = SpatialiteConnectionsHandler.INSTANCE.getDb(dbPath);
//
//        IGeometryParser gp = db.getType().getGeometryParser();
//        return db.execOnConnection(connection -> {
//            List<Feature> tmp = new ArrayList<>();
//            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(query)) {
//                IHMResultSetMetaData md = rs.getMetaData();
//                int columnCount = md.getColumnCount();
//                while (rs.next()) {
//                    String id = rs.getString(1);
//                    Geometry geometry = gp.fromResultSet(rs, columnCount);
//
//                    Feature feature = new Feature(tableName, dbPath, id, geometry);
//                    for (int i = 2; i < columnCount - 1; i++) {
//                        String cName = md.getColumnName(i);
//                        String value = rs.getString(i);
//
//                        EDataType type = EDataType.getType4Name(cName);
//                        feature.addAttribute(cName, value, type.name());
//                    }
//                    tmp.add(feature);
//                }
//            }
//            return tmp;
//        });
    }


    @Override
    public String getDbPath() {
        return dbPath;
    }

    @Override
    public void addNewFeatureByGeometry(Geometry geometry, int geometrySrid)
            throws Exception {
        ASpatialDb db = SpatialiteConnectionsHandler.INSTANCE.getDb(getDbPath());


        String geometryFieldName = gCol.geometryColumnName;
        int srid = gCol.srid;
        ESpatialiteGeometryType spatialiteGeometryType = geometryType.toSpatialiteGeometryType();
        String geometryTypeCast = spatialiteGeometryType.getGeometryTypeCast();
        String spaceDimensionsCast = spatialiteGeometryType.getSpaceDimensionsCast();
        String multiSingleCast = spatialiteGeometryType.getMultiSingleCast();


        long newId = -1;
        // get list of non geom fields and default values
        String nonGeomFieldsNames = "";
        String nonGeomFieldsValues = "";
        for (String[] columnInfo : tableColumnInfos) {
            String field = columnInfo[0];
            String fieldType = columnInfo[1];
            boolean ignore = SpatialiteUtilities.doIgnoreField(field);
            if (!ignore) {
                EDataType tableFieldType = EDataType.getType4Name(fieldType);
                if (tableFieldType != null) {
                    nonGeomFieldsNames = nonGeomFieldsNames + "," + field;
                    String valueToSet = tableFieldType.getDefaultValueForSql();
                    if (columnInfo[2].equals("1")) {
                        long max = db.getMax(tableName, field);
                        newId = max + 1;
                        valueToSet = String.valueOf(newId);
                    }
                    nonGeomFieldsValues = nonGeomFieldsValues + "," + valueToSet;
                }
            }
        }

        boolean doTransform = true;
        if (srid == geometrySrid) {
            doTransform = false;
        }

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("insert into \"").append(tableName);//NON-NLS
        sbIn.append("\" (");
        sbIn.append(geometryFieldName);
        // add fields
        if (nonGeomFieldsNames.length() > 0) {
            sbIn.append(nonGeomFieldsNames);
        }
        sbIn.append(") values (");//NON-NLS
        if (doTransform)
            sbIn.append("ST_Transform(");//NON-NLS
        if (multiSingleCast != null)
            sbIn.append(multiSingleCast).append("(");
        if (spaceDimensionsCast != null)
            sbIn.append(spaceDimensionsCast).append("(");
        if (geometryTypeCast != null)
            sbIn.append(geometryTypeCast).append("(");
        sbIn.append("GeomFromText('");//NON-NLS
        sbIn.append(geometry.toText());
        sbIn.append("' , ");
        sbIn.append(geometrySrid);
        sbIn.append(")");
        if (geometryTypeCast != null)
            sbIn.append(")");
        if (spaceDimensionsCast != null)
            sbIn.append(")");
        if (multiSingleCast != null)
            sbIn.append(")");
        if (doTransform) {
            sbIn.append(",");
            sbIn.append(srid);
            sbIn.append(")");
        }
        // add field default values
        if (nonGeomFieldsNames.length() > 0) {
            sbIn.append(nonGeomFieldsValues);
        }
        sbIn.append(")");
        String insertQuery = sbIn.toString();

        db.executeInsertUpdateDeleteSql(insertQuery);


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
        ASpatialDb db = SpatialiteConnectionsHandler.INSTANCE.getDb(feature.getDatabasePath());
        String geometryFieldName = gCol.geometryColumnName;
        int srid = gCol.srid;
        ESpatialiteGeometryType spatialiteGeometryType = geometryType.toSpatialiteGeometryType();
        String geometryTypeCast = spatialiteGeometryType.getGeometryTypeCast();
        String spaceDimensionsCast = spatialiteGeometryType.getSpaceDimensionsCast();
        String multiSingleCast = spatialiteGeometryType.getMultiSingleCast();

        boolean doTransform = true;
        if (srid == geometrySrid) {
            doTransform = false;
        }

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update \"").append(tableName);//NON-NLS
        sbIn.append("\" set ");//NON-NLS
        sbIn.append(geometryFieldName);
        sbIn.append(" = ");
        if (doTransform)
            sbIn.append("ST_Transform(");//NON-NLS
        if (multiSingleCast != null)
            sbIn.append(multiSingleCast).append("(");
        if (spaceDimensionsCast != null)
            sbIn.append(spaceDimensionsCast).append("(");
        if (geometryTypeCast != null)
            sbIn.append(geometryTypeCast).append("(");
        sbIn.append("GeomFromText('");//NON-NLS
        sbIn.append(geometry.toText());
        sbIn.append("' , ");
        sbIn.append(geometrySrid);
        sbIn.append(")");
        if (geometryTypeCast != null)
            sbIn.append(")");
        if (spaceDimensionsCast != null)
            sbIn.append(")");
        if (multiSingleCast != null)
            sbIn.append(")");
        if (doTransform) {
            sbIn.append(",");
            sbIn.append(srid);
            sbIn.append(")");
        }
        sbIn.append("");
        sbIn.append(" where ");//NON-NLS
        sbIn.append(feature.getIdFieldName()).append("=");
        sbIn.append(feature.getIdFieldValue());
        String insertQuery = sbIn.toString();
        db.executeInsertUpdateDeleteSql(insertQuery);


        long id = feature.getIdFieldValue();
        IGPDrawable drawable = drawablesMap.get(id);
        if (drawable != null)
            remove((Drawable) drawable);
        drawablesMap.remove(id);

        Geometry g = geometry;
        if (doTransform) {
            g = db.reproject(geometry, geometrySrid, srid);
        }
        addNewGeometry(g, id);
        update();
    }

    public void deleteFeatures(List<Feature> features) throws Exception {
        if (features.size() == 0) return;
        Feature firstFeature = features.get(0);
        ASpatialDb db = SpatialiteConnectionsHandler.INSTANCE.getDb(firstFeature.getDatabasePath());
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
}
