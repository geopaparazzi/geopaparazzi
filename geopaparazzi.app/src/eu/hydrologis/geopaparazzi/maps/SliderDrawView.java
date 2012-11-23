package eu.hydrologis.geopaparazzi.maps;

import static java.lang.Math.abs;
import static java.lang.Math.round;

import java.util.List;

import jsqlite.Exception;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import eu.geopaparazzi.library.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.library.database.spatial.core.SpatialTable;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.hydrologis.geopaparazzi.R;

public class SliderDrawView extends View {
    private MapView mapView;
    private final Paint measurePaint = new Paint();
    private final Paint measureTextPaint = new Paint();

    private final Path measurePath = new Path();
    private float currentX;
    private float currentY;
    private float lastX = -1;
    private float lastY = -1;

    // private boolean imperial = false;
    // private boolean nautical = false;

    private float measuredDistance = Float.NaN;
    private String distanceString;
    // private final ResourceProxy resourceProxy;

    private final Point tmpP = new Point();
    private final Rect rect = new Rect();

    private boolean doMeasureMode = false;
    private boolean doInfoMode = false;

    public SliderDrawView( Context context, AttributeSet attrs ) {
        super(context, attrs);

        measurePaint.setAntiAlias(true);
        measurePaint.setColor(Color.DKGRAY);
        measurePaint.setStrokeWidth(3f);
        measurePaint.setStyle(Paint.Style.STROKE);

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
            canvas.drawPath(measurePath, measurePaint);
            int upper = 70;
            int delta = 5;
            measureTextPaint.getTextBounds(distanceString, 0, distanceString.length(), rect);
            int textWidth = rect.width();
            int textHeight = rect.height();
            int x = cWidth / 2 - textWidth / 2;
            canvas.drawText(distanceString, x, upper, measureTextPaint);
            String distanceText = String.valueOf((int) measuredDistance);
            // String distanceText = distanceText((int) measuredDistance, imperial, nautical);
            measureTextPaint.getTextBounds(distanceText, 0, distanceText.length(), rect);
            textWidth = rect.width();
            x = cWidth / 2 - textWidth / 2;
            canvas.drawText(distanceText, x, upper + delta + textHeight, measureTextPaint);
            if (Debug.D)
                Logger.d(this, "Drawing measure path text: " + upper); //$NON-NLS-1$
        }
    }

    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        if (mapView == null || mapView.isClickable()) {
            return false;
        }

        if (doInfoMode) {
            int action = event.getAction();
            switch( action ) {
            case MotionEvent.ACTION_UP:
                Projection pj = mapView.getProjection();
                float x = event.getX();
                float y = event.getY();
                int delta = 10;
                GeoPoint p = pj.fromPixels(round(x), round(y));
                GeoPoint ul = pj.fromPixels(round(x - delta), round(y - delta));
                GeoPoint lr = pj.fromPixels(round(x + delta), round(y + delta));
                double yW = p.getLatitude();
                double xW = p.getLongitude();
                double n = ul.getLatitude();
                double w = ul.getLongitude();
                double s = lr.getLatitude();
                double e = lr.getLongitude();
                infoDialog(xW, yW, n, w, s, e);

                if (Debug.D)
                    Logger.d(this, "UNTOUCH: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$//$NON-NLS-2$
                break;
            }
        }
        if (doMeasureMode) {

            Projection pj = mapView.getProjection();
            // handle drawing
            currentX = event.getX();
            currentY = event.getY();

            tmpP.set(round(currentX), round(currentY));

            // if (lastX == -1 || lastY == -1) {
            // // lose the first drag and set the delta
            // lastX = currentX;
            // lastY = currentY;
            // return true;
            // }

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

                if (Debug.D)
                    Logger.d(this, "TOUCH: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$//$NON-NLS-2$
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
                if (Debug.D)
                    Logger.d(this, "DRAG: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$ //$NON-NLS-2$
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
                if (Debug.D)
                    Logger.d(this, "UNTOUCH: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$//$NON-NLS-2$
                break;
            }
        }
        return true;
    }

    private void infoDialog( final double xW, final double yW, final double n, final double w, final double s, final double e ) {
        try {
            final SpatialDatabasesManager sdbManager = SpatialDatabasesManager.getInstance();
            final List<SpatialTable> spatialTables = sdbManager.getSpatialTables(false);

            final Context context = getContext();
            final ProgressDialog importDialog = new ProgressDialog(context);
            importDialog.setCancelable(true);
            importDialog.setTitle("INFO");
            importDialog.setMessage("Extracting information...");
            importDialog.setCancelable(true);
            importDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            importDialog.setProgress(0);
            importDialog.setMax(spatialTables.size());
            importDialog.show();

            new AsyncTask<String, Void, String>(){

                protected String doInBackground( String... params ) {
                    try {
                        StringBuilder sb = new StringBuilder();
                        for( SpatialTable spatialTable : spatialTables ) {
                            if (spatialTable.style.enabled == 0) {
                                continue;
                            }
                            StringBuilder sbTmp = new StringBuilder();
                            if (spatialTable.isPolygon()) {
                                // query just the point
                                sdbManager.intersectionToString("4326", spatialTable, yW, xW, sbTmp, "\t");
                            } else {
                                sdbManager.intersectionToString("4326", spatialTable, n, s, e, w, sbTmp, "\t");
                            }
                            sb.append(spatialTable.name).append("\n");
                            sb.append(sbTmp);
                            sb.append("\n----------------------\n");

                            onProgressUpdate(0);
                        }
                        return sb.toString();
                    } catch (Exception e) {
                        return "ERROR: " + e.getLocalizedMessage();
                    }

                }

                protected void onProgressUpdate( Integer... progress ) { // on UI thread!
                    importDialog.incrementProgressBy(1);
                }

                protected void onPostExecute( String response ) { // on UI thread!
                    importDialog.dismiss();
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
    public void disableMeasureMode() {
        doMeasureMode = false;
        this.mapView = null;
        measuredDistance = 0;
        measurePath.reset();
        invalidate();
    }

    public void enableMeasureMode( MapView mapView ) {
        this.mapView = mapView;
        doMeasureMode = true;
    }

    public void disableInfo() {
        doInfoMode = false;
        this.mapView = null;
    }

    public void enableInfo( MapView mapView ) {
        this.mapView = mapView;
        doInfoMode = true;
    }
}
