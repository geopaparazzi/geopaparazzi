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
package eu.geopaparazzi.spatialite.database.spatial.core;

import static eu.geopaparazzi.spatialite.util.DaoSpatialite.METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME;
import static eu.geopaparazzi.spatialite.util.DaoSpatialite.METADATA_VECTOR_LAYERS_TABLE_NAME;
import static eu.geopaparazzi.spatialite.util.DaoSpatialite.PROPERTIESTABLE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import android.content.Context;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ColorUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteContextHolder;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryIterator;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryType;
import eu.geopaparazzi.spatialite.util.DaoSpatialite;
import eu.geopaparazzi.spatialite.util.OrderComparator;
import eu.geopaparazzi.spatialite.util.SpatialiteDatabaseType;
import eu.geopaparazzi.spatialite.util.SpatialiteUtilities;
import eu.geopaparazzi.spatialite.util.Style;

/**
* An utility class to handle the spatial database.
*
* @author Andrea Antonello (www.hydrologis.com)
*/
@SuppressWarnings("nls")
public class SpatialiteDatabaseHandler extends SpatialDatabaseHandler {

    private String uniqueDbName4DataProperties = "";

    private Database db_java;

    private HashMap<String, Paint> fillPaints = new HashMap<String, Paint>();
    private HashMap<String, Paint> strokePaints = new HashMap<String, Paint>();

    private List<SpatialVectorTable> vectorTableList;
    private List<SpatialRasterTable> rasterTableList;

    private SpatialiteDatabaseType databaseType = null;

    // List of all SpatialView of Database [view_name,view_data] - parse for 'geometry_column;min_x,min_y,max_x,max_y'
    private HashMap<String, String> spatialVectorMap = new HashMap<String, String>();
    // List of all SpatialView of Database [view_name,view_data] - that have errors
    private HashMap<String, String> spatialVectorMapErrors = new HashMap<String, String>();

    /**
* Constructor.
*
* @param dbPath the path to the database this handler connects to.
* @throws IOException if something goes wrong.
*/
    public SpatialiteDatabaseHandler( String dbPath ) throws IOException {
        super(dbPath);
        try {
            try {
                Context context = SpatialiteContextHolder.INSTANCE.getContext();
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
            db_java = new jsqlite.Database();
            db_java.open(databasePath, jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);

            checkAndUpdatePropertiesUniqueNames();

            // check database and collect the views list
            try {
                databaseType = DaoSpatialite.checkDatabaseTypeAndValidity(db_java, spatialVectorMap, spatialVectorMapErrors);
                isDatabaseValid = true;
                // GPLog.androidLog(-1,"GeopaparazziOverlay.getGeometryIteratorInBounds version["+DaoSpatialite.getJavaSqliteDescription(db_java,"test")+"]");
            } catch (Exception e) {
                isDatabaseValid = false;
            }

            if (!isValid()) {
                close();
            }
        } catch (Exception e) {
            GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + databaseFile.getAbsolutePath() + "]", e);
        }
    }

    @Override
    public void open() {
        /*
* TODO @mj10777 shouldn't the db be opened here instead of the constructor?
*/
    }

