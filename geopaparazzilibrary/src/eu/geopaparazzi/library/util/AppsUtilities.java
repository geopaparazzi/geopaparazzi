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

package eu.geopaparazzi.library.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;

/**
 * An utility to handle 3rd party apps.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AppsUtilities {

    /**
     * Opens the comapss app.
     *
     * @param context the context to use.
     */
    public static void checkAndOpenGpsStatus(final Context context) {
        String gpsStatusAction = "com.eclipsim.gpsstatus.VIEW";
        String gpsStatusPackage = "com.eclipsim.gpsstatus";
        boolean hasGpsStatus = false;
        List<PackageInfo> installedPackages = new ArrayList<PackageInfo>();

        { // try to get the installed packages list. Seems to have troubles over different
            // versions, so trying them all
            try {
                installedPackages = context.getPackageManager().getInstalledPackages(0);
            } catch (Exception e) {
                // ignore
            }
            if (installedPackages.size() == 0)
                try {
                    installedPackages = context.getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES);
                } catch (Exception e) {
                    // ignore
                }
        }

        if (installedPackages.size() > 0) {
            // if a list is available, check if the status gps is installed
            for (PackageInfo packageInfo : installedPackages) {
                String packageName = packageInfo.packageName;
                if (packageName.startsWith(gpsStatusPackage)) {
                    hasGpsStatus = true;
                    break;
                }
            }
        } else {
            /*
             * if no package list is available, for now try to fire it up anyways.
             * This has been a problem for a user on droidx with android 2.2.1.
             */
            hasGpsStatus = true;
        }

        if (hasGpsStatus) {
            Intent intent = new Intent(gpsStatusAction);
            context.startActivity(intent);
        } else {
            new AlertDialog.Builder(context).setTitle(context.getString(R.string.installgpsstatus_title))
                    .setMessage(context.getString(R.string.installgpsstatus_message)).setIcon(android.R.drawable.ic_dialog_info)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // ignore
                        }
                    }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://search?q=com.eclipsim.gpsstatus2"));
                    context.startActivity(intent);
                }
            }).show();
        }
    }

}
