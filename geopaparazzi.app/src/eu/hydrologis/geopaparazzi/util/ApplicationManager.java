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
package eu.hydrologis.geopaparazzi.util;

import static eu.hydrologis.geopaparazzi.util.Constants.DECIMATION_FACTOR;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_KMLEXPORT;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_MEDIA;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;

/**
 * Singleton that takes care of all the sensors and gps and loggings.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ApplicationManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private Context context;

    private File databaseFile;
    private File geoPaparazziDir;
    private File mediaDir;
    private File kmlExportDir;

    private File debugLogFile;

    private ResourcesManager resourcesManager;
    private static ApplicationManager applicationManager;

    /**
     * The getter for the {@link ApplicationManager} singleton.
     * 
     * <p>This is a singletone but might require to be recreated
     * in every moment of the application. This is due to the fact
     * that when the application looses focus (for example because of
     * an incoming call, and therefore at a random moment, if the memory 
     * is too low, the parent activity could have been killed by 
     * the system in background. In which case we need to recreate it.) 
     * 
     * @param context the context to refer to.
     * @return
     */
    public static ApplicationManager getInstance( Context context ) {
        if (applicationManager == null) {
            try {
                applicationManager = new ApplicationManager(context);
            } catch (IOException e) {
                return null;
            }
        }
        return applicationManager;
    }

    public static void resetManager() {
        applicationManager = null;
    }

    public Context getContext() {
        return context;
    }

    private ApplicationManager( Context context ) throws IOException {
        this.context = context;

        /*
         * take care to create all the folders needed
         * 
         * The default structure is:
         * 
         * geopaparazzi 
         *    | 
         *    |--- media 
         *    |       |-- IMG_***.jpg 
         *    |       |-- AUDIO_***.3gp 
         *    |       `-- etc 
         *    |--- geopaparazzi.db 
         *    |--- tags.json 
         *    |        
         *    |--- debug.log 
         *    |        
         *    `--- export
         */
        resourcesManager = ResourcesManager.getInstance(context);
        geoPaparazziDir = resourcesManager.getApplicationDir();
        databaseFile = resourcesManager.getDatabaseFile();
        mediaDir = new File(geoPaparazziDir, PATH_MEDIA);
        if (!mediaDir.exists())
            if (!mediaDir.mkdir())
                Utilities.messageDialog(
                        context,
                        MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard),
                                mediaDir.getAbsolutePath()), null);
        debugLogFile = resourcesManager.getDebugLogFile();

        kmlExportDir = new File(geoPaparazziDir, PATH_KMLEXPORT);
        if (!kmlExportDir.exists())
            if (!kmlExportDir.mkdir())
                Utilities.messageDialog(
                        context,
                        MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard),
                                kmlExportDir.getAbsolutePath()), null);

    }

    public File getGeoPaparazziDir() {
        return geoPaparazziDir;
    }

    public File getDatabaseFile() {
        return databaseFile;
    }

    public File getKmlExportDir() {
        return kmlExportDir;
    }

    public File getDebugLogFile() {
        return debugLogFile;
    }

    public File getMediaDir() {
        return mediaDir;
    }

    public int getDecimationFactor() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String decimationFactorStr = preferences.getString(DECIMATION_FACTOR, "5"); //$NON-NLS-1$
        int decimationFactor = 5;
        try {
            decimationFactor = Integer.parseInt(decimationFactorStr);
        } catch (Exception e) {
            // use default
        }
        return decimationFactor;
    }

}