    /**
* Is the database file considered valid?
*
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
    public List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception {
        if (vectorTableList == null || forceRead) {
            vectorTableList = new ArrayList<SpatialVectorTable>();
            checkAndCollectTables();
        }
        return vectorTableList;
    }

    /**
* Collects bounds and center as wgs84 4326.
*
* @param srid the source srid.
* @param centerCoordinate the coordinate array to fill with the center.
* @param boundsCoordinates the coordinate array to fill with the bounds as [w,s,e,n].
*/
    private void collectBoundsAndCenter( String srid, double[] centerCoordinate, double[] boundsCoordinates ) {
        String centerQuery = "";
        try {
            Stmt centerStmt = null;
            double bounds_west = boundsCoordinates[0];
            double bounds_south = boundsCoordinates[1];
            double bounds_east = boundsCoordinates[2];
            double bounds_north = boundsCoordinates[3];
            // srid=3068
            // 3460.411441 1208.430179 49230.152810 38747.958906
            // SELECT
            // CastToXY(ST_Transform(MakePoint((3460.411441+(49230.152810-3460.411441)/2),(1208.430179+(38747.958906-1208.430179)/2),3068),4326))
            // AS Center
            // SELECT CastToXY(ST_Transform(MakePoint(3460.411441,1208.430179,3068),4326)) AS
            // South_West
            // SELECT CastToXY(ST_Transform(MakePoint(49230.152810,38747.958906,3068),4326)) AS
            // North_East
            try {
                WKBReader wkbReader = new WKBReader();
                StringBuilder centerBuilder = new StringBuilder();
                centerBuilder.append("SELECT ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                // centerBuilder.append("select AsText(ST_Transform(MakePoint(");
                centerBuilder.append("(" + bounds_west + " + (" + bounds_east + " - " + bounds_west + ")/2), ");
                centerBuilder.append("(" + bounds_south + " + (" + bounds_north + " - " + bounds_south + ")/2), ");
                centerBuilder.append(srid);
                centerBuilder.append("),4326))) AS Center,");
                centerBuilder.append("ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                centerBuilder.append("" + bounds_west + "," + bounds_south + ", ");
                centerBuilder.append(srid);
                centerBuilder.append("),4326))) AS South_West,");
                centerBuilder.append("ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                centerBuilder.append("" + bounds_east + "," + bounds_north + ", ");
                centerBuilder.append(srid);
                centerBuilder.append("),4326))) AS North_East ");
                // centerBuilder.append("';");
                centerQuery = centerBuilder.toString();
                centerStmt = db_java.prepare(centerQuery);
                if (centerStmt.step()) {
                    byte[] geomBytes = centerStmt.column_bytes(0);
                    Geometry geometry = wkbReader.read(geomBytes);
                    Coordinate coordinate = geometry.getCoordinate();
                    centerCoordinate[0] = coordinate.x;
                    centerCoordinate[1] = coordinate.y;
                    geomBytes = centerStmt.column_bytes(1);
                    geometry = wkbReader.read(geomBytes);
                    coordinate = geometry.getCoordinate();
                    // South_West
                    boundsCoordinates[0] = coordinate.x;
                    boundsCoordinates[1] = coordinate.y;
                    geomBytes = centerStmt.column_bytes(2);
                    geometry = wkbReader.read(geomBytes);
                    coordinate = geometry.getCoordinate();
                    // North_East
                    boundsCoordinates[2] = coordinate.x;
                    boundsCoordinates[3] = coordinate.y;
                }
            } catch (java.lang.Exception e) {
                GPLog.androidLog(4, "SpatialiteDatabaseHandler.collectBoundsAndCenter Bounds[" + centerQuery + "]", e);
            } finally {
                if (centerStmt != null)
                    centerStmt.close();
            }
        } catch (java.lang.Exception e) {
            GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + databasePath + "] sql[" + centerQuery + "]", e);
        }
    }

    @Override
    public List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception {
        if (rasterTableList == null || forceRead) {
            rasterTableList = new ArrayList<SpatialRasterTable>();
            checkAndCollectTables();
        }
        return rasterTableList;
    }

    /**
* Checks if the table names in the properties table are defined properly.
*
* <p>The unique table name is a concatenation of:<br>
* <b>dbPath#tablename#geometrytype</b>
* <p>If the name doesn't start with the database path, it needs to
* be updated. The rest is anyways unique inside the database.
*
* @throws Exception if something went wrong.
*/
    private void checkAndUpdatePropertiesUniqueNames() throws Exception {
        List<Style> allStyles = DaoSpatialite.getAllStyles(db_java);
        if (allStyles == null) {
            /*
* something went wrong in the reading of the table,
* which might be due to an upgrade of table structure.
* Remove and recreate the table.
*/
            DaoSpatialite.deleteStyleTable(db_java);
            DaoSpatialite.createPropertiesTable(db_java);
        } else {
            for( Style style : allStyles ) {
                if (!style.name.startsWith(uniqueDbName4DataProperties + SpatialiteUtilities.UNIQUENAME_SEPARATOR)) {
                    // need to update the name in the style and also in the database
                    String[] split = style.name.split(SpatialiteUtilities.UNIQUENAME_SEPARATOR);
                    if (split.length == 3) {
                        String newName = uniqueDbName4DataProperties + SpatialiteUtilities.UNIQUENAME_SEPARATOR + split[1]
                                + SpatialiteUtilities.UNIQUENAME_SEPARATOR + split[2];
                        style.name = newName;
                        DaoSpatialite.updateStyleName(db_java, newName, style.id);
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
        int propertiesTableColumnCount = DaoSpatialite.checkTableExistence(db_java, PROPERTIESTABLE);
        if (propertiesTableColumnCount == 0) {
            DaoSpatialite.createPropertiesTable(db_java);
            for( SpatialVectorTable spatialTable : vectorTableList ) {
                DaoSpatialite.createDefaultPropertiesForTable(db_java, spatialTable.getUniqueNameBasedOnDbFilePath());
            }
        }
    }

    public float[] getTableBounds( SpatialTable spatialTable ) throws Exception {
        return spatialTable.getTableBounds();
    }

    /**
* Get the fill {@link Paint} for a given style.
*
* <p>Paints are cached and reused.</p>
*
* @param style the {@link Style} to use.
* @return the paint.
*/
    public Paint getFillPaint4Style( Style style ) {
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
*
* <p>Paints are cached and reused.</p>
*
* @param style the {@link Style} to use.
* @return the paint.
*/
    public Paint getStrokePaint4Style( Style style ) {
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
                for( int i = 0; i < split.length; i++ ) {
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
* @param table the vector table.
* @param n north bound.
* @param s south bound.
* @param e east bound.
* @param w west bound.
* @return list of WKB geometries.
*/
    public List<byte[]> getWKBFromTableInBounds( String destSrid, SpatialVectorTable table, double n, double s, double e, double w ) {
        List<byte[]> list = new ArrayList<byte[]>();
        String query = SpatialiteUtilities.buildGeometriesInBoundsQuery(destSrid, table, n, s, e, w);
        try {
            Stmt stmt = db_java.prepare(query);
            try {
                while( stmt.step() ) {
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
    public byte[] getRasterTile( String query ) {
        try {
            Stmt stmt = db_java.prepare(query);
            try {
                if (stmt.step()) {
                    byte[] bytes = stmt.column_bytes(0);
                    return bytes;
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
* @param table the table to use.
* @param n north bound.
* @param s south bound.
* @param e east bound.
* @param w west bound.
* @return the geometries iterator.
*/
    public GeometryIterator getGeometryIteratorInBounds( String destSrid, SpatialVectorTable table, double n, double s, double e,
            double w ) {
        String query = SpatialiteUtilities.buildGeometriesInBoundsQuery(destSrid, table, n, s, e, w);
        // GPLog.androidLog(-1,"GeopaparazziOverlay.getGeometryIteratorInBounds query["+query+"]");

        return new GeometryIterator(db_java, query);
    }

    public void close() throws Exception {
        if (db_java != null) {
            db_java.close();
        }
    }

    /**
* Performs an intersection query on a vector table and returns a string info version of the result.
*
* @param boundsSrid the srid of the bounds supplied.
* @param spatialTable the vector table to query.
* @param n north bound.
* @param s south bound.
* @param e east bound.
* @param w west bound.
* @param resultStringBuilder the builder of the result.
* @param indentStr the indenting to use for formatting.
* @throws Exception if something goes wrong.
*/
    public void intersectionToStringBBOX( String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e,
            double w, StringBuilder resultStringBuilder, String indentStr ) throws Exception {
        boolean doTransform = false;
        if (!spatialTable.getSrid().equals(boundsSrid)) {
            doTransform = true;
        }
        String query = null;
        {
            StringBuilder sbQ = new StringBuilder();
            sbQ.append("SELECT ");
            sbQ.append("*");
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

            query = sbQ.toString();
        }

        Stmt stmt = db_java.prepare(query);
        try {
            while( stmt.step() ) {
                int column_count = stmt.column_count();
                for( int i = 0; i < column_count; i++ ) {
                    String cName = stmt.column_name(i);
                    if (cName.equalsIgnoreCase(spatialTable.getGeomName())) {
                        continue;
                    }

                    String value = stmt.column_string(i);
                    resultStringBuilder.append(indentStr).append(cName).append(": ").append(value).append("\n");
                }
                resultStringBuilder.append("\n");
            }
        } finally {
            stmt.close();
        }
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
*
* <b>THIS METHOD IS VERY EXPERIMENTAL AND A WORK IN PROGRESS</b>
*
* <br>- name of Field
* <br> - type of field as defined in Database
* <br>- OGC 12-128r9 from 2013-11-19
* <br>-- older versions will not be supported
* <br>- With SQLite versions 3.7.17 and later : 'PRAGMA application_id' [1196437808]
* <br>-- older (for us invalid) Geopackage Files return 0
*
* @return the {@link HashMap} of field name to its type.
*/
    private HashMap<String, String> collectGpkgTables() throws Exception {
        Stmt this_stmt = null;
        HashMap<String, String> fieldName2TypeMap = new HashMap<String, String>();
        String s_srid = "";
        String s_gpkg = "gpkg"; // SELECT data_type,table_name,srs_id FROM gpkg_contents
        int i_srid = 0;
        String s_table_name = "";
        String s_tiles_field_name = "";
        String s_data_type = "";
        String s_sql_layers = "";
        int[] zoomLevels = {0, 22};
        switch( databaseType ) {
        case GEOPACKAGE: { // GeoPackage Files [gpkg]
            StringBuilder sb_layers = new StringBuilder();
            s_sql_layers = "SELECT data_type,table_name,srs_id FROM " + s_gpkg + "_contents";
            // 20140101.world_Haiti.gpkg
            // Luciad_GeoPackage.gpkg: Assume that 1=4326 ; 2=3857
            // [features] [lakemead_clipped] [1]
            // [tiles] [o18229_tif_tiles] [2]
            // [featuresWithRasters] [observations] [2]
            // this is a list of jpeg-images and points - the points have wsg84 values but are set
            // as 2
            // -- the srid for tiles can also be retrieved from raster_columns.srid [also 2]
            // Sample_Geopackage_Haiti.gpkg:
            // [tiles] [fromosm_tiles] [3857]
            // [features] [geonames] [4326]
            // 'features' == vector ; 'tiles' = raster
            // SELECT table_name,srs_id FROM gpkg_contents WHERE data_type = 'features';
            try {
                this_stmt = db_java.prepare(s_sql_layers);
                while( this_stmt.step() ) {
                    i_srid = 0;
                    s_data_type = this_stmt.column_string(0);
                    // filter out everything we have no idea how to deal with
                    if ((s_data_type.equals("features")) || (s_data_type.equals("tiles"))) {
                        // 'featuresWithRasters' is being ignored until further notice
                        s_table_name = this_stmt.column_string(1);
                        s_srid = this_stmt.column_string(2);
                        if (!s_srid.equals("")) {
                            i_srid = Integer.parseInt(s_srid);
                            if ((i_srid > 0) && (i_srid < 3)) {
                                if (i_srid == 1)
                                    i_srid = 4326;
                                if (i_srid == 2)
                                    i_srid = 3857;
                            }
                            if (i_srid > 3)
                                fieldName2TypeMap.put(s_table_name, i_srid + ";" + s_data_type);
                        }
                    }
                }
            } catch (java.lang.Exception e) {
                // invalid gpkg file when gpkg_contents does not exist
                isDatabaseValid = false;
                return fieldName2TypeMap;
            } finally {
                this_stmt.close();
            }
            ArrayList<SpatialVectorTable> vector_TableList = new ArrayList<SpatialVectorTable>();
            ArrayList<SpatialRasterTable> raster_TableList = new ArrayList<SpatialRasterTable>();
            HashMap<String, String> table_list = new HashMap<String, String>();
            fieldName2TypeMap = new HashMap<String, String>();
            for( int i = 0; i < fieldName2TypeMap.size(); i++ ) {
                for( Map.Entry<String, String> table_entry : table_list.entrySet() ) {
                    s_table_name = table_entry.getKey();
                    s_data_type = table_entry.getValue();
                    s_tiles_field_name = "tile_data";
                    String[] sa_split = s_data_type.split(";");
                    if (sa_split.length == 2) {
                        s_srid = sa_split[0];
                        i_srid = Integer.parseInt(s_srid);
                        s_data_type = sa_split[1];
                    }
                    // for 'tiles' the zoom levels
                    if ((!s_table_name.equals("")) && (s_data_type.equals("tiles"))) {
                        sb_layers.append("SELECT min(");
                        sb_layers.append("zoom_level");
                        sb_layers.append("),max(");
                        sb_layers.append("zoom_level");
                        sb_layers.append(") FROM ");
                        sb_layers.append(s_gpkg + "_tile_matrix");
                        sb_layers.append(" WHERE ");
                        sb_layers.append("table_name");
                        sb_layers.append("='");
                        sb_layers.append(s_table_name);
                        sb_layers.append("';");
                        s_sql_layers = sb_layers.toString();
                        sb_layers = new StringBuilder();
                        this_stmt = db_java.prepare(s_sql_layers);
                        try {
                            if (this_stmt.step()) {
                                zoomLevels[0] = this_stmt.column_int(0);
                                zoomLevels[1] = this_stmt.column_int(1);
                            }
                        } catch (java.lang.Exception e) {
                            GPLog.androidLog(4, "SpatialiteDatabaseHandler.get_tables_gpkg [tiles - min/max zoom] prepair["
                                    + s_sql_layers + "]", e);
                        } finally {
                            if (this_stmt != null) {
                                this_stmt.close();
                            }
                        }
                    }
                    // for 'features' and 'tiles' the bounds
                    if (!s_table_name.equals("")) {
                        if (!s_srid.equals("4326")) {
                            // [Sample_Geopackage_Haiti.gpkg, but was 4326
                            // and does not need to be transformed]
                            sb_layers.append("SELECT ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                            sb_layers.append("(min_x + (max_x-min_x)/2), ");
                            sb_layers.append("(min_y + (max_y-min_y)/2), ");
                            sb_layers.append("srs_id");
                            sb_layers.append("),4326))) AS Center,");
                            sb_layers.append("ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                            sb_layers.append("min_x,min_y, ");
                            sb_layers.append("srs_id");
                            sb_layers.append("),4326))) AS South_West,");
                            sb_layers.append("ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                            sb_layers.append("max_x,max_y, ");
                            sb_layers.append("srs_id");
                            sb_layers.append("),4326))) AS North_East FROM ");
                            sb_layers.append(s_gpkg + "_contents");
                            sb_layers.append(" WHERE ");
                            sb_layers.append("table_name");
                            sb_layers.append("='");
                            sb_layers.append(s_table_name);
                            // sb_layers.append(METADATA_GEOPACKAGECONTENT_DATA_TYPE);
                            // sb_layers.append("='");
                            // sb_layers.append(METADATA_GEOPACKAGECONTENT_DATA_TYPE_FEATURES);
                            sb_layers.append("';");
                        } else {
                            sb_layers.append("SELECT ST_AsBinary(CastToXY(MakePoint(");
                            sb_layers.append("(min_x + (max_x-min_x)/2), ");
                            sb_layers.append("(min_y + (max_y-min_y)/2),");
                            sb_layers.append("4326))) AS Center,");
                            sb_layers.append("ST_AsBinary(CastToXY(MakePoint(");
                            sb_layers.append("min_x,min_y,");
                            sb_layers.append("4326))) AS South_West,");
                            sb_layers.append("ST_AsBinary(CastToXY(MakePoint(");
                            sb_layers.append("max_x,max_y,");
                            sb_layers.append("4326))) AS North_East FROM ");
                            sb_layers.append(s_gpkg + "_contents");
                            sb_layers.append(" WHERE ");
                            sb_layers.append("table_name");
                            sb_layers.append("='");
                            sb_layers.append(s_table_name);
                            sb_layers.append("';");
                        }
                        s_sql_layers = sb_layers.toString();
                        if (!s_sql_layers.equals("")) {
                            isDatabaseValid = true;
                            String geometry_column = "";
                            // GPLog.androidLog(-1,"SpatialiteDatabaseHandler["+getFileNamePath()+"] sql["
                            // + s_sql_layers+ "] valid["+b_database_valid+"] ");
                            try {
                                this_stmt = db_java.prepare(s_sql_layers);
                                while( this_stmt.step() ) {
                                    String s_layer_type = "geometry";
                                    int geometry_type = 0;
                                    double[] centerCoordinate = {0.0, 0.0};
                                    double[] boundsCoordinates = {-180.0f, -85.05113f, 180.0f, 85.05113f};
                                    int i_row_count = 0;
                                    int i_coord_dimension = 0;
                                    int i_spatial_index_enabled = 0;
                                    String s_last_verified = "";
                                    int i_valid = 0;
                                    WKBReader wkbReader = new WKBReader();
                                    byte[] geomBytes = this_stmt.column_bytes(0);
                                    Geometry geometry = wkbReader.read(geomBytes);
                                    Coordinate coordinate = geometry.getCoordinate();
                                    centerCoordinate[0] = coordinate.x;
                                    centerCoordinate[1] = coordinate.y;
                                    geomBytes = this_stmt.column_bytes(1);
                                    geometry = wkbReader.read(geomBytes);
                                    coordinate = geometry.getCoordinate();
                                    boundsCoordinates[0] = coordinate.x;
                                    boundsCoordinates[1] = coordinate.y;
                                    geomBytes = this_stmt.column_bytes(2);
                                    geometry = wkbReader.read(geomBytes);
                                    coordinate = geometry.getCoordinate();
                                    boundsCoordinates[2] = coordinate.x;
                                    boundsCoordinates[3] = coordinate.y;
                                    // Zoom levels with non-vector data
                                    checkAndAdaptDatabaseBounds(boundsCoordinates, zoomLevels);
                                    if (s_data_type.equals("features")) {
                                        // TODO
                                    }
                                    if (s_data_type.equals("tiles")) {
                                        SpatialRasterTable table = new SpatialRasterTable(getDatabasePath(), "", s_srid,
                                                zoomLevels[0], zoomLevels[1], centerCoordinate[0], centerCoordinate[1], null,
                                                boundsCoordinates);
                                        table.setMapType(s_gpkg);
                                        // table.setTableName(s_table_name);
                                        table.setColumnName(s_tiles_field_name);
                                        // setDescription(s_table_name);
                                        // table.setDescription(this.databaseDescription);
                                        raster_TableList.add(table);
                                    }
                                }
                            } catch (java.lang.Exception e) {
                                GPLog.androidLog(4, "SpatialiteDatabaseHandler.get_tables_gpkg [bounds] prepair[" + s_sql_layers
                                        + "]", e);
                            } finally {
                                if (this_stmt != null) {
                                    this_stmt.close();
                                }
                            }
                            if (vector_TableList.size() > 0)
                                this.vectorTableList = vector_TableList;
                            if (raster_TableList.size() > 0)
                                this.rasterTableList = raster_TableList;
                        }
                    }
                }
            }
        }
            break;
        }
        return fieldName2TypeMap;
    }

    /**
* Load list of Table [Vector] for Spatialite4+ Files
* - for Spaltialite4+ all needed information has been collected in DaoSpatialite.checkDatabaseTypeAndValidity()
* <br>- name of Field
* <br>- type of field as defined in Database
*
* @return the {@link HashMap} of field name to its type.
*/
    private HashMap<String, String> collectVectorTables() throws Exception {
        // mj10777 20140315: when a final decision NOT to support normal-views is made
        // - the 'table_fields' logic can be removed
        HashMap<String, String> table_fields = new HashMap<String, String>();
        String vector_key=""; // term used when building the sql, used as map.key
        String vector_value=""; // to retrieve map.value (=vector_data+vector_extent)
        for( Map.Entry<String, String> vector_entry : spatialVectorMap.entrySet() ) {
            // berlin_stadtteile
            vector_key = vector_entry.getKey();
            // soldner_polygon;14;3;2;3068;1;20847.6171111586,18733.613614603,20847.6171111586,18733.613614603
            vector_value = vector_entry.getValue();
            double[] boundsCoordinates = new double[]{0.0, 0.0, 0.0, 0.0};
            double[] centerCoordinate = new double[]{0.0, 0.0};
            HashMap<String, String> fields_list = new HashMap<String, String>();
            int i_geometry_type=0;
            String[] sa_string = vector_key.split(";");
            if (sa_string.length == 5) {
             String table_name=sa_string[0];
             String geometry_column=sa_string[1];
             String s_layer_type=sa_string[2];
             String s_ROWID_PK=sa_string[3];
             int i_view_read_only = Integer.parseInt(sa_string[4]);
             sa_string = vector_value.split(";");
             if (sa_string.length == 7) {
              String s_geometry_type = sa_string[0];
              i_geometry_type = Integer.parseInt(s_geometry_type);
              GeometryType geometry_type = GeometryType.forValue(i_geometry_type);
              s_geometry_type = geometry_type.toString();
              String s_coord_dimension=sa_string[1];
              String s_srid=sa_string[2];
              int i_spatial_index_enabled=Integer.parseInt(sa_string[3]); // should always be 1
              int i_row_count = Integer.parseInt(sa_string[4]);
              String s_bounds = sa_string[5];
              String s_last_verified=sa_string[6];
              sa_string = s_bounds.split(",");
              if (sa_string.length == 4) {
               try {
                boundsCoordinates[0] = Double.parseDouble(sa_string[0]);
                boundsCoordinates[1] = Double.parseDouble(sa_string[1]);
                boundsCoordinates[2] = Double.parseDouble(sa_string[2]);
                boundsCoordinates[3] = Double.parseDouble(sa_string[3]);
               } catch (NumberFormatException e) {
               }
               if (i_spatial_index_enabled != 1)
               {
               }
               if (!s_srid.equals("4326")) { // Transform into wsg84 if needed
                collectBoundsAndCenter(s_srid, centerCoordinate, boundsCoordinates);
               } else {
                centerCoordinate[0] = boundsCoordinates[0] + (boundsCoordinates[2] - boundsCoordinates[0]) / 2;
                centerCoordinate[1] = boundsCoordinates[1] + (boundsCoordinates[3] - boundsCoordinates[1]) / 2;
               }
               checkAndAdaptDatabaseBounds(boundsCoordinates, null);
               // GPLog.androidLog(-1,"SpatialiteDatabaseHandler["+databaseFile.getAbsolutePath()+"] vector_key["+vector_key+"] vector_data[" + vector_data+ "] ");
               // no Zoom levels with
               // vector data
               SpatialVectorTable table = new SpatialVectorTable(getDatabasePath(), table_name, geometry_column,
               i_geometry_type, s_srid, centerCoordinate, boundsCoordinates, s_layer_type);
               // compleate list of fields of
               // this table
               fields_list = DaoSpatialite.collectTableFields(db_java, table_name);
               table.setFieldsList(fields_list,s_ROWID_PK,i_view_read_only);
               vectorTableList.add(table);
             }
           }
        }
      }
      return table_fields;
    }

    /**
* Load list of Table [Vector] for pre-Spatialite+4+ Files
* - for pre-Spaltialite4 we must gather the bounds ourselfs
* <br>- name of Field
* <br>- type of field as defined in Database
*
* @return the {@link HashMap} of field name to its type.
*/
    private HashMap<String, String> collectSpatialiteTables() throws Exception {
        Stmt this_stmt = null;
        List<SpatialVectorTable> vectorTableList;
        HashMap<String, String> table_fields = new HashMap<String, String>();
        StringBuilder sb_layers = new StringBuilder();
        String s_srid = "";
        int i_srid = 0;
        String table_name = "";
        String s_sql_layers = "";
        switch( databaseType ) {
        case SPATIALITE3: { // Spatialite Files version 2+3=3
            sb_layers.append("SELECT ");
            sb_layers.append("f_table_name");
            sb_layers.append(", ");
            sb_layers.append("f_geometry_column");
            sb_layers.append(", ");
            sb_layers.append("type");
            sb_layers.append(",");
            sb_layers.append("srid");
            sb_layers.append(" FROM ");
            sb_layers.append("geometry_columns");
            sb_layers.append(" ORDER BY f_table_name;");
            // version 3 ['type' instead of 'geometry_type']:
            // SELECT f_table_name,f_geometry_column,geometry_type,srid FROM geometry_columns ORDER BY f_table_name
            s_sql_layers = sb_layers.toString();
            break;
        }
        }
        if (!s_sql_layers.equals("")) {
            sb_layers = new StringBuilder();
            isDatabaseValid = true;
            vectorTableList = new ArrayList<SpatialVectorTable>();
            table_fields = new HashMap<String, String>();
            String geometry_column = "";
            boolean b_UpdateLayerStatistics = true;
            try {
                this_stmt = db_java.prepare(s_sql_layers);
                while( this_stmt.step() ) {
                    String s_layer_type = "geometry";
                    int i_geometry_type = 0;
                    String s_geometry_type = "";
                    double[] centerCoordinate = {0.0, 0.0};
                    double[] boundsCoordinates = {-180.0f, -85.05113f, 180.0f, 85.05113f};
                    int i_row_count = 0;
                    HashMap<String, String> fields_list = new HashMap<String, String>();
                    table_name = this_stmt.column_string(0);
                    // GPLog.androidLog(-1,"SpatialiteDatabaseHandler["+getFileNamePath()+"] tablename["+table_name+"]");
                    geometry_column = this_stmt.column_string(1);
                    // for older spatialite v2+3 : Query extent of table
                    // and fill boundsCoordinates
                    s_geometry_type = this_stmt.column_string(2);
                    i_geometry_type = GeometryType.forValue(s_geometry_type);
                    i_srid = this_stmt.column_int(3);
                    s_srid = String.valueOf(i_srid);
                    // 'rows_count;min_x,min_y,max_x,max_y;datetimestamp_now'
                    String s_vector_extent=DaoSpatialite.SpatialiteRetrieveBounds(db_java,table_name,geometry_column);
                    String[] sa_string = s_vector_extent.split(";");
                    if (sa_string.length == 3) {
                     i_row_count = Integer.parseInt(sa_string[0]);
                     String s_bounds=sa_string[1];
                     String s_last_verified=sa_string[2];
                     sa_string = s_bounds.split(",");
                     if (sa_string.length == 4) {
                      try {
                       boundsCoordinates[0] = Double.parseDouble(sa_string[0]);
                       boundsCoordinates[1] = Double.parseDouble(sa_string[1]);
                       boundsCoordinates[2] = Double.parseDouble(sa_string[2]);
                       boundsCoordinates[3] = Double.parseDouble(sa_string[3]);
                      } catch (NumberFormatException e) {
                      }
                     }
                    }
                    // mj10777 20140315: when a final decision NOT to support normal-views is made
                    // - the 'table_fields' logic can be removed
                    // this should have a list of unique geometry-fields, we will look later for
                    // these in the views
                    if (table_fields.get(geometry_column) == null)
                        table_fields.put(geometry_column, s_geometry_type);
                    if (!s_srid.equals("4326")) { // Transform into wsg84 if needed
                        collectBoundsAndCenter(s_srid, centerCoordinate, boundsCoordinates);
                    } else {
                        centerCoordinate[0] = boundsCoordinates[0] + (boundsCoordinates[2] - boundsCoordinates[0]) / 2;
                        centerCoordinate[1] = boundsCoordinates[1] + (boundsCoordinates[3] - boundsCoordinates[1]) / 2;
                    }
                    checkAndAdaptDatabaseBounds(boundsCoordinates, null);
                    // no Zoom levels with
                    // vector data
                    SpatialVectorTable table = new SpatialVectorTable(getDatabasePath(), table_name, geometry_column,
                            i_geometry_type, s_srid, centerCoordinate, boundsCoordinates, s_layer_type);
                    // compleate list of fields of
                    // this table
                    fields_list = DaoSpatialite.collectTableFields(db_java, table_name);
                    table.setFieldsList(fields_list,"ROWID",-1);
                    vectorTableList.add(table);
                }

            } catch (Exception e) {
                GPLog.androidLog(4, "SpatialiteDatabaseHandler.get_tables_spatialite prepair[" + s_sql_layers + "]", e);
            } finally {
                if (this_stmt != null) {
                    this_stmt.close();
                }
            }
            this.vectorTableList = vectorTableList;
        }
        return table_fields;
    }

    /**
* Checks (and adapts) the overall database bounds based on the passed coordinates.
*
* <p>Goal: when painting the Geometries: check of viewport is inside these bounds.
* <br>- if the Viewport is outside these Bounds: all Tables can be ignored
* <br>-- this is called when the Tables are created
*
* @param boundsCoordinates bounds to check against the overall.
*/
    private void checkAndAdaptDatabaseBounds( double[] boundsCoordinates, int[] zoomLevels ) {
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
*
* <p>The {@link HashMap} will contain:
* <ul>
* <li>name of Field
* <li>type of field as defined in Database
* </ul>
*
* @param doLoadTables 0 = do not load table, check if valid only; 1=load tables
* @return fields_list [name of field, type of field]
*/
    private HashMap<String, String> checkAndCollectTables() throws Exception {
        // mj10777 20140315: when a final decision NOT to support normal-views is made
        // - the 'table_fields' logic can be removed
        HashMap<String, String> tableFields = new HashMap<String, String>();
        switch( databaseType ) {
        case GEOPACKAGE: {
            // GeoPackage Files [gpkg]
            tableFields = collectGpkgTables();
        }
            break;
        case SPATIALITE3:{
         // // Spatialite Files version 2+3=3 only
         tableFields = collectSpatialiteTables();
        }
        break;
        case SPATIALITE4: {
            // Spatialite Files version 4=4 only
             tableFields = collectVectorTables();

        }
            break;
        }
        if (isValid()) {
            switch( databaseType ) {
            case SPATIALITE3:
            case SPATIALITE4: {
                // Spatialite Files version 2+3=3 ; version 4=4
                // Placeholder for normal-views

            }
                break;
            }
            if (vectorTableList != null) {
                // now read styles
                checkPropertiesTable();
                // assign the styles
                for( SpatialVectorTable spatialTable : vectorTableList ) {
                    Style style4Table = null;
                    try {
                        style4Table = DaoSpatialite.getStyle4Table(db_java, spatialTable.getUniqueNameBasedOnDbFilePath());
                    } catch (java.lang.Exception e) {
                        DaoSpatialite.deleteStyleTable(db_java);
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
        return tableFields;
    }

    /**
* Update a style definiton in the db.
*
* @param style the {@link Style} to update.
* @throws Exception if something goes wrong.
*/
    public void updateStyle( Style style ) throws Exception {
        DaoSpatialite.updateStyle(db_java, style);
    }

    /**
* Delete and recreate a default properties table for this database.
*
* @throws Exception if something goes wrong.
*/
    public void resetStyleTable() throws Exception {
        DaoSpatialite.deleteStyleTable(db_java);
        DaoSpatialite.createPropertiesTable(db_java);
        for( SpatialVectorTable spatialTable : vectorTableList ) {
            DaoSpatialite.createDefaultPropertiesForTable(db_java, spatialTable.getUniqueNameBasedOnDbFilePath());
        }
    }

    public Database getDatabase() {
        return db_java;
    }

}
