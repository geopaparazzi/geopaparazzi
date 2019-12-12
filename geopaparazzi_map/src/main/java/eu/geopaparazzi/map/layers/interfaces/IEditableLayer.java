package eu.geopaparazzi.map.layers.interfaces;

import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

import eu.geopaparazzi.map.features.Feature;

/**
 *
 */
public interface IEditableLayer extends IGpLayer {

    /**
     * @return true if the layer allows for geometry editing.
     */
    boolean isEditable();


    /**
     * @return true if the layer is activated for editing.
     */
    boolean isInEditingMode();


    /**
     * Get the list of features, given a query.
     *
     * @param env optional envelope.
     * @return the list of features
     * @throws Exception
     */
    List<Feature> getFeatures(Envelope env) throws Exception;

    /**
     * Delete a list of features in the given database.
     * <p/>
     * <b>The features need to be from the same table</b>
     *
     * @param features the features list to remove.
     * @throws Exception if something goes wrong.
     */
    void deleteFeatures(List<Feature> features) throws Exception;

    /**
     * Add a new feature that has only the geometry (all other fields are default value).
     *
     * @param geometry the geometry.
     * @param srid     the srid of the added geometry, in case reprojection is needed.
     * @throws Exception
     */
    void addNewFeatureByGeometry(Geometry geometry, int srid) throws Exception;

    /**
     * Updates the geometry of a feature in the given database.
     *
     * @param feature      the feature to update.
     * @param geometry     the new geometry to set.
     * @param geometrySrid the srid of the new geometry, in case reprojection is necessary.
     * @throws Exception if something goes wrong.
     */
    void updateFeatureGeometry(Feature feature, Geometry geometry, int geometrySrid) throws Exception;

    /**
     * @return the type of the geometry fo rthis layer.
     */
    EGeometryType getGeometryType();
}
