package eu.geopaparazzi.map.layers.interfaces;

/**
 * A layer that supports offlint tile datasources.
 * <p>
 * At the time mbtiles layers.
 */
public interface IRasterTileOfflineLayer extends IGpLayer {

    String getPath();

}

