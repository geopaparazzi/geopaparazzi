package eu.geopaparazzi.map.layers.interfaces;

import org.locationtech.jts.geom.Envelope;

import java.util.List;

import eu.geopaparazzi.map.features.Feature;

/**
 *
 */
public interface IVectorDbLayer extends IEditableLayer {

    /**
     * @return the path to the database.
     */
    String getDbPath();
}
