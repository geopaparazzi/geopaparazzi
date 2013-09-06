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
package eu.geopaparazzi.spatialite.database.spatial.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

/**
 * An utility class to handle the spatial database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteDatabaseHandler implements ISpatialDatabaseHandler {

    // 3857
    // private GeometryFactory gf = new GeometryFactory();
    // private WKBWriter wr = new WKBWriter();
    // private WKBReader wkbReader = new WKBReader(gf);

    private static final String METADATA_TABLE_GEOPACKAGE_CONTENTS = "geopackage_contents";
    private static final String METADATA_TABLE_TILE_MATRIX = "tile_matrix_metadata";
    private static final String METADATA_TABLE_RASTER_COLUMNS = "raster_columns";
    private static final String METADATA_TABLE_GEOMETRY_COLUMNS = "geometry_columns";

    private static final String METADATA_GEOPACKAGECONTENT_TABLE_NAME = "table_name";
    private static final String METADATA_TILE_TABLE_NAME = "t_table_name";
    private static final String METADATA_ZOOM_LEVEL = "zoom_level";
    private static final String METADATA_RASTER_COLUMN = "r_raster_column";
    private static final String METADATA_RASTER_TABLE_NAME = "r_table_name";
    private static final String METADATA_SRID = "srid";
    private static final String METADATA_GEOMETRY_TYPE4 = "geometry_type";
    private static final String METADATA_GEOMETRY_TYPE3 = "type";
    private static final String METADATA_GEOMETRY_COLUMN = "f_geometry_column";
    private static final String METADATA_TABLE_NAME = "f_table_name";

    private static final String NAME = "name";
    private static final String SIZE = "size";
    private static final String FILLCOLOR = "fillcolor";
    private static final String STROKECOLOR = "strokecolor";
    private static final String FILLALPHA = "fillalpha";
    private static final String STROKEALPHA = "strokealpha";
    private static final String SHAPE = "shape";
    private static final String WIDTH = "width";
    private static final String TEXTSIZE = "textsize";
    private static final String TEXTFIELD = "textfield";
    private static final String ENABLED = "enabled";
    private static final String ORDER = "layerorder";
    private static final String DECIMATION = "decimationfactor";

    private final String PROPERTIESTABLE = "dataproperties";

    private Database db;

    private HashMap<String, Paint> fillPaints = new HashMap<String, Paint>();
    private HashMap<String, Paint> strokePaints = new HashMap<String, Paint>();

    private List<SpatialVectorTable> vectorTableList;
    private List<SpatialRasterTable> rasterTableList;
    private String fileName;

    public SpatialiteDatabaseHandler( String dbPath ) {
        try {
            File spatialDbFile = new File(dbPath);
            if (!spatialDbFile.getParentFile().exists()) {
                throw new RuntimeException();
            }
            db = new jsqlite.Database();
            db.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                    | jsqlite.Constants.SQLITE_OPEN_CREATE);
            fileName = spatialDbFile.getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * Get the version of Spatialite.
     * 
     * @return the version of Spatialite.
     * @throws Exception
     */
    public String getSpatialiteVersion() throws Exception {
        Stmt stmt = db.prepare("SELECT spatialite_version();");
        try {
            if (stmt.step()) {
                String value = stmt.column_string(0);
                return value;
            }
        } finally {
            stmt.close();
        }
        return "-";
    }

    /**
     * Get the version of proj.
     * 
     * @return the version of proj.
     * @throws Exception
     */
    public String getProj4Version() throws Exception {
        Stmt stmt = db.prepare("SELECT proj4_version();");
        try {
            if (stmt.step()) {
                String value = stmt.column_string(0);
                return value;
            }
        } finally {
            stmt.close();
        }
        return "-";
    }

    /**
     * Get the version of geos.
     * 
     * @return the version of geos.
     * @throws Exception
     */
    public String getGeosVersion() throws Exception {
        Stmt stmt = db.prepare("SELECT geos_version();");
        try {
            if (stmt.step()) {
                String value = stmt.column_string(0);
                return value;
            }
        } finally {
            stmt.close();
        }
        return "-";
    }

    @Override
    public List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception {
        if (vectorTableList == null || forceRead) {
            vectorTableList = new ArrayList<SpatialVectorTable>();

            StringBuilder sb3 = new StringBuilder();
            sb3.append("select ");
            sb3.append(METADATA_TABLE_NAME);
            sb3.append(", ");
            sb3.append(METADATA_GEOMETRY_COLUMN);
            sb3.append(", ");
            sb3.append(METADATA_GEOMETRY_TYPE3);
            sb3.append(",");
            sb3.append(METADATA_SRID);
            sb3.append(" from ");
            sb3.append(METADATA_TABLE_GEOMETRY_COLUMNS);
            sb3.append(";");
            String query3 = sb3.toString();

            boolean is3 = true;
            Stmt stmt = null;
            try {
                stmt = db.prepare(query3);
            } catch (java.lang.Exception e) {
                // try with spatialite 4 syntax
                StringBuilder sb4 = new StringBuilder();
                sb4.append("select ");
                sb4.append(METADATA_TABLE_NAME);
                sb4.append(", ");
                sb4.append(METADATA_GEOMETRY_COLUMN);
                sb4.append(", ");
                sb4.append(METADATA_GEOMETRY_TYPE4);
                sb4.append(",");
                sb4.append(METADATA_SRID);
                sb4.append(" from ");
                sb4.append(METADATA_TABLE_GEOMETRY_COLUMNS);
                sb4.append(";");
                String query4 = sb4.toString();
                stmt = db.prepare(query4);
                is3 = false;
            }
            try {
                while( stmt.step() ) {
                    String name = stmt.column_string(0);
                    String geomName = stmt.column_string(1);

                    int geomType = 0;
                    if (is3) {
                        String type = stmt.column_string(2);
                        geomType = GeometryType.forValue(type);
                    } else {
                        geomType = stmt.column_int(2);
                    }

                    String srid = String.valueOf(stmt.column_int(3));
                    SpatialVectorTable table = new SpatialVectorTable(name, geomName, geomType, srid);
                    vectorTableList.add(table);
                }
            } finally {
                stmt.close();
            }

            // now read styles
            checkPropertiesTable();

            // assign the styles
            for( SpatialVectorTable spatialTable : vectorTableList ) {
                Style style4Table = getStyle4Table(spatialTable.getName());
                if (style4Table == null) {
                    spatialTable.makeDefaultStyle();
                } else {
                    spatialTable.setStyle(style4Table);
                }
            }
        }
        OrderComparator orderComparator = new OrderComparator();
        Collections.sort(vectorTableList, orderComparator);

        return vectorTableList;
    }

    @Override
    public List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception {
        if (rasterTableList == null || forceRead) {
            rasterTableList = new ArrayList<SpatialRasterTable>();

            StringBuilder sb = new StringBuilder();
            sb.append("select ");
            sb.append(METADATA_RASTER_TABLE_NAME);
            sb.append(", ");
            sb.append(METADATA_RASTER_COLUMN);
            sb.append(", srid from ");
            sb.append(METADATA_TABLE_RASTER_COLUMNS);
            sb.append(";");
            String query = sb.toString();
            Stmt stmt = db.prepare(query);
            try {
                while( stmt.step() ) {
                    String tableName = stmt.column_string(0);
                    String columnName = stmt.column_string(1);
                    String srid = String.valueOf(stmt.column_int(2));

                    if (tableName != null) {
                        int[] zoomLevels = {0, 18};
                        getZoomLevels(tableName, zoomLevels);

                        double[] centerCoordinate = {0.0, 0.0};
                        getCenterCoordinate4326(tableName, centerCoordinate);

                        SpatialRasterTable table = new SpatialRasterTable(tableName, columnName, srid, zoomLevels[0],
                                zoomLevels[1], centerCoordinate[0], centerCoordinate[1], null);
                        rasterTableList.add(table);
                    }

                }
            } finally {
                stmt.close();
            }
        }
        // OrderComparator orderComparator = new OrderComparator();
        // Collections.sort(rasterTableList, orderComparator);

        return rasterTableList;
    }

    /**
     * Extract the center coordinate of a raster tileset.
     * 
     * @param tableName the raster table name.
     * @param centerCoordinate teh coordinate array to update with the extracted values.
     */
    private void getCenterCoordinate4326( String tableName, double[] centerCoordinate ) {
        try {
            Stmt centerStmt = null;
            try {
                WKBReader wkbReader = new WKBReader();

                StringBuilder centerBuilder = new StringBuilder();
                centerBuilder.append("select ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                // centerBuilder.append("select AsText(ST_Transform(MakePoint(");
                centerBuilder.append("(min_x + (max_x-min_x)/2), ");
                centerBuilder.append("(min_y + (max_y-min_y)/2), ");
                centerBuilder.append(METADATA_SRID);
                centerBuilder.append("), 4326))) from ");
                centerBuilder.append(METADATA_TABLE_GEOPACKAGE_CONTENTS);
                centerBuilder.append(" where ");
                centerBuilder.append(METADATA_GEOPACKAGECONTENT_TABLE_NAME);
                centerBuilder.append("='");
                centerBuilder.append(tableName);
                centerBuilder.append("';");
                String centerQuery = centerBuilder.toString();

                centerStmt = db.prepare(centerQuery);
                if (centerStmt.step()) {
                    // String geomBytes = centerStmt.column_string(0);
                    // System.out.println();
                    byte[] geomBytes = centerStmt.column_bytes(0);
                    Geometry geometry = wkbReader.read(geomBytes);
                    Coordinate coordinate = geometry.getCoordinate();
                    centerCoordinate[0] = coordinate.x;
                    centerCoordinate[1] = coordinate.y;
                }
            } finally {
                if (centerStmt != null)
                    centerStmt.close();
            }
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the available zoomlevels for a raster table.
     * 
     * @param tableName the raster table name.
     * @param zoomLevels the zoomlevels array to update with the min and max levels available.
     * @throws Exception
     */
    private void getZoomLevels( String tableName, int[] zoomLevels ) throws Exception {
        Stmt zoomStmt = null;
        try {
            StringBuilder zoomBuilder = new StringBuilder();
            zoomBuilder.append("SELECT min(");
            zoomBuilder.append(METADATA_ZOOM_LEVEL);
            zoomBuilder.append("),max(");
            zoomBuilder.append(METADATA_ZOOM_LEVEL);
            zoomBuilder.append(") FROM ");
            zoomBuilder.append(METADATA_TABLE_TILE_MATRIX);
            zoomBuilder.append(" WHERE ");
            zoomBuilder.append(METADATA_TILE_TABLE_NAME);
            zoomBuilder.append("='");
            zoomBuilder.append(tableName);
            zoomBuilder.append("';");
            String zoomQuery = zoomBuilder.toString();
            zoomStmt = db.prepare(zoomQuery);
            if (zoomStmt.step()) {
                zoomLevels[0] = zoomStmt.column_int(0);
                zoomLevels[1] = zoomStmt.column_int(1);
            }
        } finally {
            if (zoomStmt != null)
                zoomStmt.close();
        }
    }

    /**
     * Check availability of style for the tables.
     * 
     * @throws Exception
     */
    private void checkPropertiesTable() throws Exception {
        String checkTableQuery = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + PROPERTIESTABLE + "';";
        Stmt stmt = db.prepare(checkTableQuery);
        boolean tableExists = false;
        try {
            if (stmt.step()) {
                String name = stmt.column_string(0);
                if (name != null) {
                    tableExists = true;
                }
            }
        } finally {
            stmt.close();
        }
        if (!tableExists) {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ");
            sb.append(PROPERTIESTABLE);
            sb.append(" (");
            sb.append(NAME).append(" TEXT, ");
            sb.append(SIZE).append(" REAL, ");
            sb.append(FILLCOLOR).append(" TEXT, ");
            sb.append(STROKECOLOR).append(" TEXT, ");
            sb.append(FILLALPHA).append(" REAL, ");
            sb.append(STROKEALPHA).append(" REAL, ");
            sb.append(SHAPE).append(" TEXT, ");
            sb.append(WIDTH).append(" REAL, ");
            sb.append(TEXTSIZE).append(" REAL, ");
            sb.append(TEXTFIELD).append(" TEXT, ");
            sb.append(ENABLED).append(" INTEGER, ");
            sb.append(ORDER).append(" INTEGER,");
            sb.append(DECIMATION).append(" REAL");
            sb.append(" );");
            String query = sb.toString();
            db.exec(query, null);

            for( SpatialVectorTable spatialTable : vectorTableList ) {
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
                sbIn.append(TEXTSIZE).append(" , ");
                sbIn.append(TEXTFIELD).append(" , ");
                sbIn.append(ENABLED).append(" , ");
                sbIn.append(ORDER).append(" , ");
                sbIn.append(DECIMATION);
                sbIn.append(" ) ");
                sbIn.append(" values ");
                sbIn.append(" ( ");
                Style style = new Style();
                style.name = spatialTable.getName();
                sbIn.append(style.insertValuesString());
                sbIn.append(" );");

                String insertQuery = sbIn.toString();
                db.exec(insertQuery, null);
            }
        }
    }

    /**
     * Retrieve the {@link Style} for a given table.
     * 
     * @param tableName
     * @return
     * @throws Exception
     */
    public Style getStyle4Table( String tableName ) throws Exception {
        Style style = new Style();
        style.name = tableName;

        StringBuilder sbSel = new StringBuilder();
        sbSel.append("select ");
        sbSel.append(SIZE).append(" , ");
        sbSel.append(FILLCOLOR).append(" , ");
        sbSel.append(STROKECOLOR).append(" , ");
        sbSel.append(FILLALPHA).append(" , ");
        sbSel.append(STROKEALPHA).append(" , ");
        sbSel.append(SHAPE).append(" , ");
        sbSel.append(WIDTH).append(" , ");
        sbSel.append(TEXTSIZE).append(" , ");
        sbSel.append(TEXTFIELD).append(" , ");
        sbSel.append(ENABLED).append(" , ");
        sbSel.append(ORDER).append(" , ");
        sbSel.append(DECIMATION);
        sbSel.append(" from ");
        sbSel.append(PROPERTIESTABLE);
        sbSel.append(" where ");
        sbSel.append(NAME).append(" ='").append(tableName).append("';");

        String selectQuery = sbSel.toString();
        Stmt stmt = db.prepare(selectQuery);
        try {
            if (stmt.step()) {
                style.size = (float) stmt.column_double(0);
                style.fillcolor = stmt.column_string(1);
                style.strokecolor = stmt.column_string(2);
                style.fillalpha = (float) stmt.column_double(3);
                style.strokealpha = (float) stmt.column_double(4);
                style.shape = stmt.column_string(5);
                style.width = (float) stmt.column_double(6);
                style.textsize = (float) stmt.column_double(7);
                style.textfield = stmt.column_string(8);
                style.enabled = stmt.column_int(9);
                style.order = stmt.column_int(10);
                style.decimationFactor = (float) stmt.column_double(11);
            }
        } finally {
            stmt.close();
        }
        return style;
    }

    public float[] getTableBounds( SpatialVectorTable spatialTable, String destSrid ) throws Exception {
        boolean doTransform = false;
        if (!spatialTable.getSrid().equals(destSrid)) {
            doTransform = true;
        }

        StringBuilder geomSb = new StringBuilder();
        if (doTransform)
            geomSb.append("ST_Transform(");
        geomSb.append(spatialTable.getGeomName());
        if (doTransform) {
            geomSb.append(", ");
            geomSb.append(destSrid);
            geomSb.append(")");
        }
        String geom = geomSb.toString();

        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT Min(MbrMinX(");
        qSb.append(geom);
        qSb.append(")) AS min_x, Min(MbrMinY(");
        qSb.append(geom);
        qSb.append(")) AS min_y,");
        qSb.append("Max(MbrMaxX(");
        qSb.append(geom);
        qSb.append(")) AS max_x, Max(MbrMaxY(");
        qSb.append(geom);
        qSb.append(")) AS max_y");
        qSb.append(" FROM ");
        qSb.append(spatialTable.getName());
        qSb.append(";");

        String selectQuery = qSb.toString();
        Stmt stmt = db.prepare(selectQuery);
        try {
            if (stmt.step()) {
                float w = (float) stmt.column_double(0);
                float s = (float) stmt.column_double(1);
                float e = (float) stmt.column_double(2);
                float n = (float) stmt.column_double(3);

                return new float[]{n, s, e, w};
            }
        } finally {
            stmt.close();
        }
        return null;
    }

    /**
     * Update a style definition.
     * 
     * @param style the {@link Style} to set.
     * @throws Exception
     */
    public void updateStyle( Style style ) throws Exception {
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
        sbIn.append(TEXTSIZE).append("=").append(style.textsize).append(" , ");
        sbIn.append(TEXTFIELD).append("='").append(style.textfield).append("' , ");
        sbIn.append(ENABLED).append("=").append(style.enabled).append(" , ");
        sbIn.append(ORDER).append("=").append(style.order).append(" , ");
        sbIn.append(DECIMATION).append("=").append(style.decimationFactor);
        sbIn.append(" where ");
        sbIn.append(NAME);
        sbIn.append("='");
        sbIn.append(style.name);
        sbIn.append("';");

        String updateQuery = sbIn.toString();
        db.exec(updateQuery, null);
    }

    @Override
    public Paint getFillPaint4Style( Style style ) {
        Paint paint = fillPaints.get(style.name);
        if (paint == null) {
            paint = new Paint();
            fillPaints.put(style.name, paint);
        }
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor(style.fillcolor));
        float alpha = style.fillalpha * 255f;
        paint.setAlpha((int) alpha);
        return paint;
    }

    @Override
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
        paint.setColor(Color.parseColor(style.strokecolor));
        float alpha = style.strokealpha * 255f;
        paint.setAlpha((int) alpha);
        paint.setStrokeWidth(style.width);
        return paint;
    }

    public List<byte[]> getWKBFromTableInBounds( String destSrid, SpatialVectorTable table, double n, double s, double e, double w ) {
        List<byte[]> list = new ArrayList<byte[]>();
        String query = buildGeometriesInBoundsQuery(destSrid, table, n, s, e, w);
        try {
            Stmt stmt = db.prepare(query);
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
            Stmt stmt = db.prepare(query);
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

    @Override
    public GeometryIterator getGeometryIteratorInBounds( String destSrid, SpatialVectorTable table, double n, double s, double e,
            double w ) {
        String query = buildGeometriesInBoundsQuery(destSrid, table, n, s, e, w);
        return new GeometryIterator(db, query);
    }

    private String buildGeometriesInBoundsQuery( String destSrid, SpatialVectorTable table, double n, double s, double e, double w ) {
        boolean doTransform = false;
        if (!table.getSrid().equals(destSrid)) {
            doTransform = true;
        }

        StringBuilder mbrSb = new StringBuilder();
        if (doTransform)
            mbrSb.append("ST_Transform(");
        mbrSb.append("BuildMBR(");
        mbrSb.append(w);
        mbrSb.append(", ");
        mbrSb.append(n);
        mbrSb.append(", ");
        mbrSb.append(e);
        mbrSb.append(", ");
        mbrSb.append(s);
        if (doTransform) {
            mbrSb.append(", ");
            mbrSb.append(destSrid);
            mbrSb.append("), ");
            mbrSb.append(table.getSrid());
        }
        mbrSb.append(")");
        String mbr = mbrSb.toString();

        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT ST_AsBinary(CastToXY(");
        if (doTransform)
            qSb.append("ST_Transform(");
        qSb.append(table.getGeomName());
        if (doTransform) {
            qSb.append(", ");
            qSb.append(destSrid);
            qSb.append(")");
        }
        qSb.append("))");
        // qSb.append(", AsText(");
        // if (doTransform)
        // qSb.append("ST_Transform(");
        // qSb.append(table.geomName);
        // if (doTransform) {
        // qSb.append(", ");
        // qSb.append(destSrid);
        // qSb.append(")");
        // }
        // qSb.append(")");
        qSb.append(" FROM ");
        qSb.append(table.getName());
        qSb.append(" WHERE ST_Intersects(");
        qSb.append(table.getGeomName());
        qSb.append(", ");
        qSb.append(mbr);
        qSb.append(") = 1");
        qSb.append("   AND ROWID IN (");
        qSb.append("     SELECT ROWID FROM Spatialindex WHERE f_table_name ='");
        qSb.append(table.getName());
        qSb.append("'");
        qSb.append("     AND search_frame = ");
        qSb.append(mbr);
        qSb.append(" );");
        String q = qSb.toString();

        return q;
    }

    public void close() throws Exception {
        if (db != null) {
            db.close();
        }
    }

    public void intersectionToStringBBOX( String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e,
            double w, StringBuilder sb, String indentStr ) throws Exception {
        boolean doTransform = false;
        if (!spatialTable.getSrid().equals(boundsSrid)) {
            doTransform = true;
        }

        String query = null;

        // SELECT che-cazzo-ti-pare-a-te
        // FROM qualche-tavola
        // WHERE ROWID IN (
        // SELECT ROWID
        // FROM SpatialIndex
        // WHERE f_table_name = 'qualche-tavola'
        // AND search_frame = il-tuo-bbox
        // );

        // {
        // StringBuilder sbQ = new StringBuilder();
        // sbQ.append("SELECT ");
        // sbQ.append("*");
        // sbQ.append(" from ").append(spatialTable.name);
        // sbQ.append(" where ROWID IN (");
        // sbQ.append(" SELECT ROWID FROM Spatialindex WHERE f_table_name ='");
        // sbQ.append(spatialTable.name);
        // sbQ.append("' AND search_frame = ");
        // if (doTransform)
        // sbQ.append("ST_Transform(");
        // sbQ.append("BuildMBR(");
        // sbQ.append(w);
        // sbQ.append(", ");
        // sbQ.append(s);
        // sbQ.append(", ");
        // sbQ.append(e);
        // sbQ.append(", ");
        // sbQ.append(n);
        // if (doTransform) {
        // sbQ.append(", ");
        // sbQ.append(boundsSrid);
        // }
        // sbQ.append(")");
        // if (doTransform) {
        // sbQ.append(",");
        // sbQ.append(spatialTable.srid);
        // sbQ.append(")");
        // }
        // sbQ.append(");");
        //
        // query = sbQ.toString();
        // Logger.i(this, query);
        // }
        {
            StringBuilder sbQ = new StringBuilder();
            sbQ.append("SELECT ");
            sbQ.append("*");
            sbQ.append(" from ").append(spatialTable.getName());
            sbQ.append(" where ST_Intersects(");
            if (doTransform)
                sbQ.append("ST_Transform(");
            sbQ.append("BuildMBR(");
            sbQ.append(w);
            sbQ.append(", ");
            sbQ.append(s);
            sbQ.append(", ");
            sbQ.append(e);
            sbQ.append(", ");
            sbQ.append(n);
            if (doTransform) {
                sbQ.append(", ");
                sbQ.append(boundsSrid);
                sbQ.append("),");
                sbQ.append(spatialTable.getSrid());
            }
            sbQ.append("),");
            sbQ.append(spatialTable.getGeomName());
            sbQ.append(");");

            query = sbQ.toString();

            // Logger.i(this, query);
        }

        Stmt stmt = db.prepare(query);
        try {
            while( stmt.step() ) {
                int column_count = stmt.column_count();
                for( int i = 0; i < column_count; i++ ) {
                    String cName = stmt.column_name(i);
                    if (cName.equalsIgnoreCase(spatialTable.getGeomName())) {
                        continue;
                    }

                    String value = stmt.column_string(i);
                    sb.append(indentStr).append(cName).append(": ").append(value).append("\n");
                }
                sb.append("\n");
            }
        } finally {
            stmt.close();
        }
    }

    public void intersectionToString4Polygon( String queryPointSrid, SpatialVectorTable spatialTable, double n, double e,
            StringBuilder sb, String indentStr ) throws Exception {
        boolean doTransform = false;
        if (!spatialTable.getSrid().equals(queryPointSrid)) {
            doTransform = true;
        }

        StringBuilder sbQ = new StringBuilder();
        sbQ.append("SELECT * FROM ");
        sbQ.append(spatialTable.getName());
        sbQ.append(" WHERE ST_Intersects(");
        sbQ.append(spatialTable.getGeomName());
        sbQ.append(", ");
        if (doTransform)
            sbQ.append("ST_Transform(");
        sbQ.append("MakePoint(");
        sbQ.append(e);
        sbQ.append(",");
        sbQ.append(n);
        if (doTransform) {
            sbQ.append(", ");
            sbQ.append(queryPointSrid);
            sbQ.append("), ");
            sbQ.append(spatialTable.getSrid());
        }
        sbQ.append(")) = 1 ");
        sbQ.append("AND ROWID IN (");
        sbQ.append("SELECT ROWID FROM Spatialindex WHERE f_table_name ='");
        sbQ.append(spatialTable.getName());
        sbQ.append("' AND search_frame = ");
        if (doTransform)
            sbQ.append("ST_Transform(");
        sbQ.append("MakePoint(");
        sbQ.append(e);
        sbQ.append(",");
        sbQ.append(n);
        if (doTransform) {
            sbQ.append(", ");
            sbQ.append(queryPointSrid);
            sbQ.append("), ");
            sbQ.append(spatialTable.getSrid());
        }
        sbQ.append("));");
        String query = sbQ.toString();

        Stmt stmt = db.prepare(query);
        try {
            while( stmt.step() ) {
                int column_count = stmt.column_count();
                for( int i = 0; i < column_count; i++ ) {
                    String cName = stmt.column_name(i);
                    if (cName.equalsIgnoreCase(spatialTable.getGeomName())) {
                        continue;
                    }

                    String value = stmt.column_string(i);
                    sb.append(indentStr).append(cName).append(": ").append(value).append("\n");
                }
                sb.append("\n");
            }
        } finally {
            stmt.close();
        }
    }

    // public String queryComuni() {
    // sb.append(SEP);
    // sb.append("Query Comuni...\n");
    //
    // String query = "SELECT " + NOME + //
    // " from " + COMUNITABLE + //
    // " order by " + NOME + ";";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db.prepare(query);
    // int index = 0;
    // while( stmt.step() ) {
    // String nomeStr = stmt.column_string(0);
    // sb.append("\t").append(nomeStr).append("\n");
    // if (index++ > 5) {
    // break;
    // }
    // }
    // sb.append("\t...");
    // stmt.close();
    // } catch (Exception e) {
    // error(e);
    // }
    //
    // sb.append("Done...\n");
    //
    // return sb.toString();
    // }
    //
    // public String queryComuniWithGeom() {
    // sb.append(SEP);
    // sb.append("Query Comuni with AsText(Geometry)...\n");
    //
    // String query = "SELECT " + NOME + //
    // " , " + AS_TEXT_GEOMETRY + //
    // " as geom from " + COMUNITABLE + //
    // " where geom not null;";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db.prepare(query);
    // while( stmt.step() ) {
    // String nomeStr = stmt.column_string(0);
    // String geomStr = stmt.column_string(1);
    // String substring = geomStr;
    // if (substring.length() > 40)
    // substring = geomStr.substring(0, 40);
    // sb.append("\t").append(nomeStr).append(" - ").append(substring).append("...\n");
    // break;
    // }
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // sb.append("Done...\n");
    //
    // return sb.toString();
    // }
    //
    // public String queryGeomTypeAndSrid() {
    // sb.append(SEP);
    // sb.append("Query Comuni geom type and srid...\n");
    //
    // String query = "SELECT " + NOME + //
    // " , " + AS_TEXT_GEOMETRY + //
    // " as geom from " + COMUNITABLE + //
    // " where geom not null;";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db.prepare(query);
    // while( stmt.step() ) {
    // String nomeStr = stmt.column_string(0);
    // String geomStr = stmt.column_string(1);
    // String substring = geomStr;
    // if (substring.length() > 40)
    // substring = geomStr.substring(0, 40);
    // sb.append("\t").append(nomeStr).append(" - ").append(substring).append("...\n");
    // break;
    // }
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // sb.append("Done...\n");
    //
    // return sb.toString();
    // }
    //
    // public String queryComuniArea() {
    // sb.append(SEP);
    // sb.append("Query Comuni area sum...\n");
    //
    // String query = "SELECT ST_Area(Geometry) / 1000000.0 from " + COMUNITABLE + //
    // ";";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db.prepare(query);
    // double totalArea = 0;
    // while( stmt.step() ) {
    // double area = stmt.column_double(0);
    // totalArea = totalArea + area;
    // }
    // sb.append("\tTotal area by summing each area: ").append(totalArea).append("Km2\n");
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // query = "SELECT sum(ST_Area(Geometry) / 1000000.0) from " + COMUNITABLE + //
    // ";";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db.prepare(query);
    // double totalArea = 0;
    // if (stmt.step()) {
    // double area = stmt.column_double(0);
    // totalArea = totalArea + area;
    // }
    // sb.append("\tTotal area by summing in query: ").append(totalArea).append("Km2\n");
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // sb.append("Done...\n");
    //
    // return sb.toString();
    // }
    //
    // public String queryComuniNearby() {
    // sb.append(SEP);
    // sb.append("Query Comuni nearby...\n");
    //
    // String query =
    // "SELECT Hex(ST_AsBinary(ST_Buffer(Geometry, 1.0))), ST_Srid(Geometry), ST_GeometryType(Geometry) from "
    // + COMUNITABLE + //
    // " where " + NOME + "= 'Bolzano';";
    // sb.append("Execute query: ").append(query).append("\n");
    // String bufferGeom = "";
    // String bufferGeomShort = "";
    // try {
    // Stmt stmt = db.prepare(query);
    // if (stmt.step()) {
    // bufferGeom = stmt.column_string(0);
    // String geomSrid = stmt.column_string(1);
    // String geomType = stmt.column_string(2);
    // sb.append("\tThe selected geometry is of type: ").append(geomType).append(" and of SRID: ").append(geomSrid)
    // .append("\n");
    // }
    // bufferGeomShort = bufferGeom;
    // if (bufferGeom.length() > 10)
    // bufferGeomShort = bufferGeom.substring(0, 10) + "...";
    // sb.append("\tBolzano polygon buffer geometry in HEX: ").append(bufferGeomShort).append("\n");
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    //
    // query = "SELECT " + NOME + ", AsText(ST_centroid(Geometry)) from " + COMUNITABLE + //
    // " where ST_Intersects( ST_GeomFromWKB(x'" + bufferGeom + "') , Geometry );";
    // // just for print
    // String tmpQuery = "SELECT " + NOME + " from " + COMUNITABLE + //
    // " where ST_Intersects( ST_GeomFromWKB(x'" + bufferGeomShort + "') , Geometry );";
    // sb.append("Execute query: ").append(tmpQuery).append("\n");
    // try {
    // sb.append("\tComuni nearby Bolzano: \n");
    // Stmt stmt = db.prepare(query);
    // while( stmt.step() ) {
    // String name = stmt.column_string(0);
    // String wkt = stmt.column_string(1);
    // sb.append("\t\t").append(name).append(" - with centroid in ").append(wkt).append("\n");
    // }
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // sb.append("Done...\n");
    //
    // return sb.toString();
    // }
    //
    // public byte[] getBolzanoWKB() {
    // String query = "SELECT ST_AsBinary(ST_Transform(Geometry, 4326)) from " + COMUNITABLE + //
    // " where " + NOME + "= 'Bolzano';";
    // try {
    // Stmt stmt = db.prepare(query);
    // byte[] theGeom = null;
    // if (stmt.step()) {
    // theGeom = stmt.column_bytes(0);
    // }
    // stmt.close();
    // return theGeom;
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // return null;
    // }
    //
    // public List<byte[]> getIntersectingWKB( double n, double s, double e, double w ) {
    // List<byte[]> list = new ArrayList<byte[]>();
    // Coordinate ll = new Coordinate(w, s);
    // Coordinate ul = new Coordinate(w, n);
    // Coordinate ur = new Coordinate(e, n);
    // Coordinate lr = new Coordinate(e, s);
    // Polygon bboxPolygon = gf.createPolygon(new Coordinate[]{ll, ul, ur, lr, ll});
    //
    // byte[] bbox = wr.write(bboxPolygon);
    // String query = "SELECT ST_AsBinary(ST_Transform(Geometry, 4326)) from " + COMUNITABLE + //
    // " where ST_Intersects(ST_Transform(Geometry, 4326), ST_GeomFromWKB(?));";
    // try {
    // Stmt stmt = db.prepare(query);
    // stmt.bind(1, bbox);
    // while( stmt.step() ) {
    // list.add(stmt.column_bytes(0));
    // }
    // stmt.close();
    // return list;
    // } catch (Exception ex) {
    // ex.printStackTrace();
    // }
    // return null;
    // }
    //
    // public String doSimpleTransform() {
    //
    // sb.append(SEP);
    // sb.append("Coordinate transformation...\n");
    //
    // String query = "SELECT AsText(Transform(MakePoint(" + TEST_LON + ", " + TEST_LAT +
    // ", 4326), 32632));";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db.prepare(query);
    // if (stmt.step()) {
    // String pointStr = stmt.column_string(0);
    // sb.append("\t").append(TEST_LON + "/" + TEST_LAT + "/EPSG:4326").append(" = ")//
    // .append(pointStr + "/EPSG:32632").append("...\n");
    // }
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // sb.append("Done...\n");
    //
    // return sb.toString();
    //
    // }

}
