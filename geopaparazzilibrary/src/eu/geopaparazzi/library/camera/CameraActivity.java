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
package eu.geopaparazzi.library.camera;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.sensors.SensorsManager;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;

/**
 * The taking pictures activity.
 * 
 * <p>
 * The image is created in a <b>media</b> folder inside the 
 * application folder. If the intent bundle contains a 
 * {@link LibraryConstants#PREFS_KEY_CAMERA_IMAGESAVEFOLDER}
 * value, that one is used as relative path inside the application folder.
 * </p>
 * <p>
 * The bundle is supposed to contain the gps position available through the keys:
 * {@link LibraryConstants#LONGITUDE},{@link LibraryConstants#LATITUDE},
 * {@link LibraryConstants#ELEVATION},{@link LibraryConstants#AZIMUTH} 
 * </p>
 * 
 * <p>
 * The activity returns the relative path to the generated image, that can be
 * retrieved through the {@link LibraryConstants#PREFS_KEY_PATH} key from
 * the bundle.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class CameraActivity extends Activity {

    private static final int CAMERA_PIC_REQUEST = 1337;
    private File mediaFolder;
    private String currentDatestring;
    private String imageFilePath;
    private Date currentDate;
    private double lon;
    private double lat;
    private double elevation;
    private int lastImageId;
    private String imageName;
    private String imagePropertiesName;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        Bundle extras = getIntent().getExtras();
        File imageSaveFolder = null;
        try {
            imageSaveFolder = ResourcesManager.getInstance(this).getMediaDir();
            if (extras != null) {
                String imageSaveFolderRelativePath = extras.getString(LibraryConstants.PREFS_KEY_CAMERA_IMAGESAVEFOLDER);
                if (imageSaveFolderRelativePath != null && imageSaveFolderRelativePath.length() > 0) {
                    File applicationDir = ResourcesManager.getInstance(this).getApplicationDir();
                    imageSaveFolder = new File(applicationDir, imageSaveFolderRelativePath);
                }
                imageName = extras.getString(LibraryConstants.PREFS_KEY_CAMERA_IMAGENAME);
                lon = extras.getDouble(LibraryConstants.LONGITUDE);
                lat = extras.getDouble(LibraryConstants.LATITUDE);
                elevation = extras.getDouble(LibraryConstants.ELEVATION);
            } else {
                throw new RuntimeException("Not implemented yet...");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!imageSaveFolder.exists()) {
            if (!imageSaveFolder.mkdirs()) {
                Runnable runnable = new Runnable(){
                    public void run() {
                        finish();
                    }
                };
                Utilities.messageDialog(this, getString(R.string.cantcreate_img_folder), runnable);
                return;
            }
        }

        mediaFolder = imageSaveFolder;

        currentDate = new Date();
        currentDatestring = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(currentDate);

        if (imageName == null) {
            imageName = "IMG_" + currentDatestring + ".jpg";
            imagePropertiesName = "IMG_" + currentDatestring + ".properties";
        } else {
            imageFilePath = mediaFolder.getAbsolutePath() + File.separator + imageName;
            File imgFile = new File(imageFilePath);
            String nameWithoutExtention = FileUtilities.getNameWithoutExtention(imgFile);
            imagePropertiesName = nameWithoutExtention + ".properties";
        }

        imageFilePath = mediaFolder.getAbsolutePath() + File.separator + imageName;
        File imgFile = new File(imageFilePath);
        Uri outputFileUri = Uri.fromFile(imgFile);

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

        lastImageId = getLastImageId();

        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if (requestCode == CAMERA_PIC_REQUEST) {
            checkTakenPictureConsistency();

            Intent intent = getIntent();

            File imageFile = new File(imageFilePath);
            if (imageFile.exists()) {

                SensorsManager sensorsManager = SensorsManager.getInstance(this);
                double azimuth = sensorsManager.getPictureAzimuth();

                double latitude = lat;
                double longitude = lon;

                String latRef = "N";
                String lonRef = "E";
                if (lat < 0) {
                    latRef = "S";
                }
                if (lon < 0) {
                    lonRef = "W";
                }
                double exifLat = Math.abs(latitude);
                double exifLon = Math.abs(longitude);

                String latString = Utilities.degreeDecimal2ExifFormat(exifLat);
                String lonString = Utilities.degreeDecimal2ExifFormat(exifLon);
                String altimString = String.valueOf(elevation);
                String azimuthString = String.valueOf((int) azimuth);

                if (GPLog.LOG) {
                    GPLog.addLogEntry(this, null, null, "Exif Lat=" + exifLat + " -- Lon=" + exifLon + " -- Azim=" + azimuth
                            + " -- Altim=" + altimString);
                }

                try {
                    ExifInterface exif = new ExifInterface(imageFilePath);

                    String latStringOld = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                    String lonStringOld = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                    String latRefStringOld = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                    String lonRefStringOld = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
                    String dateOld = exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
                    String timeOld = exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
                    String dateTimeOld = exif.getAttribute(ExifInterface.TAG_DATETIME);

                    Date date = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.parse(currentDatestring);

                    String exifDate = TimeUtilities.INSTANCE.EXIFFORMATTER.format(date);

                    String[] dateTimeSplit = exifDate.split("\\s+");
                    if (dateTimeOld == null || dateTimeOld.trim().length() <= 0) {
                        exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate);
                    }
                    if (dateOld == null || dateOld.trim().length() <= 0) {
                        exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, dateTimeSplit[0]);
                    }
                    if (timeOld == null || timeOld.trim().length() <= 0) {
                        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, dateTimeSplit[1]);
                    }
                    if (latStringOld == null || latRefStringOld == null || latStringOld.trim().length() <= 0) {
                        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latString);
                        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latRef);
                    }
                    if (lonStringOld == null || lonRefStringOld == null || lonStringOld.trim().length() <= 0) {
                        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lonString);
                        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lonRef);
                    }
                    String azz = (int) (azimuth * 100.0) + "/100";
                    String alt = (int) (elevation * 100.0) + "/100";
                    exif.setAttribute("GPSImgDirection", azz);
                    // exif.setAttribute("GPSImgDirectionRef", "M");
                    exif.setAttribute("GPSAltitude", alt);
                    // exif.setAttribute("GPSAltitudeRef", "0");

                    exif.saveAttributes();

                    // create props file
                    String propertiesFilePath = mediaFolder.getAbsolutePath() + File.separator + imagePropertiesName;
                    File propertiesFile = new File(propertiesFilePath);
                    BufferedWriter bW = null;
                    try {
                        StringBuilder sb = new StringBuilder();
                        sb.append("latitude=");
                        sb.append(String.valueOf(latitude));
                        sb.append("\nlongitude=");
                        sb.append(String.valueOf(longitude));
                        sb.append("\nazimuth=");
                        sb.append(azimuthString);
                        sb.append("\naltim=");
                        sb.append(altimString);
                        sb.append("\nutctimestamp=");
                        sb.append(currentDatestring).append("\n");
                        bW = new BufferedWriter(new FileWriter(propertiesFile));
                        bW.write(sb.toString());
                    } finally {
                        bW.close();
                    }

                    /*
                     * add the image to the database
                     */
                    String relativeImageFilePath = mediaFolder.getName() + File.separator + imageName;

                    intent.putExtra(LibraryConstants.PREFS_KEY_PATH, relativeImageFilePath);
                    intent.putExtra(LibraryConstants.LATITUDE, latitude);
                    intent.putExtra(LibraryConstants.LONGITUDE, longitude);
                    intent.putExtra(LibraryConstants.ELEVATION, elevation);
                    intent.putExtra(LibraryConstants.AZIMUTH, azimuth);
                    intent.putExtra(LibraryConstants.OBJECT_EXISTS, true);
                } catch (Exception e) {
                    Utilities.messageDialog(this, "An error occurred while adding gps info to the picture.", null);
                    e.printStackTrace();
                }
            } else {
                intent.putExtra(LibraryConstants.OBJECT_EXISTS, false);
            }

            setResult(Activity.RESULT_OK, intent);

            finish();
        }
    }
    private void checkTakenPictureConsistency() {
        /*
         * Checking for duplicate images
         * This is necessary because some camera implementation not only save where you want them to save but also in their default location.
         */
        final String[] projection = {MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.SIZE, MediaStore.Images.ImageColumns._ID};
        final String imageOrderBy = MediaStore.Images.Media._ID + " DESC";
        final String imageWhere = MediaStore.Images.Media._ID + ">?";
        final String[] imageArguments = {Integer.toString(lastImageId)};
        Cursor imageCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, imageWhere, imageArguments,
                imageOrderBy);
        List<File> cameraTakenMediaFiles = new ArrayList<File>();
        if (imageCursor.getCount() > 0) {
            while( imageCursor.moveToNext() ) {
                // int id =
                // imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
                String path = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                // Long takenTimeStamp =
                // imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN));
                // Long size =
                // imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.Media.SIZE));
                cameraTakenMediaFiles.add(new File(path));
            }
        }
        imageCursor.close();

        File imageFile = new File(imageFilePath);

        boolean imageExists = imageFile.exists();
        if (GPLog.LOG)
            GPLog.addLogEntry("Image file: " + imageFilePath + " exists: " + imageExists);

        if (!imageExists && cameraTakenMediaFiles.size() > 0) {
            // was not saved where I wanted, but the camera saved one in the media folder
            // try to copy over the one saved by the camera and then delete
            try {
                File cameraDoubleFile = cameraTakenMediaFiles.get(cameraTakenMediaFiles.size() - 1);
                FileUtilities.copyFile(cameraDoubleFile, imageFile);
                cameraDoubleFile.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for( File cameraTakenFile : cameraTakenMediaFiles ) {
            // delete the one duplicated
            cameraTakenFile.delete();
        }
    }

    /**
     * Gets the last image id from the media store.
     * 
     * @return the last image id from the media store.
     */
    private int getLastImageId() {
        final String[] imageColumns = {MediaStore.Images.Media._ID};
        final String imageOrderBy = MediaStore.Images.Media._ID + " DESC";
        final String imageWhere = null;
        final String[] imageArguments = null;
        Cursor imageCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageColumns, imageWhere, imageArguments,
                imageOrderBy);
        if (imageCursor.moveToFirst()) {
            int id = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
            imageCursor.close();
            return id;
        } else {
            return 0;
        }
    }

}