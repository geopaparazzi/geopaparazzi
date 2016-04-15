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

import java.util.HashMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.geopaparazzi.library.util.LibraryConstants;
import jsqlite.*;

/**
 * SPL_Rasterlite related doa.
 *
 * @author Mark Johnson
 */
public class SPL_Rasterlite {
    public static final String LOGTAG = "SPL_RASTERLITE";
    /**
     * Return info of Rasterlite2
     * - will be filled on first Database connection when empty
     * -- called in checkDatabaseTypeAndValidity
     * --- if this is empty, then the Driver has NOT been compiled for RasterLite2
     * '0.8;x86_64-linux-gnu'
     */
    public static String Rasterlite2Version_CPU = "";

    /*
     * @return true, if there is rasterlite support.
     */
    public static boolean hasRasterLiteSupport(){
        return !SPL_Rasterlite.Rasterlite2Version_CPU.equals("");
    }

    /**
     * Retrieve rasterlite2 Raster-Image of a given bound and size.
     * <p/>
     * <p>https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RL2_GetMapImageFromRaster
     *
     * @param db the database to use.
     * @param rasterTable the table to use.
     * @param tileBounds  [west,south,east,north] [minx, miny, maxx, maxy] bounds.
     * @param tileSize default 256 [Tile.TILE_SIZE].
     * @return the image data as byte[]
     */
    public static byte[] getRasterTileInBounds(Database db, AbstractSpatialTable rasterTable, double[] tileBounds, int tileSize) {

        byte[] bytes = SPL_Rasterlite.rl2_GetMapImageFromRasterTile(db, rasterTable.getSrid(), rasterTable.getTableName(),
                tileBounds, tileSize, rasterTable.getStyleNameRaster());
        if (bytes != null) {
            return bytes;
        }
        return null;
    }

    /**
     * Retrieve rasterlite2 Raster-Tile of a given bound [4326,wsg84] with the given size.
     * <p/>
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RL2_GetMapImageFromRaster
     *
     * @param dbSpatialite    Database connection to use
     * @param destSrid     the destination srid (of the rasterlite2 image).
     * @param coverageName the table to use.
     * @param tileBounds   [west,south,east,north] [minx, miny, maxx, maxy] bounds.
     * @param i_tile_size  default 256 [Tile.TILE_SIZE].
     * @param styleName the table to use.
     * @return the image data as byte[] as jpeg
     */
    public static byte[] rl2_GetMapImageFromRasterTile(Database dbSpatialite, String destSrid, String coverageName, double[] tileBounds,
                                             int i_tile_size,String styleNameRaster) {
        return rl2_GetMapImageFromRaster(dbSpatialite, "4326", destSrid, coverageName, i_tile_size, i_tile_size, tileBounds,
                styleNameRaster, "image/jpeg", "#ffffff", 0, 80, 1);
    }

