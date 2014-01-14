/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2013  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.library.database;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Check and set logging levels.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GPLogPreferencesHandler {

    /**
     * Preferences key for log.
     */
    public static String PREFS_KEY_LOG = "PREFS_KEY_LOG";

    /**
     * Preferences key for log heavy.
     */
    public static String PREFS_KEY_LOG_HEAVY = "PREFS_KEY_LOG_HEAVY";

    /**
     * Preferences key for all log.
     */
    public static String PREFS_KEY_LOG_ABSURD = "PREFS_KEY_LOG_ABSURD";

    /**
     * @param doLog log flag.
     * @param preferences the preferences.
     */
    public static void setLog( boolean doLog, SharedPreferences preferences ) {
        GPLog.LOG = doLog;
        Editor edit = preferences.edit();
        edit.putBoolean(PREFS_KEY_LOG, doLog);
        edit.commit();
    }

    /**
     * @param doLog log flag.
     * @param preferences the preferences.
     */
    public static void setLogHeavy( boolean doLog, SharedPreferences preferences ) {
        GPLog.LOG_HEAVY = doLog;
        Editor edit = preferences.edit();
        edit.putBoolean(PREFS_KEY_LOG_HEAVY, doLog);
        edit.commit();
    }

    /**
     * @param doLog log flag.
     * @param preferences the preferences.
     */
    public static void setLogAbsurd( boolean doLog, SharedPreferences preferences ) {
        GPLog.LOG_ABSURD = doLog;
        Editor edit = preferences.edit();
        edit.putBoolean(PREFS_KEY_LOG_ABSURD, doLog);
        edit.commit();
    }

    /**
     * @param preferences the preferences.
     * @return if <code>true</code>, log is enabled.
     */
    public static boolean checkLog( SharedPreferences preferences ) {
        boolean doLog = preferences.getBoolean(PREFS_KEY_LOG, false);
        GPLog.LOG = doLog;
        return doLog;
    }

    /**
     * @param preferences the preferences.
     * @return if <code>true</code>, log is enabled.
     */
    public static boolean checkLogHeavy( SharedPreferences preferences ) {
        boolean doLog = preferences.getBoolean(PREFS_KEY_LOG_HEAVY, false);
        GPLog.LOG_HEAVY = doLog;
        return doLog;
    }

    /**
     * @param preferences the preferences.
     * @return if <code>true</code>, log is enabled.
     */
    public static boolean checkLogAbsurd( SharedPreferences preferences ) {
        boolean doLog = preferences.getBoolean(PREFS_KEY_LOG_ABSURD, false);
        GPLog.LOG_ABSURD = doLog;
        return doLog;
    }

}
