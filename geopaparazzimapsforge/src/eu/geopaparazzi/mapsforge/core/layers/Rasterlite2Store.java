package eu.geopaparazzi.mapsforge.core.layers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.mbtiles.MBTilesDb;
import org.hortonmachine.dbs.rasterlite.Rasterlite2Coverage;
import org.hortonmachine.dbs.rasterlite.Rasterlite2Db;
import org.hortonmachine.dbs.utils.MercatorUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.model.common.Observer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Set;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.images.ImageUtilities;

/**
 * A "tilecache" storing map tiles that is prepopulated and never removes any files.
 * This tile store uses the standard TMS directory layout of zoomlevel/y/x . To support
 * a different directory structure override the findFile method.
 */
public class Rasterlite2Store implements TileCache {

    private AndroidGraphicFactory graphicFactory;

    private boolean doAlpha = false;
    private int alpha;
    private boolean doTransparentColor = false;
    private int transparentColor;
    private String table;
    private int tileSize;
    private Rasterlite2Coverage coverage;

    /**
     * @param rl2File the rl2 db file.
     * @throws IllegalArgumentException if the root directory cannot be a tile store
     */
    public Rasterlite2Store(File rl2File, String table, int tileSize) throws Exception {
        this.table = table;
        this.tileSize = tileSize;
        ASpatialDb db = EDb.SPATIALITE4ANDROID.getSpatialDb();
        boolean exists = db.open(rl2File.getAbsolutePath());
        if (!exists)
            throw new RuntimeException("needs to exist");


        Rasterlite2Db rlt2Db = new Rasterlite2Db(db);
        List<Rasterlite2Coverage> rasterCoverages = rlt2Db.getRasterCoverages(false);
        for (Rasterlite2Coverage raster : rasterCoverages) {
            if (raster.getName().equals(table)) {
                coverage = raster;
                break;
            }
        }
        if (coverage == null) throw new IllegalArgumentException("No table by name: " + table);
        graphicFactory = AndroidGraphicFactory.INSTANCE;
    }


    public void setAlpha(Integer alphaObj) {
        if (alphaObj != null) {
            doAlpha = true;
            this.alpha = alphaObj;
        }
    }

    public void setTransparentColor(Integer transparentColorObj) {
        if (transparentColorObj != null) {
            doAlpha = true;
            this.transparentColor = transparentColorObj;
        }
    }


    @Override
    public synchronized boolean containsKey(Job key) {
        return true;
    }

    @Override
    public synchronized void destroy() {
        // no-op
    }

    @Override
    public synchronized TileBitmap get(Job key) {

        try {
//            int[] tmsTiles = MBTilesDb.osmTile2TmsTile(key.tile.tileX, key.tile.tileY, key.tile.zoomLevel);
//            int[] tmsTiles = new int[]{key.tile.tileX, key.tile.tileY};


            //[minx, miny_osm, maxx, maxy_osm of tile_bounds]
//            int[] tile_bounds = {tmsTiles[0], tmsTiles[1], tmsTiles[0], tmsTiles[1]};
            //minx, miny, maxx, maxy
            double[] wsen = MercatorUtils.tileLatLonBounds(key.tile.tileX, key.tile.tileY, key.tile.zoomLevel, tileSize);
//            final double[] wsen = MBTilesDb.TileBounds_to_LatLonBounds(tile_bounds, key.tile.zoomLevel);
            double w = wsen[0];
            double s = wsen[1];
            double e = wsen[2];
            double n = wsen[3];
            GeometryFactory gf = new GeometryFactory();
            Coordinate[] coordinates = {
                    new Coordinate(w, s),
                    new Coordinate(w, n),
                    new Coordinate(e, n),
                    new Coordinate(e, s),
                    new Coordinate(w, s)
            };
            Polygon polygon = gf.createPolygon(coordinates);

            byte[] imageBytes = coverage.getRL2Image(polygon, "4326", tileSize, tileSize);
            if (imageBytes != null) {
                Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bmp != null) {
                    ByteArrayInputStream bis = null;
                    ByteArrayOutputStream bos = null;
                    try {

                        int tileSize = key.tile.tileSize;
                        if (tileSize != 256) {
                            bmp = Bitmap.createScaledBitmap(bmp, tileSize, tileSize, false);
                        }

                        if (doAlpha) {
                            bmp = ImageUtilities.makeBitmapTransparent(bmp, alpha);
                        }

                        if (doTransparentColor) {
                            bmp = ImageUtilities.makeTransparent(bmp, transparentColor);
                        }

                        bos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
                        byte[] bytes = bos.toByteArray();
                        bis = new ByteArrayInputStream(bytes);
                        TileBitmap tileBitmap = graphicFactory.createTileBitmap(bis, tileSize, key.hasAlpha);
                        return tileBitmap;
                    } catch (Exception ex) {
                        return null;
                    } finally {
                        if (bis != null)
                            IOUtils.closeQuietly(bis);
                        if (bos != null)
                            IOUtils.closeQuietly(bos);
                    }
                }
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
        return null;
    }

    @Override
    public synchronized int getCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public synchronized int getCapacityFirstLevel() {
        return getCapacity();
    }

    @Override
    public TileBitmap getImmediately(Job key) {
        return get(key);
    }

    @Override
    public synchronized void purge() {
        // no-op
    }

    @Override
    public synchronized void put(Job key, TileBitmap bitmap) {
        // no-op
    }


    @Override
    public void setWorkingSet(Set<Job> key) {
        // all tiles are always in the cache
    }

    @Override
    public void addObserver(final Observer observer) {
    }

    @Override
    public void removeObserver(final Observer observer) {
    }
}
