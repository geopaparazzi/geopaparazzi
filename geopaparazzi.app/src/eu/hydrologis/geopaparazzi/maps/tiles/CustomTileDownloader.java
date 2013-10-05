/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.maps.tiles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.database.spatial.core.MbtilesDatabaseHandler;

/**
 * A MapGenerator that downloads tiles from the Mapnik server at OpenStreetMap.
 */
public class CustomTileDownloader extends TileDownloader {

    /**
     * Possible schemas
     */
    private enum TILESCHEMA {
        tms, google, wms
    }

    private static String HOST_NAME;
    private static String PROTOCOL = "http"; //$NON-NLS-1$
    private static byte ZOOM_MIN = 0;
    private static byte ZOOM_MAX = 18;
    private static byte ZOOM_DEFAULT = 0; // mbtiles specific
    private final int minZoom;
    private final int maxZoom;
    private final int defaultZoom; // mbtiles specific
    private final double centerX; // wsg84
    private final double centerY; // wsg84
    private final double bounds_west; // wsg84
    private final double bounds_east; // wsg84
    private final double bounds_north; // wsg84
    private final double bounds_south; // wsg84
    private String s_mbtiles_file; // mbtiles specific
    private File file_mbtiles = null; // mbtiles specific
    private String s_name; // mbtiles specific
    private String s_description; // mbtiles specific
    private String s_format; // mbtiles specific
    private String s_tile_row_type = "tms"; // mbtiles specific
    private int i_force_unique = 0;
    private int i_force_bounds = 1; // after each insert, update bounds and min/max zoom levels
    private MbtilesDatabaseHandler mbtiles_db = null;
    private HashMap<String, String> mbtiles_metadata = null;

    private GeoPoint centerPoint = new GeoPoint(0, 0);

    private String tilePart;
    private boolean isFile = false;
    private TILESCHEMA type = TILESCHEMA.google;

