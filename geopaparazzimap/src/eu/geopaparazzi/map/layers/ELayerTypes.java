package eu.geopaparazzi.map.layers;

import eu.geopaparazzi.map.layers.userlayers.BitmapTileServiceLayer;
import eu.geopaparazzi.map.layers.userlayers.MBTilesLayer;
import eu.geopaparazzi.map.layers.userlayers.MapsforgeLayer;
import eu.geopaparazzi.map.layers.userlayers.SpatialiteTableLayer;
import eu.geopaparazzi.map.layers.userlayers.VectorTilesServiceLayer;

public enum ELayerTypes {
    MAPSFORGE("mapsforge", MapsforgeLayer.class.getCanonicalName()),
    VECTORTILESSERVICE("VectorTilesService", VectorTilesServiceLayer.class.getCanonicalName()),
    BITMAPTILESERVICE("BitmapTileService", BitmapTileServiceLayer.class.getCanonicalName()),
    MBTILES("mbtiles", MBTilesLayer.class.getCanonicalName()),
    MAPURL("mapurl", BitmapTileServiceLayer.class.getCanonicalName()),
    SPATIALITE("sqlite", SpatialiteTableLayer.class.getCanonicalName());

    public static final int[] OPACITY_LEVELS = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

    public static final String MBTILES_EXT = "mbtiles";
    public static final String MAPSFORGE_EXT = "map";
    public static final String SPATIALITE_EXT = "sqlite";
    public static final String RASTERLITE2_EXT = "rl2";
    public static final String MAPURL_EXT = "mapurl";


    private final String label;
    private final String className;

    ELayerTypes(String label, String className) {
        this.label = label;
        this.className = className;
    }

    public static ELayerTypes fromType(String typeString) throws Exception {
        for (ELayerTypes type : values()) {
            if (type.className.equals(typeString)) {
                return type;
            }
        }
        return null;
    }

    public String getType() {
        return className;
    }

    public static ELayerTypes fromFileExt(String fileName) throws Exception {
        fileName = fileName.toLowerCase();
        if (fileName.endsWith(MBTILES_EXT)) {
            return ELayerTypes.MBTILES;
        } else if (fileName.endsWith(MAPSFORGE_EXT)) {
            return ELayerTypes.MAPSFORGE;
        } else if (fileName.endsWith(SPATIALITE_EXT)) {
            return ELayerTypes.SPATIALITE;
        } else if (fileName.endsWith(MAPURL_EXT)) {
            return ELayerTypes.MAPURL;
        }

        return null;
    }

}
