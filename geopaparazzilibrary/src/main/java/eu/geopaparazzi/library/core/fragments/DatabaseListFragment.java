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

package eu.geopaparazzi.library.core.fragments;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.DbCursorAdapter;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.RawSqlCursorLoader;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * A generic database list fragment.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DatabaseListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private DbCursorAdapter mCursorAdapter;
    private static final int LOADER_ID = 1;

    private SQLiteDatabase mDatabase;
    private String mSql;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_databaselist, container, false);

        Bundle arguments = getArguments();
        mSql = arguments.getString(LibraryConstants.PREFS_KEY_QUERY);

        try {
            mDatabase = GPApplication.getInstance().getDatabase();
            if (!mDatabase.isOpen()) {
                mDatabase = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (mDatabase != null && mSql != null) {
                mCursorAdapter = new DbCursorAdapter(getActivity(), null);
                setListAdapter(mCursorAdapter);

                LoaderManager loaderManager = getLoaderManager();
                loaderManager.initLoader(LOADER_ID, null, this);
            }
        } catch (SQLException e) {
            String msg = "An error occurred while launching the query: " + mSql;
            GPLog.error(this, msg, e);
            GPDialogs.warningDialog(getActivity(), msg,
                    new Runnable() {
                        public void run() {
                            getActivity().finish();
                        }
                    });
        }

        return v;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        RawSqlCursorLoader cl = new RawSqlCursorLoader(getActivity(), mDatabase, mSql);
        return cl;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_ID:
                // The asynchronous load is complete and the data
                // is now available for use. Only now can we associate
                // the queried Cursor with the CursorAdapter.
                mCursorAdapter.swapCursor(data);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // For whatever reason, the Loader's data is now unavailable.
        // Remove any references to the old data by replacing it with
        // a null Cursor.
        mCursorAdapter.swapCursor(null);
    }
}
