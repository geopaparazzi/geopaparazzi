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

import java.io.File;
import java.io.IOException;
import java.util.List;

import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import jsqlite.Exception;

/**
 * Abstract class for spatial database handlers.
 * 
 * <p>Spatial databases are spatialite db seen for their
 * vector and raster content. They therefore can be containing 
 * geometries or raster tiles. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public abstract class AbstractSpatialDatabaseHandler {
    /**
     * The database path. 
     */
    protected String databasePath;
    /**
     * The database file.
     */
    protected File databaseFile;
    /**
     * The database file name.
     */
    protected String databaseFileName;
    /**
     * The database file name without extension.
     */
    protected String databaseFileNameNoExtension;
    /**
     * A name for the table.
     */
    protected String tableName;
    /**
     * The table srid.
     */
    protected String srid;
    /**
     * The table center longitude.
     */
    protected double centerX;
    /**
     * The table center latitude.
     */
    protected double centerY;
    /**
     * Western table bound.
     */
    protected double boundsWest;
    /**
     * Southern table bound.
     */
    protected double boundsSouth;
    /**
     * Eastern table bound.
     */
    protected double boundsEast;
    /**
     * Northern table bound.
     */
    protected double boundsNorth;
    /**
     * Min allowed zoom.
     */
    protected int minZoom = 0;
    /**
     * Max allowed zoom.
     */
    protected int maxZoom = 22;
    /**
     * The default zoom for the table.
     */
    protected int defaultZoom = 17;

    /**
     * Flag to define the validity of the database.
     */
    protected boolean isDatabaseValid = false;

    /**
     * Constructor.
     * 
     * @param databasePath the path to the database to handle.
     * @throws IOException  if something goes wrong.
     */
    public AbstractSpatialDatabaseHandler(String databasePath) throws IOException {
        this.databasePath = databasePath;
        databaseFile = new File(databasePath);
        // if (!databaseFile.exists()) {
        // throw new FileNotFoundException("Database file not found: " + databasePath);
        // }
        databaseFileName = databaseFile.getName();
        databaseFileNameNoExtension = databaseFile.getName().substring(0, databaseFile.getName().lastIndexOf("."));
    }

    /**
     * Open the database, with all default tasks
     */
    public abstract void open();

    /**
      * Return the absolute path of the database.
      *
      * <p>default: file name with path and extention
      * <p>mbtiles : will be a '.mbtiles' sqlite-file-name
      * <p>map : will be a mapforge '.map' file-name
      *
      * @return the absolute database path.
      */
    public String getDatabasePath() {
        return databasePath;
    }

    /**
     * Return database {@link File}.
     *
     * @return the database file.
     */
    public File getFile() {
        return databaseFile;
    }

    /**
     * Returns the database file name with extension.
     *
     * @return the database file name with extension.
     */
    public String getFileName() {
        return databaseFileName;
    }

    /**
     * Returns the database file name without extension.
     *
     * @return the database file name without extension.
     */
    public String getName() {
        return this.databaseFileNameNoExtension;
    }

    /**
     * Return Min Zoom.
     *
     * @return integer minzoom.
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
      * Return Max Zoom.
      *
      * @return integer maxzoom.
      */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
     * Retrieve Zoom level
     *
    * @return defaultZoom
     */
    public int getDefaultZoom() {
        return defaultZoom;
    }

    /**
     * Set default Zoom level
     *
     * @param defaultZoom desired Zoom level
     */
    public void setDefaultZoom( int defaultZoom ) {
        this.defaultZoom = defaultZoom;
    }

    /**
     * Return Min/Max Zoom as string
     *
     * @return String min/maxzoom
     */
    public String getMinMaxZoomLevelsAsString() {
        return getMinZoom() + "-" + getMaxZoom();
    }

    /**
     * Return West X Value [Longitude]
     *
     * <p>default :  -180.0 [if not otherwise set]
     * <p>mbtiles : taken from 1st value of metadata 'bounds'
     *
     * @return double of West X Value [Longitude]
     */
    public double getMinLongitude() {
        return boundsWest;
    }

    /**
      * Return South Y Value [Latitude]
      *
      * <p>default :  -85.05113 [if not otherwise set]
      * <p>mbtiles : taken from 2nd value of metadata 'bounds'
      *
      * @return double of South Y Value [Latitude]
      */
    public double getMinLatitude() {
        return boundsSouth;
    }

    /**
      * Return East X Value [Longitude]
      *
      * <p>default :  180.0 [if not otherwise set]
      * <p>mbtiles : taken from 3th value of metadata 'bounds'
      *
      * @return double of East X Value [Longitude]
      */
    public double getMaxLongitude() {
        return boundsEast;
    }

    /**
      * Return North Y Value [Latitude]
      *
      * <p>default :  85.05113 [if not otherwise set]
      * <p>mbtiles : taken from 4th value of metadata 'bounds'
      *
      * @return double of North Y Value [Latitude]
      */
    public double getMaxLatitude() {
        return boundsNorth;
    }

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

    /**
     * Return String of bounds [wms-format]
     *
     * <p>x_min,y_min,x_max,y_max
     *
     * @return bounds formatted using wms format
     */
    public String getBoundsAsString() {
        return boundsWest + "," + boundsSouth + "," + boundsEast + "," + boundsNorth;
    }

    /**
     * Return String of Map-Center with default Zoom
     *
     * <p>x_position,y_position,default_zoom
     *
     * @return center formatted using mbtiles format
     */
    public String getCenterAsString() {
        return centerX + "," + centerY + "," + defaultZoom;
    }

    /**
      * Is the database file considered valid.
      * 
      * @return <code>true</code> if the db file is valid.
      */
    public abstract boolean isValid();

    /**
     * Get the spatial vector tables from the database.
     *
     * @param forceRead force a clean read from the db instead of using cached.
     * @return the list of {@link eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable}s.
     * @throws Exception  if something goes wrong.
     */
    public abstract List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception;

    /**
     * Get the spatial raster tables from the database.
     *
     * @param forceRead force a clean read from the db instead of using cached.
     * @return the list of {@link eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable}s.
     * @throws Exception  if something goes wrong.
     */
    public abstract List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception;

    /**
    * Fetch the raster tile in bytes for a given query.
    *
    * @param query the query to use.
    * @return the tile image bytes.
    */
    public abstract byte[] getRasterTile( String query );

    /**
     * Get the table of the supplied bounds.
     * 
     * @param spatialTable the {@link eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable}.
     * @return the table bounds as wgs84 [n, s, e, w].
     * @throws Exception  if something goes wrong.
     */
    public abstract float[] getTableBounds( AbstractSpatialTable spatialTable ) throws Exception;

    /**
    * Closes the database handler, freeing its resources.
    *
    * @throws Exception if something goes wrong.
    */
    public abstract void close() throws Exception;

}
