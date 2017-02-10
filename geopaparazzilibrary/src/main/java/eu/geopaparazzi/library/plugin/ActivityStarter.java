package eu.geopaparazzi.library.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import eu.geopaparazzi.library.util.IActivitySupporter;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ActivityStarter implements IActivitySupporter {
    private Intent intent = new Intent();
    private Context context;

    public ActivityStarter(Context context) {
        this.context = context;
    }


    @Override
    public void startActivity(Intent intent) {
        context.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public FragmentManager getSupportFragmentManager() {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.getSupportFragmentManager();
        }
        return null;
    }

//    public void start(Context context, String action) {
//        intent.setAction(action);
//        List<ResolveInfo> info = context.getPackageManager().queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
//        if (intent.resolveActivity(context.getPackageManager()) != null) {
//            context.startActivity(intent);
//        }
//    }
}
