package eu.geopaparazzi.map.layers;

import android.graphics.BitmapFactory;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.mbtiles.MBTilesDb;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileCache;
import org.oscim.tiling.ITileCache.TileReader;
import org.oscim.tiling.ITileCache.TileWriter;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.QueryResult;
import org.oscim.tiling.source.ITileDecoder;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import eu.geopaparazzi.library.images.ImageUtilities;

import static org.oscim.tiling.QueryResult.DELAYED;
import static org.oscim.tiling.QueryResult.FAILED;
import static org.oscim.tiling.QueryResult.SUCCESS;

public class MBTilesTileDataSource implements ITileDataSource {
    static final Logger log = LoggerFactory.getLogger(MBTilesTileDataSource.class);

    private final MBTilesDb db;
    private final ADb adb;
    private final Integer transparentColor;
    private Integer alpha;

    public MBTilesTileDataSource(String dbPath, Integer alpha, Integer transparentColor) throws Exception {
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
//            if (Tile.SIZE != 256) {
//                android.graphics.Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
//                bmp = android.graphics.Bitmap.createScaledBitmap(bmp, Tile.SIZE, Tile.SIZE, false);
//                ByteArrayOutputStream bos = new ByteArrayOutputStream();
//                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bos);
//                imageBytes = bos.toByteArray();
//            }

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

//    public synchronized TileBitmap get(Job key) {
//
//        try {
//            byte[] imageBytes = mbTilesDb.getTile(key.tile.tileX, key.tile.tileY, key.tile.zoomLevel);
//            if (imageBytes != null) {
//                Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
//                if (bmp != null) {
//                    ByteArrayInputStream bis = null;
//                    ByteArrayOutputStream bos = null;
//                    try {
//
//                        int tileSize = key.tile.tileSize;
//                        if (tileSize != 256) {
//                            bmp = Bitmap.createScaledBitmap(bmp, tileSize, tileSize, false);
//                        }
//
//                        if (doAlpha) {
//                            bmp = ImageUtilities.makeBitmapTransparent(bmp, alpha);
//                        }
//
//                        if (doTransparentColor) {
//                            bmp = ImageUtilities.makeTransparent(bmp, transparentColor);
//                        }
//
//                        bos = new ByteArrayOutputStream();
//                        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
//                        byte[] bytes = bos.toByteArray();
//                        bis = new ByteArrayInputStream(bytes);
//                        TileBitmap tileBitmap = graphicFactory.createTileBitmap(bis, tileSize, key.hasAlpha);
//                        return tileBitmap;
//                    } catch (Exception e) {
//                        return null;
//                    } finally {
//                        if (bis != null)
//                            IOUtils.closeQuietly(bis);
//                        if (bos != null)
//                            IOUtils.closeQuietly(bos);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            GPLog.error(this, null, e);
//        }
//        return null;
//    }

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