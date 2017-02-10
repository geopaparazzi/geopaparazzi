package eu.geopaparazzi.library.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public abstract class PluginServiceConnection implements android.content.ServiceConnection {
    private boolean bound = false;

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        bound = false;
    }

    public boolean isBound() {
        return bound;
    }

    public void disconnect(Context context) {
        if (bound) {
            bound = false;
            context.unbindService(this);
        }
    }

}
