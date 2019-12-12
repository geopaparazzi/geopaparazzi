package eu.geopaparazzi.map.layers;

import android.graphics.BitmapFactory;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.mbtiles.MBTilesDb;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import eu.geopaparazzi.library.images.ImageUtilities;

import static org.oscim.tiling.QueryResult.FAILED;

/**
 * A tile data source for MBTiles raster databases.
 *
 * @author Andrea Antonello
 */
@SuppressWarnings("ALL")
public class MBTilesTileDataSource implements ITileDataSource {
    static final Logger log = LoggerFactory.getLogger(MBTilesTileDataSource.class);

    private final MBTilesDb db;
    private final ADb adb;
    private final Integer transparentColor;
    private Integer alpha;

    /**
     * Build a tile data source.
     *
     * @param dbPath           the path to the mbtiles database.
     * @param alpha            an optional alpha value [0-255] to make the tile transparent.
     * @param transparentColor an optional color that will be made transparent in the bitmap.
     * @throws Exception
     */
    MBTilesTileDataSource(String dbPath, Integer alpha, Integer transparentColor) throws Exception {
        adb = EDb.SPATIALITE4ANDROID.getSpatialDb();
        boolean exists = adb.open(dbPath);
        if (!exists)
            throw new RuntimeException("needs to exist");
        this.alpha = alpha;
        this.transparentColor = transparentColor;
        db = new MBTilesDb(adb);
        db.setTileRowType("tms");
    }

    @Override
    public void query(MapTile tile, ITileDataSink sink) {
        QueryResult res = FAILED;

        try {
            byte[] imageBytes = db.getTile(tile.tileX, tile.tileY, tile.zoomLevel);
            if (transparentColor != null || alpha != null) {
                android.graphics.Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (transparentColor != null) {
                    bmp = ImageUtilities.makeTransparent(bmp, transparentColor);
                }
                if (alpha != null) {
                    bmp = ImageUtilities.makeBitmapTransparent(bmp, alpha);
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bos);
                imageBytes = bos.toByteArray();
            }

            Bitmap bitmap = AndroidGraphics.decodeBitmap(new ByteArrayInputStream(imageBytes));

            sink.setTileImage(bitmap);
            res = QueryResult.SUCCESS;
        } catch (Exception e) {
            log.debug("{} Error: {}", tile, e.getMessage());
        } finally {
            sink.completed(res);
        }
    }

    @Override
    public void dispose() {
        try {
            adb.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cancel() {
        try {
            adb.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}