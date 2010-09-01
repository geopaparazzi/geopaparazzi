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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * The taking pictures activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback {

    private Camera camera;
    // private boolean isPreviewRunning = false;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private File picturesDir;

    // private Camera.PictureCallback mPictureCallbackRaw = new Camera.PictureCallback(){
    // public void onPictureTaken( byte[] data, Camera c ) {
    // CameraActivity.this.camera.startPreview();
    // }
    // };

    // private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback(){
    // public void onShutter() {
    // }
    // };
    private ApplicationManager deviceManager;
    private ProgressDialog progressDialog;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.camera);

        surfaceView = (SurfaceView) findViewById(R.id.surface);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        deviceManager = ApplicationManager.getInstance();
        picturesDir = deviceManager.getPicturesDir();

    }

    public boolean onKeyDown( int keyCode, KeyEvent event ) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_CAMERA) {
            this.progressDialog = ProgressDialog.show(this, " Working...", " Retrieving image ", true, false);

            try {
                GpsLocation loc = deviceManager.getLoc();
                double lat = -1;
                double lon = -1;
                double altim = -1;
                if (loc != null) {
                    lat = loc.getLatitude();
                    lon = loc.getLongitude();
                    altim = loc.getAltitude();
                }
                double azimuth = deviceManager.getAzimuth();
                // long utcTimeInSeconds = deviceManager.getUtcTime() / 1000L;

                String latString = String.valueOf(lat);
                String lonString = String.valueOf(lon);
                String altimString = String.valueOf(altim);
                // String timeString = String.valueOf(utcTimeInSeconds);
                String azimuthString = String.valueOf((int) azimuth);

                // create props file
                final String currentDatestring = Constants.TIMESTAMPFORMATTER.format(new Date());
                String propertiesFilePath = picturesDir.getAbsolutePath() + "/IMG_" + currentDatestring + ".properties";
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
                } catch (IOException e1) {
                    throw new IOException(e1.getLocalizedMessage());
                } finally {
                    bW.close();
                }

                String imageFilePath = picturesDir.getAbsolutePath() + "/IMG_" + currentDatestring + ".jpg";
                File imgFile = new File(imageFilePath);
                Camera.PictureCallback captureCallback = new ImageCaptureCallback(imgFile, this);
                capture(captureCallback);

                return true;
            } catch (IOException e) {
                e.printStackTrace();
                alert("An error occurred while taking the picture: " + e.getLocalizedMessage());
                return false;
            } finally {
                this.progressDialog.dismiss();
            }
        }
        return false;
    }

    private void alert( String msg ) {
        Log.e("CameraActivity", msg);
        new AlertDialog.Builder(this).setTitle("An error occurred!").setMessage(msg)
                .setPositiveButton("Continue", new android.content.DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int arg1 ) {
                    }
                }).show();
    }

    public void surfaceChanged( SurfaceHolder holder, int format, int w, int h ) {
        // if (this.isPreviewRunning) {
        // camera.stopPreview();
        // }
        Camera.Parameters p = camera.getParameters();
        Size previewSize = p.getPreviewSize();
        Log.d("CAMERAACTIVITY", "Preview size before: " + previewSize.width + "/" + previewSize.height);

        p.setPreviewSize(previewSize.width, previewSize.height);
        camera.setParameters(p);
        camera.startPreview();
        // try {
        // this.camera.setPreviewDisplay(holder);
        // this.isPreviewRunning = true;
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
    }

    public void surfaceCreated( SurfaceHolder holder ) {
        camera = Camera.open();
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceDestroyed( SurfaceHolder holder ) {
        camera.stopPreview();
        // this.isPreviewRunning = false;
        camera.release();
        camera = null;
    }

    public boolean capture( Camera.PictureCallback jpegHandler ) {
        if (camera != null) {
            camera.takePicture(null, null, jpegHandler);
            return true;
        } else {
            return false;
        }
    }
}