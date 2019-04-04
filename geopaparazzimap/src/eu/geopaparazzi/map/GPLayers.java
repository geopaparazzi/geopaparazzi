package eu.geopaparazzi.map;

import org.oscim.map.Layers;
import org.oscim.map.Map;

import eu.geopaparazzi.map.layers.GPMapScaleBarLayer;

public class GPLayers {

    private final Layers layers;

    public GPLayers(GPMapView mapView){
        layers = mapView.map().layers();
    }


    public void add(GPLayer gpMapScaleBarLayer) {
    }
}
