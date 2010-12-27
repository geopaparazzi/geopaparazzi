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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;

/**
 * A logger class that can also write to file.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Logger {

    private static boolean logToFile = false;
    private static File debugLogFile;

    private static enum LOGTYPE {
        D, I, W, E;
    }

    public Logger( File logFile ) {
        if (logFile == null) {
            logToFile = false;
        } else {
            debugLogFile = logFile;
            logToFile = true;
        }
    }

    private static String toName( Object obj ) {
        String simpleName = obj.getClass().getSimpleName();
        return simpleName.toUpperCase();
    }

    public static int e( Object caller, String message, Throwable t ) {
        String name = toName(caller);
        return e(name, message, t);
    }

    private static int e( String name, String message, Throwable t ) {
        if (!logToFile) {
            return Log.e(name, message);
        } else {
            // do also normal log
            Log.e(name, message, t);
            // and dump
            StringBuilder sb = new StringBuilder();
            sb.append(message);
            sb.append("\n");
            sb.append(t.getMessage());
            StackTraceElement[] stackTrace = t.getStackTrace();
            for( StackTraceElement ste : stackTrace ) {
                sb.append("in class: ");
                sb.append(ste.getClassName()).append(" method: ");
                sb.append(ste.getMethodName()).append(" line:");
                sb.append(ste.getLineNumber()).append("\n");
            }
            message = sb.toString();
            dumpToFile(name, message, LOGTYPE.E);
        }
        return -1;
    }

    public static int d( Object caller, String message ) {
        String name = toName(caller);
        return d(name, message);
    }

    private static int d( String name, String message ) {
        if (!logToFile) {
            return Log.d(name, message);
        } else {
            dumpToFile(name, message, LOGTYPE.D);
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
        BufferedWriter bw = null;
        StringBuilder sb = new StringBuilder();
        try {
            bw = new BufferedWriter(new FileWriter(debugLogFile, true), 1024);
            switch( type ) {
            case D:
                sb.append("DEBUG: ");
                break;
            case I:
                sb.append("INFO: ");
                break;
            case W:
                sb.append("WARN: ");
                break;
            case E:
                sb.append("ERROR: ");
                break;
            default:
                sb.append("DEBUG: ");
                break;
            }
            sb.append(name);
            sb.append(" - ");
            sb.append(message);
            sb.append("\n");
            bw.write(sb.toString());
        } catch (IOException e) {
            Log.e("LOGGER", "error in dumping to file 1", e);
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    Log.e("LOGGER", "error in dumping to file 2", e);
                    e.printStackTrace();
                }
            }
        }
    }

}
