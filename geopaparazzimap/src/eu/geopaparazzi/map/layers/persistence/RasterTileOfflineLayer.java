package eu.geopaparazzi.map.layers.persistence;

import org.json.JSONObject;

public class RasterTileOfflineLayer implements IGpLayer {
    public String name;

    public String path;

    public Integer alpha;

    public String eraseColor;

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public JSONObject toJson() {
        return null;
    }
}

