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
package eu.hydrologis.geopaparazzi.maps.overlays;

import static eu.hydrologis.geopaparazzi.util.Constants.E6;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.ResourceProxy;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.Overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.maps.DataManager;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Note;

/**
 * Overlay to show gps notes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class NotesOverlay extends Overlay {

    private final Paint mPaint = new Paint();
    private final Paint mTextPaint = new Paint();

    private Context context;

    final private Rect screenRect = new Rect();

    private boolean touchDragging = false;
    private boolean doDraw = true;
    private int zoomLevel1;
    private int zoomLevel2;
    private int zoomLevelLabelLength1;
    private int zoomLevelLabelLength2;
    private boolean gpsUpdate = false;
    private List<Note> notesInWorldBounds = new ArrayList<Note>();

    public NotesOverlay( final Context ctx, final ResourceProxy pResourceProxy ) {
        super(pResourceProxy);
        this.context = ctx;

        float textSizeMedium = ctx.getResources().getDimension(R.dimen.text_normal);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(textSizeMedium);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        zoomLevel1 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM1, "14"));
        zoomLevel2 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM2, "16"));
        zoomLevelLabelLength1 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM1_LABELLENGTH, "4"));
        zoomLevelLabelLength2 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM2_LABELLENGTH, "-1"));
    }

    public void setDoDraw( boolean doDraw ) {
        this.doDraw = doDraw;
        if (Debug.D) Logger.d(this, "Will draw: " + doDraw);
    }

    public void setGpsUpdate( boolean gpsUpdate ) {
        this.gpsUpdate = gpsUpdate;
    }

    protected void draw( final Canvas canvas, final MapView mapsView, final boolean shadow ) {
        if (touchDragging || shadow || !doDraw || mapsView.isAnimating() || !DataManager.getInstance().areNotesVisible())
            return;

        BoundingBoxE6 boundingBox = mapsView.getBoundingBox();
        float y0 = boundingBox.getLatNorthE6() / E6;
        float y1 = boundingBox.getLatSouthE6() / E6;
        float x0 = boundingBox.getLonWestE6() / E6;
        float x1 = boundingBox.getLonEastE6() / E6;

        Projection pj = mapsView.getProjection();

        int screenWidth = canvas.getWidth();
        int screenHeight = canvas.getHeight();
        screenRect.contains(0, 0, screenWidth, screenHeight);
        mapsView.getScreenRect(screenRect);

        int zoomLevel = mapsView.getZoomLevel();

        if (gpsUpdate) {
            drawNotes(canvas, pj, zoomLevel);
            gpsUpdate = false;
            return;
        }

        try {
            notesInWorldBounds = DaoNotes.getNotesInWorldBounds(context, y0, y1, x0, x1);
            drawNotes(canvas, pj, zoomLevel);

        } catch (IOException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

    }

    private void drawNotes( final Canvas canvas, Projection pj, int zoomLevel ) {
        int notesColor = DataManager.getInstance().getNotesColor();
        float notesWidth = DataManager.getInstance().getNotesWidth();
        mPaint.setAntiAlias(true);
        mPaint.setColor(notesColor);
        mPaint.setStrokeWidth(notesWidth);
        mPaint.setStyle(Paint.Style.FILL);
        for( Note note : notesInWorldBounds ) {
            float lat = (float) note.getLat();
            float lon = (float) note.getLon();

            GeoPoint g = new GeoPoint(lat, lon);
            Point mapPixels = pj.toMapPixels(g, null);

            canvas.drawPoint(mapPixels.x, mapPixels.y, mPaint);
            drawLabel(canvas, note.getName(), mapPixels.x, mapPixels.y, mTextPaint, zoomLevel);
        }
    }

    private void drawLabel( Canvas canvas, String label, float positionX, float positionY, Paint paint, int zoom ) {
        if (label == null || label.length() == 0) {
            return;
        }
        if (zoom >= zoomLevel1) {
            if (zoom < zoomLevel2) {
                if (zoomLevelLabelLength1 != -1 && label.length() > zoomLevelLabelLength1) {
                    label = label.substring(0, zoomLevelLabelLength1);
                }
            } else {
                if (zoomLevelLabelLength2 != -1 && label.length() > zoomLevelLabelLength2) {
                    label = label.substring(0, zoomLevelLabelLength2);
                }
            }
            canvas.drawText(label, positionX, positionY, paint);
        }
    }

    @Override
    public boolean onTouchEvent( MotionEvent event, MapView mapView ) {
        int action = event.getAction();
        switch( action ) {
        case MotionEvent.ACTION_MOVE:
            touchDragging = true;
            break;
        case MotionEvent.ACTION_UP:
            touchDragging = false;
            mapView.invalidate();
            break;
        }
        return super.onTouchEvent(event, mapView);
    }

}
