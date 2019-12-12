package eu.geopaparazzi.map.layers.interfaces;

/**
 * A layer that allows for the creation also of the 3d buildings and labels.
 * <p>
 * At the time mapsforge and vector layers.
 */
public interface IVectorTileOfflineLayer extends IGpLayer {

    String getPath();
}