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
public class PermissionFineLocation extends AChainedPermissionHelper {

    public static int FINE_LOCATION_PERMISSION_REQUESTCODE = 2;

    @Override
    public String getDescription() {
        return "Fine Location";
    }

    @Override
    public boolean hasPermission(Context context) {
        if (canAskPermission) {
            if (context.checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
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
    }

    @Override
    public boolean hasGainedPermission(int requestCode, int[] grantResults) {
        if (requestCode == FINE_LOCATION_PERMISSION_REQUESTCODE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }


}
