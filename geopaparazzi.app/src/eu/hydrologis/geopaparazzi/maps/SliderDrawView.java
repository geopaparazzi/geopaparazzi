package eu.hydrologis.geopaparazzi.maps;

import static java.lang.Math.abs;
import static java.lang.Math.round;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import eu.geopaparazzi.library.R;

public class SliderDrawView extends View {
    private MapView mapView;
    private final Paint measurePaint = new Paint();
    private final Paint measureTextPaint = new Paint();
    private boolean doDraw = true;
    private boolean isOn = false;

    private final Path measurePath = new Path();
    private int currentX;
    private int currentY;
    private int lastX;
    private int lastY;

    // private boolean imperial = false;
    // private boolean nautical = false;

    private float measuredDistance = Float.NaN;
    private String distanceString;
    // private final ResourceProxy resourceProxy;

    private final Point tmpP = new Point();

    public SliderDrawView( Context context, AttributeSet attrs ) {
        super(context, attrs);

        measurePaint.setAntiAlias(true);
        measurePaint.setColor(Color.DKGRAY);
        measurePaint.setStrokeWidth(3f);
        measurePaint.setStyle(Paint.Style.STROKE);

        measureTextPaint.setAntiAlias(true);
        measureTextPaint.setTextSize(12);
        // measureTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

        distanceString = "Distance: ";// context.getResources().getString(R.string.distance);
    }

    protected void onDraw( Canvas canvas ) {
        super.onDraw(canvas);

        try {
            Paint paint = new Paint();
            // paint.setStyle(Paint.Style.FILL);
            // paint.setColor(Color.BLACK);
            // canvas.drawPaint(paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(100, 100, 30, paint);
        } catch (Exception e) {

        }

        if (mapView == null || mapView.isClickable()) {
            return;
        }

        canvas.drawPath(measurePath, measurePaint);

        // Projection pj = mapView.getProjection();
        // GeoPoint mapCenter = mapView.getMapPosition().getMapCenter();
        // Point center = pj.toPixels(mapCenter, null);
        // BoundingBoxE6 boundingBox = mapView.getBoundingBox();
        // int latNorthE6 = boundingBox.getLatNorthE6();
        // int latText = latNorthE6 - (latNorthE6 - mapCenter.getLatitudeE6()) / 3;
        //
        // Point textPoint = pj.toMapPixels(new GeoPoint(latText, mapCenter.getLongitudeE6()),
        // null);
        //
        // int upper = textPoint.y;
        // int delta = 5;
        // Rect rect = new Rect();
        // measureTextPaint.getTextBounds(distanceString, 0, distanceString.length(), rect);
        // int textWidth = rect.width();
        // int textHeight = rect.height();
        // int x = center.x - textWidth / 2;
        // canvas.drawText(distanceString, x, upper, measureTextPaint);
        //
        // String distanceText = String.valueOf((int) measuredDistance);
        // // String distanceText = distanceText((int) measuredDistance, imperial, nautical);
        // measureTextPaint.getTextBounds(distanceText, 0, distanceText.length(), rect);
        // textWidth = rect.width();
        // x = center.x - textWidth / 2;
        // canvas.drawText(distanceText, x, upper + delta + textHeight, measureTextPaint);
        //
        // if (Debug.D)
        //            Logger.d(this, "Drawing measure path text: " + upper); //$NON-NLS-1$
    }

    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        // TODO Auto-generated method stub

        if (mapView == null || mapView.isClickable()) {
            return true;
        }

        Projection pj = mapView.getProjection();
        // handle drawing
        currentX = (int) round(event.getX());
        currentY = (int) round(event.getY());
        // Logger.d(this, "point: " + currentX + "/" + currentY);

        if (lastX == -1 || lastY == -1) {
            // lose the first drag and set the delta
            lastX = currentX;
            lastY = currentY;
            return true;
        }

        int action = event.getAction();
        switch( action ) {
        case MotionEvent.ACTION_DOWN:
            // Logger.d(this, "First point....");
            measuredDistance = 0;
            measurePath.reset();
            GeoPoint firstGeoPoint = pj.fromPixels(currentX, currentY);
            pj.toPixels(firstGeoPoint, tmpP);
            measurePath.moveTo(tmpP.x, tmpP.y);
            break;
        case MotionEvent.ACTION_MOVE:
            int dx = currentX - lastX;
            int dy = currentY - lastY;
            if (abs(dx) < 1 && abs(dy) < 1) {
                lastX = currentX;
                lastY = currentY;
                return true;
            }
            GeoPoint currentGeoPoint = pj.fromPixels(currentX, currentY);
            pj.toPixels(currentGeoPoint, tmpP);
            measurePath.lineTo(tmpP.x, tmpP.y);
            // the measurement
            GeoPoint previousGeoPoint = pj.fromPixels(lastX, lastY);

            Location l1 = new Location("gps");
            l1.setLatitude(previousGeoPoint.getLatitude());
            l1.setLongitude(previousGeoPoint.getLongitude());
            Location l2 = new Location("gps");
            l2.setLatitude(currentGeoPoint.getLatitude());
            l2.setLongitude(currentGeoPoint.getLongitude());

            float distanceTo = l1.distanceTo(l2);
            lastX = currentX;
            lastY = currentY;
            measuredDistance = measuredDistance + distanceTo;
            // Logger.d(this, "Recording points. Distance = " + measuredDistance);
            invalidate();
            break;
        case MotionEvent.ACTION_UP:
            break;
        }
        return super.onTouchEvent(event);
    }

    public void setMapView( MapView mapView ) {
        this.mapView = mapView;
    }
}
