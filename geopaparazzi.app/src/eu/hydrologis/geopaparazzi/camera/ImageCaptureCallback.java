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
        try {
            FileOutputStream filoutputStream = new FileOutputStream(imgFile);
            filoutputStream.write(data);
            filoutputStream.flush();
            filoutputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (imgFile.exists() && imgFile.length() > 0) {
            Toast.makeText(cameraActivity, R.string.imagesaved, Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(cameraActivity, R.string.imagenonsaved, Toast.LENGTH_LONG).show();
        }
        
    }
}
