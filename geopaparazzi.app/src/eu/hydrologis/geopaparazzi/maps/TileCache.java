/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.maps;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.tan;
import static java.lang.Math.toRadians;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * A cache for osm tiles.
 * 
 * <p>
 * It also takes care to retrieve missing tiles from
 * web or disk, if they are not in cache.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TileCache {
    /**
     * The root folder for OSM tile storing. 
     */
    private File osmCacheDir = null;

    /**
     * The maximum number of tiles kept in memory.
     */
    public static final int imgCacheLimit = 16;

    /**
     * The caching {@link HashMap}.
     */
    private Map<String, Bitmap> tileCache = Collections.synchronizedMap(new HashMap<String, Bitmap>(27));

    /**
     * List of tiles that are currently being fectched and therefore 
     * should not be requested again.
     */
    private List<String> loadingTilesList = new ArrayList<String>();

    /**
     * The list of keys used to keep track of order of tiles.
     */
    private List<String> keyList = new ArrayList<String>(27);

    private Bitmap dummyTile;

    private final boolean internetIsOn;

    private final static double delta = 0.1;

    /**
     * Constructor of {@link TileCache}.
     * 
     * @param osmCacheDir the folder where to create the cache.
     * @param internetIsOn flag to tell if the device is online for download.
     * @param dummyTile the image to use as empty.
     */
    public TileCache( File osmCacheDir, boolean internetIsOn, Bitmap dummyTile ) {
        this.internetIsOn = internetIsOn;
        this.dummyTile = dummyTile;
        this.osmCacheDir = osmCacheDir;
    }

    /**
     * Puts a tile in cache, checking for cache size and limit.
     * 
     * @param key the tile's definition string.
     * @param tile the tile {@link Bitmap}.
     */
    public void put( String key, Bitmap tile ) {
        synchronized (tileCache) {
            int size = tileCache.size();
            // Logger.d(LOGTAG, "Inserting Tile: " + key + " - Size reached = " + size);
            while( size > imgCacheLimit ) {
                // remove oldest tile
                // Logger.d(LOGTAG, "Removing Tile. Size = " + size);
                String oldestKey = keyList.get(0);
                // Logger.d(LOGTAG, "Removing Tile. Oldest key = " + oldestKey);
                Bitmap bitmap = tileCache.get(oldestKey);
                bitmap.recycle();
                tileCache.remove(oldestKey);
                keyList.remove(0);
                size = keyList.size();
            }
            keyList.add(key);
            tileCache.put(key, tile);
        }
    }

    /**
     * Gather a tile from the cache.
     * 
     * <p>
     * The sequence of actions are:
     * <ul>
     *  <li>check to see if the tile is in cache</li>
     *  <li>if tile not in cache check if the file exists</li>
     *  <li>if file doesn't exist, get it online</li>
     * </ul>
     * </p>
     * 
     * @param zoom the zoomlevel.
     * @param xtile the x index of the tile.
     * @param ytile the y index of the tile.
     * @return the tile or a dummy image if the tile could not be retrieved.
     * @throws IOException
     */
    public Bitmap get( int zoom, int xtile, int ytile ) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("/"); //$NON-NLS-1$
        sb.append(zoom);
        sb.append("/"); //$NON-NLS-1$
        sb.append(xtile);
        sb.append("/"); //$NON-NLS-1$
        sb.append(ytile);
        sb.append(".png"); //$NON-NLS-1$

        return get(sb.toString());
    }

    /**
     * Gather a tile from the cache.
     * 
     * @param zoomXtileYtile
     * @return the tile or a dummy image if the tile could not be retrieved.
     * @throws IOException
     * @see {@link #get(int, int, int)}
     */
    public synchronized Bitmap get( String zoomXtileYtile ) throws IOException {
        final String tileDef = zoomXtileYtile;
        int lastSlash = tileDef.lastIndexOf("/");

        final String folder = tileDef.substring(0, lastSlash);
        final String img = tileDef.substring(lastSlash + 1);

        // check in cache
        Bitmap tileBitmap = tileCache.get(tileDef);
        if (tileBitmap != null) {
            // Log.v(LOGTAG, "Using image from cache: " + tileDef);
            return tileBitmap;
        }
        File tileFile = new File(osmCacheDir + tileDef);
        if (tileFile.exists()) {
            byte[] byteArrayForBitmap = new byte[16 * 1024];
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inTempStorage = byteArrayForBitmap;
            tileBitmap = BitmapFactory.decodeFile(tileFile.getAbsolutePath(), opt);
            // BitmapDrawable bitmapDrawable = (BitmapDrawable) Drawable.createFromPath(tileFile
            // .getAbsolutePath());
            if (tileBitmap == null) {
                Logger.d(this, "Problems reading image from disk: " + tileFile); //$NON-NLS-1$
                boolean delete = tileFile.delete();
                if (!delete) {
                    tileFile.deleteOnExit();
                }
                tileBitmap = dummyTile;
            } else {
                put(tileDef, tileBitmap);
                // Log.v(LOGTAG, "File bitmap in cache: " + tileDef);
            }
            return tileBitmap;
        } else {
            // try to get it online

            /*
             * first check if it is not already being fetched.
             * If it is being fetched, instead return the
             * dummy image, so that it will be loaded 
             * as soon as is on board.
             */
            if (loadingTilesList.contains(tileDef)) {
                return dummyTile;
            } else {
                if (internetIsOn)
                    loadingTilesList.add(tileDef);
            }

            /*
             * Preload a dummy tile and start a thread for 
             * fetching the tile.
             */
            tileBitmap = dummyTile;
            if (internetIsOn) {
                new Thread(){
                    public void run() {
                        Bitmap tmpTileBitmap = null;
                        StringBuilder sb = new StringBuilder();
                        sb.append("http://tile.openstreetmap.org"); //$NON-NLS-1$
                        sb.append(tileDef);
                        String urlStr = sb.toString();
                        InputStream tileInputStream = null;
                        try {
                            // Log.v(LOGTAG, "Getting image from web: " + urlStr);
                            URL osmFetchUrl = new URL(urlStr);
                            tileInputStream = (InputStream) osmFetchUrl.getContent();
                            // BitmapDrawable bitmapDrawable = (BitmapDrawable) Drawable
                            // .createFromStream(tileInputStream, "src");
                            byte[] byteArrayForBitmap = new byte[16 * 1024];
                            BitmapFactory.Options opt = new BitmapFactory.Options();
                            opt.inTempStorage = byteArrayForBitmap;
                            tmpTileBitmap = BitmapFactory.decodeStream(tileInputStream, null, opt);
                            if (tmpTileBitmap != null) {
                                put(tileDef, tmpTileBitmap);
                                // create folder if it doesn't exist
                                File folderFile = new File(osmCacheDir, folder);
                                folderFile.mkdirs();
                                File outFile = new File(folderFile, img);
                                dumpPicture(outFile, tmpTileBitmap);
                                // Log.v(LOGTAG, "Web bitmap in cache: " + tileDef);
                            }
                        } catch (Exception e) {
                            Logger.e(this, "Problems reading image from web: " + urlStr, e);
                        } finally {
                            if (tileInputStream != null) {
                                try {
                                    tileInputStream.close();
                                } catch (IOException e) {
                                    Logger.e(this, e.getLocalizedMessage(), e);
                                    e.printStackTrace();
                                }
                            }
                        }
                        // remove the tile from the being fetched list
                        loadingTilesList.remove(tileDef);
                    }
                }.start();
            }
            return tileBitmap;
        }
    }

    public void clear() {
        Collection<Bitmap> values = tileCache.values();
        for( Bitmap bitmap : values ) {
            bitmap.recycle();
        }
        tileCache.clear();
        if (dummyTile != null) {
            dummyTile.recycle();
            dummyTile = null;
        }
        Logger.d(this, "Cleared tiles cache");
    }

    private void dumpPicture( File file, Bitmap bitmap ) throws IOException {
        FileOutputStream fOut = null;
        fOut = new FileOutputStream(file);
        // boolean didCompress =
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
        // Logger.d(LOGTAG, "Compressed bitmap: " + file.getAbsolutePath() + " (successfull = " +
        // didCompress + ")");
        fOut.flush();
        fOut.close();
    }

    /**
     * Retrieves the tile coordinates from the world coordinates.
     * 
     * @param lat world latitude.
     * @param lon world longitude.
     * @param zoom the required zoomlevel.
     * @return the array containing x, y of the tile.
     */
    public static int[] latLon2ContainingTileNumber( final double lat, final double lon, final int zoom ) {
        int xtile = (int) floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) floor((1 - log(tan(toRadians(lat)) + 1 / cos(toRadians(lat))) / PI) / 2 * (1 << zoom));
        // return ("" + zoom + "/" + xtile + "/" + ytile);
        return new int[]{xtile, ytile};
    }

    /**
     * Utility to fetch tiles.
     * 
     * <p>This will be usefull for example to download a complete area
     * and various zoomlevels. 
     * 
     * @TODO check how well this fits in OSM policy before using it (bunch download). 
     * 
     * @param cacheDir the folder into which to save the tiles.
     * @param tileSet the set of tiledefinitions to fetch.
     * @param isInternetOn 
     * @param theDummyTile a dummy tile for missing values
     * @throws IOException
     */
    public static void fetchTiles( File cacheDir, TreeSet<String> tileSet, boolean isInternetOn, Bitmap theDummyTile )
            throws IOException {
        TileCache tC = new TileCache(cacheDir, isInternetOn, theDummyTile);
        for( String tileDef : tileSet ) {
            tC.get(tileDef);
        }
    }

    /**
     * Utility to fetch tiles numbers in a given boundary.
     * 
     * @param startLon the first coord longitude.
     * @param startLat the first coord latitude.
     * @param endLon the last coord longitude.
     * @param endLat the last coord latitude.
     * @param zoomLevels the levels to count.
     * @return the set of tiles in the string form xtile,ytile.
     */
    public static TreeSet<String> fetchTilesSet( double startLon, double startLat, double endLon, double endLat,
            int... zoomLevels ) {
        TreeSet<String> tilesSet = new TreeSet<String>();
        int[] previousTile = null;
        for( int i = 0; i < zoomLevels.length; i++ ) {
            int zoom = zoomLevels[i];
            for( double lon = startLon; lon <= endLon; lon = lon + delta ) {
                for( double lat = startLat; lat <= endLat; lat = lat + delta ) {
                    int[] xyTile = latLon2ContainingTileNumber(lat, lon, zoom);
                    if (previousTile == null) {
                        previousTile = xyTile;
                    } else {
                        if (Arrays.equals(previousTile, xyTile)) {
                            continue;
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("/"); //$NON-NLS-1$
                    sb.append(zoom);
                    sb.append("/"); //$NON-NLS-1$
                    sb.append(xyTile[0]);
                    sb.append("/"); //$NON-NLS-1$
                    sb.append(xyTile[1]);
                    sb.append(".png"); //$NON-NLS-1$
                    String tileString = sb.toString();
                    tilesSet.add(tileString);

                    previousTile = xyTile;
                }
            }
        }
        return tilesSet;
    }

}
