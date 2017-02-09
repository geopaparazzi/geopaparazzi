package eu.geopaparazzi.library.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ActivityStarter implements IActivityStarter {
    private Intent intent = new Intent();

    public void start(Context context, String action) {
        intent.setAction(action);
        List<ResolveInfo> info = context.getPackageManager().queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }
}
