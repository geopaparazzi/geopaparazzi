package eu.geopaparazzi.map.layers.utils;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.geopackage.android.GPGeopackageDb;
import org.hortonmachine.dbs.utils.BasicStyle;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.geopaparazzi.library.style.Style;

public enum GeopackageConnectionsHandler {
    INSTANCE;

    HashMap<String, List<String>> connection2TablesMap = new HashMap<>();
    HashMap<String, ASpatialDb> connection2DbsMap = new HashMap<>();
    public static final String DUMMY = "dummy";
    public static final String LABEL_THEME_SEPARATOR = "@@";

    /**
     * Call this to mark a table as in use.
     *
     * <p>This helps keeping trak of layers of a same database. Spatialite is singlethread, so one connection should be used
     * in order to not mess in write mode.</p>
     *
     * @param dbPath    the db path.
     * @param tableName the table to mark as opened.
     * @throws Exception
     */
    public void openTable(String dbPath, String tableName) throws Exception {
        ASpatialDb db = getDb(dbPath);
        if (db.hasTable(tableName)) {
            List<String> openTables = connection2TablesMap.get(dbPath);
            if (openTables == null) {
                openTables = new ArrayList<>();
                connection2TablesMap.put(dbPath, openTables);
            }
            if (!openTables.contains(tableName))
                openTables.add(tableName);
        }
    }

    /**
     * Dispose a previously opened table.
     *
     * <p>If this is the last table in use, then also the db connection is closed.</p>
     *
     * @param dbPath    the db path.
     * @param tableName the table to dispose.
     * @throws Exception
     */
    public void disposeTable(String dbPath, String tableName) throws Exception {
        ASpatialDb db = connection2DbsMap.get(dbPath);
        if (db == null)
            return;
        if (db.hasTable(tableName)) {
            List<String> openTables = connection2TablesMap.get(dbPath);
            if (openTables == null) {
                // this should not happen
                throw new IllegalArgumentException("The requested db has no open tables.");
            }
            if (!openTables.contains(tableName)) {
                throw new IllegalArgumentException("The requested db does not have an open table: " + tableName);
            }

            openTables.remove(tableName);

            if (openTables.size() == 0) {
                // also close the connection to the db and remove it
                connection2TablesMap.remove(dbPath);
                ASpatialDb removed = connection2DbsMap.remove(dbPath);
                if (removed != null) {
                    removed.close();
                }
            }
        }
    }


    /**
     * .
     * Get the geometry type of a table in a db.
     *
     * @param dbPath    the db path
     * @param tableName the name of the table.
     * @return the {@link EGeometryType}.
     * @throws Exception
     */
    public EGeometryType getGeometryType(String dbPath, String tableName) throws Exception {
        ASpatialDb db = getDb(dbPath);
        GeometryColumn geometryColumn = db.getGeometryColumnsForTable(tableName);
        return geometryColumn.geometryType;
    }

    public Style getStyleForTable(String dbPath, String tableName, String labelField) throws Exception {
        ASpatialDb db = getDb(dbPath);
        BasicStyle bs = ((GPGeopackageDb) db).getBasicStyle(tableName);
        Style st = new Style();
        st.name = tableName;
        int i = 1;
        st.id = bs.id;
        st.size = (float) bs.size;
        st.fillcolor = bs.fillcolor;
        st.strokecolor = bs.strokecolor;
        st.fillalpha = (float) bs.fillalpha;
        st.strokealpha = (float) bs.strokealpha;
        st.shape = bs.shape;
        st.width = (float) bs.width;
        st.labelsize = (float) bs.labelsize;
        st.labelfield = bs.labelfield;
        st.labelvisible = bs.labelvisible;
        st.enabled = bs.enabled;
        st.order = bs.order;
        st.dashPattern = bs.dashPattern;
        st.minZoom = bs.minZoom;
        st.maxZoom = bs.maxZoom;
        st.decimationFactor = (float) bs.decimationFactor;
        return st;
    }

    public List<Geometry> getGeometries(String dbPath, String tableName, Style gpStyle) throws Exception {
        ASpatialDb db = getDb(dbPath);
        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
        String query = buildGeometriesInBoundsQuery(db, tableName, gCol, gpStyle, null);

        IGeometryParser gp = db.getType().getGeometryParser();
        List<Geometry> geoms = db.execOnConnection(connection -> {
            List<Geometry> tmp = new ArrayList<>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Geometry geometry = gp.fromResultSet(rs, 1);
                    if (geometry != null) {
                        String label = rs.getString(2);
                        geometry.setUserData(label + LABEL_THEME_SEPARATOR + "");
                        tmp.add(geometry);
                    }
                }
            }
            return tmp;
        });
        return geoms;
    }


    /**
     * Create data query.
     *
     * @param db                  the db to use.ASpatialDb
     * @param tableName           the table to query.
     * @param tableGeometryColumn the table geom column.
     * @param tableStyle          the table style.
     * @param env                 optional envelope.
     * @return the query.
     */
    public static String buildGeometriesInBoundsQuery(ASpatialDb db, String tableName, GeometryColumn tableGeometryColumn, Style tableStyle, Envelope env) throws Exception {
        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT ");
        qSb.append(tableGeometryColumn.geometryColumnName);
        if (tableStyle.labelvisible == 1) {
            qSb.append(",");
            qSb.append(tableStyle.labelfield);
        } else {
            qSb.append(",'" + DUMMY + "'");
        }
        qSb.append(" FROM ");
        qSb.append("\"").append(tableName).append("\"");

        if (env != null) {
            double x1 = env.getMinX();
            double y1 = env.getMinY();
            double x2 = env.getMaxX();
            double y2 = env.getMaxY();
            String spatialindexBBoxWherePiece = db.getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
            qSb.append(" WHERE ").append(spatialindexBBoxWherePiece);
        }
        String q = qSb.toString();
        return q;
    }

    public static String buildGetFirstGeometry(ASpatialDb db, String tableName, GeometryColumn tableGeometryColumn) {
        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT ");
        qSb.append(tableGeometryColumn.geometryColumnName);
        qSb.append(" FROM ");
        qSb.append("\"").append(tableName).append("\" limit 1");
        String q = qSb.toString();
        return q;
    }


    public Geometry getFirstGeometry(String dbPath, String tableName) throws Exception {
        ASpatialDb db = getDb(dbPath);
        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
        String query = buildGetFirstGeometry(db, tableName, gCol);

        IGeometryParser gp = db.getType().getGeometryParser();
        return db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    Geometry geometry = gp.fromResultSet(rs, 1);
                    return geometry;
                }
            }
            return null;
        });
    }

    public ASpatialDb getDb(String dbPath) throws Exception {
        ASpatialDb geopackageDb = connection2DbsMap.get(dbPath);
        if (geopackageDb == null) {
            geopackageDb = EDb.GEOPACKAGE4ANDROID.getSpatialDb();
            geopackageDb.open(dbPath);
            connection2DbsMap.put(dbPath, geopackageDb);
        }
        return geopackageDb;
    }
}
