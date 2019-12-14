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
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;

import org.hortonmachine.dbs.utils.MercatorUtils;
import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.utilities.Constants;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.features.editing.EditManager;
import eu.geopaparazzi.map.features.editing.EditingView;
import eu.geopaparazzi.map.features.tools.MapTool;
import eu.geopaparazzi.map.layers.interfaces.IGpLayer;
import eu.geopaparazzi.map.layers.interfaces.ILabeledLayer;
import eu.geopaparazzi.map.layers.interfaces.IVectorDbLayer;
import eu.geopaparazzi.map.layers.userlayers.GeopackageTableLayer;
import eu.geopaparazzi.map.layers.userlayers.GeopackageTilesLayer;
import eu.geopaparazzi.map.layers.utils.GeopackageConnectionsHandler;
import eu.geopaparazzi.map.proj.OverlayViewProjection;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * A tool to measure by means of drawing on the map.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PanLabelsTool extends MapTool {
    private OverlayViewProjection projection;
    private List<ILabeledLayer> labeledLayers = new ArrayList<>();
//    private boolean doDraw = true;

    /**
     * Constructor.
     *
     * @param mapView the mapview reference.
     */
    public PanLabelsTool(GPMapView mapView) {
        super(mapView);
    }

    public void activate() {
        if (mapView != null) {
            mapView.setClickable(false);

            List<IGpLayer> layers = mapView.getLayers();
            for (IGpLayer layer : layers) {
                if (layer instanceof ILabeledLayer) {
                    labeledLayers.add((ILabeledLayer) layer);
                }
            }
            EditingView editingView = EditManager.INSTANCE.getEditingView();
            projection = new OverlayViewProjection(mapView, editingView);

        }
    }

    public void onToolDraw(Canvas canvas) {
        try {
//            if (doDraw) {

            for (ILabeledLayer l : labeledLayers) {
                l.drawLabels(canvas, projection);
            }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean onToolTouchEvent(MotionEvent event) {
//        if (mapView == null) {
//            return false;
//        }
//        int action = event.getAction();
//        Log.i("BAU" ,"Action: "+ action);
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                doDraw = false;
//                break;
//            case MotionEvent.ACTION_MOVE:
//                doDraw = false;
//                break;
//            case MotionEvent.ACTION_UP:
//                doDraw = true;
//                EditManager.INSTANCE.invalidateEditingView();
//                break;
//        }

        return false;
    }

    public void disable() {
        if (mapView != null) {
            mapView.setClickable(true);
            mapView = null;
        }
    }

    @Override
    public void onViewChanged() {
        // ignore
    }
}
