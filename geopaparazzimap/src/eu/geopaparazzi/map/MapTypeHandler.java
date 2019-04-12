package eu.geopaparazzi.map;

public enum MapTypeHandler {
    MAPSFORGE("map", "mapsforge"),
    MBTILES("mbtiles", "mbtiles");

    private final String extension;
    private final String label;

    MapTypeHandler(String extension, String label) {
        this.extension = extension;
        this.label = label;
    }

    public static MapTypeHandler forLabel(String label) {
        for (MapTypeHandler value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        return null;
    }


    public static MapTypeHandler forExtension(String ext) {
        for (MapTypeHandler value : values()) {
            if (value.extension.equals(ext)) {
                return value;
            }
        }
        return null;
    }


}
