package eu.geopaparazzi.map.layers.userlayers;

import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.android.cache.TileCache;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Layers;
import org.oscim.tiling.source.bitmap.BitmapTileSource;

import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.IRasterTileOnlineLayer;
import eu.geopaparazzi.map.layers.utils.EOnlineTileSources;

public class BitmapTileServiceLayer extends BitmapTileLayer implements IRasterTileOnlineLayer {

    private final GPMapView mapView;
    private final String name;
    private final String url;
    private final String tilePath;
    private final int maxZoom;
    private float bitmapAlpha;

    public BitmapTileServiceLayer(GPMapView mapView, EOnlineTileSources onlineTileSource, float bitmapAlpha) {
        super(mapView.map(), getTileSource(mapView, onlineTileSource.getUrl(), onlineTileSource.getTilePath(), onlineTileSource.getMaxZoom()), bitmapAlpha);

        this.mapView = mapView;
        this.name = onlineTileSource.getName();
        this.url = onlineTileSource.getUrl();
        this.tilePath = onlineTileSource.getTilePath();
        this.maxZoom = onlineTileSource.getMaxZoom();
    }

    public BitmapTileServiceLayer(GPMapView mapView, String name, String url, String tilePath, int maxZoom, float bitmapAlpha) {
        super(mapView.map(), getTileSource(mapView, url, tilePath, maxZoom), bitmapAlpha);

        this.mapView = mapView;
        this.name = name != null ? name : url;
        this.url = url;
        this.tilePath = tilePath;
        this.maxZoom = maxZoom;
        this.bitmapAlpha = bitmapAlpha;
    }

    private static BitmapTileSource getTileSource(GPMapView mapView, String url, String tilePath, int maxZoom) {
        BitmapTileSource tileSource = BitmapTileSource.builder()
                .url(url)
                .tilePath(tilePath)
                .zoomMax(maxZoom).build();

        String cacheFile = tileSource.getUrl()
                .toString()
                .replaceFirst("https?://", "")//NON-NLS
                .replaceAll("/", "-");

        log.debug("use bitmap cache {}", cacheFile);//NON-NLS
        TileCache mCache = new TileCache(mapView.getContext(), null, cacheFile);
        mCache.setCacheSize(512 * (1 << 10));
        tileSource.setCache(mCache);

        return tileSource;
    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public GPMapView getMapView() {
        return mapView;
    }

    @Override
    public void load() {
        Layers layers = map().layers();
        layers.add(this, LayerGroups.GROUP_MAPLAYERS.getGroupId());
    }

    @Override
    public void reloadData() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void dispose() {
        onDetach();
        if (mTileSource != null)
            mTileSource.close();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject jo = toDefaultJson();
        jo.put(LAYERPATH_TAG, tilePath);
        jo.put(LAYERURL_TAG, url);
        jo.put(LAYERMAXZOOM_TAG, maxZoom);
        jo.put(LAYERALPHA_TAG, bitmapAlpha);
        return jo;
    }
}
