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
    private final double centerX;
    private final double centerY;

    public SpatialRasterTable( String tableName, String columnName, String srid, int minZoom, int maxZoom, double centerX,
            double centerY, String tileQuery ) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.srid = srid;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.centerX = centerX;
        this.centerY = centerY;

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

    /**
     * Get the tile retrieve query with place holders for zoom, column and row.
     * 
     * @return the query to use for this raster set.
     */
    public String getTileQuery() {
        return tileQuery;
    }

}
