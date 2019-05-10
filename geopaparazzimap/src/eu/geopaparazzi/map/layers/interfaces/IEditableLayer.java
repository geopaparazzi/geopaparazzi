package eu.geopaparazzi.map.layers.interfaces;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

import eu.geopaparazzi.map.features.Feature;
import eu.geopaparazzi.map.layers.utils.SpatialiteConnectionsHandler;

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
    static void deleteFeatures(List<Feature> features) throws Exception {
        if (features.size() == 0) return;
        Feature firstFeature = features.get(0);
        ASpatialDb db = SpatialiteConnectionsHandler.INSTANCE.getDb(firstFeature.getDatabasePath());
        String tableName = firstFeature.getTableName();

        StringBuilder sbIn = new StringBuilder();
        sbIn.append("delete from \"").append(tableName);
        sbIn.append("\" where ");

        int idIndex = firstFeature.getIdIndex();
        String indexName = firstFeature.getAttributeNames().get(idIndex);

        StringBuilder sb = new StringBuilder();
        for (Feature feature : features) {
            sb.append(" OR ");
            sb.append(indexName).append("=");
            sb.append(feature.getAttributeValues().get(idIndex));
        }
        String valuesPart = sb.substring(4);

        sbIn.append(valuesPart);

        String updateQuery = sbIn.toString();
        db.executeInsertUpdateDeleteSql(updateQuery);
    }

    /**
     * Add a new feature that has only the geometry (all other fields are default value).
     *
     * @param geometry the geometry.
     * @param srid the srid of the added geometry, in case reprojection is needed.
     * @throws Exception
     */
    void addNewFeatureByGeometry(Geometry geometry, int srid) throws Exception ;

    /**
     * @return the type of the geometry fo rthis layer.
     */
    EGeometryType getGeometryType();
}
