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

package eu.geopaparazzi.library.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.ByteArrayOutputStream;
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
    public static final int MAXBLOBSIZE = 1900000;
    public static final int THUMBNAILWIDTH = 100;

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

    public static String getMapImageName(Date date) {
        if (date == null)
            date = new Date();
        String currentDatestring = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(date);
        return "MAP_" + currentDatestring + ".png";
    }

    public static boolean isImagePath(String path) {
        return path.toLowerCase().endsWith("jpg") || path.toLowerCase().endsWith("png");
    }

    /**
     * Get the default temporary image file name.
     *
     * @param ext and optional dot+extension to add. If null, '.jpg' is used.
     * @return the image name.
     */
    public static String getTempImageName(String ext) {
        if (ext == null) ext = ".jpg";
        return "tmp_gp_image" + ext;
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

        // It is necessary to rotate the image before converting to bytes, as the exif information
        // will be lost afterwards and the image will be incorrectly oriented in some devices
        float orientation = getRotation(imageFilePath);
        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);

            image = Bitmap.createBitmap(image, 0, 0, image.getWidth(),
                    image.getHeight(), matrix, true);
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
    }

    /**
     * Get an image and thumbnail from a file by its path.
     *
     * @param imageFilePath the image path.
     * @param tryCount      times to try in 300 millis loop, in case the image is
     *                      not yet on disk. (ugly but no other way right now)
     * @return the image and thumbnail data or null.
     */
    public static byte[][] getImageAndThumbnailFromPath(String imageFilePath, int tryCount) {
        byte[][] imageAndThumbNail = new byte[2][];

        // first read full image and check existence
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

        // It is necessary to rotate the image before converting to bytes, as the exif information
        // will be lost afterwards and the image will be incorrectly oriented in some devices
        float orientation = getRotation(imageFilePath);
        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);

            image = Bitmap.createBitmap(image, 0, 0, image.getWidth(),
                    image.getHeight(), matrix, true);
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // define sampling for thumbnail
        float sampleSizeF = (float) width / (float) THUMBNAILWIDTH;
        float newHeight = height/sampleSizeF;
        Bitmap thumbnail =  Bitmap.createScaledBitmap(image, THUMBNAILWIDTH, (int)newHeight, false);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageBytes = stream.toByteArray();

        stream = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] thumbnailBytes = stream.toByteArray();

        imageAndThumbNail[0] = imageBytes;
        imageAndThumbNail[1] = thumbnailBytes;
        return imageAndThumbNail;
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

    /**
     * Calculates the optimum inSampleSize parameter based on the real
     * image size and the required subsampled size
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Loads a subsampled version of the image
     *
     * @param imgPath The path to the image to load
     * @param reqWidth The width required for the subsampled version
     * @param reqHeight The height required for the subsampled version
     * @return
     */
    public static Bitmap decodeSampledBitmapFromFile(String imgPath,
                                                     int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imgPath, options);
    }

    public static float getRotation(String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90f;
            } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180f;
            } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270f;
            }
        } catch (IOException e) {}
        return 0f;
    }
}
