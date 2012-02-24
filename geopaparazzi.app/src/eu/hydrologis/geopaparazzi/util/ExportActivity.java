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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import eu.geopaparazzi.library.gpx.GpxExport;
import eu.geopaparazzi.library.gpx.GpxRepresenter;
import eu.geopaparazzi.library.kml.KmlRepresenter;
import eu.geopaparazzi.library.kml.KmzExport;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.geopaparazzi.library.webproject.ReturnCodes;
import eu.geopaparazzi.library.webproject.WebProjectManager;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;

/**
 * Activity for export tasks.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ExportActivity extends Activity {

    public static final String PREF_KEY_USER = "geopapcloud_user_key"; //$NON-NLS-1$
    public static final String PREF_KEY_PWD = "geopapcloud_pwd_key"; //$NON-NLS-1$
    public static final String PREF_KEY_SERVER = "geopapcloud_server_key";//$NON-NLS-1$

    private ProgressDialog kmlProgressDialog;
    private ProgressDialog gpxProgressDialog;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.export);

        ImageButton kmzExportButton = (ImageButton) findViewById(R.id.kmzExportButton);
        kmzExportButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                exportKmz();
            }
        });
        ImageButton gpxExportButton = (ImageButton) findViewById(R.id.gpxExportButton);
        gpxExportButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                exportGpx();
            }
        });
        ImageButton cloudExportButton = (ImageButton) findViewById(R.id.cloudExportButton);
        cloudExportButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                ExportActivity context = ExportActivity.this;
                if (!NetworkUtilities.isNetworkAvailable(context)) {
                    Utilities.messageDialog(context, context.getString(R.string.available_only_with_network), null);
                    return;
                }

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                String user = preferences.getString(PREF_KEY_USER, ""); //$NON-NLS-1$
                String pwd = preferences.getString(PREF_KEY_PWD, ""); //$NON-NLS-1$
                String serverUrl = preferences.getString(PREF_KEY_SERVER, ""); //$NON-NLS-1$

                if (user.length() == 0 || pwd.length() == 0 || serverUrl.length() == 0) {
                    Utilities.messageDialog(context,
                            "The geopap-cloud preferences are not set properly. Please check your settings.", null);
                    return;
                }

                boolean addMedia = false;
                ReturnCodes returnCode = WebProjectManager.INSTANCE.uploadProject(context, addMedia, serverUrl, user, pwd);
                if (returnCode == ReturnCodes.ERROR) {
                    Utilities.messageDialog(context, "An error occurred while uploading the project to the Geopap-cloud", null);
                } else {
                    Utilities.messageDialog(context, "The project has been succesfully uploaded to the Geopap-cloud", null);
                }
            }
        });
    }

    private void exportKmz() {
        kmlProgressDialog = ProgressDialog.show(ExportActivity.this, getString(R.string.geopaparazziactivity_exporting_kmz),
                "", true, true); //$NON-NLS-1$
        new AsyncTask<String, Void, String>(){
            protected String doInBackground( String... params ) {
                try {
                    List<KmlRepresenter> kmlRepresenterList = new ArrayList<KmlRepresenter>();
                    /*
                     * add gps logs
                     */
                    HashMap<Long, Line> linesMap = DaoGpsLog.getLinesMap(ExportActivity.this);
                    Collection<Line> linesCollection = linesMap.values();
                    for( Line line : linesCollection ) {
                        kmlRepresenterList.add(line);
                    }
                    /*
                     * get notes
                     */
                    List<Note> notesList = DaoNotes.getNotesList(ExportActivity.this);
                    for( Note note : notesList ) {
                        kmlRepresenterList.add(note);
                    }
                    /*
                     * add pictures
                     */
                    List<Image> imagesList = DaoImages.getImagesList(ExportActivity.this);
                    for( Image image : imagesList ) {
                        kmlRepresenterList.add(image);
                    }

                    File kmlExportDir = ResourcesManager.getInstance(ExportActivity.this).getExportDir();
                    String filename = "geopaparazzi_" + LibraryConstants.TIMESTAMPFORMATTER.format(new Date()) + ".kmz"; //$NON-NLS-1$ //$NON-NLS-2$
                    File kmlOutputFile = new File(kmlExportDir, filename);
                    KmzExport export = new KmzExport(null, kmlOutputFile);
                    export.export(ExportActivity.this, kmlRepresenterList);

                    return kmlOutputFile.getAbsolutePath();
                } catch (IOException e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    return null;
                }
            }

            protected void onPostExecute( String response ) { // on UI thread!
                kmlProgressDialog.dismiss();
                String msg = ""; //$NON-NLS-1$
                if (response.length() > 0) {
                    msg = getString(R.string.kmlsaved) + response;
                } else {
                    msg = getString(R.string.kmlnonsaved);
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(ExportActivity.this);
                builder.setMessage(msg).setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                            public void onClick( DialogInterface dialog, int id ) {
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        }.execute((String) null);
    }

    private void exportGpx() {
        gpxProgressDialog = ProgressDialog.show(ExportActivity.this, getString(R.string.geopaparazziactivity_exporting_kmz),
                "", true, true); //$NON-NLS-1$
        new AsyncTask<String, Void, String>(){
            protected String doInBackground( String... params ) {
                try {
                    List<GpxRepresenter> gpxRepresenterList = new ArrayList<GpxRepresenter>();
                    /*
                     * add gps logs
                     */
                    HashMap<Long, Line> linesMap = DaoGpsLog.getLinesMap(ExportActivity.this);
                    Collection<Line> linesCollection = linesMap.values();
                    for( Line line : linesCollection ) {
                        gpxRepresenterList.add(line);
                    }
                    /*
                     * get notes
                     */
                    List<Note> notesList = DaoNotes.getNotesList(ExportActivity.this);
                    for( Note note : notesList ) {
                        gpxRepresenterList.add(note);
                    }

                    File gpxExportDir = ResourcesManager.getInstance(ExportActivity.this).getExportDir();
                    String filename = "geopaparazzi_" + LibraryConstants.TIMESTAMPFORMATTER.format(new Date()) + ".gpx"; //$NON-NLS-1$ //$NON-NLS-2$
                    File gpxOutputFile = new File(gpxExportDir, filename);
                    GpxExport export = new GpxExport(null, gpxOutputFile);
                    export.export(ExportActivity.this, gpxRepresenterList);

                    return gpxOutputFile.getAbsolutePath();
                } catch (IOException e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    return null;
                }
            }

            protected void onPostExecute( String response ) { // on UI thread!
                gpxProgressDialog.dismiss();
                String msg = ""; //$NON-NLS-1$
                if (response.length() > 0) {
                    msg = getString(R.string.kmlsaved) + response;
                } else {
                    msg = getString(R.string.kmlnonsaved);
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(ExportActivity.this);
                builder.setMessage(msg).setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                            public void onClick( DialogInterface dialog, int id ) {
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        }.execute((String) null);
    }

}
