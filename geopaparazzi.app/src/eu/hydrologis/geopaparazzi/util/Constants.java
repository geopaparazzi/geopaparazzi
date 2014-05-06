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

import eu.hydrologis.geopaparazzi.gpx.GpxImportActivity;

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
    // public final String MAIN = "android.intent.action.MAIN";
    // public final String TOGGLE_GPS = "eu.hydrologis.geopaparazzi.camera.TOGGLE_GPS";
    // public final String MAP_VIEW = "eu.hydrologis.geopaparazzi.maps.MAP_VIEW";
    // public final String VIEW_COMPASS = "eu.hydrologis.geopaparazzi.compass.VIEW_COMPASS";
    // public final String INSERT_COORD = "eu.hydrologis.geopaparazzi.util.INSERT_COORD";
    // public final String EXPORT_KML = "eu.hydrologis.geopaparazzi.kml.EXPORT_KML";
    // public final String GPSLOG_DATALIST = "eu.hydrologis.geopaparazzi.maps.GPSLOG_DATALIST";
    // public final String GPSLOG_PROPERTIES = "eu.hydrologis.geopaparazzi.maps.GPSLOG_PROPERTIES";
    // public final String MAPSDATALIST = "eu.hydrologis.geopaparazzi.maps.MAPSDATALIST";
    // public final String MAPDATAPROPERTIES = "eu.hydrologis.geopaparazzi.maps.MAPDATAPROPERTIES";
    // public final String PREFERENCES = "eu.hydrologis.geopaparazzi.preferences.PREFERENCES";
    // public final String VIEW_IN_CHART = "eu.hydrologis.geopaparazzi.chart.VIEW_IN_CHART";
    // public final String MEASUREMENT_INFO = "eu.hydrologis.geopaparazzi.maps.MEASUREMENT_INFO";
    // public final String ABOUT = "eu.hydrologis.geopaparazzi.util.ABOUT";
    /**
     * Intent id to run the {@link GpxImportActivity}.
     */
    public final String GPXIMPORT = "eu.hydrologis.geopaparazzi.gpx.GPXIMPORT";
    // public final String TAGS = "eu.hydrologis.geopaparazzi.maps.TAGS";
    // public final String FORM = "eu.hydrologis.geopaparazzi.maps.tags.FORM";
    // public final String OSMCATEGORYACTIVITY =
    // "eu.hydrologis.geopaparazzi.maps.OSMCATEGORYACTIVITY";
    // public final String OSMFORMACTIVITY = "eu.hydrologis.geopaparazzi.osm.OSMFORMACTIVITY";

    /**
     * 
     */
    public final String ID = "ID";
    /**
     * 
     */
    public final String ISLINE = "ISLINE";
    /**
     * 
     */
    public final String MEASURECOORDSX = "MEASURECOORDSX";
    /**
     * 
     */
    public final String MEASURECOORDSY = "MEASURECOORDSY";
    /**
     * 
     */
    public final String MEASUREDIST = "MEASUREDIST";
    /**
     * 
     */
    public final String NSEW_COORDS = "NSEW_COORDS";

    /**
     * 
     */
    public static final String GEOPAPARAZZI = "geopaparazzi";
    /**
     * 
     */
    public final String PATH_MEDIA = File.separator + "media";
    /**
     * 
     */
    public final String PATH_KMLEXPORT = File.separator + "export";

    /**
     * 
     */
    public static final String PREFS_KEY_SCREEN_ON = "PREFS_KEY_SCREEN_ON";
    /**
     * 
     */
    public static final String PREFS_KEY_IMPERIAL = "PREFS_KEY_IMPERIAL";
    /**
     * 
     */
    public static final String PREFS_KEY_RETINA = "PREFS_KEY_RETINA";

    /**
     * 
     */
    public static final String PREFS_KEY_NOTES_COLOR = "PREFS_KEY_NOTES_COLOR";
    /**
     * 
     */
    public static final String PREFS_KEY_NOTES_WIDTH = "PREFS_KEY_NOTES_WIDTH";
    /**
     * 
     */
    public static final String PREFS_KEY_GPSLOG4PROPERTIES = "PREFS_KEY_GPSLOG4PROPERTIES";
    /**
     * 
     */
    public static final String PREFS_KEY_MAP4PROPERTIES = "PREFS_KEY_MAP4PROPERTIES";

    /**
     * 
     */
    public static final String PREFS_KEY_AUTOMATIC_CENTER_GPS = "enable_automatic_center_on_gps";

    /**
     * 
     */
    public static final String PREFS_KEY_ZOOM1 = "labels_zoom1";
    /**
     * 
     */
    public static final String PREFS_KEY_ZOOM1_LABELLENGTH = "labels_length_zoom1";
    /**
     * 
     */
    public static final String PREFS_KEY_ZOOM2 = "labels_zoom2";
    /**
     * 
     */
    public static final String PREFS_KEY_ZOOM2_LABELLENGTH = "labels_length_zoom2";
    /**
     * 
     */
    public static final String PREFS_KEY_MINIMAPON = "PREFS_KEY_MINIMAPON";
    /**
     * 
     */
    public static final String PREFS_KEY_SCALEBARON = "PREFS_KEY_SCALEBARON";
    /**
     * 
     */
    public static final String PREFS_KEY_COMPASSON = "PREFS_KEY_COMPASSON";

    /**
     * 
     */
    public static final int MAP_TYPE_POINT = 0;
    /**
     * 
     */
    public static final int MAP_TYPE_LINE = 1;
    /**
     * 
     */
    public static final int MAP_TYPE_POLYGON = 2;

    // public static final String BASEFOLDERKEY = "geopaparazzi_basefolder_key";
    /**
     * 
     */
    public static final String DECIMATION_FACTOR = "decimation_list";
    // public static final String GPSLOGGINGINTERVALKEY = "gps_logging_interval";
    // public static final String GPSLOGGINGDISTANCEKEY = "gps_logging_distance";
    /**
     * 
     */
    public static final String SMSCATCHERKEY = "sms_catcher";
    /**
     * 
     */
    public static final String PANICKEY = "panic_number";

    /*
     * OSM
     */
    /**
     * 
     */
    public static final String OSM_CATEGORY_KEY = "OSM_CATEGORY_KEY";
    /**
     * 
     */
    public static final String OSM_TAG_KEY = "OSM_TAG_KEY";
    /**
     * 
     */
    public static final String PREFS_KEY_DOOSM = "PREFS_KEY_DOOSM";
    /**
     * 
     */
    public static final String PREFS_KEY_OSMTAGSVERSION = "PREFS_KEY_OSMTAGSVERSION";

    /*
     * CLOUD
     */
    /**
     * 
     */
    public static final String PREF_KEY_USER = "geopapcloud_user_key"; //$NON-NLS-1$
    /**
     * 
     */
    public static final String PREF_KEY_PWD = "geopapcloud_pwd_key"; //$NON-NLS-1$
    /**
     * 
     */
    public static final String PREF_KEY_SERVER = "geopapcloud_server_key";//$NON-NLS-1$

    /*
     * mapsforge
     * for i_version=1 [MapDirManager] : PREFS_KEY_TILESOURCE,PREFS_KEY_TILESOURCE_FILE moved to LibraryConstants
     */
    // public static final String PREFS_KEY_TILESOURCE = "PREFS_KEY_TILESOURCE";
    // public static final String PREFS_KEY_TILESOURCE_FILE = "PREFS_KEY_TILESOURCE_FILE";
    /**
     * 
     */
    public static final String PREFS_KEY_MAPSVIEW_TEXTSIZE_FACTOR = "mapsview_textsize_factor";
    /*
     * cross properties
     */
    /**
     * 
     */
    public static final String PREFS_KEY_CROSS_WIDTH = "PREFS_KEY_CROSS_WIDTH";
    /**
     * 
     */
    public static final String PREFS_KEY_CROSS_COLOR = "PREFS_KEY_CROSS_COLOR";
    /**
     * 
     */
    public static final String PREFS_KEY_CROSS_SIZE = "PREFS_KEY_CROSS_SIZE";
    /*
     * custom notes icon properties
     */
    /**
     * 
     */
    public static final String PREFS_KEY_NOTES_CHECK = "PREFS_KEY_NOTES_CHECK";
    /**
     * 
     */
    public static final String PREFS_KEY_NOTES_CUSTOMCOLOR = "PREFS_KEY_NOTES_CUSTOMCOLOR";
    /**
     * 
     */
    public static final String PREFS_KEY_NOTES_SIZE = "PREFS_KEY_NOTES_SIZE";
    /**
     * 
     */
    public static final String PREFS_KEY_NOTES_OPACITY = "PREFS_KEY_NOTES_OPACITY";

    /*
     * notes text properties
     */
    /**
     * 
     */
    public static final String PREFS_KEY_NOTES_TEXT_VISIBLE = "PREFS_KEY_NOTES_TEXT_VISIBLE";
    /**
     * 
     */
    public static final String PREFS_KEY_NOTES_TEXT_DOHALO = "PREFS_KEY_NOTES_TEXT_DOHALO";
    /**
     * 
     */
    public static final String PREFS_KEY_NOTES_TEXT_SIZE = "PREFS_KEY_NOTES_TEXT_SIZE";
}
