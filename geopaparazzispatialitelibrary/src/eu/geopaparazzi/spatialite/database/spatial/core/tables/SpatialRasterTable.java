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
import java.io.Serializable;

import jsqlite.Database;

import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite;
import eu.geopaparazzi.library.database.GPLog;
/**
 * A raster table from the spatial db.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SpatialRasterTable extends AbstractSpatialTable implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * constructor.
     * 
     * @param spatialite_db the class 'Database' connection [will be null].
     * @param vector_key major Parameters  needed for creation in AbstractSpatialTable.
     * @param vector_value minor Parameters needed for creation in AbstractSpatialTable.
     */
    public SpatialRasterTable(Database spatialite_db, String vector_key,String  vector_value) 
    {
      super(spatialite_db,vector_key,vector_value);
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
      * Return String of Columnname of SPL_Geopackage
      *
      * @return Columnname of SPL_Geopackage
      */
    public String getColumnName() {
        return tableName;
    }

    /**
      * Set String of Columnname of SPL_Geopackage / tablename of RasterLite2 image
      * [mj10777: not really needed]
      * @param s_table_name the name to set.
      */
    public void setColumnName( String s_table_name ) {
        this.tableName = s_table_name;
    }

    /**
      * Set String of Title of RasterLite2 image
      * 
      * 
      * @param title the name to set.
      */
    public void setTitle( String title ) {
        this.title = title;
    }
    /**
      * Set String of Description of RasterLite2 image
      * 
      * 
      * @param description the name to set.
      */
    public void setDescription( String description ) {
        this.description = description;
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

    @Override
    public double[] longLat2Srid(double lon, double lat) {
        // For MBTiles,Map,Mapurl: a Memory-Database will be used
        double[] srid_Coordinate=new double[]{lon,lat};
         try 
          {
           srid_Coordinate=DaoSpatialite.longLat2Srid(getDatabase(1),lon,lat,getSrid());
          } 
          catch (jsqlite.Exception e_stmt) 
          {
           GPLog.error("SpatialRasterTable", "longLat2Srid[" +getSrid()+ "] db[" + getDatabase(1).getFilename() + "]", e_stmt);
          }      
          return srid_Coordinate;   
    }

    @Override
    public boolean isEditable() {
        return false;
    }

}
