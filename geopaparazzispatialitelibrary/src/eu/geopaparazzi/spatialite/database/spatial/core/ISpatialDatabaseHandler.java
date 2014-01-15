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
import java.util.List;

import jsqlite.Exception;
import android.graphics.Paint;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryIterator;
import eu.geopaparazzi.spatialite.util.Style;

/**
 * Interface for spatial database handlers.
 * 
 * <p>Spatial databases are spatialite db seen for their
 * vector and raster content. They therefore can be containing 
 * geometries or raster tiles. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface ISpatialDatabaseHandler {
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

    // /**
    // * Return long description for the database handler.
    // *
    // * <p>default: s_name with bounds and center
    // * <p>mbtiles : metadata description
    // * <p>map : will be value of 'comment', if not null
    // *
    // * @return long description for the database handler.
    // */
    // public String getDescription();
    //
    // /**
    // * Set long description for the database.
    // *
    // * @param databaseDescription a description for the database.
    // */
    // public void setDescription( String databaseDescription );

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
     * @return the list of {@link SpatialVectorTable}s.
     * @throws Exception  if something goes wrong.
     */
    public abstract List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception;

    /**
     * Get the spatial raster tables from the database.
     *
     * @param forceRead force a clean read from the db instead of using cached.
     * @return the list of {@link SpatialVectorTable}s.
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
     * Get the table's bounds.
     *
     * @param spatialTable the table to use.
     * @param destSrid the srid to which to project to.
     * @return the bounds as [n,s,e,w].
     * @throws Exception  if something goes wrong.
     */
    public float[] getTableBounds( SpatialVectorTable spatialTable, String destSrid ) throws Exception;

    /**
     * Get the {@link GeometryIterator} of a table in a given bound.
     *
     * @param destSrid the srid to which to transform to.
     * @param table the table to use.
     * @param n north bound.
     * @param s south bound.
     * @param e east bound.
     * @param w west bound.
     * @return the geometries iterator.
     */
    public abstract GeometryIterator getGeometryIteratorInBounds( String destSrid, SpatialVectorTable table, double n, double s,
            double e, double w );

    /**
     * Get the stroke {@link Paint} for a given style.
     *
     * <p>Paints are cached and reused.</p>
     *
     * @param style the {@link Style} to use.
     * @return the paint.
     */
    public abstract Paint getStrokePaint4Style( Style style );

    /**
     * Get the fill {@link Paint} for a given style.
     *
     * <p>Paints are cached and reused.</p>
     *
     * @param style the {@link Style} to use.
     * @return the paint.
     */
    public abstract Paint getFillPaint4Style( Style style );

    /**
     * Closes the database handler, freeing its resources.
     * 
     * @throws Exception  if something goes wrong.
     */
    public void close() throws Exception;

    /**
     * Update the style definition in the database with the supplied {@link Style}.
     * 
     * @param style the style to use as update.
     * @throws Exception  if something goes wrong.
     */
    public void updateStyle( Style style ) throws Exception;

    /**
     * Performs an intersection query on a vector table and returns a string info version of the result.
     * 
     * @param boundsSrid the srid of the bounds supplied.
     * @param spatialTable the vector table to query.
     * @param n north bound.
     * @param s south bound.
     * @param e east bound.
     * @param w west bound.
     * @param resultStringBuilder the builder of the result.
     * @param indentStr the indenting to use for formatting.
     * @throws Exception  if something goes wrong.
     */
    public void intersectionToStringBBOX( String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e,
            double w, StringBuilder resultStringBuilder, String indentStr ) throws Exception;

}
