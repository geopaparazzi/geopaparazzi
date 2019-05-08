package eu.geopaparazzi.map.layers.interfaces;

import org.locationtech.jts.geom.Envelope;

import java.util.List;

import eu.geopaparazzi.map.features.Feature;

/**
 *
 */
public interface IVectorDbLayer extends IGpLayer {

    /**
     * @return true if the layer allows for geometry editing.
     */
    boolean isEditable();

    /**
     * Get the list of features, given a query.
     *
     * @param env optional envelope.
     * @return the list of features
     * @throws Exception
     */
    List<Feature> getFeatures(Envelope env) throws Exception;

    /**
     * @return the path to the database.
     */
    String getDbPath();
}
