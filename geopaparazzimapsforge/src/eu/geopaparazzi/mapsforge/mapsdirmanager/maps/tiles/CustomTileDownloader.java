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
package eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;
import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.MbtilesDatabaseHandler;

/**
 * A MapGenerator that downloads tiles from the Mapnik server at OpenStreetMap.
 */
@SuppressWarnings("nls")
public class CustomTileDownloader extends TileDownloader {

    private static final int ZOOM_LEVEL_DIFF = 1;

    private static final String YYY_STR = "YYY";
    private static final String XXX_STR = "XXX";
    private static final String ZZZ_STR = "ZZZ";
    private static final String SSS_STR = "SSS";
    private static final String URL_STR = "url";
    private static final String GEOPAPARAZZI_STR = "Geopaparazzi";
    private static final String USER_AGENT_STR = "User-Agent";
    private static final String HTTP_STR = "http";
    private static final String HTTP_PROTOCOL_STR = "http://";
    private static final String FILE_PROTOCOL_STR = "file:";
    private static final String REQUEST_PROTOCOL_STR = "request_protocol";
    private static final String REQUEST_Y_TYPE_STR = "request_y_type";
    private static final String REQUEST_ZOOM_LEVELS_URL_STR = "request_zoom_levels_url";
    private static final String REQUEST_BOUNDS_URL_STR = "request_bounds_url";
    private static final String REQUEST_URL_STR = "request_url";
    private static final String FILE_STR = "file";
    private static final String LOAD_STR = "load";
    private static final String DELETE_STR = "delete";
    private static final String RESET_METADATA_STR = "reset_metadata";
    private static final String UPDATE_BOUNDS_STR = "update_bounds";
    private static final String VACUUM_STR = "vacuum";
    private static final String DROP_STR = "drop";
    private static final String REPLACE_STR = "replace";
    private static final String FILL_STR = "fill";
    private static final String RUN_STR = "run";
    private static final String REQUEST_ZOOM_LEVELS_STR = "request_zoom_levels";
    private static final String REQUEST_BOUNDS_STR = "request_bounds";
    private static final String REQUEST_TYPE_STR = "request_type";
    private static final String FORCE_UNIQUE_STR = "force_unique";
    private static final String DEFAULTZOOM_STR = "defaultzoom";
    private static final String TILE_ROW_TYPE_STR = "tile_row_type";
    private static final String MBTILES_STR = "mbtiles";
    private static final String TYPE_STR = "type";
    private static final String MAXZOOM_STR = "maxzoom";
    private static final String MINZOOM_STR = "minzoom";
    private static final String CENTER_STR = "center";
    private static final String BOUNDS_STR = "bounds";
    private static final String FORMAT_STR = "format";
    private static final String NAME_STR = "name";
    private static final String DESCRIPTION_STR = "description";

    /**
     * Possible schemas
     */
    private enum TILESCHEMA {
        tms, google, wms, osm
    }
    // no wonder this was causing problems, must must NOT be static with a manager
    private String HOST_NAME = "";
    private String PROTOCOL = HTTP_STR; //$NON-NLS-1$
    private byte ZOOM_MIN = 0;
    private byte ZOOM_MAX = 22;
    private byte ZOOM_DEFAULT = 14; // mbtiles specific
    private final int minZoom;
    private final int maxZoom;
    private final int defaultZoom; // mbtiles specific
    private final double centerX; // wsg84
    private final double centerY; // wsg84
    private final double boundsWest; // wsg84
    private final double boundsEast; // wsg84
    private final double boundsNorth; // wsg84
    private final double boundsSouth; // wsg84
    private String mbtilesFilePath; // mbtiles specific
    private File mbtilesFile = null; // mbtiles specific
    private String name; // mbtiles specific
    private String description; // mbtiles specific
    private String format; // mbtiles specific
    private String tileRowType = "tms"; // mbtiles specific
    private int i_force_unique = 0;
    private MbtilesDatabaseHandler mbtilesDatabase = null;
    private HashMap<String, String> mapurlMetadata = null; // list for future editing
    private HashMap<String, String> mbtilesMetadataMap = null; // list for mbtiles support
    private HashMap<String, String> mbtilesRequestUrl = null; // list for mbtiles request support
    private int i_tile_server = 0; // if no 'SSS' is found, server logic will not be called
    private String requestType = "";
    private String requestUrl = "";
    private String requestBounds = "";
    private String requestedZoomLevels = "";
    private GeoPoint centerPoint = new GeoPoint(0, 0);

