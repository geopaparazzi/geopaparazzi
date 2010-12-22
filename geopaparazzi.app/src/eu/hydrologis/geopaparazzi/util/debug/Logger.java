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
package eu.hydrologis.geopaparazzi.util.debug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;

/**
 * A logger class tat can also write to file.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Logger {

    private static boolean logToFile = false;
    private static File debugLogFile;

    private static enum LOGTYPE {
        D, I, W;
    }

    public Logger( Context context ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.enable_debug);
        logToFile = preferences.getBoolean(key, false);
        if (logToFile) {
            debugLogFile = ApplicationManager.getInstance(context).getDebugLogFile();
        }
    }

    private static String toName( Object obj ) {
        String simpleName = obj.getClass().getSimpleName();
        return simpleName.toUpperCase();
    }

    public static int d( Object caller, String message ) {
        if (!logToFile) {
            return Log.d(toName(caller), message);
        } else {
            dumpToFile(toName(caller), message, LOGTYPE.D);
        }
        return -1;
    }

    public static int i( Object caller, String message ) {
        if (!logToFile) {
            return Log.i(toName(caller), message);
        } else {
            dumpToFile(toName(caller), message, LOGTYPE.I);
        }
        return -1;
    }

    public static int w( Object caller, String message ) {
        if (!logToFile) {
            return Log.w(toName(caller), message);
        } else {
            dumpToFile(toName(caller), message, LOGTYPE.W);
        }
        return -1;
    }

    private static void dumpToFile( String name, String message, LOGTYPE type ) {
        BufferedWriter br = null;
        try {
            br = new BufferedWriter(new FileWriter(debugLogFile));
            switch( type ) {
            case D:
                br.write("DEBUG: ");
                break;
            case I:
                br.write("INFO: ");
                break;
            case W:
                br.write("WARN: ");
                break;
            default:
                br.write("DEBUG: ");
                break;
            }
            br.write(name);
            br.write(" - ");
            br.write(message);
            br.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
