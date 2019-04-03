package eu.geopaparazzi.map.layers;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import org.hortonmachine.dbs.utils.MercatorUtils;
import org.oscim.backend.canvas.Color;
import org.oscim.core.MercatorProjection;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.utils.ColorUtil;

import java.io.IOException;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.TableDescriptions;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.map.GPMapView;

public class NotesLayer extends VectorLayer {

    private final SharedPreferences peferences;

    public NotesLayer(GPMapView mapView) {
        super(mapView.map());

        peferences = PreferenceManager.getDefaultSharedPreferences(mapView.getContext());
        try {
            reloadData();
        } catch (IOException e) {
            GPLog.error(this, null, e);
        }
    }

    public void reloadData() throws IOException {
        tmpDrawables.clear();
        mDrawables.clear();

        String opacityStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_OPACITY, "255"); //$NON-NLS-1$
        String sizeStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_SIZE, LibraryConstants.DEFAULT_NOTES_SIZE + ""); //$NON-NLS-1$
        String colorStr = peferences.getString(LibraryConstants.PREFS_KEY_NOTES_CUSTOMCOLOR, ColorUtilities.BLUE.getHex()); //$NON-NLS-1$
        int noteSize = Integer.parseInt(sizeStr);
        float opacity = Integer.parseInt(opacityStr);
        float alpha = 1 - opacity / 255f;

        Style pointStyle = null;

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

//        STRtree tmp = new STRtree();
        try (Cursor c = sqliteDatabase.rawQuery(query, null)) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                int i = 0;
//                LayerNote note = new LayerNote();
                long id = c.getLong(i++);
                double lon = c.getDouble(i++);
                double lat = c.getDouble(i++);
//                note.elev = c.getDouble(i++);
//                note.text = c.getString(i++);
//                note.ts = c.getLong(i++);
//                String form = c.getString(i);
//                note.hasForm = form != null && form.length() > 0;
//                tmp.insert(new Envelope(new Coordinate(note.lon, note.lat)), note);


                if (pointStyle == null) {
                    double longitudeFromMeters = MercatorUtils.metersXToLongitude(noteSize);

                    pointStyle = Style.builder()
                            .buffer(longitudeFromMeters)
                            .strokeColor(colorStr)
                            .fillColor(colorStr)
                            .fillAlpha(opacity)
                            .build();
                }
                add(new PointDrawable(lat, lon, pointStyle));

//                currentMinLon = Math.min(currentMinLon, note.lon);
//                currentMaxLon = Math.max(currentMaxLon, note.lon);
//                currentMinLat = Math.min(currentMinLat, note.lat);
//                currentMaxLat = Math.max(currentMaxLat, note.lat);

                c.moveToNext();
            }

//            tmp.build();
        }


        update();
    }


    public void disable() {
        setEnabled(false);
    }


    public void enable() {
        setEnabled(true);
    }
}
