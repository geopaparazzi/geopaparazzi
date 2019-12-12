/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.library.util;

import java.text.DecimalFormat;

/**
 * Some constants used in the lib.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("ALL")
public interface LibraryConstants {

    /**
     * The default extention for geopaparazzi databases.
     */
    String GEOPAPARAZZI_DB_EXTENSION = ".gpap";

    /**
     * The epsg for lat/long wgs84.
     */
    int SRID_WGS84_4326 = 4326;
    /**
     * The epsg Sperical Mercator used by OSM.
     */
    int SRID_MERCATOR_3857 = 3857;
    /**
     *
     */
    float E6 = 1000000f;

    float PICKRADIUS = 0.00001f;

    /**
     * Default width of new logs.
     */
    float DEFAULT_LOG_WIDTH = 4f;

    int DEFAULT_NOTES_SIZE = 50;

    /**
     * Formatter for lat/long coordinates. 6 digits will suffice.
     */
    DecimalFormat COORDINATE_FORMATTER = new DecimalFormat("#.000000"); //$NON-NLS-1$

    /**
     * Decimal formatter with 2 digits.
     */
    DecimalFormat DECIMAL_FORMATTER_2 = new DecimalFormat("0.00"); //$NON-NLS-1$
    /**
     * Decimal formatter with 1 digits.
     */
    DecimalFormat DECIMAL_FORMATTER_1 = new DecimalFormat("0.0"); //$NON-NLS-1$

    /**
     * Key used by the gps logger to store the lat in the prefs.
     *
     * <p><b>Note that this will always be *E6 and of type float, so to
     * get the actual value you will have to divide by E6.</b>
     *
     * <p>
     * The gps logger uses this key to regularly store the
     * gps data recorded, so this should not be used to store own
     * data, which would be overwritten.
     * </p>
     */
    String PREFS_KEY_LAT = "PREFS_KEY_LAT"; //$NON-NLS-1$

    /**
     * Key used by the gps logger to store the lon in the prefs.
     *
     * <p><b>Note that this will always be *E6 and of type float, so to
     * get the actual value you will have to divide by E6.</b>
     *
     * <p>
     * The gps logger uses this key to regularly store the
     * gps data recorded, so this should not be used to store own
     * data, which would be overwritten.
     * </p>
     */
    String PREFS_KEY_LON = "PREFS_KEY_LON"; //$NON-NLS-1$

    /**
     * Key used by the gps logger to store the elev in the prefs.
     *
     * <p>
     * The gps logger uses this key to regularly store the
     * gps data recorded, so this should not be used to store own
     * data, which would be overwritten.
     * </p>
     */
    String PREFS_KEY_ELEV = "PREFS_KEY_ELEV"; //$NON-NLS-1$

    /**
     * Key used to store the mapview center latitude.
     *
     * <p><b>Note that this will always be *E6 and of type float, so to
     * get the actual value you will have to divide by E6.</b>
     *
     * <p>
     * This is used every time the map center changes,
     * so this should not be used to store own
     * data, which would be overwritten.
     * </p>
     */
    String PREFS_KEY_MAPCENTER_LAT = "PREFS_KEY_MAPCENTER_LAT"; //$NON-NLS-1$

    /**
     * Key used to store the mapview center longitude.
     *
     * <p><b>Note that this will always be *E6 and of type float, so to
     * get the actual value you will have to divide by E6.</b>
     *
     * <p>
     * This is used every time the map center changes,
     * so this should not be used to store own
     * data, which would be overwritten.
     * </p>
     */
    String PREFS_KEY_MAPCENTER_LON = "PREFS_KEY_MAPCENTER_LON"; //$NON-NLS-1$

    /**
     * Key used to store the mapview zoom level.
     */
    String PREFS_KEY_MAP_ZOOM = "PREFS_KEY_MAP_ZOOM"; //$NON-NLS-1$

    /**
     * Key used to pass a name or description temporarily through bundles.
     */
    String NAME = "NAME"; //$NON-NLS-1$

    /**
     * Key used to pass a lat temporarily through bundles.
     */
    String LATITUDE = "LATITUDE"; //$NON-NLS-1$

    /**
     * Key used to pass a lon temporarily through bundles.
     */
    String LONGITUDE = "LONGITUDE"; //$NON-NLS-1$

    /**
     * Key used to pass an array containing [n,s,w,e] temporarily through bundles.
     */
    String NSWE = "NSWE"; //$NON-NLS-1$

    /**
     * Key used to pass a zoom level temporarily through bundles.
     */
    String ZOOMLEVEL = "ZOOMLEVEL"; //$NON-NLS-1$

    /**
     * Key used to pass an elevation temporarily through bundles.
     */
    String ELEVATION = "ELEVATION"; //$NON-NLS-1$

    /**
     * Key used to pass an azimuth value temporarily through bundles.
     */
    String AZIMUTH = "AZIMUTH"; //$NON-NLS-1$

    /**
     * Key used to pass an the existence of an object through the bundles.
     */
    String OBJECT_EXISTS = "OBJECT_EXISTS"; //$NON-NLS-1$

    /**
     * Key used to pass a route string temporarily through bundles.
     */
    String ROUTE = "ROUTE"; //$NON-NLS-1$

    /**
     * Name for a general temporary image.
     */
    String TMPPNGIMAGENAME = "tmp.png"; //$NON-NLS-1$

    String OSM = "OSM"; //$NON-NLS-1$

    /**
     * Key used to store and retrieve a custom path to the external storage.
     */
    String PREFS_KEY_CUSTOM_EXTERNALSTORAGE = "PREFS_KEY_CUSTOM_EXTERNALSTORAGE"; //$NON-NLS-1$

    /**
     * Key used to store and retrieve the database of the application.
     */
    String PREFS_KEY_DATABASE_TO_LOAD = "PREFS_KEY_DATABASE_TO_LOAD"; //$NON-NLS-1$

    /**
     * Key used to store and retrieve the gps logging interval to use.
     */
    String PREFS_KEY_GPSLOGGINGINTERVAL = "PREFS_KEY_GPS_LOGGING_INTERVAL"; //$NON-NLS-1$

    /**
     * Key used to store and retrieve the gps logging distance to use.
     */
    String PREFS_KEY_GPSLOGGINGDISTANCE = "PREFS_KEY_GPS_LOGGING_DISTANCE"; //$NON-NLS-1$

    /**
     * Key used to store and retrieve the gps mode to use (apply on android listener or just on application base).
     */
    String PREFS_KEY_GPSDOATANDROIDLEVEL = "PREFS_KEY_GPSDOATANDROIDLEVEL"; //$NON-NLS-1$

    /**
     * Key used to store for sms catching.
     */
    String PREFS_KEY_SMSCATCHER = "PREFS_KEY_SMSCATCHER"; //$NON-NLS-1$

    /**
     * Key used to define a path that is passed through any workflow.
     */
    String PREFS_KEY_PATH = "PREFS_KEY_PATH"; //$NON-NLS-1$

    /**
     * Key used to define a note that is passed through any workflow.
     */
    String PREFS_KEY_NOTE = "PREFS_KEY_NOTE"; //$NON-NLS-1$

    /**
     * Key used to define a path into which to save a new camera generated image.
     */
    String PREFS_KEY_CAMERA_IMAGESAVEFOLDER = "PREFS_KEY_CAMERA_IMAGESAVEFOLDER"; //$NON-NLS-1$

    /**
     * Key used to define a name for new camera generated image.
     */
    String PREFS_KEY_CAMERA_IMAGENAME = "PREFS_KEY_CAMERA_IMAGENAME"; //$NON-NLS-1$

    /**
     * Key used to pass image data bytes.
     */
    String PREFS_KEY_IMAGEDATA = "PREFS_KEY_IMAGEDATA"; //$NON-NLS-1$

    /**
     * Key used to define if the network should be used instead of the GPS.
     */
    String PREFS_KEY_GPS_USE_NETWORK_POSITION = "PREFS_KEY_GPS_USE_NETWORK_POSITION"; //$NON-NLS-1$

    /**
     * Key used to define form data that are passed through any workflow.
     */
    String PREFS_KEY_FORM = "PREFS_KEY_FORM"; //$NON-NLS-1$

    /**
     * Key used to define a json form that is passed through any workflow.
     */
    String PREFS_KEY_FORM_JSON = "PREFS_KEY_FORM_JSON"; //$NON-NLS-1$

    /**
     * Key used to define a form name that is passed through any workflow.
     */
    String PREFS_KEY_FORM_NAME = "PREFS_KEY_FORM_NAME"; //$NON-NLS-1$

    /**
     * Key used to define a form category that is passed through any workflow.
     */
    String PREFS_KEY_FORM_CAT = "PREFS_KEY_FORM_CAT"; //$NON-NLS-1$

    /**
     * Key used to define a user name that is passed through any workflow.
     */
    String PREFS_KEY_USER = "PREFS_KEY_USER"; //$NON-NLS-1$

    /**
     * Key used to define a password that is passed through any workflow.
     */
    String PREFS_KEY_PWD = "PREFS_KEY_PWD"; //$NON-NLS-1$

    /**
     * Key used to define a url that is passed through any workflow.
     */
    String PREFS_KEY_URL = "PREFS_KEY_URL"; //$NON-NLS-1$

    /**
     * Key used to define a profile url that is passed through any workflow.
     */
    String PREFS_KEY_PROFILE_URL = "PREFS_KEY_PROFILE_URL"; //$NON-NLS-1$

    /**
     * Key used to define a text that is passed through the workflow. Generic.
     */
    String PREFS_KEY_TEXT = "PREFS_KEY_TEXT"; //$NON-NLS-1$
    /**
     * Key used to define a query that is passed through the workflow.
     */
    String PREFS_KEY_QUERY = "PREFS_KEY_QUERY"; //$NON-NLS-1$

    /**
     * Key used to define the mock mode in the prefs.
     */
    String PREFS_KEY_MOCKMODE = "PREFS_KEY_MOCKMODE";

    /**
     * Key used to define the mock class to use in the prefs.
     */
    String PREFS_KEY_MOCKCLASS = "PREFS_KEY_MOCKCLASS";

    /**
     * Default gps logging interval.
     */
    int GPS_LOGGING_INTERVAL = 3;

    /**
     * Default gps logging distance.
     */
    float GPS_LOGGING_DISTANCE = 1f;
    /**
     * Key for tilesource in preferences.
     */
    String PREFS_KEY_TILESOURCE = "PREFS_KEY_TILESOURCE";
    /**
     * Key for tilesource file in preferences.
     */
    String PREFS_KEY_TILESOURCE_FILE = "PREFS_KEY_TILESOURCE_FILE";
    /**
     * Key for tilesource title in preferences.
     */
    String PREFS_KEY_TILESOURCE_TITLE = "PREFS_KEY_TILESOURCE_TITLE";

    /**
     * Key to passdatabase ids of objects through intents.
     */
    String DATABASE_ID = "DATABASE_ID";

    /**
     * The name of the db to put mapsforge extracted data in
     */
    String MAPSFORGE_EXTRACTED_DB_NAME = "mapsforge_extracted.sqlite";

    /**
     * The name of the templade db of geopap.
     */
    String GEOPAPARAZZI_TEMPLATE_DB_NAME = "geopaparazzi_template.sqlite";

    /**
     * Key used to define the last picked path.
     */
    String PREFS_KEY_LASTPATH = "PREFS_KEY_LASTPATH";

    String PREFS_KEY_CAMERA_WARNING_SHOWN = "PREFS_KEY_CAMERA_WARNING_SHOWN";

    String PREFS_KEY_RESTART_APPLICATION = "PREFS_KEY_RESTART_APPLICATION";


    String PREFS_KEY_IMAGES_VISIBLE = "PREFS_KEY_IMAGES_VISIBLE";
    String PREFS_KEY_NOTES_VISIBLE = "PREFS_KEY_NOTES_VISIBLE";
    /*
     * notes text properties
     */
    String PREFS_KEY_NOTES_TEXT_VISIBLE = "PREFS_KEY_NOTES_TEXT_VISIBLE";
    String PREFS_KEY_IMAGES_TEXT_VISIBLE = "PREFS_KEY_IMAGES_TEXT_VISIBLE";
    String PREFS_KEY_NOTES_TEXT_DOHALO = "PREFS_KEY_NOTES_TEXT_DOHALO";
    String PREFS_KEY_NOTES_TEXT_SIZE = "PREFS_KEY_NOTES_TEXT_SIZE";
    /*
     * custom notes icon properties
     */
    String PREFS_KEY_NOTES_CHECK = "PREFS_KEY_NOTES_CHECK";
    String PREFS_KEY_NOTES_CUSTOMCOLOR = "PREFS_KEY_NOTES_CUSTOMCOLOR";
    String PREFS_KEY_NOTES_SIZE = "PREFS_KEY_NOTES_SIZE";
    String PREFS_KEY_NOTES_OPACITY = "PREFS_KEY_NOTES_OPACITY";


    String PREFS_KEY_AUTOMATIC_CENTER_GPS = "enable_automatic_center_on_gps";
    String PREFS_KEY_ROTATE_MAP_WITH_GPS = "rotate_map_with_gps";
    String PREFS_KEY_SHOW_GPS_INFO = "show_gps_info";
    String PREFS_KEY_IGNORE_GPS_ACCURACY = "ignore_gps_accuracy";

}
