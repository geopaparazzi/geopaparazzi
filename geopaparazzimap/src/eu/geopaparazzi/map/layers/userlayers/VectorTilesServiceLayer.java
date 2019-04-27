package eu.geopaparazzi.map.layers.userlayers;

import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.VectorTilesOnlineSource;
import eu.geopaparazzi.map.layers.interfaces.IVectorTileOfflineLayer;
import eu.geopaparazzi.map.layers.interfaces.IVectorTileOnlineLayer;

public class VectorTilesServiceLayer extends OsmTileLayer implements IVectorTileOnlineLayer {

    private String name;
    private GPMapView mapView;
    private String url;
    private String tilePath;
    private UrlTileSource tileSource;

    public VectorTilesServiceLayer(GPMapView mapView, String name, String url, String tilePath) {
        super(mapView.map());
        this.mapView = mapView;
        this.url = url;
        this.tilePath = tilePath;

        this.name = name != null ? name : url;
    }

    public void load() {
        tileSource = VectorTilesOnlineSource.builder()
//                .apiKey("xxxxxxx") // Put a proper API key
                .url(url).tilePath(tilePath)
                .zoomMin(0).zoomMax(20)
                .httpFactory(new OkHttpEngine.OkHttpFactory())
                //.locale("en")
                .build();

//            // Cache the tiles into a local SQLite database
//            mCache = new TileCache(this, null, "tile.db");
//            mCache.setCacheSize(512 * (1 << 10));
//            tileSource.setCache(mCache);

        setTileSource(tileSource);

        Layers layers = mapView.map().layers();
        layers.add(this, LayerGroups.GROUP_USERLAYERS.getGroupId());

//        // Building layer
//        layers.add(new BuildingLayer(map(), this), LayerGroups.GROUP_3D.getGroupId());
//
//        // labels layer
//        layers.add(new LabelLayer(map(), this), LayerGroups.GROUP_3D.getGroupId());
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

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
    public JSONObject toJson() throws JSONException {
        JSONObject jo = toDefaultJson();
        jo.put(LAYERPATH_TAG, tilePath);
        jo.put(LAYERURL_TAG, url);
        return jo;
    }

    @Override
    public void dispose() {
        if (tileSource != null)
            tileSource.close();
    }
}
