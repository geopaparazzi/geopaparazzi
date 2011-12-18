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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.util.NetworkUtilities;
import eu.hydrologis.geopaparazzi.util.Note;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;

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

    public static final String PREF_KEY_USER = "osm_user_key";
    public static final String PREF_KEY_PWD = "osm_pwd_key";
    public static final String PREF_KEY_SERVER = "osm_server_key";

    /**
     * Send OSM notes to the server.
     * 
     * @param context the context.
     * @throws Exception
     */
    public static void sendOsmNotes( Context context ) throws Exception {
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

        List<Note> notesList = DaoNotes.getNotesList(context);
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for( Note note : notesList ) {
            String form = note.getForm();
            if (form != null) {
                sb.append(",\n");
                sb.append(form);
            }
        }
        sb.append("]");

        String json = sb.toString();
        json = json.substring(1);

        wpsXmlString = wpsXmlString.replaceFirst("JSON", json);

        String response = NetworkUtilities.sendPost(serverUrl, wpsXmlString, null, null);
        Logger.i("OSMUTILITIES", response);
    }

    /**
     * Read from an inputstream and convert the read stuff to a String. Useful for text files
     * that are available as streams.
     * 
     * @param inputStream
     * @return the read string
     * @throws IOException 
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
}
