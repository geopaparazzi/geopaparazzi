package eu.geopaparazzi.map.layers.interfaces;

import android.graphics.Canvas;

import eu.geopaparazzi.map.proj.OverlayViewProjection;

/**
 *
 */
public interface ILabeledLayer extends IEditableLayer {

    /**
     * Draw labels on the canvas.
     */
    void drawLabels(Canvas canvas, OverlayViewProjection prj) throws Exception;
}
