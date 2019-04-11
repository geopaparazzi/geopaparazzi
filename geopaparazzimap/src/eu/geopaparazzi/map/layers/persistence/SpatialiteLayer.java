package eu.geopaparazzi.map.layers.persistence;

import org.json.JSONObject;

/**
 * A layer that allows for the creation also of the 3d buildings and labels.
 *
 * At the time mapsforge and vector layers.
 *
 */
public class SpatialiteLayer extends IGpLayer {

    public String name;

    public String dbPath;

    public String tableName;

    @Override
    public JSONObject toJson() {
        return null;
    }
}
