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

/**
 * Spatial table interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface ISpatialTable {
    /**
     * Return the absolute path of the database.
     *
     * <p>default: file name with path and extention
     * <p>mbtiles : will be a '.mbtiles' sqlite-file-name
     * <p>map : will be a mapforge '.map' file-name
     *
     * @return the absolute database path.
     */
    public abstract String getDatabasePath();

    /**
     * Getter for the table's srid.
     * 
     * @return the table srid.
     */
    public String getSrid();

    /**
     * Return database {@link File}.
     *
     * @return the database file.
     */
    public File getFile();

    /**
     * Returns the database file name with extension.
     *
     * @return the database file name with extension.
     */
    public String getFileName();

    /**
     * Returns the database file name without extension.
     *
     * @return the database file name without extension.
     */
    public String getName();

    /**
     * Returns a description.
     *
     * @return a description.
     */
    public String getDescription();

    /**
     * Return Min Zoom.
     *
     * @return integer minzoom.
     */
    public int getMinZoom();

    /**
      * Return Max Zoom.
      *
      * @return integer maxzoom.
      */
    public int getMaxZoom();

    /**
     * Retrieve Zoom level
     *
    * @return defaultZoom
     */
    public int getDefaultZoom();

    /**
     * Set default Zoom level
     *
     * @param defaultZoom desired Zoom level
     */
    public void setDefaultZoom( int defaultZoom );

    /**
     * Return Min/Max Zoom as string
     *
     * @return String min/maxzoom
     */
    public String getMinMaxZoomLevelsAsString();

    /**
     * Return West X Value [Longitude]
     *
     * <p>default :  -180.0 [if not otherwise set]
     * <p>mbtiles : taken from 1st value of metadata 'bounds'
     *
     * @return double of West X Value [Longitude]
     */
    public double getMinLongitude();

    /**
      * Return South Y Value [Latitude]
      *
      * <p>default :  -85.05113 [if not otherwise set]
      * <p>mbtiles : taken from 2nd value of metadata 'bounds'
      *
      * @return double of South Y Value [Latitude]
      */
    public double getMinLatitude();

    /**
      * Return East X Value [Longitude]
      *
      * <p>default :  180.0 [if not otherwise set]
      * <p>mbtiles : taken from 3th value of metadata 'bounds'
      *
      * @return double of East X Value [Longitude]
      */
    public double getMaxLongitude();

    /**
      * Return North Y Value [Latitude]
      *
      * <p>default :  85.05113 [if not otherwise set]
      * <p>mbtiles : taken from 4th value of metadata 'bounds'
      *
      * @return double of North Y Value [Latitude]
      */
    public double getMaxLatitude();

    /**
      * Return Center X Value [Longitude]
      *
      * <p>default : center of bounds
      * <p>mbtiles : taken from 1st value of metadata 'center'
      *
      * @return double of X Value [Longitude]
      */
    public double getCenterX();

    /**
      * Return Center Y Value [Latitude]
      *
      * <p>default : center of bounds
      * <p>mbtiles : taken from 2nd value of metadata 'center'
      *
      * @return double of Y Value [Latitude]
      */
    public double getCenterY();

    /**
     * Return String of bounds [wms-format]
     *
     * <p>x_min,y_min,x_max,y_max
     *
     * @return bounds formatted using wms format
     */
    public String getBoundsAsString();

    /**
     * Return String of Map-Center with default Zoom
     *
     * <p>x_position,y_position,default_zoom
     *
     * @return center formatted using mbtiles format
     */
    public String getCenterAsString();
}
