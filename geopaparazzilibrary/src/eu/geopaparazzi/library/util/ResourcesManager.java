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
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_CUSTOM_EXTERNALSTORAGE;
import static eu.geopaparazzi.library.util.Utilities.messageDialog;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;

/**
 * Singleton that takes care of resources management.
 * 
 * <p>It creates a folder structure with possible database and log file names.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ResourcesManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String PATH_MAPS = "maps"; //$NON-NLS-1$

    private static final String PATH_MEDIA = "media"; //$NON-NLS-1$

    /**
     * The nomedia file defining folders that should not be searched for media.
     */
    public static final String NO_MEDIA = ".nomedia"; //$NON-NLS-1$

    private File applicationDir;

    private File databaseFile;

    private File mediaDir;
    private File mapsDir;

    private File exportDir;

    private static ResourcesManager resourcesManager;

    private String applicationLabel;

    private static boolean useInternalMemory = true;

    private File sdcardDir;

    private boolean createdApplicationDirOnInit = false;

    /**
     * @param useInternalMemory if <code>true</code>, internal memory is used.
     */
    public static void setUseInternalMemory( boolean useInternalMemory ) {
        ResourcesManager.useInternalMemory = useInternalMemory;
    }

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
     * @throws Exception  if something goes wrong.
     */
    public synchronized static ResourcesManager getInstance( Context context ) throws Exception {
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

    private ResourcesManager( Context context ) throws Exception {
        Context appContext = context.getApplicationContext();
        ApplicationInfo appInfo = appContext.getApplicationInfo();

        String packageName = appInfo.packageName;
        int lastDot = packageName.lastIndexOf('.');
        applicationLabel = packageName.replace('.', '_');
        if (lastDot != -1) {
            applicationLabel = packageName.substring(lastDot + 1, packageName.length());
        }
        applicationLabel = applicationLabel.toLowerCase();
        String databaseName = applicationLabel + ".db"; //$NON-NLS-1$
        /*
         * take care to create all the folders needed
         * 
         * The default structure is:
         * 
         * sdcard
         *    |
         *    |-- applicationname 
         *    |          | 
         *    |          |--- applicationname.db 
         *    |          |--- media  (folder)
         *    |          |--- export  (folder)
         *    |          `--- debug.log 
         *    `-- mapsdir
         */
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        String baseFolder = preferences.getString(PREFS_KEY_BASEFOLDER, ""); //$NON-NLS-1$
        applicationDir = new File(baseFolder);
        File parentFile = applicationDir.getParentFile();
        boolean parentExists = false;
        boolean parentCanWrite = false;
        if (parentFile != null) {
            parentExists = parentFile.exists();
            parentCanWrite = parentFile.canWrite();
        }
        // the folder doesn't exist for some reason, fallback on default
        String state = Environment.getExternalStorageState();
        if (GPLog.LOG_HEAVY) {
            Log.i("RESOURCESMANAGER", state);
        }
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

        String cantCreateSdcardmsg = appContext.getResources().getString(R.string.cantcreate_sdcard);
        File possibleApplicationDir;
        if (mExternalStorageAvailable && mExternalStorageWriteable) {
            // and external storage exists and is usable
            String customFolderPath = preferences.getString(PREFS_KEY_CUSTOM_EXTERNALSTORAGE, "asdasdpoipoi");
            customFolderPath = customFolderPath.trim();
            File customFolderFile = new File(customFolderPath);
            if (customFolderFile.exists() && customFolderFile.isDirectory() && customFolderFile.canWrite()) {
                /*
                 * the user wants a different storage path:
                 * - use that as sdcard
                 * - create an app folder inside it
                 */
                sdcardDir = customFolderFile;
                possibleApplicationDir = new File(sdcardDir, applicationLabel);
            } else {
                if (customFolderPath.equals("internal")) {
                    /*
                     * the user folder doesn't exist, but is "internal":
                     * - use internal app memory
                     * - set sdcard anyways to the external folder for maps use
                     */
                    useInternalMemory = true;
                    possibleApplicationDir = appContext.getDir(applicationLabel, Context.MODE_PRIVATE);
                    sdcardDir = Environment.getExternalStorageDirectory();
                } else {
                    sdcardDir = Environment.getExternalStorageDirectory();
                    possibleApplicationDir = new File(sdcardDir, applicationLabel);
                }
            }
        } else if (useInternalMemory) {
            /*
             * no external storage available:
             * - use internal memory
             * - set sdcard for maps inside the space
             */
            possibleApplicationDir = appContext.getDir(applicationLabel, Context.MODE_PRIVATE);
            sdcardDir = possibleApplicationDir;
        } else {
            String msgFormat = Utilities.format(cantCreateSdcardmsg, "sdcard/" + applicationLabel);
            throw new IOException(msgFormat);
        }

        if (baseFolder.length() == 0 || !parentExists || !parentCanWrite) {
            applicationDir = possibleApplicationDir;
        }

        // if (GPLog.LOG_HEAVY) {
        Log.i("RESOURCESMANAGER", "Possible app dir: " + applicationDir);
        // }

        String applicationDirPath = applicationDir.getAbsolutePath();
        if (!applicationDir.exists()) {
            createdApplicationDirOnInit = true;

            // RandomAccessFile file = null;
            // try {
            // file = new RandomAccessFile(applicationDir, "rw");
            // final FileLock fileLock = file.getChannel().tryLock();
            // Log.i("RESOURCESMANAGER", "Got the lock? " + (null != fileLock));
            // if (null != fileLock) {
            // Log.i("RESOURCESMANAGER", "Is a valid lock? " + fileLock.isValid());
            // }
            // } finally {
            // file.close();
            // }

            // Process proc = Runtime.getRuntime().exec(new String[]{"lsof",
            // applicationDir.getAbsolutePath()});
            // StringBuilder sb = new StringBuilder("LOSF RESULT: ");
            // BufferedReader stdInput = new BufferedReader(new
            // InputStreamReader(proc.getInputStream()));
            // BufferedReader stdError = new BufferedReader(new
            // InputStreamReader(proc.getErrorStream()));
            // String s;
            // while( (s = stdInput.readLine()) != null ) {
            // sb.append(s).append("\n");
            // }
            // while( (s = stdError.readLine()) != null ) {
            // sb.append(s).append("\n");
            // }
            // Log.i("RESOURCESMANAGER", sb.toString());
            if (!applicationDir.mkdirs()) {
                String msgFormat = Utilities.format(cantCreateSdcardmsg, applicationDirPath);
                throw new IOException(msgFormat);
            }
        }
        if (GPLog.LOG_HEAVY) {
            Log.i("RESOURCESMANAGER", "App dir exists: " + applicationDir.exists());
        }
        databaseFile = new File(applicationDirPath, databaseName);

        mediaDir = new File(applicationDir, PATH_MEDIA);
        if (!mediaDir.exists())
            if (!mediaDir.mkdir()) {
                String msgFormat = Utilities.format(cantCreateSdcardmsg, mediaDir.getAbsolutePath());
                throw new IOException(msgFormat);
            }

        exportDir = applicationDir.getParentFile();

        mapsDir = new File(sdcardDir, PATH_MAPS);
        if (!mapsDir.exists())
            if (!mapsDir.mkdir()) {
                String msgFormat = Utilities.format(cantCreateSdcardmsg, mapsDir.getAbsolutePath());
                messageDialog(appContext, msgFormat, null);
                mapsDir = sdcardDir;
            }
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
     * Get info about the application folder's pre-existence.
     * 
     * @return <code>true</code> if on initialisation an 
     *      application folder had to be created, <code>false</code>
     *      if the application folder already existed.
     */
    public boolean hadToCreateApplicationDirOnInit() {
        return createdApplicationDirOnInit;
    }

    /**
     * Get the file to the main application's parent folder.
     * 
     * @return the {@link File} to the app's parent folder.
     */
    public File getApplicationParentDir() {
        return applicationDir.getParentFile();
    }

    /**
     * Get the sdcard dir or <code>null</code>.
     * 
     * @return the sdcard folder file.
     */
    public File getSdcardDir() {
        return sdcardDir;
    }

    /**
     * Sets a new application folder. 
     * 
     * <p>Note that this will reset all the folders and resources that are bound 
     * to it. For example there might be the need to recreate the database file.</p>
     * 
     * @param context the context to use.
     * @param path the path to the new application.
     */
    public void setApplicationDir( Context context, String path ) {
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

    /**
     * Get the default maps folder.
     * 
     * @return the default maps folder.
     */
    public File getMapsDir() {
        return mapsDir;
    }

    /**
     * Update the description file of the project.
     * 
     * @param description a new description for the project.
     * @throws IOException  if something goes wrong.
     */
    public void addProjectDescription( String description ) throws IOException {
        File applicationDir = getApplicationDir();
        File descriptionFile = new File(applicationDir, "description"); //$NON-NLS-1$
        FileUtilities.writefile(description, descriptionFile);
    }

}
