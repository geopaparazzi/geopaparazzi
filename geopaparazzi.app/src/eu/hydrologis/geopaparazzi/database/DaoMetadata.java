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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;

import static eu.hydrologis.geopaparazzi.database.TableDescriptions.*;
import static eu.hydrologis.geopaparazzi.database.TableDescriptions.TABLE_METADATA;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoMetadata {

    public static final String EMPTY_VALUE = " - ";

    /**
     * Create the notes tables.
     *
     * @throws java.io.IOException if something goes wrong.
     */
    public static void createTables() throws IOException {
        StringBuilder sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_METADATA);
        sB.append(" (");
        sB.append(MetadataTableFields.COLUMN_KEY.getFieldName()).append(" TEXT NOT NULL, ");
        sB.append(MetadataTableFields.COLUMN_VALUE.getFieldName()).append(" TEXT NOT NULL ");
        sB.append(");");
        String CREATE_TABLE_PROJECT = sB.toString();

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        if (GPLog.LOG_HEAVY)
            Log.i("DaoProject", "Create the project table with: \n" + CREATE_TABLE_PROJECT);

        sqliteDatabase.beginTransaction();
        try {
            sqliteDatabase.execSQL(CREATE_TABLE_PROJECT);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DaoProject", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }


    /**
     * Populate the project metadata table.
     *
     * @param name         the project name
     * @param description  an optional description.
     * @param notes        optional notes.
     * @param creationUser the user creating the project.
     * @throws java.io.IOException if something goes wrong.
     */
    public static void initProjectMetadata(String name, String description, String notes, String creationUser) throws IOException {
        Date creationDate = new Date();
        if (name == null) {
            name = "project-" + TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(creationDate);
        }
        if (description == null) {
            description = EMPTY_VALUE;
        }
        if (notes == null) {
            notes = EMPTY_VALUE;
        }
        if (creationUser == null) {
            creationUser = "dummy user";
        }

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(MetadataTableFields.COLUMN_KEY.getFieldName(), MetadataTableFields.KEY_NAME.getFieldName());
            values.put(MetadataTableFields.COLUMN_VALUE.getFieldName(), name);
            sqliteDatabase.insertOrThrow(TABLE_METADATA, null, values);

            values = new ContentValues();
            values.put(MetadataTableFields.COLUMN_KEY.getFieldName(), MetadataTableFields.KEY_DESCRIPTION.getFieldName());
            values.put(MetadataTableFields.COLUMN_VALUE.getFieldName(), description);
            sqliteDatabase.insertOrThrow(TABLE_METADATA, null, values);

            values = new ContentValues();
            values.put(MetadataTableFields.COLUMN_KEY.getFieldName(), MetadataTableFields.KEY_NOTES.getFieldName());
            values.put(MetadataTableFields.COLUMN_VALUE.getFieldName(), notes);
            sqliteDatabase.insertOrThrow(TABLE_METADATA, null, values);

            values = new ContentValues();
            values.put(MetadataTableFields.COLUMN_KEY.getFieldName(), MetadataTableFields.KEY_CREATIONTS.getFieldName());
            values.put(MetadataTableFields.COLUMN_VALUE.getFieldName(), String.valueOf(creationDate.getTime()));
            sqliteDatabase.insertOrThrow(TABLE_METADATA, null, values);

            values = new ContentValues();
            values.put(MetadataTableFields.COLUMN_KEY.getFieldName(), MetadataTableFields.KEY_LASTTS.getFieldName());
            values.put(MetadataTableFields.COLUMN_VALUE.getFieldName(), EMPTY_VALUE);
            sqliteDatabase.insertOrThrow(TABLE_METADATA, null, values);

            values = new ContentValues();
            values.put(MetadataTableFields.COLUMN_KEY.getFieldName(), MetadataTableFields.KEY_CREATIONUSER.getFieldName());
            values.put(MetadataTableFields.COLUMN_VALUE.getFieldName(), creationUser);
            sqliteDatabase.insertOrThrow(TABLE_METADATA, null, values);

            values = new ContentValues();
            values.put(MetadataTableFields.COLUMN_KEY.getFieldName(), MetadataTableFields.KEY_LASTUSER.getFieldName());
            values.put(MetadataTableFields.COLUMN_VALUE.getFieldName(), EMPTY_VALUE);
            sqliteDatabase.insertOrThrow(TABLE_METADATA, null, values);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DaoProject", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Set a value of the metadata.
     *
     * @param key   the key to use (from {@link eu.hydrologis.geopaparazzi.database.TableDescriptions.MetadataTableFields}).
     * @param value the value to set.
     * @throws java.io.IOException if something goes wrong.
     */
    public static void setValue(String key, String value) throws IOException {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(MetadataTableFields.COLUMN_VALUE.getFieldName(), value);

        String where = MetadataTableFields.COLUMN_KEY.getFieldName() + "='" + key + "'";

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.update(TABLE_METADATA, updatedValues, where, null);
    }

    /**
     * Get the metadata.
     *
     * @return the map of metadata.
     * @throws java.io.IOException if something goes wrong.
     */
    public static HashMap<String, String> getProjectMetadata() throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        HashMap<String, String> metadata = new HashMap<String, String>();

        String asColumnsToReturn[] = { //
                MetadataTableFields.COLUMN_KEY.getFieldName(), //
                MetadataTableFields.COLUMN_VALUE.getFieldName()
        };// ,
        Cursor c = sqliteDatabase.query(TABLE_METADATA, asColumnsToReturn, null, null, null, null, null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            String key = c.getString(0);
            String value = c.getString(1);

            metadata.put(key, value);

            c.moveToNext();
        }
        c.close();
        return metadata;
    }


}
