package eu.geopaparazzi.map.layers;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;

public class MBTilesTileSource extends TileSource {
    private final MBTilesTileDataSource ds;

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
