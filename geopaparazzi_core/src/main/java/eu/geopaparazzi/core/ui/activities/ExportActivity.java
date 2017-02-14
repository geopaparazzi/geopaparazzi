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
package eu.geopaparazzi.core.ui.activities;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.maps.SpatialiteMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.plugin.ExtensionPoints;
import eu.geopaparazzi.library.plugin.PluginLoaderListener;
import eu.geopaparazzi.library.plugin.menu.IMenuLoader;
import eu.geopaparazzi.library.plugin.menu.MenuLoader;
import eu.geopaparazzi.library.plugin.style.StyleHelper;
import eu.geopaparazzi.library.plugin.types.IMenuEntry;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.webproject.WebDataUploadListActivity;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoBookmarks;
import eu.geopaparazzi.core.database.DaoImages;
import eu.geopaparazzi.core.database.objects.Bookmark;
import eu.geopaparazzi.core.ui.dialogs.GpxExportDialogFragment;
import eu.geopaparazzi.core.ui.dialogs.KmzExportDialogFragment;
import eu.geopaparazzi.core.ui.dialogs.StageExportDialogFragment;
import eu.geopaparazzi.core.utilities.Constants;

import static eu.geopaparazzi.library.util.LibraryConstants.DATABASE_ID;

/**
 * Activity for export tasks.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ExportActivity extends AppCompatActivity implements
        NfcAdapter.CreateBeamUrisCallback, IActivitySupporter {
    public static final int START_REQUEST_CODE = 666;
    private NfcAdapter mNfcAdapter;

    // List of URIs to provide to Android Beam
    private Uri[] mFileUris = new Uri[1];
    private PendingIntent pendingIntent;

    private SparseArray<IMenuEntry> menuEntriesMap = new SparseArray<>();

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_export);

        Toolbar toolbar = (Toolbar) findViewById(eu.geopaparazzi.core.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);


        try {
            checkNfc();
        } catch (Exception e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
        }

//        Button cloudDataExportButton = (Button) findViewById(R.id.cloudDataExportButton);
//        cloudDataExportButton.setOnClickListener(new Button.OnClickListener() {
//            public void onClick(View v) {
//                final ExportActivity context = ExportActivity.this;
//                if (!NetworkUtilities.isNetworkAvailable(context)) {
//                    GPDialogs.infoDialog(context, context.getString(R.string.available_only_with_network), null);
//                    return;
//                }
//
//                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//                final String user = preferences.getString(Constants.PREF_KEY_USER, "geopaparazziuser"); //$NON-NLS-1$
//                final String pwd = preferences.getString(Constants.PREF_KEY_PWD, "geopaparazzipwd"); //$NON-NLS-1$
//                final String serverUrl = preferences.getString(Constants.PREF_KEY_SERVER, ""); //$NON-NLS-1$
//                if (serverUrl.length() == 0) {
//                    GPDialogs.infoDialog(context, getString(R.string.error_set_cloud_settings), null);
//                    return;
//                }
//
//                Intent webExportIntent = new Intent(ExportActivity.this, WebDataUploadListActivity.class);
//                webExportIntent.putExtra(LibraryConstants.PREFS_KEY_URL, serverUrl);
//                webExportIntent.putExtra(LibraryConstants.PREFS_KEY_USER, user);
//                webExportIntent.putExtra(LibraryConstants.PREFS_KEY_PWD, pwd);
//                List<SpatialiteMap> spatialiteMaps = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps();
//                List<String> databases = new ArrayList<String>();
//                for (int i = 0; i < spatialiteMaps.size(); i++) {
//                    String dbPath = spatialiteMaps.get(i).databasePath;
//                    if (!databases.contains(dbPath)) {
//                        databases.add(dbPath);
//                    }
//                }
//                webExportIntent.putExtra(DATABASE_ID, databases.toArray(new String[0]));
//                startActivity(webExportIntent);
//            }
//        });

        MenuLoader menuLoader = new MenuLoader(this, ExtensionPoints.MENU_EXPORT_PROVIDER);
        menuLoader.addListener(new PluginLoaderListener<MenuLoader>() {
            @Override
            public void pluginLoaded(MenuLoader loader) {
                addMenuEntries(loader.getEntries());
            }
        });
        menuLoader.connect();
    }

    protected void addMenuEntries(List<IMenuEntry> entries) {
        menuEntriesMap.clear();
        int code = START_REQUEST_CODE + 1;
        for (final eu.geopaparazzi.library.plugin.types.IMenuEntry entry : entries) {
            final Context context = this;

            Button button = new Button(context);
            LinearLayout.LayoutParams lp = StyleHelper.styleButton(this, button);
            button.setText(entry.getLabel());
            entry.setRequestCode(code);
            menuEntriesMap.put(code, entry);
            LinearLayout container = (LinearLayout) findViewById(R.id.scrollView);
            container.addView(button, lp);
            button.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    entry.onClick(ExportActivity.this);
                }
            });
        }
    }

    private void checkNfc() throws Exception {
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null) {
            if (Build.VERSION.SDK_INT >
                    Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mNfcAdapter.setBeamPushUrisCallback(this, this);
                File databaseFile = ResourcesManager.getInstance(this).getDatabaseFile();
                mFileUris[0] = Uri.fromFile(databaseFile);
            } else {
                mNfcAdapter = null;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);

    }


    @Override
    public Uri[] createBeamUris(NfcEvent nfcEvent) {
        GPLog.addLogEntry(this, "URI SENT: " + mFileUris[0]);
        return mFileUris;
    }

    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);

        // Check to see that the Activity started due to an Android Beam
        String action = getIntent().getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            GPLog.addLogEntry(this, "Incoming NFC event.");
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    void processIntent(Intent intent) {
        Uri beamUri = intent.getData();
        String path = beamUri.getPath();
        GPLog.addLogEntry(this, "Incoming URI path: " + path);
        if (TextUtils.equals(beamUri.getScheme(), "file") &&
                path.endsWith("gpap")) {
            System.out.println(path);
            File pathFile = new File(path);
            boolean exists = pathFile.exists();
            System.out.println(exists);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }
}
