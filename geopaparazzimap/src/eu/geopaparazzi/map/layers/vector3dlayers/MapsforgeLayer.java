package eu.geopaparazzi.map.layers.vector3dlayers;

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
import eu.geopaparazzi.map.layers.persistence.IVectorTileOfflineLayer;

public class MapsforgeLayer extends OsmTileLayer implements IVectorTileOfflineLayer {

    private final String name;
    private String path;
    private GPMapView mapView;
    private String[] mapPaths;

    public MapsforgeLayer(GPMapView mapView, String... mapPaths) {
        super(mapView.map());
        this.mapView = mapView;
        this.mapPaths = mapPaths;

        List<String> names = new ArrayList<>();
        for (String mapPath : mapPaths) {
            String mapName = FileUtilities.getNameWithoutExtention(new File(mapPath));
            names.add(mapName);
        }
        name = names.stream().collect(Collectors.joining("-"));
        path = Arrays.stream(mapPaths).collect(Collectors.joining(PATHS_DELIMITER));
    }

    public void load(Integer positionIndex) {
        MultiMapFileTileSource tileSource = new MultiMapFileTileSource();
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

            int systemLayersIndex = mapView.getSystemLayersIndex();
            Layers layers = map().layers();
            if (positionIndex != null) {
                layers.add(positionIndex, this);
            } else {
                layers.add(systemLayersIndex, this);
            }
            // Building layer
            systemLayersIndex = mapView.getSystemLayersIndex();
            layers.add(systemLayersIndex, new BuildingLayer(map(), this));

            // labels layer
            layers.add(map().layers().size() - 1, new LabelLayer(map(), this));

        }
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
        // name
        // path
        // theme
        // type
        JSONObject jo = new JSONObject();
        jo.put(LAYERNAME_TAG, name);
        jo.put(LAYERPATH_TAG, path);
        return jo;
    }
}
