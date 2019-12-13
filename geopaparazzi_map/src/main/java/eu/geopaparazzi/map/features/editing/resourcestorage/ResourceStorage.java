package eu.geopaparazzi.map.features.editing.resourcestorage;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.map.layers.ELayerTypes;
import eu.geopaparazzi.map.layers.utils.GeopackageConnectionsHandler;
import eu.geopaparazzi.map.layers.utils.SpatialiteConnectionsHandler;

/**
 * Stores resources (such as images) associated with a particular feature.
 * It can store different types of files (PDFs, videos, etc) using ExternalResourceType.
 * <p>
 * The current implementation only stores the path to the resource on the
 * device filesystem, but it could be easily extended to store the resources
 * as a blobs instead.
 * <p>
 * The resources (or resource paths) are stored on an auxiliar table within the
 * Spatialite database that contains the features.
 *
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("ALL")
public class ResourceStorage {
    // the auxiliary table used to store the resources
    public static final String AUX_TABLE_NAME = "geopap_resource";
    // the table fields
    public static final String ID_FIELD = "id";
    public static final String RESTABLE_FIELD = "restable";
    public static final String RESTYPE_FIELD = "type";
    public static final String RESNAME_FIELD = "resname";
    public static final String ROWFK_FIELD = "rowidfk";
    public static final String RESPATH_FIELD = "respath";
    public static final String RESBLOB_FIELD = "resblob";
    public static final String RESBLOBTHUMB_FIELD = "resthumb";

    // the layer to which resources will be linked
    private String tableName;
    private ASpatialDb database;

    protected ResourceStorage(String tableName, ASpatialDb database) {
        this.tableName = tableName;
        this.database = database;
    }

    public String getTableName() {
        return this.tableName;
    }

    public String getDbPath() {
        return this.database.getDatabasePath();
    }

    public List<Resource> getThumbnails(long rowIdFk) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SELECT ");
        buffer.append(ID_FIELD).append(", ").append(RESBLOBTHUMB_FIELD).append(", ").append(RESNAME_FIELD).append(", ").append(RESTYPE_FIELD);
        buffer.append(" FROM ").append(AUX_TABLE_NAME);
        buffer.append(" WHERE ");
        buffer.append(RESTABLE_FIELD).append("='").append(this.tableName).append("' AND ");
        buffer.append(ROWFK_FIELD).append("=").append(rowIdFk);

        String sqlCommand = buffer.toString();
        try {
            return database.execOnConnection(connection -> {
                ArrayList<Resource> result = new ArrayList<Resource>();
                try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sqlCommand)) {
                    while (rs.next()) {
                        int i = 1;
                        long id = rs.getLong(i++);
                        byte[] thumbnail = rs.getBytes(i++);
                        String name = rs.getString(i++);
                        String typeStr = rs.getString(i);
                        Resource.ResourceType type;
                        if (typeStr.startsWith("EXTERNAL_") || typeStr.startsWith("BLOB_")) {
                            // legacy resource type
                            type = Resource.ResourceType.valueOf(typeStr);
                        } else {
                            type = Resource.ResourceType.THUMBNAIL;
                        }
                        Resource res = new Resource(id, name, type);
                        res.setThumbnail(thumbnail);
                        result.add(res);
                    }
                    return result;
                }
            });
        } catch (java.lang.Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in getThumbnails sql[" + sqlCommand + "] db[" + getDbPath() + "]", e);
        }
        return new ArrayList<>();
    }

    protected ExternalResource buildExternalResource(long id, String path, String name, Resource.ResourceType type, String mimeType) {
        if (type != null) { // legacy resource type
            return new ExternalResource(id, path, name, type);
        }
        // new-style resource using mime type
        type = Resource.ResourceType.EXTERNAL_FILE;
        return new ExternalResource(id, path, name, type, mimeType);
    }

    protected BlobResource buildBlobResource(long id, byte[] data, String name, Resource.ResourceType type, String mimeType) {
        if (type != null) { // legacy resource type
            return new BlobResource(id, data, name, type);
        }
        // new-style resource using mime type
        type = Resource.ResourceType.BLOB_FILE;
        return new BlobResource(id, data, name, type, mimeType);
    }

    /**
     * Gets the resource which has the provided id PK
     *
     * @param id
     * @return
     */
    public Resource getResource(long id) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SELECT ");
        buffer.append(RESBLOB_FIELD).append(", ").append(RESNAME_FIELD)
                .append(", ").append(RESTYPE_FIELD)
                .append(", ").append(RESBLOBTHUMB_FIELD)
                .append(", ").append(RESPATH_FIELD);
        buffer.append(" FROM ").append(AUX_TABLE_NAME);
        buffer.append(" WHERE ");
        buffer.append(RESTABLE_FIELD).append("='").append(this.tableName).append("' AND ");
        buffer.append(ID_FIELD).append("=").append(id);

        String sqlCommand = buffer.toString();
        try {
            return database.execOnConnection(connection -> {
                try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sqlCommand)) {
                    if (rs.next()) {
                        int i = 1;
                        byte[] data = rs.getBytes(i++);
                        String name = rs.getString(i++);
                        String typeStr = rs.getString(i++);
                        byte[] thumbnail = rs.getBytes(i++);
                        String path = rs.getString(i++);

                        Resource.ResourceType type = null;
                        Resource res;
                        if (typeStr.startsWith("EXTERNAL_") || typeStr.startsWith("BLOB_")) {
                            // legacy resource type
                            type = Resource.ResourceType.valueOf(typeStr);
                        }
                        if (data == null || data.length == 0) { // external resource
                            res = buildExternalResource(id, path, name, type, typeStr);
                        } else { // blob resource
                            res = buildBlobResource(id, data, name, type, typeStr);
                        }
                        res.setThumbnail(thumbnail);
                        return res;
                    }
                    return null;
                }
            });
        } catch (java.lang.Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in getResource sql[" + sqlCommand + "] db[" + database.getDatabasePath() + "]", e);
        }

        return null;
    }

    public void insertResource(long rowIdFk, ExternalResource res) {
        // INSERT INTO AUX_TABLE_NAME
        // (RESTABLE_FIELD. ROWFK_FIELD. RESTYPE_FIELD, RESNAME_FIELD, RESPATH_FIELD) VALUES ()
        StringBuffer buffer = new StringBuffer();
        buffer.append("INSERT INTO ").append(AUX_TABLE_NAME);
        buffer.append(" (");
        buffer.append(RESTABLE_FIELD).append(", ");
        buffer.append(ROWFK_FIELD).append(", ");
        buffer.append(RESTYPE_FIELD).append(", ");
        buffer.append(RESNAME_FIELD).append(", ");
        buffer.append(RESPATH_FIELD);
        buffer.append(") VALUES ('");
        buffer.append(this.tableName).append("', ");
        buffer.append(Long.toString(rowIdFk)).append(", '");
        if (res.getType() == Resource.ResourceType.BLOB_FILE
                || res.getType() == Resource.ResourceType.EXTERNAL_FILE) {
            // use mime type
            buffer.append(res.getMimeType()).append("', '");
        } else {  // legacy way, use type enum
            buffer.append(res.getType().toString()).append("', '");
        }
        buffer.append(res.getName()).append("', '");
        buffer.append(res.getPath());
        buffer.append("' )");

        String sqlCommand = buffer.toString();
        try {
            database.executeInsertUpdateDeleteSql(sqlCommand);
        } catch (java.lang.Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in insertResource sql[" + sqlCommand + "] db[" + database.getDatabasePath() + "]", e);
        }
    }

    public void insertResource(long rowIdFk, BlobResource res) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("INSERT INTO ").append(AUX_TABLE_NAME);
        buffer.append(" (");
        buffer.append(RESTABLE_FIELD).append(", ");
        buffer.append(ROWFK_FIELD).append(", ");
        buffer.append(RESTYPE_FIELD).append(", ");
        buffer.append(RESNAME_FIELD).append(", ");
        buffer.append(RESBLOB_FIELD).append(", ");
        buffer.append(RESBLOBTHUMB_FIELD);
        buffer.append(") VALUES (?, ?, ?, ?, ?, ?)");

        String sqlCommand = buffer.toString();
        try {
            String mimeType;
            if (res.getType() == Resource.ResourceType.EXTERNAL_FILE
                    || res.getType() == Resource.ResourceType.BLOB_FILE) {
                // use mime type
                mimeType = res.getMimeType();
            } else { // legacy way, use type enum
                mimeType = res.getType().toString();
            }
            database.executeInsertUpdateDeletePreparedSql(sqlCommand, new Object[]{tableName, rowIdFk, mimeType, res.getName(), res.getBlob(), res.getThumbnail()});
        } catch (java.lang.Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in insertResource sql[" + sqlCommand + "] db[" + database.getDatabasePath() + "]", e);
        }
    }

    public void deleteResource(long rowId) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("DELETE FROM ").append(AUX_TABLE_NAME);
        buffer.append(" WHERE ");
        buffer.append(ID_FIELD).append("=").append(rowId);
        String sqlCommand = buffer.toString();
        try {
            database.executeInsertUpdateDeleteSql(sqlCommand);
        } catch (java.lang.Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in deleteResource sql[" + sqlCommand + "] db[" + database.getDatabasePath() + "]", e);
        }
    }

    public void deleteResource(Resource resource) {
        if (resource instanceof ExternalResource) {
            String imgPath = ((ExternalResource) resource).getPath();
            File f = new File(imgPath);
            if (f.exists()) {
                f.delete();
            }
        }
        deleteResource(resource.getId());
    }

    public static ResourceStorage getStorage(String tableName, String databasePath) throws java.lang.Exception {
        ASpatialDb db = null;

        ELayerTypes layerType = ELayerTypes.fromFileExt(databasePath);
        if (layerType == ELayerTypes.SPATIALITE) {
            db = SpatialiteConnectionsHandler.INSTANCE.getDb(databasePath);
        } else if (layerType == ELayerTypes.GEOPACKAGE) {
            db = GeopackageConnectionsHandler.INSTANCE.getDb(databasePath);
        }

        if (!db.hasTable(AUX_TABLE_NAME)) {
            addResTable(db);
        }
        return new ResourceStorage(tableName, db);
    }


    public static void addResTable(ASpatialDb database) {
        String sqlCommand = String.format("CREATE TABLE %s (%s integer PRIMARY KEY NOT NULL, %s text, %s integer, %s TEXT, %s TEXT, %s TEXT, %s BLOB, %s BLOB)",
                AUX_TABLE_NAME, ID_FIELD, RESTABLE_FIELD, ROWFK_FIELD, RESTYPE_FIELD, RESNAME_FIELD, RESPATH_FIELD, RESBLOB_FIELD, RESBLOBTHUMB_FIELD);
        try {
            database.executeInsertUpdateDeleteSql(sqlCommand);
        } catch (java.lang.Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getDatabasePath() + "]", e);
        }

    }

}
