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
package eu.geopaparazzi.library.database;

import java.io.IOException;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * A database list activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DatabaseListActivity extends ListActivity {
    @SuppressWarnings("nls")
    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.database_list);

        Bundle extras = getIntent().getExtras();
        String query = extras.getString(LibraryConstants.PREFS_KEY_QUERY);
        String[] queryFields = extras.getStringArray(LibraryConstants.PREFS_KEY_QUERYFIELDS);

        SQLiteDatabase database = null;
        try {
            database = ADbHelper.getInstance().getDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (database != null && query != null && queryFields != null) {
            Cursor cursor = database.rawQuery(query, null);
            startManagingCursor(cursor);

            int[] viewsArray = new int[queryFields.length];
            for( int i = 0; i < viewsArray.length; i++ ) {
                viewsArray[i] = R.id.database_row_text;
            }

            SimpleCursorAdapter data = new SimpleCursorAdapter(this, R.layout.database_list_row, cursor, queryFields, viewsArray);
            setListAdapter(data);
        }

    }
}
