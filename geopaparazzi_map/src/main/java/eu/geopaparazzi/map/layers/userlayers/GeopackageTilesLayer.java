package eu.geopaparazzi.map.layers.userlayers;

import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.map.Layers;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.layers.GeopackageTileSource;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.IRasterTileOfflineLayer;
import eu.geopaparazzi.map.layers.utils.GeopackageConnectionsHandler;

public class GeopackageTilesLayer extends BitmapTileLayer implements IRasterTileOfflineLayer {
    private final String name;
    private Float alpha;
    private Integer transparentColor;
    private GPMapView mapView;
    private String dbPath;

    public GeopackageTilesLayer(GPMapView mapView, String dbPath, String tableName, Float alpha, Integer transparentColor) throws Exception {
        super(mapView.map(), new GeopackageTileSource(dbPath, tableName, null, transparentColor));
        this.mapView = mapView;
        this.dbPath = dbPath;
        name = tableName;
        this.alpha = alpha;
        if (alpha != null)
            setBitmapAlpha(alpha, false);
        this.transparentColor = transparentColor;
    }

    @Override
    public String getPath() {
        return dbPath;
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
        try {
            GeopackageConnectionsHandler.INSTANCE.openTable(dbPath, name);
        } catch (Exception e) {
            GPLog.error(this, "ERROR", e);
        }
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
    public JSONObject toJson() throws JSONException {
        JSONObject jo = toDefaultJson();
        jo.put(LAYERPATH_TAG, dbPath);
        if (alpha != null)
            jo.put(LAYERALPHA_TAG, alpha);
        if (transparentColor != null)
            jo.put(LAYERTRANSPARENTCOLOR_TAG, transparentColor);
        return jo;
    }

    @Override
    public void dispose() {
        mTileSource.close();
    }
}
