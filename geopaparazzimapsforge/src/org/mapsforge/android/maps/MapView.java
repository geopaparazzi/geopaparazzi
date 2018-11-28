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
package org.mapsforge.android.maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;

import org.mapsforge.android.AndroidUtils;
import org.mapsforge.android.maps.inputhandling.MapMover;
import org.mapsforge.android.maps.inputhandling.TouchEventHandler;
import org.mapsforge.android.maps.inputhandling.ZoomAnimator;
import org.mapsforge.android.maps.mapgenerator.FileSystemTileCache;
import org.mapsforge.android.maps.mapgenerator.InMemoryTileCache;
import org.mapsforge.android.maps.mapgenerator.JobParameters;
import org.mapsforge.android.maps.mapgenerator.JobQueue;
import org.mapsforge.android.maps.mapgenerator.JobTheme;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorFactory;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.MapWorker;
import org.mapsforge.android.maps.mapgenerator.TileCache;
import org.mapsforge.android.maps.mapgenerator.databaserenderer.DatabaseRenderer;
import org.mapsforge.android.maps.mapgenerator.databaserenderer.ExternalRenderTheme;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.android.maps.overlay.OverlayList;
import org.mapsforge.android.maps.rendertheme.InternalRenderTheme;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;

import eu.geopaparazzi.library.database.GPLog;

/**
 * A MapView shows a map on the display of the device. It handles all user input and touch gestures to move and zoom the
 * map. This MapView also includes a scale bar and zoom controls. The {@link #getController()} method returns a
 * {@link MapController} to programmatically modify the position and zoom level of the map.
 * <p/>
 * This implementation supports offline map rendering as well as downloading map images (tiles) over an Internet
 * connection. The operation mode of a MapView can be set in the constructor and changed at runtime with the
 * {@link #setMapGeneratorInternal(MapGenerator)} method. Some MapView parameters depend on the selected operation mode.
 * <p/>
 * In offline rendering mode a special database file is required which contains the map data. Map files can be stored in
 * any folder. The current map file is set by calling {@link #setMapFile(File)}. To retrieve the current
 * {@link MapDatabase}, use the {@link #getMapDatabase()} method.
 * <p/>
 * {@link Overlay Overlays} can be used to display geographical data such as points and ways. To draw an overlay on top
 * of the map, add it to the list returned by {@link #getOverlays()}.
 */
public class MapView extends ViewGroup {
    /**
     * Default render theme of the MapView.
     */
    public static final InternalRenderTheme DEFAULT_RENDER_THEME = InternalRenderTheme.OSMARENDER;

    private static final float DEFAULT_TEXT_SCALE = 1;
    private static final int DEFAULT_TILE_CACHE_SIZE_FILE_SYSTEM = 100;
    private static final int DEFAULT_TILE_CACHE_SIZE_IN_MEMORY = 20;

    private DebugSettings debugSettings;
    private TileCache fileSystemTileCache;
    private final FpsCounter fpsCounter;
    private final FrameBuffer frameBuffer;
    private TileCache inMemoryTileCache;
    private JobParameters jobParameters;
    private final JobQueue jobQueue;
    private final MapController mapController;
    private final MapDatabase mapDatabase;
    private File mapFile;
    private MapGenerator mapGenerator;
    private final MapMover mapMover;
    private final MapScaleBar mapScaleBar;
    private final MapViewPosition mapViewPosition;
    private final MapWorker mapWorker;
    private final MapZoomControls mapZoomControls;
    private final List<Overlay> overlays;
    private final Projection projection;
    private final TouchEventHandler touchEventHandler;
    private final ZoomAnimator zoomAnimator;

    /**
     * @param context the enclosing MapActivity instance.
     * @throws IllegalArgumentException if the context object is not an instance of {@link MapActivity}.
     */
    public MapView(Context context) {
        this(context, null, new DatabaseRenderer());
    }

