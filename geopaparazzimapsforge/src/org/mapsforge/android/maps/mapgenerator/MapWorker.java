/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.mapgenerator;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.PausableThread;
import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * A MapWorker uses a {@link MapGenerator} to generate map tiles. It runs in a separate thread to avoid blocking the UI
 * thread.
 */
public class MapWorker extends PausableThread {
    private static final String THREAD_NAME = "MapWorker";

    private TileCache fileSystemTileCache;
    private TileCache inMemoryTileCache;
    private JobQueue jobQueue;
    private MapGenerator mapGenerator;
    private MapView mapView;
    private Bitmap tileBitmap;

    /**
     * @param mapView the MapView for which this MapWorker generates map tiles.
     */
    public MapWorker(MapView mapView) {
        super();
        this.mapView = mapView;
        this.jobQueue = mapView.getJobQueue();
        this.inMemoryTileCache = mapView.getInMemoryTileCache();
        this.fileSystemTileCache = mapView.getFileSystemTileCache();
        this.tileBitmap = Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE, Bitmap.Config.RGB_565);
    }

    /**
     * @param mapGenerator the MapGenerator which this MapWorker should use.
     */
    public void setMapGenerator(MapGenerator mapGenerator) {
        this.mapGenerator = mapGenerator;
    }

    @Override
    protected void afterRun() {
        this.mapView = null;
        this.mapGenerator = null;
        this.fileSystemTileCache = null;
        this.inMemoryTileCache = null;
        this.jobQueue = null;
        this.tileBitmap.recycle();
        this.tileBitmap = null;
    }

    @Override
    protected void doWork() {
        MapGeneratorJob mapGeneratorJob = this.jobQueue.poll();

        if (mapGeneratorJob == null)
            return;

        if (inMemoryTileCache != null && inMemoryTileCache.containsKey(mapGeneratorJob)) {
            return;
        } else if (fileSystemTileCache != null && fileSystemTileCache.containsKey(mapGeneratorJob)) {
            return;
        }

        boolean success;
        try {
            success = this.mapGenerator.executeJob(mapGeneratorJob, this.tileBitmap);
        } catch (Exception e) {
            Log.e(THREAD_NAME, "Error with tile: " + this.tileBitmap + ": " + e.getLocalizedMessage());
            success = false;
        }

        if (!isInterrupted() && success) {
            if (this.mapView.getFrameBuffer().drawBitmap(mapGeneratorJob.tile, this.tileBitmap)) {
                this.inMemoryTileCache.put(mapGeneratorJob, this.tileBitmap);
            }
            this.mapView.postInvalidate();
            this.fileSystemTileCache.put(mapGeneratorJob, this.tileBitmap);
        }
    }

    @Override
    protected String getThreadName() {
        return THREAD_NAME;
    }

    @Override
    protected int getThreadPriority() {
        return (Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2;
    }

    @Override
    protected boolean hasWork() {
        return !this.jobQueue.isEmpty();
    }
}
