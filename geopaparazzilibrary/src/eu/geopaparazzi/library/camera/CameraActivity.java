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
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.sensors.SensorsManager;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

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

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        Bundle extras = getIntent().getExtras();
        File imageSaveFolder = ResourcesManager.getInstance(this).getMediaDir();
        if (extras != null) {
            String imageSaveFolderRelativePath = extras.getString(LibraryConstants.PREFS_KEY_CAMERA_IMAGESAVEFOLDER);
            if (imageSaveFolderRelativePath != null && imageSaveFolderRelativePath.length() > 0) {
                File applicationDir = ResourcesManager.getInstance(this).getApplicationDir();
                imageSaveFolder = new File(applicationDir, imageSaveFolderRelativePath);
            }
            lon = extras.getDouble(LibraryConstants.LONGITUDE);
            lat = extras.getDouble(LibraryConstants.LATITUDE);
            elevation = extras.getDouble(LibraryConstants.ELEVATION);
        } else {
            throw new RuntimeException("Not implemented yet...");
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
        currentDatestring = LibraryConstants.TIMESTAMPFORMATTER.format(currentDate);
        imageFilePath = mediaFolder.getAbsolutePath() + "/IMG_" + currentDatestring + ".jpg";
        File imgFile = new File(imageFilePath);
        Uri outputFileUri = Uri.fromFile(imgFile);

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if (requestCode == CAMERA_PIC_REQUEST) {
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
            lat = Math.abs(lat);
            lon = Math.abs(lon);

            String latString = Utilities.degreeDecimal2ExifFormat(lat);
            String lonString = Utilities.degreeDecimal2ExifFormat(lon);
            String altimString = String.valueOf(elevation);
            String azimuthString = String.valueOf((int) azimuth);

            if (Debug.D) {
                Logger.i(this, "Lat=" + lat + " -- Lon=" + lon + " -- Azim=" + azimuth + " -- Altim=" + altimString);
            }

            try {
                ExifInterface exif = new ExifInterface(imageFilePath);

                String azz = (int) (azimuth * 100.0) + "/100";
                String alt = (int) (elevation * 100.0) + "/100";
                exif.setAttribute("GPSImgDirection", azz);
                // exif.setAttribute("GPSImgDirectionRef", "M");
                exif.setAttribute("GPSAltitude", alt);
                // exif.setAttribute("GPSAltitudeRef", "0");

                Date date = LibraryConstants.TIMESTAMPFORMATTER.parse(currentDatestring);
                exif.setAttribute(ExifInterface.TAG_DATETIME, date.toString());
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latString);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latRef);
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lonString);
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lonRef);

                exif.saveAttributes();

                // create props file
                String propertiesFilePath = mediaFolder.getAbsolutePath() + "/IMG_" + currentDatestring + ".properties";
                File propertiesFile = new File(propertiesFilePath);
                BufferedWriter bW = null;
                try {
                    bW = new BufferedWriter(new FileWriter(propertiesFile));
                    bW.write("latitude=");
                    bW.write(String.valueOf(lat));
                    bW.write("\nlongitude=");
                    bW.write(String.valueOf(lon));
                    bW.write("\nazimuth=");
                    bW.write(azimuthString);
                    bW.write("\naltim=");
                    bW.write(altimString);
                    bW.write("\nutctimestamp=");
                    bW.write(currentDatestring);
                } finally {
                    bW.close();
                }

                /*
                 * add the image to the database
                 */
                String relativeImageFilePath = mediaFolder.getName() + "/IMG_" + currentDatestring + ".jpg";

                Intent intent = getIntent();
                intent.putExtra(LibraryConstants.PREFS_KEY_PATH, relativeImageFilePath);
                intent.putExtra(LibraryConstants.LATITUDE, latitude);
                intent.putExtra(LibraryConstants.LONGITUDE, longitude);
                intent.putExtra(LibraryConstants.ELEVATION, elevation);
                intent.putExtra(LibraryConstants.AZIMUTH, azimuth);
                setResult(Activity.RESULT_OK, intent);

            } catch (Exception e) {
                Utilities.messageDialog(this, "An error occurred while adding gps info to the picture.", null);
                e.printStackTrace();
            }
            finish();
        }
    }

}