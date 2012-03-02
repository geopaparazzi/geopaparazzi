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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.DirectoryBrowserActivity;
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
                Utilities.messageDialog(context, "Not implemented yet", null); //$NON-NLS-1$

                // if (!NetworkUtilities.isNetworkAvailable(context)) {
                // Utilities.messageDialog(context,
                // context.getString(R.string.available_only_with_network), null);
                // return;
                // }
                //
                // SharedPreferences preferences =
                // PreferenceManager.getDefaultSharedPreferences(context);
                //                final String user = preferences.getString(PREF_KEY_USER, ""); //$NON-NLS-1$
                //                final String pwd = preferences.getString(PREF_KEY_PWD, ""); //$NON-NLS-1$
                //                final String serverUrl = preferences.getString(PREF_KEY_SERVER, ""); //$NON-NLS-1$
                //
                // if (user.length() == 0 || pwd.length() == 0 || serverUrl.length() == 0) {
                // Utilities.messageDialog(context, R.string.error_set_cloud_settings, null);
                // return;
                // }
                //
                // importFromCloud(context, serverUrl, user, pwd);
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
