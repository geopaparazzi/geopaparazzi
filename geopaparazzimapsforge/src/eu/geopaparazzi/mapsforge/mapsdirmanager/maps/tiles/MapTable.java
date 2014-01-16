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
package eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles;
import java.io.File;

import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.spatialite.database.spatial.core.ISpatialTable;
import eu.geopaparazzi.spatialite.util.SpatialiteTypes;
/**
 * A map table from the map db.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 *  adapted to work with map databases [mapsforge] Mark Johnson (www.mj10777.de)
 */
@SuppressWarnings("nls")
public class MapTable implements ISpatialTable {

    private final String srid;
    // all DatabaseHandler/Table classes should use these names
    private File dbFile;
    private File dbStyleFile;
    private String dbPath;
    private String databaseFileName;
    private String name;
    private String mapType = SpatialiteTypes.MAP.getTypeName();
    private String tileQuery;
    private final int minZoom;
    private final int maxZoom;
    private final double centerX; // wsg84
    private final double centerY; // wsg84
    private final double boundsWest; // wsg84
    private final double boundsEast; // wsg84
    private final double boundsNorth; // wsg84
    private final double boundsSouth; // wsg84
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
    public MapTable( String dbPath, String name, String srid, int minZoom, int maxZoom, double centerX, double centerY,
            String tileQuery, double[] bounds ) {
        this.dbPath = dbPath;
        this.dbFile = new File(dbPath);
        this.dbStyleFile = findXmlFile(dbFile);
        this.databaseFileName = dbFile.getName();
        this.name = name;
        this.srid = srid;
        if (minZoom > maxZoom) {
            int i_zoom = minZoom;
            minZoom = maxZoom;
            maxZoom = i_zoom;
        }
        if ((minZoom < 0) || (minZoom > 22))
            minZoom = 0;
        if ((maxZoom < 0) || (maxZoom > 22))
            maxZoom = 22;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.defaultZoom = minZoom;
        this.centerX = centerX;
        this.centerY = centerY;
        this.boundsWest = bounds[0];
        this.boundsSouth = bounds[1];
        this.boundsEast = bounds[2];
        this.boundsNorth = bounds[3];
        if (tileQuery != null) {
            this.tileQuery = tileQuery;
        } else {
            tileQuery = "select " + name + " from " + dbPath + " where zoom_level = ? AND tile_column = ? AND tile_row = ?";
        }
    }

    private static File findXmlFile( File file ) {
        String nameWithoutExtention = FileUtilities.getNameWithoutExtention(file);
        File xmlFile = new File(file.getParentFile(), nameWithoutExtention + ".xml");
        if (!xmlFile.exists()) {
            // try also file.map.xml
            xmlFile = new File(file.getParentFile(), file.getName() + ".xml");
        }
        return xmlFile;
    }

    public String getSrid() {
        return srid;
    }

    public String getDatabasePath() {
        return this.dbPath;
    }

    public String getFileName() {
        return this.databaseFileName; // file_map.getName();
    }

    public String getName() {
        if ((name == null) || (name.length() == 0)) {
            name = this.dbFile.getName().substring(0, this.dbFile.getName().lastIndexOf("."));
        }
        return this.name;
    }

    public String getDescription() {
        return getName() + " bounds[" + getBoundsAsString() + "] center[" + getCenterAsString() + "]";
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
    // // -----------------------------------------------
    // /**
    // * Set type of map/file
    // *
    // * <p>raster: can be different: mbtiles,db,sqlite,gpkg
    // * <p>mbtiles : mbtiles
    // * <p>map : map
    // *
    // * @return s_name as short name of map/file
    // */
    // public void setMapType( String s_map_type ) {
    // this.mapType = s_map_type;
    // }

    public String getBoundsAsString() {
        return boundsWest + "," + boundsSouth + "," + boundsEast + "," + boundsNorth;
    }

    public String getCenterAsString() {
        return centerX + "," + centerY + "," + defaultZoom;
    }

    public File getFile() {
        return this.dbFile;
    }

    /**
     * Getter for the map style file.
     * 
     * <p>Note that the file might be not existing.
     * 
     * @return the map style file.
     */
    public File getXmlFile() {
        return this.dbStyleFile;
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
        return boundsWest;
    }

    public double getMinLatitude() {
        return boundsSouth;
    }

    public double getMaxLongitude() {
        return boundsEast;
    }

    public double getMaxLatitude() {
        return boundsNorth;
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
