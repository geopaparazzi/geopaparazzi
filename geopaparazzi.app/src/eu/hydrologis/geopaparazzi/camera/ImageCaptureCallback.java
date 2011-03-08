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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.widget.Toast;

/**
 * The {@link PictureCallback callback} that takes care of writing the image to disk.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImageCaptureCallback implements PictureCallback {
    private final File imgFile;
    private final CameraActivity cameraActivity;

    public ImageCaptureCallback( File imgFile, CameraActivity cameraActivity ) throws IOException {
        this.imgFile = imgFile;
        this.cameraActivity = cameraActivity;
    }

    public void onPictureTaken( byte[] data, Camera camera ) {
        FileOutputStream filoutputStream = null;
        try {
            filoutputStream = new FileOutputStream(imgFile);
            filoutputStream.write(data);
        } catch (Exception e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        } finally {
            if (filoutputStream != null) {
                try {
                    filoutputStream.flush();
                    filoutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (imgFile.exists() && imgFile.length() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(cameraActivity);
            String ok = cameraActivity.getResources().getString(R.string.ok);
            String msg = cameraActivity.getResources().getString(R.string.imagesaved);
            builder.setMessage(msg).setCancelable(false).setPositiveButton(ok, new DialogInterface.OnClickListener(){
                public void onClick( DialogInterface dialog, int id ) {
                    cameraActivity.finish();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            Toast.makeText(cameraActivity, R.string.imagenonsaved, Toast.LENGTH_LONG).show();
        }

    }
}
