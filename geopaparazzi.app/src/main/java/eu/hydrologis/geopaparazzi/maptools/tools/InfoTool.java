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
package eu.hydrologis.geopaparazzi.maptools.tools;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcelable;
import android.view.MotionEvent;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.core.maps.SpatialiteMap;
import eu.geopaparazzi.library.style.ToolColors;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maptools.FeaturePagerActivity;
import eu.hydrologis.geopaparazzi.maptools.FeatureUtilities;
import eu.hydrologis.geopaparazzi.maptools.MapTool;
import eu.hydrologis.geopaparazzi.mapview.overlays.SliderDrawProjection;
import jsqlite.Exception;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * A tool to query data.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class InfoTool extends MapTool {
    private static final int TOUCH_BOX_THRES = 10;

    private final Paint infoRectPaintStroke = new Paint();
    private final Paint infoRectPaintFill = new Paint();
    private final Rect rect = new Rect();

    private float currentX;
    private float currentY;
    private float lastX = -1;
    private float lastY = -1;

    private final Point tmpP = new Point();
    private final Point startP = new Point();

    private float left;
    private float right;
    private float bottom;
    private float top;


    private SliderDrawProjection sliderDrawProjection;

    private ToolGroup parentGroup;

    /**
     * Constructor.
     *
     * @param parentGroup the parent group.
     * @param mapView     the mapview reference.
     */
    public InfoTool(ToolGroup parentGroup, MapView mapView) {
        super(mapView);
        this.parentGroup = parentGroup;
        sliderDrawProjection = new SliderDrawProjection(mapView, EditManager.INSTANCE.getEditingView());

        int stroke = ColorUtilities.toColor(ToolColors.infoselection_stroke.getHex());
        int fill = ColorUtilities.toColor(ToolColors.infoselection_fill.getHex());
        infoRectPaintFill.setAntiAlias(true);
        infoRectPaintFill.setColor(fill);
        infoRectPaintFill.setAlpha(80);
        infoRectPaintFill.setStyle(Paint.Style.FILL);
        infoRectPaintStroke.setAntiAlias(true);
        infoRectPaintStroke.setStrokeWidth(1.5f);
        infoRectPaintStroke.setColor(stroke);
        infoRectPaintStroke.setStyle(Paint.Style.STROKE);
    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(false);
    }

    public void onToolDraw(Canvas canvas) {
        canvas.drawRect(rect, infoRectPaintFill);
        canvas.drawRect(rect, infoRectPaintStroke);
    }

    public boolean onToolTouchEvent(MotionEvent event) {
        if (mapView == null || mapView.isClickable()) {
            return false;
        }
        Projection pj = sliderDrawProjection;

        // handle drawing
        currentX = event.getX();
        currentY = event.getY();

        int action = event.getAction();
        switch (action) {
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

                EditManager.INSTANCE.invalidateEditingView();
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

        return true;
    }

    public void disable() {
        if (mapView != null) {
            mapView.setClickable(true);
            mapView = null;
        }
    }

    private void infoDialog(final double n, final double w, final double s, final double e) {
        try {

            final List<SpatialVectorTable> visibleTables = new ArrayList<>();
            HashMap<SpatialiteMap, SpatialVectorTable> spatialiteMaps2TablesMap = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap();
            for (Map.Entry<SpatialiteMap, SpatialVectorTable> entry : spatialiteMaps2TablesMap.entrySet()) {
                if (!entry.getKey().isVisible) {
                    continue;
                }
                visibleTables.add(entry.getValue());
            }

            final Context context = EditManager.INSTANCE.getEditingView().getContext();

            StringAsyncTask task =  new StringAsyncTask(context) {
                private List<Feature> features = new ArrayList<>();

                @Override
                protected String doBackgroundWork() {
                    try {
                        features.clear();
                        boolean oneEnabled = visibleTables.size() > 0;
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

                            for (SpatialVectorTable spatialTable : visibleTables) {
                                String query = SpatialiteDatabaseHandler.getIntersectionQueryBBOX(
                                        LibraryConstants.SRID_WGS84_4326, spatialTable, north, south, east, west);

                                List<Feature> featuresList = FeatureUtilities.buildWithoutGeometry(query, spatialTable);
                                features.addAll(featuresList);

                                publishProgress(1);
                                // Escape early if cancel() is called
                                if (isCancelled())
                                    return "CANCEL";
                            }
                        }
                        return "";
                    } catch (Exception e) {
                        GPLog.error(this, null, e); //$NON-NLS-1$
                        return "ERROR: " + e.getLocalizedMessage();
                    }
                }

                @Override
                protected void doUiPostWork(String response) {
                    if (response.startsWith("ERROR")) {
                        GPDialogs.warningDialog(context, response, null);
                    } else if (response.startsWith("CANCEL")) {
                        return;
                    } else {
                        if (features.size() > 0) {
                            Intent intent = new Intent(context, FeaturePagerActivity.class);
                            intent.putParcelableArrayListExtra(FeatureUtilities.KEY_FEATURESLIST,
                                    (ArrayList<? extends Parcelable>) features);
                            intent.putExtra(FeatureUtilities.KEY_READONLY, true);
                            context.startActivity(intent);
                        }
                    }
                    parentGroup.onToolFinished(InfoTool.this);
                }

            };
            task.setProgressDialog(context.getString(R.string.info_uppercase), context.getString(R.string.extracting_info), true, visibleTables.size());
            task.execute();

        } catch (java.lang.Exception ex) {
            GPLog.error(this, null, ex); //$NON-NLS-1$
        }
    }

    @Override
    public void onViewChanged() {
        // ignore
    }
}
