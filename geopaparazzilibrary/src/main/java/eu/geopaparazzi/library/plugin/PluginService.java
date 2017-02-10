package eu.geopaparazzi.library.plugin;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;


/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public abstract class PluginService extends IntentService {
     protected String action;

    /**
     * @param name Service name
     * @param serviceAction The name of the action that will be used as extension point (for
     *                      instance, "eu.geopaparazzi.core.extension.importer.spatialite.PICK"
     *                      for the extension point used to register importer menu entries)
     */
    public PluginService(String name, String serviceAction) {
        super(name);
        this.action = serviceAction;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public abstract IBinder onBind (Intent intent);
}
