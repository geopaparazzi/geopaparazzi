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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.gps.GpsManager;
import eu.hydrologis.geopaparazzi.sensors.SensorsManager;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;

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

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        mediaFolder = ApplicationManager.getInstance(this).getMediaDir();

        Date date = new Date();
        currentDatestring = Constants.TIMESTAMPFORMATTER.format(date);
        String imageFilePath = mediaFolder.getAbsolutePath() + "/IMG_" + currentDatestring + ".jpg";
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
            // long utcTimeInSeconds = deviceManager.getUtcTime() / 1000L;

            String latString = String.valueOf(lat);
            String lonString = String.valueOf(lon);
            String altimString = String.valueOf(altim);
            // String timeString = String.valueOf(utcTimeInSeconds);
            String azimuthString = String.valueOf((int) azimuth);

            // create props file
            String propertiesFilePath = mediaFolder.getAbsolutePath() + "/IMG_" + currentDatestring + ".properties";
            File propertiesFile = new File(propertiesFilePath);
            BufferedWriter bW = null;
            try {
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            finish();
        }
    }

}