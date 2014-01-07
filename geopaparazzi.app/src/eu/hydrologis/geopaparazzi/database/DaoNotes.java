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
package eu.hydrologis.geopaparazzi.database;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.GeoPoint;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.Drawable;
import android.util.Log;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.hydrologis.geopaparazzi.maps.overlays.NoteOverlayItem;
import eu.hydrologis.geopaparazzi.util.Note;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoNotes {

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_LON = "lon";
    private static final String COLUMN_LAT = "lat";
    private static final String COLUMN_ALTIM = "altim";
    private static final String COLUMN_TS = "ts";
    private static final String COLUMN_TEXT = "text";
    private static final String COLUMN_CATEGORY = "cat";
    private static final String COLUMN_FORM = "form";

    /**
     * The type of the note.
     * 
     * <p>
     * For now we support:
     * <ul>
     *  <li>0 = simple note</li>
     *  <li>1 = osm note</li>
     * </ul>
     * </p>
     */
    private static final String COLUMN_TYPE = "type";

    public static final String TABLE_NOTES = "notes";

    private static long LASTINSERTEDNOTE_ID = -1;


    /**
     * @param lon
     * @param lat
     * @param altim
     * @param timestamp the UTC timestamp string.
     * @param text
     * @param category
     * @param form
     * @param type
     * @throws IOException
     */
    public static void addNote( double lon, double lat, double altim, String timestamp, String text, String category, String form,
            int type ) throws IOException {
        if (category == null) {
            category = NoteType.POI.getDef();
        }

        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            addNoteNoTransaction(lon, lat, altim, timestamp, text, category, form, type, sqliteDatabase);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAONOTES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void addNoteNoTransaction( double lon, double lat, double altim, String timestamp, String text, String category,
            String form, int type, SQLiteDatabase sqliteDatabase ) {
        if (category == null) {
            category = NoteType.POI.getDef();
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_LON, lon);
        values.put(COLUMN_LAT, lat);
        values.put(COLUMN_ALTIM, altim);
        values.put(COLUMN_TS, timestamp);
        values.put(COLUMN_TEXT, text);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_FORM, form);
        values.put(COLUMN_TYPE, type);
        LASTINSERTEDNOTE_ID = sqliteDatabase.insertOrThrow(TABLE_NOTES, null, values);
    }

    public static void deleteNote( long id ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            // delete note
            String query = "delete from " + TABLE_NOTES + " where " + COLUMN_ID + " = " + id;
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAONOTES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void updateForm( long id, String noteText, String jsonStr ) throws IOException {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(COLUMN_FORM, jsonStr);
        if (noteText != null && noteText.length() > 0) {
            updatedValues.put(COLUMN_TEXT, noteText);
        }

        String where = COLUMN_ID + "=" + id;
        String[] whereArgs = null;

        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();

        sqliteDatabase.update(TABLE_NOTES, updatedValues, where, whereArgs);
    }

    public static void deleteNotesByType( NoteType noteType ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            // delete note
            String query = "delete from " + TABLE_NOTES + " where " + COLUMN_TYPE + " = " + noteType.getTypeNum();
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAONOTES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void deleteLastInsertedNote() throws IOException {
        if (LASTINSERTEDNOTE_ID != -1) {
            deleteNote(LASTINSERTEDNOTE_ID);
        }
    }

    // public static void importGpxToNotes( Context context, GpxItem gpxItem ) throws IOException {
    // SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
    // sqliteDatabase.beginTransaction();
    // try {
    // List<PointF3D> points = gpxItem.read();
    // List<String> names = gpxItem.getNames();
    // for( int i = 0; i < points.size(); i++ ) {
    // Date date = new Date(System.currentTimeMillis());
    // String dateStrs = Constants.TIME_FORMATTER_SQLITE.format(date);
    // PointF3D point = points.get(i);
    // String name = names.get(i);
    // ContentValues values = new ContentValues();
    // values.put(COLUMN_LON, point.x);
    // values.put(COLUMN_LAT, point.y);
    // values.put(COLUMN_ALTIM, point.getZ());
    // values.put(COLUMN_TS, dateStrs);
    // values.put(COLUMN_TEXT, name);
    // sqliteDatabase.insertOrThrow(TABLE_NOTES, null, values);
    // }
    // sqliteDatabase.setTransactionSuccessful();
    // } catch (Exception e) {
    // throw new IOException(e.getLocalizedMessage());
    // } finally {
    // sqliteDatabase.endTransaction();
    // }
    // }

    /**
     * Get the collected notes from the database inside a given bound.
     * @param n
     * @param s
     * @param w
     * @param e
     * 
     * @return the list of notes inside the bounds.
     * @throws IOException
     */
    public static List<Note> getNotesInWorldBounds( float n, float s, float w, float e ) throws IOException {

        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        String query = "SELECT _id, lon, lat, altim, text, cat, ts, type, form FROM XXX WHERE (lon BETWEEN XXX AND XXX) AND (lat BETWEEN XXX AND XXX)";
        // String[] args = new String[]{TABLE_NOTES, String.valueOf(w), String.valueOf(e),
        // String.valueOf(s), String.valueOf(n)};

        query = query.replaceFirst("XXX", TABLE_NOTES);
        query = query.replaceFirst("XXX", String.valueOf(w));
        query = query.replaceFirst("XXX", String.valueOf(e));
        query = query.replaceFirst("XXX", String.valueOf(s));
        query = query.replaceFirst("XXX", String.valueOf(n));

        // if (Debug.D) Logger.i("DAONOTES", "Query: " + query);

        Cursor c = sqliteDatabase.rawQuery(query, null);
        List<Note> notes = new ArrayList<Note>();
        c.moveToFirst();
        while( !c.isAfterLast() ) {
            long id = c.getLong(0);
            double lon = c.getDouble(1);
            double lat = c.getDouble(2);
            double altim = c.getDouble(3);
            String text = c.getString(4);
            String category = c.getString(5);
            String timestamp = c.getString(6);
            int type = c.getInt(7);
            String form = c.getString(8);

            Note note = new Note(id, text, "", timestamp, lon, lat, altim, category, form, type);
            notes.add(note);
            c.moveToNext();
        }
        c.close();
        return notes;
    }

    /**
     * Get the list of notes from the db.
     * 
     * @return list of notes.
     * @throws IOException
     */
    public static List<Note> getNotesList() throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        List<Note> notesList = new ArrayList<Note>();
        String asColumnsToReturn[] = {COLUMN_ID, COLUMN_LON, COLUMN_LAT, COLUMN_ALTIM, COLUMN_TS, COLUMN_TEXT, COLUMN_CATEGORY,
                COLUMN_FORM, COLUMN_TYPE};
        String strSortOrder = "_id ASC";
        Cursor c = sqliteDatabase.query(TABLE_NOTES, asColumnsToReturn, null, null, null, null, strSortOrder);
        c.moveToFirst();
        while( !c.isAfterLast() ) {
            long id = c.getLong(0);
            double lon = c.getDouble(1);
            double lat = c.getDouble(2);
            double altim = c.getDouble(3);
            String timestamp = c.getString(4);
            String text = c.getString(5);
            String category = c.getString(6);
            String form = c.getString(7);
            int type = c.getInt(8);

            Note note = new Note(id, text, "", timestamp, lon, lat, altim, category, form, type);
            notesList.add(note);
            c.moveToNext();
        }
        c.close();
        return notesList;
    }

    /**
     * Get the list of notes from the db as OverlayItems.
     * @param marker 
     * @return list of notes.
     * @throws IOException
     */
    public static List<OverlayItem> getNoteOverlaysList( Drawable marker ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        List<OverlayItem> notesList = new ArrayList<OverlayItem>();
        String asColumnsToReturn[] = {COLUMN_LON, COLUMN_LAT, COLUMN_TS, COLUMN_TEXT};// ,
                                                                                      // COLUMN_FORM};
        String strSortOrder = "_id ASC";
        Cursor c = sqliteDatabase.query(TABLE_NOTES, asColumnsToReturn, null, null, null, null, strSortOrder);
        c.moveToFirst();
        while( !c.isAfterLast() ) {
            double lon = c.getDouble(0);
            double lat = c.getDouble(1);
            String date = c.getString(2);
            String text = c.getString(3);
            // String form = c.getString(4);

            StringBuilder description = new StringBuilder();
            description.append(text);
            description.append("\n");
            description.append(date);

            NoteOverlayItem item1 = new NoteOverlayItem(new GeoPoint(lat, lon), text, description.toString(), marker);
            notesList.add(item1);

            c.moveToNext();
        }
        c.close();
        return notesList;
    }

    public static void upgradeNotesFromDB1ToDB2( SQLiteDatabase db ) throws IOException {

        StringBuilder sB = new StringBuilder();
        // make sure the form column doesn't exist
        sB = new StringBuilder();
        sB.append("SELECT ");
        sB.append(COLUMN_FORM);
        sB.append(" FROM ");
        sB.append(TABLE_NOTES);
        sB.append(";");
        String checkColumnQuery = sB.toString();
        try {
            db.rawQuery(checkColumnQuery, null);
            // if it comes to this point, the form column
            // exists already. Nothing to do.
            if (GPLog.LOG_ANDROID)
                Log.i("DAONOTES", "Database already contains form column. Skipping upgrade.");
            return;
        } catch (Exception e) {
            // ignore and add column
        }

        sB = new StringBuilder();
        sB.append("ALTER TABLE ");
        sB.append(TABLE_NOTES);
        sB.append(" ADD COLUMN ");
        sB.append(COLUMN_FORM).append(" CLOB;");
        String addColumnQuery = sB.toString();

        if (GPLog.LOG_ANDROID)
            Log.i("DAONOTES", "Upgrading database from version 1 to version 2.");

        db.beginTransaction();
        try {
            db.execSQL(addColumnQuery);
            db.setVersion(2);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DAONOTES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            db.endTransaction();
        }
    }

    public static void upgradeNotesFromDB4ToDB5( SQLiteDatabase db ) throws IOException {
        StringBuilder sB = new StringBuilder();
        // make sure the form column doesn't exist
        sB = new StringBuilder();
        sB.append("SELECT ");
        sB.append(COLUMN_TYPE);
        sB.append(" FROM ");
        sB.append(TABLE_NOTES);
        sB.append(";");
        String checkColumnQuery = sB.toString();
        try {
            db.rawQuery(checkColumnQuery, null);
            // if it comes to this point, the type column
            // exists already. Nothing to do.
            if (GPLog.LOG_ANDROID)
                Log.i("DAONOTES", "Database already contains the type column. Skipping upgrade.");
            return;
        } catch (Exception e) {
            // ignore and add column
        }

        sB = new StringBuilder();
        sB.append("ALTER TABLE ");
        sB.append(TABLE_NOTES);
        sB.append(" ADD COLUMN ");
        sB.append(COLUMN_TYPE).append(" INTEGER;");
        String addColumnQuery = sB.toString();

        if (GPLog.LOG_ANDROID)
            Log.i("DAONOTES", "Upgrading database from version 4 to version 5.");

        db.beginTransaction();
        try {
            db.execSQL(addColumnQuery);
            db.setVersion(5);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DAONOTES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            db.endTransaction();
        }
    }

    public static void upgradeNotesFromDB5ToDB6( SQLiteDatabase db ) throws IOException {
        StringBuilder sB = new StringBuilder();
        // make sure the form column doesn't exist
        sB = new StringBuilder();
        sB.append("SELECT ");
        sB.append(COLUMN_CATEGORY);
        sB.append(" FROM ");
        sB.append(TABLE_NOTES);
        sB.append(";");
        String checkColumnQuery = sB.toString();
        try {
            db.rawQuery(checkColumnQuery, null);
            // if it comes to this point, the type column
            // exists already. Nothing to do.
            if (GPLog.LOG_ANDROID)
                Log.i("DAONOTES", "Database already contains the category column. Skipping upgrade.");
            return;
        } catch (Exception e) {
            // ignore and add column
        }

        sB = new StringBuilder();
        sB.append("ALTER TABLE ");
        sB.append(TABLE_NOTES);
        sB.append(" ADD COLUMN ");
        sB.append(COLUMN_CATEGORY).append(" TEXT;");
        String addColumnQuery = sB.toString();

        if (GPLog.LOG_ANDROID)
            Log.i("DAONOTES", "Upgrading database from version 5 to version 6.");

        db.beginTransaction();
        try {
            db.execSQL(addColumnQuery);
            db.setVersion(6);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DAONOTES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            db.endTransaction();
        }
    }

    public static void createTables() throws IOException {
        StringBuilder sB = new StringBuilder();

        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_NOTES);
        sB.append(" (");
        sB.append(COLUMN_ID);
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(COLUMN_LON).append(" REAL NOT NULL, ");
        sB.append(COLUMN_LAT).append(" REAL NOT NULL,");
        sB.append(COLUMN_ALTIM).append(" REAL NOT NULL,");
        sB.append(COLUMN_TS).append(" DATE NOT NULL,");
        sB.append(COLUMN_TEXT).append(" TEXT NOT NULL, ");
        sB.append(COLUMN_CATEGORY).append(" TEXT, ");
        sB.append(COLUMN_FORM).append(" CLOB, ");
        sB.append(COLUMN_TYPE).append(" INTEGER");
        sB.append(");");
        String CREATE_TABLE_NOTES = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX notes_ts_idx ON ");
        sB.append(TABLE_NOTES);
        sB.append(" ( ");
        sB.append(COLUMN_TS);
        sB.append(" );");
        String CREATE_INDEX_NOTES_TS = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX notes_x_by_y_idx ON ");
        sB.append(TABLE_NOTES);
        sB.append(" ( ");
        sB.append(COLUMN_LON);
        sB.append(", ");
        sB.append(COLUMN_LAT);
        sB.append(" );");
        String CREATE_INDEX_NOTES_X_BY_Y = sB.toString();

        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        if (GPLog.LOG_HEAVY)
            Log.i("DAONOTES", "Create the notes table.");

        sqliteDatabase.beginTransaction();
        try {
            sqliteDatabase.execSQL(CREATE_TABLE_NOTES);
            sqliteDatabase.execSQL(CREATE_INDEX_NOTES_TS);
            sqliteDatabase.execSQL(CREATE_INDEX_NOTES_X_BY_Y);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DAONOTES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

}
