/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.geopaparazzi.library.permissions;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PermissionWriteStorage implements IChainedPermissionHelper {

    public static int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUESTCODE = 1;

    @Override
    public String getDescription() {
        return "Write External Storage";
    }

    @Override
    public boolean hasPermission(Context context) {
        if (canAskPermission) {
            if (context.checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void requestPermission(final Activity activity) {
        if (canAskPermission) {
            if (activity.checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {

                if (canAskPermission) {
                    if (activity.shouldShowRequestPermissionRationale(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(activity);
                        builder.setMessage("Geopaparazzi needs to write data to your device. To do so it needs the related permission granted.");
                        builder.setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (canAskPermission) {
                                            activity.requestPermissions(new String[]{
                                                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                    WRITE_EXTERNAL_STORAGE_PERMISSION_REQUESTCODE);
                                        }
                                    }
                                }
                        );
                        // display the dialog
                        builder.create().show();
                    } else {
                        // request permission
                        if (canAskPermission) {
                            activity.requestPermissions(
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    WRITE_EXTERNAL_STORAGE_PERMISSION_REQUESTCODE);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean hasGainedPermission(int requestCode, int[] grantResults) {
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_REQUESTCODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
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
