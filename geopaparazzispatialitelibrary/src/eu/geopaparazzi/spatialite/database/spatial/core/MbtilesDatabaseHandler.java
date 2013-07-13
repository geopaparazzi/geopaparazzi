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
import java.util.List;

import jsqlite.Exception;
import android.graphics.Paint;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MBTilesDroidSpitter;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MbTilesMetadata;

/**
 * An utility class to handle an mbtiles database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MbtilesDatabaseHandler implements ISpatialDatabaseHandler {

    public final static String TABLE_METADATA = "metadata";
    public final static String COL_METADATA_NAME = "name";
    public final static String COL_METADATA_VALUE = "value";

    private List<SpatialRasterTable> rasterTableList;
    private String fileName;
    private MBTilesDroidSpitter db;

    public MbtilesDatabaseHandler( String dbPath ) {
        File spatialDbFile = new File(dbPath);
        if (!spatialDbFile.getParentFile().exists()) {
            throw new RuntimeException();
        }
        db = new MBTilesDroidSpitter(spatialDbFile);
        fileName = spatialDbFile.getName();

        int lastDot = fileName.lastIndexOf("."); //$NON-NLS-1$
        fileName = fileName.substring(0, lastDot);
    }

    public String getFileName() {
        return fileName;
    }

    public List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception {
        return Collections.emptyList();
    }

    public List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception {
        if (rasterTableList == null || forceRead) {
            rasterTableList = new ArrayList<SpatialRasterTable>();

            db.open(true, "1.0");
            MbTilesMetadata metadata = db.getMetadata();
            float[] bounds = metadata.bounds;// left, bottom, right, top

            // String tableName = metadata.name;
            String columnName = null;

            float centerX = 0f;
            float centerY = 0f;
            if (bounds != null) {
                centerX = bounds[0] + (bounds[2] - bounds[0]) / 2f;
                centerY = bounds[1] + (bounds[3] - bounds[1]) / 2f;
            }

            SpatialRasterTable table = new SpatialRasterTable(fileName, columnName, "3857", metadata.minZoom, metadata.maxZoom,
                    centerX, centerY, "?,?,?");
            rasterTableList.add(table);

        }
        return rasterTableList;
    }

    @Override
    public float[] getTableBounds( SpatialVectorTable spatialTable, String destSrid ) throws Exception {
        MbTilesMetadata metadata = db.getMetadata();
        float[] bounds = metadata.bounds;// left, bottom, right, top
        float w = bounds[0];
        float s = bounds[1];
        float e = bounds[2];
        float n = bounds[3];
        return new float[]{n, s, e, w};
    }

    public byte[] getRasterTile( String query ) {
        String[] split = query.split(",");
        if (split.length != 3) {
            return null;
        }
        int z = Integer.parseInt(split[0]);
        int x = Integer.parseInt(split[1]);
        int y = Integer.parseInt(split[2]);

        int[] tmsTileXY = googleTile2TmsTile(x, y, z);

        byte[] tileAsBytes = db.getTileAsBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]), split[0]);
        return tileAsBytes;
    }

    /**
     * Converts Google tile coordinates to TMS Tile coordinates.
     * 
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     * 
     * @param tx the x tile number.
     * @param ty the y tile number.
     * @param zoom the current zoom level.
     * @return the converted values.
     */
    public static int[] googleTile2TmsTile( int tx, int ty, int zoom ) {
        return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
    }

    public void close() throws Exception {
        if (db != null) {
            db.close();
        }
    }

    // /////////////////////////////////////////////////
    // UNUSED
    // /////////////////////////////////////////////////

    public GeometryIterator getGeometryIteratorInBounds( String destSrid, SpatialVectorTable table, double n, double s, double e,
            double w ) {
        return null;
    }
    public Paint getFillPaint4Style( Style style ) {
        return null;
    }

    public Paint getStrokePaint4Style( Style style ) {
        return null;
    }

    @Override
    public void updateStyle( Style style ) throws Exception {
    }

    @Override
    public void intersectionToStringBBOX( String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e,
            double w, StringBuilder sb, String indentStr ) throws Exception {
    }

    @Override
    public void intersectionToString4Polygon( String boundsSrid, SpatialVectorTable spatialTable, double n, double e,
            StringBuilder sb, String indentStr ) throws Exception {
    }

}
