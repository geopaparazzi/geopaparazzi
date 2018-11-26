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
public class PermissionForegroundService extends AChainedPermissionHelper {

    public static int FOREGROUND_SERVICE_PERMISSION_REQUESTCODE = 6;

    @Override
    public String getDescription() {
        return "Foreground Service";
    }

    @Override
    public boolean hasPermission(Context context) {
        if (canAskPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (context.checkSelfPermission(
                        Manifest.permission.FOREGROUND_SERVICE) !=
                        PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void requestPermission(final Activity activity) {
        if (canAskPermission) {
            if (activity.checkSelfPermission(
                    Manifest.permission.FOREGROUND_SERVICE) !=
                    PackageManager.PERMISSION_GRANTED) {

                if (canAskPermission) {
                    if (activity.shouldShowRequestPermissionRationale(
                            Manifest.permission.FOREGROUND_SERVICE)) {
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(activity);
                        builder.setMessage("The application needs to run as forground service to be able to keep the GPS logging precise when in background mode. To do so it needs the related permission granted.");
                        builder.setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (canAskPermission) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                activity.requestPermissions(new String[]{
                                                                Manifest.permission.FOREGROUND_SERVICE},
                                                        FOREGROUND_SERVICE_PERMISSION_REQUESTCODE);
                                            }
                                        }
                                    }
                                }
                        );
                        // display the dialog
                        builder.create().show();
                    } else {
                        // request permission
                        if (canAskPermission) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                activity.requestPermissions(
                                        new String[]{Manifest.permission.FOREGROUND_SERVICE},
                                        FOREGROUND_SERVICE_PERMISSION_REQUESTCODE);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean hasGainedPermission(int requestCode, int[] grantResults) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (requestCode == FOREGROUND_SERVICE_PERMISSION_REQUESTCODE &&
                    grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                return true;
            return false;
        } else {
            return true;
        }
    }


}
