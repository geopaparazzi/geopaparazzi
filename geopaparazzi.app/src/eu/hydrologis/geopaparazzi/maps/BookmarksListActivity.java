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
package eu.hydrologis.geopaparazzi.maps;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Bookmark;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * Bookmarks listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class BookmarksListActivity extends ListActivity {
    private Bookmark[] bookmarksArray;
    private Comparator<Bookmark> bookmarksSorter = new ItemComparators.BookmarksComparator(true);

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.mapslist);

        refreshList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        Logger.d(this, "refreshing bookmarks list");
        bookmarksArray = new Bookmark[0];
        try {
            List<Bookmark> bookmarksList = DaoBookmarks.getAllBookmarks(this);
            Collections.sort(bookmarksList, bookmarksSorter);
            bookmarksArray = (Bookmark[]) bookmarksList.toArray(new Bookmark[bookmarksList.size()]);
        } catch (IOException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        ArrayAdapter<Bookmark> arrayAdapter = new ArrayAdapter<Bookmark>(this, R.layout.bookmark_row, bookmarksArray);
        setListAdapter(arrayAdapter);
    }

    @Override
    protected void onListItemClick( ListView parent, View v, int position, long id ) {
        MapView mapView = ApplicationManager.getInstance(this).getMapView();
        Bookmark bookmark = bookmarksArray[position];
        mapView.zoomTo((int) bookmark.getZoom());
        mapView.setGotoCoordinate(bookmark.getLon(), bookmark.getLat());
        mapView.invalidate();
        finish();
    }

}
