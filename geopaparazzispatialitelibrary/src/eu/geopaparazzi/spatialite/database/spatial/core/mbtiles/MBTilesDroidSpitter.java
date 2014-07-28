/**
 * @author Simon Thépot aka djcoin <simon.thepot@gmail.com, simon.thepot@makina-corpus.com>
  * adapted to create and fill mbtiles databases Mark Johnson (www.mj10777.de)
 */
package eu.geopaparazzi.spatialite.database.spatial.core.mbtiles;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MbTilesMetadata.MetadataParseException;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MbTilesMetadata.MetadataValidator;

public class MBTilesDroidSpitter {
    private SQLiteDatabase db_mbtiles = null;
    private File file_mbtiles;
    String s_mbtiles_file;
    String s_name;
    private MbTilesMetadata metadata = null;
    private String s_metadataVersion = "1.1";
    private String s_tile_row_type = "tms";
    private String s_center_parm = "";
    private int i_type_tiles = -1; // mbtiles is only valid if 'i_type_tiles' == 0 or 1 [table or
                                   // view]
    private boolean b_grid_id = false;
    private int i_request_url_count = -1; // > 0 table 'request_url' exists
    public static final int i_request_url_count_read_value = 0;
    public static final int i_request_url_count_read_db = 1;
    public static final int i_request_url_count_create = 2;
    public static final int i_request_url_count_drop = 3;
    public static final int i_request_url_count_insert = 4;
    public static final int i_request_url_count_delete = 5;
    private boolean b_mbtiles_valid = false;
    private HashMap<String, String> mbtiles_metadata = null;
    HashMap<String, String> bounds_lat_long = null;
    // avoid SpatialiteLockException's - multiple read/writes will be queued
    private ReentrantReadWriteLock db_lock = new ReentrantReadWriteLock();
    // -----------------------------------------------
    /**
      * Constructor MBTilesDroidSpitter
      *
      * <ul>
      * <i>if the file does not exist, a valid mbtile database will be created</i>
      * <i>if the parent directory does not exist, it will be created</i>
      * </ul>
      *
      * @param file_mbtiles mbtiles.db file to open
      * @param mbtiles_metadata list of initial metadata values to set upon creation [otherwise can be null]
      */
    public MBTilesDroidSpitter( File file_mbtiles, HashMap<String, String> mbtiles_metadata ) {
        this.file_mbtiles = file_mbtiles;
        if (mbtiles_metadata == null)
            this.mbtiles_metadata = new LinkedHashMap<String, String>();
        else
            this.mbtiles_metadata = mbtiles_metadata;
        // GPLog.androidLog(-1,"MBTilesDroidSpitter[" + file_mbtiles.getAbsolutePath() + "]");
        if (!this.file_mbtiles.exists()) { // if the parent directory does not exist, it will be
                                           // created
                                           // - a mbtiles database will be created with default
                                           // values and closed
            try {
                if (!this.file_mbtiles.getName().endsWith(".mbtiles")) { // .mbtiles files must have
                                                                         // an .mbtiles extention,
                                                                         // force this
                    String s_mbtiles_path = file_mbtiles.getParentFile().getAbsolutePath();
                    s_name = this.file_mbtiles.getName().substring(0, this.file_mbtiles.getName().lastIndexOf("."));
                    this.file_mbtiles = new File(s_mbtiles_path + "/" + s_name + ".mbtiles");
                }
                create_mbtiles(this.file_mbtiles);
            } catch (IOException e) {
                GPLog.androidLog(4, "MBTilesDroidSpitter[" + file_mbtiles.getAbsolutePath() + "]", e);
            }
        }
        this.s_name = this.file_mbtiles.getName().substring(0, this.file_mbtiles.getName().lastIndexOf("."));
        this.s_mbtiles_file = file_mbtiles.getAbsolutePath();
    }
    // -----------------------------------------------
    /**
      * Open mbtiles Database
      * @param fetchMetadata 1: fetch and load the mbtiles metaadata
      * @return void
      */
    public void open( boolean fetchMetadata, String metadataVersion ) {
        if (metadataVersion != "")
            this.s_metadataVersion = metadataVersion;
        db_mbtiles = SQLiteDatabase.openOrCreateDatabase(file_mbtiles, null);
        if (!fetchMetadata)
            return;
        try {
            fetchMetadata(this.s_metadataVersion);
        } catch (MetadataParseException e) {
            GPLog.androidLog(4, "MBTilesDroidSpitter[" + file_mbtiles.getAbsolutePath() + "]", e);
        }
        if (!isValid()) { // this mbtiles file is invalid
        }
    }
    // -----------------------------------------------
    /**
      * Close mbtiles Database
      * @return void
      */
    public void close() {
        db_mbtiles.close();
    }
    // -----------------------------------------------
    /**
      * Retrieve SQLiteDatabase connection
      * @return SQLiteDatabase connection of mbtiles.db
      */
    public SQLiteDatabase getmbtiles() {
        return db_mbtiles;
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
    public String getFileNamePath() {
        return this.s_mbtiles_file; // file_map.getAbsolutePath();
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
      * Return list of all zoom-levels and Bounds in LatLong
      * - last entry: min/max zoom-levels and Bounds
      * - this is calculated from the Database and will update the metadata-table
      * @return bounds_lat_long list of zoom-levels and Bounds in LatLong
      */
    public HashMap<String, String> getBoundsZoomLevels() {
        if (bounds_lat_long == null) {
            int i_reload_metadata = 1;
            fetch_bounds_minmax(i_reload_metadata, 1);
        }
        return bounds_lat_long;
    }
    // -----------------------------------------------
    /**
      * Return center position with zoom-level
      * -  entry: from metatable
      * @return Center as LatLong and default zoom-level [13.37771496361961,52.51628011262304,17]
      */
    public String getCenterParms() {
        return this.s_center_parm;
    }
    // -----------------------------------------------
    /**
      * Function to retrieve Tile Drawable Bitmap from the mbtiles Database
      * - i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will be converted to 'tms' notation if needed]
      * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @return Drawable Bitmap of the tile or null if no tile matched the given parameters
      */
    public Drawable getTileAsDrawable( int i_x, int i_y_osm, int i_z ) {
        return new BitmapDrawable(getTileAsBitmap(i_x, i_y_osm, i_z));
    }
    // -----------------------------------------------
    /**
      * Function to retrieve Tile Bitmap from the mbtiles Database
      * - i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will be converted to 'tms' notation if needed]
      * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @return Bitmap of the tile or null if no tile matched the given parameters
      */
    public Bitmap getTileAsBitmap( int i_x, int i_y_osm, int i_z ) {
        // TODO: Optimize this if we have mbtiles_metadata with bound or min/max zoomlevels
        // Do not make any request and return null if we know it won't match any tile
        byte[] bb = getTileAsBytes(i_x, i_y_osm, i_z);
        return BitmapFactory.decodeByteArray(bb, 0, bb.length);
    }
    // -----------------------------------------------
    /**
      * Function to retrieve Tile byte[] from the mbtiles Database
      * - i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will be converted to 'tms' notation if needed]
      * - first of two function that use the 'tms' numbering for the y/tile_row value
      * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @return byte[] of the tile to be used to create a Bitmap or null if no tile matched the given parameters
      */
    public byte[] getTileAsBytes( int i_x, int i_y_osm, int i_z ) {
        int i_y = i_y_osm;
        String s_x = "";
        String s_y = "";
        String s_z = "";
        if (s_tile_row_type.equals("tms")) {
            int[] tmsTileXY = MBTilesDroidSpitter.googleTile2TmsTile(i_x, i_y_osm, i_z);
            i_y = tmsTileXY[1];
        }
        try {
            s_x = Integer.toString(i_x);
            s_y = Integer.toString(i_y);
            s_z = Integer.toString(i_z);
        } catch (NumberFormatException e) {
            return null;
        }
        // db_lock.readLock().lock();
        byte[] blob_data = null;
        try {
            final Cursor c = db_mbtiles.rawQuery(
                    "select tile_data from tiles where tile_column=? and tile_row=? and zoom_level=?",
                    new String[]{s_x, s_y, s_z});
            if (!c.moveToFirst()) {
                c.close();
                // db_lock.readLock().unlock();
                return null;
            }
            blob_data = c.getBlob(c.getColumnIndex("tile_data"));
            c.close();
        } catch (Exception e) {
        } finally { // causes crash
                    // db_lock.readLock().unlock();
        }
        return blob_data;
    }
    // -----------------------------------------------
    /**
      * Function to insert a new Tile Bitmap to the mbtiles Database
      * - i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will be converted to 'tms' notation if needed]
      * - checking will be done to determin if the Bitmap is blank [i.e. all pixels have the same RGB]
      * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param tile_bitmap the Bitmap to extract image-data extracted from. [Will be converted to JPG or PNG depending on metdata setting]
      * @param i_force_unique 1=check if image is unique in Database [may be slow if used]
      * @return 0: correct, otherwise error
      * @throws IOException  if something goes wrong.
      */
    public int insertBitmapTile( int i_x, int i_y_osm, int i_z, Bitmap tile_bitmap, int i_force_unique ) throws IOException {
        int i_rc = 0;
        // i_parm=1: 'ff-ee-dd.rgb' [to be used as tile_id], blank if image is not Blank (all pixels
        // use one RGB value)
        String s_tile_id = get_pixel_rgb_toString(tile_bitmap, 1);
        ByteArrayOutputStream ba_stream = new ByteArrayOutputStream();
        try {
            if (this.mbtiles_metadata.get("format") == "png") { // 'png' should be avoided, can
                                                                // create very big databases
                tile_bitmap.compress(Bitmap.CompressFormat.PNG, 100, ba_stream);
            } else { // 'jpg' should be used where possible
                tile_bitmap.compress(Bitmap.CompressFormat.JPEG, 75, ba_stream);
            }
            byte[] ba_tile_data = ba_stream.toByteArray();
            i_rc = insertTile(s_tile_id, i_x, i_y_osm, i_z, ba_tile_data, i_force_unique);
        } catch (Exception e) {
            i_rc = 1;
            GPLog.androidLog(4, "MBTilesDroidSpitter[" + file_mbtiles.getAbsolutePath() + "]", e);
        }
        // GPLog.androidLog(-1,"MBTilesDroidSpitter.insertBitmapTile: inserting["+i_z+"/"+i_x+"/"+i_y_osm+"] rc=["+i_rc+"]");
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Function to insert a new Tile byte-data to the mbtiles Database
      * - i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will be converted to 'tms' notation if needed]
      * - second of two function that use the 'tms' numbering for the y/tile_row value
      * - blank [i.e. all pixels have the same RGB] image will only be saved once in the'images' table and reference in the 'map' table
      * @param tile_id for images/map tables tile_id field [only filled when Bitmap is blank [all pixels haves ame rgb]
      * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param ba_tile_data the image-data extracted from the Bitmap.
      * @param i_force_unique 1=check if image is unique in Database [may be slow if used]
      * @return 0: no error
      */
    private int insertTile( String s_tile_id, int i_x, int i_y_osm, int i_z, byte[] ba_tile_data, int i_force_unique )
            throws IOException { // i_rc=0: correct, otherwise error
        int i_rc = 0;
        if (!isValid()) { // this mbtiles file is invalid
            return 100; // invalid mbtiles
        }
        boolean b_unique = true;
        if (s_tile_id == "") {
            s_tile_id = get_tile_id_from_zxy(i_z, i_x, i_y_osm);
        } else { // This should be a 'Blank' Image :'ff-ee-dd.rgb', check if allready stored in
                 // 'images' table
            b_unique = search_blank_image(s_tile_id);
        }
        int i_y = i_y_osm;
        if (s_tile_row_type.equals("tms")) {
            int[] tmsTileXY = MBTilesDroidSpitter.googleTile2TmsTile(i_x, i_y_osm, i_z);
            i_y = tmsTileXY[1];
        }
        if (i_force_unique > 1)
            i_force_unique = 0;
        String s_images_tablename = "images";
        String s_map_tablename = "map";
        String s_tiles_tablename = "tiles";
        String s_mbtiles_field_tile_id = "tile_id";
        String s_mbtiles_field_grid_id = "grid_id";
        String s_mbtiles_field_tile_data = "tile_data";
        String s_mbtiles_field_zoom_level = "zoom_level";
        String s_mbtiles_field_tile_column = "tile_column";
        String s_mbtiles_field_tile_row = "tile_row";
        String s_grid_id = "";
        // The use of 'i_force_unique == 1' will probely slow things down to a craw
        // GPLog.androidLog(1,"insertTile  tile_id["+s_tile_id+"] force_unique["+i_force_unique+"] unique["+b_unique+"]");
        if ((i_force_unique == 1) && (b_unique)) {
            // mj10777: not yet properly tested:
            // - query the images table, searching for
            // 'ba_tile_data'
            // -- if found:
            // --- set 'b_unique=false;'
            // --- replace 's_tile_id' with images.tile_id of
            // found record
            String s_tile_id_query = "";
            try {
                s_tile_id_query = search_tile_image(ba_tile_data);
            } catch (Exception e) {
                i_rc = 1;
            }
            if (s_tile_id_query != "") { // We have this image, do not add again
                b_unique = false;
                // replace the present tile_id with the found referenced tile_id
                // the 'map' table will now reference the existing image in 'images'
                s_tile_id = s_tile_id_query;
            }
        }
        // The Database may have been closed in the meantime, we just don't know that yet - should
        // bail out gracefully
        // - avoid 'IllegalStateException' '(conn# x): already closed'
        if (db_mbtiles.isOpen()) { // You cannot lock the Database if the connection is not open
            db_lock.writeLock().lock();
            db_mbtiles.beginTransaction();
            try {
                if (b_unique) { // We do not have this image, add it
                    if (i_type_tiles == 1) {
                        ContentValues image_values = new ContentValues();
                        image_values.put(s_mbtiles_field_tile_data, ba_tile_data);
                        image_values.put(s_mbtiles_field_tile_id, s_tile_id);
                        db_mbtiles.insertOrThrow(s_images_tablename, null, image_values);
                    }
                }
                if (i_type_tiles == 1) { // 'tiles' is a view
                                         // Note: the 'map' table will/should only reference an
                                         // existing image in the 'images'.
                                         // table
                                         // - it is possible that there is more than one reference
                                         // to an existing image
                                         // -- sample: an area has 15 tiles of one color (all pixels
                                         // of the tile have the same
                                         // RGB)
                                         // --- this image will be stored 1 time in 'images', but
                                         // will be used 15 times in 'map'
                    ContentValues map_values = new ContentValues();
                    map_values.put(s_mbtiles_field_zoom_level, i_z);
                    map_values.put(s_mbtiles_field_tile_column, i_x);
                    map_values.put(s_mbtiles_field_tile_row, i_y);
                    map_values.put(s_mbtiles_field_tile_id, s_tile_id);
                    if (b_grid_id)
                        map_values.put(s_mbtiles_field_grid_id, s_grid_id);
                    db_mbtiles.insertOrThrow(s_map_tablename, null, map_values);
                }
                if (i_type_tiles == 0) { // 'tiles' is a table
                    ContentValues tiles_values = new ContentValues();
                    tiles_values.put(s_mbtiles_field_zoom_level, i_z);
                    tiles_values.put(s_mbtiles_field_tile_column, i_x);
                    tiles_values.put(s_mbtiles_field_tile_row, i_y);
                    tiles_values.put(s_mbtiles_field_tile_data, ba_tile_data);
                    db_mbtiles.insertOrThrow(s_tiles_tablename, null, tiles_values);
                }
                db_mbtiles.setTransactionSuccessful();
            } catch (Exception e) {
                int i_catch_rc = 0;
                if (e.getMessage() != null) {
                    String s_message = e.getMessage();
                    if (s_message.indexOf("code 19") != -1) { // When the tile
                                                              // allready exists:
                                                              // not to be
                                                              // considered an
                                                              // error
                        i_rc = 0; // this will delete the request_url entry
                        i_catch_rc = 19;
                    }
                }
                if (i_catch_rc == 0) {
                    throw new IOException("MBTilesDroidSpitter:insertTile error[" + e.getLocalizedMessage() + "] rc=" + i_rc);
                }
            } finally {
                db_mbtiles.endTransaction();
                db_lock.writeLock().unlock();
                int i_update = 1;
                try { // if the bounds or min/max zoom have changed, update changed values and
                      // reload
                      // metadata
                    i_update = checkBounds(i_x, i_y_osm, i_z, i_update);
                    // i_update=0: inside bounds ; othewise bounds and metadata have changed - not
                    // used
                } catch (Exception e) {
                    i_rc = 1;
                }
            }
        } else { // '(conn# x): already closed'
            i_rc = 1;
            return i_rc;
        }

        // GPLog.androidLog(-1,"MBTilesDroidSpitter.insertTile: inserted["+i_z+"/"+i_x+"/"+i_y_osm+"] rc=["+i_rc+"]");
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Function to check if image is blank
      * - avoids duplicate images
      * @param s_tile_id the image tile_id
      * @return true if unique or false if tile_id was found
      */
    private boolean search_blank_image( String s_tile_id ) throws IOException {
        boolean b_unique = true;
        if (i_type_tiles != 1) { // there will be no 'images' table, return
            return b_unique;
        }
        String s_sql_query = "SELECT count(tile_id) AS count_tile_id FROM images WHERE (tile_id = '" + s_tile_id + "')";
        // mj10777: A good wms-server to test 'blank-images' (i.e. all pixels of image have the
        // same RGB) is:
        // http://fbinter.stadt-berlin.de/fb/wms/senstadt/ortsteil?LAYERS=0&FORMAT=image/jpeg&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=visual&SRS=EPSG:4326&BBOX=XXX,YYY,XXX,YYY&WIDTH=256&HEIGHT=256
        // SELECT count(tile_id) FROM images where tile_id like '%.rgb' : 8
        // SELECT count(tile_id) FROM map where tile_id like '%.rgb' : 177
        db_lock.readLock().lock();
        try {
            final Cursor c = db_mbtiles.rawQuery(s_sql_query, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    int i_count_tile_id = c.getInt(c.getColumnIndex("count_tile_id"));
                    if (i_count_tile_id > 0) { // We have this image, do not add again
                        b_unique = false;
                    }
                }
                c.close();
            }
        } catch (Exception e) {
            String s_message = e.getMessage();
            int index_of_text = s_message.indexOf("closed");
            if (index_of_text != -1) {
                // attempt to re-open an already-closed [sometimes: 'already closed']
                return false;
            } else {
                throw new IOException("MBTilesDroidSpitter:search_blank_image query[" + s_sql_query + "] error["
                        + e.getLocalizedMessage() + "] ");
            }
        } finally {
            db_lock.readLock().unlock();
        }
        return b_unique;
    }
    // -----------------------------------------------
    /**
      * Function to check if image exists in the image-table
      * - avoids duplicate images
      * @param ba_tile_data the image-data extracted from the Bitmap.
      * @return tile_id of found image or blank
      */
    private String search_tile_image( byte[] ba_tile_data ) throws IOException { // The use of
                                                                                 // 'i_force_unique
                                                                                 // == 1' will
                                                                                 // probely slow
                                                                                 // things down to a
                                                                                 // craw
        String s_tile_id = "";
        if (i_type_tiles != 1) { // there will be no 'images' table, return
            return s_tile_id;
        }
        String s_tile_data = get_hex(ba_tile_data);
        String s_sql_query = "SELECT tile_id FROM images WHERE (hex(tile_data) = '" + s_tile_data + "')";
        db_lock.readLock().lock();
        try { // ?? another way to query for binary data in java ??
            final Cursor c = db_mbtiles.rawQuery(s_sql_query, null);
            if (c != null) {
                if (c.moveToFirst()) { // TODO: do something if multiple results are returned
                    s_tile_id = c.getString(c.getColumnIndex("tile_id"));
                }
                c.close();
            }
        } catch (Exception e) {
            throw new IOException("MBTilesDroidSpitter:search_tile_image query[" + s_sql_query + "] error["
                    + e.getLocalizedMessage() + "] ");
        } finally {
            db_lock.readLock().unlock();
        }
        if (s_tile_id != "") {
            String msg = "MBTilesDroidSpitter:search_tile_image[" + file_mbtiles.getAbsolutePath() + "]  tile_id[" + s_tile_id
                    + "] [a non-blank unique image has been found]";
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("MBTilesDroidSpitter", msg);
        }
        return s_tile_id;
    }
    // -----------------------------------------------
    /**
      * Function to check if inserted tile is outside known bounds and min/max zoom, update metadata if desired
      * - i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will be converted to 'tms' notation if needed]
      * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_update 1=updata metadate if outside of range [bounds, min/max zoom]
      * @return 1: metdata was updated
      */
    public int checkBounds( int i_x, int i_y_osm, int i_z, int i_update ) throws IOException {
        int i_rc = 0;
        // GPLog.androidLog(-1,"MBTilesDroidSpitter.icheckBounds: parms["+i_z+"/"+i_x+"/"+i_y_osm+"] i_fetch_bounds["+i_fetch_bounds+"]");
        // minx, miny, maxx, maxy
        double[] tileBounds = tileLatLonBounds(i_x, i_y_osm, i_z, 256);
        HashMap<String, String> update_metadata = this.metadata.checkTileLocation(tileBounds, i_z);
        if (update_metadata.size() > 0) {
            if (i_update == 1) { // the bounds or min/max zoom have changed, update changed values
                                 // and reload metadata
                int i_reload_metadata = 1; // call fetchMetadata so that the new values will take
                                           // effect
                try {
                    update_mbtiles_metadata(db_mbtiles, update_metadata, i_reload_metadata);
                } catch (Exception e) {
                    GPLog.androidLog(4, "MBTilesDroidSpitter[" + file_mbtiles.getAbsolutePath() + "]", e);
                }
            }
            i_rc = 1;
        }
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Query the mbtiles metadata-table and returns validated results
      * - when called the first time, a mbtiles validity check is done
      * @return HashMap<String,String> metadate [key,value]
      */
    public MbTilesMetadata fetchMetadata( String metadataVersion ) throws MetadataParseException {
        Cursor c = db_mbtiles.query(MbTilesSQLite.TABLE_METADATA, new String[]{MbTilesSQLite.COL_METADATA_NAME,
                MbTilesSQLite.COL_METADATA_VALUE}, null, null, null, null, null);
        MetadataValidator validator = MbTilesMetadata.MetadataValidatorFactory.getMetadataValidatorFromVersion(metadataVersion);
        if (validator == null)
            return null;
        this.metadata = MbTilesMetadata.createFromCursor(c, c.getColumnIndex(MbTilesSQLite.COL_METADATA_NAME),
                c.getColumnIndex(MbTilesSQLite.COL_METADATA_VALUE), validator);
        if (this.metadata != null) { // mbtiles is only valid if 'metadata' has values
            this.s_tile_row_type = this.metadata.s_tile_row_type;
            this.s_center_parm = this.metadata.s_center_parm;
            if (i_type_tiles < 0) { // mbtiles is only valid if 'i_type_tiles' == 0 or 1 [table or
                                    // view]
                i_type_tiles = check_type_tiles();
                switch( i_type_tiles ) {
                case 0:
                case 1:
                    b_mbtiles_valid = true;
                    break;
                default:
                    b_mbtiles_valid = false;
                    break;
                }
            }
        }
        return this.metadata;
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
    public boolean isValid() {
        return b_mbtiles_valid;
    }
    // -----------------------------------------------
    /**
      * Checks table type of 'tiles' [needed for an insert of a tile]
      *  tiles : do the fields zoom_level [z], tile_column [x], tile_row [y] and tile_data exist
      * - both as 'view' or 'table' must have these fields
      * when tiles is a view:
      * - map : do the fields zoom_level [z], tile_column [x], tile_row [y] and tile_id exist
      * -- this table should also have a grid_id [but not needed for this implementation]
      * - images : do the fields tile_data and tile_id exist
      * with these checks, reading / writing should always work [if a netadata table exists, the mbtiles is valid]
      * @return i_rc to set 'i_type_tiles' [view[1] or table[0]], anything else should be consider an error for inserting
      */
    private int check_type_tiles() {
        int i_rc = -1;
        boolean b_mbtiles_valid = false;
        String s_type_tiles = "";
        String s_field = "";
        int i_field_count = 0;
        Cursor c_tiles = db_mbtiles.rawQuery("SELECT type AS type_tiles FROM sqlite_master WHERE tbl_name = 'tiles'", null);
        if ((c_tiles != null) && (c_tiles.getColumnCount() > 0) && (c_tiles.getCount() > 0)) {
            if (c_tiles.moveToFirst()) {
                s_type_tiles = c_tiles.getString(c_tiles.getColumnIndex("type_tiles"));
            }
            c_tiles.close();
        }
        if (s_type_tiles.equals("view") || s_type_tiles.equals("table")) { // i_type_tiles<0 =
                                                                           // invalid mbtiles ;
                                                                           // 0='tiles' is a table ;
                                                                           // 1='tiles' is a view
            Cursor c_fields = db_mbtiles.rawQuery("pragma table_info(tiles)", null);
            if (c_fields != null) {
                if (c_fields.moveToFirst()) {
                    do { // tiles : do the fields zoom_level [z], tile_column [x], tile_row [y] and
                         // tile_data exist
                        s_field = c_fields.getString(c_fields.getColumnIndex("name"));
                        if ((s_field.equals("zoom_level")) || (s_field.equals("tile_column")) || (s_field.equals("tile_row"))
                                || (s_field.equals("tile_data"))) {
                            i_field_count++;
                        } else {
                            if (s_field.equals("grid_id")) { // not all mbtiles map tables have a
                                                             // 'grid_id': avoid insert error
                                b_grid_id = true;
                            }
                        }
                    } while( c_fields.moveToNext() );
                }
                c_fields.close();
            }
            if (i_field_count != 4) { // set as invalid
                s_type_tiles = "";
            }
            if (s_type_tiles.equals("table")) {
                i_rc = 0;
                b_mbtiles_valid = true;
            }
            if (s_type_tiles.equals("view")) { // the view will reference 2 tables [map,images] with
                                               // specific fields
                i_field_count = 0;
                c_fields = db_mbtiles.rawQuery("pragma table_info(map)", null);
                if (c_fields != null) {
                    if (c_fields.moveToFirst()) {
                        do { // map : do the fields zoom_level [z], tile_column [x], tile_row [y]
                             // and tile_id exist
                            s_field = c_fields.getString(c_fields.getColumnIndex("name"));
                            if ((s_field.equals("zoom_level")) || (s_field.equals("tile_column")) || (s_field.equals("tile_row"))
                                    || (s_field.equals("tile_id"))) {
                                i_field_count++;
                            } else {
                                if (s_field.equals("grid_id")) { // not all mbtiles map tables have
                                                                 // a 'grid_id': avoid insert error
                                    b_grid_id = true;
                                }
                            }
                        } while( c_fields.moveToNext() );
                    }
                    c_fields.close();
                }
                if (i_field_count == 4) {
                    i_field_count = 0;
                    c_fields = db_mbtiles.rawQuery("pragma table_info(images)", null);
                    if (c_fields != null) {
                        if (c_fields.moveToFirst()) {
                            do { // images : do the fields tile_data and tile_id exist
                                s_field = c_fields.getString(c_fields.getColumnIndex("name"));
                                if ((s_field.equals("tile_id")) || (s_field.equals("tile_data"))) {
                                    i_field_count++;
                                }
                            } while( c_fields.moveToNext() );
                        }
                        c_fields.close();
                    }
                    if (i_field_count == 2) {
                        i_rc = 1;
                        b_mbtiles_valid = true;
                    }
                }
                if (i_rc != 1) {
                    s_type_tiles = "";
                }
            }
        }
        if (b_mbtiles_valid) {
            get_request_url_count(i_request_url_count_read_db); // will read status from Database
        }
        return i_rc;
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
    public List<String> build_request_list( double[] request_bounds, int i_zoom_level, String s_request_type,
            String s_url_source, String s_request_y_type ) {
        List<String> list_tile_id = new ArrayList<String>(); // this will be the final object -
                                                             // depending on type
        if ((!s_request_type.equals("fill")) && (!s_request_type.equals("replace")) && (!s_request_type.equals("exists")))
            s_request_type = "exists"; // set default, if invalid
        int[] tile_bounds = LatLonBounds_to_TileBounds(request_bounds, i_zoom_level);
        // i_zoom=tile_bounds[0];
        int i_min_x = tile_bounds[1];
        int i_min_y_osm = tile_bounds[2];
        int i_max_x = tile_bounds[3];
        int i_max_y_osm = tile_bounds[4];
        int i_min_y_tms = i_min_y_osm;
        int i_max_y_tms = i_max_y_osm;
        int i_min_y = i_min_y_osm;
        int i_max_y = i_max_y_osm;
        int i_count_x = i_max_x - i_max_x;
        int i_count_y = i_max_y_osm - i_max_y_osm;
        if (s_tile_row_type.equals("tms")) {
            int[] tmsTileXY = MBTilesDroidSpitter.googleTile2TmsTile(i_min_x, i_min_y_osm, i_zoom_level);
            i_min_y_tms = tmsTileXY[1];
            i_min_y = i_min_y_tms;
            tmsTileXY = MBTilesDroidSpitter.googleTile2TmsTile(i_max_x, i_max_y_osm, i_zoom_level);
            i_max_y_tms = tmsTileXY[1];
            i_max_y = i_max_y_tms;
        }
        if ((s_request_type.equals("fill")) || (s_request_type.equals("replace"))) { // Sorted from
                                                                                     // West to East
                                                                                     // ; North to
                                                                                     // South
            for( int x = i_min_x; x <= i_max_x; x++ ) { // fill the list will all possible
                                                        // combinations : will be the result of
                                                        // 'replace'
                                                        // the osm number for north[max] is smaller
                                                        // that south[min]
                for( int y = i_max_y_osm; y <= i_min_y_osm; y++ ) { // input must be y with
                                                                    // osm-notation - returned will
                                                                    // be the notation based on the
                                                                    // active database
                    String s_tile_id = get_tile_id_from_zxy(i_zoom_level, x, y);
                    if (!s_url_source.equals("")) { // We are adding from an existing set of tiles,
                                                    // the tile must exist
                        String s_file = s_url_source;
                        int[] zxy_osm_tms = get_zxy_from_tile_id(s_tile_id);
                        if ((zxy_osm_tms != null) && (zxy_osm_tms.length == 4)) {
                            int i_z = zxy_osm_tms[0];
                            int i_x = zxy_osm_tms[1];
                            int i_y_osm = zxy_osm_tms[2];
                            int i_y_tms = zxy_osm_tms[3];
                            int i_y = i_y_osm;
                            if (s_request_y_type.equals("tms")) {
                                i_y = i_y_tms;
                            }
                            int indexOfZ = s_file.indexOf("ZZZ");
                            if (indexOfZ != -1) { // tile-server: replace ZZZ,XXX,YYY
                                s_file = s_file.replaceFirst("ZZZ", String.valueOf(i_z)); //$NON-NLS-1$
                                s_file = s_file.replaceFirst("XXX", String.valueOf(i_x)); //$NON-NLS-1$
                                s_file = s_file.replaceFirst("YYY", String.valueOf(i_y)); //$NON-NLS-1$
                                File file_tile = new File(s_file);
                                if (!file_tile.exists()) { // if the tile-file does not exist, do
                                                           // not add
                                    s_tile_id = "";
                                }
                            }
                        }
                    }
                    if (!s_tile_id.equals("")) {
                        list_tile_id.add(s_tile_id);
                    }
                }
            }
            if (s_request_type.equals("replace")) {
                return list_tile_id; // returns all posibilities
            }
        }
        List<String> list_tile_id_exists = new ArrayList<String>();
        String s_table = "map";
        if (i_type_tiles == 0) { // mbtiles is only valid if 'i_type_tiles' == 0 or 1 [table or
                                 // view]
            s_table = "tiles"; // map will not exist
        }
        String s_select_where = "WHERE ((zoom_level = " + i_zoom_level + ") AND ";
        s_select_where = s_select_where + "((tile_column >= " + i_min_x + ") AND (tile_column <= " + i_max_x + ")) AND ";
        if (s_tile_row_type.equals("tms")) { // tms: Sorted from West to East ; North to South
            s_select_where = s_select_where + "((tile_row >= " + i_min_y + ") AND (tile_row <= " + i_max_y + "))) ";
            s_select_where = s_select_where + "ORDER BY tile_column ASC,tile_row DESC";
        } else { // osm: Sorted from West to East ; North to South
            s_select_where = s_select_where + "((tile_row >= " + i_max_y + ") AND (tile_row <= " + i_min_y + "))) ";
            s_select_where = s_select_where + "ORDER BY tile_column ASC,tile_row ASC";
        }
        String s_select_sql = "SELECT tile_column,tile_row FROM " + s_table + " " + s_select_where;
        // SELECT tile_column,tile_row FROM map
        // WHERE ((zoom_level = 15) AND
        // ((tile_column >= 1759) AND (tile_column <= 17608)) AND
        // ((tile_row >= 22013) AND (tile_row <= 22026)))
        // ORDER BY tile_column ASC,tile_row DESC
        db_lock.readLock().lock();
        try {
            Cursor c_tiles = db_mbtiles.rawQuery(s_select_sql, null);
            if ((c_tiles != null) && (c_tiles.getColumnCount() > 0) && (c_tiles.getCount() > 0)) {
                if (c_tiles.moveToFirst()) {
                    do { // map : do the fields zoom_level [z], tile_column [x], tile_row [y] and
                         // tile_id exist
                        int i_tile_column = c_tiles.getInt(c_tiles.getColumnIndex("tile_column"));
                        int i_tile_row = c_tiles.getInt(c_tiles.getColumnIndex("tile_row"));
                        String s_tile_id = i_zoom_level + "-" + i_tile_column + "-" + i_tile_row + "." + s_tile_row_type;
                        list_tile_id_exists.add(s_tile_id);
                    } while( c_tiles.moveToNext() );
                }
            }
            c_tiles.close();
        } catch (Exception e) {
        } finally {
            db_lock.readLock().unlock();
        }
        // GPLog.androidLog(-1,"build_request_list["+s_request_type+"] all["+list_tile_id.size()+"] exists["+list_tile_id_exists.size()+"] sql["+s_select_sql+"]");
        if (s_request_type.equals("exists")) {
            list_tile_id.clear();
            return list_tile_id_exists; // returns all that exist
        }
        // 'fill' remove existing from the compleate list and return the missing
        for( int i = 0; i < list_tile_id_exists.size(); i++ ) {
            String s_tile_id = list_tile_id_exists.get(i);
            list_tile_id.remove(s_tile_id);
        }
        list_tile_id_exists.clear();
        return list_tile_id;
    }
    // -----------------------------------------------
    /**
      * Returns status of table: request_url
      * parm values:
      * 0: return existing value [set when database was opended,not reading the table] [MBTilesDroidSpitter.i_request_url_read_value]
      * 1 : return existing value return existing value [reading the table with count after checking if it exits] [MBTilesDroidSpitter.i_request_url_read_db]
      * 2: create table (if it does not exist) [MBTilesDroidSpitter.i_request_url_count_create]
      * 3: delete table (if it does exist) [MBTilesDroidSpitter.i_request_url_count_drop]
      * -- may be called within a '.beginTransaction()'
      * @param i_parm type of result
      * @return i_request_url_count [< 0: table does not exist; 0=exist but is empty ; > 0 open requests]
      */
    public int get_request_url_count( int i_parm ) {
        String s_sql_request_url = "";
        switch( i_parm ) {
        case i_request_url_count_drop: { // create if '-1' or delete if '0', otherwise return count
            s_sql_request_url = "DROP TABLE IF EXISTS request_url";
            try {
                db_mbtiles.execSQL(s_sql_request_url);
            } catch (Exception e) {
                s_sql_request_url = "";
                GPLog.androidLog(4, "MBTilesDroidSplitter: [" + getName() + "] -E-> get_request_url_count: parm[" + i_parm
                        + "][2=DROP or CREATE request_url] get_request_url_count[" + this.i_request_url_count + "]  sql["
                        + s_sql_request_url + "]", e);
            } finally {
                if (!s_sql_request_url.equals("")) { // no error [DROP or CREATE was compleated
                                                     // correctly]
                                                     // table has been deleted and is empty
                    this.i_request_url_count = -1;
                }
            }
        }
            break;
        case i_request_url_count_create: {
            s_sql_request_url = "CREATE TABLE IF NOT EXISTS request_url (tile_id TEXT PRIMARY KEY,tile_url TEXT)";
            try {
                db_mbtiles.execSQL(s_sql_request_url);
            } catch (Exception e) {
                s_sql_request_url = "";
                GPLog.androidLog(4, "MBTilesDroidSplitter: [" + getName() + "] -E-> get_request_url_count: parm[" + i_parm
                        + "][2=DROP or CREATE request_url] get_request_url_count[" + this.i_request_url_count + "]  sql["
                        + s_sql_request_url + "]", e);
            } finally {
                if (!s_sql_request_url.equals("")) { // no error [DROP or CREATE was compleated
                                                     // correctly]
                                                     // table has been created and is empty
                    this.i_request_url_count = 0;
                }
            }
        }
            break;
        case i_request_url_count_read_db: { // read from database
            int i_field_count = 0;
            db_lock.readLock().lock();
            try {
                s_sql_request_url = "pragma table_info(request_url)";
                Cursor c_tiles = db_mbtiles.rawQuery(s_sql_request_url, null);
                if ((c_tiles != null) && (c_tiles.getColumnCount() > 0) && (c_tiles.getCount() > 0)) { // then
                                                                                                       // the
                                                                                                       // table
                                                                                                       // exists
                                                                                                       // [no
                                                                                                       // it
                                                                                                       // does
                                                                                                       // not!
                                                                                                       // fields
                                                                                                       // must
                                                                                                       // be
                    // read]
                    if ((c_tiles.getCount() > 0) && (c_tiles.moveToFirst())) {
                        do { // tiles : do the fields zoom_level [z], tile_column [x], tile_row [y]
                             // and tile_data exist
                            String s_field = c_tiles.getString(c_tiles.getColumnIndex("name"));
                            if ((s_field.equals("tile_id")) || (s_field.equals("tile_url"))) {
                                i_field_count++;
                            }
                        } while( c_tiles.moveToNext() );
                    }
                    c_tiles.close();
                    if (i_field_count > 0) {
                        this.i_request_url_count = 0;
                        s_sql_request_url = "SELECT count(tile_id) AS count_id FROM request_url";
                        c_tiles = db_mbtiles.rawQuery(s_sql_request_url, null);
                        if ((c_tiles != null) && (c_tiles.getColumnCount() > 0) && (c_tiles.getCount() > 0)) {
                            if (c_tiles.moveToFirst()) {
                                this.i_request_url_count = c_tiles.getInt(c_tiles.getColumnIndex("count_id"));
                            }
                        }
                        c_tiles.close();
                    } else {
                        this.i_request_url_count = -1;
                    }
                }
            } catch (Exception e) {
                GPLog.androidLog(4, "MBTilesDroidSplitter: [" + getName() + "] -E-> get_request_url_count: parm[" + i_parm
                        + "][1=pragma table_info(request_url) or count(tile_id)] get_request_url_count["
                        + this.i_request_url_count + "]  sql[" + s_sql_request_url + "]", e);
            } finally {
                db_lock.readLock().unlock();
            }
        }
            break;
        default:
        case i_request_url_count_read_value: { // return existing value
        }
            break;
        }
        return this.i_request_url_count;
    }
    // -----------------------------------------------
    /**
      * insert/delete of record in table: request_url
      * parm values:
      * 3: insert record with: s_tile_id and s_tile_url [MBTilesDroidSpitter.i_request_url_count_insert]
      * - 'request_url' will be created if it does not exist
      * 1: delete record with: s_tile_id, delete table if count is 0
      * 4: delete record with: s_tile_id, delete table if count is 0 [MBTilesDroidSpitter.i_request_url_count_delete]
      * - 'request_url' will be deleted when the last records is deleted
      * may be called within a '.beginTransaction()'
      * @param i_parm type of command
      * @param s_tile_id tile_id to use
      * @param s_tile_url full url to retrieve tile with
      * @return i_request_url_count [< 0: table does not exist; 0=exist but is empty ; > 0 open requests]
      */
    public int insert_request_url( int i_parm, String s_tile_id, String s_tile_url ) {
        String s_sql_request_url = "";
        switch( i_parm ) {
        case i_request_url_count_delete: { // delete
            s_sql_request_url = "DELETE FROM 'request_url' WHERE ( tile_id = '" + s_tile_id + "')";
            try { // no lock/unlock here, done in 'insert_list_request_url'
                db_mbtiles.execSQL(s_sql_request_url);
                this.i_request_url_count--;
            } catch (Exception e) {
                // GPLog.androidLog(4,"MBTilesDroidSplitter: ["+getName()+"] -E-> insert_request_url: parm["+i_parm+"][3=INSERT;4=DELETE]  tile_id["+s_tile_id+"]",e);
            } finally {
                if (this.i_request_url_count < 1) { // this will delete the empty table
                    this.i_request_url_count = 0;
                    get_request_url_count(i_request_url_count_drop);
                }
            }
        }
            break;
        case i_request_url_count_insert: { // insert
            if (this.i_request_url_count < 0) { // this will create the table
                get_request_url_count(i_request_url_count_create);
            }
            // s_sql_request_url =
            // "INSERT OR REPLACE INTO 'request_url' VALUES('"+s_tile_id+"','"+s_tile_url+"')";
            ContentValues tiles_values = new ContentValues();
            tiles_values.put("tile_id", s_tile_id);
            tiles_values.put("tile_url", s_tile_url);
            try { // no lock/unlock here, done in 'insert_list_request_url'
                db_mbtiles.insertOrThrow("request_url", null, tiles_values);
                // will only be added if it did not exist
                this.i_request_url_count++;
            } catch (Exception e) {
                // GPLog.androidLog(4,"MBTilesDroidSplitter: ["+getName()+"] -E-> insert_request_url: parm["+i_parm+"][3=INSERT;4=DELETE]  tile_id["+s_tile_id+"]",e);
            }
        }
            break;
        }
        // GPLog.androidLog(-1,"MBTilesDroidSplitter: ["+getName()+"] -I-> insert_request_url: parm["+i_parm+"][3=INSERT;4=DELETE]  tile_id["+s_tile_id+"]");
        return this.i_request_url_count;
    }
    // -----------------------------------------------
    /**
      * bulk insert of records in table: request_url
      * - the request_url table will be created if it does not exist
      * - inserting is done for each Zoom-Level
      * - '.beginTransaction()' and '.endTransaction()' [and, of course: '.setTransactionSuccessful();']
      * @return i_request_url_count [< 0: table does not exist; 0=exist but is empty ; > 0 open requests]
      */
    public int insert_list_request_url( HashMap<String, String> mbtiles_request_url ) {
        int i_request_url_count_prev = this.i_request_url_count;
        db_lock.writeLock().lock();
        db_mbtiles.beginTransaction();
        try {
            for( Map.Entry<String, String> request_url : mbtiles_request_url.entrySet() ) {
                String s_tile_id = request_url.getKey();
                String s_tile_url = request_url.getValue();
                insert_request_url(i_request_url_count_insert, s_tile_id, s_tile_url);
            }
            db_mbtiles.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.androidLog(4, "MBTilesDroidSplitter: [" + getName() + "] -E-> insert_list_request_url["
                    + this.i_request_url_count + "]  mbtiles_request_url.size[" + mbtiles_request_url.size() + "] empty["
                    + mbtiles_request_url.isEmpty() + "]", e);
        } finally {
            db_mbtiles.endTransaction();
            db_lock.writeLock().unlock();
        }
        // GPLog.androidLog(-1,"["+getName()+"] insert_list_request_url["+this.i_request_url_count+"] ");
        return this.i_request_url_count;
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
        db_lock.writeLock().lock();
        try {
            i_rc = 2;
            db_mbtiles.execSQL("VACUUM");
            i_rc = 1;
            // db_mbtiles.execSQL("ANALYZE"); // ANALYZE
            i_rc = 0;
        } catch (Exception e) {
            GPLog.androidLog(4, "MBTilesDroidSplitter: [" + getName() + "] -E-> on_analyze_vacuum[" + i_rc + "] ", e);
        } finally {
            db_lock.writeLock().unlock();
            GPLog.androidLog(-1, "MBTilesDroidSplitter: [" + getName() + "] -I-> on_analyze_vacuum[" + i_rc + "] ");
        }
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Returns list of collected 'request_url'
      * - Query only when 'this.i_request_url_count' > 0 ; i.e. Table exists and has records
      * @param i_limit amount of records to retrieve [i_limit < 1 == all]
      * @return HashMap<String,String> mbtiles_request_url [tile_id,tile_url]
      */
    public HashMap<String, String> retrieve_request_url( int i_limit ) {
        HashMap<String, String> mbtiles_request_url = new LinkedHashMap<String, String>();
        String s_limit = "";
        if ((i_limit > 0) && (i_limit < this.i_request_url_count)) { // avoid excesive memory usage
            s_limit = " LIMIT " + i_limit;
        }
        if (this.i_request_url_count > 0) {
            db_lock.readLock().lock();
            try {
                String s_mbtiles_request_url = "SELECT tile_id,tile_url FROM request_url" + s_limit;
                Cursor c_tiles = db_mbtiles.rawQuery(s_mbtiles_request_url, null);
                if ((c_tiles != null) && (c_tiles.getColumnCount() > 0) && (c_tiles.getCount() > 0)) { // avoid
                                                                                                       // CursorIndexOutOfBoundsException
                    if ((c_tiles.getCount() > 0) && (c_tiles.moveToFirst())) {
                        do {
                            String s_tile_id = c_tiles.getString(c_tiles.getColumnIndex("tile_id"));
                            String s_tile_url = c_tiles.getString(c_tiles.getColumnIndex("tile_url"));
                            mbtiles_request_url.put(s_tile_id, s_tile_url);
                        } while( c_tiles.moveToNext() );
                    }
                    c_tiles.close();
                }
            } catch (Exception e) {
                GPLog.androidLog(4, "MBTilesDroidSplitter: [" + getName() + "] -E-> retrieve_request_url["
                        + this.i_request_url_count + "] ", e);
            } finally {
                db_lock.readLock().unlock();
            }
        }
        return mbtiles_request_url;
    }
    // -----------------------------------------------
    /**
      * Returns result of last called fetchMetadata
      * @return HashMap<String,String> metadate [key,value]
      */
    public MbTilesMetadata getMetadata() {
        return this.metadata;
    }
    // -----------------------------------------------
    /**
      * Function to create new mbtiles Database
      * - parent diretories will be created, if needed
      * - needed Tables/View and default values for metdata-table will be created
      * @param file_mbtiles name of Database file
      * @return 0: no error
      */
    public int create_mbtiles( File file_mbtiles ) throws IOException {
        int i_rc = 0;
        File dir_mbtiles = file_mbtiles.getParentFile();
        String mbtiles_name = file_mbtiles.getName().substring(0, file_mbtiles.getName().lastIndexOf("."));
        if (!dir_mbtiles.exists()) {
            if (!dir_mbtiles.mkdir()) {
                throw new IOException("MBTilesDroidSpitter: create_mbtiles: mbtiles_dir[" + dir_mbtiles.getAbsolutePath()
                        + "] creation failed");
            }
        }
        SQLiteDatabase sqlite_db = SQLiteDatabase.openOrCreateDatabase(file_mbtiles, null);
        if (sqlite_db != null) {
            sqlite_db.setLocale(Locale.getDefault());
            sqlite_db.setLockingEnabled(false);
            // CREATE TABLES and default values
            try { // create default tables for mbtiles
                  // set default values for metadata, where not supplied in 'this.mbtiles_metadata'
                create_mbtiles_tables(sqlite_db, mbtiles_name);
                // write all metatdata values to database (with 'null', 'this.mbtiles_metadata' will
                // be used)
                int i_reload_metadata = 0; // do not reload, since we are closing the db
                update_mbtiles_metadata(sqlite_db, null, i_reload_metadata);
            } catch (Exception e) {
                sqlite_db.close();
                sqlite_db = null;
                GPLog.androidLog(4, "MBTilesDroidSpitter[" + file_mbtiles.getAbsolutePath() + "]", e);
                i_rc = 2;
                return i_rc;
            }
            sqlite_db.close();
            sqlite_db = null;
        }
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Function to create needed mbtiles Tables/Views
      * - default values will be added to this.mbtiles_metadata where needed
      * @param mbtiles_db Database connection
      * @param mbtiles_name used for metadata.name if otherwise not set in this.mbtiles_metadata
      * @return 0: no error
      */
    public int create_mbtiles_tables( SQLiteDatabase mbtiles_db, String mbtiles_name ) throws IOException {
        int i_rc = 0;
        String s_mbtiles_field_tile_data = "tile_data";
        String s_mbtiles_field_zoom_level = "zoom_level";
        String s_mbtiles_field_tile_column = "tile_column";
        String s_mbtiles_field_tile_row = "tile_row";
        String s_mbtiles_field_tile_id = "tile_id";
        String s_mbtiles_field_grid_id = "grid_id";
        String s_mbtiles_field_name = "name";
        String s_mbtiles_field_value = "value";
        String s_metadata_tablename = "metadata";
        String s_images_tablename = "images";
        String s_tiles_tablename = "tiles";
        String s_map_tablename = "map";
        // -----------------------------------------------
        String s_sql_create_grid_key = "CREATE TABLE IF NOT EXISTS grid_key (" + s_mbtiles_field_grid_id + " TEXT,key_name TEXT)";
        String s_sql_create_grid_utfgrid = "CREATE TABLE IF NOT EXISTS grid_utfgrid (" + s_mbtiles_field_grid_id
                + " TEXT,grid_utfgrid BLOB)";
        String s_sql_create_images = "CREATE TABLE IF NOT EXISTS " + s_images_tablename + " (" + s_mbtiles_field_tile_data
                + " blob," + s_mbtiles_field_tile_id + " text)";
        String s_sql_create_keymap = "CREATE TABLE IF NOT EXISTS keymap (key_name TEXT,key_json TEXT)";
        String s_sql_create_map = "CREATE TABLE IF NOT EXISTS " + s_map_tablename + " (" + s_mbtiles_field_zoom_level
                + " INTEGER," + s_mbtiles_field_tile_column + " INTEGER," + s_mbtiles_field_tile_row + " INTEGER,"
                + s_mbtiles_field_tile_id + " TEXT," + s_mbtiles_field_grid_id + " TEXT)";
        String s_sql_create_metadata = "CREATE TABLE IF NOT EXISTS " + s_metadata_tablename + " (" + s_mbtiles_field_name
                + " text," + s_mbtiles_field_value + " text)";
        // CREATE VIEW tiles AS SELECT map.zoom_level AS zoom_level,map.tile_column AS
        // tile_column,map.tile_row AS tile_row,images.tile_data AS tile_data FROM map JOIN images
        // ON images.tile_id = map.tile_id ORDER BY zoom_level,tile_column,tile_row
        String s_sql_create_view_tiles = "CREATE VIEW IF NOT EXISTS " + s_tiles_tablename + " AS SELECT " + s_map_tablename + "."
                + s_mbtiles_field_zoom_level + " AS " + s_mbtiles_field_zoom_level + "," + s_map_tablename + "."
                + s_mbtiles_field_tile_column + " AS " + s_mbtiles_field_tile_column + "," + s_map_tablename + "."
                + s_mbtiles_field_tile_row + " AS " + s_mbtiles_field_tile_row + "," + s_images_tablename + "."
                + s_mbtiles_field_tile_data + " AS " + s_mbtiles_field_tile_data + " FROM " + s_map_tablename + " JOIN "
                + s_images_tablename + " ON " + s_images_tablename + "." + s_mbtiles_field_tile_id + " = " + s_map_tablename
                + "." + s_mbtiles_field_tile_id + " ORDER BY " + s_mbtiles_field_zoom_level + "," + s_mbtiles_field_tile_column
                + "," + s_mbtiles_field_tile_row;
        String s_sql_create_view_grids = "CREATE VIEW IF NOT EXISTS grids AS SELECT " + s_map_tablename + "."
                + s_mbtiles_field_zoom_level + " AS " + s_mbtiles_field_zoom_level + "," + s_map_tablename + "."
                + s_mbtiles_field_tile_column + " AS " + s_mbtiles_field_tile_column + "," + s_map_tablename + "."
                + s_mbtiles_field_tile_row + " AS " + s_mbtiles_field_tile_row + ",grid_utfgrid.grid_utfgrid AS grid FROM "
                + s_map_tablename + " JOIN grid_utfgrid ON grid_utfgrid." + s_mbtiles_field_grid_id + " = " + s_map_tablename
                + "." + s_mbtiles_field_grid_id;
        String s_sql_create_view_grid_data = "CREATE VIEW IF NOT EXISTS grid_data AS SELECT " + s_map_tablename + "."
                + s_mbtiles_field_zoom_level + " AS " + s_mbtiles_field_zoom_level + "," + s_map_tablename + "."
                + s_mbtiles_field_tile_column + " AS " + s_mbtiles_field_tile_column + "," + s_map_tablename + "."
                + s_mbtiles_field_tile_row + " AS " + s_mbtiles_field_tile_row
                + ",keymap.key_name AS key_name,keymap.key_json AS key_json FROM " + s_map_tablename + " JOIN grid_key ON "
                + s_map_tablename + "." + s_mbtiles_field_grid_id + " = grid_key." + s_mbtiles_field_grid_id
                + " JOIN keymap ON grid_key.key_name = keymap.key_name";
        String s_sql_create_index_grid_key_lookup = "CREATE UNIQUE INDEX IF NOT EXISTS grid_key_lookup ON grid_key ("
                + s_mbtiles_field_grid_id + ",key_name)";
        String s_sql_create_index_grid_utfgrid_lookup = "CREATE UNIQUE INDEX IF NOT EXISTS grid_utfgrid_lookup ON grid_utfgrid ("
                + s_mbtiles_field_grid_id + ")";
        String s_sql_create_index_images = "CREATE UNIQUE INDEX IF NOT EXISTS " + s_images_tablename + "_id ON "
                + s_images_tablename + " (" + s_mbtiles_field_tile_id + " )";
        String s_sql_create_index_keymap_lookup = "CREATE UNIQUE INDEX IF NOT EXISTS keymap_lookup ON keymap (key_name)";
        String s_sql_create_index_map = "CREATE UNIQUE INDEX IF NOT EXISTS " + s_map_tablename + "_index ON " + s_map_tablename
                + " (" + s_mbtiles_field_zoom_level + "," + s_mbtiles_field_tile_column + "," + s_mbtiles_field_tile_row + ")";
        String s_sql_create_index_metadata = "CREATE UNIQUE INDEX IF NOT EXISTS " + s_metadata_tablename + "_index ON "
                + s_metadata_tablename + " (" + s_mbtiles_field_name + ")";
        // String
        // s_sql_create_android_metadata="CREATE TABLE IF NOT EXISTS android_"+s_metadata_tablename+" (locale text)";
        // mj10777: not needed in android - done with sqlite_db.setLocale(Locale.getDefault());
        // ----------------------------------------------
        db_lock.writeLock().lock();
        mbtiles_db.beginTransaction();
        try {
            mbtiles_db.execSQL(s_sql_create_grid_key);
            mbtiles_db.execSQL(s_sql_create_grid_utfgrid);
            mbtiles_db.execSQL(s_sql_create_images);
            mbtiles_db.execSQL(s_sql_create_keymap);
            mbtiles_db.execSQL(s_sql_create_map);
            mbtiles_db.execSQL(s_sql_create_metadata);
            mbtiles_db.execSQL(s_sql_create_view_tiles);
            mbtiles_db.execSQL(s_sql_create_view_grids);
            mbtiles_db.execSQL(s_sql_create_view_grid_data);
            mbtiles_db.execSQL(s_sql_create_index_grid_key_lookup);
            mbtiles_db.execSQL(s_sql_create_index_grid_utfgrid_lookup);
            mbtiles_db.execSQL(s_sql_create_index_images);
            mbtiles_db.execSQL(s_sql_create_index_keymap_lookup);
            mbtiles_db.execSQL(s_sql_create_index_map);
            mbtiles_db.execSQL(s_sql_create_index_metadata);
            // mbtiles_db.execSQL(s_sql_create_android_metadata);
            mbtiles_db.setTransactionSuccessful();
        } catch (Exception e) {
            i_rc = 1;
            throw new IOException("MBTilesDroidSpitter: create_mbtiles_tables error[" + e.getLocalizedMessage() + "]");
        } finally {
            mbtiles_db.endTransaction();
            db_lock.writeLock().unlock();
        }
        // ----------------------------------------------
        if (this.mbtiles_metadata.get("type") == null)
            this.mbtiles_metadata.put("type", "baselayer");
        if (this.mbtiles_metadata.get("tile_row_type") == null) {
            this.mbtiles_metadata.put("tile_row_type", s_tile_row_type);
        } else {
            this.s_tile_row_type = this.mbtiles_metadata.get("tile_row_type");
        }
        if (this.mbtiles_metadata.get("version") == null)
            this.mbtiles_metadata.put("version", "1.1");
        if (this.mbtiles_metadata.get("format") == null)
            this.mbtiles_metadata.put("format", "jpg");
        if (this.mbtiles_metadata.get("name") == null)
            this.mbtiles_metadata.put("name", mbtiles_name);
        if (this.mbtiles_metadata.get("description") == null)
            this.mbtiles_metadata.put("description", mbtiles_name);
        if (this.mbtiles_metadata.get("bounds") == null)
            this.mbtiles_metadata.put("bounds", "-180.0,-85.05113,180.0,85.05113");
        if (this.mbtiles_metadata.get("center") == null)
            this.mbtiles_metadata.put("center", "0.0,0.0,1");
        if (this.mbtiles_metadata.get("minzoom") == null)
            this.mbtiles_metadata.put("minzoom", "1");
        if (this.mbtiles_metadata.get("maxzoom") == null)
            this.mbtiles_metadata.put("maxzoom", "1");
        // ----------------------------------------------
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * General Function to update mbtiles metadata Table
      * @param mbtiles_db Database connection [upon creation, this is a local variable, otherwise the class variable]
      * @param mbtiles_metadata list of key,values to update. [fill this with valued that need to be added/changed]
      * @param i_reload_metadata 1: reload values after update [not needed upon creation, update after bounds/center/zoom changes]
      * @return 0: no error
      */
    public int update_mbtiles_metadata( SQLiteDatabase mbtiles_db, HashMap<String, String> mbtiles_metadata, int i_reload_metadata )
            throws IOException { // i_rc=0: no error
        int i_rc = 0;
        if (mbtiles_db == null)
            mbtiles_db = getmbtiles();
        if (mbtiles_metadata == null)
            mbtiles_metadata = this.mbtiles_metadata;
        String s_metadata_tablename = "metadata";
        String s_tile_insert_metadata = "";
        // ----------------------------------------------
        db_lock.writeLock().lock();
        mbtiles_db.beginTransaction();
        try {
            for( Map.Entry<String, String> metadata : mbtiles_metadata.entrySet() ) {
                s_tile_insert_metadata = "INSERT OR REPLACE INTO '" + s_metadata_tablename + "' VALUES('" + metadata.getKey()
                        + "','" + metadata.getValue() + "')";
                mbtiles_db.execSQL(s_tile_insert_metadata);
                // GPLog.androidLog(-1, "MBTilesDroidSpitter:update_mbtiles_metadata[" +
                // s_tile_insert_metadata + "]");
            }
            mbtiles_db.setTransactionSuccessful();
        } catch (Exception e) {
            i_rc = 1;
            throw new IOException("MBTilesDroidSpitter:update_mbtiles_metadata sql[" + s_tile_insert_metadata + "] error["
                    + e.getLocalizedMessage() + "]");
        } finally {
            mbtiles_db.endTransaction();
            db_lock.writeLock().unlock();
            if (i_reload_metadata == 1) { // should be done when bounds of min/max zoom has changed
                try {
                    fetchMetadata(this.s_metadataVersion);
                } catch (Exception e) {
                    i_rc = 1;
                }
            }
        }
        // ----------------------------------------------
        return i_rc;
    }
    // -----------------------------------------------
    /**
       * Retrieve min/max tiles for each zoom-level from mbtiles
      * - no checking for possible 'holes' inside zoom-level are done
      * @param i_reload_metadata reload values after update [not needed upon creation, update after bounds/center/zoom changes]
      * @param i_update update to mbiles database if bounds min/max zoom have changed
      * @return the retrieved values. ['zoom','min_x,min_y,max_x,max_y']
      */
    public HashMap<String, String> fetch_bounds_minmax( int i_reload_metadata, int i_update ) {
        HashMap<String, String> update_metadata = new LinkedHashMap<String, String>();
        HashMap<String, String> bounds_min_max = fetch_bounds_minmax_tiles();
        String s_zoom_min_max = "";
        String s_bounds_tiles = "";
        String s_bounds_center = "";
        String s_minzoom = "";
        String s_maxzoom = "";
        String s_centerzoom = "";
        // GPLog.androidLog(-1,"MBTilesDroidSpitter.fetch_bounds_minmax: parms["+i_reload_metadata+"/"+i_update+"] bounds_min_max=["+bounds_min_max.size()+"]");
        if (bounds_min_max.size() > 0) {
            if (bounds_lat_long != null) {
                bounds_lat_long.clear();
            }
            bounds_lat_long = fetch_bounds_minmax_latlong(bounds_min_max, 256);
            if (bounds_lat_long.size() > 0) { // how to retrieve that last value only?
                for( Map.Entry<String, String> zoom_levels : bounds_lat_long.entrySet() ) {
                    s_zoom_min_max = zoom_levels.getKey();
                    s_bounds_tiles = zoom_levels.getValue();
                }
                if ((s_bounds_tiles != "") && (s_bounds_tiles != "")) {
                    String[] sa_splitted = s_zoom_min_max.split(",");
                    if (sa_splitted.length == 3) { // only the last record (with min/max/center
                                                   // zoom) will
                                                   // be used
                        s_minzoom = sa_splitted[0];
                        s_maxzoom = sa_splitted[1];
                        s_centerzoom = sa_splitted[2];
                        if ((s_minzoom != "") && (s_maxzoom != "")) {
                            sa_splitted = s_bounds_tiles.split(";");
                            if (sa_splitted.length == 2) {
                                s_bounds_tiles = sa_splitted[0];
                                s_bounds_center = sa_splitted[1] + "," + s_centerzoom;
                                sa_splitted = s_bounds_tiles.split(",");
                                if (sa_splitted.length == 4) {
                                    update_metadata.put("bounds", s_bounds_tiles);
                                    update_metadata.put("minzoom", s_minzoom);
                                    update_metadata.put("maxzoom", s_maxzoom);
                                    update_metadata.put("center", s_bounds_center);
                                }
                            }
                        }
                    }
                }
            }
        }
        if ((i_update == 1) && (update_metadata.size() > 0)) {
            // GPLog.androidLog(1,"fetch_bounds_minmax[update]  bounds["+s_bounds_tiles+"] minzoom["+s_minzoom+"] maxzoom["+s_maxzoom+"] center["+s_bounds_center+"]");
            try {
                update_mbtiles_metadata(db_mbtiles, update_metadata, i_reload_metadata);
            } catch (Exception e) {
                GPLog.androidLog(4, "MBTilesDroidSpitter[" + file_mbtiles.getAbsolutePath() + "]", e);
            }
        }
        return update_metadata;
    }
    // -----------------------------------------------
    // SELECT count(tile_id) from map WHERE zoom_level = 7;
    // SELECT tile_id from map WHERE zoom_level = 7;
    // to delete a zoom_level properly:
    // DELETE FROM map WHERE zoom_level = 7;
    // DELETE FROM map WHERE tile_id = "ff-ff-ff.rgb";
    // -----------------------------------------------
    /**
       * Retrieve min/max tiles for each zoom-level from mbtiles
      * - no checking for possible 'holes' inside zoom-level are done
      * - 20131107 mj10777: this function causes problems when online retrieving is done
      * -- with big databases it takes time to compleate this, so the application can stall or crash
      * --- an alternitive will be worked out at a later time
      * - 20131123: now work correctl and speedaly - but should only be used when really needed
      * @return the retrieved values. ['zoom','min_x,min_y,max_x,max_y']
      */
    public HashMap<String, String> fetch_bounds_minmax_tiles() {
        HashMap<String, String> bounds_min_max = new LinkedHashMap<String, String>();
        List<Integer> zoom_levels = null;
        int i_zoom_level = 0;
        int i_version = 0;
        // These querys run much quicker with 'maps"
        String s_table = "map";
        if (i_type_tiles == 0) { // mbtiles is only valid if 'i_type_tiles' == 0 or 1 [table or
                                 // view]
            s_table = "tiles"; // map will not exist
        }
        // SELECT zoom_level,min(tile_column) AS min_x,min(tile_row) AS min_y,max(tile_column) AS
        // max_x,max(tile_row) AS max_y FROM tiles WHERE zoom_level IN(SELECT DISTINCT zoom_level
        // FROM tiles ORDER BY zoom_level ASC) GROUP BY zoom_level
        // tiles: 00:00:07.160 ; map 00:00:03.200
        // SELECT DISTINCT zoom_level FROM tiles ORDER BY zoom_level ASC
        // tiles: 00:00:03.209 ; map 00:00:00.040
        // SELECT zoom_level,min(tile_column) AS min_x,min(tile_row) AS min_y,max(tile_column) AS
        // max_x,max(tile_row) AS max_y FROM tiles WHERE zoom_level = 16
        // tiles: 00:00:03.200 ; map 00:00:00.040
        String SQL_GET_MINMAXZOOM_TILES = "SELECT zoom_level,min(tile_column) AS min_x,min(tile_row) AS min_y,max(tile_column) AS max_x,max(tile_row) AS max_y FROM "
                + s_table
                + " WHERE zoom_level IN(SELECT DISTINCT zoom_level FROM "
                + s_table
                + " ORDER BY zoom_level ASC) GROUP BY zoom_level";
        Cursor c_tiles = null;
        db_lock.readLock().lock();
        try {
            // GPLog.androidLog(-1,"MBTilesDroidSpitter.fetch_bounds_minmax_tiles: sql["+SQL_GET_MINMAXZOOM_TILES+"]");
            c_tiles = db_mbtiles.rawQuery(SQL_GET_MINMAXZOOM_TILES, null);
            // mj10777: 20131123: avoid using table/view 'tiles' - with big databases can bring
            // things to a halt
            if ((c_tiles != null) && (c_tiles.getColumnCount() > 0) && (c_tiles.getCount() > 0)) { // avoid
                                                                                                   // CursorIndexOutOfBoundsException
                c_tiles.moveToFirst();
                do { // 12 2197 2750 2203 2754
                    String s_zoom = c_tiles.getString(0);
                    String s_bounds_tiles = c_tiles.getString(1) + "," + c_tiles.getString(2) + "," + c_tiles.getString(3) + ","
                            + c_tiles.getString(4);
                    bounds_min_max.put(s_zoom, s_bounds_tiles);
                    // GPLog.androidLog(-1,"MBTilesDroidSpitter.fetch_bounds_minmax_tiles: sql["+s_zoom+","+s_bounds_tiles+"]");
                } while( c_tiles.moveToNext() );
                c_tiles.close();
            }
        } catch (Exception e) {
            GPLog.androidLog(4, "MBTilesDroidSpitter.fetch_bounds_minmax_tiles:  sql[" + SQL_GET_MINMAXZOOM_TILES + "]", e);
        } finally {
            db_lock.readLock().unlock();
        }
        return bounds_min_max;
    }
    // -----------------------------------------------
    /**
      * Convert zoom/min/max tile number bounds into lat long for each zoom-level
      * - last entry the min-max zoom and the min/max lat/long of all zoom-levels
      * @param bounds_tiles (result of fetch_bounds_minmax_tiles())
      * @return the converted values. ['zoom','min_x,min_y,max_x,max_y']
      */
    public HashMap<String, String> fetch_bounds_minmax_latlong( HashMap<String, String> bounds_tiles, int i_tize_size ) {
        double[] max_bounds = new double[]{180.0, 85.05113, -180.0, -85.05113, 0, 0};
        int i_min_zoom = 22;
        int i_max_zoom = 0;
        int i_zoom_center = 0;
        HashMap<String, String> bounds_lat_long = new LinkedHashMap<String, String>();
        for( Map.Entry<String, String> zoom_bounds : bounds_tiles.entrySet() ) {
            double[] tile_bounds = tile_bounds_to_latlong(zoom_bounds.getKey(), zoom_bounds.getValue(), i_tize_size);
            int i_zoom = Integer.parseInt(zoom_bounds.getKey());
            if (i_zoom < i_min_zoom)
                i_min_zoom = i_zoom;
            if (i_zoom > i_max_zoom)
                i_max_zoom = i_zoom;
            if (tile_bounds[0] < max_bounds[0])
                max_bounds[0] = tile_bounds[0];
            if (tile_bounds[1] < max_bounds[1])
                max_bounds[1] = tile_bounds[1];
            if (tile_bounds[2] > max_bounds[2])
                max_bounds[2] = tile_bounds[2];
            if (tile_bounds[3] > max_bounds[3])
                max_bounds[3] = tile_bounds[3];
            if (((tile_bounds[4] >= max_bounds[0]) && (tile_bounds[4] <= max_bounds[2]))
                    && ((tile_bounds[5] >= max_bounds[1]) && (tile_bounds[5] <= max_bounds[3]))) {
                if (i_zoom_center < i_zoom) { // The center of the highest Zoom-Level will be set as
                                              // center
                    max_bounds[4] = tile_bounds[4];
                    max_bounds[5] = tile_bounds[5];
                    i_zoom_center = i_zoom;
                }
            }
            String s_bounds_tiles = tile_bounds[0] + "," + tile_bounds[1] + "," + tile_bounds[2] + "," + tile_bounds[3] + ";"
                    + tile_bounds[4] + "," + tile_bounds[5];
            bounds_lat_long.put(zoom_bounds.getKey(), s_bounds_tiles);
            // GPLog.androidLog(-1,"MBTilesDroidSpitter.fetch_bounds_minmax_tiles: bounds_lat_long["+i_zoom+","+s_bounds_tiles+"]");
        }
        String s_zoom = i_min_zoom + "," + i_max_zoom + "," + i_zoom_center;
        String s_bounds_tiles = max_bounds[0] + "," + max_bounds[1] + "," + max_bounds[2] + "," + max_bounds[3] + ";"
                + max_bounds[4] + "," + max_bounds[5];
        bounds_lat_long.put(s_zoom, s_bounds_tiles);
        return bounds_lat_long;
    }
    // -----------------------------------------------
    /**
     * Retrieve zoom_level, x_tile,y_tile_osm from tile_id
     * - y_tile_osm is always in osm notation
     * -- 'tms' or 'osm' depending on the setting of the active mbtiles.db
     * @param i_z zoom_level
     * @param i_x x_tile
     * @param i_y_osm y_tile [n osm notation]
     * @return  s_tile_id tile_id to use [z-x-y.osm/tms]
     */
    public String get_tile_id_from_zxy( int i_z, int i_x, int i_y_osm ) {
        int i_y = i_y_osm;
        if (s_tile_row_type.equals("tms")) {
            int[] tmsTileXY = MBTilesDroidSpitter.googleTile2TmsTile(i_x, i_y_osm, i_z);
            i_y = tmsTileXY[1];
        }
        return i_z + "-" + i_x + "-" + i_y + "." + s_tile_row_type; // 'tms' or 'osm';
    }
    // -----------------------------------------------
    /**
      * Converts min/max tile-numbers of zoom level into mix/max lat/long
      *
      * @param s_zoom the zoom level (as string).
      * @param s_tile_bounds format ['min_x,min_y_tms,max_x,max_y_tms'] in tile-numbers of zoom
      * @param i_tize_size tile size [256].
      * @return the converted values. ['min_x,min_y,max_x,max_y']
      */
    public static double[] tile_bounds_to_latlong( String s_zoom, String s_tile_bounds, int i_tize_size ) { // min_x,min_y_tms,max_x,max_y_tms
        String[] sa_splitted = s_tile_bounds.split(",");
        if (sa_splitted.length != 4)
            return null;
        int[] tile_bounds = new int[4];
        int i_zoom = 0;
        try {
            for( int i = 0; i < sa_splitted.length; i++ ) {
                if (i == 0)
                    i_zoom = Integer.parseInt(s_zoom);
                tile_bounds[i] = Integer.parseInt(sa_splitted[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        int i_min_x = tile_bounds[0];
        int i_min_y_tms = tile_bounds[1];
        int i_max_x = tile_bounds[2];
        int i_max_y_tms = tile_bounds[3];
        int[] tms_values = tmsTile2GoogleTile(i_min_x, i_min_y_tms, i_zoom);
        int i_min_y_osm = tms_values[1];
        tms_values = tmsTile2GoogleTile(i_max_x, i_max_y_tms, i_zoom);
        int i_max_y_osm = tms_values[1];
        double[] bounds = tileLatLonBounds(i_min_x, i_min_y_osm, i_zoom, i_tize_size);
        double d_min_x = bounds[0];
        double d_min_y = bounds[1];
        bounds = tileLatLonBounds(i_max_x, i_max_y_osm, i_zoom, i_tize_size);
        double d_max_x = bounds[2];
        double d_max_y = bounds[3];
        double d_center_x = (d_max_x + d_min_x) / 2;
        double d_center_y = (d_max_y + d_min_y) / 2;
        return new double[]{d_min_x, d_min_y, d_max_x, d_max_y, d_center_x, d_center_y};
    }
    // -----------------------------------------------
    /**
      * Converts byte[] to hex String
      *
      * @param ba_data the byte[] to convert
      * @return the hex string to be used to compare with sql 'WHERE (hex(tile_data) = '?')'
      */
    public static String get_hex( byte[] ba_data ) {
        if (ba_data == null) {
            return null;
        }
        final StringBuilder sb_hex = new StringBuilder(2 * ba_data.length);
        Formatter formatter = new Formatter(sb_hex);
        for( final byte b : ba_data ) {
            formatter.format("%02x", b);
        }
        return sb_hex.toString();
    }
    // -----------------------------------------------
    static int[] rb_table = null; // for get_pixel_rgb
    static int[] g_table = null; // get_pixel_rgb
    // -----------------------------------------------
    /**
     * Determin if the Bitmap is blank (i.e. all pixels are of ONE colour)
     * - RGB_565 will be converted to ARGB_8888
     * @param this_bitmap Bitmap to check
     * @param i_parm 0: 'ffeedd' ; 1: 'ff-ee-dd.rgb' [to be used as tile_id]
     * @return  RGB Values of this colour if unique, otherwise empty
     */
    public static String get_pixel_rgb_toString( Bitmap this_bitmap, int i_parm ) {
        String s_rgb = "";
        // ----------------------------------------------
        int[] rgb = get_pixel_rgb(this_bitmap);
        if ((rgb != null) && (rgb.length == 3)) {
            for( int i = 0; i < rgb.length; i++ ) {
                String s_rgb_value = String.format("%02x", (0xFF & rgb[i])); // .toUpperCase();
                if ((i_parm == 1) && (i < (rgb.length - 1)))
                    s_rgb_value = s_rgb_value + "-";
                s_rgb = s_rgb + s_rgb_value;
            }
            if (i_parm == 1)
                s_rgb = s_rgb + ".rgb";
        }
        // ----------------------------------------------
        return s_rgb;
    }
    // -----------------------------------------------
    /**
     * Retrieve zoom_level, x_tile,y_tile_osm from tile_id
     * - y_tile_osm is always in osm notation
     * @param s_tile_id tile_id to use
     * @return  zxy_osm z,x,y_osm values
     */
    public static int[] get_zxy_from_tile_id( String s_tile_id ) {
        int[] zxy_osm = null;
        String[] sa_string = s_tile_id.split("-");
        if (sa_string.length == 3) { // s_tile_id = i_z + "-" + i_x + "-" + i_y + "." +
                                     // s_tile_row_type; // 'tms' or 'osm'
            int i_z = Integer.parseInt(sa_string[0]);
            int i_x = Integer.parseInt(sa_string[1]);
            String s_y = sa_string[2];
            sa_string = s_y.split("\\."); // not .split(".");
            // get_zxy_from_tile_id[17-70427-88057.tms] s_y[88057.tms] [0]
            // GPLog.androidLog(-1,"get_zxy_from_tile_id["+s_tile_id+"] s_y["+s_y+"] ["+sa_string.length+"]");
            if (sa_string.length == 2) {
                int i_y_osm = Integer.parseInt(sa_string[0]);
                int i_y_tms = i_y_osm;
                s_y = sa_string[1];
                zxy_osm = new int[]{i_z, i_x, i_y_osm, i_y_tms};
                if (s_y.equals("tms")) {
                    int[] xy_osm = tmsTile2GoogleTile(i_x, i_y_tms, i_z);
                    if (xy_osm.length == 2) {
                        zxy_osm[2] = xy_osm[1];
                    }
                }
            }
        }
        return zxy_osm;
    }
    // -----------------------------------------------
    /**
     * Determin if the Bitmap is blank (i.e. all pixels are of ONE colour)
     * - RGB_565 will be converted to ARGB_8888
     * @param this_bitmap Bitmap to check
     * @return  RGB Values of this colour, otherwise null
     */
    public static int[] get_pixel_rgb( Bitmap this_bitmap ) {
        int[] rgb = null;
        int i_image_width = this_bitmap.getWidth();
        int i_image_height = this_bitmap.getHeight();
        Bitmap.Config i_bitmap_config = this_bitmap.getConfig();
        int i_R = 0;
        int i_G = 0;
        int i_B = 0;
        // ----------------------------------------------
        for( int x = 0; x < i_image_width; x++ ) {
            for( int y = 0; y < i_image_height; y++ ) {
                int[] pixel_rgb = get_pixel_rgb(i_bitmap_config, this_bitmap.getPixel(x, y));
                if ((pixel_rgb != null) && (pixel_rgb[0] != 0) && (pixel_rgb[1] != 0) && (pixel_rgb[2] != 0)) {
                    if ((x == 0) && (y == 0)) {
                        i_R = pixel_rgb[0];
                        i_G = pixel_rgb[1];
                        i_B = pixel_rgb[2];
                    } else {
                        if ((pixel_rgb[0] != i_R) || (pixel_rgb[1] != i_G) || (pixel_rgb[2] != i_B)) {
                            return rgb; // This image has more than one color, return null
                        }
                    }
                }
            }
        }
        // ----------------------------------------------
        rgb = new int[]{i_R, i_G, i_B}; // This image is of one color, return the RGB Values
        // ----------------------------------------------
        return rgb;
    }
    // -----------------------------------------------
    /**
      * Retrieve RGB value of Bitmap Pixel
      * - RGB_565 will be converted to ARGB_8888
      * @param i_bitmap_config Bitmap.Config of Bitmap
      * @param i_pixel Bitmap.Pixel value
      * @return  RGB Value of this Pixel, otherwise null
      */
    public static int[] get_pixel_rgb( Bitmap.Config i_bitmap_config, int i_pixel ) { // i_image_height
                                                                                      // - i_pixel =
                                                                                      // 0xff000000
                                                                                      // | (rgb[0]
                                                                                      // << 16) |
                                                                                      // (rgb[1] <<
                                                                                      // 8) |
                                                                                      // rgb[2];
        int[] rgb = null;
        switch( i_bitmap_config ) {
        case RGB_565: {
            // Bitmap.Config.RGB_565=4
            if (rb_table == null) {
                int i = 32;
                rb_table = new int[i];
                for( i = 0; i < 32; i++ )
                    rb_table[i] = 255 * i / 31;
                i = 64;
                g_table = new int[i];
                for( i = 0; i < 64; i++ )
                    g_table[i] = 255 * i / 63;
            }
            rgb = new int[]{rb_table[(i_pixel >> 11) & 31] << 16, g_table[(i_pixel >> 5) & 63] << 8, rb_table[i_pixel & 31]};
        }
            break;
        // ALPHA_8, ARGB_4444
        case ARGB_8888: {
            rgb = new int[]{(i_pixel & 0xff0000) >> 16, (i_pixel & 0x00ff00) >> 8, (i_pixel & 0x0000ff) >> 0};
            // ARGB_8888 - i_pixel = 0xff000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
        }
            break;
        }
        return rgb;
        // return new int[]{i_pixel >> 16 & 0xff,i_pixel >> 8 & 0xff,i_pixel >> 0xff};
    }
    // -----------------------------------------------
    // mj10777: copied from
    // - geopaparazzilibrary/src/eu/geopaparazzi/library/util/Utilities.java
    // -----------------------------------------------
    static double originShift = 2 * Math.PI * 6378137 / 2.0;
    /**
      * Converts Google tile coordinates to TMS Tile coordinates.
      *
      * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
      *
      * @param tx the x tile number.
      * @param ty the y tile number. [osm notation]
      * @param zoom the current zoom level.
      * @return the converted values.
      */
    public static int[] googleTile2TmsTile( int tx, int ty, int zoom ) {
        return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
    }
    /**
        * Converts TMS tile coordinates to Google Tile coordinates.
        *
        * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
        *
        * @param tx the x tile number.
        * @param ty the y tile number.  [tms notation]
        * @param zoom the current zoom level.
        * @return the converted values.
        */
    public static int[] tmsTile2GoogleTile( int tx, int ty, int zoom ) {
        return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
    }
    /**
      * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
      *
      * @param tx
      * @param ty  [osm notation]
      * @param zoom
      * @param tileSize
      * @return [minx, miny, maxx, maxy]
      */
    public static double[] tileLatLonBounds( int tx, int ty, int zoom, int tileSize ) {
        double[] bounds = tileBounds(tx, ty, zoom, tileSize);
        double[] mins = metersToLatLon(bounds[0], bounds[1]);
        double[] maxs = metersToLatLon(bounds[2], bounds[3]);
        return new double[]{mins[1], maxs[0], maxs[1], mins[0]};
    }

    /**
     * <p>Code copied from: http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Lon..2Flat._to_tile_numbers </p>
     * 20131128: corrections added to correct going over or under max/min extent
     * - was causing http 400 Bad Requests
     * - updated openstreetmap wiki
     * @param latlong_bounds [position_y,position_x]
     * @param zoom
     * @return [zoom,xtile,ytile_osm]
     */
    public static int[] getTileNumber( final double lat, final double lon, final int zoom ) {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile_osm = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat)))
                / Math.PI)
                / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile_osm < 0)
            ytile_osm = 0;
        if (ytile_osm >= (1 << zoom))
            ytile_osm = ((1 << zoom) - 1);
        return new int[]{zoom, xtile, ytile_osm};
    }

    /**
       * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
       *
       * @param latlong_bounds [minx,miny,maxx,minx]
       * @param i_zoom
       * @return [zoom,minx, miny, maxx, maxy of tile_bounds]
       */
    public static int[] LatLonBounds_to_TileBounds( double[] latlong_bounds, int i_zoom ) {
        int[] min_tile_bounds = getTileNumber(latlong_bounds[1], latlong_bounds[0], i_zoom);
        int[] max_tile_bounds = getTileNumber(latlong_bounds[3], latlong_bounds[2], i_zoom);
        return new int[]{i_zoom, min_tile_bounds[1], min_tile_bounds[2], max_tile_bounds[1], max_tile_bounds[2]};
    }
    /**
      * <p>Code adapted from: LatLonBounds_to_TileBounds</p>
      *
      * @param tile_bounds [minx, miny_osm, maxx, maxy_osm of tile_bounds]
      * @param i_zoom
      * @return [zoom,minx, miny, maxx, maxy of tile_bounds]
      * @return latlong_bounds [minx,miny,maxx,minx]
      */
    public static double[] TileBounds_to_LatLonBounds( int[] tile_bounds, int i_zoom ) {
        int i_min_x = tile_bounds[0];
        int i_min_y_osm = tile_bounds[1];
        int i_max_x = tile_bounds[2];
        int i_max_y_osm = tile_bounds[3];
        double[] bounds = tileLatLonBounds(i_min_x, i_min_y_osm, i_zoom, 256);
        double d_min_x = bounds[0];
        double d_min_y = bounds[1];
        bounds = tileLatLonBounds(i_max_x, i_max_y_osm, i_zoom, 256);
        double d_max_x = bounds[2];
        double d_max_y = bounds[3];
        return new double[]{d_min_x, d_min_y, d_max_x, d_max_y};
    }
    /**
      * Returns bounds of the given tile in EPSG:900913 coordinates
      *
      * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
      *
      * @param tx
      * @param ty
      * @param zoom
      * @return [minx, miny, maxx, maxy]
      */
    public static double[] tileBounds( int tx, int ty, int zoom, int tileSize ) {
        double[] min = pixelsToMeters(tx * tileSize, ty * tileSize, zoom, tileSize);
        double minx = min[0], miny = min[1];
        double[] max = pixelsToMeters((tx + 1) * tileSize, (ty + 1) * tileSize, zoom, tileSize);
        double maxx = max[0], maxy = max[1];
        return new double[]{minx, miny, maxx, maxy};
    }
    /**
     * Converts pixel coordinates in given zoom level of pyramid to EPSG:900913
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @return
     */
    public static double[] pixelsToMeters( double px, double py, int zoom, int tileSize ) {
        double res = getResolution(zoom, tileSize);
        double mx = px * res - originShift;
        double my = py * res - originShift;
        return new double[]{mx, my};
    }
    /**
     * Converts XY point from Spherical Mercator EPSG:900913 to lat/lon in WGS84
     * Datum
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @return
     */
    public static double[] metersToLatLon( double mx, double my ) { // double originShift = 2 *
                                                                    // Math.PI * 6378137 / 2.0;
        double lon = (mx / originShift) * 180.0;
        double lat = (my / originShift) * 180.0;
        lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return new double[]{-lat, lon};
    }
    /**
     * Resolution (meters/pixel) for given zoom level (measured at Equator)
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @return
     */
    public static double getResolution( int zoom, int tileSize ) {
        // return (2 * Math.PI * 6378137) / (this.tileSize * 2**zoom)
        double initialResolution = 2 * Math.PI * 6378137 / tileSize;
        return initialResolution / Math.pow(2, zoom);
    }
}
