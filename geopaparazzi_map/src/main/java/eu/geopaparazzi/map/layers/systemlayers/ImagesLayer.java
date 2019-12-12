package eu.geopaparazzi.map.layers.systemlayers;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.map.Layers;
import org.oscim.map.Map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IImagesDbHelper;
import eu.geopaparazzi.library.database.TableDescriptions;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.ISystemLayer;

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_IMAGES_TEXT_VISIBLE;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_IMAGES_VISIBLE;

public class ImagesLayer extends ItemizedLayer<MarkerItem> implements ItemizedLayer.OnItemGestureListener<MarkerItem>, ISystemLayer {
    private static final int FG_COLOR = 0xFF000000; // 100 percent black. AARRGGBB
    private static final int BG_COLOR = 0x80FF69B4; // 50 percent pink. AARRGGBB
    private static final int TRANSP_WHITE = 0x80FFFFFF; // 50 percent white. AARRGGBB
    public static String NAME = null;
    private static Bitmap imagesBitmap;
    private boolean showLabels;
    private GPMapView mapView;
    private static int textSize;
    private static String colorStr;

    public ImagesLayer(GPMapView mapView) {
        super(mapView.map(), getMarkerSymbol(mapView));
        this.mapView = mapView;
        getName(mapView.getContext());

        setOnItemGestureListener(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GPApplication.getInstance());
        boolean imagesVisible = preferences.getBoolean(PREFS_KEY_IMAGES_VISIBLE, true);
        showLabels = preferences.getBoolean(PREFS_KEY_IMAGES_TEXT_VISIBLE, true);

        try {
            if (imagesVisible)
                reloadData();
        } catch (IOException e) {
            GPLog.error(this, null, e);
        }


    }

    public static String getName(Context context) {
        if (NAME == null) {
            NAME = context.getString(R.string.layername_images);
        }
        return NAME;
    }

