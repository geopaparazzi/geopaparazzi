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
package eu.geopaparazzi.library.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_CUSTOM_EXTERNALSTORAGE;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_DATABASE_TO_LOAD;

/**
 * Singleton that takes care of resources management.
 * <p/>
 * <p>It creates a folder structure with possible database and log file names.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ResourcesManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String PATH_TEMP = "temp"; //$NON-NLS-1$

    /**
     * The nomedia file defining folders that should not be searched for media.
     */
    public static final String NO_MEDIA = ".nomedia"; //$NON-NLS-1$

    /**
     * The support folder for geopap. If not there, it is created.
     * <p/>
     * <p>It has the name of the application and resides in the sdcard.</p>
     * <p>It contains for example the json tags file and temporary files if necessary.
     */
    private File applicationSupportFolder;

    /**
     * The database file for geopap.
     */
    private File databaseFile;

    /**
     * The temporary folder.
     */
    private File tempDir;

    private static ResourcesManager resourcesManager;

    /**
     * The name of the application.
     */
    private String applicationLabel;

    private static boolean useInternalMemory = true;

    private File mainStorageDir;
    private List<File> otherStorageDirs = new ArrayList<>();
    private final String packageName;

    /**
     * @param useInternalMemory if <code>true</code>, internal memory is used.
     */
    public static void setUseInternalMemory(boolean useInternalMemory) {
        ResourcesManager.useInternalMemory = useInternalMemory;
    }

    /**
     * The getter for the {@link ResourcesManager} singleton.
     * <p>
     * <p>This is a singleton but might require to be recreated
     * in every moment of the application. This is due to the fact
     * that when the application looses focus (for example because of
     * an incoming call, and therefore at a random moment, if the memory
     * is too low, the parent activity could have been killed by
     * the system in background. In which case we need to recreate it.)
     *
     * @param context the context to refer to.
     * @return the {@link ResourcesManager} instance.
     * @throws Exception if something goes wrong.
     */
    public synchronized static ResourcesManager getInstance(Context context) throws Exception {
        if (resourcesManager == null) {
            resourcesManager = new ResourcesManager(context);
        }
        return resourcesManager;
    }

    /**
     * Reset the {@link ResourcesManager}.
     */
    public static void resetManager() {
        resourcesManager = null;
    }

    /**
     * Getter for the app name.
     *
     * @return the name of the app.
     */
    public String getApplicationName() {
        return applicationLabel;
    }

    private ResourcesManager(Context context) throws Exception {
        Context appContext = context.getApplicationContext();
        ApplicationInfo appInfo = appContext.getApplicationInfo();

        packageName = appInfo.packageName;
        int lastDot = packageName.lastIndexOf('.');
        applicationLabel = packageName.replace('.', '_');
        if (lastDot != -1) {
            applicationLabel = packageName.substring(lastDot + 1, packageName.length());
        }
        applicationLabel = applicationLabel.toLowerCase();

        /*
         * take care to create all the folders needed
         *
         * The default structure is:
         *
         * sdcard
         *    |
         *    |-- applicationname.gpap -> main database
         *    |-- applicationSupportfolder
         *    |          |
         *    |          |--- temp/ -> temporary files
         *    |          `--- tags.json
         *    |
         *    `-- mapsdir
         */
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        String cantCreateSdcardmsg = appContext.getResources().getString(R.string.cantcreate_sdcard);

        File[] extDirs = context.getExternalFilesDirs(null);
        File[] extRootDirs = new File[extDirs.length];
        // remove the portion of the path that getExternalFilesDirs returns
        // that we don't want
        for (int i = 0; i < extDirs.length; i++) {
            if (extDirs[i] == null){
                continue;
            } else {
                String regex = "/Android/data/" + getPackageName() + "/files";
                extRootDirs[i] = new File(extDirs[i].toString().replaceAll(regex, ""));
            }
        }

        for (File file : extRootDirs) {
            if (file == null){
                continue;
            } else if (file.exists()) {
                if (file.canWrite() & mainStorageDir == null) {
                    mainStorageDir = file;
                } else if (file.canRead()) {
                    otherStorageDirs.add(file);
                }
            }
        }

        if (mainStorageDir != null) {
            applicationSupportFolder = new File(mainStorageDir, applicationLabel);
        } else if (useInternalMemory) {
                /*
                 * no external storage available:
                 * - use internal memory
                 * - set sdcard for maps inside the space
                 */
            applicationSupportFolder = appContext.getDir(applicationLabel, Context.MODE_PRIVATE);
            mainStorageDir = applicationSupportFolder;
        } else {
            String msgFormat = Utilities.format(cantCreateSdcardmsg, "sdcard/" + applicationLabel);
            throw new IOException(msgFormat);
        }


        // if (GPLog.LOG_HEAVY) {
        Log.i("RESOURCESMANAGER", "Application support folder: " + applicationSupportFolder);
        // }

        String applicationDirPath = applicationSupportFolder.getAbsolutePath();
        if (!applicationSupportFolder.exists()) {
            if (!applicationSupportFolder.mkdirs()) {
                String msgFormat = Utilities.format(cantCreateSdcardmsg, applicationDirPath);
                throw new IOException(msgFormat);
            }
        }
        if (GPLog.LOG_HEAVY) {
            Log.i("RESOURCESMANAGER", "Application support folder exists: " + applicationSupportFolder.exists());
        }

        /*
         * get the database file
         */
        String databasePath = null;
        if (ProfilesHandler.INSTANCE.getActiveProfile() != null) {
            String projectPath = ProfilesHandler.INSTANCE.getActiveProfile().projectPath;
            if (projectPath != null && new File(projectPath).exists()) {
                databasePath = projectPath;
            }
        }
        if (databasePath == null)
            databasePath = preferences.getString(PREFS_KEY_DATABASE_TO_LOAD, "asdasdpoipoi");
        databaseFile = new File(databasePath);
        if (databaseFile.getParentFile() == null || !databaseFile.getParentFile().exists()) {
            // fallback on the default
            String databaseName = applicationLabel + LibraryConstants.GEOPAPARAZZI_DB_EXTENSION;
            databaseFile = new File(mainStorageDir, databaseName);
        }


        tempDir = new File(applicationSupportFolder, PATH_TEMP);
        if (!tempDir.exists())
            if (!tempDir.mkdir()) {
                String msgFormat = Utilities.format(cantCreateSdcardmsg, tempDir.getAbsolutePath());
                GPDialogs.infoDialog(appContext, msgFormat, null);
                tempDir = mainStorageDir;
            }

        Editor editor = preferences.edit();
        editor.putString(LibraryConstants.PREFS_KEY_CUSTOM_EXTERNALSTORAGE, mainStorageDir.getAbsolutePath());
        editor.apply();
    }

    /**
     * Get the name of the package..
     *
     * @return the name of the package.
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Get the file to the main application support folder.
     *
     * @return the {@link File} to the app folder.
     */
    public File getApplicationSupporterDir() {
        return applicationSupportFolder;
    }

    /**
     * Get the main writable storage dir.
     *
     * @return the writable storage dir.
     */
    public File getMainStorageDir() {
        return mainStorageDir;
    }

    /**
     * Get the other available storage folder apart of the main one.
     *
     * @return the list of other storage dirs.
     */
    public List<File> getOtherStorageDirs() {
        return otherStorageDirs;
    }

    /**
     * Get the file to a default database location for the app.
     * <p>
     * <p>This path is generated with default values and can be
     * exploited. It doesn't assure that in the location there really is a db.
     *
     * @return the {@link File} to the database.
     */
    public File getDatabaseFile() {
        return databaseFile;
    }

    /**
     * Get the temporary folder.
     *
     * @return the temp folder.
     */
    public File getTempDir() {
        return tempDir;
    }
}
