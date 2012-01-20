/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
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

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_BASEFOLDER;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import eu.geopaparazzi.library.R;

/**
 * Singleton that takes care of resources management.
 * 
 * <p>It creates a folder structure with possible database and log file names.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ResourcesManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String PATH_MEDIA = "media"; //$NON-NLS-1$

    private static final String PATH_EXPORT = "export"; //$NON-NLS-1$

    private Context context;

    private File applicationDir;
    private File debugLogFile;
    private File databaseFile;

    private File mediaDir;

    private File exportDir;

    private static ResourcesManager resourcesManager;

    /**
     * The getter for the {@link ResourcesManager} singleton.
     * 
     * <p>This is a singletone but might require to be recreated
     * in every moment of the application. This is due to the fact
     * that when the application looses focus (for example because of
     * an incoming call, and therefore at a random moment, if the memory 
     * is too low, the parent activity could have been killed by 
     * the system in background. In which case we need to recreate it.) 
     * 
     * @param context the context to refer to.
     * @return the {@link ResourcesManager} instance.
     */
    public synchronized static ResourcesManager getInstance( Context context ) {
        if (resourcesManager == null) {
            try {
                resourcesManager = new ResourcesManager(context);
            } catch (IOException e) {
                return null;
            }
        }
        return resourcesManager;
    }

    public static void resetManager() {
        resourcesManager = null;
    }

    public Context getContext() {
        return context;
    }

    private ResourcesManager( Context context ) throws IOException {
        this.context = context;
        ApplicationInfo appInfo = context.getApplicationInfo();
        String applicationLabel = context.getPackageManager().getApplicationLabel(appInfo).toString();
        applicationLabel = applicationLabel.toLowerCase();

        String databaseName = applicationLabel + ".db"; //$NON-NLS-1$
        /*
         * take care to create all the folders needed
         * 
         * The default structure is:
         * 
         * applicationname 
         *    | 
         *    |--- applicationname.db 
         *    |--- media  (folder)
         *    |--- export  (folder)
         *    `--- debug.log 
         */
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String baseFolder = preferences.getString(PREFS_KEY_BASEFOLDER, ""); //$NON-NLS-1$
        applicationDir = new File(baseFolder);
        File parentFile = applicationDir.getParentFile();
        boolean parentExists = false;
        boolean parentCanWrite = false;
        if (parentFile != null) {
            parentExists = parentFile.exists();
            parentCanWrite = parentFile.canWrite();
        }
        if (baseFolder.length() == 0 || !parentExists || !parentCanWrite) {
            // the folder doesn't exist for some reason, fallback on default
            String state = Environment.getExternalStorageState();
            boolean mExternalStorageAvailable;
            boolean mExternalStorageWriteable;
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                mExternalStorageAvailable = mExternalStorageWriteable = true;
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                mExternalStorageAvailable = true;
                mExternalStorageWriteable = false;
            } else {
                mExternalStorageAvailable = mExternalStorageWriteable = false;
            }
            if (mExternalStorageAvailable && mExternalStorageWriteable) {
                File sdcardDir = Environment.getExternalStorageDirectory();
                applicationDir = new File(sdcardDir, applicationLabel);
            } else {
                throw new IOException();
            }
        }

        String applicationDirPath = applicationDir.getAbsolutePath();
        if (!applicationDir.exists())
            if (!applicationDir.mkdirs()) {
                String msg = "Wasn\'t able to create folder on sdcard ({0}). Exiting...";
                // String msg = context.getResources().getString(R.string.cantcreate_sdcard);
                String msgFormat = MessageFormat.format(msg, applicationDirPath);
                Utilities.messageDialog(context, msgFormat, null);
            }
        databaseFile = new File(applicationDirPath, databaseName);
        debugLogFile = new File(applicationDirPath, "debug.log"); //$NON-NLS-1$

        mediaDir = new File(applicationDir, PATH_MEDIA);
        if (!mediaDir.exists())
            if (!mediaDir.mkdir())
                Utilities.messageDialog(
                        context,
                        MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard),
                                mediaDir.getAbsolutePath()), null);

        exportDir = new File(applicationDir, PATH_EXPORT);
        if (!exportDir.exists())
            if (!exportDir.mkdir())
                Utilities.messageDialog(
                        context,
                        MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard),
                                exportDir.getAbsolutePath()), null);
    }

    /**
     * Get the file to the main application folder.
     * 
     * @return the {@link File} to the app folder.
     */
    public File getApplicationDir() {
        return applicationDir;
    }

    /**
     * Sets a new application folder. 
     * 
     * <p>Note that this will reset all the folders and resources that are bound 
     * to it. For example there might be the need to recreate the database file.</p>
     * 
     * @param path the path to the new application.
     */
    public void setApplicationDir( String path ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = preferences.edit();
        editor.putString(LibraryConstants.PREFS_KEY_BASEFOLDER, path);
        editor.commit();
        resetManager();
    }

    /**
     * Get the file to a default database location for the app.
     * 
     * <p>This path is generated with default values and can be
     * exploited. It doesn't assure that in the location there really is a db.  
     * 
     * @return the {@link File} to the database.
     */
    public File getDatabaseFile() {
        return databaseFile;
    }

    /**
     * Get the {@link File} to the log file.
     * 
     * @return the {@link File} to the log file. 
     */
    public File getDebugLogFile() {
        return debugLogFile;
    }

    /**
     * Get the default media folder.
     * 
     * @return the default media folder.
     */
    public File getMediaDir() {
        return mediaDir;
    }

    /**
     * Get the default export folder.
     * 
     * @return the default export folder.
     */
    public File getExportDir() {
        return exportDir;
    }

}
