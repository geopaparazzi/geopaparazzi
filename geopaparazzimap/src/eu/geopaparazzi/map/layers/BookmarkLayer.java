package eu.geopaparazzi.map.layers;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.map.Map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.TableDescriptions;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.map.GPMapView;

public class BookmarkLayer extends ItemizedLayer<MarkerItem> implements ItemizedLayer.OnItemGestureListener<MarkerItem> {
    private static final int FG_COLOR = 0xFF000000; // 100 percent black. AARRGGBB
    private static final int BG_COLOR = 0x80FF69B4; // 50 percent pink. AARRGGBB
    private static final int TRANSP_WHITE = 0x80FFFFFF; // 50 percent white. AARRGGBB
    private static Bitmap imagesBitmap;
    private GPMapView mapView;
    private static String colorStr;

    public static final String TABLE_BOOKMARKS = "bookmarks";

    public BookmarkLayer(GPMapView mapView) {
        super(mapView.map(), getMarkerSymbol(mapView));
        this.mapView = mapView;
        setOnItemGestureListener(this);

        try {
            reloadData();
        } catch (IOException e) {
            GPLog.error(this, null, e);
        }


    }

    private static MarkerSymbol getMarkerSymbol(GPMapView mapView) {
        SharedPreferences peferences = PreferenceManager.getDefaultSharedPreferences(mapView.getContext());
        String textSizeStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_TEXT_SIZE, LibraryConstants.DEFAULT_NOTES_SIZE + ""); //$NON-NLS-1$
        colorStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_CUSTOMCOLOR, ColorUtilities.ALMOST_BLACK.getHex());
        Drawable imagesDrawable = Compat.getDrawable(mapView.getContext(), eu.geopaparazzi.library.R.drawable.ic_bookmarks_48dp);

        imagesBitmap = AndroidGraphics.drawableToBitmap(imagesDrawable);

        return new MarkerSymbol(imagesBitmap, MarkerSymbol.HotspotPlace.UPPER_LEFT_CORNER, false);
    }

    public BookmarkLayer(Map map, MarkerSymbol defaultMarker) {
        super(map, defaultMarker);
    }

    public void reloadData() throws IOException {
        SQLiteDatabase sqliteDatabase = GPApplication.getInstance().getDatabase();

        List<MarkerItem> bookmarks = new ArrayList<>();
        String query = "SELECT lon, lat, text FROM " + TABLE_BOOKMARKS;

        Cursor c = null;
        try {
            c = sqliteDatabase.rawQuery(query, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                double lon = c.getDouble(0);
                double lat = c.getDouble(1);
                String text = c.getString(2);

                String descr = "bookmark: " + text + "\n" +
                        "longitude: " + lon + "\n" +
                        "latitude: " + lat;
                bookmarks.add(new MarkerItem(text, descr, new GeoPoint(lat, lon)));
                c.moveToNext();
            }
        } finally {
            if (c != null)
                c.close();
        }


        for (MarkerItem mi : bookmarks) {
            mi.setMarker(new MarkerSymbol(imagesBitmap, MarkerSymbol.HotspotPlace.CENTER, true));
        }
        addItems(bookmarks);

        update();
    }


    public void disable() {
        setEnabled(false);
    }


    public void enable() {
        setEnabled(true);
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        if (item != null) {
            String description = item.getSnippet();
            GPDialogs.infoDialog(mapView.getContext(), description, null);
        }
        return false;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        return false;
    }


}