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

import java.io.IOException;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.util.TimeUtilities;

/**
 * The class that handles logging to the database.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class GPLog {
    /**
     * If <code>true</code>, android logging is activated.
     */
    public final static boolean LOG_ANDROID = true;
    /**
     * If <code>true</code>, normal logging is activated.
     */
    public static boolean LOG = true;

    /**
     * If <code>true</code> heavy logging is activated.
     */
    public static boolean LOG_HEAVY = true;

    /**
     * If <code>true</code>, all logging is activated.
     */
    public static boolean LOG_ABSURD = false;

    /**
     * 
     */
    public static final String ERROR_TAG = "ERROR_GEOPAPARAZZI";

    /**
     * 
     */
    public static final String TABLE_LOG = "log";
    /**
     * 
     */
    public static final String COLUMN_ID = "_id";
    /**
     * 
     */
    public static final String COLUMN_DATAORA = "dataora";
    /**
     * 
     */
    public static final String COLUMN_LOGMSG = "logmsg";

    /**
     * Global default log tag (used in {@link #androidLog(int, String)} and {@link #androidLog(int, String, Throwable)}.
     */
    public static String GLOBAL_LOG_TAG = "GEOPAPARAZZI";

    /**
     * Global default log level (used in {@link #androidLog(int, String)} and {@link #androidLog(int, String, Throwable)}.
     */
    public static int GLOBAL_LOG_LEVEL = 0;

    /**
     * Create the default log table.
     *
     * @param sqliteDatabase the db into which to create the table.
     * @throws IOException  if something goes wrong.
     */
    public static void createTables( SQLiteDatabase sqliteDatabase ) throws IOException {
        StringBuilder sB = new StringBuilder();

        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_LOG);
        sB.append(" (");
        sB.append(COLUMN_ID);
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(COLUMN_DATAORA).append(" INTEGER NOT NULL, ");
        sB.append(COLUMN_LOGMSG).append(" TEXT ");
        sB.append(");");
        String CREATE_TABLE = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX " + TABLE_LOG + "_" + COLUMN_ID + " ON ");
        sB.append(TABLE_LOG);
        sB.append(" ( ");
        sB.append(COLUMN_ID);
        sB.append(" );");
        String CREATE_INDEX = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX " + TABLE_LOG + "_" + COLUMN_DATAORA + " ON ");
        sB.append(TABLE_LOG);
        sB.append(" ( ");
        sB.append(COLUMN_DATAORA);
        sB.append(" );");
        String CREATE_INDEX_DATE = sB.toString();

        sqliteDatabase.beginTransaction();
        try {
            sqliteDatabase.execSQL(CREATE_TABLE);
            sqliteDatabase.execSQL(CREATE_INDEX);
            sqliteDatabase.execSQL(CREATE_INDEX_DATE);
            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Add a new log entry.
     *
     * @param logMessage the message to insert in the log.
     */
    public static void addLogEntry( String logMessage ) {
        try {
            Date date = new Date();
            SQLiteDatabase sqliteDatabase = GPApplication.getInstance().getDatabase();
            if (sqliteDatabase != null && sqliteDatabase.isOpen()) {
                ContentValues values = new ContentValues();
                long time = date.getTime();
                values.put(COLUMN_DATAORA, time);
                values.put(COLUMN_LOGMSG, logMessage);
                insertOrThrow(sqliteDatabase, TABLE_LOG, values);
            }

            if (LOG_ANDROID) {
                StringBuilder sb = new StringBuilder();
                sb.append(TimeUtilities.INSTANCE.iso8601Format.format(date));
                sb.append(": ");
                sb.append(logMessage);
                String string = sb.toString();
                log(GLOBAL_LOG_TAG, string);
            }
        } catch (Exception e) {
            Log.e(GLOBAL_LOG_TAG, logMessage, e);
        }
    }

    private static int log( String tag, String string ) {
        if (string == null || string.length() == 0) {
            string = "no message passed to the log";
        }
        return Log.i(tag, string);
    }

    /**
     * Add a log entry by concatenating (;) some more info in the message.
     *
     * @param caller the calling class or tage name.
     * @param user a user name or id. If
     *              <code>null</code>, defaults to UNKNOWN_USER
     * @param tag a tag for the log message. If <code>null</code>,
     *              defaults to INFO.
     * @param logMessage the message itself.
     */
    public static void addLogEntry( Object caller, //
            String user, //
            String tag,//
            String logMessage ) {

        StringBuilder sb = new StringBuilder();
        if (user == null || user.length() == 0) {
            user = "UU";
        }
        sb.append(user).append(";");
        if (tag == null || tag.length() == 0) {
            tag = "INFO";
        }
        sb.append(tag).append(";");

        if (caller != null) {
            String name = toName(caller);
            if (name.length() > 0)
                sb.append(name).append(": ");
        }
        sb.append(logMessage);
        try {
            addLogEntry(sb.toString());
        } catch (Exception e) {
            Log.e(ERROR_TAG, "Error inserting in log.", e);
        }
    }

    /**
     * Add a log entry by concatenating (;) some more info in the message.
     *
     * @param caller the calling class or tage name.
     * @param logMessage the message itself.
     */
    public static void addLogEntry( Object caller, //
            String logMessage ) {
        addLogEntry(caller, null, null, logMessage);
    }

    /**
     * Error log.
     * 
     * @param caller caller object.
     * @param msg message or <code>null</code>.
     * @param t a throwable.
     */
    public static void error( Object caller, String msg, Throwable t ) {
        String localizedMessage = t.getLocalizedMessage();
        if (msg != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(msg);
            sb.append(": ");
            sb.append(localizedMessage);
            localizedMessage = sb.toString();
        }
        addLogEntry(caller, null, ERROR_TAG, localizedMessage);
        if (LOG_ANDROID) {
            log("GPLOG_ERROR", localizedMessage);
        }
        String stackTrace = Log.getStackTraceString(t);
        addLogEntry(caller, null, ERROR_TAG, stackTrace);
        if (LOG_ANDROID) {
            log("GPLOG_ERROR", stackTrace);
        }
    }
    /**
     * Do an insert or throw with the proper error handling.
     * @param table
     * @param values
     *
     * @return
     * @throws IOException
     */
    private static long insertOrThrow( SQLiteDatabase sqliteDatabase, String table, ContentValues values ) throws Exception {
        if (sqliteDatabase == null || !sqliteDatabase.isOpen()) {
            throw new Exception("Database not ready!");
        }
        long id = sqliteDatabase.insertOrThrow(table, null, values);
        if (id == -1) {
            Set<Entry<String, Object>> valueSet = values.valueSet();
            StringBuilder sb = new StringBuilder();
            sb.append("Insert failed with: \n");
            for( Entry<String, Object> entry : valueSet ) {
                sb.append("(").append(entry.getKey()).append(",");
                sb.append(entry.getValue()).append(")\n");
            }
            String message = sb.toString();
            throw new IOException(message);
        }
        return id;
    }

    /**
     * Clear the log table.
     *
     * @param db the db to use.
     * @throws Exception  if something goes wrong.
     */
    public static void clearLogTable( SQLiteDatabase db ) throws Exception {
        String deleteLogQuery = "delete from " + TABLE_LOG;
        db.beginTransaction();
        try {
            db.execSQL(deleteLogQuery);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * @return the query to get id,datetimestring,logmsg.
     */
    public static String getLogQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("select _id, datetime(dataora/1000, 'unixepoch', 'localtime') as timestamp, logmsg from log order by dataora desc");
        String query = sb.toString();
        return query;
    }

    private static String toName( Object obj ) {
        if (obj instanceof String) {
            String name = (String) obj;
            return name;
        }
        String simpleName = obj.getClass().getSimpleName();
        return simpleName.toUpperCase();
    }

    // ////////////////////////////////////////////////////
    // ANDROID LOG UTILITIES
    // ////////////////////////////////////////////////////

    /**
    * Function for global logging.
    * 
    * @param logLevel the log level to use. 
    * <ul>
    * <li>-1=use global value</li>
    * <li>0=no message</li>
    * <li>1=info</li>
    * <li>2=warning</li>
    * <li>3=error</li>
    * <li>4=debug</li>
    * <li>5=What a Terrible Failure!</li>
    * <li>6=verbose</li>
    * </ul>
    * @param message message text to be shown in logcat.
    * @param exception result of Log.getStackTraceString(exception) will be added to the message.
    */
    public static void androidLog( int logLevel, String message, Throwable exception ) {
        if (!LOG_ANDROID)
            return;
        if (exception != null) {
            message += "\n" + Log.getStackTraceString(exception);
        }
        androidLog(logLevel, message);
    }
    /**
    * Function for global logging.
    *
    * @param logLevel the log level to use. 
    * <ul>
    * <li>-1=use global value</li>
    * <li>0=no message</li>
    * <li>1=info</li>
    * <li>2=warning</li>
    * <li>3=error</li>
    * <li>4=debug</li>
    * <li>5=What a Terrible Failure!</li>
    * <li>6=verbose</li>
    * </ul>
    * @param message message text to be shown in logcat.
    */
    public static void androidLog( int logLevel, String message ) {
        if (!LOG_ANDROID)
            return;
        if (logLevel < 0)
            logLevel = GLOBAL_LOG_LEVEL;
        if (GLOBAL_LOG_TAG == null || GLOBAL_LOG_TAG.length() == 0)
            GLOBAL_LOG_TAG = "GEOPAPARAZZI";
        switch( logLevel ) {
        case 0:
            // ignore
            break;
        case 2: // method is used to log warnings.
            Log.w(GLOBAL_LOG_TAG, message);
            break;
        case 3: // method is used to log errors.
            Log.e(GLOBAL_LOG_TAG, message);
            break;
        case 4: // method is used to log debug messages.
            Log.d(GLOBAL_LOG_TAG, message);
            break;
        case 5: // method is used to log terrible failures that should never happen.
            Log.wtf(GLOBAL_LOG_TAG, message);
            break;
        case 6: // method is used to log verbose messages.
            Log.v(GLOBAL_LOG_TAG, message);
            break;
        case 1:
        default: // method is used to log informational messages.
            Log.i(GLOBAL_LOG_TAG, message);
            break;
        }
    }

}
