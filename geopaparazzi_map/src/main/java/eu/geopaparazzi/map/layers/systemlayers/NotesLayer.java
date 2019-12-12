package eu.geopaparazzi.map.layers.systemlayers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
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
import eu.geopaparazzi.library.database.ANote;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.TableDescriptions;
import eu.geopaparazzi.library.forms.FormActivity;
import eu.geopaparazzi.library.forms.FormInfoHolder;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.ISystemLayer;

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_NOTES_TEXT_VISIBLE;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_NOTES_VISIBLE;

public class NotesLayer extends ItemizedLayer<MarkerItem> implements ItemizedLayer.OnItemGestureListener<MarkerItem>, ISystemLayer {
    private static final int FG_COLOR = 0xFF000000; // 100 percent black. AARRGGBB
    private static final int BG_COLOR = 0x80FF69B4; // 50 percent pink. AARRGGBB
    private static final int TRANSP_WHITE = 0x80FFFFFF; // 50 percent white. AARRGGBB
    private static String NAME = null;
    public static final String NONFORMSTART = "@";

    public static final int FORMUPDATE_RETURN_CODE = 669;
    private static Bitmap notesBitmap;
    private GPMapView mapView;
    private IActivitySupporter activitySupporter;
    private static int textSize;
    private static String colorStr;
    private boolean showLabels;

    public NotesLayer(GPMapView mapView, IActivitySupporter activitySupporter) {
        super(mapView.map(), getMarkerSymbol(mapView));
        this.mapView = mapView;
        getName(mapView.getContext());

        this.activitySupporter = activitySupporter;
        setOnItemGestureListener(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GPApplication.getInstance());
        boolean notesVisible = preferences.getBoolean(PREFS_KEY_NOTES_VISIBLE, true);

        showLabels = preferences.getBoolean(PREFS_KEY_NOTES_TEXT_VISIBLE, true);

        try {
            if (notesVisible)
                reloadData();
        } catch (IOException e) {
            GPLog.error(this, null, e);
        }


    }

    public static String getName(Context context) {
        if (NAME == null) {
            NAME = context.getString(R.string.layername_notes);
        }
        return NAME;
    }

