package eu.geopaparazzi.core.maptools.resourceviews;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.core.maptools.FeatureUtilities;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.spatialite.database.spatial.activities.camera.CameraDbActivity;
import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.Resource;
import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.BlobResource;
import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.ExternalResource;
import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.ResourceStorage;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.maptools.FeaturePagerActivity;

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
    private ImageGridViewAdapter<ImageItem> gridAdapter;
    private ArrayList<ImageItem> imageItems = new ArrayList<ImageItem>();
    private ResourceStorage storage;
    private long rowId = -1;
    private TextView numImagesView;
    private File imageSaveFolder;
    private boolean isReadOnly = true;

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
        isReadOnly = origIntent.getBooleanExtra(FeatureUtilities.KEY_READONLY, true);

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
        refreshThumbnails(imageItems);
        gridAdapter = new ImageGridViewAdapter<ImageItem>(this, R.layout.fragment_image_item, imageItems);
        gridView.setAdapter(gridAdapter);

        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ImageItem item = (ImageItem) parent.getItemAtPosition(position);
                removeImage(item);
                return true;
            }

        });

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ImageItem item = (ImageItem) parent.getItemAtPosition(position);
                showDocument(item);
            }

        });

    }

    protected void removeImage(ImageItem item) {
        final Resource res = item.getResource();
        final ImageItem theItem = item;
        GPDialogs.yesNoMessageDialog(ResourceBrowser.this, getString(R.string.confirm_remove_image), new Runnable() {
            @Override
            public void run() {
                storage.deleteResource(res);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            gridAdapter.remove(theItem);
                        } catch (Exception e) {
                            GPLog.error(this, null, e);
                        }
                    }
                });

            }
        }, null);
    }

    protected void showDocument(ImageItem item) {
        try {
            long resId = item.getResource().getId();
            // the item resource only contains the thumbnail,
            // so we need to get the complete resource
            final Resource res = storage.getResource(resId);
            if (res instanceof BlobResource) {
                BlobResource completeRes = (BlobResource) res;
                if (res.getType() == Resource.ResourceType.BLOB_IMAGE) {
                    AppsUtilities.showImage(completeRes.getBlob(), completeRes.getName(), this);
                } else if (res.getType() == Resource.ResourceType.BLOB_PDF) {
                    AppsUtilities.showPDF(completeRes.getBlob(), completeRes.getName(), this);
                } else if (completeRes.getMimeType().contains("/")) { // assume it is a mime type
                    AppsUtilities.showDocument(completeRes.getBlob(), completeRes.getMimeType(), completeRes.getName(), this);
                }

            }
            else {
                ExternalResource completeRes = (ExternalResource) res;
                //ToDo: the "getMainStorageDir" needs to be from the profile in case it is set to SECONDARYSTORAGE
                File image  = new File(ResourcesManager.getInstance(this).getMainStorageDir(), completeRes.getPath());
                if (res.getType() == Resource.ResourceType.EXTERNAL_IMAGE) {
                    AppsUtilities.showImage(image, this);
                } else if (res.getType() == Resource.ResourceType.EXTERNAL_PDF) {
                    AppsUtilities.showPDF(image, this);
                } else if (completeRes.getMimeType().contains("/")) { // assume it is a mime type
                    AppsUtilities.showDocument(image, completeRes.getMimeType(), this);
                }
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    protected void startCameraActivity() {
        Intent intent = new Intent(ResourceBrowser.this, CameraDbActivity.class);
        try {
            intent.putExtra(LibraryConstants.PREFS_KEY_CAMERA_IMAGESAVEFOLDER, imageSaveFolder.toString());
            intent.putExtra(CameraDbActivity.DBPATH_EXTRA_MESSAGE, storage.getDbFile());
            intent.putExtra(CameraDbActivity.TABLENAME_EXTRA_MESSAGE, storage.getTableName());
            intent.putExtra(CameraDbActivity.ROWID_EXTRA_MESSAGE, this.rowId);
            ResourceBrowser.this.startActivityForResult(intent, CAMERA_RETURN_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private ArrayList<ImageItem> refreshThumbnails(ArrayList<ImageItem> imageItems) {
        imageItems.clear();
        List<Resource> resources = storage.getThumbnails(rowId); //, Resource.ResourceType.BLOB_IMAGE);
        int i=1;
        for (Resource r: resources) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(r.getThumbnail(), 0, r.getThumbnail().length);
            String title = getResources().getString(R.string.Document_title);
            imageItems.add(new ImageItem(bitmap, r.getName(), r));
        }
        String text = getResources().getQuantityString(R.plurals.n_images, imageItems.size(), imageItems.size());
        numImagesView.setText(text);

        return imageItems;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isReadOnly) {
            getMenuInflater().inflate(R.menu.menu_capturepicture, menu);
        }
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
        if (CAMERA_RETURN_CODE == requestCode) {
            if(resultCode == RESULT_OK){
                if (data.getBooleanExtra(LibraryConstants.OBJECT_EXISTS, false)) {
                    refreshThumbnails(imageItems);
                    gridAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
