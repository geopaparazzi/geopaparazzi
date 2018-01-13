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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.activities.DirectoryBrowserActivity;
import eu.geopaparazzi.library.images.ImageUtilities;

/**
 * An utility to handle 3rd party apps.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Cesar Martinez Izquierdo (www.scolab.es)
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

        final String gpsStatusPackage = "com.android.gpstest";
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
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(gpsStatusPackage);
            if (launchIntent != null) {
                context.startActivity(launchIntent);//null pointer check in case package name was not found
            }
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
                    intent.setData(Uri.parse("market://details?id=" + gpsStatusPackage));
                    context.startActivity(intent);
                }
            }).show();
        }
    }

    /**
     * Start a file picking activity.
     *
     * @param activityStarter
     * @param requestCode
     * @param title
     * @param filterExtensions
     * @param startPath
     * @throws Exception
     */
    public static void pickFile(IActivitySupporter activityStarter, int requestCode, String title, String[] filterExtensions, String startPath) throws Exception {
        if (startPath == null) {
            startPath = Utilities.getLastFilePath(activityStarter.getContext());
        }

        Intent browseIntent = new Intent(activityStarter.getContext(), DirectoryBrowserActivity.class);
        browseIntent.putExtra(DirectoryBrowserActivity.EXTENSIONS, filterExtensions);
        browseIntent.putExtra(DirectoryBrowserActivity.STARTFOLDERPATH, startPath);
        activityStarter.startActivityForResult(browseIntent, requestCode);
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
    public static void pickFileByExternalBrowser(IActivitySupporter activityStarter, int requestCode, String title, String mimeType, Uri uri) {
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

    public static void pickFolder(IActivitySupporter activityStarter, int requestCode, String title, String startPath, String[] visibleExtensions) throws Exception {
        if (startPath == null) {
            startPath = Utilities.getLastFilePath(activityStarter.getContext());
        }

        Intent browseIntent = new Intent(activityStarter.getContext(), DirectoryBrowserActivity.class);
        browseIntent.putExtra(DirectoryBrowserActivity.DOFOLDER, true);
        browseIntent.putExtra(DirectoryBrowserActivity.EXTENSIONS, visibleExtensions);
        browseIntent.putExtra(DirectoryBrowserActivity.STARTFOLDERPATH, startPath);
        activityStarter.startActivityForResult(browseIntent, requestCode);
    }


    public static void checkAmazeExplorer(final IActivitySupporter activityStarter) {
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

    /**
     * Show an image through intent.
     *
     * @param imageData the image data.
     * @param imageName the image name.
     * @param context   the context to use.
     * @throws Exception
     */
    public static void showImage(byte[] imageData, String imageName, Context context) throws Exception {
        File tempDir = ResourcesManager.getInstance(context).getTempDir();
        String ext = ".jpg";
        if (imageName.endsWith(".png")) {
            ext = ".png";
        }
        File imageFile = new File(tempDir, ImageUtilities.getTempImageName(ext));
        ImageUtilities.writeImageDataToFile(imageData, imageFile.getAbsolutePath());

        showImage(imageFile, context);
    }

    /**
     * Show and image.
     *
     * @param imageFile the image file.
     * @param context   the context to use.
     * @throws Exception
     */
    public static void showImage(File imageFile, Context context) throws Exception {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Utilities.getFileUriInApplicationFolder(context, imageFile);
        intent.setDataAndType(uri, "image/*"); //$NON-NLS-1$

        grantPermission(context, intent, uri);
        context.startActivity(intent);
    }


    /**
     * Grant permission to access a file from another app.
     * <p>
     * <p>This is necessary since Android 7.</p>
     *
     * @param context the context.
     * @param intent  the intent.
     * @param uri     the file uri.
     */
    public static void grantPermission(Context context, Intent intent, Uri uri) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    /**
     * Opens the comapss app.
     *
     * @param context the context to use.
     */
    public static void checkAndOpenGpsStatusNonFoss(final Context context) {
        String GPS_STATUS_PACKAGE_NAME = "com.eclipsim.gpsstatus2";
        String GPS_STATUS_CLASS_NAME = "com.eclipsim.gpsstatus2.GPSStatus";
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
                if (packageName.startsWith(GPS_STATUS_PACKAGE_NAME)) {
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
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName(GPS_STATUS_PACKAGE_NAME, GPS_STATUS_CLASS_NAME));

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

    public static void openGoogleMapsBetween2Coordinates(final Context context, double x1, double y1, double x2, double y2) {
        String uriString = "https://maps.google.com/maps?saddr=" + y1 + "," + x1 + "&daddr=" + y2 + "," + x2;

//        String uriString = "https://www.google.com/maps/dir/?api=1&origin=" + x1 + "," + y1 + "&destination=" + x2 + "," + y2 + "&travelmode=driving";
        Uri gmmIntentUri = Uri.parse(uriString);
        Intent intent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        intent.setPackage("com.google.android.apps.maps");
        context.startActivity(intent);
    }


}
