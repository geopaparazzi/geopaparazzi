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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import eu.geopaparazzi.library.database.GPLog;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FileUtilities {
    public static void copyFile( String fromFile, String toFile ) throws IOException {
        File in = new File(fromFile);
        File out = new File(toFile);
        copyFile(in, out);
    }

    public static void copyFile( File in, File out ) throws IOException {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        copyFile(fis, fos);
    }

    public static void copyFile( InputStream fis, OutputStream fos ) throws IOException {
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while( (i = fis.read(buf)) != -1 ) {
                fos.write(buf, 0, i);
            }
        } finally {
            if (fis != null)
                fis.close();
            if (fos != null)
                fos.close();
        }
    }

    public static String getNameWithoutExtention( File file ) {
        String name = file.getName();
        int lastDot = name.lastIndexOf("."); //$NON-NLS-1$
        name = name.substring(0, lastDot);
        return name;
    }

    public static String readfile( File file ) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = null;
            while( (line = br.readLine()) != null ) {
                if (line.length() == 0 || line.startsWith("#")) { //$NON-NLS-1$
                    continue;
                }
                sb.append(line).append("\n"); //$NON-NLS-1$
            }
            return sb.toString();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    GPLog.error("FILEUTILS", e.getLocalizedMessage(), e); //$NON-NLS-1$
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<String> readfileToList( File file ) throws IOException {
        BufferedReader br = null;
        List<String> linesList = new ArrayList<String>();
        try {
            br = new BufferedReader(new FileReader(file));
            String line = null;
            while( (line = br.readLine()) != null ) {
                if (line.length() == 0 || line.startsWith("#")) { //$NON-NLS-1$
                    continue;
                }
                linesList.add(line.trim());
            }
            return linesList;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    GPLog.error("FILEUTILS", e.getLocalizedMessage(), e); //$NON-NLS-1$
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writefile( String text, File file ) throws IOException {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(text);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    GPLog.error("FILEUTILS", e.getLocalizedMessage(), e); //$NON-NLS-1$
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns true if all deletions were successful. If a deletion fails, the method stops
     * attempting to delete and returns false.
     * 
     * @param filehandle
     * @return true if all deletions were successful
     */
    public static boolean deleteFileOrDir( File filehandle ) {

        if (filehandle.isDirectory()) {
            String[] children = filehandle.list();
            for( int i = 0; i < children.length; i++ ) {
                boolean success = deleteFileOrDir(new File(filehandle, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        boolean isdel = filehandle.delete();
        if (!isdel) {
            // if it didn't work, which often happens on windows systems,
            // remove on exit
            filehandle.deleteOnExit();
        }

        return isdel;
    }

    /**
     * Delete file or folder recursively on exit of the program
     * 
     * @param filehandle
     * @return true if all went well
     */
    public static boolean deleteFileOrDirOnExit( File filehandle ) {
        if (filehandle.isDirectory()) {
            String[] children = filehandle.list();
            for( int i = 0; i < children.length; i++ ) {
                boolean success = deleteFileOrDir(new File(filehandle, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        filehandle.deleteOnExit();
        return true;
    }

    /**
     * Checks if a given file exists in a supplied folder.
     * 
     * @param fileName the name of the file to check.
     * @param folder the folder.
     * @return <code>true</code>, if the file exists
     */
    public static boolean fileExistsInFolder( final String fileName, File folder ) {
        File[] listFiles = folder.listFiles(new FilenameFilter(){
            public boolean accept( File arg0, String tmpName ) {
                return fileName.trim().equals(tmpName.trim());
            }
        });

        return listFiles.length > 0;
    }

    /**
     * Read a bitmap, resampled to the supplied width.
     * 
     * @param imageFile the image to read.
     * @param newWidth the new width to which to sample.
     * @return the read {@link Bitmap}.
     */
    public static Bitmap readScaledBitmap( File imageFile, int newWidth ) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bounds);
        int width = bounds.outWidth;

        float sampleSizeF = (float) width / (float) newWidth;
        int sampleSize = Math.round(sampleSizeF);
        BitmapFactory.Options resample = new BitmapFactory.Options();
        resample.inSampleSize = sampleSize;

        Bitmap thumbnail = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), resample);
        return thumbnail;
    }

}
