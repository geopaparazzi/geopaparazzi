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
package eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MBTilesDroidSpitter;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MBtilesAsync;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MbTilesMetadata;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;
import jsqlite.Exception;

/**
 * An utility class to handle an mbtiles database.
 *
 * author Andrea Antonello (www.hydrologis.com)
 * adapted to create and fill mbtiles databases Mark Johnson (www.mj10777.de)
 */
@SuppressWarnings("nls")
public class MbtilesDatabaseHandler extends AbstractSpatialDatabaseHandler {

    private List<SpatialRasterTable> rasterTableList;
    private MBTilesDroidSpitter mbtilesSplitter;
    private HashMap<String, String> mbtilesMetadata = null;
    /**
     * 
     */
    public HashMap<String, String> async_mbtiles_metadata = null;
    private MBtilesAsync mbtiles_async = null;

    @SuppressWarnings("javadoc")
    public static enum AsyncTasks {
        ASYNC_PARMS, //
        ANALYZE_VACUUM, //
        REQUEST_URL, //
        REQUEST_CREATE, //
        REQUEST_DROP, //
        REQUEST_DELETE, //
        REQUEST_PING, //
        UPDATE_BOUNDS, //
        RESET_METADATA
    }

    /**
     * List of async tasks to be completed.
     */
    public List<MbtilesDatabaseHandler.AsyncTasks> asyncTasksList = new ArrayList<MbtilesDatabaseHandler.AsyncTasks>();

    /** * */
    public String s_request_url_source = "";
    /** * */
    public String s_request_protocol = ""; // 'file' or 'http'
    /** * */
    public String s_request_bounds = "";


    /** * */
    public String s_request_bounds_url = "";
    /** * */
    public String s_request_zoom_levels = "";
    /** * */
    public String s_request_zoom_levels_url = "";
    /** * */
    public String s_request_y_type = "osm"; // 0=osm ; 1=tms ; 2=wms
    /** * */
    public String s_request_type = ""; // 'fill', 'replace'

    /**
      * Constructor.
      *
      * <ul>
      *  <li>if the file does not exist, a valid mbtile database will be created</li>
      *  <li>if the parent directory does not exist, it will be created</li>
      * </ul>
      *
      * @param dbPath full path to mbtiles file to open.
      * @param initMetadata list of initial metadata values to set 
      *                         upon creation (can be <code>null</code>).
     * @throws IOException  if something goes wrong. 
      */
    public MbtilesDatabaseHandler( String dbPath, HashMap<String, String> initMetadata ) throws IOException {
        super(dbPathCheck(dbPath));
        this.mbtilesMetadata = initMetadata;
        mbtilesSplitter = new MBTilesDroidSpitter(databaseFile, mbtilesMetadata);
    }

    /**
     * @mj107777 WHY IS THIS DONE HERE? DOES THIS WORK?
     * 
     * @param dbPath
     * @return
     */
    private static String dbPathCheck( String dbPath ) {
        if (!dbPath.endsWith(SpatialDataType.MBTILES.getExtension())) {
            // .mbtiles files must have an .mbtiles
            // extension, force this
            dbPath = dbPath.substring(0, dbPath.lastIndexOf(".")) + SpatialDataType.MBTILES.getExtension();
        }
        return dbPath;
    }

    public boolean isValid() {
        if (mbtilesSplitter.getmbtiles() == null) { // in case .'open' was forgotten
            open(); // "" : default value will be used '1.1'
        }
        return mbtilesSplitter.isValid();
    }

    /**
      * Called during Construction of Async-Tasks.
      * 
      * <p>- Database connection needed
      * 
      * @return list of Tasks to be completed.
      */
    public List<MbtilesDatabaseHandler.AsyncTasks> getAsyncTasks() {
        if (mbtilesSplitter.getmbtiles() == null) { // in case .'open' was forgotten
            open(); // "" : default value will be used '1.1'
        }
        return asyncTasksList;
    }

    public List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception {
        return Collections.emptyList();
    }

    public List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception {
        if (rasterTableList == null || forceRead) {
            rasterTableList = new ArrayList<SpatialRasterTable>();
            open();
            double[] d_bounds = {this.boundsWest, this.boundsSouth, this.boundsEast, this.boundsNorth};
            SpatialRasterTable table = new SpatialRasterTable(databasePath, databaseFileNameNoExtension, "3857", this.minZoom,
                    this.maxZoom, centerX, centerY, "?,?,?", d_bounds);
            table.setDefaultZoom(defaultZoom);
            // table.setDescription(getDescription());
            table.setMapType(SpatialDataType.MBTILES.getTypeName());
            rasterTableList.add(table);
        }
        return rasterTableList;
    }

