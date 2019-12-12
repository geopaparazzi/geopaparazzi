package eu.geopaparazzi.map.gui;

/**
 * MapLayerItem class to hold info for list adapters in view.
 */
public class MapLayerItem {
    /**
     * The current layer position.
     */
    public int position;
    /**
     * Main item name.
     */
    public String name;
    /**
     * A url, if the resource is online else null.
     */
    public String url;
    /**
     * The path to the resource if it is a file, the tilePath if it is an online resource.
     */
    public String path;
    /**
     * If true, the item is visible and active.
     */
    public boolean enabled;
    /**
     * If true, the resource is a system resource.
     */
    public boolean isSystem;
    /**
     * If true, the resource is in editing mode.
     */
    public boolean isEditing;
    /**
     * The layer's class
     */
    public String type;
}
