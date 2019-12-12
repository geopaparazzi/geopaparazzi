package eu.geopaparazzi.map.layers.interfaces;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import eu.geopaparazzi.map.GPMapView;

@SuppressWarnings("ALL")
public interface IGpLayer {
    String PATHS_DELIMITER = ";";

    String LAYERTYPE_TAG = "type";
    String LAYERENABLED_TAG = "enabled";
    String LAYEREDITING_TAG = "editing";
    String LAYERNAME_TAG = "name";
    String LAYERPATH_TAG = "path";
    String LAYERDOLABELS_TAG = "dolabels";
    String LAYERDO3D_TAG = "do3d";
    String LAYERURL_TAG = "url";
    String LAYERMAXZOOM_TAG = "maxzoom";
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

    void reloadData() throws Exception;

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

    static String getName(Context context) {
        return "";
    }

}
