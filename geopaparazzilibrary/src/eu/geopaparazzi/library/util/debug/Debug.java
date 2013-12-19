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

import android.os.Debug.MemoryInfo;

/**
 * Small interface to get hold of all debug possibilities in one place. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class Debug {
    /**
     * Flag to define if we are in debug mode.
     * 
     * <p>For release = <code>false</code>.
     */
    public final static boolean D = false;

    /**
     * Flag to define the use of mock locations instead of the gps.
     * 
     * <p>For release = <code>false</code>.
     */
    public final static boolean doMock = false;

    /**
     * Flag to define normal drawing of renderers, as opposed to drawing some debug info.
     * 
     * <p>For release = <code>true</code>.
     */
    public final static boolean doDrawNormal = true;

    /**
     * Flag to define if the tags file should be overwritten. 
     * 
     * <p>For release = <code>false</code>.
     */
    public final static boolean doOverwriteTags = false;

    /**
     * Flag to define the use of mock locations instead of the gps.
     * 
     * <p>For release = <code>false</code>.
     */
    public final static boolean debugMemory = true;

    /**
     * Dump heap data to a folder.
     * 
     * <p>This will need to be converted to be analized in MAT with:
     * <pre>
     * hprof-conv heap_dump_id_.dalvik-hprof heap_dump_id.hprof
     * </pre>
     * 
     * @param folder the folder to which to dump to.
     * @param id the id to add to the file name.
     * @throws IOException 
     */
    public static void dumpHProfData( File folder, int id ) throws IOException {
        String absPath = new File(folder, "heap_dump_" + id + ".dalvik-hprof").getAbsolutePath();
        android.os.Debug.dumpHprofData(absPath);
    }

    public static void attachUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(new GPUncaughtExceptionHandler());
    }

    /**
     * Get system memory status.
     * 
     * <p>Info taken from: http://huenlil.pixnet.net/blog/post/26872625
     * 
     * @return a description of the current status.
     */
    public static String getMemoryStatus() {
        MemoryInfo memoryInfo = new android.os.Debug.MemoryInfo();
        android.os.Debug.getMemoryInfo(memoryInfo);

        /*
         * The Pss number is a metric the kernel computes that takes into 
         * account memory sharing -- basically each page of RAM in a process 
         * is scaled by a ratio of the number of other processes also using 
         * that page. This way you can (in theory) add up the pss across all 
         * processes to see the total RAM they are using, and compare pss between 
         * processes to get a rough idea of their relative weight.
         */
        double totalPss = memoryInfo.getTotalPss() / 1024.0;
        /*
         * The other interesting metric here is PrivateDirty, which is basically 
         * the amount of RAM inside the process that can not be paged to disk 
         * (it is not backed by the same data on disk), and is not shared with 
         * any other processes. Another way to look at this is the RAM that will 
         * become available to the system when that process goes away (and probably 
         * quickly subsumed into caches and other uses of it).
         */
        double totalPrivateDirty = memoryInfo.getTotalPrivateDirty() / 1024.0;
        double totalSharedDirty = memoryInfo.getTotalSharedDirty() / 1024.0;
        String memMessage = String.format("Memory Pss=%.2f MB\nMemory Private=%.2f MB\nMemory Shared=%.2f MB", totalPss,
                totalPrivateDirty, totalSharedDirty);
        return memMessage;
    }

}
