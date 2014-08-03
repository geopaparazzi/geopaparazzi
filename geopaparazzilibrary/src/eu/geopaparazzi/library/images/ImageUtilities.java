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

package eu.geopaparazzi.library.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import eu.geopaparazzi.library.util.TimeUtilities;

/**
 * Images helper utilities.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImageUtilities {

    public static String getSketchImageName(Date date) {
        if (date == null)
            date = new Date();
        String currentDatestring = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(date);
        return "SKETCH_" + currentDatestring + ".png";
    }

    public static String getCameraImageName(Date date) {
        if (date == null)
            date = new Date();
        String currentDatestring = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(date);
        return "IMG_" + currentDatestring + ".jpg";
    }


    /**
     * Get an image from a file by its path.
     *
     * @param imageFilePath the image path.
     * @param tryCount      times to try in 300 millis loop, in case the image is
     *                      not yet on disk. (ugly but no other way right now)
     * @return the image data or null.
     */
    public static byte[] getImageFromPath(String imageFilePath, int tryCount) {
        Bitmap image = BitmapFactory.decodeFile(imageFilePath);
        int count = 0;
        while (image == null && ++count < tryCount) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            image = BitmapFactory.decodeFile(imageFilePath);
        }
        if (image == null) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
    }


    public static Bitmap getScaledBitmap(int targetW, int targetH, String imagePath) {

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);
        return bitmap;
    }

    public static Bitmap getImageFromImageData(byte[] imageData) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        return bitmap;
    }

    /**
     * Write am image to disk.
     *
     * @param imageData the data to write.
     * @param imagePath the path to write to.
     * @throws IOException
     */
    public static void writeImageDataToFile(byte[] imageData, String imagePath) throws IOException {
        FileOutputStream fout = new FileOutputStream(imagePath);
        try {
            fout.write(imageData);
        } finally {
            fout.close();
        }
    }
}
