package eu.hydrologis.geopaparazzi.utilities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

/**
 * Created by hydrologis on 03/01/16.
 */
public class PermissionFineLocation implements IChainedPermissionHelper {

    public static int FINE_LOCATION_PERMISSION_REQUESTCODE = 2;


    @Override
    public boolean hasPermission(Context context) {
        if (context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }


    @Override
    public void requestPermission(final Activity activity) {
        if (activity.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            if (activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(activity);
                builder.setMessage("Geopaparazzi needs to access the gps on your device to get the current location.");
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.requestPermissions(new String[]{
                                                Manifest.permission.ACCESS_FINE_LOCATION},
                                        FINE_LOCATION_PERMISSION_REQUESTCODE);
                            }
                        }
                );
                // display the dialog
                builder.create().show();
            } else {
                // request permission
                activity.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        FINE_LOCATION_PERMISSION_REQUESTCODE);
            }
        }
    }

    @Override
    public boolean hasGainedPermission(int requestCode, int[] grantResults) {
        if (requestCode == FINE_LOCATION_PERMISSION_REQUESTCODE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }

    @Override
    public IChainedPermissionHelper getNextWithoutPermission(Context context) {
        return null;
    }

}
