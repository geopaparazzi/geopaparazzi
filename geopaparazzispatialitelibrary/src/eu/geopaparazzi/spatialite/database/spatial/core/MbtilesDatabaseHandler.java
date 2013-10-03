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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.LinkedHashMap;

import jsqlite.Exception;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MBTilesDroidSpitter;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MbTilesMetadata;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;

/**
 * An utility class to handle an mbtiles database.
 *
 * author Andrea Antonello (www.hydrologis.com)
 * adapted to create and fill mbtiles databases Mark Johnson (www.mj10777.de)
 */
public class MbtilesDatabaseHandler implements ISpatialDatabaseHandler
{
 public final static String TABLE_METADATA = "metadata";
 public final static String COL_METADATA_NAME = "name";
 public final static String COL_METADATA_VALUE = "value";
 private List<SpatialRasterTable> rasterTableList;
 private String s_mbtiles_file;
 private MBTilesDroidSpitter db_mbtiles;
 private HashMap<String,String> mbtiles_metadata = null;
 private int i_force_unique=0;
 // -----------------------------------------------
 /**
   * Constructor MbtilesDatabaseHandler
   * - if the file does not exist, a valid mbtile database will be created
   * - if the parent directory does not exist, it will be created
   * @param s_mbtiles_path full path to mbtiles file to open
   * @param mbtiles_metadata list of initial metadata values to set apon creation [otherwise can be null]
   * @return void
   */
 public MbtilesDatabaseHandler(String s_mbtiles_path,HashMap<String,String> mbtiles_metadata)
 {
  this.mbtiles_metadata=mbtiles_metadata;
  File file_mbtiles = new File(s_mbtiles_path);
  s_mbtiles_file = file_mbtiles.getName().substring(0,file_mbtiles.getName().lastIndexOf("."));
  db_mbtiles = new MBTilesDroidSpitter(file_mbtiles,this.mbtiles_metadata);
 }
 public String getFileName()
 {
  return s_mbtiles_file;
 }
 public List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception
 {
  return Collections.emptyList();
 }
 public List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception
 {
  if (rasterTableList == null || forceRead)
  {
   rasterTableList = new ArrayList<SpatialRasterTable>();
   db_mbtiles.open(true,""); // "" : default value will be used '1.1'
   MbTilesMetadata metadata = db_mbtiles.getMetadata();
   float[] bounds = metadata.bounds;// left, bottom, right, top
   double[] d_bounds={bounds[0],bounds[1],bounds[2],bounds[3]};
   float[] center = metadata.center;// center_x,center_y,zoom
   String tableName = metadata.name;
   String columnName = null;
   float centerX = 0f;
   float centerY = 0f;
   int defaultZoom=metadata.maxZoom;
   if (center != null)
   {
    centerX = center[0];
    centerY = center[1];
    defaultZoom=(int)center[2];
   }
   else
   {
    if (bounds != null)
    {
     centerX = bounds[0] + (bounds[2] - bounds[0]) / 2f;
     centerY = bounds[1] + (bounds[3] - bounds[1]) / 2f;
    }
   }
   SpatialRasterTable table = new SpatialRasterTable(s_mbtiles_file, columnName,"3857",metadata.minZoom,metadata.maxZoom,centerX,centerY,"?,?,?",d_bounds);
   table.setDefaultZoom(defaultZoom); // for mbtiles the desired center can be set by the database developer and may be different than the true center/zoom
   rasterTableList.add(table);
  }
  return rasterTableList;
 }
 @Override
 public float[] getTableBounds(SpatialVectorTable spatialTable,String destSrid) throws Exception
 {
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
   * - i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will be converted to 'tms' notation if needed]
   * @param query Format 'z,x,y_osm'
   * @return byte[] of the tile or null if no tile matched the given parameters
   */
 public byte[] getRasterTile(String query)
 {
  String[] split = query.split(",");
  if (split.length != 3)
  {
   return null;
  }
  int i_z = 0;
  int i_x = 0;
  int i_y_osm = 0;
  try
  {
   i_z = Integer.parseInt(split[0]);
   i_x = Integer.parseInt(split[1]);
   i_y_osm = Integer.parseInt(split[2]);
  }
  catch (NumberFormatException e)
  {
   return null;
  }
  byte[] tileAsBytes = db_mbtiles.getTileAsBytes(i_x,i_y_osm,i_z);
  return tileAsBytes;
 }
 // -----------------------------------------------
 /**
   * Function to retrieve Tile Bitmap from the mbtiles Database [for 'CustomTileDownloader']
   * - i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will be converted to 'tms' notation if needed]
   * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
   * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
   * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
   * @param i_pixel_size the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
   * @param tile_bitmap retrieve the Bitmap as done in 'CustomTileDownloader'
   * @return Bitmap of the tile or null if no tile matched the given parameters
   */
 public boolean getBitmapTile(int i_x,int i_y_osm,int i_z,int i_pixel_size,Bitmap tile_bitmap)
 {
  boolean b_rc=true;
  if (db_mbtiles.getmbtiles()  == null)
  { // in case .'open' was forgotton
   db_mbtiles.open(true,""); // "" : default value will be used '1.1'
  }
  int[] pixels=new int[i_pixel_size*i_pixel_size];
  byte[] rasterBytes = db_mbtiles.getTileAsBytes(i_x,i_y_osm,i_z);
  if (rasterBytes == null)
  {
   b_rc=false;
   return b_rc;
  }
  Bitmap decodedBitmap = null;
  decodedBitmap = BitmapFactory.decodeByteArray(rasterBytes,0,rasterBytes.length);
  // check if the input stream could be decoded into a bitmap
  if (decodedBitmap != null)
  { // copy all pixels from the decoded bitmap to the color array
   decodedBitmap.getPixels(pixels,0,i_pixel_size,0,0,i_pixel_size,i_pixel_size);
   decodedBitmap.recycle();
  }
  else
  {
   b_rc=false;
   return b_rc;
  }
  // copy all pixels from the color array to the tile bitmap
  tile_bitmap.setPixels(pixels,0,i_pixel_size,0,0,i_pixel_size,i_pixel_size);
  return b_rc;
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
   * @param i_fetch_bounds 1=force a calculation of the bounds and min/max zoom levels
   * @return 0: correct, otherwise error
   */
 public int insertBitmapTile(int i_x,int i_y_osm,int i_z,Bitmap tile_bitmap,int i_fetch_bounds) throws IOException
 { // i_rc= correct, otherwise error
  int i_rc=0;
  try
  { // i_rc=0: inserted [if needed bounds min/max zoom have been updated]
   i_rc=db_mbtiles.insertBitmapTile(i_x,i_y_osm,i_z,tile_bitmap,i_force_unique,i_fetch_bounds);
  }
  catch (IOException e)
  {
   i_rc=1;
   SpatialDatabasesManager.app_log(3,"["+s_mbtiles_file+"] "+e.getMessage());
   e.printStackTrace();
  }
  return i_rc;
 }
 // -----------------------------------------------
 /**
   * Close mbtiles Database
   * @return void
   */
 public void close() throws Exception
 {
  if (db_mbtiles != null)
  {
   db_mbtiles.close();
  }
 }
  // -----------------------------------------------
 /**
   * Update mbtiles Bounds / Zoom (min/max) levels
   * @return void
   */
 public void update_bounds()
 {
  if (db_mbtiles != null)
  {
   db_mbtiles.fetch_bounds_minmax(0,1);;
  }
 }
 // /////////////////////////////////////////////////
 // UNUSED
 // /////////////////////////////////////////////////
 public GeometryIterator getGeometryIteratorInBounds( String destSrid, SpatialVectorTable table, double n, double s, double e, double w )
 {
  return null;
 }
 public Paint getFillPaint4Style( Style style )
 {
  return null;
 }
 public Paint getStrokePaint4Style( Style style )
 {
  return null;
 }
 @Override
 public void updateStyle( Style style ) throws Exception
 {
 }
 @Override
 public void intersectionToStringBBOX( String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e, double w, StringBuilder sb, String indentStr ) throws Exception
 {
 }
 @Override
 public void intersectionToString4Polygon( String boundsSrid, SpatialVectorTable spatialTable, double n, double e,StringBuilder sb, String indentStr ) throws Exception
 {
 }
}
