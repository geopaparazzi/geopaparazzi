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
package eu.geopaparazzi.spatialite.database.spatial.core.daos;

import com.vividsolutions.jts.geom.Geometry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.util.DataType;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteUtilities;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;


/**
 * Spatialite support methods.
 * <p/>
 * This class should contain a more user oriented tasks API.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoSpatialite implements ISpatialiteTableAndFieldsNames {

    /**
     * Collects the fields of a given table.
     * <p/>
     * <br>- name of Field
     * <br>- type of field as defined in Database
     *
     * @param database  the database to use.
     * @param tableName name of table to read.
     * @return the {@link HashMap} of fields: [name of field, type of field]
     * @throws Exception if something goes wrong.
     */
    public static HashMap<String, String> collectTableFields(Database database, String tableName) throws Exception {

        HashMap<String, String> fieldNamesToTypeMap = new LinkedHashMap<String, String>();
        String s_sql_command = "pragma table_info('" + tableName + "')";
        String tableType = "";
        String sqlCreationString = "";
        Stmt statement = null;
        String name = "";
        try {
            statement = database.prepare(s_sql_command);
            while (statement.step()) {
                name = statement.column_string(1);
                tableType = statement.column_string(2);
                sqlCreationString = statement.column_string(5); // pk
                // try to unify the data-types: varchar(??),int(11) mysql-syntax
                if (tableType.contains("int("))
                    tableType = "INTEGER";
                if (tableType.contains("varchar("))
                    tableType = "TEXT";
                // pk: 0 || 1;Data-TypeTEXT || DOUBLE || INTEGER || REAL || DATE || BLOB ||
                // geometry-types
                fieldNamesToTypeMap.put(name, sqlCreationString + ";" + tableType.toUpperCase(Locale.US));
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE",
                    "collectTableFields[" + tableName + "] sql[" + s_sql_command + "] db[" + database.getFilename()
                            + "]", e_stmt
            );
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return fieldNamesToTypeMap;
    }

    /**
     * Retrieves the {@link Database} containing a given table by its unique table name.
     *
     * @param uniqueTableName the table name.
     * @return the database the table is in.
     * @throws Exception
     */
    public static Database getDatabaseFromUniqueTableName(String uniqueTableName) throws Exception {
        SpatialVectorTable spatialTable = SpatialDatabasesManager.getInstance().getVectorTableByName(uniqueTableName);
        AbstractSpatialDatabaseHandler vectorHandler = SpatialDatabasesManager.getInstance().getVectorHandler(spatialTable);
        if (vectorHandler instanceof SpatialiteDatabaseHandler) {
            SpatialiteDatabaseHandler spatialiteDbHandler = (SpatialiteDatabaseHandler) vectorHandler;
            return spatialiteDbHandler.getDatabase();
        }
        return null;
    }

    /**
     * Retrieve the SpatialVectorTable from a unique table name.
     *
     * @param uniqueTableName the table name.
     * @return the table.
     * @throws Exception
     */
    public static SpatialVectorTable getSpatialVectorTableFromUniqueTableName(String uniqueTableName) throws Exception {
        return SpatialDatabasesManager.getInstance().getVectorTableByName(uniqueTableName);
    }


    /**
     * Attemt to retrieve row-count and bounds for this geometry field.
     *
     * @param database       the db to use.
     * @param tableName      the table of the db to use.
     * @param geometryColumn the geometry field of the table to use.
     * @return 'rows_count;min_x,min_y,max_x,max_y;datetimestamp_now'.
     * @throws Exception if something goes wrong.
     */
    public static String getGeometriesBoundsString(Database database, String tableName, String geometryColumn)
            throws Exception {
        StringBuilder queryBuilder = new StringBuilder();
        String s_vector_extent = "";
        // return the format used in DaoSpatialite.checkDatabaseTypeAndValidity()
        queryBuilder.append("SELECT count(");
        queryBuilder.append(geometryColumn);
        queryBuilder.append(")||';'||Min(MbrMinX(");
        queryBuilder.append(geometryColumn);
        queryBuilder.append("))||','||Min(MbrMinY(");
        queryBuilder.append(geometryColumn);
        queryBuilder.append("))||','||Max(MbrMaxX(");
        queryBuilder.append(geometryColumn);
        queryBuilder.append("))||','||Max(MbrMaxY(");
        queryBuilder.append(geometryColumn);
        queryBuilder.append("))||';'||strftime('%Y-%m-%dT%H:%M:%fZ','now')");
        queryBuilder.append(" FROM ");
        queryBuilder.append(tableName);
        queryBuilder.append(";");
        // ;617;7255796.59288944,246133.478270624,7395508.96772464,520956.218508861;2014-03-26T06:32:58.572Z
        String s_select_bounds = queryBuilder.toString();
        Stmt statement = null;
        try {
            statement = database.prepare(s_select_bounds);
            if (statement.step()) {
                if (statement.column_string(0) != null) { // The geometries may be null, thus
                    // returns null
                    s_vector_extent = statement.column_string(0);
                    return s_vector_extent;
                }
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "spatialiteRetrieveBounds sql[" + s_select_bounds + "] db[" + database.getFilename() + "]",
                    e_stmt);
        } finally {
            if (statement != null)
                statement.close();
        }
        return s_vector_extent;
    }

    /**
     * Attemt to count geometry field.
     * returned the number of Geometries that are NOT NULL
     * - no recovery attemts should be done when this returns 0
     * --- will abort attemts to recover if returns 0
     * --- this speeds up the loading by 50% in my case
     * VECTOR_LAYERS_QUERY_MODE=3 : about 5 seconds [before about 10 seconds]
     * VECTOR_LAYERS_QUERY_MODE=0 : about 2 seconds
     *
     * @param database       the db to use.
     * @param tableName      the table of the db to use.
     * @param geometryColumn the geometry field of the table to use.
     * @return count of Geometries NOT NULL
     * @throws Exception if something goes wrong.
     */
    public static int getGeometriesCount(Database database, String tableName, String geometryColumn) throws Exception {
        int i_count = 0;
        if ((tableName.equals("")) || (geometryColumn.equals("")))
            return i_count;
        // SELECT CreateSpatialIndex('prov2008_s','Geometry');
        String s_CountGeometries = "SELECT count('" + geometryColumn + "') FROM '" + tableName + "' WHERE '" + geometryColumn
                + "' IS NOT NULL;";
        Stmt statement = null;
        try {
            statement = database.prepare(s_CountGeometries);
            if (statement.step()) {
                i_count = statement.column_int(0);
                return i_count;
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "spatialiteCountGeometries sql[" + s_CountGeometries
                    + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null)
                statement.close();
        }
        return i_count;
    }


    /**
     * Delete a list of features in the given database.
     * <p/>
     * <b>The features need to be from the same table</b>
     *
     * @param features the features list.
     * @throws Exception if something goes wrong.
     */
    public static void deleteFeatures(List<Feature> features) throws Exception {
        Feature firstFeature = features.get(0);

        String uniqueTableName = firstFeature.getUniqueTableName();
        Database database = getDatabaseFromUniqueTableName(uniqueTableName);
        String tableName = firstFeature.getTableName();

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("delete from ").append(tableName);
        sbIn.append(" where ");

        StringBuilder sb = new StringBuilder();
        for (Feature feature : features) {
            sb.append(" OR ");
            sb.append(SpatialiteUtilities.SPATIALTABLE_ID_FIELD).append("=");
            sb.append(feature.getId());
        }
        String valuesPart = sb.substring(4);

        sbIn.append(valuesPart);

        String updateQuery = sbIn.toString();
        database.exec(updateQuery, null);
    }

    /**
     * Add a new spatial record by adding a geometry.
     * <p/>
     * <p>The other attributes will not be populated.
     *
     * @param geometry           the geometry that will create the new record.
     * @param geometrySrid       the srid of the geometry without the EPSG prefix.
     * @param spatialVectorTable the table into which to insert the record.
     * @throws Exception if something goes wrong.
     */
    public static void addNewFeatureByGeometry(Geometry geometry, String geometrySrid, SpatialVectorTable spatialVectorTable)
            throws Exception {
        String uniqueTableName = spatialVectorTable.getUniqueNameBasedOnDbFilePath();
        Database database = getDatabaseFromUniqueTableName(uniqueTableName);
        String tableName = spatialVectorTable.getTableName();
        String geometryFieldName = spatialVectorTable.getGeomName();
        String srid = spatialVectorTable.getSrid();
        int geomType = spatialVectorTable.getGeomType();
        GeometryType geometryType = GeometryType.forValue(geomType);
        String geometryTypeCast = geometryType.getGeometryTypeCast();
        String spaceDimensionsCast = geometryType.getSpaceDimensionsCast();
        String multiSingleCast = geometryType.getMultiSingleCast();

        // get list of non geom fields and default values
        String nonGeomFieldsNames = "";
        String nonGeomFieldsValues = "";
        for (String field : spatialVectorTable.getTableFieldNamesList()) {
            boolean ignore = SpatialiteUtilities.doIgnoreField(field);
            if (!ignore) {
                DataType tableFieldType = spatialVectorTable.getTableFieldType(field);
                if (tableFieldType != null) {
                    nonGeomFieldsNames = nonGeomFieldsNames + "," + field;
                    nonGeomFieldsValues = nonGeomFieldsValues + "," + tableFieldType.getDefaultValueForSql();
                }
            }
        }

        boolean doTransform = true;
        if (srid.equals(geometrySrid)) {
            doTransform = false;
        }

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("insert into ").append(tableName);
        sbIn.append(" (");
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
        database.exec(insertQuery, null);
    }

    /**
     * Updates the alphanumeric values of a feature in the given database.
     *
     * @param database the database.
     * @param feature  the feature.
     * @throws Exception if something goes wrong.
     */
    public static void updateFeatureAlphanumericAttributes(Database database, Feature feature) throws Exception {
        String tableName = feature.getTableName();
        List<String> attributeNames = feature.getAttributeNames();
        List<String> attributeValuesStrings = feature.getAttributeValuesStrings();
        List<String> attributeTypes = feature.getAttributeTypes();

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update ").append(tableName);
        sbIn.append(" set ");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attributeNames.size(); i++) {
            String fieldName = attributeNames.get(i);
            String value = attributeValuesStrings.get(i);
            String type = attributeTypes.get(i);
            boolean ignore = SpatialiteUtilities.doIgnoreField(fieldName);
            if (!ignore) {
                DataType dataType = DataType.getType4Name(type);
                if (dataType == DataType.TEXT) {
                    sb.append(" , ").append(fieldName).append("='").append(value).append("'");
                } else {
                    sb.append(" , ").append(fieldName).append("=").append(value);
                }
            }
        }
        String valuesPart = sb.substring(3);

        sbIn.append(" ");
        sbIn.append(valuesPart);
        sbIn.append(" where ");
        sbIn.append(SpatialiteUtilities.SPATIALTABLE_ID_FIELD);
        sbIn.append("=");
        sbIn.append(feature.getId());

        String updateQuery = sbIn.toString();
        database.exec(updateQuery, null);
    }

    /**
     * Updates the geometry of a feature in the given database.
     *
     * @throws Exception if something goes wrong.
     */
    public static void updateFeatureGeometry(String id, Geometry geometry, String geometrySrid, SpatialVectorTable spatialVectorTable)
            throws Exception {
        String uniqueTableName = spatialVectorTable.getUniqueNameBasedOnDbFilePath();
        Database database = getDatabaseFromUniqueTableName(uniqueTableName);
        String tableName = spatialVectorTable.getTableName();
        String geometryFieldName = spatialVectorTable.getGeomName();
        String srid = spatialVectorTable.getSrid();
        int geomType = spatialVectorTable.getGeomType();
        GeometryType geometryType = GeometryType.forValue(geomType);
        String geometryTypeCast = geometryType.getGeometryTypeCast();
        String spaceDimensionsCast = geometryType.getSpaceDimensionsCast();
        String multiSingleCast = geometryType.getMultiSingleCast();

        boolean doTransform = true;
        if (srid.equals(geometrySrid)) {
            doTransform = false;
        }

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update ").append(tableName);
        sbIn.append(" set ");
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
        sbIn.append(SpatialiteUtilities.SPATIALTABLE_ID_FIELD).append("=");
        sbIn.append(id);
        String insertQuery = sbIn.toString();
        database.exec(insertQuery, null);
    }

    /**
     * Get the area and length in original units of a feature by its id.
     *
     * @param id                 the id of the feature, as defined by field
     *                           {@link eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteUtilities#SPATIALTABLE_ID_FIELD}
     * @param spatialVectorTable the table in which the feature resides.
     * @return the array with [area, length].
     * @throws Exception if something goes wrong.
     */
    public static double[] getAreaAndLengthById(String id, SpatialVectorTable spatialVectorTable) throws Exception {
        String uniqueTableName = spatialVectorTable.getUniqueNameBasedOnDbFilePath();
        Database database = getDatabaseFromUniqueTableName(uniqueTableName);
        String tableName = spatialVectorTable.getTableName();
        String geomName = spatialVectorTable.getGeomName();

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("SELECT ");
        sbIn.append("Area(").append(geomName).append("),");
        sbIn.append("Length(").append(geomName).append(")");
        sbIn.append(" from ").append(tableName);
        sbIn.append(" where ");
        sbIn.append(SpatialiteUtilities.SPATIALTABLE_ID_FIELD).append(" = ").append(id);

        String selectQuery = sbIn.toString();
        Stmt statement = null;
        try {
            statement = database.prepare(selectQuery);
            if (statement.step()) {
                double area = statement.column_double(0);
                double length = statement.column_double(1);

                return new double[]{area, length};
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE",
                    "getAreaAndLengthById[" + tableName + "] sql[" + selectQuery + "] db[" + database.getFilename()
                            + "]", e_stmt
            );
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return null;
    }
}
