package eu.geopaparazzi.library.plugin;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;


/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public abstract class PluginService extends IntentService {

    /**
     * @param name Service name
     */
    public PluginService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public abstract IBinder onBind (Intent intent);
}
