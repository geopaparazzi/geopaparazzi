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
package eu.hydrologis.geopaparazzi.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.FileTypes;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivityStarter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TextRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.webproject.WebDataListActivity;
import eu.geopaparazzi.library.webproject.WebProjectsListActivity;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.ui.activities.tantomapurls.TantoMapurlsActivity;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.objects.Bookmark;
import eu.hydrologis.geopaparazzi.ui.dialogs.GpxImportDialogFragment;
import eu.hydrologis.geopaparazzi.utilities.Constants;
import gov.nasa.worldwind.AddWMSDialog;
import gov.nasa.worldwind.ogc.OGCBoundingBox;
import gov.nasa.worldwind.ogc.wms.WMSCapabilityInformation;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;

/**
 * Activity for export tasks.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImportActivity extends AppCompatActivity implements IActivityStarter, AddWMSDialog.OnWMSLayersAddedListener {

    public static final int PICKFILE_REQUEST_CODE = 666;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_import);

        Toolbar toolbar = (Toolbar) findViewById(eu.hydrologis.geopaparazzi.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Button gpxExportButton = (Button) findViewById(R.id.gpxImportButton);
        gpxExportButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                try {
                    importGpx();
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                }
            }
        });
        Button tantoMapurlsImportButton = (Button) findViewById(R.id.tantoMapurlsImportButton);
        tantoMapurlsImportButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent browseIntent = new Intent(ImportActivity.this, TantoMapurlsActivity.class);
                startActivity(browseIntent);
                finish();
            }
        });
        Button cloudImportButton = (Button) findViewById(R.id.cloudImportButton);
        cloudImportButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                final ImportActivity context = ImportActivity.this;

                if (!NetworkUtilities.isNetworkAvailable(context)) {
                    GPDialogs.infoDialog(context, context.getString(R.string.available_only_with_network), null);
                    return;
                }

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ImportActivity.this);
                final String user = preferences.getString(Constants.PREF_KEY_USER, "geopaparazziuser"); //$NON-NLS-1$
                final String passwd = preferences.getString(Constants.PREF_KEY_PWD, "geopaparazzipwd"); //$NON-NLS-1$
                final String server = preferences.getString(Constants.PREF_KEY_SERVER, ""); //$NON-NLS-1$

                if (server.length() == 0) {
                    GPDialogs.infoDialog(context, getString(R.string.error_set_cloud_settings), null);
                    return;
                }

                Intent webImportIntent = new Intent(ImportActivity.this, WebProjectsListActivity.class);
                webImportIntent.putExtra(LibraryConstants.PREFS_KEY_URL, server);
                webImportIntent.putExtra(LibraryConstants.PREFS_KEY_USER, user);
                webImportIntent.putExtra(LibraryConstants.PREFS_KEY_PWD, passwd);
                startActivity(webImportIntent);
            }
        });

        Button cloudSpatialiteImportButton = (Button) findViewById(R.id.cloudSpatialiteImportButton);
        cloudSpatialiteImportButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                final ImportActivity context = ImportActivity.this;

                if (!NetworkUtilities.isNetworkAvailable(context)) {
                    GPDialogs.infoDialog(context, context.getString(R.string.available_only_with_network), null);
                    return;
                }

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ImportActivity.this);
                final String user = preferences.getString(Constants.PREF_KEY_USER, "geopaparazziuser"); //$NON-NLS-1$
                final String passwd = preferences.getString(Constants.PREF_KEY_PWD, "geopaparazzipwd"); //$NON-NLS-1$
                final String server = preferences.getString(Constants.PREF_KEY_SERVER, ""); //$NON-NLS-1$

                if (server.length() == 0) {
                    GPDialogs.infoDialog(context, getString(R.string.error_set_cloud_settings), null);
                    return;
                }

                Intent webImportIntent = new Intent(ImportActivity.this, WebDataListActivity.class);
                webImportIntent.putExtra(LibraryConstants.PREFS_KEY_URL, server);
                webImportIntent.putExtra(LibraryConstants.PREFS_KEY_USER, user);
                webImportIntent.putExtra(LibraryConstants.PREFS_KEY_PWD, passwd);
                startActivityForResult(webImportIntent, WebDataListActivity.DOWNLOADDATA_RETURN_CODE);
            }
        });

        Button bookmarksImportButton = (Button) findViewById(R.id.bookmarksImportButton);
        bookmarksImportButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                importBookmarks();
            }
        });

        Button defaultDatabaseImportButton = (Button) findViewById(R.id.templatedatabaseImportButton);
        defaultDatabaseImportButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                importTemplateDatabase();
            }
        });

        Button wmsImportButton = (Button) findViewById(R.id.wmsImportButton);
        wmsImportButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                final ImportActivity context = ImportActivity.this;
                if (!NetworkUtilities.isNetworkAvailable(context)) {
                    GPDialogs.infoDialog(context, context.getString(R.string.available_only_with_network), null);
                    return;
                }
                AddWMSDialog addWMSDialog = AddWMSDialog.newInstance(null);
                addWMSDialog.show(getSupportFragmentManager(), "wms import");
            }
        });
    }

    private void importTemplateDatabase() {
        String ts = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date());
        String newName = "spatialite_" + ts + ".sqlite";
        GPDialogs.inputMessageDialog(this, getString(R.string.name_new_teample_db), newName, new TextRunnable() {
            @Override
            public void run() {

                try {
                    File sdcardDir = ResourcesManager.getInstance(ImportActivity.this).getSdcardDir();
                    File newDbFile = new File(sdcardDir, theTextToRunOn);

                    AssetManager assetManager = ImportActivity.this.getAssets();
                    InputStream inputStream = assetManager.open(LibraryConstants.GEOPAPARAZZI_TEMPLATE_DB_NAME);

                    FileUtilities.copyFile(inputStream, new FileOutputStream(newDbFile));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GPDialogs.infoDialog(ImportActivity.this, getString(R.string.new_template_db_create), null);
                        }
                    });
                } catch (final Exception e) {
                    GPLog.error(ImportActivity.this, null, e);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GPDialogs.errorDialog(ImportActivity.this, e, null);
                        }
                    });
                }

            }
        });

    }

    private void importGpx() throws Exception {
        String title = getString(R.string.select_gpx_file);
        AppsUtilities.pickFile(this, PICKFILE_REQUEST_CODE, title, new String[]{FileTypes.GPX.getExtension()}, null);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (PICKFILE_REQUEST_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String filePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                        if (!filePath.toLowerCase().endsWith(FileTypes.GPX.getExtension())) {
                            GPDialogs.warningDialog(this, getString(R.string.no_gpx_selected), null);
                            return;
                        }
                        File file = new File(filePath);
                        if (file.exists()) {
                            Utilities.setLastFilePath(this, filePath);
                            GpxImportDialogFragment gpxImportDialogFragment = GpxImportDialogFragment.newInstance(file.getAbsolutePath());
                            gpxImportDialogFragment.show(getSupportFragmentManager(), "gpx import");
                        }
                    } catch (Exception e) {
                        GPDialogs.errorDialog(this, e, null);
                    }
                }
                break;
            }
            case (WebDataListActivity.DOWNLOADDATA_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String filePath = data.getStringExtra(LibraryConstants.DATABASE_ID);
                        File file = new File(filePath);
                        if (file.exists()) {
                            SpatialiteSourcesManager.INSTANCE.addSpatialiteMapFromFile(file);
                        }
                    } catch (Exception e) {
                        GPDialogs.errorDialog(this, e, null);
                    }
                }
                break;
            }
        }
    }

    private void importBookmarks() {
        final ImportActivity context = ImportActivity.this;

        ResourcesManager resourcesManager = null;
        try {
            resourcesManager = ResourcesManager.getInstance(context);

            final File sdcardDir = resourcesManager.getSdcardDir();
            File[] bookmarksfileList = sdcardDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return filename.startsWith("bookmarks") && filename.endsWith(".csv");
                }
            });
            if (bookmarksfileList.length == 0) {
                GPDialogs.warningDialog(context, getString(R.string.no_bookmarks_csv), null);
                return;
            }

            final String[] items = new String[bookmarksfileList.length];
            for (int i = 0; i < items.length; i++) {
                items[i] = bookmarksfileList[i].getName();
            }

            new AlertDialog.Builder(this).setSingleChoiceItems(items, 0, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            String selectedItem = items[selectedPosition];
                            dialog.dismiss();
                            doImport(context, sdcardDir, selectedItem);
                        }
                    }).show();
        } catch (Exception e1) {
            GPLog.error(this, null, e1);
            GPDialogs.warningDialog(this, getString(R.string.bookmarks_import_error), null);
        }

    }

    private void doImport(final ImportActivity context, File sdcardDir, String fileName) {
        File bookmarksfile = new File(sdcardDir, fileName); //$NON-NLS-1$
        if (bookmarksfile.exists()) {
            try {
                // try to load it
                List<Bookmark> allBookmarks = DaoBookmarks.getAllBookmarks();
                TreeSet<String> bookmarksNames = new TreeSet<>();
                for (Bookmark bookmark : allBookmarks) {
                    String tmpName = bookmark.getName();
                    bookmarksNames.add(tmpName.trim());
                }

                List<String> bookmarksList = FileUtilities.readfileToList(bookmarksfile);
                int imported = 0;
                for (String bookmarkLine : bookmarksList) {
                    String[] split = bookmarkLine.split(","); //$NON-NLS-1$
                    // bookmarks are of type: Agritur BeB In Valle, 45.46564, 11.58969, 12
                    if (split.length < 3) {
                        continue;
                    }
                    String name = split[0].trim();
                    if (bookmarksNames.contains(name)) {
                        continue;
                    }
                    try {
                        double zoom = 16.0;
                        if (split.length == 4) {
                            zoom = Double.parseDouble(split[3]);
                        }
                        double lat = Double.parseDouble(split[1]);
                        double lon = Double.parseDouble(split[2]);
                        DaoBookmarks.addBookmark(lon, lat, name, zoom, -1, -1, -1, -1);
                        imported++;
                    } catch (Exception e) {
                        GPLog.error(this, null, e);
                    }
                }

                GPDialogs.infoDialog(context, getString(R.string.successfully_imported_bookmarks) + imported, null);
            } catch (IOException e) {
                GPLog.error(this, null, e);
                GPDialogs.infoDialog(context, getString(R.string.error_bookmarks_import), null);
            }
        } else {
            GPDialogs.warningDialog(context, getString(R.string.no_bookmarks_csv), null);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onWMSLayersAdded(String baseurl, String forcedWmsVersion, List<AddWMSDialog.LayerInfo> layersToAdd) {
        for (AddWMSDialog.LayerInfo li : layersToAdd) {
            String layerName = li.getName();

            StringBuilder sb = new StringBuilder();
            String wmsversion = "1.1.1";

            if (forcedWmsVersion != null) {
                wmsversion = forcedWmsVersion;
            } else if (li.caps.getVersion() != null) {
                wmsversion = li.caps.getVersion();
            }
            WMSCapabilityInformation capabilityInformation = li.caps.getCapabilityInformation();
            //            for (String imageFormat : capabilityInformation.getImageFormats()) {
            //                if (imageFormat.toLowerCase().endsWith("png") || imageFormat.toLowerCase().endsWith("jpeg"))
            //                    sb.append("format=").append(imageFormat).append("\n");
            //                break;
            //            }


            List<WMSLayerCapabilities> layerCapabilities = capabilityInformation.getLayerCapabilities();


            for (WMSLayerCapabilities layerCapability : layerCapabilities) {
                String srs = null;
                Set<String> crsList = layerCapability.getCRS();
                if (crsList.size() == 0) {
                    crsList = layerCapability.getSRS();
                }
                for (String crs : crsList) {
                    if (crs.equals("CRS:84") || crs.equals("EPSG:4326")) {
                        srs = crs;

                        boolean doLonLat = false;
                        if (crs.equals("CRS:84")) {
                            doLonLat = true;
                        } else if (crs.equals("EPSG:4326") && !wmsversion.equals("1.3.0")) {
                            doLonLat = true;
                        }

                        String bboxStr;
                        if (doLonLat) {
                            bboxStr = "XXX,YYY,XXX,YYY";
                        } else {
                            bboxStr = "YYY,XXX,YYY,XXX";
                        }
                        sb.append("url=" + baseurl.trim() + "?REQUEST=GetMap&SERVICE=WMS&VERSION=" + wmsversion //
                                + "&LAYERS=" + layerName + "&STYLES=&FORMAT=image/png&BGCOLOR=0xFFFFFF&TRANSPARENT=TRUE&SRS=" //
                                + srs + "&BBOX=" + bboxStr + "&WIDTH=256&HEIGHT=256\n");
                        sb.append("minzoom=1\n");
                        sb.append("maxzoom=22\n");
                        sb.append("defaultzoom=17\n");
                        sb.append("format=png\n");
                        sb.append("type=wms\n");
                        sb.append("description=").append(layerName).append("\n");


                        break;
                    }
                }

                if (srs == null) {
                    // TODO
                    return;
                }

                for (OGCBoundingBox bbox : layerCapability.getBoundingBoxes()) {
                    String crs = bbox.getCRS();
                    if (crs.equals("CRS:84") || crs.equals("EPSG:4326")) {
                        double centerX = bbox.getMinx() + (bbox.getMaxx() - bbox.getMinx()) / 2.0;
                        double centerY = bbox.getMiny() + (bbox.getMaxy() - bbox.getMiny()) / 2.0;
                        sb.append("center=");
                        sb.append(centerX).append(" ").append(centerY);
                        sb.append("\n");

                    }
                }

            }

            try {
                File applicationSupporterDir = ResourcesManager.getInstance(this).getApplicationSupporterDir();
                File newMapurl = new File(applicationSupporterDir, layerName + ".mapurl");

                sb.append("mbtiles=defaulttiles/_" + newMapurl.getName() + ".mbtiles\n");

                String mapurlText = sb.toString();
                FileUtilities.writefile(mapurlText, newMapurl);

                BaseMapSourcesManager.INSTANCE.addBaseMapFromFile(newMapurl);
                Button wmsImportButton = (Button) findViewById(R.id.wmsImportButton);
                GPDialogs.quickInfo(wmsImportButton, getString(R.string.wms_mapurl_added) + newMapurl.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            break;
        }

    }
}