    private static MarkerSymbol getMarkerSymbol(GPMapView mapView) {
        SharedPreferences peferences = PreferenceManager.getDefaultSharedPreferences(mapView.getContext());
        // notes type
        boolean doCustom = peferences.getBoolean(LibraryConstants.PREFS_KEY_NOTES_CHECK, true);
        String textSizeStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_TEXT_SIZE, LibraryConstants.DEFAULT_NOTES_SIZE + ""); //$NON-NLS-1$
        textSize = Integer.parseInt(textSizeStr);
        colorStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_CUSTOMCOLOR, ColorUtilities.ALMOST_BLACK.getHex());
        Drawable notesDrawable;
        if (doCustom) {
            String opacityStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_OPACITY, "255"); //$NON-NLS-1$
            String sizeStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_SIZE, LibraryConstants.DEFAULT_NOTES_SIZE + ""); //$NON-NLS-1$
            int noteSize = Integer.parseInt(sizeStr);
            float opacity = Integer.parseInt(opacityStr);

            OvalShape notesShape = new OvalShape();
            android.graphics.Paint notesPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            notesPaint.setStyle(android.graphics.Paint.Style.FILL);
            notesPaint.setColor(ColorUtilities.toColor(colorStr));
            notesPaint.setAlpha((int) opacity);

            ShapeDrawable notesShapeDrawable = new ShapeDrawable(notesShape);
            android.graphics.Paint paint = notesShapeDrawable.getPaint();
            paint.set(notesPaint);
            notesShapeDrawable.setIntrinsicHeight(noteSize);
            notesShapeDrawable.setIntrinsicWidth(noteSize);
            notesDrawable = notesShapeDrawable;
        } else {
            notesDrawable = Compat.getDrawable(mapView.getContext(), eu.geopaparazzi.library.R.drawable.ic_place_accent_24dp);
        }

        notesBitmap = AndroidGraphics.drawableToBitmap(notesDrawable);

        return new MarkerSymbol(notesBitmap, MarkerSymbol.HotspotPlace.CENTER, false);
    }

    public NotesLayer(Map map, MarkerSymbol defaultMarker) {
        super(map, defaultMarker);
    }

    public void reloadData() throws IOException {
        SQLiteDatabase sqliteDatabase = GPApplication.getInstance().getDatabase();

        String query = "SELECT " +//NON-NLS
                TableDescriptions.NotesTableFields.COLUMN_ID.getFieldName() +
                ", " +//
                TableDescriptions.NotesTableFields.COLUMN_LON.getFieldName() +
                ", " +//
                TableDescriptions.NotesTableFields.COLUMN_LAT.getFieldName() +
                ", " +//
                TableDescriptions.NotesTableFields.COLUMN_ALTIM.getFieldName() +
                ", " +//
                TableDescriptions.NotesTableFields.COLUMN_TEXT.getFieldName() +
                ", " +//
                TableDescriptions.NotesTableFields.COLUMN_TS.getFieldName() +
                ", " +//
                TableDescriptions.NotesTableFields.COLUMN_FORM.getFieldName() +//
                " FROM " + TableDescriptions.TABLE_NOTES;//NON-NLS
        query = query + " WHERE " + TableDescriptions.NotesTableFields.COLUMN_ISDIRTY.getFieldName() + " = 1";//NON-NLS

        List<MarkerItem> pts = new ArrayList<>();
        try (Cursor c = sqliteDatabase.rawQuery(query, null)) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                int i = 0;
                long id = c.getLong(i++);
                double lon = c.getDouble(i++);
                double lat = c.getDouble(i++);
                double elev = c.getDouble(i++);
                String text = c.getString(i++);
                long ts = c.getLong(i++);
                String form = c.getString(i);
                boolean hasForm = form != null && form.length() > 0;


                String descr;
                if (!hasForm) {
                    descr = NONFORMSTART + "note: " + text + "\n" +//NON-NLS
                            "id: " + id + "\n" +//NON-NLS
                            "longitude: " + lon + "\n" +//NON-NLS
                            "latitude: " + lat + "\n" +//NON-NLS
                            "elevation: " + elev + "\n" +//NON-NLS
                            "timestamp: " + TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));//NON-NLS
                } else {
                    descr = form;
                }

                pts.add(new MarkerItem(id, text, descr, new GeoPoint(lat, lon)));
                c.moveToNext();
            }

            for (MarkerItem mi : pts) {
                mi.setMarker(createAdvancedSymbol(mi, notesBitmap));
            }
            addItems(pts);
        }


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
            if (description.startsWith(NONFORMSTART)) {
                GPDialogs.infoDialog(mapView.getContext(), description.substring(1), null);
            } else {
                try {
                    long uid = (long) item.getUid();
                    ANote note = DefaultHelperClasses.getDefaulfNotesHelper().getNoteById(uid);

                    GeoPoint point = item.getPoint();
                    double lat = point.getLatitude();
                    double lon = point.getLongitude();
                    Intent formIntent = new Intent(mapView.getContext(), FormActivity.class);
                    FormInfoHolder formInfoHolder = new FormInfoHolder();
                    formInfoHolder.sectionName = item.title;
                    formInfoHolder.formName = null;
                    formInfoHolder.noteId = note.getId();
                    formInfoHolder.longitude = lon;
                    formInfoHolder.latitude = lat;
                    formInfoHolder.sectionObjectString = item.getSnippet();
                    formInfoHolder.objectExists = true;
                    formIntent.putExtra(FormInfoHolder.BUNDLE_KEY_INFOHOLDER, formInfoHolder);

                    activitySupporter.startActivityForResult(formIntent, FORMUPDATE_RETURN_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        return false;
    }


    /**
     * Creates a transparent symbol with text and description.
     * PREFS_KEY_IMAGES_TEXT_VISIBLE
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
        int dist2symbol = (int) Math.round(bitmapHeight * 1.5);

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
            markerCanvas.drawBitmap(titleBitmap, xSize * 0.5f - (titleWidth * 0.5f), 0);
        markerCanvas.drawBitmap(poiBitmap, xSize * 0.5f - (symbolWidth * 0.5f), ySize * 0.5f - (symbolWidth * 0.5f));

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
