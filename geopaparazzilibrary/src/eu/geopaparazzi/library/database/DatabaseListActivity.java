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

import android.app.ListActivity;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;

/**
 * A database list activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DatabaseListActivity extends ListActivity {
    private Cursor cursor;

    @SuppressWarnings("nls")
    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.database_list);

        Bundle extras = getIntent().getExtras();
        String query = extras.getString(LibraryConstants.PREFS_KEY_QUERY);

        SQLiteDatabase database = null;
        try {
            database = GPApplication.getInstance().getDatabase();
            if (!database.isOpen()) {
                database = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (database != null && query != null) {
                cursor = database.rawQuery(query, null);
                startManagingCursor(cursor);

                DbCursorAdapter data = new DbCursorAdapter(this, cursor);
                setListAdapter(data);
            }
        } catch (SQLException e) {
            Utilities.messageDialog(this, "An error occurred while launching the query: " + e.getLocalizedMessage(),
                    new Runnable(){
                        public void run() {
                            finish();
                        }
                    });
        }

    }

    @Override
    protected void onDestroy() {
        if (cursor != null) {
            stopManagingCursor(cursor);
            cursor.close();
        }
        super.onDestroy();
    }
}
