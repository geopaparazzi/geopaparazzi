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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.R;

/**
 * An utility to handle 3rd party apps.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AppsUtilities {
    public static final String AMAZE_PACKAGE = "com.amaze.filemanager";
    public static final String AMAZE_TITLE = "com.amaze.filemanager.extra.TITLE";

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

    /**
     * Start activity to pick a file.
     * <p>
     * <p>
     * For mimetype and uri one could use:
     * <pre>
     *        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("gpx");
     *        if (mimeType == null) mimeType = "text/gpx";
     *        Uri uri = Uri.parse(ResourcesManager.getInstance(this).getMapsDir().getAbsolutePath());
     *     </pre>
     * </p>
     *
     * @param activityStarter the activity that will recieve the picked file.
     * @param requestCode     the requestcode to use on result.
     * @param title           optional title.
     * @param mimeType        the mimetype.
     * @param uri             the uri of the start folder.
     */
    public static void pickFile(IActivityStarter activityStarter, int requestCode, String title, String mimeType, Uri uri) {
        // first try with amaze
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setPackage(AppsUtilities.AMAZE_PACKAGE);
        if (title != null)
            intent.putExtra(AppsUtilities.AMAZE_TITLE, title);
        intent.setDataAndType(uri, mimeType);
        try {
            activityStarter.startActivityForResult(intent, requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            // try with es explorer
            intent = new Intent("com.estrongs.action.PICK_FILE ");
            if (title != null)
                intent.putExtra("com.estrongs.intent.extra.TITLE", title);
            intent.setDataAndType(uri, mimeType);
            try {
                activityStarter.startActivityForResult(intent, requestCode);
            } catch (android.content.ActivityNotFoundException ex2) {
                // try with generic
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setDataAndType(uri, mimeType);
                Intent chooser = Intent.createChooser(intent, title);
                try {
                    activityStarter.startActivityForResult(chooser, requestCode);
                } catch (android.content.ActivityNotFoundException ex3) {
                    // direct to install a file manager
                    AppsUtilities.checkAmazeExplorer(activityStarter);
                }
            }
        }
    }


    public static void checkAmazeExplorer(final IActivityStarter activityStarter) {
        Context context = activityStarter.getContext();
        boolean hasPackage = hasPackage(context, AMAZE_PACKAGE);

        if (!hasPackage) {
            new AlertDialog.Builder(context).setTitle(context.getString(R.string.installamaze_title))
                    .setMessage(context.getString(R.string.installamaze_message)).setIcon(android.R.drawable.ic_dialog_info)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // ignore
                        }
                    }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://search?q=" + AMAZE_PACKAGE));
                    activityStarter.startActivity(intent);
                }
            }).show();
        }
    }

    private static boolean hasPackage(Context context, String searchedPackageName) {
        boolean hasPackage = false;
        List<PackageInfo> installedPackages = new ArrayList<PackageInfo>();
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

        if (installedPackages.size() > 0) {
            // if a list is available, check if the status gps is installed
            for (PackageInfo packageInfo : installedPackages) {
                String packageName = packageInfo.packageName;
                if (packageName.startsWith(searchedPackageName)) {
                    hasPackage = true;
                    break;
                }
            }
        } else {
            /*
             * if no package list is available, for now try to fire it up anyways.
             * This has been a problem for a user on droidx with android 2.2.1.
             */
            hasPackage = true;
        }
        return hasPackage;
    }

}
