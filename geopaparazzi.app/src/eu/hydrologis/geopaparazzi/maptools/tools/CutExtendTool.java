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

import static java.lang.Math.abs;
import static java.lang.Math.round;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.MotionEvent;
import eu.geopaparazzi.library.features.EditManager;
import eu.hydrologis.geopaparazzi.maps.overlays.SliderDrawProjection;
import eu.hydrologis.geopaparazzi.maptools.core.MapTool;

/**
 * A tool to cut or extend features.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CutExtendTool extends MapTool {

    private final Paint drawingPaintStroke = new Paint();
    private final Paint drawingPaintFill = new Paint();
    private final Paint selectedGeometryPaintStroke = new Paint();
    private final Paint selectedGeometryPaintFill = new Paint();

    private float currentX;
    private float currentY;
    private float lastX = -1;
    private float lastY = -1;

    private final Point tmpP = new Point();
    private final Point startP = new Point();
    private Point endP = null;

    private Path drawingPath = new Path();

    // private ProgressDialog infoProgressDialog;

    private SliderDrawProjection editingViewProjection;

    /**
     * Constructor.
     * 
     * @param mapView the mapview reference.
     * @param doCut if <code>true</code>, do cut as opposed to extend.
     */
    public CutExtendTool( MapView mapView, boolean doCut ) {
        super(mapView);
        editingViewProjection = new SliderDrawProjection(mapView, EditManager.INSTANCE.getEditingView());

        // Context context = GeopaparazziApplication.getInstance().getApplicationContext();
        // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        drawingPaintFill.setAntiAlias(true);
        drawingPaintFill.setColor(Color.RED);
        // drawingPaintFill.setAlpha(80);
        drawingPaintFill.setStyle(Paint.Style.FILL);
        drawingPaintStroke.setAntiAlias(true);
        drawingPaintStroke.setStrokeWidth(5f);
        drawingPaintStroke.setColor(Color.RED);
        drawingPaintStroke.setStyle(Paint.Style.STROKE);

        selectedGeometryPaintFill.setAntiAlias(true);
        selectedGeometryPaintFill.setColor(Color.RED);
        // selectedGeometryPaintFill.setAlpha(80);
        selectedGeometryPaintFill.setStyle(Paint.Style.FILL);
        selectedGeometryPaintStroke.setAntiAlias(true);
        selectedGeometryPaintStroke.setStrokeWidth(3f);
        selectedGeometryPaintStroke.setColor(Color.YELLOW);
        selectedGeometryPaintStroke.setStyle(Paint.Style.STROKE);

    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(false);
    }

    public void onToolDraw( Canvas canvas ) {
        canvas.drawCircle(startP.x, startP.y, 15, drawingPaintFill);
        canvas.drawPath(drawingPath, drawingPaintStroke);
        if (endP != null) {
            canvas.drawCircle(endP.x, endP.y, 15, drawingPaintFill);
        }
    }

    public boolean onToolTouchEvent( MotionEvent event ) {
        if (mapView == null || mapView.isClickable()) {
            return false;
        }
        Projection pj = editingViewProjection;

        // handle drawing
        currentX = event.getX();
        currentY = event.getY();

        int action = event.getAction();
        switch( action ) {
        case MotionEvent.ACTION_DOWN:
            GeoPoint startGeoPoint = pj.fromPixels(round(currentX), round(currentY));
            pj.toPixels(startGeoPoint, startP);

            endP = null;

            drawingPath.reset();
            drawingPath.moveTo(startP.x, startP.y);

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
            drawingPath.lineTo(tmpP.x, tmpP.y);

            EditManager.INSTANCE.invalidateEditingView();
            break;
        case MotionEvent.ACTION_UP:

            endP = new Point();
            GeoPoint endGeoPoint = pj.fromPixels(round(currentX), round(currentY));
            pj.toPixels(endGeoPoint, endP);

            EditManager.INSTANCE.invalidateEditingView();
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

    // private void select( final double n, final double w, final double s, final double e ) {
    //
    // ILayer editLayer = EditManager.INSTANCE.getEditLayer();
    // SpatialVectorTableLayer layer = (SpatialVectorTableLayer) editLayer;
    // final SpatialVectorTable spatialVectorTable = layer.getSpatialVectorTable();
    //
    // final Context context = EditManager.INSTANCE.getEditingView().getContext();
    // infoProgressDialog = new ProgressDialog(context);
    // infoProgressDialog.setCancelable(true);
    // infoProgressDialog.setTitle("SELECT");
    // infoProgressDialog.setMessage("Selecting features...");
    // infoProgressDialog.setCancelable(false);
    // infoProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    // infoProgressDialog.setIndeterminate(true);
    // infoProgressDialog.show();
    //
    // new AsyncTask<String, Integer, String>(){
    // private List<Feature> features = new ArrayList<Feature>();
    //
    // protected String doInBackground( String... params ) {
    // try {
    // features.clear();
    // double north = n;
    // double south = s;
    // if (n - s == 0) {
    // south = n - 1;
    // }
    // double west = w;
    // double east = e;
    // if (e - w == 0) {
    // west = e - 1;
    // }
    //
    // String query =
    // SpatialiteUtilities.getBboxIntersectingFeaturesQuery(LibraryConstants.SRID_WGS84_4326,
    // spatialVectorTable, north, south, east, west);
    // features = FeatureUtilities.buildFeatures(query, spatialVectorTable);
    //
    // return "";
    // } catch (Exception e) {
    // return "ERROR: " + e.getLocalizedMessage();
    // }
    //
    // }
    //
    // protected void onProgressUpdate( Integer... progress ) { // on UI thread!
    // if (infoProgressDialog != null && infoProgressDialog.isShowing())
    // infoProgressDialog.incrementProgressBy(progress[0]);
    // }
    //
    // protected void onPostExecute( String response ) { // on UI thread!
    // Utilities.dismissProgressDialog(infoProgressDialog);
    // if (response.startsWith("ERROR")) {
    // Utilities.messageDialog(context, response, null);
    // } else if (response.startsWith("CANCEL")) {
    // return;
    // } else {
    // if (features.size() > 0) {
    // // Intent intent = new Intent(context, FeaturePagerActivity.class);
    // // intent.putParcelableArrayListExtra(FeatureUtilities.KEY_FEATURESLIST,
    // // (ArrayList< ? extends Parcelable>) features);
    // // intent.putExtra(FeatureUtilities.KEY_READONLY, true);
    // // context.startActivity(intent);
    // Utilities.toast(context, "Selected features: " + features.size(), Toast.LENGTH_SHORT);
    // }
    //
    // OnSelectionToolGroup selectionGroup = new OnSelectionToolGroup(mapView, features);
    // EditManager.INSTANCE.setActiveToolGroup(selectionGroup);
    // }
    // }
    //
    // }.execute((String) null);
    //
    // }

}
