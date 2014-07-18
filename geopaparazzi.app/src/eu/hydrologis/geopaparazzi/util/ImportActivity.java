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
package eu.hydrologis.geopaparazzi.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.TreeSet;

import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.DirectoryBrowserActivity;
import eu.geopaparazzi.library.webproject.WebProjectsListActivity;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.tantomapurls.TantoMapurlsActivity;

import static eu.hydrologis.geopaparazzi.util.Constants.PREF_KEY_PWD;
import static eu.hydrologis.geopaparazzi.util.Constants.PREF_KEY_SERVER;
import static eu.hydrologis.geopaparazzi.util.Constants.PREF_KEY_USER;

/**
 * Activity for export tasks.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImportActivity extends Activity {

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.imports);

        ImageButton gpxExportButton = (ImageButton) findViewById(R.id.gpxImportButton);
        gpxExportButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                try {
                    importGpx();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        ImageButton tantoMapurlsImportButton = (ImageButton) findViewById(R.id.tantoMapurlsImportButton);
        tantoMapurlsImportButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Intent browseIntent = new Intent(ImportActivity.this, TantoMapurlsActivity.class);
                startActivity(browseIntent);
                finish();
            }
        });
        ImageButton cloudImportButton = (ImageButton) findViewById(R.id.cloudImportButton);
        cloudImportButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                final ImportActivity context = ImportActivity.this;

                if (!NetworkUtilities.isNetworkAvailable(context)) {
                    Utilities.messageDialog(context, context.getString(R.string.available_only_with_network), null);
                    return;
                }

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ImportActivity.this);
                final String user = preferences.getString(PREF_KEY_USER, "geopaparazziuser"); //$NON-NLS-1$
                final String passwd = preferences.getString(PREF_KEY_PWD, "geopaparazzipwd"); //$NON-NLS-1$
                final String server = preferences.getString(PREF_KEY_SERVER, ""); //$NON-NLS-1$

                if (server.length() == 0) {
                    Utilities.messageDialog(context, R.string.error_set_cloud_settings, null);
                    return;
                }

                Intent webImportIntent = new Intent(ImportActivity.this, WebProjectsListActivity.class);
                webImportIntent.putExtra(LibraryConstants.PREFS_KEY_URL, server);
                webImportIntent.putExtra(LibraryConstants.PREFS_KEY_USER, user);
                webImportIntent.putExtra(LibraryConstants.PREFS_KEY_PWD, passwd);
                startActivity(webImportIntent);
            }
        });

        ImageButton bookmarksImportButton = (ImageButton) findViewById(R.id.bookmarksImportButton);
        bookmarksImportButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                importBookmarks();
            }
        });
    }

    private void importGpx() throws Exception {
        Intent browseIntent = new Intent(ImportActivity.this, DirectoryBrowserActivity.class);
        browseIntent.putExtra(DirectoryBrowserActivity.STARTFOLDERPATH, ResourcesManager.getInstance(ImportActivity.this)
                .getApplicationDir().getAbsolutePath());
        browseIntent.putExtra(DirectoryBrowserActivity.INTENT_ID, Constants.GPXIMPORT);
        browseIntent.putExtra(DirectoryBrowserActivity.EXTENTION, ".gpx"); //$NON-NLS-1$
        startActivity(browseIntent);
        finish();
    }

    private void importBookmarks() {
        final ImportActivity context = ImportActivity.this;

        ResourcesManager resourcesManager = null;
        try {
            resourcesManager = ResourcesManager.getInstance(context);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        final File sdcardDir = resourcesManager.getSdcardDir();
        File[] bookmarksfileList = sdcardDir.listFiles(new FilenameFilter(){
            public boolean accept( File dir, String filename ) {
                return filename.startsWith("bookmarks") && filename.endsWith(".csv");
            }
        });
        if (bookmarksfileList.length == 0) {
            Utilities.warningDialog(context, R.string.no_bookmarks_csv, null);
            return;
        }

        final String[] items = new String[bookmarksfileList.length];
        for( int i = 0; i < items.length; i++ ) {
            items[i] = bookmarksfileList[i].getName();
        }

        new AlertDialog.Builder(this).setSingleChoiceItems(items, 0, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){

                    public void onClick( DialogInterface dialog, int whichButton ) {
                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        String selectedItem = items[selectedPosition];
                        dialog.dismiss();
                        doImport(context, sdcardDir, selectedItem);
                    }
                }).show();

    }

    private void doImport( final ImportActivity context, File sdcardDir, String fileName ) {
        File bookmarksfile = new File(sdcardDir, fileName); //$NON-NLS-1$
        if (bookmarksfile.exists()) {
            try {
                // try to load it
                List<Bookmark> allBookmarks = DaoBookmarks.getAllBookmarks();
                TreeSet<String> bookmarksNames = new TreeSet<String>();
                for( Bookmark bookmark : allBookmarks ) {
                    String tmpName = bookmark.getName();
                    bookmarksNames.add(tmpName.trim());
                }

                List<String> bookmarksList = FileUtilities.readfileToList(bookmarksfile);
                int imported = 0;
                for( String bookmarkLine : bookmarksList ) {
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

                        e.printStackTrace();
                    }
                }

                Utilities.messageDialog(context, getString(R.string.successfully_imported_bookmarks) + imported, null);
            } catch (IOException e) {
                e.printStackTrace();
                Utilities.messageDialog(context, R.string.error_bookmarks_import, null);
            }
        } else {
            Utilities.warningDialog(context, R.string.no_bookmarks_csv, null);
        }
    }

}
