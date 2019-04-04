package eu.geopaparazzi.map.layers;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;

public class MBTilesTileSource extends TileSource {
    public static final int tileSize = 256;
    private final MBTilesTileDataSource ds;

    public MBTilesTileSource(String dbPath) throws Exception {
        ds = new MBTilesTileDataSource(dbPath);
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
