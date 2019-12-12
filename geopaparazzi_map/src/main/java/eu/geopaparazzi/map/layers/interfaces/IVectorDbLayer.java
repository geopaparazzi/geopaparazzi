package eu.geopaparazzi.map.layers.interfaces;

/**
 *
 */
public interface IVectorDbLayer extends IEditableLayer {

    /**
     * @return the path to the database.
     */
    String getDbPath();
}
