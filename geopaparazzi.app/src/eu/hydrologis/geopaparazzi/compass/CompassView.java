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
package eu.hydrologis.geopaparazzi.compass;

import java.text.DecimalFormat;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import eu.hydrologis.geopaparazzi.GeoPaparazziActivity;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.chart.ChartDrawer;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.ApplicationManagerListener;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * The view taking care of drawing and updating the compass. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CompassView extends View implements ApplicationManagerListener {
    private Paint mPaint = new Paint();
    private Path mPath = new Path();

    private static final float LINESPACING = 3f;

    /**
     * The current azimuth angle, with 0 = North, -90 = West, 90 = East
     */
    private float azimuth = -1f;
    private Bitmap compassBitmap;
    private GpsLocation loc;

    private Picture picture = null;
    private String timeString;
    private String lonString;
    private String latString;
    private String altimString;
    private String azimString;
    private ChartDrawer chartDrawer;

    // private int verticalAxisColor = Color.DKGRAY;
    // private int verticalAxisAlpha = 0;
    // private int verticalLabelsColor = Color.BLACK;
    // private int verticalLabelsAlpha = 255;
    private int horizontalAxisColor = Color.DKGRAY;
    private int horizontalAxisAlpha = 0;
    private int horizontalLabelsColor = Color.BLACK;
    private int horizontalLabelsAlpha = 255;
    private int chartColor = Color.RED;
    private int chartAlpha = 255;
    private int chartPointColor = Color.RED;
    private int chartPointAlpha = 255;
    private int backgroundColor = Color.LTGRAY;
    private int backgroundAlpha = 100;

    private DecimalFormat formatter = new DecimalFormat("0.00000"); //$NON-NLS-1$
    private String validPointsString;
    private String distanceString;
    private ApplicationManager applicationManager;

    public CompassView( GeoPaparazziActivity geopaparazziActivity ) {
        super(geopaparazziActivity);

        applicationManager = ApplicationManager.getInstance(getContext());

        float[] needle = Constants.COMPASS_NEEDLE_COORDINATES;

        for( int i = 0; i < needle.length; i = i + 2 ) {
            if (i == 0) {
                mPath.moveTo(needle[i], needle[i + 1]);
            } else {
                mPath.lineTo(needle[i], needle[i + 1]);
            }
        }
        mPath.close();

        BitmapDrawable compassDrawable = (BitmapDrawable) getResources().getDrawable(
                R.drawable.compass);
        compassDrawable.setBounds(0, 0, Constants.COMPASS_DIM_COMPASS,
                Constants.COMPASS_DIM_COMPASS);
        compassBitmap = compassDrawable.getBitmap();

        timeString = getResources().getString(R.string.utctime);
        lonString = getResources().getString(R.string.lon);
        latString = getResources().getString(R.string.lat);
        altimString = getResources().getString(R.string.altim);
        azimString = getResources().getString(R.string.azimuth);
        validPointsString = getResources().getString(R.string.log_points);
        distanceString = getResources().getString(R.string.log_distance);

        chartDrawer = new ChartDrawer("", ChartDrawer.LINE); //$NON-NLS-1$
    }

    protected void onDraw( Canvas canvas ) {

        // if (azimuth != -1) {
        Paint paint = mPaint;
        paint.setAntiAlias(true);

        if (picture == null) {
            picture = new Picture();
            Canvas recCanvas = picture.beginRecording(Constants.COMPASS_CANVAS_WIDTH,
                    Constants.COMPASS_CANVAS_HEIGHT);
            recCanvas.drawColor(Color.WHITE);
            recCanvas.drawBitmap(compassBitmap, Constants.COMPASS_X_POSITION,
                    Constants.COMPASS_Y_POSITION, paint);
            paint.setColor(Constants.COMPASS_NEEDLE_COLOR);
            paint.setAlpha(Constants.COMPASS_NEEDLE_ALPHA + 50);
            recCanvas.drawCircle(Constants.COMPASS_X_POSITION_CENTER,
                    Constants.COMPASS_Y_POSITION_CENTER, 4, paint);

            picture.endRecording();
        }
        picture.draw(canvas);
        paint.setColor(Constants.COMPASS_TEXT_COLOR);
        paint.setAlpha(255);
        paint.setTextSize(Constants.COMPASS_TEXT_SIZE);
        float x = 15f;
        float y = 30f;

        StringBuilder timeSb = new StringBuilder();
        StringBuilder latSb = new StringBuilder();
        StringBuilder lonSb = new StringBuilder();
        StringBuilder altimSb = new StringBuilder();
        StringBuilder azimuthSb = new StringBuilder();
        StringBuilder validPointSb = new StringBuilder();
        StringBuilder distanceSb = new StringBuilder();

        timeSb.append(timeString);
        latSb.append(latString);
        lonSb.append(lonString);
        altimSb.append(altimString);
        azimuthSb.append(azimString);
        validPointSb.append(validPointsString);
        distanceSb.append(distanceString);

        if (loc == null) {
            // Log.d("COMPASSVIEW", "Location from gps is null!");

            timeSb = new StringBuilder(getContext().getString(R.string.nogps_data));
            latSb = new StringBuilder(""); //$NON-NLS-1$
            lonSb = new StringBuilder(""); //$NON-NLS-1$
            altimSb = new StringBuilder(""); //$NON-NLS-1$
            azimuthSb = new StringBuilder(""); //$NON-NLS-1$
            validPointSb = new StringBuilder(""); //$NON-NLS-1$
            distanceSb = new StringBuilder(""); //$NON-NLS-1$
        } else {
            timeSb.append(" ").append(loc.getTimeString()); //$NON-NLS-1$
            latSb.append(" ").append(formatter.format(loc.getLatitude())); //$NON-NLS-1$
            lonSb.append(" ").append(formatter.format(loc.getLongitude())); //$NON-NLS-1$
            altimSb.append(" ").append((int) loc.getAltitude()); //$NON-NLS-1$
            azimuthSb.append(" ").append((int) azimuth); //$NON-NLS-1$
        }

        if (latSb.toString().length() == 0) {
            paint.setColor(Color.RED);
            paint.setAlpha(255);
            paint.setTextSize(16);
        }

        canvas.drawText(timeSb.toString(), x, y, paint);
        y = y + Constants.COMPASS_TEXT_SIZE + LINESPACING;
        canvas.drawText(lonSb.toString(), x, y, paint);
        y = y + Constants.COMPASS_TEXT_SIZE + LINESPACING;
        canvas.drawText(latSb.toString(), x, y, paint);
        y = y + Constants.COMPASS_TEXT_SIZE + LINESPACING;
        canvas.drawText(altimSb.toString(), x, y, paint);
        y = y + Constants.COMPASS_TEXT_SIZE + LINESPACING;
        canvas.drawText(azimuthSb.toString(), x, y, paint);

        if (applicationManager.isGpsLogging()) {
            y = Constants.COMPASS_DIM_COMPASS;
            validPointSb.append(" ").append(applicationManager.getCurrentRunningGpsLogPointsNum());
            canvas.drawText(validPointSb.toString(), x, y, paint);
            y = Constants.COMPASS_DIM_COMPASS - Constants.COMPASS_TEXT_SIZE - LINESPACING;
            distanceSb.append(" ").append(applicationManager.getCurrentRunningGpsLogDistance());
            canvas.drawText(distanceSb.toString(), x, y, paint);
        }

        drawChart(canvas);

        // draw needle
        if (azimuth != -1) {
            canvas.translate(Constants.COMPASS_X_POSITION_CENTER,
                    Constants.COMPASS_Y_POSITION_CENTER);
            paint.setColor(Color.RED);
            paint.setAlpha(150);
            paint.setStyle(Paint.Style.FILL);
            canvas.rotate((float) azimuth);
            canvas.drawPath(mPath, paint);
        }
    }

    private void drawChart( Canvas canvas ) {
        if (applicationManager.isGpsLogging()) {

            List<Float> last100Elevations = applicationManager.getLast100Elevations();
            float[] values = new float[last100Elevations.size()];
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;
            for( int i = 0; i < last100Elevations.size(); i++ ) {
                values[i] = last100Elevations.get(i);
                if (values[i] > max) {
                    max = values[i];
                }
                if (values[i] < min) {
                    min = values[i];
                }
            }
            if (values.length == 0) {
                values = new float[10];
                min = 0;
                max = 1000;
                for( int i = 0; i < values.length; i++ ) {
                    if (i < 5) {
                        values[i] = i * 100;
                    } else {
                        values[i] = i * 50;
                    }
                }
            }

            float border = 4;
            float chartHeight = 100;
            chartDrawer.setProperties(horizontalAxisColor, horizontalAxisAlpha,
                    horizontalLabelsColor, horizontalLabelsAlpha, chartColor, chartAlpha,
                    chartPointColor, chartPointAlpha, backgroundColor, backgroundAlpha);
            chartDrawer.drawCart(canvas, border, Constants.COMPASS_CANVAS_HEIGHT - chartHeight
                    - border, Constants.COMPASS_CANVAS_WIDTH - 2 * border, chartHeight - border,
                    max, min, new String[]{"", ""}, //$NON-NLS-1$//$NON-NLS-2$
                    new String[]{"", ""}, null, values, 2); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public void onLocationChanged( GpsLocation loc ) {
        if (loc != null) {
            this.loc = loc;
        }
        invalidate();
    }

    public void onSensorChanged( double azimuth ) {
        this.azimuth = (float) azimuth;
        invalidate();
    }

}