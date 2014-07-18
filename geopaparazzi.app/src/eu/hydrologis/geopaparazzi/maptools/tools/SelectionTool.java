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
package eu.hydrologis.geopaparazzi.maptools.tools;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.widget.Toast;

import com.vividsolutions.jts.geom.Geometry;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.layers.SpatialVectorTableLayer;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteUtilities;
import eu.hydrologis.geopaparazzi.maps.overlays.SliderDrawProjection;
import eu.hydrologis.geopaparazzi.maptools.FeatureUtilities;
import eu.hydrologis.geopaparazzi.maptools.core.MapTool;
import jsqlite.Exception;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * A tool to select data.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SelectionTool extends MapTool {
    private static final int TOUCH_BOX_THRES = 10;

    private final Paint selectRectPaintStroke = new Paint();
    private final Paint selectRectPaintFill = new Paint();
    private final Rect rect = new Rect();

    private float lastX = -1;
    private float lastY = -1;

    private final Point tmpP = new Point();
    private final Point startP = new Point();

    private float left;
    private float right;
    private float bottom;
    private float top;

    private ProgressDialog infoProgressDialog;

    private SliderDrawProjection editingViewProjection;

    /**
     * Constructor.
     *
     * @param mapView the mapview reference.
     */
    public SelectionTool(MapView mapView) {
        super(mapView);
        editingViewProjection = new SliderDrawProjection(mapView, EditManager.INSTANCE.getEditingView());

        // Context context = GeopaparazziApplication.getInstance().getApplicationContext();
        // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        selectRectPaintFill.setAntiAlias(true);
        selectRectPaintFill.setColor(Color.RED);
        selectRectPaintFill.setAlpha(80);
        selectRectPaintFill.setStyle(Paint.Style.FILL);
        selectRectPaintStroke.setAntiAlias(true);
        selectRectPaintStroke.setStrokeWidth(1.5f);
        selectRectPaintStroke.setColor(Color.YELLOW);
        selectRectPaintStroke.setStyle(Paint.Style.STROKE);
    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(false);
    }

    public void onToolDraw(Canvas canvas) {
        canvas.drawRect(rect, selectRectPaintFill);
        canvas.drawRect(rect, selectRectPaintStroke);
    }

    public boolean onToolTouchEvent(MotionEvent event) {
        if (mapView == null || mapView.isClickable()) {
            return false;
        }
        Projection pj = editingViewProjection;

        // handle drawing
        float currentX = event.getX();
        float currentY = event.getY();

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

                    select(ul.getLatitude(), ul.getLongitude(), lr.getLatitude(), lr.getLongitude());
                }

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

    private void select(final double n, final double w, final double s, final double e) {

        ILayer editLayer = EditManager.INSTANCE.getEditLayer();
        SpatialVectorTableLayer layer = (SpatialVectorTableLayer) editLayer;
        final SpatialVectorTable spatialVectorTable = layer.getSpatialVectorTable();

        final Context context = EditManager.INSTANCE.getEditingView().getContext();
        infoProgressDialog = new ProgressDialog(context);
        infoProgressDialog.setCancelable(true);
        infoProgressDialog.setTitle("SELECT");
        infoProgressDialog.setMessage("Selecting features...");
        infoProgressDialog.setCancelable(false);
        infoProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        infoProgressDialog.setIndeterminate(true);
        infoProgressDialog.show();

        new AsyncTask<String, Integer, String>() {
            private List<Feature> features = new ArrayList<Feature>();

            protected String doInBackground(String... params) {
                try {
                    features.clear();
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

                    String query = SpatialiteUtilities.getBboxIntersectingFeaturesQuery(LibraryConstants.SRID_WGS84_4326,
                            spatialVectorTable, north, south, east, west);
                    features = FeatureUtilities.buildFeatures(query, spatialVectorTable);

                    return "";
                } catch (Exception e) {
                    return "ERROR: " + e.getLocalizedMessage();
                }

            }

            protected void onProgressUpdate(Integer... progress) { // on UI thread!
                if (infoProgressDialog != null && infoProgressDialog.isShowing())
                    infoProgressDialog.incrementProgressBy(progress[0]);
            }

            protected void onPostExecute(String response) { // on UI thread!
                Utilities.dismissProgressDialog(infoProgressDialog);
                if (response.startsWith("ERROR")) {
                    Utilities.messageDialog(context, response, null);
                } else if (response.startsWith("CANCEL")) {
                    return;
                } else {
                    if (features.size() > 0) {
                        try {
                            int geomsCount = 0;
                            for (Feature feature : features) {
                                Geometry geometry = FeatureUtilities.getGeometry(feature);
                                if(geometry!=null)
                                    geomsCount = geomsCount + geometry.getNumGeometries();
                            }
                            Utilities.toast(context, "Selected " + features.size() + " features with " + geomsCount
                                    + " polygons.", Toast.LENGTH_SHORT);
                        } catch (java.lang.Exception e) {
                            e.printStackTrace();
                        }
                        OnSelectionToolGroup selectionGroup = new OnSelectionToolGroup(mapView, features);
                        EditManager.INSTANCE.setActiveToolGroup(selectionGroup);
                    }else{
                        rect.setEmpty();
                        EditManager.INSTANCE.invalidateEditingView();
                    }

                }
            }

        }.execute((String) null);

    }

}
