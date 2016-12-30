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

package eu.geopaparazzi.library.camera;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.MediaStore;

import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IImagesDbHelper;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.sensors.OrientationSensor;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * The taking pictures activity, speciallized behaviour for note pictures
 * <p/>
 * <p>
 * The image is created in a <b>tmp</b> folder inside the
 * application folder. If the intent bundle contains a
 * {@link LibraryConstants#PREFS_KEY_CAMERA_IMAGESAVEFOLDER}
 * value, that one is used as folder.
 * </p>
 * <p>
 * The bundle is supposed to contain the gps position available through the keys:
 * {@link LibraryConstants#LONGITUDE},{@link LibraryConstants#LATITUDE},
 * {@link LibraryConstants#ELEVATION},{@link LibraryConstants#AZIMUTH}
 * </p>
 * <p/>
 * <p>
 * The activity returns the id of the image inserted in the database, that can be
 * retrieved through the {@link LibraryConstants#DATABASE_ID} key from
 * the bundle.
 * </p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class CameraNoteActivity extends AbstractCameraActivity {
    protected double lon;
    protected double lat;
    protected double elevation;
    protected long noteId = -1;
    protected OrientationSensor orientationSensor;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Bundle extras = getIntent().getExtras();
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = new OrientationSensor(sensorManager, null);
        orientationSensor.register(this, SensorManager.SENSOR_DELAY_NORMAL);
        noteId = extras.getLong(LibraryConstants.DATABASE_ID);
        lon = extras.getDouble(LibraryConstants.LONGITUDE);
        lat = extras.getDouble(LibraryConstants.LATITUDE);
        elevation = extras.getDouble(LibraryConstants.ELEVATION);

        doTakePicture(icicle);
    }

    @Override
    public void doSaveData() {
        try {
            Intent intent = getIntent();
            byte[][] imageAndThumbnailArray = ImageUtilities.getImageAndThumbnailFromPath(imageFilePath, 5);

            Class<?> logHelper = Class.forName(DefaultHelperClasses.IMAGE_HELPER_CLASS);
            IImagesDbHelper imagesDbHelper = (IImagesDbHelper) logHelper.newInstance();

            double azimuth = orientationSensor.getAzimuthDegrees();

            long imageId = imagesDbHelper.addImage(lon, lat, elevation, azimuth, currentDate.getTime(), imageFile.getName(),
                    imageAndThumbnailArray[0], imageAndThumbnailArray[1], noteId);
            intent.putExtra(LibraryConstants.DATABASE_ID, imageId);
            intent.putExtra(LibraryConstants.OBJECT_EXISTS, true);

            // delete the file after insertion in db
            imageFile.delete();
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(this, e, null);
        }

    }

    @Override
    public void finish() {
        orientationSensor.unregister();
        super.finish();
    }


    /**
     * Gets the last image id from the media store.
     *
     * @return the last image id from the media store.
     */
    private int getLastImageMediaId() {
        final String[] imageColumns = {MediaStore.Images.Media._ID};
        final String imageOrderBy = MediaStore.Images.Media._ID + " DESC";
        final String imageWhere = null;
        final String[] imageArguments = null;
        Cursor imageCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageColumns, imageWhere, imageArguments,
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