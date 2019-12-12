package eu.geopaparazzi.map.gui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.features.FeatureUtilities;
import eu.geopaparazzi.map.features.editing.resourcestorage.BlobResource;
import eu.geopaparazzi.map.features.editing.resourcestorage.ExternalResource;
import eu.geopaparazzi.map.features.editing.resourcestorage.Resource;
import eu.geopaparazzi.map.features.editing.resourcestorage.ResourceStorage;

/**
 * Activity to manage images associated to a vector feature.
 * Shows existing images on a grid and offers a button to take new
 * pictures.
 *
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ResourceBrowserActivity extends AppCompatActivity {
    private static final int CAMERA_RETURN_CODE = 667;
    private GridView gridView;
    private ResourceImageGridViewAdapter<ResourceImageItem> gridAdapter;
    private ArrayList<ResourceImageItem> imageItems = new ArrayList<>();
    private ResourceStorage storage;
    private long rowId = -1;
    private TextView numImagesView;
    private File imageSaveFolder;
    private boolean isReadOnly = true;

    public final static String TABLEVIEW_EXTRA_MESSAGE = "eu.hydrologis.geopaparazzi.maptools.resourceviews.TABLEVIEW";//NON-NLS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource_browser);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent origIntent = getIntent();
        String tableName = origIntent.getStringExtra(FeaturePagerActivity.TABLENAME_EXTRA_MESSAGE);
        String dbPath = origIntent.getStringExtra(FeaturePagerActivity.DBPATH_EXTRA_MESSAGE);
        rowId = origIntent.getLongExtra(FeaturePagerActivity.ROWID_EXTRA_MESSAGE, -1);
        isReadOnly = origIntent.getBooleanExtra(FeatureUtilities.KEY_READONLY, true);

        try {
            storage = ResourceStorage.getStorage(tableName, dbPath);
            imageSaveFolder = new File(ResourcesManager.getInstance(this).getApplicationSupporterDir(), "sqlite_res");//NON-NLS
            if (!imageSaveFolder.exists()) {
                imageSaveFolder.mkdirs();
            }
        } catch (Exception exc) {
        }

        gridView = findViewById(R.id.resourcesGridView);
        numImagesView = findViewById(R.id.numImages);
        refreshThumbnails(imageItems);
        gridAdapter = new ResourceImageGridViewAdapter<>(this, R.layout.fragment_image_item, imageItems);
        gridView.setAdapter(gridAdapter);

        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ResourceImageItem item = (ResourceImageItem) parent.getItemAtPosition(position);
                removeImage(item);
                return true;
            }

        });

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ResourceImageItem item = (ResourceImageItem) parent.getItemAtPosition(position);
                showDocument(item);
            }

        });

    }

    protected void removeImage(ResourceImageItem item) {
        final Resource res = item.getResource();
        final ResourceImageItem theItem = item;
        GPDialogs.yesNoMessageDialog(ResourceBrowserActivity.this, getString(R.string.confirm_remove_image), new Runnable() {
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

    protected void showDocument(ResourceImageItem item) {
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

            } else {
                ExternalResource completeRes = (ExternalResource) res;
                //ToDo: the "getMainStorageDir" needs to be from the profile in case it is set to SECONDARYSTORAGE
                File image = new File(ResourcesManager.getInstance(this).getMainStorageDir(), completeRes.getPath());
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
        Intent intent = new Intent(ResourceBrowserActivity.this, CameraDbActivity.class);
        try {
            intent.putExtra(LibraryConstants.PREFS_KEY_CAMERA_IMAGESAVEFOLDER, imageSaveFolder.toString());
            intent.putExtra(CameraDbActivity.DBPATH_EXTRA_MESSAGE, storage.getDbPath());
            intent.putExtra(CameraDbActivity.TABLENAME_EXTRA_MESSAGE, storage.getTableName());
            intent.putExtra(CameraDbActivity.ROWID_EXTRA_MESSAGE, this.rowId);
            ResourceBrowserActivity.this.startActivityForResult(intent, CAMERA_RETURN_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private ArrayList<ResourceImageItem> refreshThumbnails(ArrayList<ResourceImageItem> imageItems) {
        imageItems.clear();
        List<Resource> resources = storage.getThumbnails(rowId); //, Resource.ResourceType.BLOB_IMAGE);
        int i = 1;
        for (Resource r : resources) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(r.getThumbnail(), 0, r.getThumbnail().length);
            String title = getResources().getString(R.string.Document_title);
            imageItems.add(new ResourceImageItem(bitmap, r.getName(), r));
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
        } else if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (CAMERA_RETURN_CODE == requestCode) {
            if (resultCode == RESULT_OK) {
                if (data.getBooleanExtra(LibraryConstants.OBJECT_EXISTS, false)) {
                    refreshThumbnails(imageItems);
                    gridAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
