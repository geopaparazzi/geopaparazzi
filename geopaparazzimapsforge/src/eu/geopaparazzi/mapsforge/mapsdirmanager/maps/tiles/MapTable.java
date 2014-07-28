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
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;
/**
 * A map table from the map db.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 *  adapted to work with map databases [mapsforge] Mark Johnson (www.mj10777.de)
 */
@SuppressWarnings("nls")
public class MapTable extends AbstractSpatialTable {

    private static final long serialVersionUID = 1L;
    private File dbStyleFile;
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
    public MapTable( String dbPath, String name, String srid, int minZoom, int maxZoom, double centerX, double centerY,
            String tileQuery, double[] bounds ) {
        super(dbPath, name, SpatialDataType.MAP.getTypeName(), srid, minZoom, maxZoom, centerX, centerY, bounds);

        this.dbStyleFile = findXmlFile(databaseFile);
        checkZooms();

        if (tileQuery != null) {
            this.tileQuery = tileQuery;
        } else {
            tileQuery = "select " + name + " from " + dbPath + " where zoom_level = ? AND tile_column = ? AND tile_row = ?";
        }
    }

    private void checkZooms() {
        if (this.minZoom > this.maxZoom) {
            int i_zoom = this.minZoom;
            this.minZoom = this.maxZoom;
            this.maxZoom = i_zoom;
        }
        if (this.minZoom < 0 || this.minZoom > 22)
            this.minZoom = 0;
        if (this.maxZoom < 0 || this.maxZoom > 22)
            this.maxZoom = 22;
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

    // /**
    // * Function to check and correct bounds / zoom level [for 'SpatialiteDatabaseHandler']
    // *
    // * @param mapCenterLocation [point/zoom to check] (most probably result of
    // PositionUtilities.getMapCenterFromPreferences(preferences,true,true);)
    // * @param doCorrectIfOutOfRange if <code>true</code>, change mapCenterLocation values if out
    // of range.
    // * @return 0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections
    // ; 1= correct tileBounds values.
    // */
    // public int checkCenterLocation( double[] mapCenterLocation, boolean doCorrectIfOutOfRange ) {
    // int i_rc = 0; // inside area
    // if (((mapCenterLocation[0] < boundsWest) || (mapCenterLocation[0] > boundsEast))
    // || ((mapCenterLocation[1] < boundsSouth) || (mapCenterLocation[1] > boundsNorth))
    // || ((mapCenterLocation[2] < minZoom) || (mapCenterLocation[2] > maxZoom))) {
    // if (((mapCenterLocation[0] >= boundsWest) && (mapCenterLocation[0] <= boundsEast))
    // && ((mapCenterLocation[1] >= boundsSouth) && (mapCenterLocation[1] <= boundsNorth))) {
    // /*
    // * We are inside the Map-Area, but Zoom is not correct
    // */
    // if (mapCenterLocation[2] < minZoom) {
    // i_rc = 1;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[2] = minZoom;
    // }
    // }
    // if (mapCenterLocation[2] > maxZoom) {
    // i_rc = 2;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[2] = maxZoom;
    // }
    // }
    // } else {
    // if (mapCenterLocation[2] < minZoom) {
    // i_rc = 11;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[2] = minZoom;
    // }
    // }
    // if (mapCenterLocation[2] > maxZoom) {
    // i_rc = 12;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[2] = maxZoom;
    // }
    // }
    // if ((mapCenterLocation[0] < boundsWest) || (mapCenterLocation[0] > boundsEast)) {
    // i_rc = 13;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[0] = centerX;
    // }
    // }
    // if ((mapCenterLocation[1] < boundsSouth) || (mapCenterLocation[1] > boundsNorth)) {
    // i_rc = 14;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[1] = centerY;
    // }
    // }
    // }
    // }
    // return i_rc;
    // }

    /**
     * Get the tile retrieve query with place holders for zoom, column and row.
     *
     * @return the query to use for this raster set.
     */
    public String getTileQuery() {
        return tileQuery;
    }

    @Override
    public boolean isEditable() {
        return false;
    }

}
