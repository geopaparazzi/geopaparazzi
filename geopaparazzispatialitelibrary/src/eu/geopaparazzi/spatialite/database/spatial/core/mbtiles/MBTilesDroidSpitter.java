/** @author Simon Thépot aka djcoin <simon.thepot@gmail.com, simon.thepot@makina-corpus.com>
  * adapted to create and fill mbtiles databases Mark Johnson (www.mj10777.de)
 */
package eu.geopaparazzi.spatialite.database.spatial.core.mbtiles;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MbTilesMetadata.MetadataParseException;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MbTilesMetadata.MetadataValidator;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;

public class MBTilesDroidSpitter
{
 private SQLiteDatabase db_mbtiles=null;
 private File file_mbtiles;
 private MbTilesMetadata metadata=null;
 private String s_metadataVersion="1.1";
 private String s_tile_row_type="tms";
 private HashMap<String,String> mbtiles_metadata = null;
 // -----------------------------------------------
 /**
   * Constructor MBTilesDroidSpitter
   * - if the file does not exist, a valid mbtile database will be created
   * - if the parent directory does not exist, it will be created
   * @param file_mbtiles mbtiles.db file to open
   * @param mbtiles_metadata list of initial metadata values to set upon creation [otherwise can be null]
   */
 public MBTilesDroidSpitter(File file_mbtiles,HashMap<String,String> mbtiles_metadata)
 {
  this.file_mbtiles = file_mbtiles;
  if (mbtiles_metadata == null)
   this.mbtiles_metadata = new LinkedHashMap<String,String>();
  else
   this.mbtiles_metadata = mbtiles_metadata;
  if (!this.file_mbtiles.exists())
  { // if the parent directory does not exist, it will be created
    // - a mbtiles database will be created with default values and closed
   try
   {
    create_mbtiles(this.file_mbtiles);
   }
   catch (IOException e)
   {
    SpatialDatabasesManager.app_log(3,"["+file_mbtiles.getName()+"] "+e.getMessage(),e);
   }
  }
 }
 // -----------------------------------------------
 /**
   * Open mbtiles Database
   * @param fetchMetadata 1: fetch and load the mbtiles metaadata
   * @return void
   */
 public void open(boolean fetchMetadata,String metadataVersion)
 {
  if (metadataVersion != "")
   this.s_metadataVersion=metadataVersion;
  db_mbtiles = SQLiteDatabase.openOrCreateDatabase(file_mbtiles, null);
  if (!fetchMetadata)
   return;
  try
  {
   fetchMetadata(this.s_metadataVersion);
  }
  catch (MetadataParseException e)
  {
   String s_stackTrace = "["+file_mbtiles.getName()+"] "+e.getMessage()+"\n"+Log.getStackTraceString(e);
   SpatialDatabasesManager.app_log(3,s_stackTrace);
  }
 }
 // -----------------------------------------------
 /**
   * Close mbtiles Database
   * @return void
   */
 public void close()
 {
  db_mbtiles.close();
 }
 // -----------------------------------------------
 /**
   * Retrieve SQLiteDatabase connection
   * @return SQLiteDatabase connection of mbtiles.db
   */
 public SQLiteDatabase getmbtiles()
 {
  return db_mbtiles;
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
 public Drawable getTileAsDrawable(int i_x,int i_y_osm,int i_z)
 {
  return new BitmapDrawable(getTileAsBitmap(i_x,i_y_osm,i_z));
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
 public Bitmap getTileAsBitmap(int i_x,int i_y_osm,int i_z)
 {
  // TODO: Optimize this if we have mbtiles_metadata with bound or min/max zoomlevels
  // Do not make any request and return null if we know it won't match any tile
  byte[] bb = getTileAsBytes(i_x,i_y_osm,i_z);
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
 public byte[] getTileAsBytes(int i_x,int i_y_osm,int i_z)
 {
  int i_y=i_y_osm;
  String s_x="";
  String s_y="";
  String s_z="";
  if (s_tile_row_type.equals("tms"))
  {
   int[] tmsTileXY = MBTilesDroidSpitter.googleTile2TmsTile(i_x,i_y_osm,i_z);
   i_y=tmsTileXY[1];
  }
  try
  {
   s_x=Integer.toString(i_x);
   s_y=Integer.toString(i_y);
   s_z=Integer.toString(i_z);
  }
  catch (NumberFormatException e)
  {
   return null;
  }
  final Cursor c = db_mbtiles.rawQuery("select tile_data from tiles where tile_column=? and tile_row=? and zoom_level=?", new String[]{s_x,s_y,s_z});
  if (!c.moveToFirst())
  {
   c.close();
   return null;
  }
  byte[] bb = c.getBlob(c.getColumnIndex("tile_data"));
  c.close();
  return bb;
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
   * @param i_fetch_bounds 1=force a calculation of the bounds and min/max zoom levels
   * @return 0: correct, otherwise error
   */
 public int insertBitmapTile(int i_x,int i_y_osm,int i_z,Bitmap tile_bitmap,int i_force_unique,int i_fetch_bounds) throws IOException
 { // i_rc=0: correct, otherwise error
  int i_rc=0;
  // i_parm=1: 'ff-ee-dd.rgb' [to be used as tile_id], blank if image is not Blank (all pixels use one RGB value)
  String s_tile_id=get_pixel_rgb_toString(tile_bitmap,1);
  ByteArrayOutputStream ba_stream = new ByteArrayOutputStream();
  try
  {
   if (this.mbtiles_metadata.get("format") == "png")
   { // 'png' should be avoided, can create very big databases
    tile_bitmap.compress(Bitmap.CompressFormat.PNG,100,ba_stream);
   }
   else
   { // 'jpg' should be used where possible
    tile_bitmap.compress(Bitmap.CompressFormat.JPEG,75,ba_stream);
   }
   byte[] ba_tile_data =ba_stream.toByteArray();
   i_rc=insertTile(s_tile_id,i_x,i_y_osm,i_z,ba_tile_data,i_force_unique,i_fetch_bounds);
  }
  catch (Exception e)
  {
   i_rc=1;
   SpatialDatabasesManager.app_log(3,"["+file_mbtiles.getName()+"] "+e.getMessage(),e);
  }
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
   * @param i_fetch_bounds 1=force a calculation of the bounds and min/max zoom levels
   * @return 0: no error
   */
 private int insertTile(String s_tile_id,int i_x,int i_y_osm,int i_z,byte[] ba_tile_data,int i_force_unique,int i_fetch_bounds) throws IOException
 { // i_rc=0: correct, otherwise error
  int i_rc=0;
  int i_y=i_y_osm;
  if (s_tile_row_type.equals("tms"))
  {
   int[] tmsTileXY = MBTilesDroidSpitter.googleTile2TmsTile(i_x,i_y_osm,i_z);
   i_y=tmsTileXY[1];
  }
  boolean b_unique=true;
  if (i_force_unique > 1)
   i_force_unique=0;
  String s_images_tablename="images";
  String s_map_tablename="map";
  String s_mbtiles_field_tile_id="tile_id";
  String s_mbtiles_field_grid_id="grid_id";
  String s_mbtiles_field_tile_data="tile_data";
  if (s_tile_id == "")
  {
   s_tile_id=i_z+"-"+i_x+"-"+i_y+"."+s_tile_row_type; // 'tms' or 'osm'
  }
  else
  { //  This should be a 'Blank' Image :'ff-ee-dd.rgb', check if allready stored in 'images' table
   String s_sql_query="SELECT count(tile_id) AS count_tile_id FROM images WHERE (tile_id = '"+s_tile_id+"')";
   // mj10777: A good wms-server to test 'blank-images' (i.e. all pixels of image have the same RGB) is:
   // http://fbinter.stadt-berlin.de/fb/wms/senstadt/ortsteil?LAYERS=0&FORMAT=image/jpeg&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=visual&SRS=EPSG:4326&BBOX=XXX,YYY,XXX,YYY&WIDTH=256&HEIGHT=256
   // SELECT count(tile_id) FROM images where tile_id like '%.rgb'  : 8
   // SELECT count(tile_id) FROM map where tile_id like '%.rgb'  : 177
   try
   {
    final Cursor c = db_mbtiles.rawQuery(s_sql_query,null);
    if (c != null)
    {
     if (c.moveToFirst())
     {
      int i_count_tile_id = c.getInt(c.getColumnIndex("count_tile_id"));
      if (i_count_tile_id > 0)
      { // We have this image, do not add again
       b_unique=false;
      }
     }
     c.close();
    }
   }
   catch (Exception e)
   {
    i_rc=1;
    throw new IOException("MBTilesDroidSpitter:insertTile query["+s_sql_query+"] error["+e.getLocalizedMessage()+"] ");
   }
  }
  String s_mbtiles_field_zoom_level="zoom_level";
  String s_mbtiles_field_tile_column="tile_column";
  String s_mbtiles_field_tile_row="tile_row";
  String s_grid_id="";
  // The use of 'i_force_unique == 1' will probely slow things down to a craw
  // SpatialDatabasesManager.app_log(1,"insertTile  tile_id["+s_tile_id+"] force_unique["+i_force_unique+"] unique["+b_unique+"]");
  if ((i_force_unique == 1) && (b_unique))
  { // mj10777: not yet properly tested:
   // - query the images table, searching for 'ba_tile_data'
   // -- if found:
   // --- set  'b_unique=false;'
   // --- replace 's_tile_id' with images.tile_id of found record
   String s_tile_id_query="";
   try
   {
    s_tile_id_query=search_tile_image(ba_tile_data);
   }
   catch (Exception e)
   {
    i_rc=1;
   }
   if (s_tile_id_query != "")
   { // We have this image, do not add again
    b_unique=false;
    // replace the present tile_id with the found referenced tile_id
    // the 'map' table will now reference the existing image in 'images'
    s_tile_id=s_tile_id_query;
   }
  }
  db_mbtiles.beginTransaction();
  try
  {
   if (b_unique)
   { // We do not have this image, add it
    ContentValues image_values = new ContentValues();
    image_values.put(s_mbtiles_field_tile_data,ba_tile_data);
    image_values.put(s_mbtiles_field_tile_id,s_tile_id);
    db_mbtiles.insertOrThrow(s_images_tablename,null,image_values);
   }
   // Note: the 'map' table will/should only reference an existing image in the 'images'. table
   // - it is possible that there is more than one reference to an existing image
   // -- sample: an area has 15 tiles of one color (all pixels of the tile have the same RGB)
   // --- this image will be stored 1 time in 'images', but will be used 15 times in 'map'
   ContentValues map_values = new ContentValues();
   map_values.put(s_mbtiles_field_zoom_level,i_z);
   map_values.put(s_mbtiles_field_tile_column,i_x);
   map_values.put(s_mbtiles_field_tile_row,i_y);
   map_values.put(s_mbtiles_field_tile_id,s_tile_id);
   map_values.put(s_mbtiles_field_grid_id,s_grid_id);
   db_mbtiles.insertOrThrow(s_map_tablename,null,map_values);
   db_mbtiles.setTransactionSuccessful();
  }
  catch (Exception e)
  {
   i_rc=1;
   throw new IOException("MBTilesDroidSpitter:insertTile error["+e.getLocalizedMessage()+"]");
  }
  finally
  {
   db_mbtiles.endTransaction();
   int i_update=1;
   try
   { // if the bounds or min/max zoom have changed, update changed values and reload metadata
    i_update=checkBounds(i_x,i_y_osm,i_z,i_update,i_fetch_bounds);
    // i_update=0: inside bounds ; othewise bounds and metadata have changed - not used
   }
   catch (Exception e)
   {
    i_rc=1;
   }
  }
  return i_rc;
 }
 // -----------------------------------------------
 /**
   * Function to check if image exists in the image-table
   * - avoids duplicate images
   * @param ba_tile_data the image-data extracted from the Bitmap.
   * @return tile_id of found image or blank
   */
 private String search_tile_image(byte[] ba_tile_data) throws IOException
 { // The use of 'i_force_unique == 1' will probely slow things down to a craw
  String s_tile_id="";
  String s_tile_data=get_hex(ba_tile_data);
  String s_sql_query="SELECT tile_id FROM images WHERE (hex(tile_data) = '"+s_tile_data+"')";
  try
  {    // ?? another way to query for binary data in java ??
   final Cursor c = db_mbtiles.rawQuery(s_sql_query,null);
   if (c != null)
   {
    if (c.moveToFirst())
    { // TODO: do something if multiple results are returned
     s_tile_id=c.getString(c.getColumnIndex("tile_id"));
    }
    c.close();
   }
   //
  }
  catch (Exception e)
  {
   throw new IOException("MBTilesDroidSpitter:search_tile_image query["+s_sql_query+"] error["+e.getLocalizedMessage()+"] ");
  }
  if (s_tile_id != "")
  {
   SpatialDatabasesManager.app_log(1,"MBTilesDroidSpitter:s earch_tile_image["+file_mbtiles.getName()+"]  tile_id["+s_tile_id+"] [a non-blank unique image has been found]");
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
   * @param i_fetch_bounds 1=force a calculation of the bounds and min/max zoom levels
   * @return 1: metdata was updated
   */
 public int checkBounds(int i_x,int i_y_osm,int i_z,int i_update,int i_fetch_bounds) throws IOException
 {
  int i_rc=0;
  if (i_fetch_bounds == 1)
  {
   try
   {
    fetch_bounds_minmax(1,i_update);
   }
   catch (Exception e)
   {
    i_rc=1;
    SpatialDatabasesManager.app_log(3,"["+file_mbtiles.getName()+"] "+e.getMessage(),e);
    // e.printStackTrace()
   }
   return i_rc;
  }
  // minx, miny, maxx, maxy
  double[] tileBounds = tileLatLonBounds(i_x,i_y_osm,i_z,256);
  HashMap<String,String> update_metadata = this.metadata.checkTileLocation(tileBounds,i_z);
  if (update_metadata.size() > 0)
  {
   if (i_update == 1)
   { // the bounds or min/max zoom have changed, update changed values and reload metadata
    int i_reload_metadata=1; // call fetchMetadata so that the new values will take effect
    try
    {
     update_mbtiles_metadata(db_mbtiles,update_metadata,i_reload_metadata);
    }
    catch (Exception e)
    {
     SpatialDatabasesManager.app_log(3,"["+file_mbtiles.getName()+"] "+e.getMessage(),e);
    }
   }
   i_rc=1;
  }
  return i_rc;
 }
 // -----------------------------------------------
 /**
   * Query the mbtile metadata-table and returns validated results
   * @return HashMap<String,String> metadate [key,value]
   */
 public MbTilesMetadata fetchMetadata(String metadataVersion) throws MetadataParseException
 {
  Cursor c = db_mbtiles.query(MbTilesSQLite.TABLE_METADATA, new String[]{MbTilesSQLite.COL_METADATA_NAME,
             MbTilesSQLite.COL_METADATA_VALUE}, null, null, null, null, null);
  MetadataValidator validator = MbTilesMetadata.MetadataValidatorFactory.getMetadataValidatorFromVersion(metadataVersion);
  if (validator == null)
   return null;
  this.metadata = MbTilesMetadata.createFromCursor(c,c.getColumnIndex(MbTilesSQLite.COL_METADATA_NAME),c.getColumnIndex(MbTilesSQLite.COL_METADATA_VALUE), validator);
  this.s_tile_row_type=this.metadata.s_tile_row_type;
  return this.metadata;
 }
 // -----------------------------------------------
 /**
   * Returns result of last called fetchMetadata
   * @return HashMap<String,String> metadate [key,value]
   */
 public MbTilesMetadata getMetadata()
 {
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
 public int create_mbtiles(File file_mbtiles)  throws IOException
 {
  int i_rc=0;
  File dir_mbtiles = file_mbtiles.getParentFile();
  String mbtiles_name=file_mbtiles.getName().substring(0,file_mbtiles.getName().lastIndexOf("."));
  if (!dir_mbtiles.exists())
  {
   if (!dir_mbtiles.mkdir())
   {
    throw new IOException("MBTilesDroidSpitter: create_mbtiles: mbtiles_dir["+dir_mbtiles.getAbsolutePath()+"] creation failed");
   }
  }
  SQLiteDatabase sqlite_db = SQLiteDatabase.openOrCreateDatabase(file_mbtiles, null);
  if (sqlite_db != null)
  {
   sqlite_db.setLocale(Locale.getDefault());
   sqlite_db.setLockingEnabled(false);
   // CREATE TABLES and default values
   try
   { // create default tables for mbtiles
     // set default values for metadata, where not supplied in 'this.mbtiles_metadata'
    create_mbtiles_tables(sqlite_db,mbtiles_name);
    // write all metatdata values to database (with 'null', 'this.mbtiles_metadata' will be used)
    int i_reload_metadata=0; // do not reload, since we are closing the db
    update_mbtiles_metadata(sqlite_db,null,i_reload_metadata);
   }
   catch (Exception e)
   {
    sqlite_db.close();
    sqlite_db = null;
    SpatialDatabasesManager.app_log(3,"["+file_mbtiles.getName()+"] "+e.getMessage(),e);
    i_rc=2;
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
 public int create_mbtiles_tables(SQLiteDatabase mbtiles_db,String mbtiles_name) throws IOException
 {
  int i_rc=0;
  String s_mbtiles_field_tile_data="tile_data";
  String s_mbtiles_field_zoom_level="zoom_level";
  String s_mbtiles_field_tile_column="tile_column";
  String s_mbtiles_field_tile_row="tile_row";
  String s_mbtiles_field_tile_id="tile_id";
  String s_mbtiles_field_grid_id="grid_id";
  String s_mbtiles_field_name="name";
  String s_mbtiles_field_value="value";
  String s_metadata_tablename="metadata";
  String s_images_tablename="images";
  String s_tiles_tablename="tiles";
  String s_map_tablename="map";
  //-----------------------------------------------
  String s_sql_create_grid_key="CREATE TABLE IF NOT EXISTS grid_key ("+s_mbtiles_field_grid_id+" TEXT,key_name TEXT)";
  String s_sql_create_grid_utfgrid="CREATE TABLE IF NOT EXISTS grid_utfgrid ("+s_mbtiles_field_grid_id+" TEXT,grid_utfgrid BLOB)";
  String s_sql_create_images="CREATE TABLE IF NOT EXISTS "+s_images_tablename+" ("+s_mbtiles_field_tile_data+" blob,"+s_mbtiles_field_tile_id+" text)";
  String s_sql_create_keymap="CREATE TABLE IF NOT EXISTS keymap (key_name TEXT,key_json TEXT)";
  String s_sql_create_map="CREATE TABLE IF NOT EXISTS "+s_map_tablename+" ("+s_mbtiles_field_zoom_level+" INTEGER,"+s_mbtiles_field_tile_column+" INTEGER,"+s_mbtiles_field_tile_row+" INTEGER,"+s_mbtiles_field_tile_id+" TEXT,"+s_mbtiles_field_grid_id+" TEXT)";
  String s_sql_create_metadata="CREATE TABLE IF NOT EXISTS "+s_metadata_tablename+" ("+s_mbtiles_field_name+" text,"+s_mbtiles_field_value+" text)";
  // CREATE VIEW tiles AS SELECT map.zoom_level AS zoom_level,map.tile_column AS tile_column,map.tile_row AS tile_row,images.tile_data AS tile_data FROM map JOIN images ON images.tile_id = map.tile_id ORDER BY zoom_level,tile_column,tile_row
  String s_sql_create_view_tiles="CREATE VIEW IF NOT EXISTS "+s_tiles_tablename+" AS SELECT "+s_map_tablename+"."+s_mbtiles_field_zoom_level+" AS "+s_mbtiles_field_zoom_level+","+s_map_tablename+"."+s_mbtiles_field_tile_column+" AS "+s_mbtiles_field_tile_column+","+s_map_tablename+"."+s_mbtiles_field_tile_row+" AS "+s_mbtiles_field_tile_row+","+s_images_tablename+"."+s_mbtiles_field_tile_data+" AS "+s_mbtiles_field_tile_data+" FROM "+s_map_tablename+" JOIN "+s_images_tablename+" ON "+s_images_tablename+"."+s_mbtiles_field_tile_id+" = "+s_map_tablename+"."+s_mbtiles_field_tile_id+" ORDER BY "+s_mbtiles_field_zoom_level+","+s_mbtiles_field_tile_column+","+s_mbtiles_field_tile_row;
  String s_sql_create_view_grids="CREATE VIEW IF NOT EXISTS grids AS SELECT "+s_map_tablename+"."+s_mbtiles_field_zoom_level+" AS "+s_mbtiles_field_zoom_level+","+s_map_tablename+"."+s_mbtiles_field_tile_column+" AS "+s_mbtiles_field_tile_column+","+s_map_tablename+"."+s_mbtiles_field_tile_row+" AS "+s_mbtiles_field_tile_row+",grid_utfgrid.grid_utfgrid AS grid FROM "+s_map_tablename+" JOIN grid_utfgrid ON grid_utfgrid."+s_mbtiles_field_grid_id+" = "+s_map_tablename+"."+s_mbtiles_field_grid_id;
  String s_sql_create_view_grid_data="CREATE VIEW IF NOT EXISTS grid_data AS SELECT "+s_map_tablename+"."+s_mbtiles_field_zoom_level+" AS "+s_mbtiles_field_zoom_level+","+s_map_tablename+"."+s_mbtiles_field_tile_column+" AS "+s_mbtiles_field_tile_column+","+s_map_tablename+"."+s_mbtiles_field_tile_row+" AS "+s_mbtiles_field_tile_row+",keymap.key_name AS key_name,keymap.key_json AS key_json FROM "+s_map_tablename+" JOIN grid_key ON "+s_map_tablename+"."+s_mbtiles_field_grid_id+" = grid_key."+s_mbtiles_field_grid_id+" JOIN keymap ON grid_key.key_name = keymap.key_name";
  String s_sql_create_index_grid_key_lookup="CREATE UNIQUE INDEX IF NOT EXISTS grid_key_lookup ON grid_key ("+s_mbtiles_field_grid_id+",key_name)";
  String s_sql_create_index_grid_utfgrid_lookup="CREATE UNIQUE INDEX IF NOT EXISTS grid_utfgrid_lookup ON grid_utfgrid ("+s_mbtiles_field_grid_id+")";
  String s_sql_create_index_images="CREATE UNIQUE INDEX IF NOT EXISTS "+s_images_tablename+"_id ON "+s_images_tablename+" ("+s_mbtiles_field_tile_id+" )";
  String s_sql_create_index_keymap_lookup="CREATE UNIQUE INDEX IF NOT EXISTS keymap_lookup ON keymap (key_name)";
  String s_sql_create_index_map="CREATE UNIQUE INDEX IF NOT EXISTS "+s_map_tablename+"_index ON "+s_map_tablename+" ("+s_mbtiles_field_zoom_level+","+s_mbtiles_field_tile_column+","+s_mbtiles_field_tile_row+")";
  String s_sql_create_index_metadata="CREATE UNIQUE INDEX IF NOT EXISTS "+s_metadata_tablename+"_index ON "+s_metadata_tablename+" ("+s_mbtiles_field_name+")";
  // String s_sql_create_android_metadata="CREATE TABLE IF NOT EXISTS android_"+s_metadata_tablename+" (locale text)";
  // mj10777: not needed in android - done with sqlite_db.setLocale(Locale.getDefault());
  // ----------------------------------------------
  mbtiles_db.beginTransaction();
  try
  {
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
  }
  catch (Exception e)
  {
   i_rc=1;
   throw new IOException("MBTilesDroidSpitter: create_mbtiles_tables error["+e.getLocalizedMessage()+"]");
  }
  finally
  {
   mbtiles_db.endTransaction();
  }
  // ----------------------------------------------
  if (this.mbtiles_metadata.get("type") == null)
   this.mbtiles_metadata.put("type","baselayer");
  if (this.mbtiles_metadata.get("tile_row_type") == null)
  {
   this.mbtiles_metadata.put("tile_row_type",s_tile_row_type);
  }
  else
  {
   this.s_tile_row_type=this.mbtiles_metadata.get("tile_row_type");
  }
  if (this.mbtiles_metadata.get("version") == null)
   this.mbtiles_metadata.put("version","1.1");
  if (this.mbtiles_metadata.get("format") == null)
   this.mbtiles_metadata.put("format","jpg");
  if (this.mbtiles_metadata.get("name") == null)
   this.mbtiles_metadata.put("name",mbtiles_name);
  if (this.mbtiles_metadata.get("description") == null)
   this.mbtiles_metadata.put("description",mbtiles_name);
  if (this.mbtiles_metadata.get("bounds") == null)
   this.mbtiles_metadata.put("bounds","-180.0,-85.05113,180.0,85.05113");
  if (this.mbtiles_metadata.get("center") == null)
   this.mbtiles_metadata.put("center","0.0,0.0,1");
  if (this.mbtiles_metadata.get("minzoom") == null)
   this.mbtiles_metadata.put("minzoom","1");
  if (this.mbtiles_metadata.get("maxzoom") == null)
   this.mbtiles_metadata.put("maxzoom","1");
  // ----------------------------------------------
  return i_rc;
 }
 // -----------------------------------------------
 /**
   * General Function to update mbtiles metadata Table
   * @param mbtiles_db Database connection [upon creation, this is a local variable, otherwise the class variable]
   * @param mbtiles_metadata list of key,values to update. [fill this with valued that need to be added/changed]
   * @param i_reload_metadata reload values after update [not needed upon creation, update after bounds/center/zoom changes]
   * @return 0: no error
   */
 public int update_mbtiles_metadata(SQLiteDatabase mbtiles_db,HashMap<String,String> mbtiles_metadata,int i_reload_metadata) throws IOException
 { // i_rc=0: no error
  int i_rc=0;
  if (mbtiles_metadata == null)
   mbtiles_metadata = this.mbtiles_metadata;
  String s_metadata_tablename="metadata";
  // ----------------------------------------------
  mbtiles_db.beginTransaction();
  try
  {
   Set<Entry<String,String>> dataSet = mbtiles_metadata.entrySet();
   for(Entry<String,String> data : dataSet)
   {
    String s_tile_insert_metadata="INSERT OR REPLACE INTO '"+s_metadata_tablename+"' VALUES('"+data.getKey()+"','"+data.getValue()+"')";
    mbtiles_db.execSQL(s_tile_insert_metadata);
   }
   mbtiles_db.setTransactionSuccessful();
  }
  catch (Exception e)
  {
   i_rc=1;
   throw new IOException("MBTilesDroidSpitter:update_mbtiles_metadata error["+e.getLocalizedMessage()+"]");
  }
  finally
  {
   mbtiles_db.endTransaction();
   if (i_reload_metadata == 1)
   { // should be done when bounds of min/max zoom has changed
    try
    {
     fetchMetadata(this.s_metadataVersion);
    }
    catch (Exception e)
    {
     i_rc=1;
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
 public HashMap<String,String> fetch_bounds_minmax(int i_reload_metadata,int i_update)
 {
  HashMap<String,String> update_metadata = new LinkedHashMap<String, String>();
  HashMap<String,String> bounds_min_max = fetch_bounds_minmax_tiles();
  if (bounds_min_max.size() > 0)
  {
   HashMap<String,String> bounds_lat_long = fetch_bounds_minmax_latlong(bounds_min_max,256);
   if (bounds_lat_long.size() > 0)
   { // how to retrieve that last value only?
    String s_zoom_min_max="";
    String s_bounds_tiles="";
    Set<Entry<String,String>> zoom_bounds = bounds_lat_long.entrySet();
    for(Entry<String,String> zoom_levels : zoom_bounds)
    {
     s_zoom_min_max=zoom_levels.getKey();
     s_bounds_tiles=zoom_levels.getValue();
    }
    if ((s_bounds_tiles != "") && (s_bounds_tiles != ""))
    {
     String[] sa_splitted = s_zoom_min_max.split(",");
     if (sa_splitted.length == 2)
     { // only the last record (with min/max zoom) will be used
      String s_minzoom=sa_splitted[0];
      String s_maxzoom=sa_splitted[1];
      if ((s_minzoom != "") && (s_maxzoom != ""))
      {
       sa_splitted = s_bounds_tiles.split(",");
       if (sa_splitted.length == 4)
       {
        update_metadata.put("bounds",s_bounds_tiles);
        update_metadata.put("minzoom",s_minzoom);
        update_metadata.put("maxzoom",s_maxzoom);
        // SpatialDatabasesManager.app_log(1,"fetch_bounds_minmax  bounds["+s_bounds_tiles+"] minzoom["+s_minzoom+"] maxzoom["+s_maxzoom+"]");
       }
      }
     }
    }
   }
  }
  if ((i_update == 1) && (update_metadata.size() > 0))
  {
   try
   {
    update_mbtiles_metadata(db_mbtiles,update_metadata,i_reload_metadata);
   }
   catch (Exception e)
   {
    SpatialDatabasesManager.app_log(3,"["+file_mbtiles.getName()+"] "+e.getMessage(),e);
   }
  }
  return update_metadata;
 }
 // -----------------------------------------------
 /**
    * Retrieve min/max tiles for each zoom-level from mbtiles
   * - no checking for possible 'holes' inside zoom-level are done
   * @return the retrieved values. ['zoom','min_x,min_y,max_x,max_y']
   */
 public HashMap<String,String> fetch_bounds_minmax_tiles()
 {
  HashMap<String,String> bounds_min_max = new LinkedHashMap<String, String>();
  final String SQL_GET_MINMAXZOOM_TILES = "SELECT zoom_level,min(tile_column) AS min_x,min(tile_row) AS min_y,max(tile_column) AS max_x,max(tile_row) AS max_y FROM tiles WHERE zoom_level IN(SELECT DISTINCT zoom_level FROM tiles ORDER BY zoom_level ASC) GROUP BY zoom_level";
  final Cursor c = db_mbtiles.rawQuery(SQL_GET_MINMAXZOOM_TILES,null);
  c.moveToFirst();
  do
  { // 12 2197 2750 2203 2754
   String s_zoom=c.getString(0);
   String s_bounds_tiles=c.getString(1)+","+c.getString(2)+","+c.getString(3)+","+c.getString(4);
   bounds_min_max.put(s_zoom,s_bounds_tiles);
  } while(c.moveToNext());
  c.close();
  return bounds_min_max;
 }
 // -----------------------------------------------
 /**
   * Convert zoom/min/max tile number bounds into lat long for each zoom-level
   * - last entry the min-max zoo and the min/max lat/long of all zoom-levels
   * @param bounds_tiles (result of fetch_bounds_minmax_tiles())
   * @return the converted values. ['zoom','min_x,min_y,max_x,max_y']
   */
 public HashMap<String,String> fetch_bounds_minmax_latlong(HashMap<String, String> bounds_tiles,int i_tize_size)
 {
  double[] max_bounds=new double[]{180.0,85.05113,-180.0,-85.05113};
  int i_min_zoom=22;
  int i_max_zoom=0;
  HashMap<String,String> bounds_lat_long = new LinkedHashMap<String, String>();
  Set<Entry<String,String>> dataset_tiles = bounds_tiles.entrySet();
  for(Entry<String,String> zoom_bounds : dataset_tiles)
  {
   double[] tile_bounds=tile_bounds_to_latlong(zoom_bounds.getKey(),zoom_bounds.getValue(),i_tize_size);
   int i_zoom= Integer.parseInt(zoom_bounds.getKey());
   if (i_zoom < i_min_zoom)
    i_min_zoom=i_zoom;
   if (i_zoom > i_max_zoom)
    i_max_zoom=i_zoom;
   if (tile_bounds[0] < max_bounds[0])
    max_bounds[0]=tile_bounds[0];
   if (tile_bounds[1] < max_bounds[1])
    max_bounds[1]=tile_bounds[1];
   if (tile_bounds[2] > max_bounds[2])
    max_bounds[2]=tile_bounds[2];
   if (tile_bounds[3] > max_bounds[3])
    max_bounds[3]=tile_bounds[3];
   String s_bounds_tiles=tile_bounds[0]+","+tile_bounds[1]+","+tile_bounds[2]+","+tile_bounds[3];
   bounds_lat_long.put(zoom_bounds.getKey(),s_bounds_tiles);
  }
  String s_zoom=i_min_zoom+","+i_max_zoom;
  String s_bounds_tiles=max_bounds[0]+","+max_bounds[1]+","+max_bounds[2]+","+max_bounds[3];
  bounds_lat_long.put(s_zoom,s_bounds_tiles);
  return bounds_lat_long;
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
 public static double[] tile_bounds_to_latlong(String s_zoom,String s_tile_bounds,int i_tize_size)
 { // min_x,min_y_tms,max_x,max_y_tms
  String[] sa_splitted = s_tile_bounds.split(",");
  if (sa_splitted.length != 4)
   return null;
  int[] tile_bounds = new int[4];
  int i_zoom=0;
  try
  {
   for(int i=0;i<sa_splitted.length;i++)
   {
    if (i == 0)
     i_zoom= Integer.parseInt(s_zoom);
    tile_bounds[i] = Integer.parseInt(sa_splitted[i]);
   }
  }
  catch (NumberFormatException e)
  {
   return null;
  }
  int i_min_x=tile_bounds[0];
  int i_min_y_tms=tile_bounds[1];
  int i_max_x=tile_bounds[2];
  int i_max_y_tms=tile_bounds[3];
  int[] tms_values = tmsTile2GoogleTile(i_min_x,i_min_y_tms,i_zoom);
  int i_min_y_osm=tms_values[1];
  tms_values = tmsTile2GoogleTile(i_max_x,i_max_y_tms,i_zoom);
  int i_max_y_osm=tms_values[1];
  double[] bounds = tileLatLonBounds(i_min_x,i_min_y_osm,i_zoom,i_tize_size);
  double d_min_x=bounds[0];
  double d_min_y=bounds[1];
  bounds = tileLatLonBounds(i_max_x,i_max_y_osm,i_zoom,i_tize_size);
  double d_max_x=bounds[2];
  double d_max_y=bounds[3];
  return new double[]{d_min_x,d_min_y,d_max_x, d_max_y};
 }
  // -----------------------------------------------
 /**
   * Converts byte[] to hex String
   *
   * @param ba_data the byte[] to convert
   * @return the hex string to be used to compare with sql 'WHERE (hex(tile_data) = '?')'
   */
 public static String get_hex(byte [] ba_data)
 {
  if (ba_data == null)
  {
   return null;
  }
  final StringBuilder sb_hex = new StringBuilder(2*ba_data.length);
  Formatter formatter = new Formatter(sb_hex);
  for (final byte b : ba_data)
  {
   formatter.format("%02x",b);
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
 public static String get_pixel_rgb_toString(Bitmap this_bitmap,int i_parm)
 {
  String s_rgb="";
  // ----------------------------------------------
  int[] rgb=get_pixel_rgb(this_bitmap);
  if ((rgb != null) && (rgb.length == 3))
  {
   for (int i=0;i<rgb.length;i++)
   {
    String s_rgb_value=String.format("%02x", (0xFF & rgb[i])); // .toUpperCase();
    if ((i_parm == 1) && (i < (rgb.length-1)))
     s_rgb_value=s_rgb_value+"-";
    s_rgb=s_rgb+s_rgb_value;
   }
   if (i_parm == 1)
    s_rgb=s_rgb+".rgb";
  }
  // ----------------------------------------------
  return s_rgb;
 }
 // -----------------------------------------------
  /**
   * Determin if the Bitmap is blank (i.e. all pixels are of ONE colour)
   * - RGB_565 will be converted to ARGB_8888
   * @param this_bitmap Bitmap to check
   * @return  RGB Values of this colour, otherwise null
   */
 public static int[] get_pixel_rgb(Bitmap this_bitmap)
 {
  int[] rgb=null;
  int i_image_width=this_bitmap.getWidth();
  int i_image_height=this_bitmap.getHeight();
  Bitmap.Config i_bitmap_config=this_bitmap.getConfig();
  int i_R=0;
  int i_G=0;
  int i_B=0;
  // ----------------------------------------------
  for (int x=0;x<i_image_width;x++)
  {
   for (int y=0;y<i_image_height;y++)
   {
    int[] pixel_rgb=get_pixel_rgb(i_bitmap_config,this_bitmap.getPixel(x,y));
    if ((pixel_rgb != null) && (pixel_rgb[0] != 0) && (pixel_rgb[1] != 0) && (pixel_rgb[2] != 0))
    {
     if ((x == 0) && (y == 0))
     {
      i_R=pixel_rgb[0];
      i_G=pixel_rgb[1];
      i_B=pixel_rgb[2];
     }
     else
     {
      if ((pixel_rgb[0] != i_R) || (pixel_rgb[1] != i_G) || (pixel_rgb[2] != i_B))
      {
       return rgb; // This image has more than one color, return null
      }
     }
    }
   }
  }
  // ----------------------------------------------
  rgb=new int[]{i_R,i_G,i_B}; // This image is of one color, return the RGB Values
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
 public static int[] get_pixel_rgb(Bitmap.Config i_bitmap_config,int i_pixel)
 { // i_image_height - i_pixel = 0xff000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
  int[] rgb=null;
  switch (i_bitmap_config)
  {
   case RGB_565:
   {
    // Bitmap.Config.RGB_565=4
    if (rb_table == null)
    {
     int i=32;
     rb_table = new int[i];
     for (i=0;i<32;i++)
      rb_table[i]=255*i/31;
     i=64;
     g_table = new int[i];
     for (i=0;i<64;i++)
      g_table[i]=255*i/63;
    }
    rgb= new int[]{rb_table[(i_pixel >> 11)&31]<<16,g_table[(i_pixel>>5)&63]<<8,rb_table[i_pixel&31]};
   }
   break;
   // ALPHA_8, ARGB_4444
   case ARGB_8888:
   {
    rgb= new int[]{(i_pixel & 0xff0000) >> 16,(i_pixel & 0x00ff00) >> 8,(i_pixel & 0x0000ff) >> 0};
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
   * @param ty the y tile number.
   * @param zoom the current zoom level.
   * @return the converted values.
   */
 public static int[] googleTile2TmsTile( int tx, int ty, int zoom )
 {
  return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
 }
 /**
     * Converts TMS tile coordinates to Google Tile coordinates.
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param tx the x tile number.
     * @param ty the y tile number.
     * @param zoom the current zoom level.
     * @return the converted values.
     */
 public static int[] tmsTile2GoogleTile( int tx, int ty, int zoom )
 {
  return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
 }
 /**
   * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
   *
   * @param tx
   * @param ty
   * @param zoom
   * @param tileSize
   * @return [minx, miny, maxx, maxy]
   */
 public static double[] tileLatLonBounds( int tx, int ty, int zoom, int tileSize )
 {
  double[] bounds = tileBounds(tx, ty, zoom, tileSize);
  double[] mins = metersToLatLon(bounds[0], bounds[1]);
  double[] maxs = metersToLatLon(bounds[2], bounds[3]);
  return new double[]{mins[1], maxs[0], maxs[1], mins[0]};
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
 public static double[] tileBounds( int tx, int ty, int zoom, int tileSize )
 {
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
 public static double[] pixelsToMeters( double px, double py, int zoom, int tileSize )
 {
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
 public static double[] metersToLatLon( double mx, double my )
 { // double originShift = 2 * Math.PI * 6378137 / 2.0;
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
 public static double getResolution( int zoom, int tileSize )
 {
  // return (2 * Math.PI * 6378137) / (this.tileSize * 2**zoom)
  double initialResolution = 2 * Math.PI * 6378137 / tileSize;
  return initialResolution / Math.pow(2, zoom);
 }
}
