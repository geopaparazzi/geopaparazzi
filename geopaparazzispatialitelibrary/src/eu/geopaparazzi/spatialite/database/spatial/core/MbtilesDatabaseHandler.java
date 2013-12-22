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
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jsqlite.Exception;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MBTilesDroidSpitter;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MbTilesMetadata;

/**
 * An utility class to handle an mbtiles database.
 *
 * author Andrea Antonello (www.hydrologis.com)
 * adapted to create and fill mbtiles databases Mark Johnson (www.mj10777.de)
 */
public class MbtilesDatabaseHandler implements ISpatialDatabaseHandler {
    public final static String TABLE_METADATA = "metadata";
    public final static String COL_METADATA_NAME = "name";
    public final static String COL_METADATA_VALUE = "value";
    private List<SpatialRasterTable> rasterTableList;
    private File file_map; // all DatabaseHandler/Table classes should use these names
    private String s_map_file; // [with path] all DatabaseHandler/Table classes should use these
                               // names
    private String s_name_file; // [without path] all DatabaseHandler/Table classes should use these
                                // names
    private String s_name; // all DatabaseHandler/Table classes should use these names
    private String s_description; // all DatabaseHandler/Table classes should use these names
    private String s_map_type = "mbtiles"; // all DatabaseHandler/Table classes should use these
                                           // names
    private MBTilesDroidSpitter db_mbtiles;
    private HashMap<String, String> mbtiles_metadata = null;
    public HashMap<String, String> async_mbtiles_metadata = null;
    private int minZoom;
    private int maxZoom;
    private double centerX; // wsg84
    private double centerY; // wsg84
    private double bounds_west; // wsg84
    private double bounds_east; // wsg84
    private double bounds_north; // wsg84
    private double bounds_south; // wsg84
    private int defaultZoom;
    // private int i_force_unique = 0;
    private MBtilesAsync mbtiles_async = null;
    // echo "ANALYZE;VACUUM; " >> ${this_dir}.sql;
    public static enum AsyncTasks { // MbtilesDatabaseHandler.AsyncTasks.ASYNC_PARMS
        ASYNC_PARMS, ANALYZE_VACUUM, REQUEST_URL, REQUEST_CREATE,
        REQUEST_DROP, REQUEST_DELETE, REQUEST_PING, UPDATE_BOUNDS,
        RESET_METADATA
    }
    public List<MbtilesDatabaseHandler.AsyncTasks> async_parms = new ArrayList<MbtilesDatabaseHandler.AsyncTasks>();
    public String s_request_url_source = "";
    public String s_request_protocol=""; // 'file' or 'http'
    public String s_request_bounds = "";
    public String s_request_bounds_url = "";
    public String s_request_zoom_levels = "";
    public String s_request_zoom_levels_url = "";
    public String s_request_y_type = "osm"; // 0=osm ; 1=tms ; 2=wms
    public String s_request_type = ""; // 'fill', 'replace'
    // -----------------------------------------------
    /**
      * Constructor MbtilesDatabaseHandler
      *
      * <ul>
      *  <li>if the file does not exist, a valid mbtile database will be created</li>
      *  <li>if the parent directory does not exist, it will be created</li>
      * </ul>
      *
      * @param s_mbtiles_path full path to mbtiles file to open
      * @param mbtiles_metadata list of initial metadata values to set upon creation [otherwise can be null]
      * @return void
      */
    public MbtilesDatabaseHandler( String s_mbtiles_path, HashMap<String, String> mbtiles_metadata ) {
        this.mbtiles_metadata = mbtiles_metadata;
        // GPLog.GLOBAL_LOG_LEVEL=1;
        // GPLog.androidLog(-1,"MbtilesDatabaseHandler[" + s_mbtiles_path + "]");
        if (!s_mbtiles_path.endsWith("." + s_map_type)) { // .mbtiles files must have an .mbtiles
                                                          // extention, force this
            s_mbtiles_path = s_mbtiles_path.substring(0, s_mbtiles_path.lastIndexOf(".")) + "." + s_map_type;
        }
        this.file_map = new File(s_mbtiles_path);
        s_map_file = file_map.getAbsolutePath();
        s_name_file = file_map.getName();
        this.s_name = file_map.getName().substring(0, file_map.getName().lastIndexOf("."));
        db_mbtiles = new MBTilesDroidSpitter(file_map, this.mbtiles_metadata);
        // GPLog.GLOBAL_LOG_LEVEL=0;
        setDescription(s_name);
        // GPLog.androidLog(-1,"MbtilesDatabaseHandler[" + file_map.getAbsolutePath() +
        // "] name["+s_name+"] s_description["+s_description+"]");
    }
    // -----------------------------------------------
    /**
      * Is the mbtiles file considerd valid
      * - metadata table exists and has data
      * - 'tiles' is either a table or a view and the correct fields exist
      * -- if a view: do the tables map and images exist with the correct fields
      * checking is done once when the 'metadata' is retrieved the first time [fetchMetadata()]
      * @return b_mbtiles_valid true if valid, otherwise false
      */
    @Override
    public boolean isValid() {
     if (db_mbtiles.getmbtiles() == null) { // in case .'open' was forgotten
            open(); // "" : default value will be used '1.1'
        }
        return db_mbtiles.isValid();
    }
    // -----------------------------------------------
    /**
      * Called during Construction of Async-Tasks
      * - Database connection needed
      * @return async_parms List of Tasks to be commpleated
      */
    public List<MbtilesDatabaseHandler.AsyncTasks> getAsyncParms() {
        if (db_mbtiles.getmbtiles() == null) { // in case .'open' was forgotten
            open(); // "" : default value will be used '1.1'
        }
        return async_parms;
    }
    public List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception {
        return Collections.emptyList();
    }

