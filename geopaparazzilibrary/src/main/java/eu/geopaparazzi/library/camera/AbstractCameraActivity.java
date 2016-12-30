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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IImagesDbHelper;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * <p>Abstract activity for taking pictures, can be subclassed to
 * get specialized behaviours
 * <p/>
 * <p>
 * The image is created in a <b>tmp</b> folder inside the
 * application folder. If the intent bundle contains a
 * {@link LibraryConstants#PREFS_KEY_CAMERA_IMAGESAVEFOLDER}
 * value, that one is used as folder.
 * </p>
 * <p/>
 * <p>
 * The activity returns the path to the image on "disk"., that can be
 * retrieved through the {@link LibraryConstants#PREFS_KEY_PATH} key from
 * the bundle.
 * </p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
@SuppressWarnings("nls")
public abstract class AbstractCameraActivity extends Activity {

    protected static final int CAMERA_PIC_REQUEST = 1337;
    protected String imageFilePath;
    protected File imageFile;
    protected Date currentDate;
    protected int lastImageMediastoreId;

    protected void doTakePicture(Bundle icicle) {
        Bundle extras = getIntent().getExtras();
        File imageSaveFolder = null;
        try {
            imageSaveFolder = ResourcesManager.getInstance(this).getTempDir();
            String imageName;
            if (extras != null) {
                String imageSaveFolderTmp = extras.getString(LibraryConstants.PREFS_KEY_CAMERA_IMAGESAVEFOLDER);
                if (imageSaveFolderTmp != null && new File(imageSaveFolderTmp).exists()) {
                    imageSaveFolder = new File(imageSaveFolderTmp);
                }
                imageName = extras.getString(LibraryConstants.PREFS_KEY_CAMERA_IMAGENAME);
            } else {
                throw new RuntimeException("Not implemented yet...");
            }


            if (!imageSaveFolder.exists()) {
                if (!imageSaveFolder.mkdirs()) {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            finish();
                        }
                    };
                    GPDialogs.warningDialog(this, getString(R.string.cantcreate_img_folder), runnable);
                    return;
                }
            }

            File mediaFolder = imageSaveFolder;

            currentDate = new Date();

            if (imageName == null) {
                imageName = ImageUtilities.getCameraImageName(currentDate);
            }

            imageFilePath = mediaFolder.getAbsolutePath() + File.separator + imageName;
            File imgFile = new File(imageFilePath);
            Uri outputFileUri = Uri.fromFile(imgFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

            lastImageMediastoreId = getLastImageMediaId();

            startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(this, e, new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_PIC_REQUEST) {
            checkTakenPictureConsistency();

            Intent intent = getIntent();
            File imageFile = new File(imageFilePath);
            if (imageFile.exists()) {
                intent.putExtra(LibraryConstants.PREFS_KEY_PATH, imageFilePath);
                intent.putExtra(LibraryConstants.OBJECT_EXISTS, true);
                doSaveData();
            } else {
                intent.putExtra(LibraryConstants.OBJECT_EXISTS, false);
            }
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    protected void checkTakenPictureConsistency() {
        try {
            /*
             * Checking for duplicate images
             * This is necessary because some camera implementation not only save where you want them to save but also in their default location.
             */
            final String[] projection = {MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Images.ImageColumns.SIZE, MediaStore.Images.ImageColumns._ID};
            final String imageOrderBy = MediaStore.Images.Media._ID + " DESC";
            final String imageWhere = MediaStore.Images.Media._ID + ">?";
            final String[] imageArguments = {Integer.toString(lastImageMediastoreId)};
            Cursor imageCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, imageWhere, imageArguments,
                    imageOrderBy);
            List<File> cameraTakenMediaFiles = new ArrayList<>();
            if (imageCursor.getCount() > 0) {
                while (imageCursor.moveToNext()) {
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

            imageFile = new File(imageFilePath);

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
                    GPLog.error(this, null, e);
                }
            }
            for (File cameraTakenFile : cameraTakenMediaFiles) {
                // delete the one duplicated
                cameraTakenFile.delete();
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
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

    protected abstract void doSaveData();
}