/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
