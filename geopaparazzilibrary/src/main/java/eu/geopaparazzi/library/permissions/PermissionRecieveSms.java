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

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PermissionRecieveSms extends AChainedPermissionHelper {

    public static int RECIEVESMS_PERMISSION_REQUESTCODE = 4;

    private AChainedPermissionHelper nextPermissionHelper;

    @Override
    public String getDescription() {
        return "Recieve SMS";
    }

    @Override
    public boolean hasPermission(Context context) {
        if (canAskPermission) {
            if (context.checkSelfPermission(
                    Manifest.permission.RECEIVE_SMS) !=
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
                    Manifest.permission.RECEIVE_SMS) !=
                    PackageManager.PERMISSION_GRANTED) {

                if (canAskPermission) {
                    if (activity.shouldShowRequestPermissionRationale(
                            Manifest.permission.RECEIVE_SMS)) {
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(activity);
                        builder.setMessage("Geopaparazzi needs to be able to recieve sms to enable certain functionalities.");
                        builder.setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (canAskPermission) {
                                            activity.requestPermissions(new String[]{
                                                            Manifest.permission.RECEIVE_SMS},
                                                    RECIEVESMS_PERMISSION_REQUESTCODE);
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
                                    new String[]{Manifest.permission.RECEIVE_SMS},
                                    RECIEVESMS_PERMISSION_REQUESTCODE);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean hasGainedPermission(int requestCode, int[] grantResults) {
        if (requestCode == RECIEVESMS_PERMISSION_REQUESTCODE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }


}
