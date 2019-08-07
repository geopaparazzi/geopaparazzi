package eu.geopaparazzi.map.layers.utils;

@SuppressWarnings("ALL")
public enum LayerGroups {
    SYSTEM(0, "System layers"),
    USER(1, "User layers");

    private int groupCode;
    private String label;

    LayerGroups(int groupCode, String label) {
        this.groupCode = groupCode;
        this.label = label;
    }

    public int getGroupCode() {
        return groupCode;
    }

    public String getLabel() {
        return label;
    }
}
