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
package eu.hydrologis.geopaparazzi.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.Coordinates;
import org.mapsforge.core.model.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.database.objects.Note;
import eu.hydrologis.geopaparazzi.database.objects.NoteOverlayItem;

import static eu.hydrologis.geopaparazzi.database.TableDescriptions.NotesTableFields;
import static eu.hydrologis.geopaparazzi.database.TableDescriptions.TABLE_NOTES;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoNotes {

    /**
     * Create the notes tables.
     *
     * @throws IOException if something goes wrong.
     */
    public static void createTables() throws IOException {
        StringBuilder sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_NOTES);
        sB.append(" (");
        sB.append(NotesTableFields.COLUMN_ID.getFieldName());
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(NotesTableFields.COLUMN_LON.getFieldName()).append(" REAL NOT NULL, ");
        sB.append(NotesTableFields.COLUMN_LAT.getFieldName()).append(" REAL NOT NULL,");
        sB.append(NotesTableFields.COLUMN_ALTIM.getFieldName()).append(" REAL NOT NULL,");
        sB.append(NotesTableFields.COLUMN_TS.getFieldName()).append(" DATE NOT NULL,");
        sB.append(NotesTableFields.COLUMN_DESCRIPTION.getFieldName()).append(" TEXT, ");
        sB.append(NotesTableFields.COLUMN_TEXT.getFieldName()).append(" TEXT NOT NULL, ");
        sB.append(NotesTableFields.COLUMN_FORM.getFieldName()).append(" CLOB, ");
        sB.append(NotesTableFields.COLUMN_STYLE.getFieldName()).append(" TEXT,");
        sB.append(NotesTableFields.COLUMN_ISDIRTY.getFieldName()).append(" INTEGER");
        sB.append(");");
        String CREATE_TABLE_NOTES = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX notes_ts_idx ON ");
        sB.append(TABLE_NOTES);
        sB.append(" ( ");
        sB.append(NotesTableFields.COLUMN_TS.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_NOTES_TS = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX notes_x_by_y_idx ON ");
        sB.append(TABLE_NOTES);
        sB.append(" ( ");
        sB.append(NotesTableFields.COLUMN_LON.getFieldName());
        sB.append(", ");
        sB.append(NotesTableFields.COLUMN_LAT.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_NOTES_X_BY_Y = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX notes_isdirty_idx ON ");
        sB.append(TABLE_NOTES);
        sB.append(" ( ");
        sB.append(NotesTableFields.COLUMN_ISDIRTY.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_NOTES_ISDIRTY = sB.toString();

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        if (GPLog.LOG_HEAVY)
            Log.i("DAONOTES", "Create the notes table.");

        sqliteDatabase.beginTransaction();
        try {
            sqliteDatabase.execSQL(CREATE_TABLE_NOTES);
            sqliteDatabase.execSQL(CREATE_INDEX_NOTES_TS);
            sqliteDatabase.execSQL(CREATE_INDEX_NOTES_X_BY_Y);
            sqliteDatabase.execSQL(CREATE_INDEX_NOTES_ISDIRTY);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DAONOTES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }


    /**
     * Add a new note to the database.
     *
     * @param lon         lon
     * @param lat         lat
     * @param altim       elevation
     * @param timestamp   the UTC timestamp in millis.
     * @param text        a text
     * @param description an optional description for the note.
     * @param form        the optional json form.
     * @param style       the optional style definition.
     * @return the inserted note id.
     * @throws IOException if something goes wrong.
     */
    public static long addNote(double lon, double lat, double altim, long timestamp, String text, String description,
                               String form, String style) throws IOException {
        if (description == null) {
            description = "note";
        }

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            long noteId = addNoteNoTransaction(lon, lat, altim, timestamp, text, description, form, style, sqliteDatabase);

            sqliteDatabase.setTransactionSuccessful();

            return noteId;
        } catch (Exception e) {
            GPLog.error("DAONOTES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Add a note without transaction (for fast insert of many).
     *
     * @param lon            lon
     * @param lat            lat
     * @param altim          elevation
     * @param timestamp      the UTC timestamp in millis.
     * @param text           the text
     * @param description    a description.
     * @param form           the json form or null
     * @param sqliteDatabase the database reference.
     * @return the inserted note id.
     */
    public static long addNoteNoTransaction(double lon, double lat, double altim, long timestamp, String text,
                                            String description, String form, String style, SQLiteDatabase sqliteDatabase) {
        ContentValues values = new ContentValues();
        values.put(NotesTableFields.COLUMN_LON.getFieldName(), lon);
        values.put(NotesTableFields.COLUMN_LAT.getFieldName(), lat);
        values.put(NotesTableFields.COLUMN_ALTIM.getFieldName(), altim);
        values.put(NotesTableFields.COLUMN_TS.getFieldName(), timestamp);
        if (description != null)
            values.put(NotesTableFields.COLUMN_DESCRIPTION.getFieldName(), description);
        values.put(NotesTableFields.COLUMN_TEXT.getFieldName(), text);
        if (form != null)
            values.put(NotesTableFields.COLUMN_FORM.getFieldName(), form);
        if (style != null)
            values.put(NotesTableFields.COLUMN_STYLE.getFieldName(), style);
        values.put(NotesTableFields.COLUMN_ISDIRTY.getFieldName(), 1);
        long noteId = sqliteDatabase.insertOrThrow(TABLE_NOTES, null, values);
        return noteId;
    }

    /**
     * Delete a note.
     *
     * @param id the note id.
     * @throws IOException if something goes wrong.
     */
    public static void deleteNote(long id) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            // delete note
            String query = "delete from " + TABLE_NOTES + " where " + NotesTableFields.COLUMN_ID.getFieldName() + " = " + id;
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

    /**
     * Delete all OSM type notes.
     *
     * @throws IOException if something goes wrong.
     */
    public static void deleteOsmNotes() throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            // delete note
            String query = "delete from " + TABLE_NOTES + " where " + NotesTableFields.COLUMN_DESCRIPTION.getFieldName() + " = " + LibraryConstants.OSM;
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


    /**
     * Update the form of a note.
     *
     * @param id       the note id.
     * @param noteText the note text.
     * @param jsonStr  the form data.
     * @throws IOException if something goes wrong.
     */
    public static void updateForm(long id, String noteText, String jsonStr) throws IOException {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(NotesTableFields.COLUMN_FORM.getFieldName(), jsonStr);
        if (noteText != null && noteText.length() > 0) {
            updatedValues.put(NotesTableFields.COLUMN_TEXT.getFieldName(), noteText);
        }

        String where = NotesTableFields.COLUMN_ID.getFieldName() + "=" + id;
        String[] whereArgs = null;

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();

        sqliteDatabase.update(TABLE_NOTES, updatedValues, where, whereArgs);
    }

    /**
     * Get the collected notes from the database inside a given bound.
     *
     * @param nswe      optional bounds as [n, s, w, e].
     * @param onlyDirty get only dirty notes.
     * @return the list of notes inside the bounds.
     * @throws IOException if something goes wrong.
     */
    public static List<Note> getNotesList(float[] nswe, boolean onlyDirty) throws IOException {

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();

        String query = "SELECT " +//
                NotesTableFields.COLUMN_ID.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_LON.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_LAT.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_ALTIM.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_TEXT.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_TS.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_DESCRIPTION.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_STYLE.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_FORM.getFieldName() +//
                ", " +//
                NotesTableFields.COLUMN_ISDIRTY.getFieldName() +//
                " FROM " + TABLE_NOTES;
        if (nswe != null) {
            query = query + " WHERE (lon BETWEEN XXX AND XXX) AND (lat BETWEEN XXX AND XXX)";
            query = query.replaceFirst("XXX", String.valueOf(nswe[2]));
            query = query.replaceFirst("XXX", String.valueOf(nswe[3]));
            query = query.replaceFirst("XXX", String.valueOf(nswe[1]));
            query = query.replaceFirst("XXX", String.valueOf(nswe[0]));
        }
        if (onlyDirty)
            query = query + " AND " + NotesTableFields.COLUMN_ISDIRTY.getFieldName() + " = 1";

        Cursor c = sqliteDatabase.rawQuery(query, null);
        List<Note> notes = new ArrayList<>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            long id = c.getLong(0);
            double lon = c.getDouble(1);
            double lat = c.getDouble(2);
            double altim = c.getDouble(3);
            String text = c.getString(4);
            long timestamp = c.getLong(5);
            String description = c.getString(6);
            String style = c.getString(7);
            String form = c.getString(8);
            int isDirty = c.getInt(9);

            Note note = new Note(id, text, description, timestamp, lon, lat, altim, form, isDirty, style);
            notes.add(note);
            c.moveToNext();
        }
        c.close();
        return notes;
    }

    public static Note getNoteById(long checkId) throws IOException {

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();

        String query = "SELECT " +//
                NotesTableFields.COLUMN_ID.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_LON.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_LAT.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_ALTIM.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_TEXT.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_TS.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_DESCRIPTION.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_STYLE.getFieldName() +
                ", " +//
                NotesTableFields.COLUMN_FORM.getFieldName() +//
                ", " +//
                NotesTableFields.COLUMN_ISDIRTY.getFieldName() +//
                " FROM " + TABLE_NOTES;
        query = query + " WHERE " + NotesTableFields.COLUMN_ID.getFieldName() + "=" + checkId;

        Cursor c = sqliteDatabase.rawQuery(query, null);
        try {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                long id = c.getLong(0);
                double lon = c.getDouble(1);
                double lat = c.getDouble(2);
                double altim = c.getDouble(3);
                String text = c.getString(4);
                long timestamp = c.getLong(5);
                String description = c.getString(6);
                String style = c.getString(7);
                String form = c.getString(8);
                int isDirty = c.getInt(9);

                Note note = new Note(id, text, description, timestamp, lon, lat, altim, form, isDirty, style);
                return note;
            }
        } finally {
            c.close();
        }
        return null;
    }

    /**
     * Get the list of notes from the db as OverlayItems.
     *
     * @param marker the marker to use.
     * @return list of notes.
     * @throws IOException if something goes wrong.
     */
    public static List<OverlayItem> getNoteOverlaysList(Drawable marker) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        List<OverlayItem> notesList = new ArrayList<>();
        String asColumnsToReturn[] = { //
                NotesTableFields.COLUMN_LON.getFieldName(), //
                NotesTableFields.COLUMN_LAT.getFieldName(), //
                NotesTableFields.COLUMN_TS.getFieldName(), //
                NotesTableFields.COLUMN_TEXT.getFieldName() //
        };// ,
        String strSortOrder = "_id ASC";
        Cursor c = sqliteDatabase.query(TABLE_NOTES, asColumnsToReturn, null, null, null, null, strSortOrder);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            double lon = c.getDouble(0);
            double lat = c.getDouble(1);


            if(!PositionUtilities.isValidCoordinateLL(lon, lat)){
                continue;
            }

            long date = c.getLong(2);
            String text = c.getString(3);

            StringBuilder description = new StringBuilder();
            description.append(text);
            description.append("\n\n");
            description.append(TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(date)));

            NoteOverlayItem item1 = new NoteOverlayItem(new GeoPoint(lat, lon), text, description.toString(), marker);
            notesList.add(item1);

            c.moveToNext();
        }
        c.close();
        return notesList;
    }


}
