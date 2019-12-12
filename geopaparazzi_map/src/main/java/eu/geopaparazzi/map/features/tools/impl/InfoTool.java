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
package eu.geopaparazzi.map.features.tools.impl;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcelable;
import android.view.MotionEvent;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.style.ToolColors;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.features.Feature;
import eu.geopaparazzi.map.features.FeatureUtilities;
import eu.geopaparazzi.map.features.editing.EditManager;
import eu.geopaparazzi.map.features.tools.MapTool;
import eu.geopaparazzi.map.features.tools.interfaces.ToolGroup;
import eu.geopaparazzi.map.gui.FeaturePagerActivity;
import eu.geopaparazzi.map.layers.ELayerTypes;
import eu.geopaparazzi.map.layers.LayerManager;
import eu.geopaparazzi.map.layers.interfaces.IVectorDbLayer;
import eu.geopaparazzi.map.layers.utils.SpatialiteConnectionsHandler;
import eu.geopaparazzi.map.proj.OverlayViewProjection;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * A tool to query data.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("ALL")
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

    private double left;
    private double right;
    private double bottom;
    private double top;


    private OverlayViewProjection sliderDrawProjection;

    private ToolGroup parentGroup;

    /**
     * Constructor.
     *
     * @param parentGroup the parent group.
     * @param mapView     the mapview reference.
     */
    public InfoTool(ToolGroup parentGroup, GPMapView mapView) {
        super(mapView);
        this.parentGroup = parentGroup;
        sliderDrawProjection = new OverlayViewProjection(mapView, EditManager.INSTANCE.getEditingView());

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
        OverlayViewProjection pj = sliderDrawProjection;

        // handle drawing
        currentX = event.getX();
        currentY = event.getY();

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Coordinate startGeoPoint = pj.fromPixels(round(currentX), round(currentY));
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
                Coordinate currentGeoPoint = pj.fromPixels(round(currentX), round(currentY));
                pj.toPixels(currentGeoPoint, tmpP);

                left = Math.min(tmpP.x, startP.x);
                right = Math.max(tmpP.x, startP.x);
                bottom = Math.max(tmpP.y, startP.y);
                top = Math.min(tmpP.y, startP.y);
                rect.set((int) left, (int) top, (int) right, (int) bottom);

                EditManager.INSTANCE.invalidateEditingView();
                break;
            case MotionEvent.ACTION_UP:

                double deltaY = abs(top - bottom);
                double deltaX = abs(right - left);
                if (deltaX > TOUCH_BOX_THRES && deltaY > TOUCH_BOX_THRES) {
                    Coordinate ul = pj.fromPixels((int) left, (int) top);
                    Coordinate lr = pj.fromPixels((int) right, (int) bottom);

                    infoDialog(ul.y, ul.x, lr.y, lr.x);
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
            List<IVectorDbLayer> vectorLayers = LayerManager.INSTANCE.getEnabledVectorLayers(mapView);
            final Context context = EditManager.INSTANCE.getEditingView().getContext();

            StringAsyncTask task = new StringAsyncTask(context) {
                private List<Feature> features = new ArrayList<>();

                @Override
                protected String doBackgroundWork() {
                    try {
                        features.clear();
                        boolean oneEnabled = vectorLayers.size() > 0;
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

                            for (IVectorDbLayer vectorLayer : vectorLayers) {
                                try {

                                    Envelope env = new Envelope(west, east, south, north);

                                    ELayerTypes layerType = ELayerTypes.fromFileExt(vectorLayer.getDbPath());
                                    List<Feature> featuresList = new ArrayList<>();
                                    if (layerType == ELayerTypes.SPATIALITE) {
                                        ASpatialDb db = SpatialiteConnectionsHandler.INSTANCE.getDb(vectorLayer.getDbPath());
                                        int mapSrid = LibraryConstants.SRID_WGS84_4326;
                                        GeometryColumn gcol = db.getGeometryColumnsForTable(vectorLayer.getName());
                                        Envelope repEnv = db.reproject(env, mapSrid, gcol.srid);
                                        featuresList.addAll(vectorLayer.getFeatures(repEnv));
                                    } else if (layerType == ELayerTypes.GEOPACKAGE) {
                                        featuresList.addAll(vectorLayer.getFeatures(env));
                                    }
                                    this.features.addAll(featuresList);

                                    publishProgress(1);
                                    // Escape early if cancel() is called
                                    if (isCancelled())
                                        return "CANCEL";
                                } catch (java.lang.Exception e1) {
                                    GPLog.error(this, null, e1);
                                }
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
                            intent.putExtra(FeatureUtilities.KEY_READONLY, true); //true);
                            context.startActivity(intent);
                        }
                    }
                    parentGroup.onToolFinished(InfoTool.this);
                }

            };
            task.setProgressDialog(context.getString(R.string.info_uppercase), context.getString(R.string.extracting_info), true, vectorLayers.size());
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
