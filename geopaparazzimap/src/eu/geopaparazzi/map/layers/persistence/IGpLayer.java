package eu.geopaparazzi.map.layers.persistence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.layers.Layer;
import org.oscim.layers.vector.AbstractVectorLayer;
import org.oscim.map.Layers;

import eu.geopaparazzi.map.GPLayers;
import eu.geopaparazzi.map.GPMap;
import eu.geopaparazzi.map.GPMapView;

public interface IGpLayer {
    String PATHS_DELIMITER = ";";

    String LAYERNAME_TAG = "name";
    String LAYERPATH_TAG = "path";
    String LAYERTHEME_TAG = "theme";
    String LAYERTYPE_TAG = "type";


    String getId();

    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    GPMapView getMapView();

    /**
     * Load the layer into the map view.
     *
     * @param index an optional position index. If null, add to the end.
     */
    void load(Integer index);

    /**
     * Remove the current layer from the map view.
     *
     * @return the position the layer had or -1 if the layer could not be found.
     */
    default int removeFromMap() {
        Layers layers = getMapView().map().layers();
        int index = 0;
        for (Layer layer : layers) {
            if (layer instanceof IGpLayer) {
                IGpLayer gpLayer = (IGpLayer) layer;
                if (gpLayer.getId().equals(getId())) {
                    layers.remove(index);
                    return index;
                }
            }
            index++;
        }
        return -1;
    }

    default void moveToPosition(int newIndex) {
        Layers layers = getMapView().map().layers();
        int index = 0;
        for (Layer layer : layers) {
            if (layer instanceof IGpLayer) {
                IGpLayer gpLayer = (IGpLayer) layer;
                if (gpLayer.getId().equals(getId())) {
                    layers.remove(index);
                }
            }
            index++;
        }
        layers.add(newIndex, (AbstractVectorLayer) this);
    }


    JSONObject toJson() throws JSONException;

    static JSONObject getMapLayersJson(GPMapView mapView) throws JSONException {
        Layers layers = mapView.map().layers();

        JSONArray layersArray = new JSONArray();
        int size = layers.size();
        for (int i = 0; i < size; i++) {
            Layer layer = layers.get(i);
            if (layer instanceof IGpLayer) {
                IGpLayer iPersistenceLayer = (IGpLayer) layer;
                JSONObject jsonObject = iPersistenceLayer.toJson();
                layersArray.put(jsonObject);
            }
        }
        JSONObject root = new JSONObject();
        root.put("layers", layersArray);
        return root;
    }

    default JSONObject toDefaultJson() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put(LAYERNAME_TAG, getName());
        return jo;
    }

}
