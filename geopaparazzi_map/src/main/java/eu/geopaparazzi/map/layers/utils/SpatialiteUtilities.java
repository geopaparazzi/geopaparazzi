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

package eu.geopaparazzi.map.layers.utils;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.datatypes.EDataType;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.Style;
import eu.geopaparazzi.map.features.Feature;

/**
 * geopaparazzi related database utilities.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("ALL")
public class SpatialiteUtilities implements ISpatialiteTableAndFieldsNames {
    public static final String DUMMY = "dummy";
    public static final String ROWID_PK = "ROWID";
    public static final String LABEL_THEME_SEPARATOR = "@@";

    /**
     * Array of fields that will be ingored in attributes handling.
     */
    public static String[] IGNORED_FIELDS = {"ROWID", "_id"};


    /**
     * The complete list of fields in the properties table.
     */
    public static List<String> PROPERTIESTABLE_FIELDS_LIST;

    static {
        List<String> fieldsList = new ArrayList<String>();
        fieldsList.add(ID);
        fieldsList.add(NAME);
        fieldsList.add(SIZE);
        fieldsList.add(FILLCOLOR);
        fieldsList.add(STROKECOLOR);
        fieldsList.add(FILLALPHA);
        fieldsList.add(STROKEALPHA);
        fieldsList.add(SHAPE);
        fieldsList.add(WIDTH);
        fieldsList.add(LABELSIZE);
        fieldsList.add(LABELFIELD);
        fieldsList.add(LABELVISIBLE);
        fieldsList.add(ENABLED);
        fieldsList.add(ORDER);
        fieldsList.add(DASH);
        fieldsList.add(MINZOOM);
        fieldsList.add(MAXZOOM);
        fieldsList.add(DECIMATION);
        fieldsList.add(THEME);
        PROPERTIESTABLE_FIELDS_LIST = Collections.unmodifiableList(fieldsList);
    }

    /**
     * Create the properties table.
     *
     * @param database the db to use.
     * @throws Exception if something goes wrong.
     */
    public static void createPropertiesTable(ASpatialDb database) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(PROPERTIESTABLE);
        sb.append(" (");
        sb.append(ID);
        sb.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sb.append(NAME).append(" TEXT, ");
        sb.append(SIZE).append(" REAL, ");
        sb.append(FILLCOLOR).append(" TEXT, ");
        sb.append(STROKECOLOR).append(" TEXT, ");
        sb.append(FILLALPHA).append(" REAL, ");
        sb.append(STROKEALPHA).append(" REAL, ");
        sb.append(SHAPE).append(" TEXT, ");
        sb.append(WIDTH).append(" REAL, ");
        sb.append(LABELSIZE).append(" REAL, ");
        sb.append(LABELFIELD).append(" TEXT, ");
        sb.append(LABELVISIBLE).append(" INTEGER, ");
        sb.append(ENABLED).append(" INTEGER, ");
        sb.append(ORDER).append(" INTEGER,");
        sb.append(DASH).append(" TEXT,");
        sb.append(MINZOOM).append(" INTEGER,");
        sb.append(MAXZOOM).append(" INTEGER,");
        sb.append(DECIMATION).append(" REAL,");
        sb.append(THEME).append(" TEXT");
        sb.append(" );");
        String query = sb.toString();
        database.executeInsertUpdateDeleteSql(query);
    }

    /**
     * Create a default properties table for a spatial table.
     *
     * @param database   the db to use.
     * @param tableName  the spatial table's unique name to create the property record for.
     * @param labelField the filed of the table to use as label.
     * @return the created style object.
     * @throws Exception if something goes wrong.
     */
    public static Style createDefaultPropertiesForTable(ASpatialDb database, String tableName,
                                                        String labelField) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("insert into ").append(PROPERTIESTABLE);
        sbIn.append(" ( ");
        sbIn.append(NAME).append(" , ");
        sbIn.append(SIZE).append(" , ");
        sbIn.append(FILLCOLOR).append(" , ");
        sbIn.append(STROKECOLOR).append(" , ");
        sbIn.append(FILLALPHA).append(" , ");
        sbIn.append(STROKEALPHA).append(" , ");
        sbIn.append(SHAPE).append(" , ");
        sbIn.append(WIDTH).append(" , ");
        sbIn.append(LABELSIZE).append(" , ");
        sbIn.append(LABELFIELD).append(" , ");
        sbIn.append(LABELVISIBLE).append(" , ");
        sbIn.append(ENABLED).append(" , ");
        sbIn.append(ORDER).append(" , ");
        sbIn.append(DASH).append(" ,");
        sbIn.append(MINZOOM).append(" ,");
        sbIn.append(MAXZOOM).append(" ,");
        sbIn.append(DECIMATION);
        sbIn.append(" ) ");
        sbIn.append(" values ");
        sbIn.append(" ( ");
        Style style = new Style();
        style.name = tableName;
        style.labelfield = labelField;
        sbIn.append(style.insertValuesString());
        sbIn.append(" );");

        String insertQuery = sbIn.toString();
        database.executeInsertUpdateDeleteSql(insertQuery);

        return style;
    }

    /**
     * Deletes the style properties table.
     *
     * @param database the db to use.
     * @throws Exception if something goes wrong.
     */
    public static void deleteStyleTable(ASpatialDb database) throws Exception {
        GPLog.addLogEntry("Resetting style table for: " + database.getDatabasePath());
        StringBuilder sbSel = new StringBuilder();
        sbSel.append("drop table if exists " + PROPERTIESTABLE + ";");

        database.executeInsertUpdateDeleteSql(sbSel.toString());
    }


    /**
     * Update the style name in the properties table.
     *
     * @param database the db to use.
     * @param name     the new name.
     * @param id       the record id of the style.
     * @throws Exception if something goes wrong.
     */
    public static void updateStyleName(ASpatialDb database, String name, long id) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update ").append(PROPERTIESTABLE);
        sbIn.append(" set ");
        sbIn.append(NAME).append("='").append(name).append("'");
        sbIn.append(" where ");
        sbIn.append(ID);
        sbIn.append("=");
        sbIn.append(id);

        String updateQuery = sbIn.toString();
        database.executeInsertUpdateDeleteSql(updateQuery);
    }

    /**
     * Update a style definition.
     *
     * @param database the db to use.
     * @param style    the {@link Style} to set.
     * @throws Exception if something goes wrong.
     */
    public static void updateStyle(ASpatialDb database, Style style) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update ").append(PROPERTIESTABLE);
        sbIn.append(" set ");
        // sbIn.append(NAME).append("='").append(style.name).append("' , ");
        sbIn.append(SIZE).append("=").append(style.size).append(" , ");
        sbIn.append(FILLCOLOR).append("='").append(style.fillcolor).append("' , ");
        sbIn.append(STROKECOLOR).append("='").append(style.strokecolor).append("' , ");
        sbIn.append(FILLALPHA).append("=").append(style.fillalpha).append(" , ");
        sbIn.append(STROKEALPHA).append("=").append(style.strokealpha).append(" , ");
        sbIn.append(SHAPE).append("='").append(style.shape).append("' , ");
        sbIn.append(WIDTH).append("=").append(style.width).append(" , ");
        sbIn.append(LABELSIZE).append("=").append(style.labelsize).append(" , ");
        sbIn.append(LABELFIELD).append("='").append(style.labelfield).append("' , ");
        sbIn.append(LABELVISIBLE).append("=").append(style.labelvisible).append(" , ");
        sbIn.append(ENABLED).append("=").append(style.enabled).append(" , ");
        sbIn.append(ORDER).append("=").append(style.order).append(" , ");
        sbIn.append(DASH).append("='").append(style.dashPattern).append("' , ");
        sbIn.append(MINZOOM).append("=").append(style.minZoom).append(" , ");
        sbIn.append(MAXZOOM).append("=").append(style.maxZoom).append(" , ");
        sbIn.append(DECIMATION).append("=").append(style.decimationFactor);
        sbIn.append(" where ");
        sbIn.append(NAME);
        sbIn.append("='");
        sbIn.append(style.name);
        sbIn.append("';");

        // NOTE THAT THE THEME STYLE IS READONLY RIGHT NOW AND NOT UPDATED

        String updateQuery = sbIn.toString();
        database.executeInsertUpdateDeleteSql(updateQuery);
    }

    /**
     * Retrieve the {@link Style} for a given table.
     *
     * @param database  the db to use.
     * @param tableName the table name.
     * @return the style.
     * @throws Exception if something goes wrong.
     */
    public static Style getStyle4Table(ASpatialDb database, String tableName, String labelField)
            throws Exception {

        if (!database.hasTable(PROPERTIESTABLE)) {
            createPropertiesTable(database);
        }


        boolean themeColumn = false;
        List<String[]> tableColumnInfos = database.getTableColumns(PROPERTIESTABLE);
        for (String[] columnInfo : tableColumnInfos) {
            if (columnInfo[0].equalsIgnoreCase(THEME)) themeColumn = true;
        }
        final boolean hasThemeColumn = themeColumn;

        StringBuilder sbSel = new StringBuilder();
        sbSel.append("select ");
        sbSel.append(ID).append(" , ");
        sbSel.append(SIZE).append(" , ");
        sbSel.append(FILLCOLOR).append(" , ");
        sbSel.append(STROKECOLOR).append(" , ");
        sbSel.append(FILLALPHA).append(" , ");
        sbSel.append(STROKEALPHA).append(" , ");
        sbSel.append(SHAPE).append(" , ");
        sbSel.append(WIDTH).append(" , ");
        sbSel.append(LABELSIZE).append(" , ");
        sbSel.append(LABELFIELD).append(" , ");
        sbSel.append(LABELVISIBLE).append(" , ");
        sbSel.append(ENABLED).append(" , ");
        sbSel.append(ORDER).append(" , ");
        sbSel.append(DASH).append(" , ");
        sbSel.append(MINZOOM).append(" , ");
        sbSel.append(MAXZOOM).append(" , ");
        sbSel.append(DECIMATION);
        if (hasThemeColumn) {
            sbSel.append(" , ");
            sbSel.append(THEME);
        }
        sbSel.append(" from ");
        sbSel.append(PROPERTIESTABLE);
        sbSel.append(" where ");
        sbSel.append(NAME).append(" ='").append(tableName).append("';");

        String selectQuery = sbSel.toString();

        Style style = database.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(selectQuery)) {
                if (rs.next()) {

                    Style st = new Style();
                    st.name = tableName;
                    int i = 1;
                    st.id = rs.getLong(i++);
                    st.size = rs.getFloat(i++);
                    st.fillcolor = rs.getString(i++);
                    st.strokecolor = rs.getString(i++);
                    st.fillalpha = rs.getFloat(i++);
                    st.strokealpha = rs.getFloat(i++);
                    st.shape = rs.getString(i++);
                    st.width = rs.getFloat(i++);
                    st.labelsize = rs.getFloat(i++);
                    st.labelfield = rs.getString(i++);
                    st.labelvisible = rs.getInt(i++);
                    st.enabled = rs.getInt(i++);
                    st.order = rs.getInt(i++);
                    st.dashPattern = rs.getString(i++);
                    st.minZoom = rs.getInt(i++);
                    st.maxZoom = rs.getInt(i++);
                    st.decimationFactor = rs.getFloat(i++);

                    String theme = null;
                    if (hasThemeColumn) {
                        theme = rs.getString(i++);
                    }
                    if (theme != null && theme.trim().length() > 0) {
                        try {
                            JSONObject root = new JSONObject(theme);
                            if (root.has(UNIQUEVALUES)) {
                                JSONObject sub = root.getJSONObject(UNIQUEVALUES);
                                String fieldName = sub.keys().next();
                                st.themeField = fieldName;
                                JSONObject fieldObject = sub.getJSONObject(fieldName);
                                st.themeMap = new HashMap<>();
                                Iterator<String> keys = fieldObject.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    JSONObject styleObject = fieldObject.getJSONObject(key);

                                    Style themeStyle = getStyle(styleObject);
                                    st.themeMap.put(key, themeStyle);
                                }
                            }
                        } catch (JSONException e) {
                            GPLog.error("SpatialiteUtilities", null, e);
                        }
                    }

                    return st;
                }
            }
            return null;
        });

        if (style == null) {
            style = createDefaultPropertiesForTable(database, tableName, labelField);
        }

        return style;
    }

    private static Style getStyle(JSONObject styleObject) throws JSONException {
        Style style = new Style();

        if (styleObject.has(ID))
            style.id = styleObject.getLong(ID);
        if (styleObject.has(NAME))
            style.name = styleObject.getString(NAME);
        if (styleObject.has(SIZE))
            style.size = styleObject.getInt(SIZE);
        if (styleObject.has(FILLCOLOR))
            style.fillcolor = styleObject.getString(FILLCOLOR);
        else
            style.fillcolor = null;
        if (styleObject.has(STROKECOLOR))
            style.strokecolor = styleObject.getString(STROKECOLOR);
        else
            style.strokecolor = null;
        if (styleObject.has(FILLALPHA))
            style.fillalpha = (float) styleObject.getDouble(FILLALPHA);
        if (styleObject.has(STROKEALPHA))
            style.strokealpha = (float) styleObject.getDouble(STROKEALPHA);
        if (styleObject.has(SHAPE))
            style.shape = styleObject.getString(SHAPE);
        if (styleObject.has(WIDTH))
            style.width = (float) styleObject.getDouble(WIDTH);
        if (styleObject.has(LABELSIZE))
            style.labelsize = (float) styleObject.getDouble(LABELSIZE);
        if (styleObject.has(LABELFIELD))
            style.labelfield = styleObject.getString(LABELFIELD);
        if (styleObject.has(LABELVISIBLE))
            style.labelvisible = styleObject.getInt(LABELVISIBLE);
        if (styleObject.has(ENABLED))
            style.enabled = styleObject.getInt(ENABLED);
        if (styleObject.has(ORDER))
            style.order = styleObject.getInt(ORDER);
        if (styleObject.has(DASH))
            style.dashPattern = styleObject.getString(DASH);
        if (styleObject.has(MINZOOM))
            style.minZoom = styleObject.getInt(MINZOOM);
        if (styleObject.has(MAXZOOM))
            style.maxZoom = styleObject.getInt(MAXZOOM);
        if (styleObject.has(DECIMATION))
            style.decimationFactor = (float) styleObject.getDouble(DECIMATION);
        return style;
    }


    /**
     * Create data query.
     *
     * @param db                  the db to use.
     * @param tableName           the table to query.
     * @param tableGeometryColumn the table geom column.
     * @param tableStyle          the table style.
     * @param destSrid            the destination srid.
     * @param env                 optional envelope.
     * @return the query.
     */
    public static String buildGeometriesInBoundsQuery(ASpatialDb db, String tableName, GeometryColumn tableGeometryColumn, Style tableStyle, int destSrid, Envelope env) {
        boolean doTransform = false;
        if (tableGeometryColumn.srid != destSrid) {
            doTransform = true;
        }

        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT ");
        qSb.append("ST_AsBinary(");
        qSb.append("CastToXY(");
        if (doTransform)
            qSb.append("ST_Transform(");
        qSb.append(tableGeometryColumn.geometryColumnName);
        if (doTransform) {
            qSb.append(",");
            qSb.append(destSrid);
            qSb.append(")");
        }
        qSb.append(")");
        qSb.append(")");
        if (tableStyle.labelvisible == 1) {
            qSb.append(",");
            qSb.append(tableStyle.labelfield);
        } else {
            qSb.append(",'" + DUMMY + "'");
        }
        if (tableStyle.themeField != null) {
            qSb.append(",");
            qSb.append(tableStyle.themeField);
        } else {
            qSb.append(",'" + DUMMY + "'");
        }
        qSb.append(" FROM ");
        qSb.append("\"").append(tableName).append("\"");

        if (env != null) {
            StringBuilder mbrSb = new StringBuilder();
            if (doTransform)
                mbrSb.append("ST_Transform(");
            mbrSb.append("BuildMBR(");
            mbrSb.append(env.getMinX());
            mbrSb.append(",");
            mbrSb.append(env.getMaxY());
            mbrSb.append(",");
            mbrSb.append(env.getMaxX());
            mbrSb.append(",");
            mbrSb.append(env.getMinY());
            if (doTransform) {
                mbrSb.append(",");
                mbrSb.append(destSrid);
                mbrSb.append("),");
                mbrSb.append(tableGeometryColumn.srid);
            }
            mbrSb.append(")");
            String mbr = mbrSb.toString();

            // the SpatialIndex would be searching for a square, the ST_Intersects the Geometry
            // the SpatialIndex could be fulfilled, but checking the Geometry could return the result
            // that it is not
            qSb.append(" WHERE ST_Intersects(");
            qSb.append(tableGeometryColumn.geometryColumnName);
            qSb.append(", ");
            qSb.append(mbr);
            qSb.append(") = 1 AND ");
            qSb.append(ROWID_PK);
            qSb.append("  IN (SELECT ");
            qSb.append(ROWID_PK);
            qSb.append(" FROM Spatialindex WHERE f_table_name ='");
            qSb.append(tableName);
            qSb.append("'");
            // if a table has more than 1 geometry, the column-name MUST be given, otherwise no results.
            qSb.append(" AND f_geometry_column = '");
            qSb.append(tableGeometryColumn.geometryColumnName);
            qSb.append("'");
            qSb.append(" AND search_frame = ");
            qSb.append(mbr);
            qSb.append(");");
        }
        String q = qSb.toString();
        return q;
    }

    public static String buildGetFirstGeometry(ASpatialDb db, String tableName, GeometryColumn tableGeometryColumn, int destSrid) {
        boolean doTransform = false;
        if (tableGeometryColumn.srid != destSrid) {
            doTransform = true;
        }

        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT ");
        qSb.append("ST_AsBinary(");
        qSb.append("CastToXY(");
        if (doTransform)
            qSb.append("ST_Transform(");
        qSb.append(tableGeometryColumn.geometryColumnName);
        if (doTransform) {
            qSb.append(",");
            qSb.append(destSrid);
            qSb.append(")");
        }
        qSb.append(")");
        qSb.append(")");
        qSb.append(" FROM ");
        qSb.append("\"").append(tableName).append("\" limit 1");

        String q = qSb.toString();
        return q;
    }

    /**
     * Checks if a field needs to be ignored.
     *
     * @param field the field to check.
     * @return <code>true</code> if the field needs to be ignored.
     */
    public static boolean doIgnoreField(String field) {
        for (String ingoredField : SpatialiteUtilities.IGNORED_FIELDS) {
            if (field.equals(ingoredField)) {
                return true;
            }
        }
        field = field.toUpperCase();
        if (DbsUtilities.reserverSqlWords.contains(field)) {
            return true;
        }
        return false;
    }

    private static String escapeString(String value) {
        return value.replaceAll("'", "''");
    }

    /**
     * Updates the alphanumeric values of a feature in the given database.
     *
     * @param database the database.
     * @param feature  the feature.
     * @throws Exception if something goes wrong.
     */
    public static void updateFeatureAlphanumericAttributes(ASpatialDb database, Feature feature) throws Exception {
        String tableName = feature.getTableName();
        List<String> attributeNames = feature.getAttributeNames();
        List<Object> attributeValues = feature.getAttributeValues();
        List<String> attributeTypes = feature.getAttributeTypes();

        int geometryIndex = feature.getGeometryIndex();
        int idIndex = feature.getIdIndex();

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update \"").append(tableName);
        sbIn.append("\" set ");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attributeNames.size(); i++) {
            if (i == idIndex || i == geometryIndex) {
                continue;
            }

            String fieldName = attributeNames.get(i);
            Object value = attributeValues.get(i);
            String type = attributeTypes.get(i);
            boolean ignore = doIgnoreField(fieldName);
            if (!ignore) {
                EDataType dataType = EDataType.getType4Name(type);
                String valueStr = "";
                if (value != null)
                    valueStr = value.toString();
                if (dataType == EDataType.TEXT || dataType == EDataType.DATE) {
                    valueStr = escapeString(valueStr);
                    sb.append(" , ").append(fieldName).append("='").append(valueStr).append("'");
                } else if ("".equals(valueStr)) {
                    sb.append(" , ").append(fieldName).append("=NULL");
                } else {
                    sb.append(" , ").append(fieldName).append("=").append(valueStr);
                }
            }
        }
        String valuesPart = sb.substring(3);

        String pkName = feature.getAttributeNames().get(idIndex);
        String pkValue = feature.getAttributeValues().get(idIndex).toString();
        sbIn.append(" ");
        sbIn.append(valuesPart);
        sbIn.append(" where ");
        sbIn.append(pkName);
        sbIn.append("=");
        sbIn.append(pkValue);

        String updateQuery = sbIn.toString();
        database.executeInsertUpdateDeleteSql(updateQuery);

        //SpatialVectorTable table = SpatialiteSourcesManager.INSTANCE.getTableFromFeature(feature);
        //createImageField(table);
    }

