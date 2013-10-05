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
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
/**
 * A raster table from the spatial db.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialRasterTable {

    private final String srid;
    private final String tableName;
    private final String columnName;
    private String tileQuery;
    private final int minZoom;
    private final int maxZoom;
    private final double centerX; // wsg84
    private final double centerY;  // wsg84
    private final double bounds_west;  // wsg84
    private final double bounds_east;  // wsg84
    private final double bounds_north;  // wsg84
    private final double bounds_south;  // wsg84
    private int defaultZoom;

    public SpatialRasterTable( String tableName, String columnName, String srid, int minZoom, int maxZoom, double centerX,
            double centerY, String tileQuery,double[] bounds) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.srid = srid;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.defaultZoom=minZoom;
        this.centerX = centerX;
        this.centerY = centerY;
        this.bounds_west=bounds[0];
        this.bounds_south=bounds[1];
        this.bounds_east=bounds[2];
        this.bounds_north=bounds[3];

        if (tileQuery != null) {
            this.tileQuery = tileQuery;
        } else {
            tileQuery = "select " + columnName + " from " + tableName
                    + " where zoom_level = ? AND tile_column = ? AND tile_row = ?";
        }
    }

    public String getSrid() {
        return srid;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public int getMinZoom() {
        return minZoom;
    }

    public int getMaxZoom() {
        return maxZoom;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }
    public void setDefaultZoom(int i_zoom)
    {
     defaultZoom=i_zoom;
    }
    // -----------------------------------------------
    /**
     * Function to check and correct bounds / zoom level [for 'SpatialiteDatabaseHandler']
     * @param mapCenterLocation [point/zoom to check] result of PositionUtilities.getMapCenterFromPreferences(preferences,true,true);
     * @param i_parm 1= change mapCenterLocation values if out of range
     * @return 0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections ; 1= correct tileBounds values.
     */
    public int checkCenterLocation(double[] mapCenterLocation,int i_parm)
    {
     int i_rc=0; // inside area
     // SpatialDatabasesManager.app_log(i_debug,"SpatialRasterTable.checkCenterLocation: center_location[x="+mapCenterLocation[0]+" ; y="+mapCenterLocation[1]+" ; z="+mapCenterLocation[2]+"] bbox=["+bounds_west+","+bounds_south+","+bounds_east+","+bounds_north+"]");
     if (((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east)) ||
          ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north)) ||
          ((mapCenterLocation[2] < minZoom) || (mapCenterLocation[2] > maxZoom)))
      {
       if (((mapCenterLocation[0] >= bounds_west) && (mapCenterLocation[0] <= bounds_east)) &&
            ((mapCenterLocation[1] >=bounds_south) && (mapCenterLocation[1] <= bounds_north)))
       { // We are inside the Map-Area, but Zoom is not correct
        if  (mapCenterLocation[2] < minZoom)
        {
         i_rc=1;
         if (i_parm == 1)
         {
          mapCenterLocation[2]=minZoom;
         }
        }
        if (mapCenterLocation[2] > maxZoom)
        {
         i_rc=2;
         if (i_parm == 1)
         {
          mapCenterLocation[2]=maxZoom;
         }
        }
       }
       else
       {
        if  (mapCenterLocation[2] < minZoom)
        {
         i_rc=11;
         if (i_parm == 1)
         {
          mapCenterLocation[2]=minZoom;
         }
        }
        if (mapCenterLocation[2] > maxZoom)
        {
         i_rc=12;
         if (i_parm == 1)
         {
          mapCenterLocation[2]=maxZoom;
         }
        }
        if ((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east))
        {
         i_rc=13;
         if (i_parm == 1)
         {
          mapCenterLocation[0]=centerX;
         }
        }
        if ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north))
        {
         i_rc=14;
         if (i_parm == 1)
         {
          mapCenterLocation[1]=centerY;
         }
        }
       }
       // SpatialDatabasesManager.app_log(i_debug,"SpatialRasterTable..checkCenterLocation: changed["+i_rc+"] : center_location[x="+mapCenterLocation[0]+" ; y="+mapCenterLocation[1]+" ; z="+mapCenterLocation[2]+"] bbox=["+bounds_west+","+bounds_south+","+bounds_east+","+bounds_north+"]");
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
