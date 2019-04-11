package eu.geopaparazzi.map.layers.persistence;

import org.json.JSONObject;

/**
 * A layer that allows for the creation also of the 3d buildings and labels.
 *
 * At the time mapsforge and vector layers.
 *
 */
public class VectorTileOnlineLayer extends IGpLayer {

    public String name;

    public String url;

    public String tilePath;

    @Override
    public JSONObject toJson() {
        return null;
    }
}
