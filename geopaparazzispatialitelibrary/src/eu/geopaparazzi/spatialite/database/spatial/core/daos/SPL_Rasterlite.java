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

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import jsqlite.Database;
import jsqlite.Stmt;

/**
 * SPL_Rasterlite related doa.
 *
 * @author Mark Johnson
 */
public class SPL_Rasterlite {

    /**
     * Return info of Rasterlite2
     * - will be filled on first Database connection when empty
     * -- called in checkDatabaseTypeAndValidity
     * --- if this is empty, then the Driver has NOT been compiled for RasterLite2
     * '0.8;x86_64-linux-gnu'
     */
    public static String Rasterlite2Version_CPU = "";

    /**
     * Retrieve rasterlite2 image of a given bound and size.
     * <p/>
     * <p>https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RL2_GetMapImage
     *
     * @param db the database to use.
     * @param rasterTable the table to use.
     * @param tileBounds  [west,south,east,north] [minx, miny, maxx, maxy] bounds.
     * @param tileSize default 256 [Tile.TILE_SIZE].
     * @return the image data as byte[]
     */
    public static byte[] getRasterTileInBounds(Database db, AbstractSpatialTable rasterTable, double[] tileBounds, int tileSize) {

        byte[] bytes = SPL_Rasterlite.rl2_GetMapImageTile(db, rasterTable.getSrid(), rasterTable.getTableName(),
                tileBounds, tileSize);
        if (bytes != null) {
            return bytes;
        }
        return null;
    }

    /**
     * Retrieve rasterlite2 tile of a given bound [4326,wsg84] with the given size.
     * <p/>
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RL2_GetMapImage
     *
     * @param sqlite_db    Database connection to use
     * @param destSrid     the destination srid (of the rasterlite2 image).
     * @param coverageName the table to use.
     * @param tileBounds   [west,south,east,north] [minx, miny, maxx, maxy] bounds.
     * @param i_tile_size  default 256 [Tile.TILE_SIZE].
     * @return the image data as byte[] as jpeg
     */
    public static byte[] rl2_GetMapImageTile(Database sqlite_db, String destSrid, String coverageName, double[] tileBounds,
                                             int i_tile_size) {
        return rl2_GetMapImage(sqlite_db, "4326", destSrid, coverageName, i_tile_size, i_tile_size, tileBounds,
                "default", "image/jpeg", "#ffffff", 0, 80, 1);
    }


    /**
     * Retrieve rasterlite2 image of a given bound and size.
     * - used by: SpatialiteUtilities.rl2_GetMapImageTile to retrieve tiles only
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RL2_GetMapImage
     *
     * @param sqlite_db    Database connection to use
     * @param sourceSrid   the srid (of the n/s/e/w positions).
     * @param destSrid     the destination srid (of the rasterlite2 image).
     * @param coverageName the table to use.
     * @param width        of image in pixel.
     * @param height       of image in pixel.
     * @param tileBounds   [west,south,east,north] [minx, miny, maxx, maxy] bounds.
     * @param styleName    used in coverage. default: 'default'
     * @param mimeType     'image/tiff' etc. default: 'image/png'
     * @param bgColor      html-syntax etc. default: '#ffffff'
     * @param transparent  0 to 100 (?).
     * @param quality      0-100 (for 'image/jpeg')
     * @param reaspect     1 = adapt image width,height if needed based on given bounds
     * @return the image data as byte[]
     */
    public static byte[] rl2_GetMapImage(Database sqlite_db, String sourceSrid, String destSrid, String coverageName, int width,
                                         int height, double[] tileBounds, String styleName, String mimeType, String bgColor, int transparent, int quality,
                                         int reaspect) {
        boolean doTransform = false;
        if (!sourceSrid.equals(destSrid)) {
            doTransform = true;
        }
        // sanity checks
        if (styleName.equals(""))
            styleName = "default";
        if (mimeType.equals(""))
            mimeType = "image/png";
        if (bgColor.equals(""))
            bgColor = "#ffffff";
        if ((transparent < 0) || (transparent > 100))
            transparent = 0;
        if ((quality < 0) || (quality > 100))
            quality = 0;
        if ((reaspect < 0) || (reaspect > 1))
            reaspect = 1; // adapt image width,height if needed based on given bounds [needed for
        // tiles]
        StringBuilder mbrSb = new StringBuilder();
        if (doTransform)
            mbrSb.append("ST_Transform(");
        mbrSb.append("BuildMBR(");
        mbrSb.append(tileBounds[0]);
        mbrSb.append(",");
        mbrSb.append(tileBounds[1]);
        mbrSb.append(",");
        mbrSb.append(tileBounds[2]);
        mbrSb.append(",");
        mbrSb.append(tileBounds[3]);
        if (doTransform) {
            mbrSb.append(",");
            mbrSb.append(sourceSrid);
            mbrSb.append("),");
            mbrSb.append(destSrid);
        }
        mbrSb.append(")");
        // SELECT
        // RL2_GetMapImage('berlin_postgrenzen.1890',BuildMBR(20800.0,22000.0,24000.0,19600.0),1200,1920,'default','image/png','#ffffff',0,0,1);
        String mbr = mbrSb.toString();
        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT RL2_GetMapImage('");
        qSb.append(coverageName);
        qSb.append("',");
        qSb.append(mbr);
        qSb.append(",");
        qSb.append(Integer.toString(width));
        qSb.append(",");
        qSb.append(Integer.toString(height));
        qSb.append(",'");
        qSb.append(styleName);
        qSb.append("','");
        qSb.append(mimeType);
        qSb.append("','");
        qSb.append(bgColor);
        qSb.append("',");
        qSb.append(Integer.toString(transparent));
        qSb.append(",");
        qSb.append(Integer.toString(quality));
        qSb.append(",");
        qSb.append(Integer.toString(reaspect));
        qSb.append(");");
        String s_sql_command = qSb.toString();
        Stmt this_stmt = null;
        byte[] ba_image = null;
        if (!SPL_Rasterlite.Rasterlite2Version_CPU.equals("")) { // only if rasterlite2 driver is active
            try {
                this_stmt = sqlite_db.prepare(s_sql_command);
                if (this_stmt.step()) {
                    ba_image = this_stmt.column_bytes(0);
                }
            } catch (jsqlite.Exception e_stmt) {
                /*
                  this internal lib error is not being caught and the application crashes
                  - the request was for a image 1/3 of the orignal size of 10607x8292 (3535x2764)
                  - big images should be avoided, since the application dies
                  'libc    : Fatal signal 11 (SIGSEGV) at 0x80c7a000 (code=1), thread 4216 (AsyncTask #2)'
                  '/data/app-lib/eu.hydrologis.geopaparazzi-2/libjsqlite.so (rl2_raster_decode+8248)'
                  'I WindowState: WIN DEATH: Window{41ee0100 u0 eu.hydrologis.geopaparazzi/eu.hydrologis.geopaparazzi.GeoPaparazziActivity}'
                */
                int i_rc = sqlite_db.last_error();
                GPLog.error("DAOSPATIALIE", "rl2_GetMapImage sql[" + s_sql_command + "] rc=" + i_rc + "]", e_stmt);
            } finally {
                // this_stmt.close();
            }
        }
        return ba_image;
    }


}
