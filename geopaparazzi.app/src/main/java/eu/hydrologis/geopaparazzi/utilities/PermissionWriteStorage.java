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
public class PermissionWriteStorage implements IChainedPermissionHelper {

    public static int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUESTCODE = 1;


    @Override
    public boolean hasPermission(Context context) {
        if (context.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    @Override
    public void requestPermission(final Activity activity) {
        if (activity.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {

            if (activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(activity);
                builder.setMessage("Geopaparazzi needs to write data to your device. To do so it needs the related permission granted.");
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.requestPermissions(new String[]{
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        WRITE_EXTERNAL_STORAGE_PERMISSION_REQUESTCODE);
                            }
                        }
                );
                // display the dialog
                builder.create().show();
            } else {
                // request permission
                activity.requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE_PERMISSION_REQUESTCODE);
            }
        }
    }

    @Override
    public boolean hasGainedPermission(int requestCode, int[] grantResults) {
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_REQUESTCODE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }

    @Override
    public IChainedPermissionHelper getNextWithoutPermission(Context context) {
        PermissionFineLocation permissionFineLocation = new PermissionFineLocation();
        if (!permissionFineLocation.hasPermission(context)) {
            return permissionFineLocation;
        } else {
            return permissionFineLocation.getNextWithoutPermission(context);
        }
    }

}
