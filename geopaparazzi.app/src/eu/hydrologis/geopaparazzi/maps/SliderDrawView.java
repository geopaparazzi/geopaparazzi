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

import static java.lang.Math.abs;
import static java.lang.Math.round;

import java.util.ArrayList;
import java.util.List;

import jsqlite.Exception;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maps.overlays.SliderDrawProjection;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * A slider view to draw on.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SliderDrawView extends View {
    private static final int TOUCH_BOX_THRES = 10;

    private MapView mapView;
    private final Paint measurePaint = new Paint();
    private final Paint measureTextPaint = new Paint();
    private final Path measurePath = new Path();

    private final Paint infoRectPaintStroke = new Paint();
    private final Paint infoRectPaintFill = new Paint();
    private final Rect rect = new Rect();

    private float currentX;
    private float currentY;
    private float lastX = -1;
    private float lastY = -1;

    private boolean doImperial = false;

    private float measuredDistance = Float.NaN;
    private String distanceString;

    private final Point tmpP = new Point();
    private final Point startP = new Point();

    private boolean doMeasureMode = false;
    private boolean doInfoMode = false;
    private float left;
    private float right;
    private float bottom;
    private float top;
    private SliderDrawProjection sliderDrawProjection;

    private StringBuilder textBuilder = new StringBuilder();
    private ProgressDialog infoProgressDialog;

    /**
     * Constructor.
     * 
     * @param context  the context to use.
     * @param attrs the attributes.
     */
    public SliderDrawView( Context context, AttributeSet attrs ) {
        super(context, attrs);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        doImperial = preferences.getBoolean(Constants.PREFS_KEY_IMPERIAL, false);

        measurePaint.setAntiAlias(true);
        measurePaint.setColor(Color.DKGRAY);
        measurePaint.setStrokeWidth(3f);
        measurePaint.setStyle(Paint.Style.STROKE);

        infoRectPaintFill.setAntiAlias(true);
        infoRectPaintFill.setColor(Color.BLUE);
        infoRectPaintFill.setAlpha(80);
        infoRectPaintFill.setStyle(Paint.Style.FILL);
        infoRectPaintStroke.setAntiAlias(true);
        infoRectPaintStroke.setStrokeWidth(1.5f);
        infoRectPaintStroke.setColor(Color.BLUE);
        infoRectPaintStroke.setStyle(Paint.Style.STROKE);

        measureTextPaint.setAntiAlias(true);
        int pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        measureTextPaint.setTextSize(pixel);
        measureTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

        distanceString = context.getString(R.string.distance);// context.getResources().getString(R.string.distance);
    }

    protected void onDraw( Canvas canvas ) {
        super.onDraw(canvas);

        if (mapView == null || mapView.isClickable()) {
            return;
        }

        if (doMeasureMode) {
            int cWidth = canvas.getWidth();
            // RectF retfF = new RectF();
            // measurePath.computeBounds(retfF, true);
            // GPLog.androidLog(-1, "DRAWINFOLINE: " + retfF);
            canvas.drawPath(measurePath, measurePaint);
            int upper = 70;
            int delta = 5;
            measureTextPaint.getTextBounds(distanceString, 0, distanceString.length(), rect);
            int textWidth = rect.width();
            int textHeight = rect.height();
            int x = cWidth / 2 - textWidth / 2;
            canvas.drawText(distanceString, x, upper, measureTextPaint);
            textBuilder.setLength(0);
            if (doImperial) {
                double distanceInFeet = Utilities.toFeet(measuredDistance);
                textBuilder.append(String.valueOf((int) distanceInFeet));
                textBuilder.append(" ft");
            } else {
                textBuilder.append(String.valueOf((int) measuredDistance));
                textBuilder.append(" m");
            }
            String distanceText = textBuilder.toString();
            measureTextPaint.getTextBounds(distanceText, 0, distanceText.length(), rect);
            textWidth = rect.width();
            x = cWidth / 2 - textWidth / 2;
            canvas.drawText(distanceText, x, upper + delta + textHeight, measureTextPaint);
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry(this, "Drawing measure path text: " + upper); //$NON-NLS-1$
        } else if (doInfoMode) {
            // GPLog.androidLog(-1, "DRAWINFOBOX: " + rect);
            canvas.drawRect(rect, infoRectPaintFill);
            canvas.drawRect(rect, infoRectPaintStroke);
        }
    }

    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        if (mapView == null || mapView.isClickable()) {
            return false;
        }

        if (doInfoMode) {

            Projection pj = sliderDrawProjection;

            // handle drawing
            currentX = event.getX();
            currentY = event.getY();

            int action = event.getAction();
            switch( action ) {
            case MotionEvent.ACTION_DOWN:
                GeoPoint startGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                pj.toPixels(startGeoPoint, startP);

                lastX = currentX;
                lastY = currentY;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = currentX - lastX;
                float dy = currentY - lastY;
                if (abs(dx) < 1 && abs(dy) < 1) {
                    lastX = currentX;
                    lastY = currentY;
                    return true;
                }
                GeoPoint currentGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                pj.toPixels(currentGeoPoint, tmpP);

                left = Math.min(tmpP.x, startP.x);
                right = Math.max(tmpP.x, startP.x);
                bottom = Math.max(tmpP.y, startP.y);
                top = Math.min(tmpP.y, startP.y);
                rect.set((int) left, (int) top, (int) right, (int) bottom);

                invalidate();
                break;
            case MotionEvent.ACTION_UP:

                float deltaY = abs(top - bottom);
                float deltaX = abs(right - left);
                if (deltaX > TOUCH_BOX_THRES && deltaY > TOUCH_BOX_THRES) {
                    GeoPoint ul = pj.fromPixels((int) left, (int) top);
                    GeoPoint lr = pj.fromPixels((int) right, (int) bottom);

                    infoDialog(ul.getLatitude(), ul.getLongitude(), lr.getLatitude(), lr.getLongitude());
                }

                if (GPLog.LOG_HEAVY)
                    GPLog.addLogEntry(this, "UNTOUCH: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$//$NON-NLS-2$
                break;
            }
        }
        if (doMeasureMode) {

            Projection pj = mapView.getProjection();
            // handle drawing
            currentX = event.getX();
            currentY = event.getY();

            tmpP.set(round(currentX), round(currentY));

            int action = event.getAction();
            switch( action ) {
            case MotionEvent.ACTION_DOWN:
                measuredDistance = 0;
                measurePath.reset();
                GeoPoint firstGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                pj.toPixels(firstGeoPoint, tmpP);
                measurePath.moveTo(tmpP.x, tmpP.y);

                lastX = currentX;
                lastY = currentY;

                if (GPLog.LOG_HEAVY)
                    GPLog.addLogEntry(this, "TOUCH: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$//$NON-NLS-2$
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = currentX - lastX;
                float dy = currentY - lastY;
                if (abs(dx) < 1 && abs(dy) < 1) {
                    lastX = currentX;
                    lastY = currentY;
                    return true;
                }

                GeoPoint currentGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                pj.toPixels(currentGeoPoint, tmpP);
                measurePath.lineTo(tmpP.x, tmpP.y);
                if (GPLog.LOG_HEAVY)
                    GPLog.addLogEntry(this, "DRAG: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$ //$NON-NLS-2$
                // the measurement
                GeoPoint previousGeoPoint = pj.fromPixels(round(lastX), round(lastY));

                Location l1 = new Location("gps"); //$NON-NLS-1$
                l1.setLatitude(previousGeoPoint.getLatitude());
                l1.setLongitude(previousGeoPoint.getLongitude());
                Location l2 = new Location("gps"); //$NON-NLS-1$
                l2.setLatitude(currentGeoPoint.getLatitude());
                l2.setLongitude(currentGeoPoint.getLongitude());

                float distanceTo = l1.distanceTo(l2);
                lastX = currentX;
                lastY = currentY;
                measuredDistance = measuredDistance + distanceTo;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (GPLog.LOG_HEAVY)
                    GPLog.addLogEntry(this, "UNTOUCH: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$//$NON-NLS-2$
                break;
            }
        }
        return true;
    }

    private void infoDialog( final double n, final double w, final double s, final double e ) {
        try {
            final SpatialDatabasesManager sdbManager = SpatialDatabasesManager.getInstance();
            List<SpatialVectorTable> spatialTables = sdbManager.getSpatialVectorTables(false);
            double[] boundsCoordinates=new double[]{w,s,e,n};
            final List<SpatialVectorTable> visibleTables = new ArrayList<SpatialVectorTable>();
            for( SpatialVectorTable spatialTable : spatialTables ) {
                if (spatialTable.getStyle().enabled == 0)  {
                    continue;
                }
                // do not add tables that are out of range
                if (!spatialTable.checkBounds(boundsCoordinates))  {
                    continue;
                }
                visibleTables.add(spatialTable);
            }

            final Context context = getContext();
            infoProgressDialog = new ProgressDialog(context);
            infoProgressDialog.setCancelable(true);
            infoProgressDialog.setTitle("INFO");
            infoProgressDialog.setMessage("Extracting information...");
            infoProgressDialog.setCancelable(true);
            infoProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            infoProgressDialog.setProgress(0);
            infoProgressDialog.setMax(visibleTables.size());
            infoProgressDialog.show();

            new AsyncTask<String, Integer, String>(){

                protected String doInBackground( String... params ) {
                    try {
                        boolean oneEnabled = visibleTables.size() > 0;
                        StringBuilder sb = new StringBuilder();
                        if (oneEnabled) {
                            double north = n;
                            double south = s;
                            if (n - s == 0) {
                                south = n - 1;
                            }
                            double west = w;
                            double east = e;
                            if (e - w == 0) {
                                west = e - 1;
                            }

                            for( SpatialVectorTable spatialTable : visibleTables ) {
                                StringBuilder sbTmp = new StringBuilder();
                                sdbManager.intersectionToString("4326", spatialTable, north, south, east, west, sbTmp, "\t");
                                if (sbTmp.length() > 0)
                                { // do not add empty results
                                 sb.append(spatialTable.getTableName()).append("\n");
                                 sb.append(sbTmp);
                                 sb.append("\n----------------------\n");
                                }
                                publishProgress(1);
                                // Escape early if cancel() is called
                                if (isCancelled())
                                    break;
                            }
                        }
                        if (sb.length() > 0)
                         return sb.toString();
                        else
                         return "no results";
                    } catch (Exception e) { 
                        return "ERROR: " + e.getLocalizedMessage();
                    }

                }

                protected void onProgressUpdate( Integer... progress ) { // on UI thread!
                    if (infoProgressDialog != null && infoProgressDialog.isShowing())
                        infoProgressDialog.incrementProgressBy(progress[0]);
                }

                protected void onPostExecute( String response ) { // on UI thread!
                    Utilities.dismissProgressDialog(infoProgressDialog);
                    if (response.startsWith("ERROR")) {
                        Utilities.messageDialog(context, response, null);
                    } else {
                        Utilities.messageDialog(context, response, null);
                    }
                }

            }.execute((String) null);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Disable measure mode. 
     */
    public void disableMeasureMode() {
        doMeasureMode = false;
        this.mapView = null;
        measuredDistance = 0;
        measurePath.reset();
        invalidate();
    }

    /**
     * Enable measure mode. 
     *
     * @param mapView the mapview.
     */
    public void enableMeasureMode( MapView mapView ) {
        this.mapView = mapView;
        doMeasureMode = true;
    }

    /**
     * Disable info mode.
     */
    public void disableInfo() {
        doInfoMode = false;
        this.mapView = null;
        rect.set(0, 0, 0, 0);
        invalidate();
    }

    /**
     * Enable info mode. 
     *
     * @param mapView the mapview.
     */
    public void enableInfo( MapView mapView ) {
        this.mapView = mapView;
        doInfoMode = true;

        sliderDrawProjection = new SliderDrawProjection(mapView, this);
    }
}
