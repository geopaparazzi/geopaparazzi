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

import static eu.hydrologis.geopaparazzi.util.Constants.E6;
import static eu.hydrologis.geopaparazzi.util.Constants.PREFS_KEY_LAT;
import static eu.hydrologis.geopaparazzi.util.Constants.PREFS_KEY_LON;
import static eu.hydrologis.geopaparazzi.util.Constants.PREFS_KEY_MINIMAPON;
import static eu.hydrologis.geopaparazzi.util.Constants.PREFS_KEY_SCALEBARON;
import static eu.hydrologis.geopaparazzi.util.Constants.PREFS_KEY_ZOOM;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.osmdroid.ResourceProxy;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SlidingDrawer;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.gps.GpsManager;
import eu.hydrologis.geopaparazzi.gps.GpsManagerListener;
import eu.hydrologis.geopaparazzi.maps.overlays.BookmarksOverlay;
import eu.hydrologis.geopaparazzi.maps.overlays.CrossOverlay;
import eu.hydrologis.geopaparazzi.maps.overlays.GpsPositionOverlay;
import eu.hydrologis.geopaparazzi.maps.overlays.LogsOverlay;
import eu.hydrologis.geopaparazzi.maps.overlays.MapsOverlay;
import eu.hydrologis.geopaparazzi.maps.overlays.MeasureToolOverlay;
import eu.hydrologis.geopaparazzi.maps.overlays.NotesOverlay;
import eu.hydrologis.geopaparazzi.util.Bookmark;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Note;
import eu.hydrologis.geopaparazzi.util.ResourceProxyImpl;
import eu.hydrologis.geopaparazzi.util.VerticalSeekBar;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsActivity extends Activity implements GpsManagerListener, MapListener {
    private static final int MENU_GPSDATA = 1;
    private static final int MENU_MAPDATA = 2;
    private static final int MENU_TILE_SOURCE_ID = 3;
    private static final int MENU_MINIMAP_ID = 4;
    private static final int MENU_SCALE_ID = 5;
    private static final int GO_TO = 6;

    private DecimalFormat formatter = new DecimalFormat("00"); //$NON-NLS-1$
    private Button zoomInButton;
    private Button zoomOutButton;
    private VerticalSeekBar zoomBar;
    private SlidingDrawer slidingDrawer;
    private boolean sliderIsOpen;
    private MapView mapsView;
    private MapController mapController;
    private ResourceProxy mResourceProxy;
    private ScaleBarOverlay mScaleBarOverlay;
    private MinimapOverlay mMiniMapOverlay;
    private LogsOverlay mLogsOverlay;
    private NotesOverlay mNotesOverlay;
    private BookmarksOverlay mBookmarksOverlay;
    private GpsPositionOverlay mGpsOverlay;
    private CrossOverlay mCrossOverlay;
    private MapsOverlay mMapsOverlay;
    private MeasureToolOverlay mMeasureOverlay;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.mapsview);

        mResourceProxy = new ResourceProxyImpl(getApplicationContext());

        mapsView = new MapView(this, 256);
        mapController = mapsView.getController();
        mapsView.setMapListener(this);
        mapsView.setMultiTouchControls(true);
        final RelativeLayout rl = (RelativeLayout) findViewById(R.id.innerlayout);
        rl.addView(this.mapsView, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        ViewportManager.INSTANCE.setMapActivity(this);

        GpsManager.getInstance(this).addListener(this);

        /* imported maps */
        {
            mMapsOverlay = new MapsOverlay(this, mResourceProxy);
            this.mapsView.getOverlays().add(mMapsOverlay);
        }

        /* gps logs */
        {
            mLogsOverlay = new LogsOverlay(this, mResourceProxy);
            this.mapsView.getOverlays().add(mLogsOverlay);
        }

        /* gps notes */
        {
            mNotesOverlay = new NotesOverlay(this, mResourceProxy);
            this.mapsView.getOverlays().add(mNotesOverlay);
        }

        /* bookmarks */
        {
            mBookmarksOverlay = new BookmarksOverlay(this, mResourceProxy);
            this.mapsView.getOverlays().add(mBookmarksOverlay);
        }

        /* gps position */
        {
            mGpsOverlay = new GpsPositionOverlay(this, mResourceProxy);
            this.mapsView.getOverlays().add(mGpsOverlay);
        }

        /* cross */
        {
            mCrossOverlay = new CrossOverlay(this, mResourceProxy);
            this.mapsView.getOverlays().add(mCrossOverlay);
        }

        /* measure tool */
        {
            mMeasureOverlay = new MeasureToolOverlay(this, mResourceProxy);
            this.mapsView.getOverlays().add(mMeasureOverlay);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        /* Scale Bar Overlay */
        {
            mScaleBarOverlay = new ScaleBarOverlay(this, mResourceProxy);
            mapsView.getOverlays().add(mScaleBarOverlay);
            // Scale bar tries to draw as 1-inch, so to put it in the top center, set x offset to
            // half screen width, minus half an inch.
            mScaleBarOverlay.setScaleBarOffset(getResources().getDisplayMetrics().widthPixels / 2
                    - getResources().getDisplayMetrics().xdpi / 2, 10);
            boolean isScalebaron = preferences.getBoolean(PREFS_KEY_SCALEBARON, false);
            mScaleBarOverlay.setEnabled(isScalebaron);
        }

        /* MiniMap */
        {
            mMiniMapOverlay = new MinimapOverlay(this, mapsView.getTileRequestCompleteHandler());
            mapsView.getOverlays().add(mMiniMapOverlay);
            Display display = getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int padding = (int) Math.floor(width * 0.05);
            width = width - padding;
            int half = (int) Math.floor(width / 2.0);
            mMiniMapOverlay.setHeight(half);
            mMiniMapOverlay.setWidth(half);
            mMiniMapOverlay.setPadding(padding);
            boolean isMinimapon = preferences.getBoolean(PREFS_KEY_MINIMAPON, false);
            mMiniMapOverlay.setEnabled(isMinimapon);
        }

        float lastCenterLon = preferences.getFloat(PREFS_KEY_LON, 0f);
        float lastCenterLat = preferences.getFloat(PREFS_KEY_LAT, 0f);
        final int zoom = preferences.getInt(Constants.PREFS_KEY_ZOOM, 16);
        mapController.setCenter(new GeoPoint(lastCenterLat, lastCenterLon));
        mapController.setZoom(zoom);

        maxZoomLevel = mapsView.getMaxZoomLevel();
        minZoomLevel = mapsView.getMinZoomLevel();

        // zoom bar
        zoomBar = (VerticalSeekBar) findViewById(R.id.ZoomBar);
        zoomBar.setMax(maxZoomLevel);
        zoomBar.setProgress(zoom);
        zoomBar.setOnSeekBarChangeListener(new VerticalSeekBar.OnSeekBarChangeListener(){
            private int progress = zoom;
            public void onStopTrackingTouch( VerticalSeekBar seekBar ) {
                setZoomGuiText(progress);
                mapsView.getController().setZoom(progress);
                inalidateMap();
                //                Logger.d(this, "Zoomed to: " + progress); //$NON-NLS-1$
            }

            public void onStartTrackingTouch( VerticalSeekBar seekBar ) {

            }

            public void onProgressChanged( VerticalSeekBar seekBar, int progress, boolean fromUser ) {
                this.progress = progress;
                setZoomGuiText(progress);
            }
        });

        int zoomInLevel = zoom + 1;
        if (zoomInLevel > maxZoomLevel) {
            zoomInLevel = maxZoomLevel;
        }
        int zoomOutLevel = zoom - 1;
        if (zoomOutLevel < minZoomLevel) {
            zoomOutLevel = minZoomLevel;
        }
        zoomInButton = (Button) findViewById(R.id.zoomin);
        zoomInButton.setText(formatter.format(zoomInLevel));
        zoomInButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                String text = zoomInButton.getText().toString();
                int newZoom = Integer.parseInt(text);
                setZoomGuiText(newZoom);
                mapsView.getController().setZoom(newZoom);
                inalidateMap();
            }
        });
        zoomOutButton = (Button) findViewById(R.id.zoomout);
        zoomOutButton.setText(formatter.format(zoomOutLevel));
        zoomOutButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                String text = zoomOutButton.getText().toString();
                int newZoom = Integer.parseInt(text);
                setZoomGuiText(newZoom);
                mapsView.getController().setZoom(newZoom);
                inalidateMap();
            }
        });

        // center on gps button
        ImageButton centerOnGps = (ImageButton) findViewById(R.id.center_on_gps_btn);
        centerOnGps.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                GpsLocation location = GpsManager.getInstance(MapsActivity.this).getLocation();
                if (location != null) {
                    setNewCenter(location.getLongitude(), location.getLatitude(), false);
                }
            }
        });

        // slidingdrawer
        final int slidingId = R.id.mapslide;
        slidingDrawer = (SlidingDrawer) findViewById(slidingId);
        final ImageView slideHandleButton = (ImageView) findViewById(R.id.mapslidehandle);

        slidingDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener(){
            public void onDrawerOpened() {
                // Logger.d(this, "Enable drawing");
                sliderIsOpen = true;
                slideHandleButton.setBackgroundResource(R.drawable.min);
                enableDrawingWithDelay();
            }
        });
        slidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener(){
            public void onDrawerClosed() {
                // Logger.d(this, "Enable drawing");
                slideHandleButton.setBackgroundResource(R.drawable.max);
                sliderIsOpen = false;
                enableDrawingWithDelay();
            }

        });

        slidingDrawer.setOnDrawerScrollListener(new SlidingDrawer.OnDrawerScrollListener(){
            public void onScrollEnded() {
                // Logger.d(this, "Scroll End Disable drawing");
                disableDrawing();
            }

            public void onScrollStarted() {
                // Logger.d(this, "Scroll Start Disable drawing");
                disableDrawing();
            }
        });

        /*
        * tool buttons
        */
        ImageButton addnotebytagButton = (ImageButton) findViewById(R.id.addnotebytagbutton);
        addnotebytagButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                GeoPoint mapCenter = mapsView.getMapCenter();
                Intent osmTagsIntent = new Intent(Constants.TAGS);
                osmTagsIntent.putExtra(Constants.VIEW_CENTER_LAT, mapCenter.getLatitudeE6() / E6);
                osmTagsIntent.putExtra(Constants.VIEW_CENTER_LON, mapCenter.getLongitudeE6() / E6);
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
                boolean isInMeasureMode = mMeasureOverlay.isInMeasureMode();
                mMeasureOverlay.setMeasureMode(!isInMeasureMode);
                if (!isInMeasureMode) {
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.measuremode_on);
                } else {
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.measuremode);
                }
                if (!isInMeasureMode) {
                    disableDrawing();
                } else {
                    enableDrawingWithDelay();
                }
            }
        });

    }

    @Override
    public void onWindowFocusChanged( boolean hasFocus ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!hasFocus) {
            GeoPoint mapCenter = mapsView.getMapCenter();
            Editor editor = preferences.edit();
            editor.putFloat(PREFS_KEY_LON, (float) (mapCenter.getLongitudeE6() / E6));
            editor.putFloat(PREFS_KEY_LAT, (float) (mapCenter.getLatitudeE6() / E6));
            editor.putInt(PREFS_KEY_ZOOM, mapsView.getZoomLevel());
            editor.commit();
        } else {
            float lastCenterLon = preferences.getFloat(PREFS_KEY_LON, 0f);
            float lastCenterLat = preferences.getFloat(PREFS_KEY_LAT, 0f);
            final int zoom = preferences.getInt(PREFS_KEY_ZOOM, 16);
            mapController.setCenter(new GeoPoint(lastCenterLat, lastCenterLon));
            mapController.setZoom(zoom);
            setZoomGuiText(zoom);
        }
        super.onWindowFocusChanged(hasFocus);
    }

    // @Override
    // protected void onPause() {
    // GeoPoint mapCenter = mapsView.getMapCenter();
    // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    // Editor editor = preferences.edit();
    // editor.putFloat(GPSLAST_LONGITUDE, mapCenter.getLongitudeE6() / E6);
    // editor.putFloat(GPSLAST_LATITUDE, mapCenter.getLatitudeE6() / E6);
    // editor.commit();
    // super.onPause();
    // }

    public MapController getMapController() {
        return mapController;
    }

    public MapView getMapsView() {
        return mapsView;
    }

    private void setZoomGuiText( int newZoom ) {
        int zoomInLevel = newZoom + 1;
        if (zoomInLevel > maxZoomLevel) {
            zoomInLevel = maxZoomLevel;
        }
        int zoomOutLevel = newZoom - 1;
        if (zoomOutLevel < minZoomLevel) {
            zoomOutLevel = minZoomLevel;
        }
        zoomInButton.setText(formatter.format(zoomInLevel));
        zoomOutButton.setText(formatter.format(zoomOutLevel));
        zoomBar.setProgress(newZoom);
    }

    public void setNewCenter( double lon, double lat, boolean drawIcon ) {
        mapController.setCenter(new GeoPoint(lat, lon));
    }

    public void setNewCenterAtZoom( final double centerX, final double centerY, final int zoom ) {
        mapsView.getController().setZoom(zoom);
        setZoomGuiText(zoom);
        mapsView.getController().setCenter(new GeoPoint((int) (centerX * E6), (int) (centerY * E6)));
        mapsView.postInvalidate();
    }

    public double[] getCenterLonLat() {
        GeoPoint mapCenter = mapsView.getMapCenter();
        double[] lonLat = {mapCenter.getLongitudeE6() / 6d, mapCenter.getLatitudeE6() / 6d};
        return lonLat;
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_GPSDATA, 1, R.string.mainmenu_gpsdataselect).setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_MAPDATA, 2, R.string.mainmenu_mapdataselect).setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_MINIMAP_ID, 3, R.string.mapsactivity_menu_toggle_minimap).setIcon(R.drawable.ic_menu_minimap);
        menu.add(Menu.NONE, MENU_SCALE_ID, 4, R.string.mapsactivity_menu_toggle_scalebar).setIcon(R.drawable.ic_menu_scalebar);
        final SubMenu subMenu = menu.addSubMenu(Menu.NONE, MENU_TILE_SOURCE_ID, 5, R.string.mapsactivity_menu_tilesource)
                .setIcon(R.drawable.ic_menu_tilesource);
        {
            for( final ITileSource tileSource : TileSourceFactory.getTileSources() ) {
                subMenu.add(0, 1000 + tileSource.ordinal(), Menu.NONE, tileSource.localizedName(mResourceProxy));
            }
        }
        menu.add(Menu.NONE, GO_TO, 6, R.string.goto_coordinate).setIcon(android.R.drawable.ic_menu_myplaces);
        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
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
        case MENU_TILE_SOURCE_ID:
            this.mapsView.invalidate();
            return true;

        case MENU_MINIMAP_ID:
            boolean isMinimapEnabled = !mMiniMapOverlay.isEnabled();
            mMiniMapOverlay.setEnabled(isMinimapEnabled);
            mapsView.invalidate();
            Editor editor1 = preferences.edit();
            editor1.putBoolean(PREFS_KEY_MINIMAPON, isMinimapEnabled);
            editor1.commit();
            return true;
        case MENU_SCALE_ID:
            boolean isScalebarEnabled = !mScaleBarOverlay.isEnabled();
            mScaleBarOverlay.setEnabled(isScalebarEnabled);
            mapsView.invalidate();
            Editor editor2 = preferences.edit();
            editor2.putBoolean(PREFS_KEY_SCALEBARON, isScalebarEnabled);
            return true;
        case GO_TO: {
            Intent intent = new Intent(Constants.INSERT_COORD);
            startActivity(intent);
            return true;
        }
        default:
            ITileSource tileSource = TileSourceFactory.getTileSource(item.getItemId() - 1000);
            mapsView.setTileSource(tileSource);
            mMiniMapOverlay.setTileSource(tileSource);
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
                            BoundingBoxE6 boundingBox = mapsView.getBoundingBox();
                            float n = boundingBox.getLatNorthE6() / E6;
                            float s = boundingBox.getLatSouthE6() / E6;
                            float w = boundingBox.getLonWestE6() / E6;
                            float e = boundingBox.getLonEastE6() / E6;
                            final List<Bookmark> bookmarksInBounds = DaoBookmarks.getBookmarksInWorldBounds(MapsActivity.this, n,
                                    s, w, e);
                            int bookmarksNum = bookmarksInBounds.size();

                            bookmarksRemoveDialog = new ProgressDialog(MapsActivity.this);
                            bookmarksRemoveDialog.setCancelable(true);
                            bookmarksRemoveDialog.setMessage(MessageFormat.format(
                                    getString(R.string.mapsactivity_delete_bookmarks), bookmarksNum));
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
                            BoundingBoxE6 boundingBox = mapsView.getBoundingBox();
                            float n = boundingBox.getLatNorthE6() / E6;
                            float s = boundingBox.getLatSouthE6() / E6;
                            float w = boundingBox.getLonWestE6() / E6;
                            float e = boundingBox.getLonEastE6() / E6;
                            final List<Note> notesInBounds = DaoNotes.getNotesInWorldBounds(MapsActivity.this, n, s, w, e);

                            int notesNum = notesInBounds.size();

                            notesRemoveDialog = new ProgressDialog(MapsActivity.this);
                            notesRemoveDialog.setCancelable(true);
                            notesRemoveDialog.setMessage(MessageFormat.format(getString(R.string.mapsactivity_delete_notes),
                                    notesNum));
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
        GeoPoint mapCenter = mapsView.getMapCenter();
        final float centerLat = mapCenter.getLatitudeE6() / E6;
        final float centerLon = mapCenter.getLongitudeE6() / E6;
        final EditText input = new EditText(this);
        final String newDate = Constants.TIME_FORMATTER.format(new Date());
        final String proposedName = "bookmark " + newDate; //$NON-NLS-1$
        input.setText(proposedName);
        Builder builder = new AlertDialog.Builder(this).setTitle(R.string.mapsactivity_new_bookmark);
        builder.setMessage(R.string.mapsactivity_enter_bookmark_name);
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

                            int zoom = mapsView.getZoomLevel();
                            BoundingBoxE6 boundingBox = mapsView.getBoundingBox();
                            float n = boundingBox.getLatNorthE6() / E6;
                            float s = boundingBox.getLatSouthE6() / E6;
                            float w = boundingBox.getLonWestE6() / E6;
                            float e = boundingBox.getLonEastE6() / E6;
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

    private void enableDrawingWithDelay() {
        new Thread(new Runnable(){
            public void run() {
                runOnUiThread(new Runnable(){
                    public void run() {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mMapsOverlay.setDoDraw(true);
                        mLogsOverlay.setDoDraw(true);
                        mNotesOverlay.setDoDraw(true);
                        mBookmarksOverlay.setDoDraw(true);
                        mGpsOverlay.setDoDraw(true);
                        inalidateMap();
                    }
                });
            }
        }).start();
    }

    private void disableDrawing() {
        mMapsOverlay.setDoDraw(false);
        mLogsOverlay.setDoDraw(false);
        mNotesOverlay.setDoDraw(false);
        mBookmarksOverlay.setDoDraw(false);
        mGpsOverlay.setDoDraw(false);
    }

    private int maxZoomLevel;
    private int minZoomLevel;
    public void onLocationChanged( GpsLocation loc ) {
        if (loc == null) {
            return;
        }
        BoundingBoxE6 boundingBox = mapsView.getBoundingBox();
        int lat = (int) ((float) loc.getLatitude() * E6);
        int lon = (int) ((float) loc.getLongitude() * E6);
        if (!boundingBox.contains(lat, lon)) {
            return;
        }

        mMapsOverlay.setGpsUpdate(true);
        mLogsOverlay.setGpsUpdate(true);
        mNotesOverlay.setGpsUpdate(true);
        mBookmarksOverlay.setGpsUpdate(true);
        mGpsOverlay.setLoc(loc);
        mapsView.invalidate();
    }

    public void onStatusChanged( boolean hasFix ) {
    }

    public boolean onScroll( ScrollEvent event ) {
        return true;
    }

    public boolean onZoom( ZoomEvent event ) {
        int zoomLevel = event.getZoomLevel();
        if (zoomInButton != null) {
            setZoomGuiText(zoomLevel);
        }
        return true;
    }

}
