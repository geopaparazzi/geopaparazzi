package eu.geopaparazzi.map.layers;

import org.oscim.backend.CanvasAdapter;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;

import eu.geopaparazzi.map.GPLayer;
import eu.geopaparazzi.map.GPMap;
import eu.geopaparazzi.map.GPMapView;

public class GPMapScaleBarLayer extends GPLayer {

    private final MapScaleBarLayer mapScaleBarLayer;

    public GPMapScaleBarLayer(GPMapView mapView) {
        super(new GPMap(mapView));
        DefaultMapScaleBar mapScaleBar = new DefaultMapScaleBar(mapView.map());
        mapScaleBarLayer = new MapScaleBarLayer(mapView.map(), mapScaleBar);
        mapScaleBarLayer.getRenderer().setPosition(GLViewport.Position.BOTTOM_LEFT);
        mapScaleBarLayer.getRenderer().setOffset(5 * CanvasAdapter.getScale(), 0);
    }
}
