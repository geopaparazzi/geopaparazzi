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
package eu.hydrologis.geopaparazzi.camera;

import static eu.hydrologis.geopaparazzi.util.Constants.TIMESTAMPFORMATTER;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.gps.GpsManager;
import eu.hydrologis.geopaparazzi.sensors.SensorsManager;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Utilities;
import eu.hydrologis.geopaparazzi.util.debug.Debug;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * The taking pictures activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class CameraActivity extends Activity {

    private static final int CAMERA_PIC_REQUEST = 1337;
    private File mediaFolder;
    private String currentDatestring;
    private String imageFilePath;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        mediaFolder = ApplicationManager.getInstance(this).getMediaDir();

        Date date = new Date();
        currentDatestring = TIMESTAMPFORMATTER.format(date);
        imageFilePath = mediaFolder.getAbsolutePath() + "/IMG_" + currentDatestring + ".jpg";
        File imgFile = new File(imageFilePath);
        Uri outputFileUri = Uri.fromFile(imgFile);

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if (requestCode == CAMERA_PIC_REQUEST) {
            GpsLocation loc = GpsManager.getInstance(this).getLocation();
            double lat = -1;
            double lon = -1;
            double altim = -1;
            if (loc != null) {
                lat = loc.getLatitude();
                lon = loc.getLongitude();
                altim = loc.getAltitude();
            }
            SensorsManager sensorsManager = SensorsManager.getInstance(this);
            double azimuth = sensorsManager.getPictureAzimuth();

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
            String altimString = String.valueOf(altim);
            String azimuthString = String.valueOf((int) azimuth);

            if (Debug.D) {
                Logger.i(this, "Lat=" + lat + " -- Lon=" + lon + " -- Azim=" + azimuth + " -- Altim=" + altimString);
            }

            try {
                ExifInterface exif = new ExifInterface(imageFilePath);

                String azz = (int) (azimuth * 100.0) + "/100";
                String alt = (int) (altim * 100.0) + "/100";
                exif.setAttribute("GPSImgDirection", azz);
                // exif.setAttribute("GPSImgDirectionRef", "M");
                exif.setAttribute("GPSAltitude", alt);
                // exif.setAttribute("GPSAltitudeRef", "0");

                Date date = TIMESTAMPFORMATTER.parse(currentDatestring);
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
                    bW.write(latString);
                    bW.write("\nlongitude=");
                    bW.write(lonString);
                    bW.write("\nazimuth=");
                    bW.write(azimuthString);
                    bW.write("\naltim=");
                    bW.write(altimString);
                    bW.write("\nutctimestamp=");
                    bW.write(currentDatestring);
                } finally {
                    bW.close();
                }
            } catch (Exception e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("An error occurred while adding gps info to the picture.").setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                            public void onClick( DialogInterface dialog, int id ) {
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                e.printStackTrace();
            }

            finish();
        }
    }

}