    public float[] getTableBounds( AbstractSpatialTable spatialTable ) throws Exception {
        MbTilesMetadata metadata = mbtilesSplitter.getMetadata();
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
        byte[] tileAsBytes = mbtilesSplitter.getTileAsBytes(i_x, i_y_osm, i_z);
        return tileAsBytes;
    }

    /**
      * Function to retrieve Tile Bitmap from the mbtiles Database.
      *
      * <p>i_y_osm must be in is Open-Street-Map 'Slippy Map' notation 
      * [will be converted to 'tms' notation if needed]
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
        if (mbtilesSplitter.getmbtiles() == null) { // in case .'open' was forgotten
            open(); // "" : default value will be used '1.1'
        }
        int[] pixels = new int[i_pixel_size * i_pixel_size];
        byte[] rasterBytes = mbtilesSplitter.getTileAsBytes(i_x, i_y_osm, i_z);
        if (rasterBytes == null) {
            b_rc = false;
            return b_rc;
        }
        Bitmap decodedBitmap = null;
        decodedBitmap = BitmapFactory.decodeByteArray(rasterBytes, 0, rasterBytes.length);
        // check if the input stream could be decoded into a bitmap
        if (decodedBitmap != null) {
            // copy all pixels from the decoded bitmap to the color array
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
      * @param forceUnique if 1, it check if image is unique in Database [may be slow if used]
      * @return 0: correct, otherwise error
      * @throws IOException  if something goes wrong.
      */
    public int insertBitmapTile( int i_x, int i_y_osm, int i_z, Bitmap tile_bitmap, int forceUnique ) throws IOException {
        try {
            return mbtilesSplitter.insertBitmapTile(i_x, i_y_osm, i_z, tile_bitmap, forceUnique);
        } catch (IOException e) {
            return 1;
        }
    }

    public void open() {
        if (mbtilesSplitter.getmbtiles() == null) {
            mbtilesSplitter.open(true, ""); // "" : default value will be used '1.1'
            loadMetadata();
        }
    }

    /**
      * Load and set metadata from mbtiles Database, with all default tasks
      * 
      * <p>- do this in one place to insure that it is allways done in the same way.
      */
    public void loadMetadata() {
        MbTilesMetadata metadata = mbtilesSplitter.getMetadata();
        float[] bounds = metadata.bounds;// left, bottom, right, top
        double[] d_bounds = {bounds[0], bounds[1], bounds[2], bounds[3]};
        float[] center = metadata.center;// center_x,center_y,zoom
        this.databaseFileNameNoExtension = metadata.name;
        // String tableName = metadata.name;
        this.defaultZoom = metadata.maxZoom;
        this.minZoom = metadata.minZoom;
        this.maxZoom = metadata.maxZoom;
        this.boundsWest = d_bounds[0];
        this.boundsSouth = d_bounds[1];
        this.boundsEast = d_bounds[2];
        this.boundsNorth = d_bounds[3];
        if (center != null) {
            this.centerX = center[0];
            this.centerY = center[1];
            this.defaultZoom = (int) center[2];
        } else {
            if (bounds != null) {
                this.centerX = bounds[0] + (bounds[2] - bounds[0]) / 2f;
                this.centerY = bounds[1] + (bounds[3] - bounds[1]) / 2f;
            }
        }
        // setDescription(metadata.description);
    }

    public void close() throws Exception {
        if (mbtiles_async != null) {
            if (mbtiles_async.getStatus() == AsyncTask.Status.RUNNING) {
                mbtiles_async.cancel(true);
            }
        }
        if (mbtilesSplitter != null) {
            mbtilesSplitter.close();
        }
    }

    /**
      * Return list of all zoom-levels and Bounds in LatLong.
      * 
      * <br>- last entry: min/max zoom-levels and Bounds
      * <br>- this is calculated from the Database and will update the metadata-table
      * 
      * @return map of zoom-levels and Bounds in LatLong
      */
    public HashMap<String, String> getBoundsZoomLevels() {
        if (mbtilesSplitter != null) {
            return mbtilesSplitter.getBoundsZoomLevels();
        }
        return new LinkedHashMap<String, String>();
    }

    /**
      * Return center position with zoom-level.
      * 
      * @return Center as [lon, lat, default zoom] 
      */
    public String getCenterParms() {
        if (mbtilesSplitter != null) {
            return mbtilesSplitter.getCenterParms();
        }
        return "";
    }

    /**
      * Update mbtiles Bounds / Zoom (min/max) levels
      * 
      * @param doReloadMetadata if 1 reload values after update [not needed upon creation, update after bounds/center/zoom changes]
      * @return o if reading was ok.
      */
    public int updateBounds( int doReloadMetadata ) {
        if (mbtilesSplitter != null) {
            mbtilesSplitter.fetch_bounds_minmax(doReloadMetadata, 1);
            loadMetadata(); // will read and reset values
            return 0;
        }
        return 1;
    }

