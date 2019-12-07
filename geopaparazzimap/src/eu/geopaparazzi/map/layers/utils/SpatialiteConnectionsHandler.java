package eu.geopaparazzi.map.layers.utils;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.geopaparazzi.library.style.Style;

public enum SpatialiteConnectionsHandler {
    INSTANCE;

    HashMap<String, List<String>> connection2TablesMap = new HashMap<>();
    HashMap<String, ASpatialDb> connection2DbsMap = new HashMap<>();

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
        Style style4Table = SpatialiteUtilities.getStyle4Table(db, tableName, labelField);
        return style4Table;
    }

    public List<Geometry> getGeometries(String dbPath, String tableName, Style gpStyle) throws Exception {
        ASpatialDb db = getDb(dbPath);
        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
        String query = SpatialiteUtilities.buildGeometriesInBoundsQuery(db, tableName, gCol, gpStyle, 4326, null);

        IGeometryParser gp = db.getType().getGeometryParser();
        List<Geometry> geoms = db.execOnConnection(connection -> {
            List<Geometry> tmp = new ArrayList<>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Geometry geometry = gp.fromResultSet(rs, 1);
                    if (geometry != null) {
                        String label = rs.getString(2);
                        String theme = rs.getString(3);
                        geometry.setUserData(label + SpatialiteUtilities.LABEL_THEME_SEPARATOR + theme);
                        tmp.add(geometry);
                    }
                }
            }
            return tmp;
        });
        return geoms;
    }

    public Geometry getFirstGeometry(String dbPath, String tableName) throws Exception {
        ASpatialDb db = getDb(dbPath);
        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
        String query = SpatialiteUtilities.buildGetFirstGeometry(db, tableName, gCol, 4326);

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
        ASpatialDb spatialDb = connection2DbsMap.get(dbPath);
        if (spatialDb == null) {
            spatialDb = EDb.SPATIALITE4ANDROID.getSpatialDb();
            spatialDb.open(dbPath);
            connection2DbsMap.put(dbPath, spatialDb);
        }
        return spatialDb;
    }
}
