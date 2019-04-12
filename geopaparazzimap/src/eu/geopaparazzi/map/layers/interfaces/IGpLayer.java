package eu.geopaparazzi.map.layers.interfaces;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.layers.Layer;
import org.oscim.layers.vector.AbstractVectorLayer;
import org.oscim.map.Layers;

import eu.geopaparazzi.map.GPMapView;

public interface IGpLayer {
    String PATHS_DELIMITER = ";";

    String LAYERTYPE_TAG = "type";
    String LAYERENABLED_TAG = "enabled";
    String LAYERNAME_TAG = "name";
    String LAYERPATH_TAG = "path";
    String LAYERTRANSPARENTCOLOR_TAG = "transparentcolor";
    String LAYERALPHA_TAG = "alpha";


    String getId();

    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    GPMapView getMapView();

    /**
     * Load the layer into the map view.
     */
    void load();

    void onResume();

    void onPause();

    void dispose();

    JSONObject toJson() throws JSONException;

    default JSONObject toDefaultJson() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put(LAYERTYPE_TAG, this.getClass().getCanonicalName());
        jo.put(LAYERNAME_TAG, getName());
        jo.put(LAYERENABLED_TAG, isEnabled());
        return jo;
    }

}
