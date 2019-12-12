package eu.geopaparazzi.map.layers;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;

/**
 * A tile source for MBTiles raster databases.
 *
 * @author Andrea Antonello
 */
public class MBTilesTileSource extends TileSource {
    private final MBTilesTileDataSource ds;

    /**
     * Build a tile source.
     *
     * @param dbPath           the path to the mbtiles database.
     * @param alpha            an optional alpha value [0-255] to make the tile transparent.
     * @param transparentColor an optional color that will be made transparent in the bitmap.
     * @throws Exception
     */
    public MBTilesTileSource(String dbPath, Integer alpha, Integer transparentColor) throws Exception {
        ds = new MBTilesTileDataSource(dbPath, alpha, transparentColor);
    }


    @Override
    public ITileDataSource getDataSource() {
        return ds;
    }

    @Override
    public OpenResult open() {
        return OpenResult.SUCCESS;
    }

    @Override
    public void close() {
        ds.dispose();
    }

}
