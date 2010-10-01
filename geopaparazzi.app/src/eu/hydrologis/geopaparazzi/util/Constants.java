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
package eu.hydrologis.geopaparazzi.util;

import java.text.SimpleDateFormat;

import android.graphics.Color;

/**
 * Various constants.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public interface Constants {
    /*
     * intent names
     */
    public final String MAIN = "android.intent.action.MAIN";
    public final String TAKE_PICTURE = "eu.hydrologis.geopaparazzi.camera.TAKE_PICTURE";
    public final String TAKE_NOTE = "eu.hydrologis.geopaparazzi.camera.TAKE_NOTE";
    public final String TOGGLE_GPS = "eu.hydrologis.geopaparazzi.camera.TOGGLE_GPS";
    public final String VIEW_IN_OSM = "eu.hydrologis.geopaparazzi.osm.VIEW_IN_OSM";
    public final String INSERT_COORD = "eu.hydrologis.geopaparazzi.util.INSERT_COORD";
    public final String EXPORT_KML = "eu.hydrologis.geopaparazzi.kml.EXPORT_KML";
    public final String GPSLOG_DATALIST = "eu.hydrologis.geopaparazzi.osm.GPSLOG_DATALIST";
    public final String GPSLOG_PROPERTIES = "eu.hydrologis.geopaparazzi.osm.GPSLOG_PROPERTIES";
    public final String MAPSDATALIST = "eu.hydrologis.geopaparazzi.osm.MAPSDATALIST";
    public final String MAPDATAPROPERTIES = "eu.hydrologis.geopaparazzi.osm.MAPDATAPROPERTIES";
    public final String PREFERENCES = "eu.hydrologis.geopaparazzi.preferences.PREFERENCES";
    public final String VIEW_IN_CHART = "eu.hydrologis.geopaparazzi.chart.VIEW_IN_CHART";
    public final String MEASUREMENT_INFO = "eu.hydrologis.geopaparazzi.osm.MEASUREMENT_INFO";
    public final String ABOUT = "eu.hydrologis.geopaparazzi.util.ABOUT";
    public final String DIRECTORYBROWSER = "eu.hydrologis.geopaparazzi.util.DIRECTORYBROWSER";
    public final String GPXIMPORT = "eu.hydrologis.geopaparazzi.gpx.GPXIMPORT";
    public final String OSMTAGS = "eu.hydrologis.geopaparazzi.osm.OSMTAGS";

    public final String ID = "ID";
    public final String INTENT_ID = "INTENT_ID";
    public final String EXTENTION = "EXTENTION";
    public final String PATH = "PATH";
    public final String ISLINE = "ISLINE";
    public final String MEASURECOORDSX = "MEASURECOORDSX";
    public final String MEASURECOORDSY = "MEASURECOORDSY";
    public final String MEASUREDIST = "MEASUREDIST";

    /**
     * Threshold for the sensor values in degrees (azimuth, pitch, roll).
     */
    public final int SENSORTHRESHOLD = 1;

    /**
     * Time threshold for gps position in milliseconds.
     */
    public final int TIMETHRESHOLD = 3000;

    /**
     * Position threshold for gps position in meters.
     */
    public final int POSITIONTHRESHOLD = 3;

    /**
     * Default latitude if no value is available.
     */
    public final double DEFAULT_LAT = Double.POSITIVE_INFINITY;

    /**
     * Default longitude if no value is available.
     */
    public final double DEFAULT_LON = Double.POSITIVE_INFINITY;

    /**
     * The width of the compass canvas.
     */
    public final int SCREEN_WIDTH = 320;

    /**
     * The height of the compass canvas. 
     */
    public final int SCREEN_HEIGHT = 480;

    /*
    * The width of the compass canvas.
     */
    public final int COMPASS_CANVAS_WIDTH = 320;

    /**
     * The height of the compass canvas. 
     */
    public final int COMPASS_CANVAS_HEIGHT = 240;

    /**
     * The X position of the compass in the canvas.
     */
    public final int COMPASS_X_POSITION = 100;

    /**
     * The Y position of the compass in the canvas.
     */
    public final int COMPASS_Y_POSITION = 10;

    /**
     * The dimension of the compass image. 
     * 
     * <p>Needs to be changed if the image is changed.
     * It is assumed to be square./p> 
     */
    public final int COMPASS_DIM_COMPASS = 220;

    /**
     * The X position of the center of the compass (needle in that point doesn't rotate).
     */
    public final int COMPASS_X_POSITION_CENTER = COMPASS_X_POSITION + COMPASS_DIM_COMPASS / 2;

    /**
     * The Y position of the center of the compass (needle in that point doesn't rotate).
     */
    public final int COMPASS_Y_POSITION_CENTER = COMPASS_Y_POSITION + 123;

    /**
     * The text size for the compass text.
     */
    public final float COMPASS_TEXT_SIZE = 16f;

    /**
     * The text color for the compass text.
     */
    public final int COMPASS_TEXT_COLOR = Color.BLACK;

    /**
     * The needle width in its widest point.
     */
    public final float COMPASS_NEEDLE_WIDTH = 14;

    /**
     * The needle color.
     */
    public final int COMPASS_NEEDLE_COLOR = Color.RED;

    /**
     * The needle alpha.
     */
    public final int COMPASS_NEEDLE_ALPHA = 150;

    /**
     * The length of the longer part of the needle.
     */
    public final float COMPASS_NEEDLE_HEIGHT_LONG = 80;

    /**
     * The length of the shorter part of the needle.
     */
    public final float COMPASS_NEEDLE_HEIGHT_SHORT = 10;

    /**
     * The notes title text length threshold.
     */
    public final int NOTES_LENGTH_LIMIT = 7;

    /**
     * The coordinates of the needle, zero centered.
     */
    public final float[] COMPASS_NEEDLE_COORDINATES = new float[]{0f, -COMPASS_NEEDLE_HEIGHT_LONG, -COMPASS_NEEDLE_WIDTH / 2f,
            0f, 0f, COMPASS_NEEDLE_HEIGHT_SHORT, COMPASS_NEEDLE_WIDTH / 2f, 0f};

    public final String PATH_GEOPAPARAZZI = "/geopaparazzi";
    public final String PATH_PICTURES = "/pictures";
    public final String PATH_NOTES = "/notes";
    public final String PATH_GPSLOGS = "/gpslogs";
    public final String PATH_KMLEXPORT = "/export";

    public final String PATH_GEOPAPARAZZIDATA = "/geopaparazzi";
    public final String PATH_OSMCACHE = "/osmcache";
    public final String PATH_GPXLINES = "/gpxlines";
    public final String PATH_GPXPOINTS = "/gpxpoints";

    public final SimpleDateFormat TIMESTAMPFORMATTER = new SimpleDateFormat("yyyyMMdd_HHmmss");
    public final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public final SimpleDateFormat TIME_FORMATTER_SQLITE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final String TEXT_SHOW_POSITION_ON_MAP = "show position on map";
    public static final String TEXT_START_GPS_LOGGING = "start gps logging";
    public static final String TEXT_STOP_GPS_LOGGING = "stop gps logging";
    public static final String TEXT_TAKE_A_GPS_NOTE = "take a gps note";
    public static final String TEXT_TAKE_PICTURE = "take picture";

    public static final int GPS_LOGGING_INTERVAL = 10;
    public static final float GPS_LOGGING_DISTANCE = 5f;

    public static final double EARTH_RADIUS_KM = 6378.137;
    public static final double WGS84FLATTENING = 1 / 298.257223563;

    public static final String PREFS_KEY_NOTES_COLOR = "PREFS_KEY_NOTES_COLOR";
    public static final String PREFS_KEY_NOTES_WIDTH = "PREFS_KEY_NOTES_WIDTH";
    public static final String PREFS_KEY_ZOOM = "PREFS_KEY_ZOOM";
    public static final String PREFS_KEY_GPSLOG4PROPERTIES = "PREFS_KEY_GPSLOG4PROPERTIES";
    public static final String PREFS_KEY_MAP4PROPERTIES = "PREFS_KEY_MAP4PROPERTIES";
    public static final String PREFS_KEY_LAT = "PREFS_KEY_LAT";
    public static final String PREFS_KEY_LON = "PREFS_KEY_LON";
    public static final String OSMVIEW_CENTER_LAT = "OSMVIEW_CENTER_LAT";
    public static final String OSMVIEW_CENTER_LON = "OSMVIEW_CENTER_LON";

    public static final int MAP_TYPE_POINT = 0;
    public static final int MAP_TYPE_LINE = 1;
    public static final int MAP_TYPE_POLYGON = 2;

    public static final String GPSLOGGINGINTERVALKEY = "gps_logging_interval";
    public static final String GPSLOGGINGDISTANCEKEY = "gps_logging_distance";
    public static final String OSMFOLDERKEY = "osm_folder";
    public static final String SMSCATCHERKEY = "sms_catcher";
    public static final String GPSLAST_LONGITUDE = "gpslast_longitude";
    public static final String GPSLAST_LATITUDE = "gpslast_latitude";

}