    /**
     * Retrieve rasterlite2 Raster-Image of a given bound and size.
     * - used by: SpatialiteUtilities.rl2_GetMapImageFromRasterTile to retrieve tiles only
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RL2_GetMapImageFromRaster
     *
     * @param dbSpatialite    Database connection to use
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
     // GetMapImageFromRaster(text coverage, BLOB geom, int width, int height,text style, text format, text bg_color,int transparent, int quality, int reaspect)                   
    public static byte[] rl2_GetMapImageFromRaster(Database dbSpatialite, String sourceSrid, String destSrid, String coverageName, int width,
                                         int height, double[] tileBounds, String styleNameRaster, String mimeType, String bgColor, int transparent, int quality,
                                         int reaspect) {
        boolean doTransform = false;
        if (!sourceSrid.equals(destSrid)) {
            doTransform = true;
        }
        // sanity checks
        if (styleNameRaster.equals(""))
            styleNameRaster = "default";
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
        // RL2_GetMapImageFromRaster('1890.berlin_postgrenzen',BuildMBR(20800.0,22000.0,24000.0,19600.0),1200,1920,'default','image/png','#ffffff',0,0,1);
        String mbr = mbrSb.toString();
        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT RL2_GetMapImageFromRaster('");
        qSb.append(coverageName);
        qSb.append("',");
        qSb.append(mbr);
        qSb.append(",");
        qSb.append(width);
        qSb.append(",");
        qSb.append(height);
        qSb.append(",'");
        qSb.append(styleNameRaster);
        qSb.append("','");
        qSb.append(mimeType);
        qSb.append("','");
        qSb.append(bgColor);
        qSb.append("',");
        qSb.append(transparent);
        qSb.append(",");
        qSb.append(quality);
        qSb.append(",");
        qSb.append(reaspect);
        qSb.append(");");
        String s_sql_command = qSb.toString();
        Stmt stmt = null;
        byte[] ba_image = null;
        if (!SPL_Rasterlite.Rasterlite2Version_CPU.equals("")) { // only if rasterlite2 driver is active
            try {
                stmt = dbSpatialite.prepare(s_sql_command);
                if (stmt.step()) {
                    ba_image = stmt.column_bytes(0);
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
                int i_rc = dbSpatialite.last_error();
                GPLog.error("SPL_Rasterlite", "rl2_GetMapImageFromRaster sql[" + s_sql_command + "] rc=" + i_rc + "]", e_stmt);
            } finally {
                if(stmt!=null)
                    try {
                        stmt.close();
                    } catch (jsqlite.Exception e) {
                        GPLog.error("SPL_Rasterlite", "rl2_GetMapImageFromRaster sql[" + s_sql_command + "]", e);
                    }
            }
        }
        return ba_image;
    }
    /**
     * Retrieve rasterlite2 Vector-Image of a given bound and size.
     * <p/>
     * [20150720: it is not yet clear for what is to be used for, or how the result look like]
     *
     * @param db the database to use.
     * @param rasterTable the table to use.
     * @param tileBounds  [west,south,east,north] [minx, miny, maxx, maxy] bounds.
     * @param tileSize default 256 [Tile.TILE_SIZE].
     * @return the image data as byte[]
     */
    public static byte[] getVectorTileInBounds(Database db, AbstractSpatialTable rasterTable, double[] tileBounds, int tileSize) {

        byte[] bytes = SPL_Rasterlite.rl2_GetMapImageFromVectorTile(db, rasterTable.getSrid(), rasterTable.getTableName(),
                tileBounds, tileSize, rasterTable.getStyleNameVector());
        if (bytes != null) {
            return bytes;
        }
        return null;
    }
    /**
     * Retrieve rasterlite2 Vector-Tile of a given bound [4326,wsg84] with the given size.
     * <p/>
     * [20150720: it is not yet clear for what is to be used for, or how the result look like]
     *
     * @param dbSpatialite    Database connection to use
     * @param destSrid     the destination srid (of the rasterlite2 image).
     * @param coverageName the table to use.
     * @param tileBounds   [west,south,east,north] [minx, miny, maxx, maxy] bounds.
     * @param i_tile_size  default 256 [Tile.TILE_SIZE].
     * @param styleName the table to use.
     * @return the image data as byte[] as jpeg
     */
    public static byte[] rl2_GetMapImageFromVectorTile(Database dbSpatialite, String destSrid, String coverageName, double[] tileBounds,
                                             int i_tile_size,String styleNameVector) {
        return rl2_GetMapImageFromVector(dbSpatialite, "4326", destSrid, coverageName, i_tile_size, i_tile_size, tileBounds,
                styleNameVector, "image/jpeg", "#ffffff", 0, 80, 1);
    }
    /**
     * Retrieve rasterlite2 Vector-Image of a given bound and size.
     * - used by: SpatialiteUtilities.rl2_GetMapImageFromVectorTile to retrieve tiles only
     * [20150720: it is not yet clear for what is to be used for, or how the result look like]
     *
     * @param dbSpatialite    Database connection to use
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
     // GetMapImageFromVector(text coverage, BLOB geom, int width, int height,text style, text format, text bg_color,	int transparent, int quality, int reaspect)             
    public static byte[] rl2_GetMapImageFromVector(Database dbSpatialite, String sourceSrid, String destSrid, String coverageName, int width,
                                         int height, double[] tileBounds, String styleNameVector, String mimeType, String bgColor, int transparent, int quality,
                                         int reaspect) {
        boolean doTransform = false;
        if (!sourceSrid.equals(destSrid)) {
            doTransform = true;
        }
        // sanity checks
        if (styleNameVector.equals(""))
            styleNameVector = "default";
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
        // RL2_GetMapImageFromVector('1890.berlin_postgrenzen',BuildMBR(20800.0,22000.0,24000.0,19600.0),1200,1920,'default','image/png','#ffffff',0,0,1);
        String mbr = mbrSb.toString();
        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT RL2_GetMapImageFromVector('");
        qSb.append(coverageName);
        qSb.append("',");
        qSb.append(mbr);
        qSb.append(",");
        qSb.append(width);
        qSb.append(",");
        qSb.append(height);
        qSb.append(",'");
        qSb.append(styleNameVector);
        qSb.append("','");
        qSb.append(mimeType);
        qSb.append("','");
        qSb.append(bgColor);
        qSb.append("',");
        qSb.append(transparent);
        qSb.append(",");
        qSb.append(quality);
        qSb.append(",");
        qSb.append(reaspect);
        qSb.append(");");
        String s_sql_command = qSb.toString();
        Stmt stmt = null;
        byte[] ba_image = null;
        if (!SPL_Rasterlite.Rasterlite2Version_CPU.equals("")) { // only if rasterlite2 driver is active
            try {
                stmt = dbSpatialite.prepare(s_sql_command);
                if (stmt.step()) {
                    ba_image = stmt.column_bytes(0);
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
                int i_rc = dbSpatialite.last_error();
                GPLog.error("SPL_Rasterlite", "rl2_GetMapImageFromVector sql[" + s_sql_command + "] rc=" + i_rc + "]", e_stmt);
            } finally {
                if(stmt!=null)
                    try {
                        stmt.close();
                    } catch (jsqlite.Exception e) {
                        GPLog.error("SPL_Rasterlite", "rl2_GetMapImageFromVector sql[" + s_sql_command + "]", e);
                    }
            }
        }
        return ba_image;
    }
    /**
     *  HashMap<Integer, Double> of Zoom-Levels 0-30
     *  Zoom-Level, Width in World-Mercator-Meters of a 256x256 Tile
     *  - 'WGS 84 / World Mercator' (3395) - UNIT=Meters            
     * @return zoom_levels
     */     
    public static int[] rl2_calculate_zoom_levels(Database dbSpatialite, String coverageName, String s_tile_size,int i_srid) {
        HashMap<Integer, Double> zoom_levels = new HashMap<Integer, Double>();  
        // TODO: remove to Mercator-specific definitions
        // 'WGS 84 / World Mercator' (3395) - UNIT=Meters            
        // Zoom-Level, Width in Meters of a 256x256 Tile
        // Value returned by gdalinfo for a geo-referenced Tile
        // values based on the position: 13.37771496361961 52.51628011262304 [Brandenburg Gate, Berlin, Germany]
        zoom_levels.put( 0,40032406.294);
        zoom_levels.put( 1,20016203.147);
        zoom_levels.put( 2,9999156.402);
        zoom_levels.put( 3,4995382.840);
        zoom_levels.put( 4,2501025.826);
        zoom_levels.put( 5,1250779.611);
        zoom_levels.put( 6,625329.273);
        zoom_levels.put( 7,312680.586);
        zoom_levels.put( 8,156344.180);
        zoom_levels.put( 9,78173.049);
        zoom_levels.put(10,39086.762);
        zoom_levels.put(11,19543.440) ;
        zoom_levels.put(12,9771.764);
        zoom_levels.put(13,4885.886);
        zoom_levels.put(14,2442.942);
        zoom_levels.put(15,1221.471);
        zoom_levels.put(16,610.735);
        zoom_levels.put(17,305.367);
        zoom_levels.put(18,152.683);
        zoom_levels.put(19,76.341);
        zoom_levels.put(20,38.170);
        zoom_levels.put(21,19.085);
        zoom_levels.put(22,9.542); // Last supported Zoom-level
        zoom_levels.put(23,4.771); 
        zoom_levels.put(24,2.385); 
        zoom_levels.put(25,1.192); 
        zoom_levels.put(26,0.596); 
        zoom_levels.put(27,0.298); 
        zoom_levels.put(28,0.149); 
        zoom_levels.put(29,0.074); 
        zoom_levels.put(30,0.037); // Last possible value that gdalwarp can create
        // -- 89.877466 min[19]	39498.490933 default[14] max[23]
        // Zoom 31. [gdalwarp] Failed to compute GCP transform: Transform is not solvable     
        String s_rl2_min_max_zoom_base=GeneralQueriesPreparer.RL2_MINMAX_ZOOMLEVEL_QUERY.getQuery();
        if (i_srid <= 0)
        { // No Transformation possible, retrieve resolution
         // s_rl2_min_max_zoom_base=GeneralQueriesPreparer.RL2_MINMAX_RESOLUTION_QUERY.getQuery();
         i_srid=Integer.parseInt(LibraryConstants.SRID_BRANDENBURGER_TOR_187999);
         s_rl2_min_max_zoom_base=s_rl2_min_max_zoom_base.replace("ST_Transform(geometry,3395)","ST_Transform(SetSRID(geometry,"+i_srid+"),3395)");
        }
        String s_sql_command = s_rl2_min_max_zoom_base.replace("COVERAGE_NAME", coverageName); 
        s_sql_command = s_sql_command.replace("TILE_WIDTH", s_tile_size);        
        double d_width_min=0.0;
        double d_width_max=0.0;
        Stmt stmt = null;
         try {
                stmt = dbSpatialite.prepare(s_sql_command);
                if (stmt.step()) {
                    d_width_max = stmt.column_double(0);
                    d_width_min = stmt.column_double(1);
                }
            } catch (jsqlite.Exception e_stmt) {
                int i_rc = dbSpatialite.last_error();
                GPLog.error("SPL_Rasterlite", "rl2_calculate_zoom_levels sql[" + s_sql_command + "] rc=" + i_rc + "]", e_stmt);
            } finally {
                if(stmt!=null)
                    try {
                        stmt.close();
                    } catch (jsqlite.Exception e) {
                        GPLog.error("SPL_Rasterlite", "rl2_calculate_zoom_levels sql[" + s_sql_command + "]", e);
                    }
            }

        if (d_width_max == d_width_min)
        { // No Pyramids ??
         d_width_min=d_width_max*8; // pyramind_level_0.x_resolution_1_8=8*pyramind_level_0.x_resolution_1_1
        }
        int i_zoom_min=-1;
        int i_zoom_max=-1;
        int i_zoom_default=-1;
        for (int i=0;i<zoom_levels.size();i++)
         {
          double meters=zoom_levels.get(i);
          if (i_zoom_min < 0)
          {
           if (meters < d_width_min)
            i_zoom_min=i+1;
          }
          if (i_zoom_default < 0)
          {
           if (meters < d_width_max)
            i_zoom_default=i;
          }
         }
         // Sanity checks
         if (i_zoom_min < 0)
          i_zoom_min= 0;
          if (i_zoom_min > i_zoom_default)
          { // switch
           i_zoom_max=i_zoom_default;
           i_zoom_default=i_zoom_min;
           i_zoom_min=i_zoom_max;
          }
         if (i_zoom_default > zoom_levels.size())
          i_zoom_default=((zoom_levels.size()-i_zoom_min)/2);
         i_zoom_max=i_zoom_default+(i_zoom_default-i_zoom_min);
         if (i_zoom_max > zoom_levels.size())
          i_zoom_max = zoom_levels.size();
         // GPLog.androidLog(-1, "rl2_calculate_zoom_levels["+coverageName+"] srid["+i_srid+"] d_width_min["+d_width_min+"] d_width_max["+d_width_max+"] zoom_min["+i_zoom_min+"] zoom_max["+i_zoom_max+"] zoom_default["+i_zoom_default+"]  s_sql_command["+s_sql_command+"]");
         // i_zoom_max = zoom_levels.size();
         // Let the Application reset supported min/max zoom-levels
         int[] zoom_level_min_max={i_zoom_min,i_zoom_max,i_zoom_default};
        return zoom_level_min_max;
    }

}
