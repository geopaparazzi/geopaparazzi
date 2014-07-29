/*
* Geopaparazzi - Digital field mapping on Android based devices
* Copyright (C) 2010 HydroloGIS (www.hydrologis.com)
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers;

import android.content.Context;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ColorUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.TableTypes;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryIterator;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.DatabaseCreationAndProperties;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.GeopaparazziDatabaseProperties;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.SPL_Rasterlite;
import eu.geopaparazzi.spatialite.database.spatial.util.comparators.OrderComparator;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialiteDatabaseType;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteUtilities;
import eu.geopaparazzi.spatialite.database.spatial.util.Style;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

import static eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite.PROPERTIESTABLE;
import static eu.geopaparazzi.spatialite.database.spatial.core.daos.GeopaparazziDatabaseProperties.createDefaultPropertiesForTable;
import static eu.geopaparazzi.spatialite.database.spatial.core.daos.GeopaparazziDatabaseProperties.createPropertiesTable;
import static eu.geopaparazzi.spatialite.database.spatial.core.daos.GeopaparazziDatabaseProperties.deleteStyleTable;
import static eu.geopaparazzi.spatialite.database.spatial.core.daos.GeopaparazziDatabaseProperties.getAllStyles;
import static eu.geopaparazzi.spatialite.database.spatial.core.daos.GeopaparazziDatabaseProperties.getStyle4Table;
import static eu.geopaparazzi.spatialite.database.spatial.core.daos.GeopaparazziDatabaseProperties.updateStyleName;

/**
 * An utility class to handle the spatial database.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SpatialiteDatabaseHandler extends AbstractSpatialDatabaseHandler {

    private String uniqueDbName4DataProperties = "";

    private Database dbJava;

    private HashMap<String, Paint> fillPaints = new HashMap<String, Paint>();
    private HashMap<String, Paint> strokePaints = new HashMap<String, Paint>();

    private List<SpatialVectorTable> vectorTableList;
    private List<SpatialRasterTable> rasterTableList;

    private SpatialiteDatabaseType databaseType = null;

    // List of all SpatialView of Database [view_name,view_data] - parse for
    // 'geometry_column;min_x,min_y,max_x,max_y'
    private HashMap<String, String> spatialVectorMap = new HashMap<String, String>();
    // List of all SpatialView of Database [view_name,view_data] - that have errors
    private HashMap<String, String> spatialVectorMapErrors = new HashMap<String, String>();

    /**
     * Constructor.
     *
     * @param dbPath the path to the database this handler connects to.
     * @throws IOException if something goes wrong.
     */
    public SpatialiteDatabaseHandler(String dbPath) throws IOException {
        super(dbPath);
        try {
            try {
                Context context = GPApplication.getInstance();
                ResourcesManager resourcesManager = ResourcesManager.getInstance(context);
                File mapsDir = resourcesManager.getMapsDir();
                String mapsPath = mapsDir.getAbsolutePath();
                if (databasePath.startsWith(mapsPath)) {
                    // this should always be true
                    String relativePath = databasePath.substring(mapsPath.length());
                    StringBuilder sb = new StringBuilder();
                    if (relativePath.startsWith(File.separator)) {
                        relativePath = relativePath.substring(1);
                    }
                    sb.append(relativePath);
                    uniqueDbName4DataProperties = sb.toString();
                }
            } catch (java.lang.Exception e) {
                GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + databaseFile.getAbsolutePath() + "]", e);
            }
            dbJava = new jsqlite.Database();
            try {
                dbJava.open(databasePath, jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);
                isDatabaseValid = true;
            } catch (Exception e) {
                GPLog.error(this, "Database marked as invalid: " + databasePath, e);
                isDatabaseValid = false;
                GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + databaseFile.getAbsolutePath() + "].open has failed", e);
            }
            if (isValid()) {
                // check database and collect the views list
                try {
                    databaseType = DatabaseCreationAndProperties.checkDatabaseTypeAndValidity(dbJava, spatialVectorMap, spatialVectorMapErrors);
                } catch (Exception e) {
                    isDatabaseValid = false;
                }
                switch (databaseType) {
             /*
               if (spatialVectorMap.size() == 0) for SPATIALITE3/4
                --> DaoSpatialite.checkDatabaseTypeAndValidity will return SpatialiteDatabaseType.UNKNOWN
                -- there is nothing to load (database empty)
             */
                    case GEOPACKAGE:
                    case SPATIALITE3:
                    case SPATIALITE4:
                        isDatabaseValid = true;
                        break;
                    default:
                        isDatabaseValid = false;
                }
            }
            if (!isValid()) {
                close();
            } else { // avoid call for invalid databases [SpatialiteDatabaseType.UNKNOWN]
                checkAndUpdatePropertiesUniqueNames();
            }
        } catch (Exception e) {
            GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + databaseFile.getAbsolutePath() + "]", e);
        }
    }

    @Override
    public void open() {
    }

    /**
     * Is the database file considered valid?
     * <p/>
     * <br>- metadata table exists and has data
     * <br>- 'tiles' is either a table or a view and the correct fields exist
     * <br>-- if a view: do the tables map and images exist with the correct fields
     * <br>checking is done once when the 'metadata' is retrieved the first time [fetchMetadata()]
     *
     * @return true if valid, otherwise false
     */
    @Override
    public boolean isValid() {
        return isDatabaseValid;
    }

    @Override
    public List<SpatialVectorTable> getSpatialVectorTables(boolean forceRead) throws Exception {
        if (vectorTableList == null || forceRead) {
            vectorTableList = new ArrayList<SpatialVectorTable>();
            checkAndCollectTables();
        }
        return vectorTableList;
    }

    @Override
    public List<SpatialRasterTable> getSpatialRasterTables(boolean forceRead) throws Exception {
        if (rasterTableList == null || forceRead) {
            rasterTableList = new ArrayList<SpatialRasterTable>();
            checkAndCollectTables();
        }
        return rasterTableList;
    }

    /**
     * Checks if the table names in the properties table are defined properly.
     * <p/>
     * <p>The unique table name is a concatenation of:<br>
     * <b>dbPath#tablename#geometrytype</b>
     * <p>If the name doesn't start with the database path, it needs to
     * be updated. The rest is anyways unique inside the database.
     *
     * @throws Exception if something went wrong.
     */
    private void checkAndUpdatePropertiesUniqueNames() throws Exception {
        List<Style> allStyles = null;
        try {
            allStyles = getAllStyles(dbJava);
        } catch (java.lang.Exception e) {
            // ignore and create a default one
        }
        if (allStyles == null) {
            /*
            * something went wrong in the reading of the table,
            * which might be due to an upgrade of table structure.
            * Remove and recreate the table.
            */
            deleteStyleTable(dbJava);
            createPropertiesTable(dbJava);
        } else {
            for (Style style : allStyles) {
                if (!style.name.startsWith(uniqueDbName4DataProperties + SpatialiteUtilities.UNIQUENAME_SEPARATOR)) {
                    // need to update the name in the style and also in the database
                    String[] split = style.name.split(SpatialiteUtilities.UNIQUENAME_SEPARATOR);
                    if (split.length == 3) {
                        String newName = uniqueDbName4DataProperties + SpatialiteUtilities.UNIQUENAME_SEPARATOR + split[1]
                                + SpatialiteUtilities.UNIQUENAME_SEPARATOR + split[2];
                        style.name = newName;
                        updateStyleName(dbJava, newName, style.id);
                    }
                }
            }
        }
    }

    /**
     * Check availability of style for the tables.
     *
     * @throws Exception
     */
    private void checkPropertiesTable() throws Exception {
        int propertiesTableColumnCount = DatabaseCreationAndProperties.checkTableExistence(dbJava, PROPERTIESTABLE);
        if (propertiesTableColumnCount == 0) {
            createPropertiesTable(dbJava);
            for (SpatialVectorTable spatialTable : vectorTableList) {
                createDefaultPropertiesForTable(dbJava, spatialTable.getUniqueNameBasedOnDbFilePath(),
                        spatialTable.getLabelField());
            }
        }
    }

    public float[] getTableBounds(AbstractSpatialTable spatialTable) throws Exception {
        return spatialTable.getTableBounds();
    }

    /**
     * Get the fill {@link Paint} for a given style.
     * <p/>
     * <p>Paints are cached and reused.</p>
     *
     * @param style the {@link Style} to use.
     * @return the paint.
     */
    public Paint getFillPaint4Style(Style style) {
        Paint paint = fillPaints.get(style.name);
        if (paint == null) {
            paint = new Paint();
            fillPaints.put(style.name, paint);
        }
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ColorUtilities.toColor(style.fillcolor));
        float alpha = style.fillalpha * 255f;
        paint.setAlpha((int) alpha);
        return paint;
    }

    /**
     * Get the stroke {@link Paint} for a given style.
     * <p/>
     * <p>Paints are cached and reused.</p>
     *
     * @param style the {@link Style} to use.
     * @return the paint.
     */
    public Paint getStrokePaint4Style(Style style) {
        Paint paint = strokePaints.get(style.name);
        if (paint == null) {
            paint = new Paint();
            strokePaints.put(style.name, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Cap.ROUND);
        paint.setStrokeJoin(Join.ROUND);
        paint.setColor(ColorUtilities.toColor(style.strokecolor));
        float alpha = style.strokealpha * 255f;
        paint.setAlpha((int) alpha);
        paint.setStrokeWidth(style.width);

        String dashPattern = style.dashPattern;
        if (dashPattern.trim().length() > 0) {
            String[] split = dashPattern.split(",");
            if (split.length > 1) {
                float[] dash = new float[split.length];
                for (int i = 0; i < split.length; i++) {
                    try {
                        float tmpDash = Float.parseFloat(split[i].trim());
                        dash[i] = tmpDash;
                    } catch (NumberFormatException e) {
                        // ignore and set default
                        dash = new float[]{20f, 10f};
                        break;
                    }
                }
                paint.setPathEffect(new DashPathEffect(dash, 0));
            }
        }

        return paint;
    }

    /**
     * Retrieve list of WKB geometries from the given table in the given bounds.
     *
     * @param destSrid the destination srid.
     * @param table    the vector table.
     * @param n        north bound.
     * @param s        south bound.
     * @param e        east bound.
     * @param w        west bound.
     * @return list of WKB geometries.
     */
    public List<byte[]> getWKBFromTableInBounds(String destSrid, SpatialVectorTable table, double n, double s, double e, double w) {
        List<byte[]> list = new ArrayList<byte[]>();
        String query = SpatialiteUtilities.buildGeometriesInBoundsQuery(destSrid, false, table, n, s, e, w);
        try {
            Stmt stmt = dbJava.prepare(query);
            try {
                while (stmt.step()) {
                    list.add(stmt.column_bytes(0));
                }
            } finally {
                stmt.close();
            }
            return list;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public byte[] getRasterTile(String query) {
        try {
            Stmt stmt = dbJava.prepare(query);
            try {
                if (stmt.step()) {
                    return stmt.column_bytes(0);
                }
            } finally {
                stmt.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Get the {@link GeometryIterator} of a table in a given bound.
     *
     * @param destSrid the srid to which to transform to.
     * @param table    the table to use.
     * @param n        north bound.
     * @param s        south bound.
     * @param e        east bound.
     * @param w        west bound.
     * @return the geometries iterator.
     */
    public GeometryIterator getGeometryIteratorInBounds(String destSrid, SpatialVectorTable table, double n, double s, double e,
                                                        double w) {
        String query = SpatialiteUtilities.buildGeometriesInBoundsQuery(destSrid, false, table, n, s, e, w);
        // GPLog.androidLog(-1,"GeopaparazziOverlay.getGeometryIteratorInBounds query["+query+"]");
        return new GeometryIterator(dbJava, query);
    }

    public void close() throws Exception {
        if (dbJava != null) {
            dbJava.close();
        }
    }

    /**
     * Performs an intersection query on a vector table and returns a string info version of the result.
     *
     * @param boundsSrid          the srid of the bounds supplied.
     * @param spatialTable        the vector table to query.
     * @param n                   north bound.
     * @param s                   south bound.
     * @param e                   east bound.
     * @param w                   west bound.
     * @param resultStringBuilder the builder of the result.
     * @param indentStr           the indenting to use for formatting.
     * @throws Exception if something goes wrong.
     */
    public void intersectionToStringBBOX(String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e,
                                         double w, StringBuilder resultStringBuilder, String indentStr) throws Exception {
        String query = getIntersectionQueryBBOX(boundsSrid, spatialTable, n, s, e, w);
        Stmt stmt = dbJava.prepare(query);
        try {
            while (stmt.step()) {
                int column_count = stmt.column_count();
                for (int i = 0; i < column_count; i++) {
                    String cName = stmt.column_name(i);
                    String value = stmt.column_string(i);
                    resultStringBuilder.append(indentStr).append(cName).append(": ").append(value).append("\n");
                }
                resultStringBuilder.append("\n");
            }
        } finally {
            stmt.close();
        }
    }

    /**
     * Get the query to run for a bounding box intersection.
     * <p/>
     * <p>This assures that the first element of the query is
     * the id field for the record as defined in {@link SpatialiteUtilities#SPATIALTABLE_ID_FIELD}.
     *
     * @param boundsSrid   the srid of the bounds requested.
     * @param spatialTable the {@link SpatialVectorTable} to query.
     * @param n            north bound.
     * @param s            south bound.
     * @param e            east bound.
     * @param w            west bound.
     * @return the query to run to get all fields.
     */
    public static String getIntersectionQueryBBOX(String boundsSrid, SpatialVectorTable spatialTable, double n, double s,
                                                  double e, double w) {
        boolean doTransform = false;
        String fieldNamesList = SpatialiteUtilities.SPATIALTABLE_ID_FIELD;
        // List of non-blob fields
        for (String field : spatialTable.getTableFieldNamesList()) {
            boolean ignore = SpatialiteUtilities.doIgnoreField(field);
            if (!ignore)
                fieldNamesList += "," + field;
        }
        if (!spatialTable.getSrid().equals(boundsSrid)) {
            doTransform = true;
        }
        StringBuilder sbQ = new StringBuilder();
        sbQ.append("SELECT ");
        sbQ.append(fieldNamesList);
        sbQ.append(" FROM ").append(spatialTable.getTableName());
        sbQ.append(" WHERE ST_Intersects(");
        if (doTransform)
            sbQ.append("ST_Transform(");
        sbQ.append("BuildMBR(");
        sbQ.append(w);
        sbQ.append(",");
        sbQ.append(s);
        sbQ.append(",");
        sbQ.append(e);
        sbQ.append(",");
        sbQ.append(n);
        if (doTransform) {
            sbQ.append(",");
            sbQ.append(boundsSrid);
            sbQ.append("),");
            sbQ.append(spatialTable.getSrid());
        }
        sbQ.append("),");
        sbQ.append(spatialTable.getGeomName());
        sbQ.append(");");

        return sbQ.toString();
    }

    // public void intersectionToString4Polygon( String queryPointSrid, SpatialVectorTable
    // spatialTable, double n, double e,
    // StringBuilder sb, String indentStr ) throws Exception {
    // boolean doTransform = false;
    // if (!spatialTable.getSrid().equals(queryPointSrid)) {
    // doTransform = true;
    // }
    //
    // StringBuilder sbQ = new StringBuilder();
    // sbQ.append("SELECT * FROM ");
    // sbQ.append(spatialTable.getName());
    // sbQ.append(" WHERE ST_Intersects(");
    // sbQ.append(spatialTable.getGeomName());
    // sbQ.append(",");
    // if (doTransform)
    // sbQ.append("ST_Transform(");
    // sbQ.append("MakePoint(");
    // sbQ.append(e);
    // sbQ.append(",");
    // sbQ.append(n);
    // if (doTransform) {
    // sbQ.append(",");
    // sbQ.append(queryPointSrid);
    // sbQ.append("),");
    // sbQ.append(spatialTable.getSrid());
    // }
    // sbQ.append(")) = 1 ");
    // sbQ.append("AND ROWID IN (");
    // sbQ.append("SELECT ROWID FROM Spatialindex WHERE f_table_name ='");
    // sbQ.append(spatialTable.getName());
    // sbQ.append("'");
    // // if a table has more than 1 geometry, the column-name MUST be given, otherwise no results.
    // sbQ.append(" AND f_geometry_column = '");
    // sbQ.append(spatialTable.getGeomName());
    // sbQ.append("'");
    // sbQ.append(" AND search_frame = ");
    // if (doTransform)
    // sbQ.append("ST_Transform(");
    // sbQ.append("MakePoint(");
    // sbQ.append(e);
    // sbQ.append(",");
    // sbQ.append(n);
    // if (doTransform) {
    // sbQ.append(",");
    // sbQ.append(queryPointSrid);
    // sbQ.append("),");
    // sbQ.append(spatialTable.getSrid());
    // }
    // sbQ.append("));");
    // String query = sbQ.toString();
    //
    // Stmt stmt = db_java.prepare(query);
    // try {
    // while( stmt.step() ) {
    // int column_count = stmt.column_count();
    // for( int i = 0; i < column_count; i++ ) {
    // String cName = stmt.column_name(i);
    // if (cName.equalsIgnoreCase(spatialTable.getGeomName())) {
    // continue;
    // }
    //
    // String value = stmt.column_string(i);
    // sb.append(indentStr).append(cName).append(": ").append(value).append("\n");
    // }
    // sb.append("\n");
    // }
    // } finally {
    // stmt.close();
    // }
    // }

    /**
     * Load list of Table [Vector/Raster] for GeoPackage Files [gpkg]
     * <p/>
     * <b>THIS METHOD IS VERY EXPERIMENTAL AND A WORK IN PROGRESS</b>
     * - rasterTableList or vectorTableList will be created if == null
     * <br>- name of Field
     * <br> - type of field as defined in Database
     * <br>- OGC 12-128r9 from 2013-11-19
     * <br>-- older versions will not be supported
     * <br>- With SQLite versions 3.7.17 and later : 'PRAGMA application_id' [1196437808]
     * <br>-- older (for us invalid) SPL_Geopackage Files return 0
     */
    private void collectGpkgTables() throws Exception {
        String vector_key = ""; // term used when building the sql, used as map.key
        String vector_value = ""; // to retrieve map.value (=vector_data+vector_extent)
        for (Map.Entry<String, String> vector_entry : spatialVectorMap.entrySet()) {
            // berlin_stadtteile
            vector_key = vector_entry.getKey();
            // soldner_polygon;14;3;2;3068;1;20847.6171111586,18733.613614603,20847.6171111586,18733.613614603
            vector_value = vector_entry.getValue();
            double[] boundsCoordinates = new double[]{0.0, 0.0, 0.0, 0.0};
            double[] centerCoordinate = new double[]{0.0, 0.0};
            HashMap<String, String> fields_list = new HashMap<String, String>();
            int i_geometry_type = 0;
            int i_view_read_only = 0;
            double horz_resolution = 0.0;
            String s_view_read_only = "";
            String[] sa_string = vector_key.split(";");
            // fromosm_tiles;tile_data;GeoPackage_tiles;© OpenStreetMap contributors, See
            // http://www.openstreetmap.org/copyright;OSM Tiles;
            // geonames;geometry;GeoPackage_features;Data from http://www.geonames.org/, under
            // Creative Commons Attribution 3.0 License;Geonames;
            if (sa_string.length == 5) {
                String table_name = sa_string[0]; // fromosm_tiles / geonames
                String geometry_column = sa_string[1]; // tile_data / geometry
                String layerType = sa_string[2]; // GeoPackage_tiles / GeoPackage_features
                String s_identifier = sa_string[3]; // short description
                String s_description = sa_string[4]; // long description
                sa_string = vector_value.split(";");
                // RGB;512;3068;1890 -
                // 1:17777;3;17903.0354299312,17211.5335278146,29889.8601630003,26582.2086184726;2014-05-09T09:18:07.230Z
                if (sa_string.length == 7) {
                    // 0;10;3857;0;
                    // 1;2;4326;0;
                    String s_geometry_type = sa_string[0]; // 1= POINT / OR min_zoom
                    String s_coord_dimension = sa_string[1]; // 2= XY / OR max_zoom
                    String s_srid = sa_string[2]; // 4326
                    String s_spatial_index_enabled = sa_string[3]; // 0
                    // -1;-75.5;18.0;-71.06667;20.08333;2013-12-24T16:32:14.000000Z
                    String s_row_count = sa_string[4]; // 0 = not possible as sub-query - but also
                    // not needed
                    String s_bounds = sa_string[5]; // -75.5;18.0;-71.06667;20.08333
                    String s_last_verified = sa_string[6]; // 2013-12-24T16:32:14.000000Z
                    sa_string = s_bounds.split(",");
                    if (sa_string.length == 4) {
                        try {
                            boundsCoordinates[0] = Double.parseDouble(sa_string[0]);
                            boundsCoordinates[1] = Double.parseDouble(sa_string[1]);
                            boundsCoordinates[2] = Double.parseDouble(sa_string[2]);
                            boundsCoordinates[3] = Double.parseDouble(sa_string[3]);
                        } catch (NumberFormatException e) {
                        }
                        if (!s_srid.equals("4326")) { // Transform into wsg84 if needed
                            SpatialiteUtilities.collectBoundsAndCenter(dbJava, s_srid, centerCoordinate, boundsCoordinates);
                        } else {
                            centerCoordinate[0] = boundsCoordinates[0] + (boundsCoordinates[2] - boundsCoordinates[0]) / 2;
                            centerCoordinate[1] = boundsCoordinates[1] + (boundsCoordinates[3] - boundsCoordinates[1]) / 2;
                        }
                        checkAndAdaptDatabaseBounds(boundsCoordinates, null);
                        if (vector_key.contains("GeoPackage_tiles")) {
                            int i_min_zoom = Integer.parseInt(s_geometry_type);
                            int i_max_zoom = Integer.parseInt(s_coord_dimension);
                            SpatialRasterTable table = new SpatialRasterTable(getDatabasePath(), "", s_srid, i_min_zoom,
                                    i_max_zoom, centerCoordinate[0], centerCoordinate[1], null, boundsCoordinates);
                            table.setMapType(layerType);
                            // table.setTableName(s_table_name);
                            table.setColumnName(geometry_column);
                            // setDescription(s_table_name);
                            // table.setDescription(this.databaseDescription);
                            if (rasterTableList == null)
                                rasterTableList = new ArrayList<SpatialRasterTable>();
                            rasterTableList.add(table);
                        } else {
                            if (vector_key.contains("GeoPackage_features")) {
                                // String table_name=sa_string[0]; // lakemead_clipped
                                // String geometry_column=sa_string[1]; // shape
                                i_view_read_only = 0; // always
                                i_geometry_type = Integer.parseInt(s_geometry_type);
                                GeometryType geometry_type = GeometryType.forValue(i_geometry_type);
                                s_geometry_type = geometry_type.toString();
                                int i_spatial_index_enabled = Integer.parseInt(s_spatial_index_enabled); // 0=no
                                // spatialiIndex
                                // for
                                // GeoPackage
                                // Files
                                int i_row_count = Integer.parseInt(s_row_count); // will always be 0
                                // no Zoom levels with
                                // vector data
                                if (i_spatial_index_enabled == 1) {
                                    SpatialVectorTable table = new SpatialVectorTable(getDatabasePath(), table_name,
                                            geometry_column, i_geometry_type, s_srid, centerCoordinate, boundsCoordinates,
                                            layerType);
                                    // compleate list of fields of
                                    // this table
                                    fields_list = DaoSpatialite.collectTableFields(dbJava, table_name);
                                    table.setFieldsList(fields_list, "ROWID", i_view_read_only);
                                    if (vectorTableList == null)
                                        vectorTableList = new ArrayList<SpatialVectorTable>();
                                    vectorTableList.add(table);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Load list of Table [Vector] for Spatialite4+ Files
     * - for Spaltialite4+ all needed information has been collected in DaoSpatialite.checkDatabaseTypeAndValidity()
     * - rasterTableList or vectorTableList will be created if == null
     * <br>- name of Field
     * <br>- type of field as defined in Database
     */
    private void collectVectorTables() throws Exception {
        String vector_key = ""; // term used when building the sql, used as map.key
        String vector_value = ""; // to retrieve map.value (=vector_data+vector_extent)
        for (Map.Entry<String, String> vector_entry : spatialVectorMap.entrySet()) {
            // berlin_stadtteile
            vector_key = vector_entry.getKey();
            // soldner_polygon;14;3;2;3068;1;20847.6171111586,18733.613614603,20847.6171111586,18733.613614603
            vector_value = vector_entry.getValue();
            double[] boundsCoordinates = new double[]{0.0, 0.0, 0.0, 0.0};
            double[] centerCoordinate = new double[]{0.0, 0.0};
            HashMap<String, String> fields_list = new HashMap<String, String>();
            int i_geometry_type = 0;
            int i_view_read_only = 0;
            String s_view_read_only = "";
            String[] sa_string = vector_key.split(";");
            // berlin_postgrenzen.1890;LOSSY_WEBP;RasterLite2;Berlin Straube Postgrenzen;1890 -
            // 1:17777;
            if (sa_string.length == 5) {
                String table_name = sa_string[0];
                String geometry_column = sa_string[1];
                String layerType = sa_string[2];
                String s_ROWID_PK = sa_string[3];
                s_view_read_only = sa_string[4];
                sa_string = vector_value.split(";");
                // RGB;512;3068;1.13008623862252;3;17903.0354299312,17211.5335278146,29889.8601630003,26582.2086184726;2014-05-09T09:18:07.230Z
                if (sa_string.length == 7) {
                    String s_geometry_type = sa_string[0];
                    String s_coord_dimension = sa_string[1];
                    String s_srid = sa_string[2];
                    String s_spatial_index_enabled = sa_string[3];
                    String s_row_count_enabled = sa_string[4];
                    String s_bounds = sa_string[5];
                    String s_last_verified = sa_string[6];
                    sa_string = s_bounds.split(",");
                    if (sa_string.length == 4) {
                        try {
                            boundsCoordinates[0] = Double.parseDouble(sa_string[0]);
                            boundsCoordinates[1] = Double.parseDouble(sa_string[1]);
                            boundsCoordinates[2] = Double.parseDouble(sa_string[2]);
                            boundsCoordinates[3] = Double.parseDouble(sa_string[3]);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                        if (!s_srid.equals("4326")) { // Transform into wsg84 if needed
                            SpatialiteUtilities.collectBoundsAndCenter(dbJava, s_srid, centerCoordinate, boundsCoordinates);
                        } else {
                            centerCoordinate[0] = boundsCoordinates[0] + (boundsCoordinates[2] - boundsCoordinates[0]) / 2;
                            centerCoordinate[1] = boundsCoordinates[1] + (boundsCoordinates[3] - boundsCoordinates[1]) / 2;
                        }
                        checkAndAdaptDatabaseBounds(boundsCoordinates, null);
                        if (layerType.equals("RasterLite2")) {
                            // s_ROWID_PK == title [Berlin Straube Postgrenzen] - needed
                            // s_view_read_only == abstract [1890 - 1:17777] - needed
                            // s_geometry_type == pixel_type [RGB] - not needed
                            // s_coord_dimension == tile_width - maybe usefull
                            // geometry_column == compression [LOSSY_WEBP] - not needed
                            // s_row_count_enabled == num_bands [3] - not needed
                            //                            int i_tile_width = Integer.parseInt(s_coord_dimension);
                            //                            double horz_resolution = Double.parseDouble(s_spatial_index_enabled);
                            //                            int i_num_bands = Integer.parseInt(s_row_count_enabled);
                            // TODO in next version add RasterTable
                            // berlin_postgrenzen.1890
                            SpatialRasterTable table = new SpatialRasterTable(getDatabasePath(), table_name, s_srid, 0, 22,
                                    centerCoordinate[0], centerCoordinate[1], null, boundsCoordinates);
                            table.setMapType(layerType);
                            table.setTitle(s_ROWID_PK);
                            table.setDescription(s_view_read_only);
                            // prevent a possible double loading
                            if (rasterTableList == null)
                                rasterTableList = new ArrayList<SpatialRasterTable>();
                            rasterTableList.add(table);
                        }
                        if ((layerType.equals(TableTypes.SPATIALTABLE.getDescription())) || (layerType.equals(TableTypes.SPATIALVIEW.getDescription()))) {
                            i_view_read_only = Integer.parseInt(s_view_read_only);
                            i_geometry_type = Integer.parseInt(s_geometry_type);
                            GeometryType geometry_type = GeometryType.forValue(i_geometry_type);
                            s_geometry_type = geometry_type.toString();
                            int i_spatial_index_enabled = Integer.parseInt(s_spatial_index_enabled); // should
                            // always
                            // be
                            // 1
                            int i_row_count = Integer.parseInt(s_row_count_enabled);
                            // no Zoom levels with
                            // vector data
                            if (i_spatial_index_enabled == 1) {
                                SpatialVectorTable table = new SpatialVectorTable(getDatabasePath(), table_name, geometry_column,
                                        i_geometry_type, s_srid, centerCoordinate, boundsCoordinates, layerType);
                                // compleate list of fields of
                                // this table
                                fields_list = DaoSpatialite.collectTableFields(dbJava, table_name);
                                table.setFieldsList(fields_list, s_ROWID_PK, i_view_read_only);
                                if (vectorTableList == null)
                                    vectorTableList = new ArrayList<SpatialVectorTable>();
                                vectorTableList.add(table);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks (and adapts) the overall database bounds based on the passed coordinates.
     * <p/>
     * <p>Goal: when painting the Geometries: check of viewport is inside these bounds.
     * <br>- if the Viewport is outside these Bounds: all Tables can be ignored
     * <br>-- this is called when the Tables are created
     *
     * @param boundsCoordinates bounds to check against the overall.
     */
    private void checkAndAdaptDatabaseBounds(double[] boundsCoordinates, int[] zoomLevels) {
        if ((this.boundsWest == 0.0) && (this.boundsSouth == 0.0) && (this.boundsEast == 0.0) && (this.boundsNorth == 0.0)) {
            this.boundsWest = boundsCoordinates[0];
            this.boundsSouth = boundsCoordinates[1];
            this.boundsEast = boundsCoordinates[2];
            this.boundsNorth = boundsCoordinates[2];
        } else {
            if (boundsCoordinates[0] < this.boundsWest)
                this.boundsWest = boundsCoordinates[0];
            if (boundsCoordinates[1] < this.boundsSouth)
                this.boundsSouth = boundsCoordinates[1];
            if (boundsCoordinates[2] > this.boundsEast)
                this.boundsEast = boundsCoordinates[2];
            if (boundsCoordinates[3] < this.boundsNorth)
                this.boundsNorth = boundsCoordinates[3];
        }
        centerX = this.boundsWest + (this.boundsEast - this.boundsWest) / 2;
        centerY = this.boundsSouth + (this.boundsNorth - this.boundsSouth) / 2;
        if ((zoomLevels != null) && (zoomLevels.length == 2)) {
            if ((this.minZoom == 0) && (this.maxZoom == 0)) {
                this.minZoom = zoomLevels[0];
                this.maxZoom = zoomLevels[1];
            } else {
                if (zoomLevels[0] < this.minZoom)
                    this.minZoom = zoomLevels[0];
                if (zoomLevels[1] > this.maxZoom)
                    this.maxZoom = zoomLevels[1];
            }
        }
    }

    /**
     * Collects tables.
     * <p/>
     * <p>The {@link HashMap} will contain:
     * <ul>
     * <li>name of Field
     * <li>type of field as defined in Database
     * </ul>
     */
    private void checkAndCollectTables() throws Exception {
        switch (databaseType) {
            case GEOPACKAGE: {
                // GeoPackage Files [gpkg]
                collectGpkgTables();
            }
            break;
            case SPATIALITE3:
            case SPATIALITE4: {
                // Spatialite Files version 2.4 ; 3 and 4
                collectVectorTables();
            }
            break;
            default:
                break;
        }
        if (isValid()) {
            if (vectorTableList != null) {
                // now read styles
                checkPropertiesTable();
                // assign the styles
                for (SpatialVectorTable spatialTable : vectorTableList) {
                    Style style4Table = null;
                    try {
                        style4Table = getStyle4Table(dbJava, spatialTable.getUniqueNameBasedOnDbFilePath(),
                                spatialTable.getLabelField());
                    } catch (java.lang.Exception e) {
                        deleteStyleTable(dbJava);
                        checkPropertiesTable();
                    }
                    if (style4Table == null) {
                        spatialTable.makeDefaultStyle();
                    } else {
                        spatialTable.setStyle(style4Table);
                    }
                }
                OrderComparator orderComparator = new OrderComparator();
                Collections.sort(vectorTableList, orderComparator);
            }
        }
        return;
    }

    /**
     * Update a style definiton in the db.
     *
     * @param style the {@link Style} to update.
     * @throws Exception if something goes wrong.
     */
    public void updateStyle(Style style) throws Exception {
        GeopaparazziDatabaseProperties.updateStyle(dbJava, style);
    }

    /**
     * Delete and recreate a default properties table for this database.
     *
     * @throws Exception if something goes wrong.
     */
    public void resetStyleTable() throws Exception {
        deleteStyleTable(dbJava);
        createPropertiesTable(dbJava);
        for (SpatialVectorTable spatialTable : vectorTableList) {
            createDefaultPropertiesForTable(dbJava, spatialTable.getUniqueNameBasedOnDbFilePath(),
                    spatialTable.getLabelField());
        }
    }

    /**
     * Getter for the spatialite db reference.
     *
     * @return the spatialite database reference.
     */
    public Database getDatabase() {
        return dbJava;
    }

}
