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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import eu.geopaparazzi.library.GeopaparazziLibraryContextHolder;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.CustomTileDatabasesManager;
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
    // no wonder this was causing problems, must must NOT be static with a manager
    private String HOST_NAME = "";
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
    private File file_map; // all DatabaseHandler/Table classes should use these names
    private String s_map_file; // [with path] all DatabaseHandler/Table classes should use these
                               // names
    private String s_name_file; // [without path] all DatabaseHandler/Table classes should use these
                                // names
    private String s_mbtiles_file; // mbtiles specific
    private File file_mbtiles = null; // mbtiles specific
    private String s_name; // mbtiles specific
    private String s_description; // mbtiles specific
    private String s_format; // mbtiles specific
    private String s_tile_row_type = "tms"; // mbtiles specific
    private int i_force_unique = 0;
    private MbtilesDatabaseHandler mbtiles_db = null;
    private HashMap<String, String> mapurl_metadata = null; // list for future editing
    private HashMap<String, String> mbtiles_metadata = null; // list for mbtiles support
    private HashMap<String, String> mbtiles_request_url = null; // list for mbtiles request support
    private int i_tile_server = 0; // if no 'SSS' is found, server logic will not be called
    private String s_request_type = "";
    private String s_request_url = "";
    private String s_request_bounds = "";
    private String s_request_zoom_levels = "";
    private GeoPoint centerPoint = new GeoPoint(0, 0);

    private String tilePart = "";
    private boolean isFile = false;
    private boolean b_reset_metadata = false;
    private TILESCHEMA type = TILESCHEMA.google;
    private boolean isConnectedToInternet;

    @SuppressWarnings("nls")
    public CustomTileDownloader( File file_map, String parentPath ) {
        super();
        s_map_file = file_map.getAbsolutePath();
        s_name_file = file_map.getName();
        this.s_name = file_map.getName().substring(0, file_map.getName().lastIndexOf("."));
        List<String> fileLines = new ArrayList<String>();
        try {
            fileLines = FileUtilities.readfileToList(file_map);
        } catch (IOException e) {
            GPLog.androidLog(4, getClass().getSimpleName() + "[CustomTileDownloader.FileUtilities.readfileToList]", e);
        }
        // parentPath = '/mnt/sdcard/maps' : this will be appended to all pathis given in the
        // 'mapurl' file
        double[] bounds = {-180.0, -85.05113, 180, 85.05113};
        double[] center = {0.0, 0.0};
        double[] request_bounds = new double[]{0.0, 0.0, 0.0, 0.0};
        s_mbtiles_file = "";
        mapurl_metadata = new LinkedHashMap<String, String>();
        mbtiles_metadata = new LinkedHashMap<String, String>();
        mbtiles_request_url = new LinkedHashMap<String, String>();
        if (GPLog.LOG_HEAVY) {
            try {
                GPLog.addLogEntry("CustomTileDownloader called with:");
                GPLog.addLogEntry("parentPath: " + parentPath);
                for( String fileLine : fileLines ) {
                    GPLog.addLogEntry("-> " + fileLine);
                }
            } catch (IOException e1) {
                GPLog.androidLog(4, getClass().getSimpleName() + "[CustomTileDownloader.nCreation]", e1);
            }
        }

        for( String line : fileLines ) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }

            int split = line.indexOf('=');
            if (split < 0) { // some sort of comment, save back when editing mapurl file
                mapurl_metadata.put(line, "");
            }
            if (split != -1) {
                String parm = line.substring(0, split);
                String value = line.substring(split + 1).trim();
                // save value for future editing
                mapurl_metadata.put(parm, value); // parm without '='
                // GPLog.androidLog(-1,"CustomTileDownloader parm[" + parm+ "] value[" + value+
                // "]");
                if (line.startsWith("url")) {
                    s_request_url = value;
                    int indexOfS = value.indexOf("SSS");
                    if (indexOfS != -1) {
                        i_tile_server = 1; // Server logic will not be called [1,2]
                    }
                    int indexOfZ = value.indexOf("ZZZ");
                    if (indexOfZ != -1) {
                        // tile_servers and local files [order of ZZZ,XXX,YY is no longer inportant]
                        // url=http://mt1.google.com/vt/lyrs=s,h&x=XXX&y=YYY&z=ZZZ
                        // url=mytilesfolder/ZZZ/XXX/YYY.png
                        // url=http://tile.openstreetmap.org/ZZZ/XXX/YYY.png
                        String s_work = value;
                        if (value.startsWith("http")) { // tms_server
                            s_work = value.substring(7); // removed: 'http://'
                        }
                        int indexOfSeperator = s_work.indexOf("/");
                        // tms_servern and local files will always have a '/' in them
                        HOST_NAME = s_work.substring(0, indexOfSeperator);
                        tilePart = s_work.substring(indexOfSeperator);
                        if (!value.startsWith("http")) { // local files
                            PROTOCOL = "file";
                            HOST_NAME = parentPath + File.separator + HOST_NAME;
                            isFile = true;
                        }
                    } else {
                        // wms_server
                        // url=http://fbinter.stadt-berlin.de/fb/wms/senstadt/plz?LAYERS=0,1&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=visual&SRS=EPSG:4326&BBOX=XXX,YYY,XXX,YYY&WIDTH=256&HEIGHT=256
                        int indexOfParms = value.indexOf("?");
                        // wms server should always have a '?' in them
                        HOST_NAME = value.substring(7, indexOfParms); // removed: 'http://'
                        tilePart = value.substring(indexOfParms);
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
                        center[0]=x;
                        center[1]=y;
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
                    // GPLog.androidLog(-1,"CustomTileDownloader[mbtiles] s_mbtiles_file["+s_mbtiles_file+"]");
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
                if (line.startsWith("request_type")) {
                    if (!value.equals("off"))
                        s_request_type = value;
                }
                if (line.startsWith("request_bounds")) {
                    s_request_bounds = value;
                    try {
                        String[] coord = value.split("\\s+"); //$NON-NLS-1$
                        request_bounds[0] = Double.parseDouble(coord[0]);
                        request_bounds[1] = Double.parseDouble(coord[1]);
                        request_bounds[2] = Double.parseDouble(coord[2]);
                        request_bounds[3] = Double.parseDouble(coord[3]);
                    } catch (NumberFormatException e) {
                        s_request_bounds = "";
                    }
                }
                if (line.startsWith("request_zoom_levels")) {
                    s_request_zoom_levels = value;
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
        setDescription(this.s_description); // will set default values with bounds and center if it
                                            // is the same as 's_name' or empty
        if (s_mbtiles_file.length() > 0) {
            if (!s_request_type.equals("")) {
                int i_run = 0;
                int i_create = 0;
                int i_delete = 0;
                int indexOfS = s_request_type.indexOf(",");
                if (indexOfS != -1) {
                    String[] sa_string = s_request_type.split(",");
                    s_request_type = "";
                    String s_comma = "";
                    for( int i = 0; i < sa_string.length; i++ ) {
                        if (sa_string[i].equals("run")) { // no 'run', no fun [ignore all commands]
                            i_run++;
                        }
                        if (sa_string[i].equals("fill")) { // will request missing tiles only
                            if (!s_request_type.equals(""))
                                s_comma = ",";
                            s_request_type += s_comma + sa_string[i].trim();
                            if (s_request_zoom_levels.equals(""))
                            { // if not set, do all
                             s_request_zoom_levels = Integer.toString(this.minZoom) + "-" + Integer.toString(this.maxZoom);
                            }
                         i_create = 1;
                        }
                        if (sa_string[i].equals("replace")) { // will replace existing tiles
                            if (!s_request_type.equals(""))
                                s_comma = ",";
                            s_request_type += s_comma + sa_string[i].trim();
                         i_create = 1; // if bothe 'fill' and 'replace' are given: 'fill' willl be used
                        }
                        if (sa_string[i].equals("drop")) { // will delete the requested tiles,
                                                           // retaining the allready downloaded
                                                           // tiles
                            if (!s_request_type.equals(""))
                                s_comma = ",";
                            s_request_type += s_comma + sa_string[i].trim();
                        }
                        if (sa_string[i].equals("vacuum")) {
                            if (!s_request_type.equals(""))
                                s_comma = ",";
                            s_request_type += s_comma + sa_string[i].trim();
                        }
                        if (sa_string[i].equals("update_bounds")) {
                            if (!s_request_type.equals(""))
                                s_comma = ",";
                            s_request_type += s_comma + sa_string[i].trim();
                        }
                        if (sa_string[i].equals("reset_metadata")) {
                            b_reset_metadata=true;
                        }
                        if (sa_string[i].equals("delete")) { // planned for future
                            if (!s_request_type.equals(""))
                                s_comma = ",";
                            s_request_type += s_comma + sa_string[i].trim();
                        }
                        if (sa_string[i].equals("load")) {
                         if (!s_request_type.equals(""))
                                s_comma = ",";
                            s_request_type += s_comma + sa_string[i].trim();
                        }
                        // GPLog.androidLog(-1, "CustomTileDownloader sa_string[" + i + "].[" + sa_string[i] + "] ["+ s_request_type + "]");
                    }
                    if (i_create != 1) {
                        s_request_bounds = "";
                        s_request_zoom_levels = "";
                        s_request_url = "";
                    }
                }
                if (i_run > 0) {
                    mbtiles_request_url.put("request_type", s_request_type);
                }
                if ((!s_request_zoom_levels.equals("")) && (!s_request_url.equals(""))) {
                    String s_bbox = this.bounds_west + "," + this.bounds_south + "," + this.bounds_east + "," + this.bounds_north;
                    if (!s_request_bounds.equals("")) {
                        s_request_bounds = request_bounds[0] + "," + request_bounds[1] + "," + request_bounds[2] + ","
                                + request_bounds[3];
                    } else { // simplify filling of upper zoom-levels, fill supported area
                        s_request_bounds = s_bbox;
                        request_bounds = bounds;
                    }
                    if ((request_bounds[0] >= bounds[0]) && (request_bounds[2] <= bounds[2]) && (request_bounds[1] >= bounds[1])
                            && (request_bounds[3] <= bounds[3])) {
                        if (PROTOCOL.equals("file"))
                        { // this must be the absolute path ; 'file:' will be added later after checking if the file exists
                         s_request_url=parentPath + File.separator + s_request_url;
                        }
                        mbtiles_request_url.put("request_url", s_request_url);
                        mbtiles_request_url.put("request_bounds", s_request_bounds);
                        mbtiles_request_url.put("request_bounds_url", s_bbox);
                        s_bbox = Integer.toString(this.minZoom) + "-" + Integer.toString(this.maxZoom);
                        mbtiles_request_url.put("request_zoom_levels_url", s_bbox);
                        mbtiles_request_url.put("request_zoom_levels", s_request_zoom_levels);
                        String s_request_y_type = "wms"; // 0=osm ; 1=tms ; 2=wms
                        if (type == TILESCHEMA.google)
                            s_request_y_type = "osm";
                        if (type == TILESCHEMA.tms)
                            s_request_y_type = "tms";
                        mbtiles_request_url.put("request_y_type", s_request_y_type);
                        mbtiles_request_url.put("request_protocol", PROTOCOL);
                        // GPLog.androidLog(-1, "CustomTileDownloader [" + PROTOCOL + "] ["+ s_request_url + "]");
                    }
                }
            }
            if ((!file_mbtiles.exists()) || (b_reset_metadata))
            {
             mbtiles_metadata.put("name", this.s_name);
             mbtiles_metadata.put("description", this.s_description);
             if (!file_mbtiles.exists())
             {
              mbtiles_metadata.put("format", this.s_format);
              mbtiles_metadata.put("tile_row_type", this.s_tile_row_type);
             }
             // 'reset_metadata': will manually reset the mbtiles-metadata entries:
             // 'name','description','bounds','center','minzoom','maxzoom' ; NOT: 'format','tile_row_type'
             String s_bbox = this.bounds_west + "," + this.bounds_south + "," + this.bounds_east + "," + this.bounds_north;
             mbtiles_metadata.put("bounds", s_bbox);
             s_bbox = this.centerX + "," + this.centerY + "," + this.defaultZoom;
             mbtiles_metadata.put("center", s_bbox);
             mbtiles_metadata.put("minzoom", Integer.toString(this.minZoom));
             mbtiles_metadata.put("maxzoom", Integer.toString(this.maxZoom));
            }
            if (file_mbtiles.exists()) { // this will open an existing mbtiles_db
                mbtiles_db = new MbtilesDatabaseHandler(file_mbtiles.getAbsolutePath(), null);
            } else { // this will create the mbtiles_db and set default values
                mbtiles_db = new MbtilesDatabaseHandler(file_mbtiles.getAbsolutePath(), mbtiles_metadata);
            }
            if (mbtiles_request_url.size() > 0) {
                mbtiles_db.run_retrieve_url(mbtiles_request_url,mbtiles_metadata);
            }
        }
        // GPLog.androidLog(-1,"CustomTileDownloader parentPath[" + parentPath+ "]");
    }
    public String getHostName() {
        return HOST_NAME;
    }
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
    // -----------------------------------------------
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
    // -----------------------------------------------
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
    // -----------------------------------------------
    /**
      * Return West X Value [Longitude]
      *
      * <p>default :  -180.0 [if not otherwise set]
      * <p>mbtiles : taken from 1st value of metadata 'bounds'
      *
      * @return double of West X Value [Longitude]
      */
    public double getMinLongitude() {
        return bounds_west;
    }
    // -----------------------------------------------
    /**
      * Return South Y Value [Latitude]
      *
      * <p>default :  -85.05113 [if not otherwise set]
      * <p>mbtiles : taken from 2nd value of metadata 'bounds'
      *
      * @return double of South Y Value [Latitude]
      */
    public double getMinLatitude() {
        return bounds_south;
    }
    // -----------------------------------------------
    /**
      * Return East X Value [Longitude]
      *
      * <p>default :  180.0 [if not otherwise set]
      * <p>mbtiles : taken from 3th value of metadata 'bounds'
      *
      * @return double of East X Value [Longitude]
      */
    public double getMaxLongitude() {
        return bounds_east;
    }
    // -----------------------------------------------
    /**
      * Return North Y Value [Latitude]
      *
      * <p>default :  85.05113 [if not otherwise set]
      * <p>mbtiles : taken from 4th value of metadata 'bounds'
      *
      * @return double of North Y Value [Latitude]
      */
    public double getMaxLatitude() {
        return bounds_north;
    }
    // -----------------------------------------------
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
    // -----------------------------------------------
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
    // -----------------------------------------------
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
    // -----------------------------------------------
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
        return s_name; // comment or file-name without path and extention
    }
    // -----------------------------------------------
    /**
      * Return String of bounds [wms-format]
      *
      * <p>x_min,y_min,x_max,y_max
      *
      * @return bounds formatted using wms format
      */
    public String getBounds_toString() {
        return bounds_west + "," + bounds_south + "," + bounds_east + "," + bounds_north;
    }
    // -----------------------------------------------
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
    // -----------------------------------------------
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
        if ((this.s_description == null) || (this.s_description.length() == 0) || (this.s_description.equals(this.s_name)))
            setDescription(getName()); // will set default values with bounds and center if it is
                                       // the same as 's_name' or empty
        return this.s_description; // long comment
    }
    // -----------------------------------------------
    /**
      * Set long description of map/file
      *
      * <p>default: s_name with bounds and center
      * <p>mbtiles : metadata description'
      * <p>map : will be value of 'comment', if not null
      *
      * @return s_description long description of map/file
      */
    public void setDescription( String s_description ) {
        if ((s_description == null) || (s_description.length() == 0) || (s_description.equals(this.s_name))) {
            this.s_description = getName() + " bounds[" + getBounds_toString() + "] center[" + getCenter_toString() + "]";
        } else
            this.s_description = s_description;
    }
    public MbtilesDatabaseHandler getmbtiles() {
        return mbtiles_db;
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
                    // GPLog.androidLog(-1,"CustomTileDownloader.executeJob: name["+getName()
                    // +"] mbtiles_db["+mbtiles_db.getFileName()+"] tilePath["+i_zoom+"/"+i_tile_x+"/"+i_tile_y_osm+"] ");
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
            String s_host_name = HOST_NAME;
            if (i_tile_server > 0) { // ['http://otileSSS.mqcdn.com/'] replace
                                     // 'http://otile1.mqcdn.com/' with ''http://otile2.mqcdn.com/'
                s_host_name = s_host_name.replaceFirst("SSS", String.valueOf(i_tile_server++)); //$NON-NLS-1$
                if (i_tile_server > 2)
                    i_tile_server = 1;
            }
            sb.append(s_host_name);
            sb.append(tilePath);
            // GPLog.androidLog(-1,"CustomTileDownloader.executeJob: name["+getName()
            // +"] host_name["+s_host_name+"] tilePath["+tilePath+"] ");
            if (isFile)
             GPLog.androidLog(-1,"CustomTileDownloader.executeJob: request["+sb.toString()+"] ");
            Bitmap decodedBitmap = null;

            Context context = GeopaparazziLibraryContextHolder.INSTANCE.getContext();
            if (context != null) {
                isConnectedToInternet = NetworkUtilities.isNetworkAvailable(context);
            }
            if (isConnectedToInternet) {
                URL url = new URL(sb.toString());
                InputStream inputStream = url.openStream();
                try {
                    decodedBitmap = BitmapFactory.decodeStream(inputStream);
                } catch (Exception e) {
                    // ignore and set the image as empty
                    if (GPLog.LOG_HEAVY)
                        GPLog.addLogEntry(this, "Could not find image: " + sb.toString()); //$NON-NLS-1$
                } finally {
                    inputStream.close();
                }
            }
            // check if the input stream could be decoded into a bitmap
            if (decodedBitmap != null) {
                if (mbtiles_db != null) {
                    // we have a valid image, store this to the active mbtiles.db
                    // [this must be done before recycle() is called]
                    // decodedBitmap == ARGB_8888 ; bitmap == RGB_565
                    mbtiles_db.insertBitmapTile(i_tile_x, i_tile_y_osm, i_zoom, decodedBitmap, i_force_unique);
                }
                // copy all pixels from the decoded bitmap to the color array
                decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
                // GPLog.androidLog(-1,"CustomTileDownloader.executeJob: retrieved["+i_zoom+"/"+i_tile_x+"/"+i_tile_y_osm+"] ");
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
        return new CustomTileDownloader(file, parentPath);
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
