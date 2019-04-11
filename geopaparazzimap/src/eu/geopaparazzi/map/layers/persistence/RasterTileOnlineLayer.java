package eu.geopaparazzi.map.layers.persistence;

import org.json.JSONObject;

public class RasterTileOnlineLayer extends IGpLayer {
    public String name;

    public String path;

    public Integer alpha;

    public String eraseColor;

    @Override
    public JSONObject toJson() {
        return null;
    }
}