    public List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception {
        if (rasterTableList == null || forceRead) {
            rasterTableList = new ArrayList<SpatialRasterTable>();
            open();
            double[] d_bounds = {this.bounds_west,this.bounds_south,this.bounds_east,this.bounds_north};
            SpatialRasterTable table = new SpatialRasterTable(s_map_file, s_name, "3857", this.minZoom, this.maxZoom,
                    centerX, centerY, "?,?,?", d_bounds);
            table.setDefaultZoom(defaultZoom);
            table.setDescription(getDescription());
            table.setMapType(s_map_type);
            // for mbtiles the desired center can be set by the
            // database developer and may be different than the
            // true center/zoom
            rasterTableList.add(table);
        }
        return rasterTableList;
    }

    @Override
    public float[] getTableBounds( SpatialVectorTable spatialTable, String destSrid ) throws Exception {
        MbTilesMetadata metadata = db_mbtiles.getMetadata();
        float[] bounds = metadata.bounds;// left, bottom, right, top
        float w = bounds[0];
        float s = bounds[1];
        float e = bounds[2];
        float n = bounds[3];
        return new float[]{n, s, e, w};
    }

    // -----------------------------------------------
    /**
      * Function to retrieve Tile byte[] from the mbtiles Database [for 'SpatialiteDatabaseHandler']
      *
      * <p>i_y_osm must be in is Open-Street-Map 'Slippy Map'
      * notation [will be converted to 'tms' notation if needed]
      *
      * @param query Format 'z,x,y_osm'
      * @return byte[] of the tile or null if no tile matched the given parameters
      */
    public byte[] getRasterTile( String query ) {
        String[] split = query.split(",");
        if (split.length != 3) {
            return null;
        }
        int i_z = 0;
        int i_x = 0;
        int i_y_osm = 0;
        try {
            i_z = Integer.parseInt(split[0]);
            i_x = Integer.parseInt(split[1]);
            i_y_osm = Integer.parseInt(split[2]);
        } catch (NumberFormatException e) {
            return null;
        }
        byte[] tileAsBytes = db_mbtiles.getTileAsBytes(i_x, i_y_osm, i_z);
        return tileAsBytes;
    }
    // -----------------------------------------------
    /**
      * Function to retrieve Tile Bitmap from the mbtiles Database [for 'CustomTileDownloader']
      *
      * <p>i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will be converted to 'tms' notation if needed]
      *
      * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_pixel_size the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param tile_bitmap retrieve the Bitmap as done in 'CustomTileDownloader'
      * @return Bitmap of the tile or null if no tile matched the given parameters
      */
    public boolean getBitmapTile( int i_x, int i_y_osm, int i_z, int i_pixel_size, Bitmap tile_bitmap ) {
        boolean b_rc = true;
        if (db_mbtiles.getmbtiles() == null) { // in case .'open' was forgotten
            open(); // "" : default value will be used '1.1'
        }
        int[] pixels = new int[i_pixel_size * i_pixel_size];
        byte[] rasterBytes = db_mbtiles.getTileAsBytes(i_x, i_y_osm, i_z);
        if (rasterBytes == null) {
            b_rc = false;
            return b_rc;
        }
        Bitmap decodedBitmap = null;
        decodedBitmap = BitmapFactory.decodeByteArray(rasterBytes, 0, rasterBytes.length);
        // check if the input stream could be decoded into a bitmap
        if (decodedBitmap != null) { // copy all pixels from the decoded bitmap to the color array
            decodedBitmap.getPixels(pixels, 0, i_pixel_size, 0, 0, i_pixel_size, i_pixel_size);
            decodedBitmap.recycle();
        } else {
            b_rc = false;
            return b_rc;
        }
        // copy all pixels from the color array to the tile bitmap
        tile_bitmap.setPixels(pixels, 0, i_pixel_size, 0, 0, i_pixel_size, i_pixel_size);
        return b_rc;
    }
    // -----------------------------------------------
    /**
      * Function to insert a new Tile Bitmap to the mbtiles Database
      *
      * <ul>
      *  <li>i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will
      *      be converted to 'tms' notation if needed]</li>
      *  <li>checking will be done to determine if the Bitmap is blank [i.e.
      *      all pixels have the same RGB]</li>
      * </ul>
      *
      * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param tile_bitmap the Bitmap to extract image-data extracted from. [Will be converted to JPG or PNG depending on metdata setting]
      * @return 0: correct, otherwise error
      */
    public int insertBitmapTile( int i_x, int i_y_osm, int i_z, Bitmap tile_bitmap, int i_force_unique ) throws IOException { // i_rc=
                                                                                                                              // correct,
                                                                                                                              // otherwise
                                                                                                                              // error
        int i_rc = 0;
        try { // i_rc=0: inserted [if needed bounds min/max zoom have been updated]
            i_rc = db_mbtiles.insertBitmapTile(i_x, i_y_osm, i_z, tile_bitmap, i_force_unique);
        } catch (IOException e) {
            i_rc = 1;
            // e.printStackTrace();
        }
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Open mbtiles Database, with all default tasks
      * @return void
      */
    public void open()  {
        if (db_mbtiles.getmbtiles() == null)
        {
         db_mbtiles.open(true, ""); // "" : default value will be used '1.1'
         load_metadata();
        }
    }
    // -----------------------------------------------
    /**
      * Load and set metadata from mbtiles Database, with all default tasks
      * - do this in one place to insure that it is allways done in the same way
      * @return void
      */
    public void load_metadata()
    {
     MbTilesMetadata metadata = db_mbtiles.getMetadata();
     float[] bounds = metadata.bounds;// left, bottom, right, top
     double[] d_bounds = {bounds[0], bounds[1], bounds[2], bounds[3]};
     float[] center = metadata.center;// center_x,center_y,zoom
     this.s_name = metadata.name;
     // String tableName = metadata.name;
     float centerX = 0f;
     float centerY = 0f;
     this.defaultZoom = metadata.maxZoom;
     this.minZoom=metadata.minZoom;
     this.maxZoom=metadata.maxZoom;
     this.bounds_west = d_bounds[0];
     this.bounds_south = d_bounds[1];
     this.bounds_east = d_bounds[2];
     this.bounds_north = d_bounds[3];
     if (center != null)
     {
      this.centerX = center[0];
      this.centerY = center[1];
      this.defaultZoom = (int) center[2];
     }
     else
     {
      if (bounds != null)
      {
       this.centerX = bounds[0] + (bounds[2] - bounds[0]) / 2f;
       this.centerY = bounds[1] + (bounds[3] - bounds[1]) / 2f;
      }
     }
     setDescription(metadata.description);
    }
    // -----------------------------------------------
    /**
      * Close mbtiles Database
      * @return void
      */
    public void close() throws Exception {
        if (mbtiles_async != null) {
            if (mbtiles_async.getStatus() == AsyncTask.Status.RUNNING) {
                mbtiles_async.cancel(true);
            }
        }
        if (db_mbtiles != null) {
            db_mbtiles.close();
        }
    }
    // -----------------------------------------------
    /**
      * Return list of all zoom-levels and Bounds in LatLong
      * - last entry: min/max zoom-levels and Bounds
      * - this is calculated from the Database and will update the metadata-table
      * @return bounds_lat_long list of zoom-levels and Bounds in LatLong
      */
    public HashMap<String, String> getBoundsZoomLevels()
    {
     if (db_mbtiles != null) {
      return db_mbtiles.getBoundsZoomLevels();
     }
     return new LinkedHashMap<String, String>();
    }
    // -----------------------------------------------
    /**
      * Return center position with zoom-level
      * -  entry: from metatable
      * @return Center as LatLong and default zoom-level [13.37771496361961,52.51628011262304,17]
      */
    public String getCenterParms()
    {
     if (db_mbtiles != null) {
      return db_mbtiles.getCenterParms();
     }
     return "";
    }
    // -----------------------------------------------
    /**
      * Update mbtiles Bounds / Zoom (min/max) levels
      * @param i_reload_metadata reload values after update [not needed upon creation, update after bounds/center/zoom changes]
      * @return void
      */
    public int update_bounds( int i_reload_metadata ) {
     int i_rc=1;
        if (db_mbtiles != null) {
            db_mbtiles.fetch_bounds_minmax(i_reload_metadata, 1);
            load_metadata(); // will read and reset values
            i_rc=0;
        }
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * General Function to update mbtiles metadata Table
      * @param mbtiles_metadata list of key,values to update. [fill this with valued that need to be added/changed]
      * @param i_reload_metadata 1: reload values after update [not needed upon creation, update after bounds/center/zoom changes]
      * @return 0: no error
      */
    public int update_metadata( HashMap<String, String> mbtiles_metadata, int i_reload_metadata ) throws IOException
    {
     int i_rc=1;
     if (db_mbtiles != null) {
            try
            {
             i_rc=db_mbtiles.update_mbtiles_metadata(null,mbtiles_metadata,i_reload_metadata);
             if (i_reload_metadata == 1)
              load_metadata(); // will read and reset values
             i_rc=0;
            }
            catch (IOException e)
            {
             GPLog.androidLog(4,"MbtilesDatabaseHandler.update_metadata[" + getFileNamePath()+ "]", e);
            }
        }
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Return long name of map/file
      *
      * <p>default: file name with path and extention
      * <p>mbtiles : will be a '.mbtiles' sqlite-file-name
      * <p>map : will be a mapforge '.map' file-name
      *
      * @return file_map.getAbsolutePath();
      */
    @Override
    public String getFileNamePath() {
        return this.s_map_file; // file_map.getAbsolutePath();
    }
    // -----------------------------------------------
    /**
      * Return short name of map/file
      *
      * <p>default: file name without path but with extention
      *
      * @return file_mapgetName();
      */
    public String getFileName() {
        return this.s_name_file; // file_map.getName();
    }
    // -----------------------------------------------
    /**
      * Return short name of map/file
      *
      * <p>default: file name without path and extention
      * <p>mbtiles : metadata 'name'
      * <p>map : will be value of 'comment', if not null
      *
      * @return s_name as short name of map/file
      */
    public String getName() {
        return this.s_name; // comment or file-name without path and extention
    }
    // -----------------------------------------------
    /**
      * Return String of bounds [wms-format]
      *
      * <p>x_min,y_min,x_max,y_max
      *
      * @return bounds formatted using wms format
      */
    public String getBounds_toString() {
        return bounds_west + "," + bounds_south + "," + bounds_east + "," + bounds_north;
    }
    // -----------------------------------------------
    /**
      * Return String of Map-Center with default Zoom
      *
      * <p>x_position,y_position,default_zoom
      *
      * @return center formatted using mbtiles format
      */
    public String getCenter_toString() {
        return centerX + "," + centerY + "," + defaultZoom;
    }
    // -----------------------------------------------
    /**
      * Return long description of map/file
      *
      * <p>default: s_name with bounds and center
      * <p>mbtiles : metadata description'
      * <p>map : will be value of 'comment', if not null
      *
      * @return s_description long description of map/file
      */
    public String getDescription() {
        if ((this.s_description == null) || (this.s_description.length() == 0) || (this.s_description.equals(this.s_name)))
            setDescription(this.s_name); // will set default values with bounds and center if it is
                                         // the same as 's_name' or empty
        return this.s_description; // long comment
    }
    // -----------------------------------------------
    /**
      * Set long description of map/file
      *
      * <p>default: s_name with bounds and center
      * <p>mbtiles : metadata description'
      * <p>map : will be value of 'comment', if not null
      *
      * @return s_description long description of map/file
      */
    public void setDescription( String s_description ) {
        if ((s_description == null) || (s_description.length() == 0) || (s_description.equals(this.s_name))) {
            this.s_description = s_name + " bounds[" + getBounds_toString() + "] center[" + getCenter_toString() + "]";
        } else
            this.s_description = s_description;
    }
    // -----------------------------------------------
    /**
      * Return map-file as 'File'
      *
      * <p>if the class does not fail, this file exists
      * <p>mbtiles : will be a '.mbtiles' sqlite-file
      * <p>map : will be a mapforge '.map' file
      *
      * @return file_map as File
      */
    public File getFile() {
        return this.file_map;
    }
    // -----------------------------------------------
    /**
      * Return Min Zoom
      *
      * <p>default :  0
      * <p>mbtiles : taken from value of metadata 'minzoom'
      * <p>map : value is given in 'StartZoomLevel'
      *
      * @return integer minzoom
      */
    public int getMinZoom() {
        return minZoom;
    }
    // -----------------------------------------------
    /**
      * Return Max Zoom
      *
      * <p>default :  22
      * <p>mbtiles : taken from value of metadata 'maxzoom'
      * <p>map : value not defined, seems to calculate bitmap from vector data [18]
      *
      * @return integer maxzoom
      */
    public int getMaxZoom() {
        return maxZoom;
    }
    // -----------------------------------------------
    /**
      * Return Min/Max Zoom as string
      *
      * <p>default :  1-22
      * <p>mbtiles : taken from value of metadata 'min/maxzoom'
      *
      * @return String min/maxzoom
      */
    public String getZoom_Levels() {
        return getMinZoom() + "-" + getMaxZoom();
    }
    // -----------------------------------------------
    /**
      * Return West X Value [Longitude]
      *
      * <p>default :  -180.0 [if not otherwise set]
      * <p>mbtiles : taken from 1st value of metadata 'bounds'
      *
      * @return double of West X Value [Longitude]
      */
    public double getMinLongitude() {
        return bounds_west;
    }
    // -----------------------------------------------
    /**
      * Return South Y Value [Latitude]
      *
      * <p>default :  -85.05113 [if not otherwise set]
      * <p>mbtiles : taken from 2nd value of metadata 'bounds'
      *
      * @return double of South Y Value [Latitude]
      */
    public double getMinLatitude() {
        return bounds_south;
    }
    // -----------------------------------------------
    /**
      * Return East X Value [Longitude]
      *
      * <p>default :  180.0 [if not otherwise set]
      * <p>mbtiles : taken from 3th value of metadata 'bounds'
      *
      * @return double of East X Value [Longitude]
      */
    public double getMaxLongitude() {
        return bounds_east;
    }
    // -----------------------------------------------
    /**
      * Return North Y Value [Latitude]
      *
      * <p>default :  85.05113 [if not otherwise set]
      * <p>mbtiles : taken from 4th value of metadata 'bounds'
      *
      * @return double of North Y Value [Latitude]
      */
    public double getMaxLatitude() {
        return bounds_north;
    }
    // -----------------------------------------------
    /**
      * Return Center X Value [Longitude]
      *
      * <p>default : center of bounds
      * <p>mbtiles : taken from 1st value of metadata 'center'
      *
      * @return double of X Value [Longitude]
      */
    public double getCenterX() {
        return centerX;
    }
    // -----------------------------------------------
    /**
      * Return Center Y Value [Latitude]
      *
      * <p>default : center of bounds
      * <p>mbtiles : taken from 2nd value of metadata 'center'
      *
      * @return double of Y Value [Latitude]
      */
    public double getCenterY() {
        return centerY;
    }
    // -----------------------------------------------
    /**
      * Retrieve Zoom level
      *
      * <p>default : minZoom
      * <p>mbtiles : taken from 3rd value of metadata 'center'
      *
     * @return defaultZoom
      */
    public int getDefaultZoom() {
        return defaultZoom;
    }
    // -----------------------------------------------
    /**
      * Set default Zoom level
      *
      * <p>default : minZoom
      * <p>mbtiles : taken from 3rd value of metadata 'center'
      *
      * @param i_zoom desired Zoom level
      */
    public void setDefaultZoom( int i_zoom ) {
        defaultZoom = i_zoom;
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
    public void run_retrieve_url( HashMap<String, String> mbtiles_request_url,HashMap<String, String> async_mbtiles_metadata ) {
        int i_run_create = 0;
        int i_run_fill = 0;
        int i_run_replace = 0;
        int i_load_url = 0;
        int i_delete = 0;
        int i_drop = 0;
        int i_vacuum = 0;
        int i_update_bounds = 0;
        this.async_mbtiles_metadata=async_mbtiles_metadata;
        for( Map.Entry<String, String> request_url : mbtiles_request_url.entrySet() ) {
            String s_key = request_url.getKey();
            String s_value = request_url.getValue();
            if (s_key.equals("request_type")) {
                if (s_value.indexOf("fill") != -1) { // will request missing tiles only
                    i_run_fill = 1;
                    s_request_type = "fill";
                }
                if (s_value.indexOf("replace") != -1) { // will replace existing tiles
                    i_run_replace = 1;
                    s_request_type = "replace";
                }
                if (s_value.indexOf("load") != -1) { // will replace existing tiles
                    i_load_url = 1;
                }
                if (s_value.indexOf("drop") != -1) { // will delete the requested tiles, retaining
                                                     // the allready downloaded tiles
                    i_drop = 1;
                }
                if (s_value.indexOf("vacuum") != -1) { // will delete the requested tiles, retaining
                                                       // the allready downloaded tiles
                    i_vacuum = 1;
                }
                if (s_value.indexOf("update_bounds") != -1) { // will do an extensive check on
                                                              // bounds and zoom-level, updating the
                                                              // mbtiles.metadata table
                    i_update_bounds = 1;
                }
                if (s_value.indexOf("delete") != -1) { // planned for future
                    i_delete = 1;
                }
            }
            if (s_key.equals("request_url")) {
                s_request_url_source = s_value;
            }
            if (s_key.equals("request_bounds")) {
                s_request_bounds = s_value;
            }
            if (s_key.equals("request_bounds_url")) {
                s_request_bounds_url = s_value;
             }
            if (s_key.equals("request_zoom_levels")) {
                s_request_zoom_levels = s_value;
            }
            if (s_key.equals("request_zoom_levels_url")) {
              // reserved for future
                s_request_zoom_levels_url = s_value;
            }
            if (s_key.equals("request_y_type")) {
             // reserved for future
                s_request_y_type = s_value;
            }
            if (s_key.equals("request_protocol")) {
             // 'file' or 'http'
                s_request_protocol = s_value;
            }

            //GPLog.androidLog(-1, "run_retrieve_url: key[" + s_key + "]  value[" + s_value + "] load[" + i_load_url + "] ");
        }
        // check if the pre-requriment for REQUEST_CREATE are fullfilled
        if ((i_run_fill != 0) || (i_run_replace != 1))
        {
         if ((i_run_fill == 1) && (i_run_replace == 1))
         {
          i_run_replace=0;
          s_request_type = "fill";
         }
         if ((!s_request_url_source.equals("")) && (!s_request_bounds.equals("")) &&
              (!s_request_zoom_levels.equals("")))
         { // run only if set, some cheding might be wise
          i_run_create=1;
         }
        }
        // The order of adding is important
        if (i_update_bounds > 0) { // will do an extensive check on bounds and zoom-level, updating
                                   // the mbtiles.metadata table
            async_parms.add(AsyncTasks.UPDATE_BOUNDS);
        }
        if (i_drop > 0) { // this should effectaly delete exiting request and reload again if
                          // requested
            async_parms.add(AsyncTasks.REQUEST_DROP);
        }
        if (i_delete > 0) { // planned for future [delete tiles of an area]
            async_parms.add(AsyncTasks.REQUEST_DELETE);
        }
        if (i_vacuum > 0) { // VACUUM should run AFTER any deleting and BEFORE any inserting
            async_parms.add(AsyncTasks.ANALYZE_VACUUM);
        }
        if (i_run_create > 0) { // REQUEST_CREATE
            async_parms.add(AsyncTasks.REQUEST_CREATE);
        }
        if (i_load_url > 0) { // will download requested tiles
            async_parms.add(AsyncTasks.REQUEST_URL);
        }
        if ((this.async_mbtiles_metadata != null) && (this.async_mbtiles_metadata.size() > 0))
        {
         async_parms.add(AsyncTasks.RESET_METADATA);
        }
        if (async_parms.size() > 0) {
            mbtiles_async = new MBtilesAsync(this);
            // with .execute(): this crashes
            // mbtiles_async.execute(AsyncTasks.ASYNC_PARMS);
                  if (Build.VERSION.SDK_INT < 12) // use numbers for backwards compatibility Build.VERSION_CODES.HONEYCOMB)
                  { // http://developer.android.com/reference/android/os/Build.VERSION_CODES.html
                   // GPLog.androidLog(-1,"run_retrieve_url.HONEYCOMB.["+Build.VERSION.SDK_INT+"]");
                      mbtiles_async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,AsyncTasks.ASYNC_PARMS);
                  }
                  else
                  {
                   // GPLog.androidLog(-1,"run_retrieve_url.OTHER.["+Build.VERSION.SDK_INT+"]");
                   mbtiles_async.execute(AsyncTasks.ASYNC_PARMS);
                  }
             // GPLog.androidLog(-1,"run_retrieve_url.Build.VERSION.SDK_INT.["+Build.VERSION.SDK_INT+"]"); // 20131125: 15, 2031221: 17
           // mbtiles_async.execute(AsyncTasks.ASYNC_PARMS);
        }
    }
    // -----------------------------------------------
    /**
      * Returns list of collected 'request_url'
      * @param i_limit amount of records to retrieve [i_limit < 1 == all]
      * @return HashMap<String,String> mbtiles_request_url [tile_id,tile_url]
      */
    public HashMap<String, String> retrieve_request_url(int i_limit) {
        if (db_mbtiles != null) {
            return db_mbtiles.retrieve_request_url(i_limit);
        }
        return new LinkedHashMap<String, String>();
    }
    // -----------------------------------------------
    /**
      * bulk insert of record in table: request_url
      * - the request_url table will be created if it does not exist
      * @return i_request_url_count [< 0: table does not exist; 0=exist but is empty ; > 0 open requests]
      */
    public int insert_list_request_url( HashMap<String, String> mbtiles_request_url ) {
        if (db_mbtiles != null) {
            return db_mbtiles.insert_list_request_url(mbtiles_request_url);
        }
        return -1;
    }
    // -----------------------------------------------
    /**
      * Returns amount of records of table: request_url
      * parm values:
      * 0: return existing value [set when database was opended,not reading the table] [MBTilesDroidSpitter.i_request_url_read_value]
      * 1 : return existing value return existing value [reading the table with count after checking if it exits] [MBTilesDroidSpitter.i_request_url_read_db]
      * 2: create table (if it does not exist) [MBTilesDroidSpitter.i_request_url_count_create]
      * 3: delete table (if it does exist) [MBTilesDroidSpitter.i_request_url_count_drop]
      * @param i_parm type of result
      * @return i_request_url_count [< 0: table does not exist; 0=exist but is empty ; > 0 open requests]
      */
    public int get_request_url_count( int i_parm ) {
        if (db_mbtiles != null) {
            return db_mbtiles.get_request_url_count(i_parm);
        }
        return -1;
    }
    // -----------------------------------------------
    /**
      * delete of record in table: request_url
      * parm values:  [3 only for internal use] - only 4 supported
      * 3: insert record with: s_tile_id and s_tile_url [MBTilesDroidSpitter.i_request_url_count_insert]
      * 4: delete record with: s_tile_id, delete table if count is 0 [MBTilesDroidSpitter.i_request_url_count_delete]
      * @param i_parm type of command
      * @param s_tile_id tile_id to use
      * @param s_tile_url full url to retrieve tile with
      * @return i_request_url_count [< 0: table does not exist; 0=exist but is empty ; > 0 open requests]
      */
    public int delete_request_url( String s_tile_id ) {
        if (db_mbtiles != null) {
            return db_mbtiles.insert_request_url(MBTilesDroidSpitter.i_request_url_count_delete, s_tile_id, "");
        }
        return -1;
    }
    // -----------------------------------------------
    /**
      * Retrieves a list of tile id requesed, based on bounds and zoom-level
      * return values values:
      * tile_id : created with: get_tile_id_from_zxy
      * - will be read and parsed with: get_zxy_from_tile_id in on_request_create_url
      * s_request_type: 'fill': only missing tiles ; 'replace' all tiles ; 'exists' tiles that exist
      * @param request_bounds bounds of request area
      * @param i_zoom_level zoom level of tiles
      * @param s_request_type request type ['fill','replace','exists']
      * @return ist_tile_id [list of 'tile_id' needed]
      */
    public List<String> build_request_list( double[] request_bounds, int i_zoom_level, String s_request_type,String s_url_source, String s_request_y_type ) {
        if (db_mbtiles != null) {
            return db_mbtiles.build_request_list(request_bounds, i_zoom_level, s_request_type,s_url_source,s_request_y_type);
        }
        return new ArrayList<String>();
    }
    // -----------------------------------------------
    /**
      * House-keeping tasks for Database
      * The ANALYZE command gathers statistics about tables and indices
      * The VACUUM command rebuilds the entire database.
      * - A VACUUM will fail if there is an open transaction, or if there are one or more active SQL statements when it is run.
      * @return 0=correct ; 1=ANALYSE has failed ; 2=VACUUM has failed
      */
    public int on_analyze_vacuum() {
        int i_rc = 0;
        if (db_mbtiles != null) {
            return db_mbtiles.on_analyze_vacuum();
        }
        return i_rc;
    }
}
