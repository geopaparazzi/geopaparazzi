package eu.geopaparazzi.core.mapview.layers;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.preference.PreferenceManager;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.core.database.TableDescriptions;
import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.mapsforge.utils.MapsforgeUtils;
import eu.geopaparazzi.spatialite.ISpatialiteTableAndFieldsNames;

import static eu.geopaparazzi.library.util.LibraryConstants.DEFAULT_NOTES_SIZE;

public class NotesLayer extends Layer implements ISpatialiteTableAndFieldsNames {
    private Bitmap notesBitmap;
    private int horizontalOffset;
    private int verticalOffset;

    private Paint notesTextPaint;
    private Paint haloPaint;
    private List<LayerNote> currentLayerNotes = null;
    private double currentMinLon;
    private double currentMinLat;
    private double currentMaxLon;
    private double currentMaxLat;

    public NotesLayer(Context context) {
        super();

        SharedPreferences mPeferences = PreferenceManager.getDefaultSharedPreferences(context);

        // notes type
        boolean isNotesTextVisible = mPeferences.getBoolean(LibraryConstants.PREFS_KEY_NOTES_TEXT_VISIBLE, true);
        boolean doCustom = mPeferences.getBoolean(LibraryConstants.PREFS_KEY_NOTES_CHECK, true);
        String opacityStr = mPeferences.getString(LibraryConstants.PREFS_KEY_NOTES_OPACITY, "255"); //$NON-NLS-1$
        String sizeStr = mPeferences.getString(LibraryConstants.PREFS_KEY_NOTES_SIZE, DEFAULT_NOTES_SIZE + ""); //$NON-NLS-1$
        String colorStr = mPeferences.getString(LibraryConstants.PREFS_KEY_NOTES_CUSTOMCOLOR, ColorUtilities.BLUE.getHex()); //$NON-NLS-1$
        int noteSize = Integer.parseInt(sizeStr);
        float opacity = Integer.parseInt(opacityStr);
        Drawable notesDrawable;
        if (doCustom) {
            android.graphics.Paint notesPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            notesPaint.setStyle(android.graphics.Paint.Style.FILL);
            notesPaint.setColor(ColorUtilities.toColor(colorStr));
            notesPaint.setAlpha((int) opacity);

            OvalShape notesShape = new OvalShape();
            ShapeDrawable notesShapeDrawable = new ShapeDrawable(notesShape);
            android.graphics.Paint paint = notesShapeDrawable.getPaint();
            paint.set(notesPaint);
            notesShapeDrawable.setIntrinsicHeight(noteSize);
            notesShapeDrawable.setIntrinsicWidth(noteSize);
            notesDrawable = notesShapeDrawable;

            if (isNotesTextVisible) {
                notesTextPaint = AndroidGraphicFactory.INSTANCE.createPaint();
                notesTextPaint.setColor(MapsforgeUtils.toColor(colorStr, -1));
                notesTextPaint.setStrokeWidth(2);
                notesTextPaint.setStyle(Style.FILL);
                notesTextPaint.setTextSize(noteSize);
                notesTextPaint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
            }
        } else {
            notesDrawable = Compat.getDrawable(context, eu.geopaparazzi.library.R.drawable.ic_place_accent_24dp);

            if (isNotesTextVisible) {
                notesTextPaint = AndroidGraphicFactory.INSTANCE.createPaint();
                notesTextPaint.setColor(AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK));
                notesTextPaint.setStrokeWidth(2);
                notesTextPaint.setStyle(Style.FILL);
                notesTextPaint.setTextSize(noteSize);
                notesTextPaint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
            }
        }
        Bitmap notesBitmap = AndroidGraphicFactory.convertToBitmap(notesDrawable);
        this.notesBitmap = notesBitmap;
        this.horizontalOffset = 0;
        this.verticalOffset = 0;

        haloPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        haloPaint.setColor(AndroidGraphicFactory.INSTANCE.createColor(Color.WHITE));
        haloPaint.setStrokeWidth(6);
        haloPaint.setStyle(Style.STROKE);
        haloPaint.setTextSize(noteSize);
        haloPaint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);


    }


    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {

        if (notesBitmap != null && !notesBitmap.isDestroyed()) {
            int width = notesBitmap.getWidth();
            int height = notesBitmap.getHeight();
            int halfBitmapWidth = width / 2;
            int halfBitmapHeight = height / 2;
            long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());

            try {
                checkNotes(boundingBox);

                for (LayerNote note : currentLayerNotes) {
                    if (boundingBox.contains(note.lat, note.lon)) {
                        double pixelX = MercatorProjection.longitudeToPixelX(note.lon, mapSize);
                        double pixelY = MercatorProjection.latitudeToPixelY(note.lat, mapSize);

                        int left = (int) (pixelX - topLeftPoint.x - halfBitmapWidth + this.horizontalOffset);
                        int top = (int) (pixelY - topLeftPoint.y - halfBitmapHeight + this.verticalOffset);
                        int right = left + width;
                        int bottom = top + height;


                        canvas.drawBitmap(notesBitmap, left, top);

                        if (notesTextPaint != null) {
                            int x = right + 5;
//                            int textHeight = notesTextPaint.getTextHeight(note.text);
                            int y = (int) (bottom + (top - bottom) / 2.0);// + textHeight / 2.0);

                            canvas.drawText(note.text, x, y, haloPaint);
                            canvas.drawText(note.text, x, y, notesTextPaint);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public synchronized void onDestroy() {
        if (this.notesBitmap != null) {
            this.notesBitmap.decrementRefCount();
        }
    }

    private static class LayerNote {
        long id;
        double lat;
        double lon;
        double elev;
        long ts;
        String text;
        boolean hasForm;
    }

    private synchronized void checkNotes(BoundingBox boundingBox) throws IOException {
//        boolean noNeedToRead = currentMinLon < boundingBox.minLongitude &&
//                currentMinLat < boundingBox.minLatitude &&
//                currentMaxLon > boundingBox.maxLongitude &&
//                currentMaxLat > boundingBox.maxLatitude;
//        if (noNeedToRead)
//            return;
//        else
//            Log.i("blah", "Reading notes...");


        double plus = 0;//boundingBox.getLongitudeSpan() * 0.2;

        SQLiteDatabase sqliteDatabase = GPApplication.getInstance().getDatabase();

        String query = "SELECT " +//
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
                " FROM " + TableDescriptions.TABLE_NOTES;
//        query = query + " WHERE (lon BETWEEN XXX AND XXX) AND (lat BETWEEN XXX AND XXX)";
//        query = query.replaceFirst("XXX", String.valueOf(boundingBox.minLongitude - plus));
//        query = query.replaceFirst("XXX", String.valueOf(boundingBox.maxLongitude + plus));
//        query = query.replaceFirst("XXX", String.valueOf(boundingBox.minLatitude - plus));
//        query = query.replaceFirst("XXX", String.valueOf(boundingBox.maxLatitude + plus));
//        query = query + " AND " + TableDescriptions.NotesTableFields.COLUMN_ISDIRTY.getFieldName() + " = 1";
        query = query + " WHERE " + TableDescriptions.NotesTableFields.COLUMN_ISDIRTY.getFieldName() + " = 1";

        try (Cursor c = sqliteDatabase.rawQuery(query, null)) {
            List<LayerNote> tmp = new ArrayList<>();
            c.moveToFirst();
            while (!c.isAfterLast()) {
                int i = 0;
                LayerNote note = new LayerNote();
                note.id = c.getLong(i++);
                note.lon = c.getDouble(i++);
                note.lat = c.getDouble(i++);
                note.elev = c.getDouble(i++);
                note.text = c.getString(i++);
                note.ts = c.getLong(i++);
                String form = c.getString(i);
                note.hasForm = form != null && form.length() > 0;
                tmp.add(note);

//                currentMinLon = Math.min(currentMinLon, note.lon);
//                currentMaxLon = Math.max(currentMaxLon, note.lon);
//                currentMinLat = Math.min(currentMinLat, note.lat);
//                currentMaxLat = Math.max(currentMaxLat, note.lat);

                c.moveToNext();
            }


            currentLayerNotes = tmp;
        }
    }

}
