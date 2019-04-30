package eu.geopaparazzi.map;

import org.oscim.map.Layers;

public class GPLayers {

    private final Layers layers;

    public GPLayers(GPMapView mapView){
        layers = mapView.map().layers();
    }


    public void add(GPLayer gpMapScaleBarLayer) {
    }
}