    @SuppressWarnings("nls")
    public CustomTileDownloader( List<String> fileLines, String parentPath ) {
        super();
        double[] bounds = {-180.0, -85.05113, 180, 85.05113};
        double[] center = {0.0, 0.0};
        s_mbtiles_file = "";
        mbtiles_metadata = new LinkedHashMap<String, String>();
        if (GPLog.LOG_HEAVY) {
            try {
                GPLog.addLogEntry("CustomTileDownloader called with:");
                GPLog.addLogEntry("parentPath: " + parentPath);
                for( String fileLine : fileLines ) {
                    GPLog.addLogEntry("-> " + fileLine);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        for( String line : fileLines ) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }

            int split = line.indexOf('=');
            if (split != -1) {
                String value = line.substring(split + 1).trim();
                if (line.startsWith("url")) {

                    int indexOfZ = value.indexOf("ZZZ");
                    if (indexOfZ != -1) {
                        HOST_NAME = value.substring(0, indexOfZ);
                        tilePart = value.substring(indexOfZ);
                    } else {
                        HOST_NAME = "http://";
                    }
                    if (value.startsWith("http")) {
                        // remove http
                        HOST_NAME = HOST_NAME.substring(7);
                        tilePart = value;
                    } else {
                        PROTOCOL = "file";
                        HOST_NAME = parentPath + File.separator + HOST_NAME;
                        isFile = true;
                    }
                }
                if (line.startsWith("minzoom")) {
                    try {
                        ZOOM_MIN = Byte.valueOf(value);
                    } catch (Exception e) {
                        // use default: handle exception
                    }
                }
                if (line.startsWith("maxzoom")) {
                    try {
                        ZOOM_MAX = Byte.valueOf(value);
                    } catch (Exception e) {
                        // use default: handle exception
                    }
                }
                if (line.startsWith("center")) {
                    try {
                        String[] coord = value.split("\\s+"); //$NON-NLS-1$
                        double x = Double.parseDouble(coord[0]);
                        double y = Double.parseDouble(coord[1]);
                        centerPoint = new GeoPoint(y, x);
                    } catch (NumberFormatException e) {
                        // use default
                    }
                }
                if (line.startsWith("type")) {
                    if (value.equals(TILESCHEMA.tms.toString())) {
                        type = TILESCHEMA.tms;
                    }
                    if (value.equals(TILESCHEMA.wms.toString())) {
                        type = TILESCHEMA.wms;
                    }
                }
                if (line.startsWith("mbtiles")) {
                    // HOST_NAME = parentPath + File.separator + HOST_NAME;
                    if (value.startsWith(File.separator)) {
                        value = value.substring(1, value.length() - 2);
                    }
                    s_mbtiles_file = parentPath + File.separator + value;
                    // SpatialDatabasesManager.app_log(-1,"CustomTileDownloader[mbtiles] s_mbtiles_file["+s_mbtiles_file+"]");
                    if (s_mbtiles_file.length() > 0) {
                        file_mbtiles = new File(s_mbtiles_file);
                    }
                }
                if (line.startsWith("bounds")) {
                    try {
                        String[] coord = value.split("\\s+"); //$NON-NLS-1$
                        bounds[0] = Double.parseDouble(coord[0]);
                        bounds[1] = Double.parseDouble(coord[1]);
                        bounds[2] = Double.parseDouble(coord[2]);
                        bounds[3] = Double.parseDouble(coord[3]);
                    } catch (NumberFormatException e) {
                        bounds = new double[]{-180.0, -85.05113, 180, 85.05113};
                    }
                }
                if (line.startsWith("name")) {
                    this.s_name = value;
                }
                if (line.startsWith("description")) {
                    this.s_description = value;
                }
                if (line.startsWith("format")) {
                    this.s_format = value;
                }
                if (line.startsWith("tile_row_type")) {
                    if (value.equals("tms") || value.equals("osm")) {
                        this.s_tile_row_type = value;
                    }
                }
                if (line.startsWith("defaultzoom")) {
                    try {
                        ZOOM_DEFAULT = Byte.valueOf(value);
                    } catch (Exception e) {
                        // use default: handle exception
                    }
                }
                if (line.startsWith("force_unique")) {
                    // will force mbtiles to check image is
                    // unique per insert [blank images are
                    // already determined and not checked]
                    try {
                        i_force_unique = Integer.parseInt(value);
                        if ((i_force_unique < 0) || (i_force_unique > 1))
                            i_force_unique = 0;
                    } catch (Exception e) {
                        i_force_unique = 0;
                    }
                }
                if (line.startsWith("force_bounds")) {
                    // will force mbtiles to check and update
                    // bounds and min/max zoom per insert
                    try {
                        i_force_bounds = Integer.parseInt(value);
                        if ((i_force_bounds < 0) || (i_force_bounds > 1))
                            i_force_bounds = 0;
                    } catch (Exception e) {
                        i_force_bounds = 0;
                    }
                }
            }
        }
        this.centerX = center[0];
        this.centerY = center[1];
        this.bounds_west = bounds[0];
        this.bounds_south = bounds[1];
        this.bounds_east = bounds[2];
        this.bounds_north = bounds[3];
        this.minZoom = ZOOM_MIN;
        this.maxZoom = ZOOM_MAX;
        if (ZOOM_MIN > ZOOM_DEFAULT)
            ZOOM_DEFAULT = ZOOM_MIN;
        this.defaultZoom = ZOOM_DEFAULT;
        if (s_mbtiles_file.length() > 0) {
            if (file_mbtiles.exists()) { // this will open an existing mbtiles_db
                mbtiles_db = new MbtilesDatabaseHandler(file_mbtiles.getAbsolutePath(), null);
            } else { // this will create the mbtiles_db and set default values
                mbtiles_metadata.put("name", this.s_name);
                mbtiles_metadata.put("description", this.s_description);
                mbtiles_metadata.put("format", this.s_format);
                mbtiles_metadata.put("tile_row_type", this.s_tile_row_type);
                String s_bbox = this.bounds_west + "," + this.bounds_south + "," + this.bounds_east + "," + this.bounds_north;
                mbtiles_metadata.put("bounds", s_bbox);
                s_bbox = this.centerX + "," + this.centerY + "," + this.defaultZoom;
                mbtiles_metadata.put("center", s_bbox);
                mbtiles_metadata.put("minzoom", Integer.toString(this.minZoom));
                mbtiles_metadata.put("maxzoom", Integer.toString(this.maxZoom));
                mbtiles_db = new MbtilesDatabaseHandler(file_mbtiles.getAbsolutePath(), mbtiles_metadata);
            }
        }
    }
    public String getHostName() {
        return HOST_NAME;
    }

    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public GeoPoint getStartPoint() {
        return centerPoint;
    }

    @Override
    public Byte getStartZoomLevel() {
        return ZOOM_MIN;
    }

    public String getTilePath( Tile tile ) {
        int zoomLevel = tile.zoomLevel;
        int tileX = (int) tile.tileX;
        int tileY = (int) tile.tileY;

        if (type == TILESCHEMA.tms) {
            int[] tmsTiles = Utilities.googleTile2TmsTile(tileX, tileY, zoomLevel);
            tileX = tmsTiles[0];
            tileY = tmsTiles[1];
        }

        if (type == TILESCHEMA.tms || type == TILESCHEMA.google) {
            String tmpTilePart = tilePart.replaceFirst("ZZZ", String.valueOf(zoomLevel)); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst("XXX", String.valueOf(tileX)); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst("YYY", String.valueOf(tileY)); //$NON-NLS-1$
            return tmpTilePart;
        }
        if (type == TILESCHEMA.wms) {
            // minx, miny, maxx, maxy
            double[] tileBounds = Utilities.tileLatLonBounds(tileX, tileY, zoomLevel, Tile.TILE_SIZE);
            String tmpTilePart = tilePart.replaceFirst("XXX", String.valueOf(tileBounds[0])); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst("YYY", String.valueOf(tileBounds[1])); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst("XXX", String.valueOf(tileBounds[2])); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst("YYY", String.valueOf(tileBounds[3])); //$NON-NLS-1$
            return tmpTilePart;
        }
        return ""; //$NON-NLS-1$
    }

    @Override
    public boolean executeJob( MapGeneratorJob mapGeneratorJob, Bitmap bitmap ) {
        try {
            Tile tile = mapGeneratorJob.tile;
            String tilePath = getTilePath(tile);
            int i_zoom = tile.zoomLevel;
            int i_tile_x = (int) tile.tileX;
            int i_tile_y_osm = (int) tile.tileY;
            if (mbtiles_db != null) { // try to retrieve this tile from the active mbtiles.db
                if (mbtiles_db.getBitmapTile(i_tile_x, i_tile_y_osm, i_zoom, Tile.TILE_SIZE, bitmap)) {
                    // tile was found and the bitmap filled, return
                    return true;
                }
            }
            StringBuilder sb = new StringBuilder();
            if (isFile) {
                sb.append("file:"); //$NON-NLS-1$
            } else {
                if (!tilePath.startsWith("http")) //$NON-NLS-1$
                    sb.append("http://"); //$NON-NLS-1$
            }
            sb.append(HOST_NAME);
            sb.append(tilePath);

            URL url = new URL(sb.toString());
            InputStream inputStream = url.openStream();
            Bitmap decodedBitmap = null;
            try {
                decodedBitmap = BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
                // ignore and set the image as empty
                if (GPLog.LOG_HEAVY)
                    GPLog.addLogEntry(this, "Could not find image: " + sb.toString()); //$NON-NLS-1$
            } finally {
                inputStream.close();
            }
            // check if the input stream could be decoded into a bitmap
            if (decodedBitmap != null) {
                if (mbtiles_db != null) {
                    // we have a valid image, store this to the active mbtiles.db
                    // [this must be done before recycle() is called]
                    // decodedBitmap == ARGB_8888 ; bitmap == RGB_565
                    mbtiles_db.insertBitmapTile(i_tile_x, i_tile_y_osm, i_zoom, decodedBitmap, i_force_unique, i_force_bounds);
                }
                // copy all pixels from the decoded bitmap to the color array
                decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
                decodedBitmap.recycle();
            } else {
                for( int i = 0; i < pixels.length; i++ ) {
                    pixels[i] = Color.WHITE;
                }
            }

            // copy all pixels from the color array to the tile bitmap
            bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
            return true;
        } catch (UnknownHostException e) {
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // TODO mj10777: check if this is safe after final has been removed from TileDownloader
    public void cleanup() {
        if (mbtiles_db != null) {
            try {
                mbtiles_db.update_bounds();
                mbtiles_db.close();
                mbtiles_db = null;
            } catch (Exception e) {
                // ignore
            }
        }
    }
    public byte getZoomLevelMax() {
        return ZOOM_MAX;
    }

    public static CustomTileDownloader file2TileDownloader( File file, String parentPath ) throws IOException {
        List<String> fileLines = FileUtilities.readfileToList(file);
        return new CustomTileDownloader(fileLines, parentPath);
    }

    /**
     * Function to check and correct bounds / zoom level [for 'CustomDownloader']
     *
     * <p>i_y_osm must be in is Open-Street-Map 'Slippy Map' notation 
     * [will be converted to 'tms' notation if needed]
     *
     * @param mapCenterLocation [point/zoom to check] result of PositionUtilities.getMapCenterFromPreferences(preferences,true,true);
     * @param doCorrectIfOutOfRange if <code>true</code>, change mapCenterLocation values if out of range
     * @return 0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; 
     *          i_parm=0 no corrections ; 1= correct tileBounds values.
     */
    public int checkCenterLocation( double[] mapCenterLocation, boolean doCorrectIfOutOfRange ) {
        int i_rc = 0; // inside area
        // SpatialDatabasesManager.app_log(-1,"CustomTileDownloader.checkCenterLocation: center_location[x="+mapCenterLocation[0]+" ; y="+mapCenterLocation[1]+" ; z="+mapCenterLocation[2]+"] bbox=["+bounds_west+","+bounds_south+","+bounds_east+","+bounds_north+"]");
        if (((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east))
                || ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north))
                || ((mapCenterLocation[2] < minZoom) || (mapCenterLocation[2] > maxZoom))) {
            if (((mapCenterLocation[0] >= bounds_west) && (mapCenterLocation[0] <= bounds_east))
                    && ((mapCenterLocation[1] >= bounds_south) && (mapCenterLocation[1] <= bounds_north))) {
                // We are inside the Map-Area, but Zoom is not correct
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
                if ((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east)) {
                    i_rc = 13;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[0] = centerX;
                    }
                }
                if ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north)) {
                    i_rc = 14;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[1] = centerY;
                    }
                }
            }
        }
        return i_rc;
    }
    // public void setMapView( MapView mapView ) {
    // this.mapView = mapView;
    // }

}
