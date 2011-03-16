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
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.gps.GpsManager;
import eu.hydrologis.geopaparazzi.sensors.SensorsManager;
import eu.hydrologis.geopaparazzi.util.Bookmark;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Note;
import eu.hydrologis.geopaparazzi.util.VerticalSeekBar;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsActivity extends Activity {
    private static final int MENU_GPSDATA = 1;
    private static final int MENU_MAPDATA = 2;
    private static final int MENU_DOWNLOADMAPS = 3;
    private static final int GO_TO = 4;

    private MapView mapsView;

    private DecimalFormat formatter = new DecimalFormat("00");
    private Button zoomInButton;
    private Button zoomOutButton;
    private VerticalSeekBar zoomBar;
    private SlidingDrawer slidingDrawer;
    private boolean sliderIsOpen;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.mapsview);

        GpsManager gpsManager = GpsManager.getInstance(this);

        // requestWindowFeature(Window.FEATURE_PROGRESS);
        mapsView = (MapView) findViewById(R.id.osmviewid);
        ViewportManager.INSTANCE.setMapActivity(this);
        gpsManager.addListener(mapsView);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // set zoom preferences
        final int zoomLevel1 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM1, "14"));
        final int zoomLevel2 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM2, "16"));
        final int zoomLevelLabelLength1 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM1_LABELLENGTH, "4"));
        final int zoomLevelLabelLength2 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM2_LABELLENGTH, "-1"));
        mapsView.setZoomLabelsParams(zoomLevel1, zoomLevelLabelLength1, zoomLevel2, zoomLevelLabelLength2);

        // zoom bar
        final int zoom = preferences.getInt(Constants.PREFS_KEY_ZOOM, 16);
        zoomBar = (VerticalSeekBar) findViewById(R.id.ZoomBar);
        zoomBar.setMax(18);
        zoomBar.setProgress(zoom);
        mapsView.setZoom(zoom);
        zoomBar.setOnSeekBarChangeListener(new VerticalSeekBar.OnSeekBarChangeListener(){
            private int progress = zoom;
            public void onStopTrackingTouch( VerticalSeekBar seekBar ) {
                setNewZoom(progress, false);
                inalidateMap();
                Logger.d(this, "Zoomed to: " + progress);
            }

            public void onStartTrackingTouch( VerticalSeekBar seekBar ) {

            }

            public void onProgressChanged( VerticalSeekBar seekBar, int progress, boolean fromUser ) {
                this.progress = progress;
                setNewZoom(progress, true);
            }
        });

        int zoomInLevel = zoom + 1;
        if (zoomInLevel > 18) {
            zoomInLevel = 18;
        }
        int zoomOutLevel = zoom - 1;
        if (zoomOutLevel < 0) {
            zoomOutLevel = 0;
        }
        zoomInButton = (Button) findViewById(R.id.zoomin);
        zoomInButton.setText(formatter.format(zoomInLevel));
        zoomInButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                String text = zoomInButton.getText().toString();
                int newZoom = Integer.parseInt(text);
                setNewZoom(newZoom, false);
                inalidateMap();

            }
        });
        zoomOutButton = (Button) findViewById(R.id.zoomout);
        zoomOutButton.setText(formatter.format(zoomOutLevel));
        zoomOutButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                String text = zoomOutButton.getText().toString();
                int newZoom = Integer.parseInt(text);
                setNewZoom(newZoom, false);
                inalidateMap();

            }
        });

        // button view
        ImageButton centerOnGps = (ImageButton) findViewById(R.id.center_on_gps_btn);

        centerOnGps.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                mapsView.centerOnGps();
            }
        });

        // slidingdrawer
        final int slidingId = R.id.mapslide;
        slidingDrawer = (SlidingDrawer) findViewById(slidingId);
        final ImageView slideHandleButton = (ImageView) findViewById(R.id.mapslidehandle);

        slidingDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener(){
            public void onDrawerOpened() {
                Logger.d(this, "Enable drawing");
                sliderIsOpen = true;
                slideHandleButton.setBackgroundResource(R.drawable.min);
                startDrawingAgain();
            }
        });
        slidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener(){
            public void onDrawerClosed() {
                Logger.d(this, "Enable drawing");
                slideHandleButton.setBackgroundResource(R.drawable.max);
                sliderIsOpen = false;
                startDrawingAgain();
            }

        });

        slidingDrawer.setOnDrawerScrollListener(new SlidingDrawer.OnDrawerScrollListener(){
            public void onScrollEnded() {
                Logger.d(this, "Scroll End Disable drawing");
                mapsView.enableDrawing(false);
            }

            public void onScrollStarted() {
                Logger.d(this, "Scroll Start Disable drawing");
                mapsView.enableDrawing(false);
            }
        });

        /*
         * tool buttons
         */
        ImageButton addnotebytagButton = (ImageButton) findViewById(R.id.addnotebytagbutton);
        addnotebytagButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Intent osmTagsIntent = new Intent(Constants.TAGS);
                osmTagsIntent.putExtra(Constants.VIEW_CENTER_LAT, mapsView.getCenterLat());
                osmTagsIntent.putExtra(Constants.VIEW_CENTER_LON, mapsView.getCenterLon());
                startActivity(osmTagsIntent);
            }
        });

        ImageButton addBookmarkButton = (ImageButton) findViewById(R.id.addbookmarkbutton);
        addBookmarkButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                addBookmark();
            }
        });

        ImageButton removeNotesButton = (ImageButton) findViewById(R.id.removenotesbutton);
        removeNotesButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                deleteVisibleNotes();
            }
        });

        ImageButton removeBookmarksButton = (ImageButton) findViewById(R.id.removebookmarkbutton);
        removeBookmarksButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                deleteVisibleBookmarks();
            }
        });

        ImageButton listBookmarksButton = (ImageButton) findViewById(R.id.bookmarkslistbutton);
        listBookmarksButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Intent intent = new Intent(MapsActivity.this, BookmarksListActivity.class);
                startActivity(intent);
            }
        });

        final ImageButton toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
        toggleMeasuremodeButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                boolean isInMeasureMode = mapsView.isMeasureMode();
                mapsView.setMeasureMode(!isInMeasureMode);
                if (!isInMeasureMode) {
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.measuremode_on);
                } else {
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.measuremode);
                }
            }
        });

        mapsView.invalidate();
    }

    public void setNewZoom( int newZoom, boolean onlyText ) {
        int zoomInLevel = newZoom + 1;
        if (zoomInLevel > 18) {
            zoomInLevel = 18;
        }
        int zoomOutLevel = newZoom - 1;
        if (zoomOutLevel < 0) {
            zoomOutLevel = 0;
        }
        zoomInButton.setText(formatter.format(zoomInLevel));
        zoomOutButton.setText(formatter.format(zoomOutLevel));
        zoomBar.setProgress(newZoom);

        if (!onlyText) {
            mapsView.setZoom(newZoom);
        }
    }

    public void setNewCenter( double lon, double lat, boolean drawIcon ) {
        mapsView.setCenter(lon, lat, drawIcon);
    }

    public double[] getCenterLonLat() {
        double[] lonLat = {mapsView.getCenterLon(), mapsView.getCenterLat()};
        return lonLat;
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_GPSDATA, 1, R.string.mainmenu_gpsdataselect).setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_MAPDATA, 2, R.string.mainmenu_mapdataselect).setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.CATEGORY_SECONDARY, GO_TO, 3, R.string.goto_coordinate).setIcon(android.R.drawable.ic_menu_myplaces);
        menu.add(Menu.CATEGORY_SECONDARY, MENU_DOWNLOADMAPS, 4, R.string.menu_download_maps).setIcon(
                android.R.drawable.ic_menu_mapmode);
        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MENU_GPSDATA:
            Intent gpsDatalistIntent = new Intent(Constants.GPSLOG_DATALIST);
            startActivity(gpsDatalistIntent);
            return true;

        case MENU_MAPDATA:
            try {
                List<MapItem> mapsList = DaoMaps.getMaps(this);
                int mapsNum = mapsList.size();
                if (mapsNum < 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.no_maps_in_list).setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                                public void onClick( DialogInterface dialog, int id ) {
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    return true;
                } else {
                    Intent mapDatalistIntent = new Intent(Constants.MAPSDATALIST);
                    startActivity(mapDatalistIntent);
                    return true;
                }
            } catch (IOException e1) {
                Logger.e(this, e1.getLocalizedMessage(), e1);
                e1.printStackTrace();
                return false;
            }
        case GO_TO: {
            Intent intent = new Intent(Constants.INSERT_COORD);
            startActivity(intent);
            return true;
        }
        case MENU_DOWNLOADMAPS:
            final SensorsManager sensorsManager = SensorsManager.getInstance(this);
            boolean isInternetOn = sensorsManager.isInternetOn();
            if (isInternetOn) {
                float screenNorth = mapsView.getScreenNorth();
                float screenSouth = mapsView.getScreenSouth();
                float screenWest = mapsView.getScreenWest();
                float screenEast = mapsView.getScreenEast();

                float[] nsew = new float[]{screenNorth, screenSouth, screenEast, screenWest};
                Intent downloadIntent = new Intent(this, MapDownloadActivity.class);
                downloadIntent.putExtra(Constants.NSEW_COORDS, nsew);
                startActivity(downloadIntent);
            } else {
                runOnUiThread(new Runnable(){
                    public void run() {
                        String msg = getResources().getString(R.string.no_internet_warning);
                        String ok = getResources().getString(R.string.ok);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                        builder.setMessage(msg).setCancelable(false).setPositiveButton(ok, new DialogInterface.OnClickListener(){
                            public void onClick( DialogInterface dialog, int id ) {
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                });
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void deleteVisibleBookmarks() {

        new AlertDialog.Builder(this).setTitle(R.string.delete_visible_bookmarks_title)
                .setMessage(R.string.delete_visible_bookmarks_prompt).setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                    }
                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                        try {
                            float n = mapsView.getScreenNorth();
                            float s = mapsView.getScreenSouth();
                            float w = mapsView.getScreenWest();
                            float e = mapsView.getScreenEast();
                            final List<Bookmark> bookmarksInBounds = DaoBookmarks.getBookmarksInWorldBounds(MapsActivity.this, n,
                                    s, w, e);
                            int bookmarksNum = bookmarksInBounds.size();

                            bookmarksRemoveDialog = new ProgressDialog(MapsActivity.this);
                            bookmarksRemoveDialog.setCancelable(true);
                            bookmarksRemoveDialog.setMessage(MessageFormat.format("Deleting {0} bookmarks...", bookmarksNum));
                            bookmarksRemoveDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            bookmarksRemoveDialog.setProgress(0);
                            bookmarksRemoveDialog.setMax(bookmarksNum);
                            bookmarksRemoveDialog.show();

                            new Thread(){
                                public void run() {
                                    for( final Bookmark bookmark : bookmarksInBounds ) {
                                        bookmarksRemoveHandler.sendEmptyMessage(0);
                                        try {
                                            DaoBookmarks.deleteBookmark(MapsActivity.this, bookmark.getId());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    bookmarksDeleted = true;
                                    bookmarksRemoveHandler.sendEmptyMessage(0);
                                }
                            }.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).show();

    }

    private void deleteVisibleNotes() {

        new AlertDialog.Builder(this).setTitle(R.string.delete_visible_notes_title)
                .setMessage(R.string.delete_visible_notes_prompt).setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                    }
                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                        try {
                            float n = mapsView.getScreenNorth();
                            float s = mapsView.getScreenSouth();
                            float w = mapsView.getScreenWest();
                            float e = mapsView.getScreenEast();
                            final List<Note> notesInBounds = DaoNotes.getNotesInWorldBounds(MapsActivity.this, n, s, w, e);

                            int notesNum = notesInBounds.size();

                            notesRemoveDialog = new ProgressDialog(MapsActivity.this);
                            notesRemoveDialog.setCancelable(true);
                            notesRemoveDialog.setMessage(MessageFormat.format("Deleting {0} notes...", notesNum));
                            notesRemoveDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            notesRemoveDialog.setProgress(0);
                            notesRemoveDialog.setMax(notesNum);
                            notesRemoveDialog.show();

                            new Thread(){
                                public void run() {
                                    for( Note note : notesInBounds ) {
                                        notesRemoveHandler.sendEmptyMessage(0);
                                        try {
                                            DaoNotes.deleteNote(MapsActivity.this, note.getId());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    notesDeleted = true;
                                    notesRemoveHandler.sendEmptyMessage(0);
                                }
                            }.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).show();

    }

    private void addBookmark() {
        final float centerLat = mapsView.getCenterLat();
        final float centerLon = mapsView.getCenterLon();
        final EditText input = new EditText(this);
        final String newDate = Constants.TIME_FORMATTER.format(new Date());
        final String proposedName = "bookmark " + newDate;
        input.setText(proposedName);
        Builder builder = new AlertDialog.Builder(this).setTitle("New Bookmark");
        builder.setMessage("Enter a name for the new bookmark (optional)");
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
                                newName = proposedName;;
                            }

                            int zoom = mapsView.getZoom();
                            float n = mapsView.getScreenNorth();
                            float s = mapsView.getScreenSouth();
                            float w = mapsView.getScreenWest();
                            float e = mapsView.getScreenEast();
                            DaoBookmarks.addBookmark(getApplicationContext(), centerLon, centerLat, newName, zoom, n, s, w, e);
                            mapsView.invalidate();
                        } catch (IOException e) {
                            Logger.e(this, e.getLocalizedMessage(), e);
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }).setCancelable(false).show();
    }

    private boolean bookmarksDeleted = false;
    private ProgressDialog bookmarksRemoveDialog;
    private Handler bookmarksRemoveHandler = new Handler(){
        public void handleMessage( Message msg ) {
            if (!bookmarksDeleted) {
                bookmarksRemoveDialog.incrementProgressBy(1);
            } else {
                bookmarksRemoveDialog.dismiss();
                mapsView.invalidate();
            }
        }
    };

    private boolean notesDeleted = false;
    private ProgressDialog notesRemoveDialog;
    private Handler notesRemoveHandler = new Handler(){
        public void handleMessage( Message msg ) {
            if (!notesDeleted) {
                notesRemoveDialog.incrementProgressBy(1);
            } else {
                notesRemoveDialog.dismiss();
                mapsView.invalidate();
            }
        }
    };

    public void inalidateMap() {
        mapsView.invalidate();
    }

    public boolean onKeyDown( int keyCode, KeyEvent event ) {
        // force to exit through the exit button
        if (keyCode == KeyEvent.KEYCODE_BACK && sliderIsOpen) {
            slidingDrawer.animateClose();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void startDrawingAgain() {
        new Thread(new Runnable(){
            public void run() {
                runOnUiThread(new Runnable(){
                    public void run() {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mapsView.enableDrawing(true);
                        inalidateMap();
                    }
                });
            }
        }).start();
    }
}
