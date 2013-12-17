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
package eu.geopaparazzi.library.util.debug;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import eu.geopaparazzi.library.GeopaparazziLibraryContextHolder;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;

import android.content.Context;
import android.util.Log;

/**
 * Trap oom to dump hprof
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GPUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    public void uncaughtException( Thread thread, Throwable ex ) {
        Log.e("GPUNCAUGHTEXCEPTIONHANDLER", ex.getLocalizedMessage());
        if (ex.getClass().equals(OutOfMemoryError.class)) {
            try {
                String date = LibraryConstants.TIMESTAMPFORMATTER.format(new Date());
                String sdcardPath = "/sdcard";
                try {
                    Context context = GeopaparazziLibraryContextHolder.INSTANCE.getContext();
                    File sdcardDir = ResourcesManager.getInstance(context).getSdcardDir();
                    sdcardPath = sdcardDir.getAbsolutePath();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String dumpPath = sdcardPath + "/dump" + date + ".hprof";
                android.os.Debug.dumpHprofData(dumpPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ex.printStackTrace();
    }
}