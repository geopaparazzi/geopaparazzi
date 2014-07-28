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
package eu.geopaparazzi.spatialite.database.spatial.core.tables;

import com.vividsolutions.jts.geom.Envelope;

import java.io.File;
import java.io.Serializable;

/**
 * Spatial table interface.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public abstract class AbstractSpatialTable implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Table type description.
     */
    protected String tableTypeDescription;

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
     * A description.
     */
    protected String description;
    /**
     * A title.
     */
    protected String title;
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
     * The map type.
     */
    protected String mapType;

    /**
     * If true, then the table is a view.
     */
    protected boolean isView = false;

    /**
     * Constructor.
     *
     * @param databasePath the db path.
     * @param tableName    a name for the table.
     * @param mapType      the map type.
     * @param srid         srid of the table.
     * @param minZoom      min zoom.
     * @param maxZoom      max zoom.
     * @param centerX      center x.
     * @param centerY      center y.
     * @param bounds       the bounds as [w,s,e,n]
     */
    public AbstractSpatialTable(String databasePath, String tableName, String mapType, String srid, int minZoom, int maxZoom,
                                double centerX, double centerY, double[] bounds) {
        this.databasePath = databasePath;
        this.tableName = tableName;
        this.mapType = mapType;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.databaseFile = new File(databasePath);
        this.databaseFileName = databaseFile.getName();
        this.databaseFileNameNoExtension = databaseFileName.substring(0, databaseFileName.lastIndexOf("."));
        this.srid = srid;
        this.centerX = centerX;
        this.centerY = centerY;
        this.boundsWest = bounds[0];
        this.boundsSouth = bounds[1];
        this.boundsEast = bounds[2];
        this.boundsNorth = bounds[3];
    }

    /**
     * Return the absolute path of the database.
     * <p/>
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
    public File getDatabaseFile() {
        return databaseFile;
    }

    /**
     * Getter for the table's srid.
     *
     * @return the table srid.
     */
    public String getSrid() {
        return srid;
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
    public String getFileNameNoExtension() {
        return databaseFileNameNoExtension;
    }

    /**
     * Getter for the table name.
     *
     * @return the name of the {@link AbstractSpatialTable}.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Return type of map/file
     * <p/>
     * <p>raster: can be different: mbtiles,db,sqlite,gpkg
     * <p>mbtiles : mbtiles
     * <p>map : map
     *
     * @return s_name as short name of map/file
     */
    public String getMapType() {
        return mapType;
    }

    /**
     * Returns the title
     *
     * @return a title.
     */
    public String getTitle() {
        if (title != null) {
            return title;
        }
        return getTableName();
    }

    /**
     * Returns a description.
     *
     * @return a description.
     */
    public String getDescription() {
        if (description != null) {
            return description;
        }
        return "map_type[" + getMapType() + "] table_name[" + getTableName() + "] srid[" + getSrid() + "] bounds["
                + getBoundsAsString() + "] center[" + getCenterAsString() + "]";
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
    public void setDefaultZoom(int defaultZoom) {
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
     * <p/>
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
     * <p/>
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
     * <p/>
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
     * <p/>
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
     * <p/>
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
     * <p/>
     * <p>default : center of bounds
     * <p>mbtiles : taken from 2nd value of metadata 'center'
     *
     * @return double of Y Value [Latitude]
     */
    public double getCenterY() {
        return centerY;
    }

    /**
     * Get table bounds.
     *
     * @return the table bounds as [n, s, e, w].
     */
    public float[] getTableBounds() {
        float w = (float) boundsWest;
        float s = (float) boundsSouth;
        float e = (float) boundsEast;
        float n = (float) boundsNorth;
        return new float[]{n, s, e, w};
    }

    /**
     * Get table envelope.
     *
     * @return the {@link Envelope}.
     */
    public Envelope getTableEnvelope() {
        float w = (float) boundsWest;
        float s = (float) boundsSouth;
        float e = (float) boundsEast;
        float n = (float) boundsNorth;
        return new Envelope(w, e, s, n);
    }

    /**
     * Check the supplied bounds against the current table bounds.
     *
     * @param boundsCoordinates as wsg84 [w,s,e,n]
     * @return <code>true</code> if the given bounds are inside the bounds the current table.
     */
    public boolean checkBounds(double[] boundsCoordinates) {
        if ((boundsCoordinates[0] >= boundsWest) && (boundsCoordinates[1] >= this.boundsSouth)
                && (boundsCoordinates[2] <= boundsEast) && (boundsCoordinates[3] <= this.boundsNorth)) {
            return true;
        }
        return false;
    }

    /**
     * Return String of bounds [wms-format]
     * <p/>
     * <p>x_min,y_min,x_max,y_max
     *
     * @return bounds formatted using wms format [w,s,e,n]
     */
    public String getBoundsAsString() {
        return boundsWest + "," + boundsSouth + "," + boundsEast + "," + boundsNorth;
    }

    /**
     * Return String of Map-Center with default Zoom
     * <p/>
     * <p>x_position,y_position,default_zoom
     *
     * @return center formatted using mbtiles format
     */
    public String getCenterAsString() {
        return centerX + "," + centerY + "," + defaultZoom;
    }

    /**
     * @return true if the table is editable.
     */
    public abstract boolean isEditable();

    /**
     * 'SpatialTable' = false [SpatialVectorTable]
     * 'RasterLite2' = false [SpatialRasterTable]
     * 'GeoPackage_features' = false [SpatialVectorTable]
     * 'GeoPackage_tiles' = false [SpatialRasterTable]
     * 'SpatialView' = true [SpatialVectorTable]
     *
     * @return true if this is a SpatialView
     */
    public boolean isView(){
        return isView;
    }

}
