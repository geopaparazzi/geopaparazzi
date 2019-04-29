package eu.geopaparazzi.map.layers;

import eu.geopaparazzi.map.layers.userlayers.BitmapTileServiceLayer;
import eu.geopaparazzi.map.layers.userlayers.MBTilesLayer;
import eu.geopaparazzi.map.layers.userlayers.MapsforgeLayer;
import eu.geopaparazzi.map.layers.userlayers.VectorTilesServiceLayer;

public enum ELayerTypes {
    MAPSFORGE("mapsforge", MapsforgeLayer.class.getCanonicalName()),
    VECTORTILESSERVICE("VectorTilesService", VectorTilesServiceLayer.class.getCanonicalName()),
    BITMAPTILESERVICE("BitmapTileService", BitmapTileServiceLayer.class.getCanonicalName()),
    MBTILES("mbtiles", MBTilesLayer.class.getCanonicalName());

    public static int[] OPACITY_LEVELS = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};


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
        throw new IllegalArgumentException("No type for class: " + typeString);
    }

}