    private static MarkerSymbol getMarkerSymbol(GPMapView mapView) {
        SharedPreferences peferences = PreferenceManager.getDefaultSharedPreferences(mapView.getContext());
        String textSizeStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_TEXT_SIZE, LibraryConstants.DEFAULT_NOTES_SIZE + ""); //$NON-NLS-1$
        textSize = Integer.parseInt(textSizeStr);
        colorStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_CUSTOMCOLOR, ColorUtilities.ALMOST_BLACK.getHex());
        Drawable imagesDrawable = Compat.getDrawable(mapView.getContext(), eu.geopaparazzi.library.R.drawable.ic_images_48dp);

        imagesBitmap = AndroidGraphics.drawableToBitmap(imagesDrawable);

        return new MarkerSymbol(imagesBitmap, MarkerSymbol.HotspotPlace.UPPER_LEFT_CORNER, false);
    }

    public ImagesLayer(Map map, MarkerSymbol defaultMarker) {
        super(map, defaultMarker);
    }

    public void reloadData() throws IOException {
        SQLiteDatabase sqliteDatabase = GPApplication.getInstance().getDatabase();

        List<MarkerItem> images = new ArrayList<>();
        String[] asColumnsToReturn = {//
                TableDescriptions.ImageTableFields.COLUMN_LON.getFieldName(),//
                TableDescriptions.ImageTableFields.COLUMN_LAT.getFieldName(), //
                TableDescriptions.ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName(),//
                TableDescriptions.ImageTableFields.COLUMN_ALTIM.getFieldName(),//
                TableDescriptions.ImageTableFields.COLUMN_TS.getFieldName(),//
                TableDescriptions.ImageTableFields.COLUMN_TEXT.getFieldName()//
        };
        String strSortOrder = "_id ASC"; //NON-NLS
        String whereString = TableDescriptions.ImageTableFields.COLUMN_NOTE_ID.getFieldName() + " < 0";
        Cursor c = sqliteDatabase.query(TableDescriptions.TABLE_IMAGES, asColumnsToReturn, whereString, null, null, null, strSortOrder);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            double lon = c.getDouble(0);
            double lat = c.getDouble(1);
            long imageDataId = c.getLong(2);
            double elev = c.getDouble(3);
            long ts = c.getLong(4);
            String text = c.getString(5);

            String descr = "note: " + text + "\n" + //NON-NLS
                    "id: " + imageDataId + "\n" +//NON-NLS
                    "longitude: " + lon + "\n" +//NON-NLS
                    "latitude: " + lat + "\n" +//NON-NLS
                    "elevation: " + elev + "\n" +//NON-NLS
                    "timestamp: " + TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));//NON-NLS

            images.add(new MarkerItem(imageDataId, text, descr, new GeoPoint(lat, lon)));
            c.moveToNext();
        }
        c.close();


        for (MarkerItem mi : images) {
            mi.setMarker(createAdvancedSymbol(mi, imagesBitmap));
        }
        addItems(images);

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
            Context context = mapView.getContext();
            long uid = (long) item.getUid();
            openImage(context, uid, item.title);
            return true;
        }
        return false;
    }


    private void openImage(Context context, long imageDataId, String title) {
        try {
            // get image from db
            int length = title.length();
            String ext = title.substring(length - 4, length);
            String tempImageName = ImageUtilities.getTempImageName(ext);
            IImagesDbHelper imageHelper = DefaultHelperClasses.getDefaulfImageHelper();
            byte[] imageData = imageHelper.getImageDataById(imageDataId, null);
            AppsUtilities.showImage(imageData, tempImageName, context);
        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }
    }


    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        return false;
    }


    /**
     * Creates a transparent symbol with text and description.
     *
     * @param item      -> the MarkerItem to process, containing title and description
     *                  if description starts with a '#' the first line of the description is drawn.
     * @param poiBitmap -> poi bitmap for the center
     * @return MarkerSymbol with title, description and symbol
     */
    private MarkerSymbol createAdvancedSymbol(MarkerItem item, Bitmap poiBitmap) {
        final Paint textPainter = CanvasAdapter.newPaint();
        textPainter.setStyle(Paint.Style.FILL);
        int textColor = ColorUtilities.toColor(colorStr);
        textPainter.setColor(textColor);
        textPainter.setTextSize(textSize);
        textPainter.setTypeface(Paint.FontFamily.MONOSPACE, Paint.FontStyle.NORMAL);

        final Paint haloTextPainter = CanvasAdapter.newPaint();
        haloTextPainter.setStyle(Paint.Style.FILL);
        haloTextPainter.setColor(Color.WHITE);
        haloTextPainter.setTextSize(textSize);
        haloTextPainter.setTypeface(Paint.FontFamily.MONOSPACE, Paint.FontStyle.BOLD);

        int bitmapHeight = poiBitmap.getHeight();
        int margin = 3;
        int dist2symbol = (int) Math.round(bitmapHeight / 2.0);

        int titleWidth = ((int) haloTextPainter.getTextWidth(item.title) + 2 * margin);
        int titleHeight = (int) (haloTextPainter.getTextHeight(item.title) + textPainter.getFontDescent() + 2 * margin);

        int symbolWidth = poiBitmap.getWidth();

        int xSize = Math.max(titleWidth, symbolWidth);
        int ySize = titleHeight + symbolWidth + dist2symbol;

        // markerCanvas, the drawing area for all: title, description and symbol
        Bitmap markerBitmap = CanvasAdapter.newBitmap(xSize, ySize, 0);
        org.oscim.backend.canvas.Canvas markerCanvas = CanvasAdapter.newCanvas();
        markerCanvas.setBitmap(markerBitmap);

        // titleCanvas for the title text
        Bitmap titleBitmap = CanvasAdapter.newBitmap(titleWidth + margin, titleHeight + margin, 0);
        org.oscim.backend.canvas.Canvas titleCanvas = CanvasAdapter.newCanvas();
        titleCanvas.setBitmap(titleBitmap);

        titleCanvas.fillRectangle(0, 0, titleWidth, titleHeight, TRANSP_WHITE);
        titleCanvas.drawText(item.title, margin, titleHeight - margin - textPainter.getFontDescent(), haloTextPainter);
        titleCanvas.drawText(item.title, margin, titleHeight - margin - textPainter.getFontDescent(), textPainter);

        if (showLabels)
            markerCanvas.drawBitmap(titleBitmap, xSize * 0.5f - (titleWidth * 0.5f), symbolWidth * 0.25f);
        markerCanvas.drawBitmap(poiBitmap, xSize * 0.5f - (symbolWidth * 0.25f), ySize * 0.5f - (symbolWidth * 0.25f));

        return (new MarkerSymbol(markerBitmap, MarkerSymbol.HotspotPlace.CENTER, true));
    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public GPMapView getMapView() {
        return mapView;
    }

    @Override
    public void load() {
        Layers layers = map().layers();
        layers.add(this, LayerGroups.GROUP_SYSTEM_TOP.getGroupId());
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return toDefaultJson();
    }

    @Override
    public void dispose() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }
}
