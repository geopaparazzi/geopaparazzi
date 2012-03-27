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

import static eu.hydrologis.geopaparazzi.util.Constants.PREF_KEY_PWD;
import static eu.hydrologis.geopaparazzi.util.Constants.PREF_KEY_SERVER;
import static eu.hydrologis.geopaparazzi.util.Constants.PREF_KEY_USER;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.DirectoryBrowserActivity;
import eu.geopaparazzi.library.webproject.WebProjectsListActivity;
import eu.hydrologis.geopaparazzi.R;

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
                importGpx();
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
    }

    // private void importFromCloud( final ImportActivity context, final String serverUrl, final
    // String user, final String pwd ) {
    // // TODO
    // }

    private void importGpx() {
        Intent browseIntent = new Intent(ImportActivity.this, DirectoryBrowserActivity.class);
        browseIntent.putExtra(DirectoryBrowserActivity.STARTFOLDERPATH, ResourcesManager.getInstance(ImportActivity.this)
                .getApplicationDir().getAbsolutePath());
        browseIntent.putExtra(DirectoryBrowserActivity.INTENT_ID, Constants.GPXIMPORT);
        browseIntent.putExtra(DirectoryBrowserActivity.EXTENTION, ".gpx"); //$NON-NLS-1$
        startActivity(browseIntent);
        finish();
    }

}
