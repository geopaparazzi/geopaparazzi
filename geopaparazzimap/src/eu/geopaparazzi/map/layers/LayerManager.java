package eu.geopaparazzi.map.layers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.layers.Layer;
import org.oscim.map.Layers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.map.GPMapThemes;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.MapTypeHandler;
import eu.geopaparazzi.map.layers.interfaces.IGpLayer;
import eu.geopaparazzi.map.layers.interfaces.ISystemLayer;
import eu.geopaparazzi.map.layers.interfaces.IVectorTileOfflineLayer;
import eu.geopaparazzi.map.layers.interfaces.IVectorTileOnlineLayer;
import eu.geopaparazzi.map.layers.systemlayers.BookmarkLayer;
import eu.geopaparazzi.map.layers.systemlayers.CurrentGpsLogLayer;
import eu.geopaparazzi.map.layers.systemlayers.GpsLogsLayer;
import eu.geopaparazzi.map.layers.systemlayers.GpsPositionLayer;
import eu.geopaparazzi.map.layers.systemlayers.GpsPositionTextLayer;
import eu.geopaparazzi.map.layers.systemlayers.ImagesLayer;
import eu.geopaparazzi.map.layers.systemlayers.NotesLayer;
import eu.geopaparazzi.map.layers.userlayers.MBTilesLayer;
import eu.geopaparazzi.map.layers.userlayers.MapsforgeLayer;
import eu.geopaparazzi.map.layers.userlayers.VectorTilesServiceLayer;


public enum LayerManager {
    INSTANCE;

    public static final String LAYERS = "layers";
    public static final String GP_LOADED_USERMAPS_KEY = "GP_LOADED_USERMAPS_KEY";
    public static final String GP_LOADED_SYSTEMMAPS_KEY = "GP_LOADED_SYSTEMMAPS_KEY";
    private List<JSONObject> userLayersDefinitions = new ArrayList<>();
    private List<JSONObject> systemLayersDefinitions = new ArrayList<>();

    /**
     * Initialize the layers from preferences
     */
    public void init() throws Exception {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GPApplication.getInstance());
        String loadedUserMapsJson = preferences.getString(GP_LOADED_USERMAPS_KEY, "{}");

        JSONObject root = new JSONObject(loadedUserMapsJson);
        if (root.has(LAYERS)) {
            JSONArray layersArray = root.getJSONArray(LAYERS);
            int length = layersArray.length();
            for (int i = 0; i < length; i++) {
                JSONObject jsonObject = layersArray.getJSONObject(i);
                userLayersDefinitions.add(jsonObject);
            }
        }
//        if (userLayersDefinitions.size() == 0) {
//            // add a default
//            JSONObject jo = new JSONObject();
//            jo.put(IGpLayer.LAYERTYPE_TAG, VectorTilesServiceLayer.class.getCanonicalName());
//            jo.put(IGpLayer.LAYERNAME_TAG, "OpenScienceMap VTM");
//            jo.put(IGpLayer.LAYERURL_TAG, "http://opensciencemap.org/tiles/vtm");
//            jo.put(IGpLayer.LAYERPATH_TAG, "/{Z}/{X}/{Y}.vtm");
//            jo.put(IGpLayer.LAYERENABLED_TAG, true);
//            userLayersDefinitions.add(jo);
//        }


