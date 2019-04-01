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
//import android.content.Intent;
//import android.graphics.Rect;
//import android.os.Parcelable;
//import android.view.MotionEvent;
//
//import org.mapsforge.core.graphics.Canvas;
//import org.mapsforge.core.graphics.Paint;
//import org.mapsforge.core.graphics.Style;
//import org.mapsforge.core.model.LatLong;
//import org.mapsforge.core.model.Point;
//import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
//import org.mapsforge.map.android.view.MapView;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import eu.geopaparazzi.library.core.maps.SpatialiteMap;
//import eu.geopaparazzi.library.style.ToolColors;
//import eu.geopaparazzi.library.database.GPLog;
//import eu.geopaparazzi.core.features.EditManager;
//import eu.geopaparazzi.library.features.Feature;
//import eu.geopaparazzi.core.features.ToolGroup;
//import eu.geopaparazzi.library.style.ColorUtilities;
//import eu.geopaparazzi.library.util.GPDialogs;
//import eu.geopaparazzi.library.util.LibraryConstants;
//import eu.geopaparazzi.library.util.StringAsyncTask;
//import eu.geopaparazzi.mapsforge.utils.MapsforgeUtils;
//import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
//import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
//import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
//import eu.geopaparazzi.core.R;
//import eu.geopaparazzi.core.maptools.FeaturePagerActivity;
//import eu.geopaparazzi.core.maptools.FeatureUtilities;
//import eu.geopaparazzi.core.maptools.MapTool;
//import eu.geopaparazzi.mapsforge.core.proj.SliderDrawProjection;
//import jsqlite.Exception;
//
//import static java.lang.Math.abs;
//import static java.lang.Math.round;
//
///**
// * A tool to query data.
// *
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class InfoTool extends MapTool {
//    private static final int TOUCH_BOX_THRES = 10;
//
//    private final Paint infoRectPaintStroke = AndroidGraphicFactory.INSTANCE.createPaint();
//    private final Paint infoRectPaintFill = AndroidGraphicFactory.INSTANCE.createPaint();
//    private final Rect rect = new Rect();
//
//    private float currentX;
//    private float currentY;
//    private float lastX = -1;
//    private float lastY = -1;
//
//    private Point tmpP ;
//    private Point startP ;
//
//    private double left;
//    private double right;
//    private double bottom;
//    private double top;
//
//
//    private SliderDrawProjection sliderDrawProjection;
//
//    private ToolGroup parentGroup;
//
//    /**
//     * Constructor.
//     *
//     * @param parentGroup the parent group.
//     * @param mapView     the mapview reference.
//     */
//    public InfoTool(ToolGroup parentGroup, MapView mapView) {
//        super(mapView);
//        this.parentGroup = parentGroup;
//        sliderDrawProjection = new SliderDrawProjection(mapView, EditManager.INSTANCE.getEditingView());
//
//        int stroke = MapsforgeUtils.toColor(ToolColors.infoselection_stroke.getHex(), -1);
//        int fill = MapsforgeUtils.toColor(ToolColors.infoselection_fill.getHex(), 80);
////        infoRectPaintFill.setAntiAlias(true);
//        infoRectPaintFill.setColor(fill);
//        infoRectPaintFill.setStyle(Style.FILL);
////        infoRectPaintStroke.setAntiAlias(true);
//        infoRectPaintStroke.setStrokeWidth(1.5f);
//        infoRectPaintStroke.setColor(stroke);
//        infoRectPaintStroke.setStyle(Style.STROKE);
//    }
//
//    public void activate() {
//        if (mapView != null)
//            mapView.setClickable(false);
//    }
//
//    public void onToolDraw(Canvas canvas) {
//        MapsforgeUtils.drawRect(canvas, rect, infoRectPaintFill);
//        MapsforgeUtils.drawRect(canvas, rect, infoRectPaintStroke);
//    }
//
//    public boolean onToolTouchEvent(MotionEvent event) {
//        if (mapView == null || mapView.isClickable()) {
//            return false;
//        }
//        SliderDrawProjection pj = sliderDrawProjection;
//
//        // handle drawing
//        currentX = event.getX();
//        currentY = event.getY();
//
//        int action = event.getAction();
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                LatLong startGeoPoint = pj.fromPixels(round(currentX), round(currentY));
//                startP = pj.toPixels(startGeoPoint);
//
//                lastX = currentX;
//                lastY = currentY;
//                break;
//            case MotionEvent.ACTION_MOVE:
//                float dx = currentX - lastX;
//                float dy = currentY - lastY;
//                if (abs(dx) < 1 && abs(dy) < 1) {
//                    lastX = currentX;
//                    lastY = currentY;
//                    return true;
//                }
//                LatLong currentGeoPoint = pj.fromPixels(round(currentX), round(currentY));
//                tmpP = pj.toPixels(currentGeoPoint);
//
//                left = Math.min(tmpP.x, startP.x);
//                right = Math.max(tmpP.x, startP.x);
//                bottom = Math.max(tmpP.y, startP.y);
//                top = Math.min(tmpP.y, startP.y);
//                rect.set((int) left, (int) top, (int) right, (int) bottom);
//
//                EditManager.INSTANCE.invalidateEditingView();
//                break;
//            case MotionEvent.ACTION_UP:
//
//                double deltaY = abs(top - bottom);
//                double deltaX = abs(right - left);
//                if (deltaX > TOUCH_BOX_THRES && deltaY > TOUCH_BOX_THRES) {
//                    LatLong ul = pj.fromPixels(left, top);
//                    LatLong lr = pj.fromPixels(right, bottom);
//
//                    infoDialog(ul.getLatitude(), ul.getLongitude(), lr.getLatitude(), lr.getLongitude());
//                }
//
//                if (GPLog.LOG_HEAVY)
//                    GPLog.addLogEntry(this, "UNTOUCH: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$//$NON-NLS-2$
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
//    }
//
//    private void infoDialog(final double n, final double w, final double s, final double e) {
//        try {
//
//            final List<SpatialVectorTable> visibleTables = new ArrayList<>();
//            HashMap<SpatialiteMap, SpatialVectorTable> spatialiteMaps2TablesMap = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap();
//            for (Map.Entry<SpatialiteMap, SpatialVectorTable> entry : spatialiteMaps2TablesMap.entrySet()) {
//                if (!entry.getKey().isVisible) {
//                    continue;
//                }
//                visibleTables.add(entry.getValue());
//            }
//
//            final Context context = EditManager.INSTANCE.getEditingView().getContext();
//
//            StringAsyncTask task =  new StringAsyncTask(context) {
//                private List<Feature> features = new ArrayList<>();
//
//                @Override
//                protected String doBackgroundWork() {
//                    try {
//                        features.clear();
//                        boolean oneEnabled = visibleTables.size() > 0;
//                        if (oneEnabled) {
//                            double north = n;
//                            double south = s;
//                            if (n - s == 0) {
//                                south = n - 1;
//                            }
//                            double west = w;
//                            double east = e;
//                            if (e - w == 0) {
//                                west = e - 1;
//                            }
//
//                            for (SpatialVectorTable spatialTable : visibleTables) {
//                                String query = SpatialiteDatabaseHandler.getIntersectionQueryBBOX(
//                                        LibraryConstants.SRID_WGS84_4326, spatialTable, north, south, east, west);
//
//                                List<Feature> featuresList = FeatureUtilities.buildWithoutGeometry(query, spatialTable);
//                                features.addAll(featuresList);
//
//                                publishProgress(1);
//                                // Escape early if cancel() is called
//                                if (isCancelled())
//                                    return "CANCEL";
//                            }
//                        }
//                        return "";
//                    } catch (Exception e) {
//                        GPLog.error(this, null, e); //$NON-NLS-1$
//                        return "ERROR: " + e.getLocalizedMessage();
//                    }
//                }
//
//                @Override
//                protected void doUiPostWork(String response) {
//                    if (response.startsWith("ERROR")) {
//                        GPDialogs.warningDialog(context, response, null);
//                    } else if (response.startsWith("CANCEL")) {
//                        return;
//                    } else {
//                        if (features.size() > 0) {
//                            Intent intent = new Intent(context, FeaturePagerActivity.class);
//                            intent.putParcelableArrayListExtra(FeatureUtilities.KEY_FEATURESLIST,
//                                    (ArrayList<? extends Parcelable>) features);
//                            intent.putExtra(FeatureUtilities.KEY_READONLY, true ); //true);
//                            context.startActivity(intent);
//                        }
//                    }
//                    parentGroup.onToolFinished(InfoTool.this);
//                }
//
//            };
//            task.setProgressDialog(context.getString(R.string.info_uppercase), context.getString(R.string.extracting_info), true, visibleTables.size());
//            task.execute();
//
//        } catch (java.lang.Exception ex) {
//            GPLog.error(this, null, ex); //$NON-NLS-1$
//        }
//    }
//
//    @Override
//    public void onViewChanged() {
//        // ignore
//    }
//}
