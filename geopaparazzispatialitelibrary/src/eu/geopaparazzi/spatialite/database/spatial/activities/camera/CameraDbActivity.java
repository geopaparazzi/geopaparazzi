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

package eu.geopaparazzi.spatialite.database.spatial.activities.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.io.File;

import eu.geopaparazzi.library.camera.AbstractCameraActivity;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IImagesDbHelper;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.AbstractResource;
import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.BlobResource;
import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.ExternalResource;
import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.ResourceStorage;

/**
 * <p>The taking pictures activity.
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
public class CameraDbActivity extends AbstractCameraActivity {
    private long rowId;
    private ResourceStorage storage;
    private File imageSaveFolder;

    public final static String TABLENAME_EXTRA_MESSAGE = "eu.hydrologis.geopaparazzi.spatialite.TABLENAME";
    public final static String DBPATH_EXTRA_MESSAGE = "eu.hydrologis.geopaparazzi.spatialite.DBPATH";
    public final static String ROWID_EXTRA_MESSAGE = "eu.hydrologis.geopaparazzi.spatialite.ROWID";

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle extras = getIntent().getExtras();
        String tableName = extras.getString(TABLENAME_EXTRA_MESSAGE);
        String dbPath = extras.getString(DBPATH_EXTRA_MESSAGE);
        rowId = extras.getLong(ROWID_EXTRA_MESSAGE);
        storage = ResourceStorage.getStorage(tableName, dbPath);
        try {
            imageSaveFolder = new File(ResourcesManager.getInstance(this).getApplicationSupporterDir(), "sqlite_res");
            if (!imageSaveFolder.exists()) {
                imageSaveFolder.mkdirs();
            }
        } catch(Exception exc) {};
        doTakePicture(icicle);
    }

    @Override
    protected void doSaveData() {
        final String imgPath = imageFilePath;
        storeImageBlob(imgPath);
        /*
        // FIXME: the activity fails when saving data using an async task
        StringAsyncTask saveDataTask = new StringAsyncTask(this) {
            private Exception ex;

            @Override
            protected String doBackgroundWork() {
                try {
                    storeImageBlob(imgPath);
                } catch (Exception e) {
                    ex = e;
                }
                return "";
            }

            @Override
            protected void doUiPostWork(String response) {
                CameraDbActivity.this.finish();

                        gridAdapter.notifyDataSetChanged();
                        if (ex != null) {
                            GPLog.error(this, "ERROR", ex);
                            GPDialogs.errorDialog(ResourceBrowser.this, ex, null);
                        }
            }
        };*/

    }

    protected void storeImagePath(String imgPath) {
        try{
            // get relative path
            imgPath = imageSaveFolder.toURI().relativize(new File(imgPath).toURI()).getPath();
        } catch (Exception e) {}
        ExternalResource res = new ExternalResource(imgPath, "", AbstractResource.ResourceType.EXTERNAL_IMAGE);
        storage.insertResource(rowId, res);
    }

    /**
     * Stores as blob the image provided as a file system path
     * @param imgPath
     */
    protected void storeImageBlob(String imgPath) {
        try{
            byte[][] imageAndThumb = ImageUtilities.getImageAndThumbnailFromPath(imgPath, 10);
            BlobResource res = new BlobResource(imageAndThumb[0], "", AbstractResource.ResourceType.BLOB_IMAGE);
            res.setThumbnail(imageAndThumb[1]);
            storage.insertResource(rowId, res);
        } catch (Exception e) {
            GPLog.error(this, "ERROR", e);
            // FIXME: properly manage errors
        }
    }
}