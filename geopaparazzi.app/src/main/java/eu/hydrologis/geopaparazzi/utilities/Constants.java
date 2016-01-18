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
    public static final String GEOPAPARAZZI = "geopaparazzi";

    public static final String PANICKEY = "panic_number";

    public static final String PREFS_KEY_SCREEN_ON = "PREFS_KEY_SCREEN_ON";

    /*
     * CLOUD
     */
    public static final String PREF_KEY_USER = "stage_user_key"; //$NON-NLS-1$
    public static final String PREF_KEY_PWD = "stage_pwd_key"; //$NON-NLS-1$
    public static final String PREF_KEY_SERVER = "stage_server_key";//$NON-NLS-1$


    /*
     * Intents
     */

    public final String GPXIMPORT_INTENT = "eu.hydrologis.geopaparazzi.gpx.GPXIMPORT";

}
