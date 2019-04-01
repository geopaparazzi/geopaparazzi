///*
// * Geopaparazzi - Digital field mapping on Android based devices
// * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package eu.geopaparazzi.core.maptools.tools;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.graphics.Rect;
//import android.graphics.Typeface;
//import android.location.Location;
//import android.preference.PreferenceManager;
//import android.util.TypedValue;
//import android.view.MotionEvent;
//
//import org.hortonmachine.dbs.utils.MercatorUtils;
//import org.mapsforge.core.graphics.Canvas;
//import org.mapsforge.core.graphics.FontFamily;
//import org.mapsforge.core.graphics.FontStyle;
//import org.mapsforge.core.graphics.Paint;
//import org.mapsforge.core.graphics.Path;
//import org.mapsforge.core.graphics.Style;
//import org.mapsforge.core.model.LatLong;
//import org.mapsforge.core.model.Point;
//import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
//import org.mapsforge.map.android.view.MapView;
//import org.mapsforge.map.util.MapViewProjection;
//
//import eu.geopaparazzi.library.database.GPLog;
//import eu.geopaparazzi.core.features.EditManager;
//import eu.geopaparazzi.library.style.ToolColors;
//import eu.geopaparazzi.core.GeopaparazziApplication;
//import eu.geopaparazzi.core.R;
//import eu.geopaparazzi.core.maptools.MapTool;
//import eu.geopaparazzi.core.utilities.Constants;
//import eu.geopaparazzi.mapsforge.utils.MapsforgeUtils;
//
//import static java.lang.Math.abs;
//import static java.lang.Math.round;
//
///**
// * A tool to measure by means of drawing on the map.
// *
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TapMeasureTool extends MapTool {
//    private final Paint measurePaint = AndroidGraphicFactory.INSTANCE.createPaint();
//    private final Paint measureTextPaint = AndroidGraphicFactory.INSTANCE.createPaint();
//    private Path measurePath = AndroidGraphicFactory.INSTANCE.createPath();
//
//    private float measuredDistance = Float.NaN;
//    private String distanceString;
//
//    private float lastX = -1;
//    private float lastY = -1;
//
//
//    private final Rect rect = new Rect();
//
//    private StringBuilder textBuilder = new StringBuilder();
//    private boolean doImperial = false;
//
//    /**
//     * Constructor.
//     *
//     * @param mapView the mapview reference.
//     */
//    public TapMeasureTool(MapView mapView) {
//        super(mapView);
//
//        Context context = GeopaparazziApplication.getInstance().getApplicationContext();
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//        doImperial = preferences.getBoolean(Constants.PREFS_KEY_IMPERIAL, false);
//
//        int stroke = MapsforgeUtils.toColor("#212121", -1);
//
////        measurePaint.setAntiAlias(true);
//        measurePaint.setColor(stroke);
//        measurePaint.setStrokeWidth(3f);
//        measurePaint.setStyle(Style.STROKE);
//
////        measureTextPaint.setAntiAlias(true);
//        int pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
//        measureTextPaint.setTextSize(pixel);
//        measureTextPaint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
//
//        distanceString = context.getString(R.string.distance);
//    }
//
//    public void activate() {
//        if (mapView != null)
//            mapView.setClickable(false);
//    }
//
//    public void onToolDraw(Canvas canvas) {
//        int cWidth = canvas.getWidth();
//        // RectF retfF = new RectF();
//        // measurePath.computeBounds(retfF, true);
//        // GPLog.androidLog(-1, "DRAWINFOLINE: " + retfF);
//        canvas.drawPath(measurePath, measurePaint);
//        int upper = 70;
//        int delta = 5;
//        int textWidth = measureTextPaint.getTextWidth(distanceString);
//        int textHeight = measureTextPaint.getTextHeight(distanceString);
//        int x = cWidth / 2 - textWidth / 2;
//        canvas.drawText(distanceString, x, upper, measureTextPaint);
//        textBuilder.setLength(0);
//        if (doImperial) {
//            double distanceInFeet = MercatorUtils.toFeet(measuredDistance);
//            textBuilder.append(String.valueOf((int) distanceInFeet));
//            textBuilder.append(" ft"); //$NON-NLS-1$
//        } else {
//            textBuilder.append(String.valueOf((int) measuredDistance));
//            textBuilder.append(" m"); //$NON-NLS-1$
//        }
//        String distanceText = textBuilder.toString();
//        textWidth = measureTextPaint.getTextWidth(distanceText);
//        x = cWidth / 2 - textWidth / 2;
//        canvas.drawText(distanceText, x, upper + delta + textHeight, measureTextPaint);
//        if (GPLog.LOG_HEAVY)
//            GPLog.addLogEntry(this, "Drawing measure path text: " + upper); //$NON-NLS-1$
//
//    }
//
//    public boolean onToolTouchEvent(MotionEvent event) {
//        if (mapView == null || mapView.isClickable()) {
//            return false;
//        }
//
//        MapViewProjection pj = mapView.getMapViewProjection();
//        // handle drawing
//        float currentX = event.getX();
//        float currentY = event.getY();
//
//        int action = event.getAction();
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                measuredDistance = 0;
//                measurePath.clear(); // TODO check
//                LatLong firstGeoPoint = pj.fromPixels(round(currentX), round(currentY));
//                Point tmpP = pj.toPixels(firstGeoPoint);
//                measurePath.moveTo((float) tmpP.x, (float) tmpP.y);
//
//                lastX = currentX;
//                lastY = currentY;
//
//                if (GPLog.LOG_HEAVY)
//                    GPLog.addLogEntry(this, "TOUCH: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$//$NON-NLS-2$
//                break;
//            case MotionEvent.ACTION_MOVE:
//                float dx = currentX - lastX;
//                float dy = currentY - lastY;
//                if (abs(dx) < 1 && abs(dy) < 1) {
//                    lastX = currentX;
//                    lastY = currentY;
//                    return true;
//                }
//
//                LatLong currentGeoPoint = pj.fromPixels(round(currentX), round(currentY));
//                Point tmpP2 = pj.toPixels(currentGeoPoint);
//                measurePath.lineTo((float) tmpP2.x, (float) tmpP2.y);
//                if (GPLog.LOG_HEAVY)
//                    GPLog.addLogEntry(this, "DRAG: " + tmpP2.x + "/" + tmpP2.y); //$NON-NLS-1$ //$NON-NLS-2$
//                // the measurement
//                LatLong previousGeoPoint = pj.fromPixels(round(lastX), round(lastY));
//
//                Location l1 = new Location("gps"); //$NON-NLS-1$
//                l1.setLatitude(previousGeoPoint.getLatitude());
//                l1.setLongitude(previousGeoPoint.getLongitude());
//                Location l2 = new Location("gps"); //$NON-NLS-1$
//                l2.setLatitude(currentGeoPoint.getLatitude());
//                l2.setLongitude(currentGeoPoint.getLongitude());
//
//                float distanceTo = l1.distanceTo(l2);
//                lastX = currentX;
//                lastY = currentY;
//                measuredDistance = measuredDistance + distanceTo;
//                EditManager.INSTANCE.invalidateEditingView();
//                break;
//            case MotionEvent.ACTION_UP:
//                if (GPLog.LOG_HEAVY)
//                    GPLog.addLogEntry(this, "UNTOUCH: " + currentX + "/" + currentY); //$NON-NLS-1$//$NON-NLS-2$
//                break;
//        }
//
//        return true;
//    }
//
//    public void disable() {
//        if (mapView != null) {
//            mapView.setClickable(true);
//            mapView = null;
//        }
//        measuredDistance = 0;
//        measurePath = null;
//    }
//
//    @Override
//    public void onViewChanged() {
//        // ignore
//    }
//}
