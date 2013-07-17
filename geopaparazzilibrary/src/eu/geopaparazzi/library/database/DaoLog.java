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

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoLog {

    public static final String TABLE_LOG = "log";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATAORA = "dataora";
    public static final String COLUMN_LOGMSG = "logmsg";

    /**
     * Create the default log table.
     * 
     * @param sqliteDatabase the db into which to create the table.
     * @throws IOException
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
     * @param sqliteDatabase the db to use.
     * @param logMessage the message to insert in the log.
     * @throws IOException
     */
    public static void addLogEntry( SQLiteDatabase sqliteDatabase, String logMessage ) throws IOException {
        ContentValues values = new ContentValues();
        long time = new Date().getTime();
        values.put(COLUMN_DATAORA, time);
        values.put(COLUMN_LOGMSG, logMessage);
        insertOrThrow(sqliteDatabase, TABLE_LOG, values);
    }

    /**
     * Add a log entry by concatenating (;) some more info in the message.
     * 
     * @param sqliteDatabase
     * @param user a user name or id.
     * @param msgType a description of the log message type. If
     *              <code>null</code>, defaults to UNKNOWN_USER
     * @param logMessage the message itself. If <code>null</code>, 
     *              defaults to INFO.
     * @throws IOException
     */
    public static void addLogEntry( SQLiteDatabase sqliteDatabase,//
            String user, //
            String msgType,//
            String logMessage ) throws IOException {

        StringBuilder sb = new StringBuilder();
        if (user == null || user.length() == 0) {
            user = "UNKNOWN_USER";
        }
        sb.append(user).append(";");
        if (msgType == null || msgType.length() == 0) {
            msgType = "INFO";
        }
        sb.append(msgType).append(";");
        sb.append(logMessage);
        addLogEntry(sqliteDatabase, sb.toString());
    }

    /**
     * Do an insert or throw with the proper error handling.
     * @param sqliteDatabase
     * @param table
     * @param values
     * 
     * @return
     * @throws IOException
     */
    private static long insertOrThrow( SQLiteDatabase sqliteDatabase, String table, ContentValues values ) throws IOException {
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
     * @throws Exception
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

    public static String getLogQuery() {
        // select _id, datetime(dataora/1000, 'unixepoch'), logmsg from log order by
        // dataora desc
        StringBuilder sb = new StringBuilder();
        sb.append("select _id, datetime(dataora/1000, 'unixepoch'), logmsg from log order by dataora desc");
        String query = sb.toString();
        return query;
    }
}
