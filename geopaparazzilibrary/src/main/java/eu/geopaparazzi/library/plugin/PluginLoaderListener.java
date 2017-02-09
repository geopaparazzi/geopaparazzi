package eu.geopaparazzi.library.plugin;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public interface PluginLoaderListener<T extends PluginLoader> {
    /**
     * This method is called when the plugin has finished loading
     *
     * @param loader
     */
    public void pluginLoaded(T loader);
}
