package eu.geopaparazzi.map.layers.userlayers;

import android.os.Build;

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
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.IVectorTileOfflineLayer;

public class MapsforgeLayer extends OsmTileLayer implements IVectorTileOfflineLayer {

    private String name;
    private String path;
    private GPMapView mapView;
    private boolean do3d;
    private boolean doLabels;
    private String[] mapPaths;
    private MultiMapFileTileSource tileSource;

    public MapsforgeLayer(GPMapView mapView, String name, boolean do3d, boolean doLabels, String... mapPaths) {
        super(mapView.map());
        this.mapView = mapView;
        this.do3d = do3d;
        this.doLabels = doLabels;
        this.mapPaths = mapPaths;

        List<String> names = new ArrayList<>();
        for (String mapPath : mapPaths) {
            String mapName = FileUtilities.getNameWithoutExtention(new File(mapPath));
            names.add(mapName);
        }
        this.name = name;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (name == null)
                this.name = names.stream().collect(Collectors.joining("-"));
            path = Arrays.stream(mapPaths).collect(Collectors.joining(PATHS_DELIMITER));
        } else {
            // TODO remove when minsdk gets 24
            if (name == null)
                this.name = Utilities.joinStrings("-", names.toArray(new String[0]));
            path = Utilities.joinStrings(PATHS_DELIMITER, mapPaths);
        }
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
            layers.add(this, LayerGroups.GROUP_MAPLAYERS.getGroupId());

            // Building layer
            if (do3d)
                layers.add(new BuildingLayer(map(), this), LayerGroups.GROUP_3D.getGroupId());

            // labels layer
            if (doLabels)
                layers.add(new LabelLayer(map(), this), LayerGroups.GROUP_3D.getGroupId());
        }
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
        jo.put(LAYERDO3D_TAG, do3d);
        jo.put(LAYERDOLABELS_TAG, doLabels);
        return jo;
    }

    @Override
    public void dispose() {
        if (tileSource != null)
            tileSource.close();
    }
}
