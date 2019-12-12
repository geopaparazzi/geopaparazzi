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
package eu.geopaparazzi.core.mapview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.MotionEvent;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoGpsLog;
import eu.geopaparazzi.core.database.objects.GpsLogInfo;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.features.editing.EditManager;
import eu.geopaparazzi.map.features.editing.EditingView;
import eu.geopaparazzi.map.features.tools.MapTool;
import eu.geopaparazzi.map.proj.OverlayViewProjection;

import static java.lang.Math.round;

/**
 * A tool to get information about the gps log.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsLogInfoTool extends MapTool {
    private final Paint linePaint = new Paint();
    private final Paint colorBoxPaint = new Paint();
    private final Paint whiteBoxPaint = new Paint();
    private final Paint measureTextPaint = new Paint();
    private final OverlayViewProjection projection;
    private final String timeString;
    private final String lonString;
    private final String latString;
    private final String altimString;

    private final Rect rect = new Rect();

    private DecimalFormat coordFormatter = new DecimalFormat("0.000000");
    private DecimalFormat elevFormatter = new DecimalFormat("0.0");


    private STRtree gpsLogInfoTree;
    private GpsLogInfo gpsLogInfo;
    private final int pixel;

    /**
     * Constructor.
     *
     * @param mapView the mapview reference.
     */
    public GpsLogInfoTool(GPMapView mapView) throws IOException {
        super(mapView);

        Context context = GeopaparazziApplication.getInstance().getApplicationContext();
        pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());

        timeString = context.getString(R.string.utctime);
        lonString = context.getString(R.string.lon);
        latString = context.getString(R.string.lat);
        altimString = context.getString(R.string.altim);

        EditingView editingView = EditManager.INSTANCE.getEditingView();
        projection = new OverlayViewProjection(mapView, editingView);

        readData(editingView);

        whiteBoxPaint.setAntiAlias(false);
        whiteBoxPaint.setColor(Color.argb(160, 255, 255, 255));
        whiteBoxPaint.setStyle(Paint.Style.FILL);

    }

    private void readData(EditingView editingView) throws IOException {
        int screenW = 0;
        int screenE = editingView.getWidth();
        int screenN = 0;
        int screenS = editingView.getHeight();
        Coordinate llPoint = projection.fromPixels(screenW, screenS);
        Coordinate urPoint = projection.fromPixels(screenE, screenN);
        double exp = 0.001;
        gpsLogInfoTree = DaoGpsLog.getGpsLogInfoTree(urPoint.y + exp, llPoint.y - exp, urPoint.x + exp, llPoint.x - exp);
    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(false);
    }

    public void onToolDraw(Canvas canvas) {
        int cWidth = canvas.getWidth();
        int cHeight = canvas.getHeight();

        int upper = 70;


        if (gpsLogInfo == null)
            return;

        GpsLogInfo logInfo = gpsLogInfo;
        int color = Color.BLACK;
        try {
            color = Color.parseColor(logInfo.color);
        } catch (Exception e) {
            // ignore
        }
        linePaint.setAntiAlias(true);
        linePaint.setColor(color);
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);

        colorBoxPaint.setAntiAlias(false);
        colorBoxPaint.setColor(color);
        colorBoxPaint.setStyle(Paint.Style.FILL);

        measureTextPaint.setAntiAlias(true);
        measureTextPaint.setTextSize(pixel);
        measureTextPaint.setColor(color);
        measureTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

        String name = logInfo.logName;
        String ts = " - ";
        try {
            ts = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(logInfo.timestamp));
        } catch (Exception e) {
            GPLog.error(this, "Error in timestamp: " + logInfo.timestamp, e);//NON-NLS
        }
        String time = timeString + ts;
        Coordinate pointXYZ = logInfo.pointXYZ;
        if (pointXYZ == null)
            pointXYZ = new Coordinate(-999, -999);
        String lon = lonString + coordFormatter.format(pointXYZ.x);
        String lat = latString + coordFormatter.format(pointXYZ.y);
        String altim = altimString + elevFormatter.format(pointXYZ.z);

        String[] texts = {name, time, lon, lat, altim};

        int runningY = upper;
        int textWidth = 0;
        for (String text : texts) {
            measureTextPaint.getTextBounds(text, 0, text.length(), rect);
            int textHeight = rect.height();
            runningY += textHeight + 3;
        }

        int inset = 10;
        int bottom = runningY + inset;
        canvas.drawRect(0, 0, cWidth, bottom, whiteBoxPaint);
        canvas.drawRect(0, runningY, cWidth, bottom, colorBoxPaint);


        runningY = upper;
        for (String text : texts) {
            measureTextPaint.getTextBounds(text, 0, text.length(), rect);
            textWidth = rect.width();
            int textHeight = rect.height();
            int x = cWidth / 2 - textWidth / 2;
            canvas.drawText(text, x, runningY, measureTextPaint);
            runningY += textHeight + 7;
        }

        Point point = projection.toPixels(pointXYZ, null);
        canvas.drawLine(point.x, point.y, Math.round(cWidth / 2.0), bottom, linePaint);
    }

    public boolean onToolTouchEvent(MotionEvent event) {
        if (mapView == null || mapView.isClickable()) {
            return false;
        }

        // handle drawing
        float currentX = event.getX();
        float currentY = event.getY();
        int deltaPixels = 100;

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                Coordinate currentGeoPoint = projection.fromPixels(round(currentX), round(currentY));
                Coordinate plusPoint = projection.fromPixels(round(currentX + deltaPixels), round(currentY + deltaPixels));

                double touchLon = currentGeoPoint.x;
                double touchLat = currentGeoPoint.y;
                double lonPlus = plusPoint.x;
                double latPlus = plusPoint.y;
                double deltaX = Math.abs(touchLon - lonPlus);
                double deltaY = Math.abs(touchLat - latPlus);
                Coordinate touchCoord = new Coordinate(touchLon, touchLat);
                Envelope queryEnvelope = new Envelope(touchCoord);
                queryEnvelope.expandBy(deltaX, deltaY);

                List<GpsLogInfo> result = gpsLogInfoTree.query(queryEnvelope);
                if (result.size() == 0) {
                    return true;
                } else {
                    GpsLogInfo nearest = null;
                    double minDist = Double.POSITIVE_INFINITY;
                    for (GpsLogInfo info : result) {
                        double dist = touchCoord.distance(info.pointXYZ);
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = info;
                        }
                    }
                    gpsLogInfo = nearest;
                }
                break;
            case MotionEvent.ACTION_UP:
                gpsLogInfo = null;
                break;
        }
        EditManager.INSTANCE.invalidateEditingView();
        return true;
    }

    @Override
    public void onViewChanged() {
        EditingView editingView = EditManager.INSTANCE.getEditingView();
        try {
            readData(editingView);
        } catch (IOException e) {
            GPLog.error(this, null, e);
        }
    }

    public void disable() {
        if (mapView != null) {
            mapView.setClickable(true);
            mapView = null;
        }
        gpsLogInfo = null;
    }

}