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
package eu.hydrologis.geopaparazzi.osm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.CompressionUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.NoteType;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Note;

/**
 * Utilities class for handling OSM matters.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class OsmUtilities {
    private static final String TEST = "test";
    /**
     * Server url for data upload.
     */
    private static final String SERVER = "http://lucadelu.org/cgi-bin/zoo_loader.cgi";

    /**
     * 
     */
    public static final String PREF_KEY_USER = "osm_user_key";
    /**
     * 
     */
    public static final String PREF_KEY_PWD = "osm_pwd_key";
    /**
     * 
     */
    public static final String PREF_KEY_SERVER = "osm_server_key";

    /**
     * Response message for an error in the json string.
     */
    public static final String ERROR_JSON = "error_json";
    /**
     * Response message for an error in the osm login or the osm server.
     */
    public static final String ERROR_OSM = "error_osm";
    /**
     * Response message for successful upload to OSM.
     */
    public static final String FEATURES_IMPORTED = "features_imported";

    /**
     * Send OSM notes to the server.
     * 
     * @param context the context.
     * @param description the changeset description.
     * @return the server response.
     * @throws Exception  if something goes wrong.
     */
    public static String sendOsmNotes( Context context, String description ) throws Exception {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String user = preferences.getString(PREF_KEY_USER, TEST);
        if (user.length() == 0) {
            user = TEST;
            Editor editor = preferences.edit();
            editor.putString(PREF_KEY_USER, user);
            editor.commit();
        }

        String pwd = preferences.getString(PREF_KEY_PWD, TEST);
        if (pwd.length() == 0) {
            pwd = TEST;
            Editor editor = preferences.edit();
            editor.putString(PREF_KEY_PWD, pwd);
            editor.commit();
        }

        String serverUrl = preferences.getString(PREF_KEY_SERVER, SERVER);
        if (serverUrl.length() == 0) {
            serverUrl = SERVER;
            Editor editor = preferences.edit();
            editor.putString(PREF_KEY_SERVER, serverUrl);
            editor.commit();
        }

        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open("tags/osm_wps.xml");
        String wpsXmlString = readInputStreamToString(inputStream);

        wpsXmlString = wpsXmlString.replaceFirst("USERNAME", user);
        wpsXmlString = wpsXmlString.replaceFirst("PASSWORD", pwd);
        wpsXmlString = wpsXmlString.replaceFirst("CHANGESET", description);

        List<Note> notesList = DaoNotes.getNotesList();
        StringBuilder sb = new StringBuilder();
        for( Note note : notesList ) {
            if (note.getType() == NoteType.OSM.getTypeNum()) {
                String form = note.getForm();
                if (form != null) {
                    sb.append(",\n");
                    sb.append(form);
                }
            }
        }
        String notesString = sb.toString();
        notesString = notesString.substring(1);
        sb = new StringBuilder();
        sb.append("[");
        sb.append(notesString);
        sb.append("]");

        String json = sb.toString();
        // json = json.substring(1);

        wpsXmlString = wpsXmlString.replaceFirst("JSON", json);
        if (GPLog.LOG)
            GPLog.addLogEntry("OSMUTILITIES", "WPSXML SENT: " + wpsXmlString);

        String response = NetworkUtilities.sendPost(context, serverUrl, wpsXmlString, null, null, true);
        if (GPLog.LOG)
            GPLog.addLogEntry("OSMUTILITIES", "RESPONSE FROM SERVER:" + response);
        return response;
    }

    /**
     * Read from an inputstream and convert the read stuff to a String. Useful for text files
     * that are available as streams.
     * 
     * @param inputStream the input stream.
     * @return the read string 
     * @throws IOException  if something goes wrong.
     */
    public static String readInputStreamToString( InputStream inputStream ) throws IOException {
        // Create the byte list to hold the data
        List<Byte> bytesList = new ArrayList<Byte>();

        byte b = 0;
        while( (b = (byte) inputStream.read()) != -1 ) {
            bytesList.add(b);
        }
        // Close the input stream and return bytes
        inputStream.close();

        byte[] bArray = new byte[bytesList.size()];
        for( int i = 0; i < bArray.length; i++ ) {
            bArray[i] = bytesList.get(i);
        }

        String file = new String(bArray);
        return file;
    }

    private static final String osmTagsZipUrlPath = "http://geopaparazzi.googlecode.com/files/osmtags.zip";
    private static final String osmTagsVersionUrlPath = "http://geopaparazzi.googlecode.com/git/extras/osmtags/VERSION";

    /**
     * Download the osm tags archive if necessary and if network is available.
     * 
     * @param activity parent activity.
     */
    public static void handleOsmTagsDownload( final Activity activity ) {

        if (!NetworkUtilities.isNetworkAvailable(activity)) {
            // Utilities
            // .messageDialog(activity,
            // "It is possible to download OSM tags only with an active internet connection", null);
            return;
        }

        boolean doTagsDownload = false;
        final int[] onlineVersion = new int[]{0};
        try {
            String versionString = NetworkUtilities.readUrl(osmTagsVersionUrlPath);
            onlineVersion[0] = Integer.parseInt(versionString);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
            int currentOsmVersion = preferences.getInt(Constants.PREFS_KEY_OSMTAGSVERSION, -1);

            if (currentOsmVersion < onlineVersion[0]) {
                doTagsDownload = true;
            }

        } catch (Exception e2) {
            e2.printStackTrace();
        }

        try {
            String[] tagCategories = OsmTagsManager.getInstance().getTagCategories(activity);
            if (tagCategories != null && !doTagsDownload) {
                return;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        new AlertDialog.Builder(activity).setTitle("OSM tags")
                .setMessage("Do you want to download the OSM tags of version " + onlineVersion[0] + "?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                        // ignore
                    }
                }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                    private File parentFile;

                    public void onClick( DialogInterface dialog, int whichButton ) {
                        try {
                            parentFile = ResourcesManager.getInstance(activity).getApplicationDir().getParentFile();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        final File osmZipFile = new File(parentFile, "osmtags.zip");
                        File osmFolderFile = new File(parentFile, "osmtags");

                        if (osmFolderFile.exists() && osmFolderFile.isDirectory()) {
                            boolean deleteFileOrDir = FileUtilities.deleteFileOrDir(osmFolderFile);
                            if (!deleteFileOrDir) {
                                Utilities
                                        .messageDialog(
                                                activity,
                                                "An osm tags folder already exists and it was not possible to remove it. Please remove the folder manually before downloading the new tags.",
                                                null);
                                return;
                            }
                        }

                        if (!NetworkUtilities.isNetworkAvailable(activity)) {
                            Utilities.messageDialog(activity, activity.getString(R.string.available_only_with_network), null);
                            return;
                        }

                        final ProgressDialog progressDialog = ProgressDialog.show(activity, "",
                                activity.getString(R.string.loading_data));

                        new AsyncTask<String, Void, String>(){
                            protected String doInBackground( String... params ) {

                                try {
                                    NetworkUtilities.sendGetRequest4File(osmTagsZipUrlPath, osmZipFile, null, null, null);
                                } catch (Exception e) {
                                    Utilities.messageDialog(activity, "An error occurred while downloading the OSM tags.", null);
                                    e.printStackTrace();
                                    return "";
                                }

                                try {
                                    CompressionUtilities.unzipFolder(osmZipFile.getAbsolutePath(), parentFile.getAbsolutePath(),
                                            true);

                                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                                    Editor editor = preferences.edit();
                                    editor.putInt(Constants.PREFS_KEY_OSMTAGSVERSION, onlineVersion[0]);
                                    editor.commit();
                                } catch (IOException e) {
                                    Utilities.messageDialog(activity,
                                            "An error occurred while unzipping the OSM tags to the device.", null);
                                    e.printStackTrace();
                                    return "";
                                } finally {
                                    osmZipFile.delete();
                                }
                                return "";
                            }

                            protected void onPostExecute( String dataset ) {
                                progressDialog.dismiss();
                            }
                        }.execute((String) null);

                    }
                }).show();

    }

}
