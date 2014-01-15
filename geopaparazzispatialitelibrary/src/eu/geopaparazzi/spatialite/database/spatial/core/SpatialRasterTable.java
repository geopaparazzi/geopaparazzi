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

import eu.geopaparazzi.spatialite.util.SpatialiteTypes;
/**
 * A raster table from the spatial db.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SpatialRasterTable implements ISpatialTable {

    private final String srid;
    private File dbFile; // all DatabaseHandler/Table classes should use these names
    private String dbPath; // [with path] all DatabaseHandler/Table classes should use these
                           // names
    private String databaseFileName; // [without path] all DatabaseHandler/Table classes should use
    // these
    // names
    private String name; // all DatabaseHandler/Table classes should use these
                         // names
    private String mapType = SpatialiteTypes.DB.getTypeName();
    private String tableName;
    private String s_column_name;
    private String tileQuery;
    private int minZoom;
    private int maxZoom;
    private double centerX; // wsg84
    private double centerY; // wsg84
    private double bounds_west; // wsg84
    private double bounds_east; // wsg84
    private double bounds_north; // wsg84
    private double bounds_south; // wsg84
    private int defaultZoom;

    /**
     * constructor.
     * 
     * @param dbPath the db path.
     * @param name the name.
     * @param srid srid of the table.
     * @param minZoom min zoom.
     * @param maxZoom max zoom.
     * @param centerX center x.
     * @param centerY center y.
     * @param tileQuery query to use for tiles fetching.
     * @param bounds the bounds as [w,s,e,n]
     */
    public SpatialRasterTable( String dbPath, String name, String srid, int minZoom, int maxZoom, double centerX, double centerY,
            String tileQuery, double[] bounds ) {
        this.dbPath = dbPath;
        this.dbFile = new File(dbPath);
        this.databaseFileName = dbFile.getName();
        this.name = name;
        this.tableName = "";
        this.s_column_name = "";
        this.srid = srid;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.defaultZoom = minZoom;
        this.centerX = centerX;
        this.centerY = centerY;
        this.bounds_west = bounds[0];
        this.bounds_south = bounds[1];
        this.bounds_east = bounds[2];
        this.bounds_north = bounds[3];
        // todo: change this
        if (tileQuery != null) {
            this.tileQuery = tileQuery;
        } else {
            tileQuery = "select " + name + " from " + dbPath + " where zoom_level = ? AND tile_column = ? AND tile_row = ?";
        }
        // setDescription(getName());
        // will set default values with bounds and center if it is the
        // same as 's_name' or empty
        // GPLog.androidLog(-1,"SpatialRasterTable[" + file_map.getAbsolutePath() +
        // "] name["+s_name+"] s_description["+s_description+"]");
    }

    public String getSrid() {
        return srid;
    }

    public String getDatabasePath() {
        return this.dbPath;
    }

    public String getFileName() {
        return this.databaseFileName;
    }

    public String getName() {
        if ((name == null) || (name.length() == 0)) {
            name = this.dbFile.getName().substring(0, this.dbFile.getName().lastIndexOf("."));
        }
        return this.name;
    }

    public String getDescription() {
        return getName();
    }

    /**
      * Return type of map/file
      *
      * <p>raster: can be different: mbtiles,db,sqlite,gpkg
      * <p>mbtiles : mbtiles
      * <p>map : map
      *
      * @return s_name as short name of map/file
      */
    public String getMapType() {
        return this.mapType;
    }

    /**
      * Return String of Tablename of Geopackage
      *
      * @return Tablename of Geopackage
      */
    public String getTableName() {
        return tableName;
    }

    /**
      * Set String of Tablename of Geopackage
      * 
     * @param tableName the name to set. 
      */
    public void setTableName( String tableName ) {
        this.tableName = tableName;
    }

    /**
      * Return String of Columnname of Geopackage
      *
      * @return Columnname of Geopackage
      */
    public String getColumnName() {
        return tableName;
    }

    /**
      * Set String of Columnname of Geopackage
      * 
      * TODO mj10777, why is the name column but it sets the table name?
      * 
      * @param s_table_name the name to set.
      */
    public void setColumnName( String s_table_name ) {
        this.tableName = s_table_name;
    }

    /**
      * Set type of map/file.
      *
      * @param mapType the type to set.
      */
    public void setMapType( String mapType ) {
        this.mapType = mapType;
    }

    public String getBoundsAsString() {
        return bounds_west + "," + bounds_south + "," + bounds_east + "," + bounds_north;
    }

    public String getCenterAsString() {
        return centerX + "," + centerY + "," + defaultZoom;
    }

    public File getFile() {
        return this.dbFile;
    }

    public int getMinZoom() {
        return minZoom;
    }

    public int getMaxZoom() {
        return maxZoom;
    }

    public String getMinMaxZoomLevelsAsString() {
        return getMinZoom() + "-" + getMaxZoom();
    }

    public double getMinLongitude() {
        return bounds_west;
    }

    public double getMinLatitude() {
        return bounds_south;
    }

    public double getMaxLongitude() {
        return bounds_east;
    }

    public double getMaxLatitude() {
        return bounds_north;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public int getDefaultZoom() {
        return defaultZoom;
    }

    public void setDefaultZoom( int i_zoom ) {
        defaultZoom = i_zoom;
    }

    /**
     * Function to check and correct bounds / zoom level [for 'SpatialiteDatabaseHandler']
     *
     * @param mapCenterLocation [point/zoom to check] (most probably result of PositionUtilities.getMapCenterFromPreferences(preferences,true,true);)
     * @param doCorrectIfOutOfRange if <code>true</code>, change mapCenterLocation values if out of range.
     * @return 0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections ; 1= correct tileBounds values.
     */
    public int checkCenterLocation( double[] mapCenterLocation, boolean doCorrectIfOutOfRange ) {
        int i_rc = 0; // inside area
        if (((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east))
                || ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north))
                || ((mapCenterLocation[2] < minZoom) || (mapCenterLocation[2] > maxZoom))) {
            if (((mapCenterLocation[0] >= bounds_west) && (mapCenterLocation[0] <= bounds_east))
                    && ((mapCenterLocation[1] >= bounds_south) && (mapCenterLocation[1] <= bounds_north))) {
                /*
                 *  We are inside the Map-Area, but Zoom is not correct
                 */
                if (mapCenterLocation[2] < minZoom) {
                    i_rc = 1;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[2] = minZoom;
                    }
                }
                if (mapCenterLocation[2] > maxZoom) {
                    i_rc = 2;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[2] = maxZoom;
                    }
                }
            } else {
                if (mapCenterLocation[2] < minZoom) {
                    i_rc = 11;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[2] = minZoom;
                    }
                }
                if (mapCenterLocation[2] > maxZoom) {
                    i_rc = 12;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[2] = maxZoom;
                    }
                }
                if ((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east)) {
                    i_rc = 13;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[0] = centerX;
                    }
                }
                if ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north)) {
                    i_rc = 14;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[1] = centerY;
                    }
                }
            }
        }
        return i_rc;
    }

    /**
     * Get the tile retrieve query with place holders for zoom, column and row.
     *
     * @return the query to use for this raster set.
     */
    public String getTileQuery() {
        return tileQuery;
    }

}
