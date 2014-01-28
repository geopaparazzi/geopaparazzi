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
import java.io.Serializable;

import eu.geopaparazzi.spatialite.util.SpatialDataType;
/**
 * A raster table from the spatial db.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SpatialRasterTable extends SpatialTable implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tileQuery;

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
        super(dbPath, name, SpatialDataType.DB.getTypeName(), srid, minZoom, maxZoom, centerX, centerY, bounds);

        // todo: change this
        if (tileQuery != null) {
            this.tileQuery = tileQuery;
        } else {
            tileQuery = "select " + name + " from " + dbPath + " where zoom_level = ? AND tile_column = ? AND tile_row = ?";
        }
    }

    /**
     * Set type of map/file.
     * 
     * <p>For raster tables this can be necessary, due to the different db types
     * (ex. gpk or mbtiles)
     *
     * @param mapType the type to set.
     */
    public void setMapType( String mapType ) {
        this.mapType = mapType;
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
     * Function to check and correct bounds / zoom level [for 'SpatialiteDatabaseHandler']
     *
     * @param mapCenterLocation [point/zoom to check] (most probably result of PositionUtilities.getMapCenterFromPreferences(preferences,true,true);)
     * @param doCorrectIfOutOfRange if <code>true</code>, change mapCenterLocation values if out of range.
     * @return 0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections ; 1= correct tileBounds values.
     */
    public int checkCenterLocation( double[] mapCenterLocation, boolean doCorrectIfOutOfRange ) {
        int i_rc = 0; // inside area
        if (((mapCenterLocation[0] < boundsWest) || (mapCenterLocation[0] > boundsEast))
                || ((mapCenterLocation[1] < boundsSouth) || (mapCenterLocation[1] > boundsNorth))
                || ((mapCenterLocation[2] < minZoom) || (mapCenterLocation[2] > maxZoom))) {
            if (((mapCenterLocation[0] >= boundsWest) && (mapCenterLocation[0] <= boundsEast))
                    && ((mapCenterLocation[1] >= boundsSouth) && (mapCenterLocation[1] <= boundsNorth))) {
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
                if ((mapCenterLocation[0] < boundsWest) || (mapCenterLocation[0] > boundsEast)) {
                    i_rc = 13;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[0] = centerX;
                    }
                }
                if ((mapCenterLocation[1] < boundsSouth) || (mapCenterLocation[1] > boundsNorth)) {
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