    /**
     * @param context      the enclosing MapActivity instance.
     * @param attributeSet a set of attributes.
     * @throws IllegalArgumentException if the context object is not an instance of {@link MapActivity}.
     */
    public MapView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, MapGeneratorFactory.createMapGenerator(attributeSet));
    }

    /**
     * @param context      the enclosing MapActivity instance.
     * @param mapGenerator the MapGenerator for this MapView.
     * @throws IllegalArgumentException if the context object is not an instance of {@link MapActivity}.
     */
    public MapView(Context context, MapGenerator mapGenerator) {
        this(context, null, mapGenerator);
    }

    private MapView(Context context, AttributeSet attributeSet, MapGenerator mapGenerator) {
        super(context, attributeSet);

        if (!(context instanceof MapActivity)) {
            throw new IllegalArgumentException("context is not an instance of MapActivity");
        }
        MapActivity mapActivity = (MapActivity) context;

        setBackgroundColor(FrameBuffer.MAP_VIEW_BACKGROUND);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setWillNotDraw(false);

        this.debugSettings = new DebugSettings(false, false, false);
        try {
            this.fileSystemTileCache = new FileSystemTileCache(DEFAULT_TILE_CACHE_SIZE_FILE_SYSTEM,
                    mapActivity.getMapViewId());
        } catch (Exception e) {
            GPLog.error(this, "ERROR:", e);
        }
        this.inMemoryTileCache = new InMemoryTileCache(DEFAULT_TILE_CACHE_SIZE_IN_MEMORY);
        this.fpsCounter = new FpsCounter();
        this.frameBuffer = new FrameBuffer(this);
        this.jobParameters = new JobParameters(DEFAULT_RENDER_THEME, DEFAULT_TEXT_SCALE);
        this.jobQueue = new JobQueue(this);
        this.mapController = new MapController(this);
        this.mapDatabase = new MapDatabase();
        this.mapViewPosition = new MapViewPosition(this);
        this.mapScaleBar = new MapScaleBar(this);
        this.mapZoomControls = new MapZoomControls(mapActivity, this);
        this.overlays = new OverlayList(this);
        this.projection = new MapViewProjection(this);
        this.touchEventHandler = TouchEventHandler.getInstance(mapActivity, this);

        this.mapWorker = new MapWorker(this);
        this.mapWorker.start();

        this.mapMover = new MapMover(this);
        this.mapMover.start();

        this.zoomAnimator = new ZoomAnimator(this);
        this.zoomAnimator.start();

        setMapGeneratorInternal(mapGenerator);
        GeoPoint startPoint = this.mapGenerator.getStartPoint();
        if (startPoint != null) {
            this.mapViewPosition.setMapCenter(startPoint);
        }

        Byte startZoomLevel = this.mapGenerator.getStartZoomLevel();
        if (startZoomLevel != null) {
            this.mapViewPosition.setZoomLevel(startZoomLevel.byteValue());
        }

        mapActivity.registerMapView(this);
    }

    /**
     * @return the MapController for this MapView.
     */
    public MapController getController() {
        return this.mapController;
    }

    /**
     * @return the debug settings which are used in this MapView.
     */
    public DebugSettings getDebugSettings() {
        return this.debugSettings;
    }

    /**
     * @return the file system tile cache which is used in this MapView.
     */
    public TileCache getFileSystemTileCache() {
        return this.fileSystemTileCache;
    }

    /**
     * @return the FPS counter which is used in this MapView.
     */
    public FpsCounter getFpsCounter() {
        return this.fpsCounter;
    }

    /**
     * @return the FrameBuffer which is used in this MapView.
     */
    public FrameBuffer getFrameBuffer() {
        return this.frameBuffer;
    }

    /**
     * @return the in-memory tile cache which is used in this MapView.
     */
    public TileCache getInMemoryTileCache() {
        return this.inMemoryTileCache;
    }

    /**
     * @param capacity the in-memory tile cache capacity to use.
     */
    public void setInMemoryTileCacheSize(int capacity) {
        this.inMemoryTileCache.destroy();
        this.inMemoryTileCache = null;
        this.inMemoryTileCache = new InMemoryTileCache(capacity);
    }

    /**
     * @return the job queue which is used in this MapView.
     */
    public JobQueue getJobQueue() {
        return this.jobQueue;
    }

    /**
     * @return the map database which is used for reading map files.
     * @throws UnsupportedOperationException if the current MapGenerator works with an Internet connection.
     */
    public MapDatabase getMapDatabase() {
        if (this.mapGenerator.requiresInternetConnection()) {
            throw new UnsupportedOperationException();
        }
        return this.mapDatabase;
    }

    /**
     * @return the currently used map file.
     * @throws UnsupportedOperationException if the current MapGenerator mode works with an Internet connection.
     */
    public File getMapFile() {
        if (this.mapGenerator.requiresInternetConnection()) {
            throw new UnsupportedOperationException();
        }
        return this.mapFile;
    }

    /**
     * @return the currently used MapGenerator (may be null).
     */
    public MapGenerator getMapGenerator() {
        return this.mapGenerator;
    }

    /**
     * @return the MapMover which is used by this MapView.
     */
    public MapMover getMapMover() {
        return this.mapMover;
    }

    /**
     * @return the current position and zoom level of this MapView.
     */
    public MapViewPosition getMapPosition() {
        return this.mapViewPosition;
    }

    /**
     * @return the scale bar which is used in this MapView.
     */
    public MapScaleBar getMapScaleBar() {
        return this.mapScaleBar;
    }

    /**
     * @return the zoom controls instance which is used in this MapView.
     */
    public MapZoomControls getMapZoomControls() {
        return this.mapZoomControls;
    }

    /**
     * Returns a thread-safe list of overlays for this MapView. It is necessary to manually synchronize on this list
     * when iterating over it.
     *
     * @return the overlay list.
     */
    public List<Overlay> getOverlays() {
        return this.overlays;
    }

    /**
     * @return the currently used projection of the map. Do not keep this object for a longer time.
     */
    public Projection getProjection() {
        return this.projection;
    }

    /**
     * Calls either {@link #invalidate()} or {@link #postInvalidate()}, depending on the current thread.
     */
    public void invalidateOnUiThread() {
        if (AndroidUtils.currentThreadIsUiThread()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    /**
     * @return true if the ZoomAnimator is currently running, false otherwise.
     */
    public boolean isZoomAnimatorRunning() {
        return this.zoomAnimator.isExecuting();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        return this.mapMover.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
        return this.mapMover.onKeyUp(keyCode, keyEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int action = this.touchEventHandler.getAction(motionEvent);
        this.mapZoomControls.onMapViewTouchEvent(action);
        return this.touchEventHandler.handleTouchEvent(motionEvent);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent motionEvent) {
        return this.mapMover.onTrackballEvent(motionEvent);
    }

    /**
     * Calculates all necessary tiles and adds jobs accordingly.
     */
    public void redrawTiles() {
        if (this.getWidth() <= 0 || this.getHeight() <= 0) {
            return;
        }

        synchronized (this.overlays) {
            for (int i = 0, n = this.overlays.size(); i < n; ++i) {
                this.overlays.get(i).requestRedraw();
            }
        }

        MapPosition mapPosition = this.mapViewPosition.getMapPosition();
        if (mapPosition == null) {
            return;
        }

        GeoPoint geoPoint = mapPosition.geoPoint;
        double pixelLeft = MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(), mapPosition.zoomLevel);
        double pixelTop = MercatorProjection.latitudeToPixelY(geoPoint.getLatitude(), mapPosition.zoomLevel);
        pixelLeft -= getWidth() >> 1;
        pixelTop -= getHeight() >> 1;

        long tileLeft = MercatorProjection.pixelXToTileX(pixelLeft, mapPosition.zoomLevel);
        long tileTop = MercatorProjection.pixelYToTileY(pixelTop, mapPosition.zoomLevel);
        long tileRight = MercatorProjection.pixelXToTileX(pixelLeft + getWidth(), mapPosition.zoomLevel);
        long tileBottom = MercatorProjection.pixelYToTileY(pixelTop + getHeight(), mapPosition.zoomLevel);

        Object cacheId;
        if (this.mapGenerator.requiresInternetConnection()) {
            cacheId = ((TileDownloader) this.mapGenerator).getHostName();
        } else {
            cacheId = this.mapFile;
        }

        for (long tileY = tileTop; tileY <= tileBottom; ++tileY) {
            for (long tileX = tileLeft; tileX <= tileRight; ++tileX) {
                Tile tile = new Tile(tileX, tileY, mapPosition.zoomLevel);
                MapGeneratorJob mapGeneratorJob = new MapGeneratorJob(tile, cacheId, this.jobParameters,
                        this.debugSettings);

                if (this.inMemoryTileCache.containsKey(mapGeneratorJob)) {
                    Bitmap bitmap = this.inMemoryTileCache.get(mapGeneratorJob);
                    this.frameBuffer.drawBitmap(mapGeneratorJob.tile, bitmap);
                } else if (this.fileSystemTileCache != null && this.fileSystemTileCache.containsKey(mapGeneratorJob)) {
                    Bitmap bitmap = this.fileSystemTileCache.get(mapGeneratorJob);

                    if (bitmap != null) {
                        this.frameBuffer.drawBitmap(mapGeneratorJob.tile, bitmap);
                        this.inMemoryTileCache.put(mapGeneratorJob, bitmap);
                    } else {
                        // the image data could not be read from the cache
                        this.jobQueue.addJob(mapGeneratorJob);
                    }
                } else {
                    // cache miss
                    this.jobQueue.addJob(mapGeneratorJob);
                }
            }
        }

        if (this.mapScaleBar.isShowMapScaleBar()) {
            this.mapScaleBar.redrawScaleBar();
        }

        invalidateOnUiThread();

        this.jobQueue.requestSchedule();
        synchronized (this.mapWorker) {
            this.mapWorker.notify();
        }
    }

    /**
     * Sets the visibility of the zoom controls.
     *
     * @param showZoomControls true if the zoom controls should be visible, false otherwise.
     */
    public void setBuiltInZoomControls(boolean showZoomControls) {
        this.mapZoomControls.setShowMapZoomControls(showZoomControls);
    }

    /**
     * Sets the center of the MapView and triggers a redraw.
     *
     * @param geoPoint the new center point of the map.
     */
    public void setCenter(GeoPoint geoPoint) {
        MapPosition mapPosition = new MapPosition(geoPoint, this.mapViewPosition.getZoomLevel());
        setCenterAndZoom(mapPosition);
    }

    /**
     * @param debugSettings the new DebugSettings for this MapView.
     */
    public void setDebugSettings(DebugSettings debugSettings) {
        this.debugSettings = debugSettings;
        clearAndRedrawMapView();
    }

    /**
     * Sets the map file for this MapView.
     *
     * @param mapFile the map file.
     * @return a FileOpenResult to describe whether the operation returned successfully.
     * @throws UnsupportedOperationException if the current MapGenerator mode works with an Internet connection.
     * @throws IllegalArgumentException      if the supplied mapFile is null.
     */
    public FileOpenResult setMapFile(File mapFile) {
        if (this.mapGenerator.requiresInternetConnection()) {
            throw new UnsupportedOperationException();
        }
        if (mapFile == null) {
            throw new IllegalArgumentException("mapFile must not be null");
        } else if (mapFile.equals(this.mapFile)) {
            // same map file as before
            return FileOpenResult.SUCCESS;
        }

        this.zoomAnimator.pause();
        this.mapWorker.pause();
        this.mapMover.pause();

        this.zoomAnimator.awaitPausing();
        this.mapMover.awaitPausing();
        this.mapWorker.awaitPausing();

        this.mapMover.stopMove();
        this.jobQueue.clear();

        this.zoomAnimator.proceed();
        this.mapWorker.proceed();
        this.mapMover.proceed();

        this.mapDatabase.closeFile();
        FileOpenResult fileOpenResult = this.mapDatabase.openFile(mapFile);
        if (fileOpenResult.isSuccess()) {
            this.mapFile = mapFile;

            GeoPoint startPoint = this.mapGenerator.getStartPoint();
            if (startPoint != null) {
                this.mapViewPosition.setMapCenter(startPoint);
            }

            Byte startZoomLevel = this.mapGenerator.getStartZoomLevel();
            if (startZoomLevel != null) {
                this.mapViewPosition.setZoomLevel(startZoomLevel.byteValue());
            }

            clearAndRedrawMapView();
            return FileOpenResult.SUCCESS;
        }
        this.mapFile = null;
        clearAndRedrawMapView();
        return fileOpenResult;
    }

    /**
     * Sets the MapGenerator for this MapView.
     *
     * @param mapGenerator the new MapGenerator.
     */
    public void setMapGenerator(MapGenerator mapGenerator) {
        if (this.mapGenerator != mapGenerator) {
            setMapGeneratorInternal(mapGenerator);
            clearAndRedrawMapView();
        }
    }

    /**
     * Sets the XML file which is used for rendering the map.
     *
     * @param renderThemeFile the XML file which defines the rendering theme.
     * @throws IllegalArgumentException      if the supplied internalRenderTheme is null.
     * @throws UnsupportedOperationException if the current MapGenerator does not support render themes.
     * @throws FileNotFoundException         if the supplied file does not exist, is a directory or cannot be read.
     */
    public void setRenderTheme(File renderThemeFile) throws FileNotFoundException {
        if (renderThemeFile == null) {
            throw new IllegalArgumentException("render theme file must not be null");
        } else if (this.mapGenerator.requiresInternetConnection()) {
            throw new UnsupportedOperationException();
        }

        JobTheme jobTheme = new ExternalRenderTheme(renderThemeFile);
        this.jobParameters = new JobParameters(jobTheme, this.jobParameters.textScale);
        clearAndRedrawMapView();
    }

    /**
     * Sets the internal theme which is used for rendering the map.
     *
     * @param internalRenderTheme the internal rendering theme.
     * @throws IllegalArgumentException      if the supplied internalRenderTheme is null.
     * @throws UnsupportedOperationException if the current MapGenerator does not support render themes.
     */
    public void setRenderTheme(InternalRenderTheme internalRenderTheme) {
        if (internalRenderTheme == null) {
            throw new IllegalArgumentException("render theme must not be null");
        } else if (this.mapGenerator.requiresInternetConnection()) {
            throw new UnsupportedOperationException();
        }

        this.jobParameters = new JobParameters(internalRenderTheme, this.jobParameters.textScale);
        clearAndRedrawMapView();
    }

    /**
     * Sets the text scale for the map rendering. Has no effect in downloading mode.
     *
     * @param textScale the new text scale for the map rendering.
     */
    public void setTextScale(float textScale) {
        this.jobParameters = new JobParameters(this.jobParameters.jobTheme, textScale);
        clearAndRedrawMapView();
    }

    /**
     * Takes a screenshot of the currently visible map and saves it as a compressed image. Zoom buttons, scale bar, FPS
     * counter, overlays, menus and the title bar are not included in the screenshot.
     *
     * @param outputFile     the image file. If the file already exists, it will be overwritten.
     * @param compressFormat the file format of the compressed image.
     * @param quality        value from 0 (low) to 100 (high). Has no effect on some formats like PNG.
     * @return true if the image was saved successfully, false otherwise.
     * @throws IOException if an error occurs while writing the image file.
     */
    public boolean takeScreenshot(CompressFormat compressFormat, int quality, File outputFile) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        boolean success = this.frameBuffer.compress(compressFormat, quality, outputStream);
        outputStream.close();
        return success;
    }

    /**
     * Zooms in or out by the given amount of zoom levels.
     *
     * @param zoomLevelDiff the difference to the current zoom level.
     * @param zoomStart     the zoom factor at the begin of the animation.
     * @return true if the zoom level was changed, false otherwise.
     */
    public boolean zoom(byte zoomLevelDiff, float zoomStart) {
        float matrixScaleFactor;
        if (zoomLevelDiff > 0) {
            // check if zoom in is possible
            if (this.mapViewPosition.getZoomLevel() + zoomLevelDiff > getMaximumPossibleZoomLevel()) {
                return false;
            }
            matrixScaleFactor = 1 << zoomLevelDiff;
        } else if (zoomLevelDiff < 0) {
            // check if zoom out is possible
            if (this.mapViewPosition.getZoomLevel() + zoomLevelDiff < this.mapZoomControls.getZoomLevelMin()) {
                return false;
            }
            matrixScaleFactor = 1.0f / (1 << -zoomLevelDiff);
        } else {
            // zoom level is unchanged
            matrixScaleFactor = 1;
        }

        this.mapViewPosition.setZoomLevel((byte) (this.mapViewPosition.getZoomLevel() + zoomLevelDiff));
        this.mapZoomControls.onZoomLevelChange(this.mapViewPosition.getZoomLevel());

        this.zoomAnimator.setParameters(zoomStart, matrixScaleFactor, getWidth() >> 1, getHeight() >> 1);
        this.zoomAnimator.startAnimation();
        return true;
    }

    private void setMapGeneratorInternal(MapGenerator mapGenerator) {
        if (mapGenerator == null) {
            throw new IllegalArgumentException("mapGenerator must not be null");
        }

        if (mapGenerator instanceof DatabaseRenderer) {
            ((DatabaseRenderer) mapGenerator).setMapDatabase(this.mapDatabase);
        }
        this.mapGenerator = mapGenerator;
        this.mapWorker.setMapGenerator(this.mapGenerator);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.frameBuffer.draw(canvas);
        synchronized (this.overlays) {
            for (int i = 0, n = this.overlays.size(); i < n; ++i) {
                try {
                    this.overlays.get(i).draw(canvas);
                } catch (Exception e) {
                    android.util.Log.e("MAPSFORG#MAPVIEW#ONDRAW", "Problems drawing overlay", e);
                    e.printStackTrace();
                }
            }
        }

        if (this.mapScaleBar.isShowMapScaleBar()) {
            this.mapScaleBar.draw(canvas);
        }

        if (this.fpsCounter.isShowFpsCounter()) {
            this.fpsCounter.draw(canvas);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mapZoomControls.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // find out how big the zoom controls should be
        this.mapZoomControls.measure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.AT_MOST));

        // make sure that MapView is big enough to display the zoom controls
        setMeasuredDimension(Math.max(MeasureSpec.getSize(widthMeasureSpec), this.mapZoomControls.getMeasuredWidth()),
                Math.max(MeasureSpec.getSize(heightMeasureSpec), this.mapZoomControls.getMeasuredHeight()));
    }

    @Override
    protected synchronized void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        this.frameBuffer.destroy();

        if (width > 0 && height > 0) {
            this.frameBuffer.onSizeChanged();
            redrawTiles();

            synchronized (this.overlays) {
                for (int i = 0, n = this.overlays.size(); i < n; ++i) {
                    this.overlays.get(i).onSizeChanged();
                }
            }
        }
    }

    void clearAndRedrawMapView() {
        this.jobQueue.clear();
        this.frameBuffer.clear();
        redrawTiles();
    }

    void destroy() {
        this.overlays.clear();

        this.mapMover.interrupt();
        this.mapWorker.interrupt();
        this.zoomAnimator.interrupt();

        try {
            this.mapWorker.join();
        } catch (InterruptedException e) {
            // restore the interrupted status
            Thread.currentThread().interrupt();
        }

        this.frameBuffer.destroy();
        this.touchEventHandler.destroy();
        this.mapScaleBar.destroy();
        try {
            if (this.inMemoryTileCache != null)
                this.inMemoryTileCache.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (this.fileSystemTileCache != null)
                this.fileSystemTileCache.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.mapDatabase.closeFile();
    }

    /**
     * @return the maximum possible zoom level.
     */
    byte getMaximumPossibleZoomLevel() {
        return (byte) Math.min(this.mapZoomControls.getZoomLevelMax(), this.mapGenerator.getZoomLevelMax());
    }

    /**
     * @return true if the current center position of this MapView is valid, false otherwise.
     */
    boolean hasValidCenter() {
        if (!this.mapViewPosition.isValid()) {
            return false;
        } else if (!this.mapGenerator.requiresInternetConnection()
                && (!this.mapDatabase.hasOpenFile() || !this.mapDatabase.getMapFileInfo().boundingBox
                .contains(getMapPosition().getMapCenter()))) {
            return false;
        }

        return true;
    }

    byte limitZoomLevel(byte zoom) {
        return (byte) Math.max(Math.min(zoom, getMaximumPossibleZoomLevel()), this.mapZoomControls.getZoomLevelMin());
    }

    void onPause() {
        this.mapWorker.pause();
        this.mapMover.pause();
        this.zoomAnimator.pause();
    }

    void onResume() {
        this.mapWorker.proceed();
        this.mapMover.proceed();
        this.zoomAnimator.proceed();
    }

    /**
     * Sets the center and zoom level of this MapView and triggers a redraw.
     *
     * @param mapPosition the new map position of this MapView.
     */
    void setCenterAndZoom(MapPosition mapPosition) {

        if (hasValidCenter()) {
            // calculate the distance between previous and current position
            MapPosition mapPositionOld = this.mapViewPosition.getMapPosition();

            GeoPoint geoPointOld = mapPositionOld.geoPoint;
            GeoPoint geoPointNew = mapPosition.geoPoint;
            double oldPixelX = MercatorProjection.longitudeToPixelX(geoPointOld.getLongitude(),
                    mapPositionOld.zoomLevel);
            double newPixelX = MercatorProjection.longitudeToPixelX(geoPointNew.getLongitude(), mapPosition.zoomLevel);

            double oldPixelY = MercatorProjection.latitudeToPixelY(geoPointOld.getLatitude(), mapPositionOld.zoomLevel);
            double newPixelY = MercatorProjection.latitudeToPixelY(geoPointNew.getLatitude(), mapPosition.zoomLevel);

            float matrixTranslateX = (float) (oldPixelX - newPixelX);
            float matrixTranslateY = (float) (oldPixelY - newPixelY);
            this.frameBuffer.matrixPostTranslate(matrixTranslateX, matrixTranslateY);
        }

        this.mapViewPosition.setMapCenterAndZoomLevel(mapPosition);
        this.mapZoomControls.onZoomLevelChange(this.mapViewPosition.getZoomLevel());
        redrawTiles();
    }
}
