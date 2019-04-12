package eu.geopaparazzi.map.layers.userlayers;

import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
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
import eu.geopaparazzi.map.layers.LayerManager;
import eu.geopaparazzi.map.layers.interfaces.IVectorTileOfflineLayer;

public class MapsforgeLayer extends OsmTileLayer implements IVectorTileOfflineLayer {

    private String name;
    private String path;
    private GPMapView mapView;
    private String[] mapPaths;
    private MultiMapFileTileSource tileSource;

    public MapsforgeLayer(GPMapView mapView, String name, String... mapPaths) {
        super(mapView.map());
        this.mapView = mapView;
        this.mapPaths = mapPaths;

        List<String> names = new ArrayList<>();
        for (String mapPath : mapPaths) {
            String mapName = FileUtilities.getNameWithoutExtention(new File(mapPath));
            names.add(mapName);
        }
        this.name = name;
        if (name == null)
            this.name = names.stream().collect(Collectors.joining("-"));
        path = Arrays.stream(mapPaths).collect(Collectors.joining(PATHS_DELIMITER));
    }

    public void load() {
        tileSource = new MultiMapFileTileSource();
        boolean okToLoad = false;
        for (String mapPath : mapPaths) {
            MapFileTileSource ts = new MapFileTileSource();
            okToLoad = ts.setMapFile(mapPath);
            if (okToLoad) {
                tileSource.add(ts);
            }
        }
        if (okToLoad) {
            setTileSource(tileSource);

            Layers layers = mapView.map().layers();
            layers.add(this, LayerGroups.GROUP_USERLAYERS.getGroupId());

            // Building layer
            layers.add(new BuildingLayer(map(), this), LayerGroups.GROUP_3D.getGroupId());

            // labels layer
            layers.add(new LabelLayer(map(), this), LayerGroups.GROUP_3D.getGroupId());
        }
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public String getPath() {
        return path;
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
        jo.put(LAYERPATH_TAG, path);
        return jo;
    }

    @Override
    public void dispose() {
        if (tileSource != null)
            tileSource.close();
    }
}
