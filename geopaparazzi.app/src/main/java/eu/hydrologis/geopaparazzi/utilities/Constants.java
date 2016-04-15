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

package eu.hydrologis.geopaparazzi.utilities;

/**
 * Various constants.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface Constants {
    String GEOPAPARAZZI = "geopaparazzi";

    String ID = "ID";

    String PANICKEY = "panic_number";

    String PREFS_KEY_SCREEN_ON = "PREFS_KEY_SCREEN_ON";

    String PREFS_KEY_IMPERIAL = "PREFS_KEY_IMPERIAL";

    String PREFS_KEY_MAPSVIEW_TEXTSIZE_FACTOR = "mapsview_textsize_factor";

    String PREFS_KEY_AUTOMATIC_CENTER_GPS = "enable_automatic_center_on_gps";

    String PREFS_KEY_RETINA = "PREFS_KEY_RETINA";

    String PREFS_KEY_IMAGES_VISIBLE = "PREFS_KEY_IMAGES_VISIBLE";

    String PREFS_KEY_NOTES_VISIBLE = "PREFS_KEY_NOTES_VISIBLE";

    String PREFS_KEY_GPSLOG4PROPERTIES = "PREFS_KEY_GPSLOG4PROPERTIES";

    /*
     * notes text properties
     */
    String PREFS_KEY_NOTES_TEXT_VISIBLE = "PREFS_KEY_NOTES_TEXT_VISIBLE";
    String PREFS_KEY_NOTES_TEXT_DOHALO = "PREFS_KEY_NOTES_TEXT_DOHALO";
    String PREFS_KEY_NOTES_TEXT_SIZE = "PREFS_KEY_NOTES_TEXT_SIZE";

    /*
     * CLOUD
     */
    String PREF_KEY_USER = "stage_user_key"; //$NON-NLS-1$
    String PREF_KEY_PWD = "stage_pwd_key"; //$NON-NLS-1$
    String PREF_KEY_SERVER = "stage_server_key";//$NON-NLS-1$


    /*
     * cross properties
     */
    String PREFS_KEY_CROSS_WIDTH = "PREFS_KEY_CROSS_WIDTH";
    String PREFS_KEY_CROSS_COLOR = "PREFS_KEY_CROSS_COLOR";
    String PREFS_KEY_CROSS_SIZE = "PREFS_KEY_CROSS_SIZE";

    /*
     * custom notes icon properties
     */
    String PREFS_KEY_NOTES_CHECK = "PREFS_KEY_NOTES_CHECK";
    String PREFS_KEY_NOTES_CUSTOMCOLOR = "PREFS_KEY_NOTES_CUSTOMCOLOR";
    String PREFS_KEY_NOTES_SIZE = "PREFS_KEY_NOTES_SIZE";
    String PREFS_KEY_NOTES_OPACITY = "PREFS_KEY_NOTES_OPACITY";
}
