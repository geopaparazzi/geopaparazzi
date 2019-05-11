package eu.geopaparazzi.map.layers.userlayers;

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
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.PolygonDrawable;
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
            layers.add(this, LayerGroups.GROUP_USERLAYERS.getGroupId());
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
        List<Geometry> geometries = SpatialiteConnectionsHandler.INSTANCE.getGeometries(dbPath, tableName, gpStyle);


        for (Geometry geom : geometries) {
            eu.geopaparazzi.library.style.Style themeStyle = null;
            if (gpStyle.themeField != null) {
                String userData = geom.getUserData().toString();
                String[] split = userData.split(SpatialiteUtilities.LABEL_THEME_SEPARATOR);
//                    String label = split[0];
                String themeFieldValue = split[1];
                themeStyle = gpStyle.themeMap.get(themeFieldValue);
            }

            if (geometryType == EGeometryType.POINT || geometryType == EGeometryType.MULTIPOINT) {
                if (pointStyle == null) {
                    pointStyle = Style.builder()
                            .buffer(gpStyle.size)
                            .strokeWidth(gpStyle.width)
                            .strokeColor(ColorUtilities.toColor(gpStyle.strokecolor))
                            .fillColor(ColorUtilities.toColor(gpStyle.fillcolor))
                            .fillAlpha(gpStyle.fillalpha)
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
                                    .build();
                            add(new PointDrawable(c.y, c.x, pointThemeStyle));
                        } else {
                            add(new PointDrawable(c.y, c.x, pointStyle));
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
                            add(new LineDrawable(geometryN, lineThemeStyle));
                        } else {
                            add(new LineDrawable(geometryN, lineStyle));
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
                            add(new PolygonDrawable(geometryN, polygonThemeStyle));
                        } else {
                            add(new PolygonDrawable(geometryN, polygonStyle));
                        }
                    }
                }
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
                    nonGeomFieldsValues = nonGeomFieldsValues + "," + tableFieldType.getDefaultValueForSql();
                }
            }
        }

        boolean doTransform = true;
        if (srid == geometrySrid) {
            doTransform = false;
        }

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("insert into \"").append(tableName);
        sbIn.append("\" (");
        sbIn.append(geometryFieldName);
        // add fields
        if (nonGeomFieldsNames.length() > 0) {
            sbIn.append(nonGeomFieldsNames);
        }
        sbIn.append(") values (");
        if (doTransform)
            sbIn.append("ST_Transform(");
        if (multiSingleCast != null)
            sbIn.append(multiSingleCast).append("(");
        if (spaceDimensionsCast != null)
            sbIn.append(spaceDimensionsCast).append("(");
        if (geometryTypeCast != null)
            sbIn.append(geometryTypeCast).append("(");
        sbIn.append("GeomFromText('");
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
        if (geometryType == EGeometryType.POINT || geometryType == EGeometryType.MULTIPOINT) {
            int numGeometries = geometry.getNumGeometries();
            for (int i = 0; i < numGeometries; i++) {
                Geometry geometryN = geometry.getGeometryN(i);
                Coordinate c = geometryN.getCoordinate();
                add(new PointDrawable(c.y, c.x, pointStyle));
            }
        } else if (geometryType == EGeometryType.LINESTRING || geometryType == EGeometryType.MULTILINESTRING) {
            int numGeometries = geometry.getNumGeometries();
            for (int i = 0; i < numGeometries; i++) {
                Geometry geometryN = geometry.getGeometryN(i);
                add(new LineDrawable(geometryN, lineStyle));
            }
        } else if (geometryType == EGeometryType.POLYGON || geometryType == EGeometryType.MULTIPOLYGON) {
            int numGeometries = geometry.getNumGeometries();
            for (int i = 0; i < numGeometries; i++) {
                Geometry geometryN = geometry.getGeometryN(i);
                add(new PolygonDrawable(geometryN, polygonStyle));
            }
        }
        update();
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
        sbIn.append("update \"").append(tableName);
        sbIn.append("\" set ");
        sbIn.append(geometryFieldName);
        sbIn.append(" = ");
        if (doTransform)
            sbIn.append("ST_Transform(");
        if (multiSingleCast != null)
            sbIn.append(multiSingleCast).append("(");
        if (spaceDimensionsCast != null)
            sbIn.append(spaceDimensionsCast).append("(");
        if (geometryTypeCast != null)
            sbIn.append(geometryTypeCast).append("(");
        sbIn.append("GeomFromText('");
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
        sbIn.append(" where ");
        sbIn.append(feature.getIdFieldName()).append("=");
        sbIn.append(feature.getIdFieldValue());
        String insertQuery = sbIn.toString();
        db.executeInsertUpdateDeleteSql(insertQuery);
    }
}
