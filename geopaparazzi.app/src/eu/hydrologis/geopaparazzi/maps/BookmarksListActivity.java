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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.util.Bookmark;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * Bookmarks listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class BookmarksListActivity extends ListActivity {
    private String[] bookmarksNames;
    private Map<String, Bookmark> bookmarksMap = new HashMap<String, Bookmark>();
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
        try {
            List<Bookmark> bookmarksList = DaoBookmarks.getAllBookmarks(this);

            Collections.sort(bookmarksList, bookmarksSorter);
            bookmarksNames = new String[bookmarksList.size()];
            bookmarksMap.clear();
            int index = 0;
            for( Bookmark bookmark : bookmarksList ) {
                String name = bookmark.getName();
                bookmarksMap.put(name, bookmark);
                bookmarksNames[index] = name;
                index++;
            }
            if (bookmarksList.size() == 0) {
                bookmarksNames = new String[]{"No bookmarks available"};
            }
        } catch (IOException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.bookmark_row, bookmarksNames){
            @Override
            public View getView( int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.bookmark_row, null);

                final TextView bookmarkText = (TextView) rowView.findViewById(R.id.bookmarkrow);
                bookmarkText.setText(bookmarksNames[position]);
                final Button renameButton = (Button) rowView.findViewById(R.id.renamebutton);
                renameButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        final String name = bookmarkText.getText().toString();
                        final EditText input = new EditText(BookmarksListActivity.this);
                        input.setText(name);
                        Builder builder = new AlertDialog.Builder(BookmarksListActivity.this).setTitle("Rename Bookmark");
                        builder.setMessage("Rename the bookmark");
                        builder.setView(input);
                        builder.setIcon(android.R.drawable.ic_dialog_info)
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                                    public void onClick( DialogInterface dialog, int whichButton ) {
                                    }
                                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                                    public void onClick( DialogInterface dialog, int whichButton ) {
                                        try {
                                            Editable value = input.getText();
                                            String newName = value.toString();
                                            if (newName == null || newName.length() < 1) {
                                                return;
                                            }
                                            Bookmark bookmark = bookmarksMap.get(name);
                                            DaoBookmarks.updateBookmarkName(BookmarksListActivity.this, bookmark.getId(), newName);
                                            refreshList();
                                        } catch (IOException e) {
                                            Logger.e(this, e.getLocalizedMessage(), e);
                                            e.printStackTrace();
                                            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG)
                                                    .show();
                                        }
                                    }
                                }).setCancelable(false).show();
                    }
                });

                bookmarkText.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        ViewportManager viewportManager = ViewportManager.INSTANCE;
                        Bookmark bookmark = bookmarksMap.get(bookmarkText.getText().toString());
                        if (bookmark != null) {
                            viewportManager.setZoomTo((int) bookmark.getZoom());
                            viewportManager.setCenterTo(bookmark.getLon(), bookmark.getLat(), false);
                            viewportManager.invalidateMap();
                        }
                        finish();
                    }
                });

                return rowView;
            }

        };
        setListAdapter(arrayAdapter);
    }

    @Override
    protected void onListItemClick( ListView parent, View v, int position, long id ) {
        ViewportManager viewportManager = ViewportManager.INSTANCE;

        String bookmarkName = bookmarksNames[position];
        Bookmark bookmark = bookmarksMap.get(bookmarkName);
        if (bookmark != null) {
            viewportManager.setZoomTo((int) bookmark.getZoom());
            viewportManager.setCenterTo(bookmark.getLon(), bookmark.getLat(), false);
            viewportManager.invalidateMap();
        }
        finish();
    }

}
