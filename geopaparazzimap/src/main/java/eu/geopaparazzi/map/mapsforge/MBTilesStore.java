//package eu.geopaparazzi.mapsforge.core.layers;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.drawable.BitmapDrawable;
//import android.support.annotation.NonNull;
//
//import org.hortonmachine.dbs.compat.ADb;
//import org.hortonmachine.dbs.compat.EDb;
//import org.hortonmachine.dbs.mbtiles.MBTilesDb;
//import org.mapsforge.core.graphics.TileBitmap;
//import org.mapsforge.core.util.IOUtils;
//import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
//import org.mapsforge.map.layer.cache.MBTilesTileCache;
//import org.mapsforge.map.layer.queue.Job;
//import org.mapsforge.map.model.common.Observer;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.util.Set;
//
//import eu.geopaparazzi.library.database.GPLog;
//import eu.geopaparazzi.library.images.ImageUtilities;
//import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MBTilesDroidSpitter;
//
///**
// * A "tilecache" storing map tiles that is prepopulated and never removes any files.
// * This tile store uses the standard TMS directory layout of zoomlevel/y/x . To support
// * a different directory structure override the findFile method.
// */
//public class MBTilesStore implements MBTilesTileCache {
//
//    private AndroidGraphicFactory graphicFactory;
//    private MBTilesDb mbTilesDb;
//
//    private boolean doAlpha = false;
//    private int alpha;
//    private boolean doTransparentColor = false;
//    private int transparentColor;
//
//    /**
//     * @param mbtilesFile the mbtiles db file.
//     * @throws IllegalArgumentException if the root directory cannot be a tile store
//     */
//    public MBTilesStore(File mbtilesFile) throws Exception {
//        ADb db = EDb.SPATIALITE4ANDROID.getDb();
//        boolean exists = db.open(mbtilesFile.getAbsolutePath());
//        if (!exists)
//            throw new RuntimeException("needs to exist");
//        mbTilesDb = new MBTilesDb(db);
//
//        graphicFactory = AndroidGraphicFactory.INSTANCE;
//    }
//
//    public void setRowType(String type){
//        mbTilesDb.setTileRowType(type);
//    }
//
//
//    public void setAlpha(Integer alphaObj) {
//        if (alphaObj != null) {
//            doAlpha = true;
//            this.alpha = alphaObj;
//        }
//    }
//
//    public void setTransparentColor(Integer transparentColorObj) {
//        if (transparentColorObj != null) {
//            doAlpha = true;
//            this.transparentColor = transparentColorObj;
//        }
//    }
//
//
//    @Override
//    public synchronized boolean containsKey(Job key) {
//        return true;
//    }
//
//    @Override
//    public synchronized void destroy() {
//        // no-op
//    }
//
//    @Override
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
//
//    @Override
//    public synchronized int getCapacity() {
//        return Integer.MAX_VALUE;
//    }
//
//    @Override
//    public synchronized int getCapacityFirstLevel() {
//        return getCapacity();
//    }
//
//    @Override
//    public TileBitmap getImmediately(Job key) {
//        return get(key);
//    }
//
//    @Override
//    public synchronized void purge() {
//        // no-op
//    }
//
//    @Override
//    public synchronized void put(Job key, TileBitmap bitmap) {
//        // no-op
//    }
//
//
//    @Override
//    public void setWorkingSet(Set<Job> key) {
//        // all tiles are always in the cache
//    }
//
//    @Override
//    public void addObserver(final Observer observer) {
//    }
//
//    @Override
//    public void removeObserver(final Observer observer) {
//    }
//}