    /**
      * General Function to update mbtiles metadata Table.
      * 
      * @param mbtilesMetadata list of key,values to update. [fill this with valued that need to be added/changed]
      * @param doReloadMetadata 1: reload values after update [not needed upon creation, update after bounds/center/zoom changes]
      * @return 0: no error
      * @throws IOException  if something goes wrong.
      */
    public int updateMetadata( HashMap<String, String> mbtilesMetadata, int doReloadMetadata ) throws IOException {
        int i_rc = 1;
        if (mbtilesSplitter != null) {
            try {
                i_rc = mbtilesSplitter.update_mbtiles_metadata(null, mbtilesMetadata, doReloadMetadata);
                if (doReloadMetadata == 1)
                    loadMetadata(); // will read and reset values
                i_rc = 0;
            } catch (IOException e) {
                GPLog.androidLog(4, "MbtilesDatabaseHandler.update_metadata[" + getDatabasePath() + "]", e);
            }
        }
        return i_rc;
    }

    /**
    * Launch async retrieve url.
    *
    * @param mbtiles_request_url a map of url retrival info.
    * @param async_mbtiles_metadata a map of mbtiles metadata.
    */
    public void runRetrieveUrl( HashMap<String, String> mbtiles_request_url, HashMap<String, String> async_mbtiles_metadata ) {
        int i_run_create = 0;
        int i_run_fill = 0;
        int i_run_replace = 0;
        int i_load_url = 0;
        int i_delete = 0;
        int i_drop = 0;
        int i_vacuum = 0;
        int i_update_bounds = 0;
        this.async_mbtiles_metadata = async_mbtiles_metadata;
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

            // GPLog.androidLog(-1, "run_retrieve_url: key[" + s_key + "]  value[" + s_value +
            // "] load[" + i_load_url + "] ");
        }
        // check if the pre-requriment for REQUEST_CREATE are fullfilled
        if ((i_run_fill != 0) || (i_run_replace != 1)) {
            if ((i_run_fill == 1) && (i_run_replace == 1)) {
                i_run_replace = 0;
                s_request_type = "fill";
            }
            if ((!s_request_url_source.equals("")) && (!s_request_bounds.equals("")) && (!s_request_zoom_levels.equals(""))) {
                // run only if set, some cheding might be wise
                i_run_create = 1;
            }
        }
        // The order of adding is important
        if (i_update_bounds > 0) { // will do an extensive check on bounds and zoom-level, updating
            // the mbtiles.metadata table
            asyncTasksList.add(AsyncTasks.UPDATE_BOUNDS);
        }
        if (i_drop > 0) { // this should effectaly delete exiting request and reload again if
            // requested
            asyncTasksList.add(AsyncTasks.REQUEST_DROP);
        }
        if (i_delete > 0) { // planned for future [delete tiles of an area]
            asyncTasksList.add(AsyncTasks.REQUEST_DELETE);
        }
        if (i_vacuum > 0) { // VACUUM should run AFTER any deleting and BEFORE any inserting
            asyncTasksList.add(AsyncTasks.ANALYZE_VACUUM);
        }
        if (i_run_create > 0) { // REQUEST_CREATE
            asyncTasksList.add(AsyncTasks.REQUEST_CREATE);
        }
        if (i_load_url > 0) { // will download requested tiles
            asyncTasksList.add(AsyncTasks.REQUEST_URL);
        }
        if ((this.async_mbtiles_metadata != null) && (this.async_mbtiles_metadata.size() > 0)) {
            asyncTasksList.add(AsyncTasks.RESET_METADATA);
        }
        if (asyncTasksList.size() > 0) {
            mbtiles_async = new MBtilesAsync(this);
            // with .execute(): this crashes
            // mbtiles_async.execute(AsyncTasks.ASYNC_PARMS);

            /*
            * moovida: THIS IS NOT 2.3.3 compatible, which is 10 and < 12
            * if it crashes, we need to fin out why, but we can't use
            * mbtiles_async.executeOnExecutor.
            */
            // if (Build.VERSION.SDK_INT < 12) // use numbers for backwards compatibility
            // Build.VERSION_CODES.HONEYCOMB)
            // { // http://developer.android.com/reference/android/os/Build.VERSION_CODES.html
            // // GPLog.androidLog(-1,"run_retrieve_url.HONEYCOMB.["+Build.VERSION.SDK_INT+"]");
            // mbtiles_async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,AsyncTasks.ASYNC_PARMS);
            // }
            // else
            // {
            // GPLog.androidLog(-1,"run_retrieve_url.OTHER.["+Build.VERSION.SDK_INT+"]");
            mbtiles_async.execute(AsyncTasks.ASYNC_PARMS);
            // }
            // GPLog.androidLog(-1,"run_retrieve_url.Build.VERSION.SDK_INT.["+Build.VERSION.SDK_INT+"]");
            // // 20131125: 15, 2031221: 17
            // mbtiles_async.execute(AsyncTasks.ASYNC_PARMS);
        }
    }

    /**
      * Returns list of collected urlmapped to their tile id. 
      * 
      * @param limit amount of records to retrieve [i_limit < 1 == all]
      * @return  the map of ids, urls.
      */
    public HashMap<String, String> getRequestUrlsMap( int limit ) {
        if (mbtilesSplitter != null) {
            return mbtilesSplitter.retrieve_request_url(limit);
        }
        return new LinkedHashMap<String, String>();
    }

    /**
      * Bulk insert of record in table.
      * 
      * - the request_url table will be created if it does not exist
      * 
      * @param requestUrlsMap the map of urls to request.
      * @return number of opened requests.
      */
    public int bulkInsertFromUrlsTilesInTable( HashMap<String, String> requestUrlsMap ) {
        if (mbtilesSplitter != null) {
            return mbtilesSplitter.insert_list_request_url(requestUrlsMap);
        }
        return -1;
    }

    /**
      * Returns amount of records of table: request_url
      * 
      * <p>parm values:
      * <br>0: return existing value [set when database was opended,not reading the table] [MBTilesDroidSpitter.i_request_url_read_value]
      * <br>1 : return existing value return existing value [reading the table with count after checking if it exits] [MBTilesDroidSpitter.i_request_url_read_db]
      * <br>2: create table (if it does not exist) [MBTilesDroidSpitter.i_request_url_count_create]
      * <br>3: delete table (if it does exist) [MBTilesDroidSpitter.i_request_url_count_drop]
      * 
      * @param parm type of result
      * @return if < 0: table does not exist; 0=exist but is empty ; > 0 open requests
      */
    public int getRequestUrlCount( int parm ) {
        if (mbtilesSplitter != null) {
            return mbtilesSplitter.get_request_url_count(parm);
        }
        return -1;
    }

    /**
      * Delete of record in table: request_url
      * 
      * <p>parm values:  [3 only for internal use] - only 4 supported
      * <br>3: insert record with: s_tile_id and s_tile_url [MBTilesDroidSpitter.i_request_url_count_insert]
      * <br>4: delete record with: s_tile_id, delete table if count is 0 [MBTilesDroidSpitter.i_request_url_count_delete]
      * 
      * @param s_tile_id tile_id to use
      * @return if< 0: table does not exist; 0=exist but is empty ; > 0 open requests
      */
    public int deleteRequestUrl( String s_tile_id ) {
        if (mbtilesSplitter != null) {
            return mbtilesSplitter.insert_request_url(MBTilesDroidSpitter.i_request_url_count_delete, s_tile_id, "");
        }
        return -1;
    }

    /**
      * Retrieves a list of tile id requested, based on bounds and zoom-level.
      * 
      * <p>return values values:
      * <br>tile_id : created with: get_tile_id_from_zxy
      * <br>- will be read and parsed with: get_zxy_from_tile_id in on_request_create_url
      * <br>s_request_type: 'fill': only missing tiles ; 'replace' all tiles ; 'exists' tiles that exist
      * 
      * @param request_bounds bounds of request area
      * @param i_zoom_level zoom level of tiles
      * @param s_request_type request type ['fill','replace','exists']
      * @param s_url_source TODO
      * @param s_request_y_type TODO 
      * @return list of 'tile_id' needed
      */
    public List<String> buildRequestList( double[] request_bounds, int i_zoom_level, String s_request_type, String s_url_source,
            String s_request_y_type ) {
        if (mbtilesSplitter != null) {
            return mbtilesSplitter.build_request_list(request_bounds, i_zoom_level, s_request_type, s_url_source,
                    s_request_y_type);
        }
        return new ArrayList<String>();
    }

    /**
      * House-keeping tasks for Database.
      * 
      * <p>The ANALYZE command gathers statistics about tables and indices
      * <br>The VACUUM command rebuilds the entire database.
      * <br>- A VACUUM will fail if there is an open transaction, or if there are one or more active SQL statements when it is run.
      * 
      * @return 0=correct ; 1=ANALYSE has failed ; 2=VACUUM has failed
      */
    public int on_analyze_vacuum() {
        int i_rc = 0;
        if (mbtilesSplitter != null) {
            return mbtilesSplitter.on_analyze_vacuum();
        }
        return i_rc;
    }
}