        String loadedSystemMapsJson = preferences.getString(GP_LOADED_SYSTEMMAPS_KEY, "{}");
        root = new JSONObject(loadedSystemMapsJson);
        if (root.has(LAYERS)) {
            JSONArray layersArray = root.getJSONArray(LAYERS);
            int length = layersArray.length();
            for (int i = 0; i < length; i++) {
                JSONObject jsonObject = layersArray.getJSONObject(i);
                systemLayersDefinitions.add(jsonObject);
            }
        } else {
            // define system layers
            JSONObject jo = new JSONObject();
            jo.put(IGpLayer.LAYERTYPE_TAG, GpsLogsLayer.class.getCanonicalName());
            jo.put(IGpLayer.LAYERNAME_TAG, GpsLogsLayer.NAME);
            jo.put(IGpLayer.LAYERENABLED_TAG, true);
            systemLayersDefinitions.add(jo);

            jo = new JSONObject();
            jo.put(IGpLayer.LAYERTYPE_TAG, CurrentGpsLogLayer.class.getCanonicalName());
            jo.put(IGpLayer.LAYERNAME_TAG, CurrentGpsLogLayer.NAME);
            jo.put(IGpLayer.LAYERENABLED_TAG, true);
            systemLayersDefinitions.add(jo);

            jo = new JSONObject();
            jo.put(IGpLayer.LAYERTYPE_TAG, BookmarkLayer.class.getCanonicalName());
            jo.put(IGpLayer.LAYERNAME_TAG, BookmarkLayer.NAME);
            jo.put(IGpLayer.LAYERENABLED_TAG, true);
            systemLayersDefinitions.add(jo);

            jo = new JSONObject();
            jo.put(IGpLayer.LAYERTYPE_TAG, ImagesLayer.class.getCanonicalName());
            jo.put(IGpLayer.LAYERNAME_TAG, ImagesLayer.NAME);
            jo.put(IGpLayer.LAYERENABLED_TAG, true);
            systemLayersDefinitions.add(jo);

            jo = new JSONObject();
            jo.put(IGpLayer.LAYERTYPE_TAG, NotesLayer.class.getCanonicalName());
            jo.put(IGpLayer.LAYERNAME_TAG, NotesLayer.NAME);
            jo.put(IGpLayer.LAYERENABLED_TAG, true);
            systemLayersDefinitions.add(jo);

            jo = new JSONObject();
            jo.put(IGpLayer.LAYERTYPE_TAG, GpsPositionLayer.class.getCanonicalName());
            jo.put(IGpLayer.LAYERNAME_TAG, GpsPositionLayer.NAME);
            jo.put(IGpLayer.LAYERENABLED_TAG, true);
            systemLayersDefinitions.add(jo);

            jo = new JSONObject();
            jo.put(IGpLayer.LAYERTYPE_TAG, GpsPositionTextLayer.class.getCanonicalName());
            jo.put(IGpLayer.LAYERNAME_TAG, GpsPositionTextLayer.NAME);
            jo.put(IGpLayer.LAYERENABLED_TAG, false);
            systemLayersDefinitions.add(jo);
        }

    }

    public void createGroups(GPMapView mapView) {
        Layers layers = mapView.map().layers();
        layers.addGroup(LayerGroups.GROUP_USERLAYERS.getGroupId());
        layers.addGroup(LayerGroups.GROUP_SYSTEM.getGroupId());
        layers.addGroup(LayerGroups.GROUP_3D.getGroupId());
        layers.addGroup(LayerGroups.GROUP_SYSTEM_TOP.getGroupId());
    }

    public List<JSONObject> getUserLayersDefinitions() {
        return userLayersDefinitions;
    }

    public List<JSONObject> getSystemLayersDefinitions() {
        return systemLayersDefinitions;
    }

    /**
     * Load all the layers in the map.
     *
     * @param mapView the map view.
     * @throws JSONException
     */
    public void loadInMap(GPMapView mapView) throws Exception {
        mapView.map().layers().removeIf(layer -> {
            return layer instanceof IGpLayer;
        });

        for (JSONObject layerDefinition : userLayersDefinitions) {
            try {
                String layerClass = layerDefinition.getString(IGpLayer.LAYERTYPE_TAG);
                String name = layerDefinition.getString(IGpLayer.LAYERNAME_TAG);
                boolean isEnabled = true;
                boolean hasEnabled = layerDefinition.has(IGpLayer.LAYERENABLED_TAG);
                if (hasEnabled)
                    isEnabled = layerDefinition.getBoolean(IGpLayer.LAYERENABLED_TAG);
                if (layerClass.equals(MapsforgeLayer.class.getCanonicalName())) {
                    String mapPath = layerDefinition.getString(IGpLayer.LAYERPATH_TAG);
                    String[] mapPaths = mapPath.split(IGpLayer.PATHS_DELIMITER);
                    MapsforgeLayer mapsforgeLayer = new MapsforgeLayer(mapView, name, mapPaths);
                    mapsforgeLayer.load();
                    mapsforgeLayer.setEnabled(isEnabled);
                } else if (layerClass.equals(MBTilesLayer.class.getCanonicalName())) {
                    String path = layerDefinition.getString(IGpLayer.LAYERPATH_TAG);
                    MBTilesLayer mbtilesLayer = new MBTilesLayer(mapView, path, null, null);
                    mbtilesLayer.load();
                    mbtilesLayer.setEnabled(isEnabled);
                } else if (layerClass.equals(VectorTilesServiceLayer.class.getCanonicalName())) {
                    String tilePath = layerDefinition.getString(IGpLayer.LAYERPATH_TAG);
                    String url = layerDefinition.getString(IGpLayer.LAYERURL_TAG);
                    VectorTilesServiceLayer vtsLayer = new VectorTilesServiceLayer(mapView, null, url, tilePath);
                    vtsLayer.load();
                    vtsLayer.setEnabled(isEnabled);
                }
            } catch (Exception e) {
                GPLog.error(this, "Unable to load layer: " + layerDefinition.toString(2), e);
            }
        }
        if (systemLayersDefinitions.size() > 0) {
            for (JSONObject layerDefinition : systemLayersDefinitions) {
                String layerClass = layerDefinition.getString(IGpLayer.LAYERTYPE_TAG);
                boolean isEnabled = true;
                boolean hasEnabled = layerDefinition.has(IGpLayer.LAYERENABLED_TAG);
                if (hasEnabled)
                    isEnabled = layerDefinition.getBoolean(IGpLayer.LAYERENABLED_TAG);

                if (layerClass.equals(GpsLogsLayer.class.getCanonicalName())) {
                    GpsLogsLayer sysLayer = new GpsLogsLayer(mapView);
                    sysLayer.load();
                    sysLayer.setEnabled(isEnabled);
                } else if (layerClass.equals(CurrentGpsLogLayer.class.getCanonicalName())) {
                    CurrentGpsLogLayer sysLayer = new CurrentGpsLogLayer(mapView);
                    sysLayer.load();
                    sysLayer.setEnabled(isEnabled);
                } else if (layerClass.equals(BookmarkLayer.class.getCanonicalName())) {
                    BookmarkLayer sysLayer = new BookmarkLayer(mapView);
                    sysLayer.load();
                    sysLayer.setEnabled(isEnabled);
                } else if (layerClass.equals(ImagesLayer.class.getCanonicalName())) {
                    ImagesLayer sysLayer = new ImagesLayer(mapView);
                    sysLayer.load();
                    sysLayer.setEnabled(isEnabled);
                } else if (layerClass.equals(NotesLayer.class.getCanonicalName())) {
                    NotesLayer sysLayer = new NotesLayer(mapView);
                    sysLayer.load();
                    sysLayer.setEnabled(isEnabled);
                } else if (layerClass.equals(GpsPositionLayer.class.getCanonicalName())) {
                    GpsPositionLayer sysLayer = new GpsPositionLayer(mapView);
                    sysLayer.load();
                    sysLayer.setEnabled(isEnabled);
                } else if (layerClass.equals(GpsPositionTextLayer.class.getCanonicalName())) {
                    GpsPositionTextLayer sysLayer = new GpsPositionTextLayer(mapView);
                    sysLayer.load();
                    sysLayer.setEnabled(isEnabled);
                }
            }
        } else {
            loadSystemLayers(mapView, systemLayersDefinitions);
        }
    }

    public void loadSystemLayers(GPMapView mapView, List<JSONObject> systemLayersDefinitions) throws Exception {
        GpsLogsLayer gpsLogsLayer = new GpsLogsLayer(mapView);
        gpsLogsLayer.load();
        systemLayersDefinitions.add(gpsLogsLayer.toJson());

        CurrentGpsLogLayer currentGpsLogLayer = new CurrentGpsLogLayer(mapView);
        currentGpsLogLayer.load();
        systemLayersDefinitions.add(currentGpsLogLayer.toJson());

        BookmarkLayer bookmarkLayer = new BookmarkLayer(mapView);
        bookmarkLayer.load();
        systemLayersDefinitions.add(bookmarkLayer.toJson());

        ImagesLayer imagesLayer = new ImagesLayer(mapView);
        imagesLayer.load();
        systemLayersDefinitions.add(imagesLayer.toJson());

        NotesLayer notesLayer = new NotesLayer(mapView);
        notesLayer.load();
        systemLayersDefinitions.add(notesLayer.toJson());

        GpsPositionLayer gpsPositionLayer = new GpsPositionLayer(mapView);
        gpsPositionLayer.load();
        systemLayersDefinitions.add(gpsPositionLayer.toJson());

        GpsPositionTextLayer gpsPositionTextLayer = new GpsPositionTextLayer(mapView);
        gpsPositionTextLayer.load();
        systemLayersDefinitions.add(gpsPositionTextLayer.toJson());
    }


