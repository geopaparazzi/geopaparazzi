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
package eu.geopaparazzi.mapsforge.mapsdirmanager.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import eu.geopaparazzi.library.util.FileUtilities;

/**
 * The default mapurls available
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DefaultMapurls {
    /**
     * The extension for mapurl files.
     */
    public static final String MAPURL_EXTENSION = ".mapurl"; //$NON-NLS-1$

    /**
     * The different available mapurls.
     */
    public enum Mapurls {
        /** */
        mapnik(eu.geopaparazzi.mapsforge.R.raw.mapnik),
        /** */
        opencycle(eu.geopaparazzi.mapsforge.R.raw.opencycle),
        /** */
        mapquest(eu.geopaparazzi.mapsforge.R.raw.mapquest),
        /** */
        mapquest_arial(eu.geopaparazzi.mapsforge.R.raw.mapquest_arial),
        /** */
        realvista_arial(eu.geopaparazzi.mapsforge.R.raw.realvista_ortofoto_italy);

        private int resourceId;

        private Mapurls( int resourceId ) {
            this.resourceId = resourceId;
        }

        /**
         * @return the resource id to retrieve the raw file.
         */
        public int getResourceId() {
            return resourceId;
        }
    }

    /**
     * Checks if a source definition file exists. If not it is created.
     * 
     * @param context the context to use.
     * @param mapsDir the maps folder file.
     * @param type the mapurl type.
     * @return the checked file.
     * @throws Exception if something goes wrong.
     */
    public static File checkSourceExistence( Context context, File mapsDir, Mapurls type ) throws Exception {
        File mapurlFile = new File(mapsDir, type.toString() + MAPURL_EXTENSION);
        if (!mapurlFile.exists()) {
            InputStream inputStream = context.getResources().openRawResource(type.getResourceId());
            OutputStream outputStream = new FileOutputStream(mapurlFile);
            FileUtilities.copyFile(inputStream, outputStream);
        }
        return mapurlFile;
    }

    /**
     * Checks if the default mapurl source definition files exist. If not they are created.
     * 
     * @param context the context to use.
     * @param mapsDir the maps folder file.
     * @throws Exception if something goes wrong.
     */
    public static void checkAllSourcesExistence( Context context, File mapsDir ) throws Exception {
        for( Mapurls mapurl : Mapurls.values() ) {
            File mapurlFile = new File(mapsDir, mapurl.toString() + MAPURL_EXTENSION);
            if (!mapurlFile.exists()) {
                InputStream inputStream = context.getResources().openRawResource(mapurl.getResourceId());
                OutputStream outputStream = new FileOutputStream(mapurlFile);
                FileUtilities.copyFile(inputStream, outputStream);
            }
        }
    }
}
