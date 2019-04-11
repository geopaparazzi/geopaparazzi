package eu.geopaparazzi.map.layers.persistence;

import eu.geopaparazzi.map.GPMapThemes;
import eu.geopaparazzi.map.layers.utils.Vector3dLayerType;

/**
 * A layer that allows for the creation also of the 3d buildings and labels.
 * <p>
 * At the time mapsforge and vector layers.
 */
public interface IVectorTileOfflineLayer extends IGpLayer {

    String getPath();
}