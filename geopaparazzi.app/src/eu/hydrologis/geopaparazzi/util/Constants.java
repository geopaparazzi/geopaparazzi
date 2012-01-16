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

import java.io.File;
import java.text.DecimalFormat;
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
    
    public final String TAKE_NOTE = "eu.hydrologis.geopaparazzi.camera.TAKE_NOTE";
    public final String TOGGLE_GPS = "eu.hydrologis.geopaparazzi.camera.TOGGLE_GPS";
    public final String MAP_VIEW = "eu.hydrologis.geopaparazzi.maps.MAP_VIEW";
    public final String VIEW_COMPASS = "eu.hydrologis.geopaparazzi.compass.VIEW_COMPASS";
    public final String INSERT_COORD = "eu.hydrologis.geopaparazzi.util.INSERT_COORD";
    public final String EXPORT_KML = "eu.hydrologis.geopaparazzi.kml.EXPORT_KML";
    public final String GPSLOG_DATALIST = "eu.hydrologis.geopaparazzi.maps.GPSLOG_DATALIST";
    public final String GPSLOG_PROPERTIES = "eu.hydrologis.geopaparazzi.maps.GPSLOG_PROPERTIES";
    public final String MAPSDATALIST = "eu.hydrologis.geopaparazzi.maps.MAPSDATALIST";
    public final String MAPDATAPROPERTIES = "eu.hydrologis.geopaparazzi.maps.MAPDATAPROPERTIES";
    public final String PREFERENCES = "eu.hydrologis.geopaparazzi.preferences.PREFERENCES";
    public final String VIEW_IN_CHART = "eu.hydrologis.geopaparazzi.chart.VIEW_IN_CHART";
    public final String MEASUREMENT_INFO = "eu.hydrologis.geopaparazzi.maps.MEASUREMENT_INFO";
    public final String ABOUT = "eu.hydrologis.geopaparazzi.util.ABOUT";
    public final String GPXIMPORT = "eu.hydrologis.geopaparazzi.gpx.GPXIMPORT";
    public final String TAGS = "eu.hydrologis.geopaparazzi.maps.TAGS";
    public final String FORM = "eu.hydrologis.geopaparazzi.maps.tags.FORM";
    public final String OSMCATEGORYACTIVITY = "eu.hydrologis.geopaparazzi.maps.OSMCATEGORYACTIVITY";
    public final String OSMFORMACTIVITY = "eu.hydrologis.geopaparazzi.osm.OSMFORMACTIVITY";

    public final String ID = "ID";
    public final String INTENT_ID = "INTENT_ID";
    public final String EXTENTION = "EXTENTION";
    public final String SHOWHIDDEN = "SHOWHIDDEN";
    public final String PATH = "PATH";
    public final String ISLINE = "ISLINE";
    public final String MEASURECOORDSX = "MEASURECOORDSX";
    public final String MEASURECOORDSY = "MEASURECOORDSY";
    public final String MEASUREDIST = "MEASUREDIST";
    public final String NSEW_COORDS = "NSEW_COORDS";

    public final float E6 = 1000000f;

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
     * The text color for the compass text.
     */
    public final int COMPASS_TEXT_COLOR = Color.BLACK;

    /**
     * The needle color.
     */
    public final int COMPASS_NEEDLE_COLOR = Color.RED;

    /**
     * The needle alpha.
     */
    public final int COMPASS_NEEDLE_ALPHA = 150;

    public static final String GEOPAPARAZZI = "geopaparazzi";
    public final String PATH_GEOPAPARAZZI = File.separator + GEOPAPARAZZI;
    public final String PATH_MEDIA = File.separator + "media";
    public final String PATH_NOTES = File.separator + "notes";
    public final String PATH_KMLEXPORT = File.separator + "export";

    public final String PATH_GEOPAPARAZZIDATA = File.separator + GEOPAPARAZZI;

    public final DecimalFormat DECIMAL_FORMATTER_2 = new DecimalFormat("0.00");
    public final SimpleDateFormat TIMESTAMPFORMATTER = new SimpleDateFormat("yyyyMMdd_HHmmss");
    public final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public final SimpleDateFormat TIME_FORMATTER_SQLITE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final int GPS_LOGGING_INTERVAL = 3;
    public static final float GPS_LOGGING_DISTANCE = 1f;

    public static final String PREFS_KEY_NOTES_COLOR = "PREFS_KEY_NOTES_COLOR";
    public static final String PREFS_KEY_NOTES_WIDTH = "PREFS_KEY_NOTES_WIDTH";
    public static final String PREFS_KEY_ZOOM = "PREFS_KEY_ZOOM";
    public static final String PREFS_KEY_GPSLOG4PROPERTIES = "PREFS_KEY_GPSLOG4PROPERTIES";
    public static final String PREFS_KEY_MAP4PROPERTIES = "PREFS_KEY_MAP4PROPERTIES";
    /**
     * Key used by the gps logger to store the lat in the prefs. 
     * 
     * <p>
     * The gps logger uses this key to regularly store the
     * gps data recorded, so this should not be used to store own
     * data, which would be overwritten. 
     * </p>
     */
    public static final String PREFS_KEY_LAT = "PREFS_KEY_LAT";
    /**
     * Key used by the gps logger to store the lon in the prefs. 
     * 
     * <p>
     * The gps logger uses this key to regularly store the
     * gps data recorded, so this should not be used to store own
     * data, which would be overwritten. 
     * </p>
     */
    public static final String PREFS_KEY_LON = "PREFS_KEY_LON";

    /**
     * Key used to store the mapview center latitude.
     * 
     * <p>
     * This is used every time the map center changes,
     * so this should not be used to store own
     * data, which would be overwritten. 
     * </p>
     */
    public static final String PREFS_KEY_MAPCENTER_LAT = "PREFS_KEY_MAPCENTER_LAT";

    /**
     * Key used to store the mapview center longitude.
     * 
     * <p>
     * This is used every time the map center changes,
     * so this should not be used to store own
     * data, which would be overwritten. 
     * </p>
     */
    public static final String PREFS_KEY_MAPCENTER_LON = "PREFS_KEY_MAPCENTER_LON";

    public static final String PREFS_KEY_AUTOMATIC_CENTER_GPS = "enable_automatic_center_on_gps";

    public static final String PREFS_KEY_ZOOM1 = "labels_zoom1";
    public static final String PREFS_KEY_ZOOM1_LABELLENGTH = "labels_length_zoom1";
    public static final String PREFS_KEY_ZOOM2 = "labels_zoom2";
    public static final String PREFS_KEY_ZOOM2_LABELLENGTH = "labels_length_zoom2";
    public static final String PREFS_KEY_MINIMAPON = "PREFS_KEY_MINIMAPON";
    public static final String PREFS_KEY_SCALEBARON = "PREFS_KEY_SCALEBARON";
    public static final String PREFS_KEY_COMPASSON = "PREFS_KEY_COMPASSON";

    public static final int MAP_TYPE_POINT = 0;
    public static final int MAP_TYPE_LINE = 1;
    public static final int MAP_TYPE_POLYGON = 2;

    public static final String BASEFOLDERKEY = "geopaparazzi_basefolder_key";
    public static final String DECIMATION_FACTOR = "decimation_list";
    public static final String GPSLOGGINGINTERVALKEY = "gps_logging_interval";
    public static final String GPSLOGGINGDISTANCEKEY = "gps_logging_distance";
    public static final String SMSCATCHERKEY = "sms_catcher";
    public static final String PANICKEY = "panic_number";

    public static final String FORMJSON_KEY = "formjson_key";
    public static final String FORMSHORTNAME_KEY = "formshortname_key";
    public static final String FORMLONGNAME_KEY = "formlongname_key";

    /*
     * OSM
     */
    public static final String OSM_CATEGORY_KEY = "OSM_CATEGORY_KEY";
    public static final String OSM_TAG_KEY = "OSM_TAG_KEY";
}
