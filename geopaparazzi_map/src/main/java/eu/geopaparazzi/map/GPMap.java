package eu.geopaparazzi.map;

import org.oscim.android.MapView;
import org.oscim.map.Map;

public class GPMap {

    private final Map map;

    public GPMap(MapView mapView) {
        map = mapView.map();
    }

    public Map getMap() {
        return map;
    }
}