//    public void updateFromMap(GPMapView mapView) throws JSONException {
//        userLayersDefinitions.clear();
//        Layers layers = mapView.map().layers();
//
//
//        int size = layers.size();
//        for (int i = 0; i < size; i++) {
//            Layer layer = layers.get(i);
//            if (layer instanceof IGpLayer && !(layer instanceof ISystemLayer)) {
//                IGpLayer iPersistenceLayer = (IGpLayer) layer;
//                JSONObject jsonObject = iPersistenceLayer.toJson();
//                userLayersDefinitions.add(jsonObject);
//            }
//        }
//
//    }

    /**
     * Dispose all the layers and save the state to preferences.
     */
    public void dispose(GPMapView mapView) throws JSONException {
        if (mapView != null) {
            JSONArray usersLayersArray = new JSONArray();
            JSONObject usersRoot = new JSONObject();
            usersRoot.put(LAYERS, usersLayersArray);

            JSONArray systemLayersArray = new JSONArray();
            JSONObject systemRoot = new JSONObject();
            systemRoot.put(LAYERS, systemLayersArray);

            for (Layer layer : mapView.map().layers()) {
                if (layer instanceof IGpLayer) {
                    if (layer instanceof ISystemLayer) {
                        IGpLayer gpLayer = (IGpLayer) layer;
                        JSONObject jsonObject = gpLayer.toJson();
                        systemLayersArray.put(jsonObject);
                        gpLayer.dispose();
                    } else {
                        IGpLayer gpLayer = (IGpLayer) layer;
                        JSONObject jsonObject = gpLayer.toJson();
                        usersLayersArray.put(jsonObject);
                        gpLayer.dispose();
                    }
                }
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GPApplication.getInstance());

            SharedPreferences.Editor editor = preferences.edit();

            String jsonString = usersRoot.toString();
            editor.putString(GP_LOADED_USERMAPS_KEY, jsonString);

            jsonString = systemRoot.toString();
            editor.putString(GP_LOADED_SYSTEMMAPS_KEY, jsonString);

            editor.apply();
        }
    }


    /**
     * Remove the current layer from the map view.
     *
     * @return the position the layer had or -1 if the layer could not be found.
     */
    public int removeFromMap(GPMapView mapView, String id) {
        Layers layers = mapView.map().layers();
        int index = 0;
        for (Layer layer : layers) {
            if (layer instanceof IGpLayer) {
                IGpLayer gpLayer = (IGpLayer) layer;
                if (gpLayer.getId().equals(id)) {
                    layers.remove(index);
                    return index;
                }
            }
            index++;
        }
        return -1;
    }

