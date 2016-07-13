package eu.hydrologis.geopaparazzi.maptools.resourceviews;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.content.res.Resources;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.camera.CameraActivity;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.ExternalResource;
import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.ResourceStorage;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maptools.FeaturePagerActivity;

/**
 * Activity to manage images associated to a vector feature.
 * Shows existing images on a grid and offers a button to take new
 * pictures.
 *
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ResourceBrowser extends AppCompatActivity {
    private static final int CAMERA_RETURN_CODE = 667;
    private GridView gridView;
    private ImageGridViewAdapter gridAdapter;
    private ResourceStorage storage;
    private long rowId = -1;
    private TextView numImagesView;
    private File imageSaveFolder;

    public final static String TABLEVIEW_EXTRA_MESSAGE = "eu.hydrologis.geopaparazzi.maptools.resourceviews.TABLEVIEW";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource_browser);

        Toolbar toolbar = (Toolbar) findViewById(eu.geopaparazzi.mapsforge.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent origIntent = getIntent();
        String tableName = origIntent.getStringExtra(FeaturePagerActivity.TABLENAME_EXTRA_MESSAGE);
        String dbPath = origIntent.getStringExtra(FeaturePagerActivity.DBPATH_EXTRA_MESSAGE);
        String rowIdStr = origIntent.getStringExtra(FeaturePagerActivity.ROWID_EXTRA_MESSAGE);
        if (rowIdStr!=null) {
            try {
                rowId = Long.parseLong(rowIdStr);
            }
            catch(NumberFormatException ex) {}
        }
        storage = ResourceStorage.getStorage(tableName, dbPath);
        try {
            imageSaveFolder = new File(ResourcesManager.getInstance(this).getApplicationSupporterDir(), "sqlite_res");
            if (!imageSaveFolder.exists()) {
                imageSaveFolder.mkdirs();
            }
        } catch(Exception exc) {};

        gridView = (GridView) findViewById(R.id.resourcesGridView);
        numImagesView = (TextView) findViewById(R.id.numImages);
        gridAdapter = new ImageGridViewAdapter(this, R.layout.fragment_image_item, getData());
        gridView.setAdapter(gridAdapter);


    }

    protected void startCameraActivity() {
        Intent intent = new Intent(ResourceBrowser.this, CameraActivity.class);
        try {
            intent.putExtra(LibraryConstants.PREFS_KEY_CAMERA_IMAGESAVEFOLDER, imageSaveFolder.toString());
            ResourceBrowser.this.startActivityForResult(intent, CAMERA_RETURN_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private ArrayList<ImageItem> getData() {
        final ArrayList<ImageItem> imageItems = new ArrayList<>();
        List<ExternalResource> resources = storage.getExternalResources(rowId, ExternalResource.TYPES.EXTERNAL_IMAGE);
        int i=0;
        for (ExternalResource r: resources) {
            String imgPath = r.getPath();
            File imgFile = new File(imgPath);
            String absPath;
            if (imgFile.isAbsolute()) {
                absPath = imgFile.getAbsolutePath();
            }
            else {
                absPath = new File(imageSaveFolder, r.getPath()).getAbsolutePath();
            }

            Bitmap bitmap = ImageUtilities.decodeSampledBitmapFromFile(absPath, 100, 100);
            imageItems.add(new ImageItem(bitmap, "Image#" + i++));
        }
        String text = getResources().getQuantityString(R.plurals.n_images, imageItems.size(), imageItems.size());
        numImagesView.setText(text);

        return imageItems;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_capturepicture, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.capture_picture) {
            startCameraActivity();
            return true;
        }
        else if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (CAMERA_RETURN_CODE == 667) {
            if(resultCode == RESULT_OK){
                if (data.getBooleanExtra(LibraryConstants.OBJECT_EXISTS, false)) {
                    final String imgPath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                    StringAsyncTask saveDataTask = new StringAsyncTask(this) {
                        private Exception ex;

                        @Override
                        protected String doBackgroundWork() {
                            try {
                                storeImagePath(imgPath);
                            } catch (Exception e) {
                                ex = e;
                            }
                            return "";
                        }

                        @Override
                        protected void doUiPostWork(String response) {
                            if (ex != null) {
                                GPLog.error(this, "ERROR", ex);
                                GPDialogs.errorDialog(ResourceBrowser.this, ex, null);
                            }
                        }

                    };
                    saveDataTask.setProgressDialog(null, getString(R.string.saving_to_database), false, null);
                    saveDataTask.execute();
                }
            }
        }
    }

    protected void storeImagePath(String imgPath) {
        try{
            // get relative path
            imgPath = imageSaveFolder.toURI().relativize(new File(imgPath).toURI()).getPath();
        } catch (Exception e) {}
        ExternalResource res = new ExternalResource(-1, imgPath, "");
        storage.insertResource(rowId, res, ExternalResource.TYPES.EXTERNAL_IMAGE);
        gridAdapter.notifyDataSetChanged();
    }
}
