package eu.geopaparazzi.map.layers.systemlayers;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.backend.CanvasAdapter;
import org.oscim.map.Layers;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;

import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.ISystemLayer;

public class GPMapScaleBarLayer extends MapScaleBarLayer implements ISystemLayer {

    private GPMapView mapView;
    public static String NAME = null;

    public GPMapScaleBarLayer(GPMapView mapView) {
        super(mapView.map(), new DefaultMapScaleBar(mapView.map()));
        this.mapView = mapView;
        getName(mapView.getContext());

        getRenderer().setPosition(GLViewport.Position.TOP_CENTER);
        getRenderer().setOffset(5 * CanvasAdapter.getScale(), 0);
    }

    public static String getName(Context context) {
        if (NAME == null) {
            NAME = context.getString(R.string.layername_scalebar);
        }
        return NAME;
    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public GPMapView getMapView() {
        return mapView;
    }

    @Override
    public void load() {
        Layers layers = map().layers();
        layers.add(this, LayerGroups.GROUP_SYSTEM_TOP.getGroupId());
    }

    @Override
    public void reloadData() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public JSONObject toJson() throws JSONException {
        return toDefaultJson();
    }
}
