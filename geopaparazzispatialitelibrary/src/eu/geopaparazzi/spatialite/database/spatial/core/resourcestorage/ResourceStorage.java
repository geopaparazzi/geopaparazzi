package eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import jsqlite.Callback;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * Stores resources (such as images) associated with a particular feature.
 * It can store different types of files (PDFs, videos, etc) using ExternalResourceType.
 *
 * The current implementation only stores the path to the resource on the
 * device filesystem, but it could be easily extended to store the resources
 * as a blobs instead.
 *
 * The resources (or resource paths) are stored on an auxiliar table within the
 * Spatialite database that contains the features.
 *
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
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
    private Database database;

    protected ResourceStorage(String tableName, Database database) {
        this.tableName = tableName;
        this.database = database;
    }

    public String getTableName() {
        return this.tableName;
    }
    public String getDbFile() {
        return this.database.getFilename();
    }

    public List<ExternalResource> getExternalResources(long rowIdFk, AbstractResource.ResourceType type) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SELECT ");
        buffer.append(ID_FIELD).append(", ").append(RESPATH_FIELD).append(", ").append(RESNAME_FIELD);
        buffer.append(" FROM ").append(AUX_TABLE_NAME);
        buffer.append(" WHERE ");
        buffer.append(RESTABLE_FIELD).append("='").append(this.tableName).append("' AND ");
        buffer.append(ROWFK_FIELD).append("=").append(rowIdFk).append(" AND ");
        buffer.append(RESTYPE_FIELD).append("='").append(type.toString()).append("'");

        String sqlCommand = buffer.toString();
        Stmt statement = null;
        ArrayList<ExternalResource> result = new ArrayList<ExternalResource>();
        try {
            statement = database.prepare(sqlCommand);
            while (statement.step()) {
                long id = statement.column_long(0);
                String path = statement.column_string(1);
                String name = statement.column_string(2);
                result.add(new ExternalResource(id, path, name, type));
            }
        } catch (Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                }
            }
        }

        return result;
    }

    public ExternalResource getExternalResource(long id) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SELECT ");
        buffer.append(RESPATH_FIELD).append(", ").append(RESNAME_FIELD).append(", ").append(RESTYPE_FIELD);
        buffer.append(" FROM ").append(AUX_TABLE_NAME);
        buffer.append(" WHERE ");
        buffer.append(RESTABLE_FIELD).append("='").append(this.tableName).append("' AND ");
        buffer.append(ID_FIELD).append("=").append(id);

        String sqlCommand = buffer.toString();
        Stmt statement = null;
        try {
            statement = database.prepare(sqlCommand);
            if (statement.step()) {
                String path = statement.column_string(0);
                String name = statement.column_string(1);
                String typeStr = statement.column_string(2);
                AbstractResource.ResourceType type = AbstractResource.ResourceType.valueOf(typeStr);
                return new ExternalResource(id, path, name, type);
            }
        } catch (Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    public List<BlobResource> getBlobResources(long rowIdFk, BlobResource.ResourceType type) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SELECT ");
        buffer.append(ID_FIELD).append(", ").append(RESBLOB_FIELD).append(", ").append(RESNAME_FIELD);
        buffer.append(", ").append(RESBLOBTHUMB_FIELD);
        buffer.append(" FROM ").append(AUX_TABLE_NAME);
        buffer.append(" WHERE ");
        buffer.append(RESTABLE_FIELD).append("='").append(this.tableName).append("' AND ");
        buffer.append(ROWFK_FIELD).append("=").append(rowIdFk).append(" AND ");
        buffer.append(RESTYPE_FIELD).append("='").append(type.toString()).append("'");

        String sqlCommand = buffer.toString();
        Stmt statement = null;
        ArrayList<BlobResource> result = new ArrayList<BlobResource>();
        try {
            statement = database.prepare(sqlCommand);
            while (statement.step()) {
                long id = statement.column_long(0);
                byte[] data = statement.column_bytes(1);
                String name = statement.column_string(2);
                byte[] thumbnail = statement.column_bytes(3);
                BlobResource res = new BlobResource(id, data, name, type);
                res.setThumbnail(thumbnail);
                result.add(res);
            }
        } catch (Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                }
            }
        }

        return result;
    }

    public List<BlobResource> getBlobThumbnails(long rowIdFk, BlobResource.ResourceType type) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SELECT ");
        buffer.append(ID_FIELD).append(", ").append(RESBLOBTHUMB_FIELD).append(", ").append(RESNAME_FIELD);
        buffer.append(" FROM ").append(AUX_TABLE_NAME);
        buffer.append(" WHERE ");
        buffer.append(RESTABLE_FIELD).append("='").append(this.tableName).append("' AND ");
        buffer.append(ROWFK_FIELD).append("=").append(rowIdFk).append(" AND ");
        buffer.append(RESTYPE_FIELD).append("='").append(type.toString()).append("'");

        String sqlCommand = buffer.toString();
        Stmt statement = null;
        ArrayList<BlobResource> result = new ArrayList<BlobResource>();
        try {
            statement = database.prepare(sqlCommand);
            while (statement.step()) {
                long id = statement.column_long(0);
                byte[] thumbnail = statement.column_bytes(1);
                String name = statement.column_string(2);
                BlobResource res = new BlobResource(id, null, name, type);
                res.setThumbnail(thumbnail);
                result.add(res);
            }
        } catch (Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                }
            }
        }

        return result;
    }


    public BlobResource getBlobResource(long id) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SELECT ");
        buffer.append(RESBLOB_FIELD).append(", ").append(RESNAME_FIELD).append(", ").append(RESTYPE_FIELD);
        buffer.append(", ").append(RESBLOBTHUMB_FIELD);
        buffer.append(" FROM ").append(AUX_TABLE_NAME);
        buffer.append(" WHERE ");
        buffer.append(RESTABLE_FIELD).append("='").append(this.tableName).append("' AND ");
        buffer.append(ID_FIELD).append("=").append(id);

        String sqlCommand = buffer.toString();
        Stmt statement = null;
        try {
            statement = database.prepare(sqlCommand);
            if (statement.step()) {
                byte[] data = statement.column_bytes(0);
                String name = statement.column_string(1);
                String typeStr = statement.column_string(2);
                AbstractResource.ResourceType type = AbstractResource.ResourceType.valueOf(typeStr);
                byte[] thumbnail = statement.column_bytes(3);
                BlobResource res = new BlobResource(id, data, name, type);
                res.setThumbnail(thumbnail);
                return res;
            }
        } catch (Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                }
            }
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
        buffer.append(res.getType().toString()).append("', '");
        buffer.append(res.getName()).append("', '");
        buffer.append(res.getPath());
        buffer.append("' )");

        //String[] args = new String[] {this.tableName, Long.toString(rowId), type.toString(), res.getName(), res.getPath()};
        String sqlCommand = buffer.toString();
        Stmt stament = null;
        try {
            stament = database.prepare(sqlCommand);
            stament.step();
        } catch (Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
            if (stament!=null) {
                try {
                    stament.close();
                } catch (Exception e) {}
            }
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

        //String[] args = new String[] {this.tableName, Long.toString(rowId), type.toString(), res.getName(), res.getPath()};
        String sqlCommand = buffer.toString();
        Stmt statement = null;
        try {
            statement = database.prepare(sqlCommand);
            statement.bind(1, this.tableName);
            statement.bind(2, rowIdFk);
            statement.bind(3, res.getType().toString());
            statement.bind(4, res.getName());
            statement.bind(5, res.getBlob());
            statement.bind(6, res.getThumbnail());
            statement.step();
        } catch (Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
            if (statement!=null) {
                try {
                    statement.close();
                } catch (Exception e) {}
            }
        }
    }

    public void deleteResource(long rowId) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("DELETE FROM ").append(AUX_TABLE_NAME);
        buffer.append(" WHERE ");
        buffer.append(ID_FIELD).append("=").append(rowId);
        String sqlCommand = buffer.toString();
        Stmt statement = null;
        try {
            statement = database.prepare(sqlCommand);
            statement.step();
        } catch (Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
            if (statement!=null) {
                try {
                    statement.close();
                } catch (Exception e) {}
            }
        }
    }

    public void deleteResource(AbstractResource resource) {
        if (resource instanceof ExternalResource) {
            String imgPath = ((ExternalResource)resource).getPath();
            File f = new File(imgPath);
            if (f.exists()) {
                f.delete();
            }
        }
        deleteResource(resource.getId());
    }

    public static ResourceStorage getStorage(String tableName, String databasePath) {
        SpatialiteDatabaseHandler spatialiteDatabaseHandler = SpatialiteSourcesManager.INSTANCE.getExistingDatabaseHandlerByPath(databasePath);
        Database database = spatialiteDatabaseHandler.getDatabase();

        if (!checkResTableExists(database)) {
            addResTable(database);
        }
        return new ResourceStorage(tableName, database);
    }

    public static boolean checkResTableExists(Database database) {
        String sqlCommand = "SELECT name FROM sqlite_master WHERE type='table' AND name='"+ AUX_TABLE_NAME +"'";
        Stmt statement = null;
        try {
            statement = database.prepare(sqlCommand);
            if (statement.step()) {
                return true;
            }
        } catch (Exception e) {
            GPLog.error("DAO" +
                    "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    public static void addResTable(Database database) {
        String sqlCommand = String.format("CREATE TABLE %s (%s integer PRIMARY KEY NOT NULL, %s text, %s integer, %s TEXT, %s TEXT, %s TEXT, %s BLOB, %s BLOB)",
                AUX_TABLE_NAME, ID_FIELD, RESTABLE_FIELD, ROWFK_FIELD, RESTYPE_FIELD, RESNAME_FIELD,  RESPATH_FIELD, RESBLOB_FIELD, RESBLOBTHUMB_FIELD);
        try {
            database.exec(sqlCommand, new Callback() {
                @Override
                public void columns(String[] coldata) {}

                @Override
                public void types(String[] types) {}

                @Override
                public boolean newrow(String[] rowdata) {
                    return false;
                }
            });
        } catch (Exception e) {
            GPLog.error("DAO" +
                            "SPATIALITE",
                    "Error in checkResTableExists sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
        }

    }

}