//    /**
//     * Retrieve the {@link Style} for all tables of a db.
//     *
//     * @param database the db to use.
//     * @return the list of styles or <code>null</code> if something went wrong.
//     */
//    public static List<Style> getAllStyles(ASpatialDb database) {
//        StringBuilder sbSel = new StringBuilder();
//        sbSel.append("select ");
//        sbSel.append(ID).append(" , ");
//        sbSel.append(NAME).append(" , ");
//        sbSel.append(SIZE).append(" , ");
//        sbSel.append(FILLCOLOR).append(" , ");
//        sbSel.append(STROKECOLOR).append(" , ");
//        sbSel.append(FILLALPHA).append(" , ");
//        sbSel.append(STROKEALPHA).append(" , ");
//        sbSel.append(SHAPE).append(" , ");
//        sbSel.append(WIDTH).append(" , ");
//        sbSel.append(LABELSIZE).append(" , ");
//        sbSel.append(LABELFIELD).append(" , ");
//        sbSel.append(LABELVISIBLE).append(" , ");
//        sbSel.append(ENABLED).append(" , ");
//        sbSel.append(ORDER).append(" , ");
//        sbSel.append(DASH).append(" , ");
//        sbSel.append(MINZOOM).append(" , ");
//        sbSel.append(MAXZOOM).append(" , ");
//        sbSel.append(DECIMATION);
//        sbSel.append(" from ");
//        sbSel.append(PROPERTIESTABLE);
//
//        String selectQuery = sbSel.toString();
//        Stmt stmt = null;
//        try {
//            stmt = database.prepare(selectQuery);
//            List<Style> stylesList = new ArrayList<Style>();
//            while (stmt.step()) {
//                Style style = new Style();
//                style.id = stmt.column_long(0);
//                style.name = stmt.column_string(1);
//                style.size = (float) stmt.column_double(2);
//                style.fillcolor = stmt.column_string(3);
//                style.strokecolor = stmt.column_string(4);
//                style.fillalpha = (float) stmt.column_double(5);
//                style.strokealpha = (float) stmt.column_double(6);
//                style.shape = stmt.column_string(7);
//                style.strokewidth = (float) stmt.column_double(8);
//                style.labelsize = (float) stmt.column_double(9);
//                style.labelfield = stmt.column_string(10);
//                style.labelvisible = stmt.column_int(11);
//                style.enabled = stmt.column_int(12);
//                style.order = stmt.column_int(13);
//                style.dashPattern = stmt.column_string(14);
//                style.minZoom = stmt.column_int(15);
//                style.maxZoom = stmt.column_int(16);
//                style.decimationFactor = (float) stmt.column_double(17);
//                stylesList.add(style);
//            }
//            return stylesList;
//        } catch (Exception e) {
//            GPLog.error("SpatialiteUtilities", null, e);
//            return null;
//        } finally {
//            try {
//                if (stmt != null)
//                    stmt.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//                return null;
//            }
//        }
//    }


}