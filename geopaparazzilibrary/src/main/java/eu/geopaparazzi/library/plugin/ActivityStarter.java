package eu.geopaparazzi.library.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;

import eu.geopaparazzi.library.util.IActivityStarter;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ActivityStarter implements IActivityStarter {
    private Intent intent = new Intent();

    @Override
    public void startActivity(Intent intent) {
        // unused
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        // unused
    }

    @Override
    public Context getContext() {
        // unused
        return null;
    }

    public void start(Context context, String action) {
        intent.setAction(action);
        List<ResolveInfo> info = context.getPackageManager().queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }
}