    private String tilePart = "";
    private boolean isFile = false;
    private boolean doResetMetadata = false;
    private TILESCHEMA type = TILESCHEMA.google;
    private boolean isConnectedToInternet;
    private boolean doScaleTiles;

    private SharedPreferences preferences;

    /**
     * Constructor.
     * 
     * @param sourceFile the source file to use as tile source definition.
     * @param parentPath the parent path.
     * @throws IOException if something goes wrong.
     */
    public CustomTileDownloader( File sourceFile, String parentPath ) throws IOException {
        super();

        Context context = GPApplication.getInstance();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        this.name = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf("."));
        List<String> fileLines = new ArrayList<String>();
        try {
            fileLines = FileUtilities.readfileToList(sourceFile);
        } catch (IOException e) {
            GPLog.error(this, getClass().getSimpleName() + "[CustomTileDownloader.FileUtilities.readfileToList]", e);
        }
        // parentPath = '/mnt/sdcard/maps' : this will be appended to all pathis given in the
        // 'mapurl' file
        double[] bounds = new double[]{-180.0, -85.05113, 180, 85.05113};
        double[] center = {0.0, 0.0};
        double[] request_bounds = new double[]{0.0, 0.0, 0.0, 0.0};
        mbtilesFilePath = "";
        mapurlMetadata = new LinkedHashMap<String, String>();
        mbtilesMetadataMap = new LinkedHashMap<String, String>();
        mbtilesRequestUrl = new LinkedHashMap<String, String>();
        if (GPLog.LOG_ABSURD) {
            StringBuilder sb = new StringBuilder();
            sb.append("CustomTileDownloader called with:\n");
            sb.append("parentPath: ");
            sb.append(parentPath);
            sb.append("\n");
            for( String fileLine : fileLines ) {
                sb.append("-> " + fileLine);
                sb.append("\n");
            }
            GPLog.addLogEntry(sb.toString());
        }

