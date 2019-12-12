package eu.geopaparazzi.map.layers;

import eu.geopaparazzi.map.layers.userlayers.BitmapTileServiceLayer;
import eu.geopaparazzi.map.layers.userlayers.GeopackageTableLayer;
import eu.geopaparazzi.map.layers.userlayers.GeopackageTilesLayer;
import eu.geopaparazzi.map.layers.userlayers.MBTilesLayer;
import eu.geopaparazzi.map.layers.userlayers.MapsforgeLayer;
import eu.geopaparazzi.map.layers.userlayers.SpatialiteTableLayer;
import eu.geopaparazzi.map.layers.userlayers.VectorTilesServiceLayer;

@SuppressWarnings("ALL")
public enum ELayerTypes {
    MAPSFORGE("mapsforge", MapsforgeLayer.class.getCanonicalName(), null),
    VECTORTILESSERVICE("VectorTilesService", VectorTilesServiceLayer.class.getCanonicalName(), null),
    BITMAPTILESERVICE("BitmapTileService", BitmapTileServiceLayer.class.getCanonicalName(), null),
    MBTILES("mbtiles", MBTilesLayer.class.getCanonicalName(), null),
    MAPURL("mapurl", BitmapTileServiceLayer.class.getCanonicalName(), null),
    GEOPACKAGE("gpkg", GeopackageTilesLayer.class.getCanonicalName(), GeopackageTableLayer.class.getCanonicalName()),
    SPATIALITE("sqlite", null, SpatialiteTableLayer.class.getCanonicalName());

    public static final int[] OPACITY_LEVELS = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

    public static final String MBTILES_EXT = "mbtiles";
    public static final String MAPSFORGE_EXT = "map";
    public static final String SPATIALITE_EXT = "sqlite";
    public static final String RASTERLITE2_EXT = "rl2";
    public static final String MAPURL_EXT = "mapurl";
    public static final String GEOPACKAGE_EXT = "gpkg";


    private final String label;
    private final String tilesClassName;
    private final String featuresClassName;

    ELayerTypes(String label, String tilesClassName, String featuresClassName) {
        this.label = label;
        this.tilesClassName = tilesClassName;
        this.featuresClassName = featuresClassName;
    }

    public static ELayerTypes fromType(String typeString) throws Exception {
        for (ELayerTypes type : values()) {
            if (type.tilesClassName != null && type.tilesClassName.equals(typeString)) {
                return type;
            }
            if (type.featuresClassName != null && type.featuresClassName.equals(typeString)) {
                return type;
            }
        }
        return null;
    }

    public String getVectorType() {
        return featuresClassName;
    }

    public String getTilesType() {
        return tilesClassName;
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
        } else if (fileName.endsWith(GEOPACKAGE_EXT)) {
            return ELayerTypes.GEOPACKAGE;
        }

        return null;
    }

}
