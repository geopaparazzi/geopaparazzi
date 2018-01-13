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
package eu.geopaparazzi.core.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TextRunnable;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoBookmarks;
import eu.geopaparazzi.core.database.objects.Bookmark;
import eu.geopaparazzi.core.database.objects.ItemComparators;

/**
 * Bookmarks listing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class BookmarksListActivity extends AppCompatActivity {
    private String[] bookmarksNames;
    private Map<String, Bookmark> bookmarksMap = new HashMap<>();
    private Comparator<Bookmark> bookmarksSorter = new ItemComparators.BookmarksComparator(false);
    private ListView listView;
    private EditText filterText;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_bookmarkslist);

        Toolbar toolbar = findViewById(eu.geopaparazzi.mapsforge.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        listView = findViewById(R.id.bookmarksList);


        refreshList();

        filterText = findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    protected void onDestroy() {
        super.onDestroy();
        filterText.removeTextChangedListener(filterTextWatcher);
    }

    private void refreshList() {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "refreshing bookmarks list"); //$NON-NLS-1$
        try {
            List<Bookmark> bookmarksList = DaoBookmarks.getAllBookmarks();

            Collections.sort(bookmarksList, bookmarksSorter);
            bookmarksNames = new String[bookmarksList.size()];
            bookmarksMap.clear();
            int index = 0;
            for (Bookmark bookmark : bookmarksList) {
                String name = bookmark.getName();
                bookmarksMap.put(name, bookmark);
                bookmarksNames[index] = name;
                index++;
            }
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        redoAdapter();
    }

    private void filterList(String filterText) {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "filter bookmarks list"); //$NON-NLS-1$
        try {
            List<Bookmark> bookmarksList = DaoBookmarks.getAllBookmarks();
            Collections.sort(bookmarksList, bookmarksSorter);

            bookmarksMap.clear();
            filterText = ".*" + filterText.toLowerCase() + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
            List<String> namesList = new ArrayList<>();
            for (Bookmark bookmark : bookmarksList) {
                String name = bookmark.getName();
                String nameLower = name.toLowerCase();
                if (nameLower.matches(filterText)) {
                    namesList.add(name);
                    bookmarksMap.put(name, bookmark);
                }
            }

            bookmarksNames = namesList.toArray(new String[namesList.size()]);
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        redoAdapter();
    }

    private void redoAdapter() {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.activity_bookmarkslist_row, bookmarksNames) {
            @Override
            public View getView(int position, View cView, ViewGroup parent) {
                final View rowView = getLayoutInflater().inflate(R.layout.activity_bookmarkslist_row, parent, false);

                final TextView bookmarkText = rowView.findViewById(R.id.bookmarkrowtext);
                bookmarkText.setText(bookmarksNames[position]);

                final ImageButton renameButton = rowView.findViewById(R.id.renamebutton);
                renameButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        final String name = bookmarkText.getText().toString();
                        String title = getString(R.string.bookmarks_list_rename);

                        GPDialogs.inputMessageDialog(BookmarksListActivity.this, title, name, new TextRunnable() {
                            @Override
                            public void run() {
                                try {
                                    if (theTextToRunOn.length() < 1) {
                                        return;
                                    }
                                    Bookmark bookmark = bookmarksMap.get(name);
                                    DaoBookmarks.updateBookmarkName(bookmark.getId(), theTextToRunOn);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            refreshList();
                                        }
                                    });

                                } catch (final IOException e) {
                                    GPLog.error(this, e.getLocalizedMessage(), e);
                                    e.printStackTrace();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG)
                                                    .show();
                                        }
                                    });

                                }
                            }
                        });


                    }
                });

                final ImageButton deleteButton = rowView.findViewById(R.id.deletebutton);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        final String name = bookmarkText.getText().toString();
                        final Bookmark bookmark = bookmarksMap.get(name);
                        GPDialogs.yesNoMessageDialog(BookmarksListActivity.this, getString(R.string.prompt_delete_bookmark),
                                new Runnable() {
                                    public void run() {
                                        new AsyncTask<String, Void, String>() {
                                            protected String doInBackground(String... params) {
                                                return ""; //$NON-NLS-1$
                                            }

                                            protected void onPostExecute(String response) {
                                                try {
                                                    DaoBookmarks.deleteBookmark(bookmark.getId());
                                                    refreshList();
                                                } catch (IOException e) {
                                                    GPLog.error(this, e.getLocalizedMessage(), e);
                                                    e.printStackTrace();
                                                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        }.execute((String) null);

                                    }
                                }, null);

                    }
                });

                final ImageButton goButton = rowView.findViewById(R.id.gobutton);
                goButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Bookmark bookmark = bookmarksMap.get(bookmarkText.getText().toString());
                        if (bookmark != null) {

                            Intent intent = getIntent();
                            intent.putExtra(LibraryConstants.LATITUDE, bookmark.getLat());
                            intent.putExtra(LibraryConstants.LONGITUDE, bookmark.getLon());
                            intent.putExtra(LibraryConstants.ZOOMLEVEL, (int) bookmark.getZoom());
                            // if (getParent() == null) {
                            setResult(Activity.RESULT_OK, intent);
                        }
                        finish();
                    }
                });

//                final ImageView proximityButton = (ImageView) rowView.findViewById(R.id.alertbutton);
//                proximityButton.setOnClickListener(new View.OnClickListener() {
//                    public void onClick(View v) {
//                        final Bookmark bookmark = bookmarksMap.get(bookmarkText.getText().toString());
//                        if (bookmark != null) {
//
//                            GPDialogs.inputMessageDialog(BookmarksListActivity.this,
//                                    getString(R.string.add_proximity_radius), "100", new TextRunnable() { //$NON-NLS-1$
//                                        public void run() {
//                                            double radius = 100;
//                                            if (theTextToRunOn.length() > 0) {
//                                                try {
//                                                    radius = Double.parseDouble(theTextToRunOn);
//                                                } catch (Exception e) {
//                                                    // ignore and use default
//                                                }
//                                            }
//                                            Context context = getApplicationContext();
//                                            String PROX_ALERT_INTENT = "com.javacodegeeks.android.lbs.ProximityAlert"; //$NON-NLS-1$
//                                            Intent intent = new Intent(PROX_ALERT_INTENT);
//                                            PendingIntent proximityIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
//                                            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//                                            locationManager.addProximityAlert(bookmark.getLat(), bookmark.getLon(),
//                                                    (float) radius, -1, proximityIntent);
//                                            IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
//                                            context.registerReceiver(new ProximityIntentReceiver(), filter);
//                                            finish();
//                                        }
//                                    });
//                        }
//                    }
//                });

                return rowView;
            }

        };

        listView.setAdapter(arrayAdapter);
    }

    private TextWatcher filterTextWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
            // ignore
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // ignore
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // arrayAdapter.getFilter().filter(s);
            filterList(s.toString());
        }
    };


}