        for( String line : fileLines ) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }

            int split = line.indexOf('=');
            if (split < 0) { // some sort of comment, save back when editing mapurl file
                mapurlMetadata.put(line, "");
            }
            if (split != -1) {
                String parm = line.substring(0, split);
                String value = line.substring(split + 1).trim();
                // save value for future editing
                mapurlMetadata.put(parm, value); // parm without '='
                // GPLog.androidLog(-1,"CustomTileDownloader parm[" + parm+ "] value[" + value+
                // "]");
                if (line.startsWith(URL_STR)) {
                    requestUrl = value;
                    int indexOfS = value.indexOf(SSS_STR);
                    if (indexOfS != -1) {
                        i_tile_server = 1; // Server logic will not be called [1,2]
                    }
                    int indexOfZ = value.indexOf(ZZZ_STR);
                    if (indexOfZ != -1) {
                        // tile_servers and local files [order of ZZZ,XXX,YY is no longer inportant]
                        // url=http://mt1.google.com/vt/lyrs=s,h&x=XXX&y=YYY&z=ZZZ
                        // url=mytilesfolder/ZZZ/XXX/YYY.png
                        // url=http://tile.openstreetmap.org/ZZZ/XXX/YYY.png
                        String s_work = value;
                        if (value.startsWith(HTTP_STR)) { // tms_server
                            s_work = value.substring(7); // removed: 'http://'
                        }
                        int indexOfSeperator = s_work.indexOf("/");
                        // tms_servers and local files will always have a '/' in them
                        HOST_NAME = s_work.substring(0, indexOfSeperator);
                        tilePart = s_work.substring(indexOfSeperator);
                        if (!value.startsWith(HTTP_STR)) { // local files
                            PROTOCOL = FILE_STR;
                            HOST_NAME = parentPath + File.separator + HOST_NAME;
                            isFile = true;
                        }
                    } else {
                        int indexOfParms = value.indexOf("?");
                        // wms server should always have a '?' in them
                        HOST_NAME = value.substring(7, indexOfParms); // removed: 'http://'
                        tilePart = value.substring(indexOfParms);
                    }
                }
                if (line.startsWith(MINZOOM_STR)) {
                    try {
                        byte b_zoom = Byte.valueOf(value);
                        if ((b_zoom >= 0) && (b_zoom <= 22)) {
                            ZOOM_MIN = b_zoom;
                        }
                    } catch (Exception e) {
                        // use default: handle exception
                    }
                }
                if (line.startsWith(MAXZOOM_STR)) {
                    try {
                        byte b_zoom = Byte.valueOf(value);
                        if ((b_zoom >= 0) && (b_zoom <= 22)) {
                            ZOOM_MAX = b_zoom;
                        }
                    } catch (Exception e) {
                        // use default: handle exception
                    }
                }
                if (line.startsWith(CENTER_STR)) {
                    try {
                        String[] coord = value.split("\\s+"); //$NON-NLS-1$
                        double x = Double.parseDouble(coord[0]);
                        double y = Double.parseDouble(coord[1]);
                        center[0] = x;
                        center[1] = y;
                        centerPoint = new GeoPoint(y, x);
                    } catch (NumberFormatException e) {
                        // use default
                    }
                }
                if (line.startsWith(TYPE_STR)) {
                    if (value.equals(TILESCHEMA.tms.toString())) {
                        type = TILESCHEMA.tms;
                    }
                    if (value.equals(TILESCHEMA.wms.toString())) {
                        type = TILESCHEMA.wms;
                    }
                }
                if (line.startsWith(MBTILES_STR)) {
                    // HOST_NAME = parentPath + File.separator + HOST_NAME;
                    if (value.startsWith(File.separator)) {
                        value = value.substring(1, value.length() - 2);
                    }
                    mbtilesFilePath = parentPath + File.separator + value;
                    // GPLog.androidLog(-1,"CustomTileDownloader[mbtiles] s_mbtiles_file["+s_mbtiles_file+"]");
                    if (mbtilesFilePath.length() > 0) {
                        mbtilesFile = new File(mbtilesFilePath);
                    }
                }
                if (line.startsWith(BOUNDS_STR)) {
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
                if (line.startsWith(NAME_STR)) {
                    this.name = value;
                }
                if (line.startsWith(DESCRIPTION_STR)) {
                    this.description = value;
                }
                if (line.startsWith(FORMAT_STR)) {
                    this.format = value;
                }
                if (line.startsWith(TILE_ROW_TYPE_STR)) {
                    if (value.equals(TILESCHEMA.tms.toString()) || value.equals(TILESCHEMA.osm.toString())) {
                        this.tileRowType = value;
                    }
                }
                if (line.startsWith(DEFAULTZOOM_STR)) {
                    try {
                        byte b_zoom = Byte.valueOf(value);
                        if ((b_zoom >= 0) && (b_zoom <= 22)) {
                            ZOOM_DEFAULT = b_zoom;
                        }
                    } catch (Exception e) {
                        // use default: handle exception
                    }
                }
                if (line.startsWith(FORCE_UNIQUE_STR)) {
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
                if (line.startsWith(REQUEST_TYPE_STR)) {
                    if (!value.equals("off"))
                        requestType = value;
                }
                if (line.startsWith(REQUEST_BOUNDS_STR)) {
                    requestBounds = value;
                    try {
                        String[] coord = value.split("\\s+"); //$NON-NLS-1$
                        request_bounds[0] = Double.parseDouble(coord[0]);
                        request_bounds[1] = Double.parseDouble(coord[1]);
                        request_bounds[2] = Double.parseDouble(coord[2]);
                        request_bounds[3] = Double.parseDouble(coord[3]);
                    } catch (NumberFormatException e) {
                        requestBounds = "";
                    }
                }
                if (line.startsWith(REQUEST_ZOOM_LEVELS_STR)) {
                    requestedZoomLevels = value;
                }
            }
        }
        this.centerX = center[0];
        this.centerY = center[1];
        this.boundsWest = bounds[0];
        this.boundsSouth = bounds[1];
        this.boundsEast = bounds[2];
        this.boundsNorth = bounds[3];
        if (ZOOM_MIN > ZOOM_MAX) {
            byte b_zoom = ZOOM_MIN;
            ZOOM_MIN = ZOOM_MAX;
            ZOOM_MAX = b_zoom;
        }
        this.minZoom = ZOOM_MIN;
        this.maxZoom = ZOOM_MAX;
        if (ZOOM_MIN > ZOOM_DEFAULT)
            ZOOM_DEFAULT = ZOOM_MIN;
        this.defaultZoom = ZOOM_DEFAULT;
        setDescription(this.description);
        if (mbtilesFilePath.length() > 0) {
            if (!requestType.equals("")) {
                int run = 0;
                int create = 0;
                int indexOfS = requestType.indexOf(",");
                if (indexOfS != -1) {
                    String[] requestTypeTokens = requestType.split(",");
                    requestType = "";
                    String comma = "";
                    for( int i = 0; i < requestTypeTokens.length; i++ ) {
                        if (requestTypeTokens[i].equals(RUN_STR)) {
                            // no 'run', no fun [ignore all commands]
                            run++;
                        }
                        if (requestTypeTokens[i].equals(FILL_STR)) {
                            // will request missing tiles only
                            if (!requestType.equals(""))
                                comma = ",";
                            requestType += comma + requestTypeTokens[i].trim();
                            if (requestedZoomLevels.equals("")) { // if not set, do all
                                requestedZoomLevels = Integer.toString(this.minZoom) + "-" + Integer.toString(this.maxZoom);
                            }
                            create = 1;
                        }
                        if (requestTypeTokens[i].equals(REPLACE_STR)) {
                            // will replace existing tiles
                            if (!requestType.equals(""))
                                comma = ",";
                            requestType += comma + requestTypeTokens[i].trim();
                            create = 1;
                            // if both 'fill' and 'replace' are given: 'fill' will
                            // be used
                        }
                        if (requestTypeTokens[i].equals(DROP_STR)) {
                            // will delete the requested tiles,
                            // retaining the already downloaded
                            // tiles
                            if (!requestType.equals(""))
                                comma = ",";
                            requestType += comma + requestTypeTokens[i].trim();
                        }
                        if (requestTypeTokens[i].equals(VACUUM_STR)) {
                            if (!requestType.equals(""))
                                comma = ",";
                            requestType += comma + requestTypeTokens[i].trim();
                        }
                        if (requestTypeTokens[i].equals(UPDATE_BOUNDS_STR)) {
                            if (!requestType.equals(""))
                                comma = ",";
                            requestType += comma + requestTypeTokens[i].trim();
                        }
                        if (requestTypeTokens[i].equals(RESET_METADATA_STR)) {
                            doResetMetadata = true;
                        }
                        if (requestTypeTokens[i].equals(DELETE_STR)) { // planned for future
                            if (!requestType.equals(""))
                                comma = ",";
                            requestType += comma + requestTypeTokens[i].trim();
                        }
                        if (requestTypeTokens[i].equals(LOAD_STR)) {
                            if (!requestType.equals(""))
                                comma = ",";
                            requestType += comma + requestTypeTokens[i].trim();
                        }
                        // GPLog.androidLog(-1, "CustomTileDownloader sa_string[" + i + "].[" +
                        // sa_string[i] + "] ["+ s_request_type + "]");
                    }
                    if (create != 1) {
                        requestBounds = "";
                        requestedZoomLevels = "";
                        requestUrl = "";
                    }
                }
                if (run > 0) {
                    mbtilesRequestUrl.put(REQUEST_TYPE_STR, requestType);
                }
                if ((!requestedZoomLevels.equals("")) && (!requestUrl.equals(""))) {
                    String s_bbox = this.boundsWest + "," + this.boundsSouth + "," + this.boundsEast + "," + this.boundsNorth;
                    if (!requestBounds.equals("")) {
                        requestBounds = request_bounds[0] + "," + request_bounds[1] + "," + request_bounds[2] + ","
                                + request_bounds[3];
                    } else {
                        // simplify filling of upper zoom-levels, fill supported area
                        requestBounds = s_bbox;
                        request_bounds = bounds;
                    }
                    if ((request_bounds[0] >= bounds[0]) && (request_bounds[2] <= bounds[2]) && (request_bounds[1] >= bounds[1])
                            && (request_bounds[3] <= bounds[3])) {
                        if (PROTOCOL.equals(FILE_STR)) {
                            // this must be the absolute path ; 'file:'
                            // will be added later after checking if the
                            // file exists
                            requestUrl = parentPath + File.separator + requestUrl;
                        }
                        mbtilesRequestUrl.put(REQUEST_URL_STR, requestUrl);
                        mbtilesRequestUrl.put(REQUEST_BOUNDS_STR, requestBounds);
                        mbtilesRequestUrl.put(REQUEST_BOUNDS_URL_STR, s_bbox);
                        s_bbox = Integer.toString(this.minZoom) + "-" + Integer.toString(this.maxZoom);
                        mbtilesRequestUrl.put(REQUEST_ZOOM_LEVELS_URL_STR, s_bbox);
                        mbtilesRequestUrl.put(REQUEST_ZOOM_LEVELS_STR, requestedZoomLevels);
                        String requestYType = TILESCHEMA.wms.toString();
                        // 0=osm ; 1=tms ; 2=wms
                        if (type == TILESCHEMA.google) {
                            requestYType = TILESCHEMA.osm.toString();
                        } else if (type == TILESCHEMA.tms) {
                            requestYType = TILESCHEMA.tms.toString();
                        }
                        mbtilesRequestUrl.put(REQUEST_Y_TYPE_STR, requestYType);
                        mbtilesRequestUrl.put(REQUEST_PROTOCOL_STR, PROTOCOL);
                        // GPLog.androidLog(-1, "CustomTileDownloader [" + PROTOCOL + "] ["+
                        // s_request_url + "]");
                    }
                }
            }
            if (!mbtilesFile.exists() || doResetMetadata) {
                mbtilesMetadataMap.put(NAME_STR, this.name);
                mbtilesMetadataMap.put(DESCRIPTION_STR, this.description);
                if (!mbtilesFile.exists()) {
                    mbtilesMetadataMap.put(FORMAT_STR, this.format);
                    mbtilesMetadataMap.put(TILE_ROW_TYPE_STR, this.tileRowType);
                }
                // 'reset_metadata': will manually reset the mbtiles-metadata entries:
                // 'name','description','bounds','center','minzoom','maxzoom' ; NOT:
                // 'format','tile_row_type'
                String s_bbox = this.boundsWest + "," + this.boundsSouth + "," + this.boundsEast + "," + this.boundsNorth;
                mbtilesMetadataMap.put(BOUNDS_STR, s_bbox);
                s_bbox = this.centerX + "," + this.centerY + "," + this.defaultZoom;
                mbtilesMetadataMap.put(CENTER_STR, s_bbox);
                mbtilesMetadataMap.put(MINZOOM_STR, Integer.toString(this.minZoom));
                mbtilesMetadataMap.put(MAXZOOM_STR, Integer.toString(this.maxZoom));
            }
            if (mbtilesFile.exists()) { // this will open an existing mbtiles_db
                mbtilesDatabase = new MbtilesDatabaseHandler(mbtilesFile.getAbsolutePath(), null);
            } else { // this will create the mbtiles_db and set default values
                mbtilesDatabase = new MbtilesDatabaseHandler(mbtilesFile.getAbsolutePath(), mbtilesMetadataMap);
            }
            if (mbtilesRequestUrl.size() > 0) {
                mbtilesDatabase.runRetrieveUrl(mbtilesRequestUrl, mbtilesMetadataMap);
            }
        }
        // GPLog.androidLog(-1,"CustomTileDownloader parentPath[" + parentPath+ "]");
    }
    public String getHostName() {
        return HOST_NAME;
    }

    /**
     * @return the tile related part of the request.
     */
    public String getTilePart() {
        return tilePart;
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

    /**
      * Return Min Zoom
      *
      * <p>default :  0
      * <p>mbtiles : taken from value of metadata 'minzoom'
      * <p>map : value is given in 'StartZoomLevel'
      *
      * @return integer minzoom
      */
    public int getMinZoom() {
        return minZoom;
    }

    /**
      * Return Max Zoom
      *
      * <p>default :  22
      * <p>mbtiles : taken from value of metadata 'maxzoom'
      * <p>map : value not defined, seems to calculate bitmap from vector data [18]
      *
      * @return integer maxzoom
      */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
      * Return West X Value [Longitude]
      *
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
      *
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
      *
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
      *
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
      *
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
      *
      * <p>default : center of bounds
      * <p>mbtiles : taken from 2nd value of metadata 'center'
      *
      * @return double of Y Value [Latitude]
      */
    public double getCenterY() {
        return centerY;
    }

    /**
      * Retrieve Zoom level
      *
      * <p>default : minZoom
      * <p>mbtiles : taken from 3rd value of metadata 'center'
      *
     * @return defaultZoom
      */
    public int getDefaultZoom() {
        return defaultZoom;
    }

    /**
      * Return short name of map/file
      *
      * <p>default: file name without path and extention
      * <p>mbtiles : metadata 'name'
      * <p>map : will be value of 'comment', if not null
      *
      * @return s_name as short name of map/file
      */
    public String getName() {
        return name;
    }

    /**
      * Return String of bounds [wms-format]
      *
      * <p>x_min,y_min,x_max,y_max
      *
      * @return bounds formatted using wms format
      */
    public String getBounds_toString() {
        return boundsWest + "," + boundsSouth + "," + boundsEast + "," + boundsNorth;
    }

    /**
      * Return String of Map-Center with default Zoom
      *
      * <p>x_position,y_position,default_zoom
      *
      * @return center formatted using mbtiles format
      */
    public String getCenter_toString() {
        return centerX + "," + centerY + "," + defaultZoom;
    }

    /**
      * Return long description of map/file
      *
      * <p>default: s_name with bounds and center
      * <p>mbtiles : metadata description'
      * <p>map : will be value of 'comment', if not null
      *
      * @return s_description long description of map/file
      */
    public String getDescription() {
        if ((this.description == null) || (this.description.length() == 0) || (this.description.equals(this.name)))
            setDescription(getName());
        // will set default values with bounds and center if it is
        // the same as 's_name' or empty
        return this.description;
    }

    /**
      * Set long description of map/file
      *
      * <p>default: s_name with bounds and center
      * <p>mbtiles : metadata description'
      * <p>map : will be value of 'comment', if not null
      * 
      * @param s_description long description of map/file 
      */
    public void setDescription( String s_description ) {
        if ((s_description == null) || (s_description.length() == 0) || (s_description.equals(this.name))) {
            this.description = getName() + " bounds[" + getBounds_toString() + "] center[" + getCenter_toString() + "]";
        } else
            this.description = s_description;
    }

    /**
     * @return the mbtiles database handler.
     */
    public MbtilesDatabaseHandler getMBTilesDatabase() {
        return mbtilesDatabase;
    }

    public String getTilePath( Tile tile ) {
        int zoomLevel = tile.zoomLevel;
        int tileX = (int) tile.tileX;
        int tileY = (int) tile.tileY;

        doScaleTiles = preferences.getBoolean("PREFS_KEY_RETINA", false);
        if (type != TILESCHEMA.wms && doScaleTiles) {
            tileX = tileX / (2 * ZOOM_LEVEL_DIFF);
            tileY = tileY / (2 * ZOOM_LEVEL_DIFF);
            zoomLevel = zoomLevel - ZOOM_LEVEL_DIFF;
        }
        if (type == TILESCHEMA.tms) {
            int[] tmsTiles = Utilities.googleTile2TmsTile(tileX, tileY, zoomLevel);
            tileX = tmsTiles[0];
            tileY = tmsTiles[1];
        }
        if (type == TILESCHEMA.tms || type == TILESCHEMA.google) {
            String tmpTilePart = tilePart.replaceFirst(ZZZ_STR, String.valueOf(zoomLevel)); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst(XXX_STR, String.valueOf(tileX)); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst(YYY_STR, String.valueOf(tileY)); //$NON-NLS-1$
            return tmpTilePart;
        } else if (type == TILESCHEMA.wms) {
            // minx, miny, maxx, maxy
            double[] tileBounds = Utilities.tileLatLonBounds(tileX, tileY, zoomLevel, Tile.TILE_SIZE);
            String tmpTilePart = tilePart.replaceFirst(XXX_STR, String.valueOf(tileBounds[0])); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst(YYY_STR, String.valueOf(tileBounds[1])); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst(XXX_STR, String.valueOf(tileBounds[2])); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst(YYY_STR, String.valueOf(tileBounds[3])); //$NON-NLS-1$
            return tmpTilePart;
        }
        return ""; //$NON-NLS-1$
    }

    @Override
    public boolean executeJob( MapGeneratorJob mapGeneratorJob, Bitmap bitmap ) {
        try {
            Tile tile = mapGeneratorJob.tile;
            int tileSize = Tile.TILE_SIZE;
            String tilePath = getTilePath(tile);
            int zoom = tile.zoomLevel;
            int tileX = (int) tile.tileX;
            int tileYOsm = (int) tile.tileY;
            if (mbtilesDatabase != null) { // try to retrieve this tile from the active mbtiles.db
                if (mbtilesDatabase.getBitmapTile(tileX, tileYOsm, zoom, tileSize, bitmap)) {
                    // tile was found and the bitmap filled, return
                    // GPLog.androidLog(-1,"CustomTileDownloader.executeJob: name["+getName()
                    // +"] mbtiles_db["+mbtiles_db.getFileName()+"] tilePath["+i_zoom+"/"+i_tile_x+"/"+i_tile_y_osm+"] ");
                    return true;
                }
            }
            StringBuilder sb = new StringBuilder();
            if (isFile) {
                sb.append(FILE_PROTOCOL_STR); //$NON-NLS-1$
            } else {
                if (!tilePath.startsWith(HTTP_STR)) //$NON-NLS-1$
                    sb.append(HTTP_PROTOCOL_STR); //$NON-NLS-1$
            }
            String s_host_name = HOST_NAME;
            if (i_tile_server > 0) {
                s_host_name = s_host_name.replaceFirst(SSS_STR, String.valueOf(i_tile_server++)); //$NON-NLS-1$
                if (i_tile_server > 2)
                    i_tile_server = 1;
            }
            sb.append(s_host_name);
            sb.append(tilePath);
            // GPLog.androidLog(-1,"CustomTileDownloader.executeJob: name["+getName()+"] host_name["+s_host_name+"] tilePath["+tilePath+"] ");
            if (isFile) {
                if (GPLog.LOG_ABSURD)
                    GPLog.androidLog(-1, "CustomTileDownloader.executeJob: request[" + sb.toString() + "] ");
            }
            Bitmap decodedBitmap = null;

            Context context = GPApplication.getInstance();
            if (context != null) {
                isConnectedToInternet = NetworkUtilities.isNetworkAvailable(context);
            }
            if (isConnectedToInternet || isFile) {
                URL url = new URL(sb.toString());
                InputStream inputStream = null;
                try {
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.setRequestProperty(USER_AGENT_STR, GEOPAPARAZZI_STR);
                    inputStream = urlConnection.getInputStream();
                    decodedBitmap = BitmapFactory.decodeStream(inputStream);
                    if (doScaleTiles && type != TILESCHEMA.wms)
                        decodedBitmap = resize(decodedBitmap, tileX, tileYOsm, ZOOM_LEVEL_DIFF, tileSize);
                } catch (Exception e) {
                    // ignore and set the image as empty
                    if (GPLog.LOG_HEAVY)
                        GPLog.addLogEntry(this, "Could not find image: " + sb.toString()); //$NON-NLS-1$
                } finally {
                    if (inputStream != null)
                        inputStream.close();
                }
            }
            // check if the input stream could be decoded into a bitmap
            if (decodedBitmap != null) {
                if (mbtilesDatabase != null) {
                    // we have a valid image, store this to the active mbtiles.db
                    // [this must be done before recycle() is called]
                    // decodedBitmap == ARGB_8888 ; bitmap == RGB_565
                    mbtilesDatabase.insertBitmapTile(tileX, tileYOsm, zoom, decodedBitmap, i_force_unique);
                }
                // copy all pixels from the decoded bitmap to the color array
                decodedBitmap.getPixels(this.pixels, 0, tileSize, 0, 0, tileSize, tileSize);
                // GPLog.androidLog(-1,"CustomTileDownloader.executeJob: retrieved["+i_zoom+"/"+i_tile_x+"/"+i_tile_y_osm+"] ");
                decodedBitmap.recycle();
            } else {
                for( int i = 0; i < pixels.length; i++ ) {
                    pixels[i] = Color.WHITE;
                }
            }
            // copy all pixels from the color array to the tile bitmap
            bitmap.setPixels(this.pixels, 0, tileSize, 0, 0, tileSize, tileSize);
            return true;
        } catch (UnknownHostException e) {
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Bitmap resize( Bitmap bitmap, final int rx, final int ry, final int zoomLevelDiff, int mTileSizePixels ) {
        int px = rx % (2 * zoomLevelDiff);
        int py = ry % (2 * zoomLevelDiff);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, //
                (mTileSizePixels * 2) * zoomLevelDiff, //
                (mTileSizePixels * 2) * zoomLevelDiff, false);
        int x = (px == 0) ? 0 : mTileSizePixels * px;
        int y = (py == 0) ? 0 : mTileSizePixels * py;
        Bitmap bitmapResized = Bitmap.createBitmap(scaledBitmap, x, y, mTileSizePixels, mTileSizePixels);
        return bitmapResized;
    }

    // TODO mj10777: check if this is safe after final has been removed from TileDownloader
    public void cleanup() {
        if (mbtilesDatabase != null) {
            try {
                mbtilesDatabase.close();
                mbtilesDatabase = null;
            } catch (Exception e) {
                // ignore
            }
        }
    }
    public byte getZoomLevelMax() {
        return ZOOM_MAX;
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
        if (((mapCenterLocation[0] < boundsWest) || (mapCenterLocation[0] > boundsEast))
                || ((mapCenterLocation[1] < boundsSouth) || (mapCenterLocation[1] > boundsNorth))
                || ((mapCenterLocation[2] < minZoom) || (mapCenterLocation[2] > maxZoom))) {
            if (((mapCenterLocation[0] >= boundsWest) && (mapCenterLocation[0] <= boundsEast))
                    && ((mapCenterLocation[1] >= boundsSouth) && (mapCenterLocation[1] <= boundsNorth))) {
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

}