//    public void swapLayers(GPMapView mapView, int oldIndex, int newIndex) {
//        Layers layers = mapView.map().layers();
//        Layer removed = layers.remove(oldIndex);
//        layers.add(newIndex, removed);
//    }


    public void onResume(GPMapView mapView) {
        if (mapView != null) {
            Layers layers = mapView.map().layers();
//            int count = (int) layers.stream().filter(l -> l instanceof IGpLayer).count();
//            if (count != userLayersDefinitions.size() + systemLayersDefinitions.size()) {
                try {
                    loadInMap(mapView);
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                }
//            }
            for (Layer layer : layers) {
                if (layer instanceof IGpLayer) {
                    IGpLayer gpLayer = (IGpLayer) layer;
                    gpLayer.onResume();
                }
            }
            int count = (int) layers.stream().filter(l -> l instanceof IVectorTileOfflineLayer || l instanceof IVectorTileOnlineLayer).count();
            if (count > 0) {
                mapView.setTheme(GPMapThemes.DEFAULT);
            }
        }
    }

    public void onPause(GPMapView mapView) {
        if (mapView != null)
            for (Layer layer : mapView.map().layers()) {
                if (layer instanceof IGpLayer) {
                    IGpLayer gpLayer = (IGpLayer) layer;
                    gpLayer.onPause();
                }
            }
    }

    public int addMapFile(File finalFile, Integer index) throws Exception {
        int i = finalFile.getName().lastIndexOf('.');
        String name = FileUtilities.getNameWithoutExtention(finalFile);
        String extension = finalFile.getName().substring(i + 1);
        MapTypeHandler mapTypeHandler = MapTypeHandler.forExtension(extension);
        if (mapTypeHandler == null) {
            throw new RuntimeException();
        }
        JSONObject jo = null;
        switch (mapTypeHandler) {
            case MAPSFORGE: {
                jo = new JSONObject();
                jo.put(IGpLayer.LAYERTYPE_TAG, MapsforgeLayer.class.getCanonicalName());
                jo.put(IGpLayer.LAYERNAME_TAG, name);
                jo.put(IGpLayer.LAYERPATH_TAG, finalFile.getAbsolutePath());
                if (!userLayersDefinitions.contains(jo))
                    if (index != null) {
                        userLayersDefinitions.add(index, jo);
                    } else {
                        userLayersDefinitions.add(jo);
                    }
                break;
            }
            case MBTILES: {
                jo = new JSONObject();
                jo.put(IGpLayer.LAYERTYPE_TAG, MBTilesLayer.class.getCanonicalName());
                jo.put(IGpLayer.LAYERNAME_TAG, name);
                jo.put(IGpLayer.LAYERPATH_TAG, finalFile.getAbsolutePath());
                if (!userLayersDefinitions.contains(jo))
                    if (index != null) {
                        userLayersDefinitions.add(index, jo);
                    } else {
                        userLayersDefinitions.add(jo);
                    }
            }
        }
        if (jo != null) {
            return userLayersDefinitions.indexOf(jo);
        } else {
            return 0;
        }


    }

    public void setEnabled(boolean isSystem, int position, boolean enabled) {
        try {
            if (isSystem) {
                JSONObject layerObj = systemLayersDefinitions.get(position);
                layerObj.put(IGpLayer.LAYERENABLED_TAG, enabled);
            } else {
                JSONObject layerObj = userLayersDefinitions.get(position);
                layerObj.put(IGpLayer.LAYERENABLED_TAG, enabled);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
