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
package eu.hydrologis.geopaparazzi.mixare;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Bookmark;
import eu.hydrologis.geopaparazzi.util.FileUtils;
import eu.hydrologis.geopaparazzi.util.Note;

/**
 * Utilities for interaction with the Mixare project. 
 * 
 * <p>
 * A mixare json object looks like:
 * <pre>
 * {
 *     "id": "2827",
 *     "lat": "46.43893",
 *     "lng": "11.21706",
 *     "elevation": "1737",
 *     "title": "Penegal",
 *     "distance": "9.756",
 *     "has_detail_page": "1",
 *     "webpage": "http%3A%2F%2Fwww.suedtirolerland.it%2Fapi%2Fmap%2FgetMarkerTplM%2F%3Fmarker_id%3D2827%26project_id%3D15%26lang_id%3D9"
 * }
 * </pre>
 * 
 * <p>With everything optional but lat, lng, elevation and title.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class MixareHandler {

    /**
     * Show the points in the supplied region in Mixare.
     * 
     * @param context
     * @param n
     * @param s
     * @param w
     * @param e
     * @throws Exception
     */
    public void runRegionOnMixare( Context context, float n, float s, float w, float e ) throws Exception {

        String mixareJson = generateMixareDataFromViewPort(context, n, s, w, e);

        File geoPaparazziDir = ApplicationManager.getInstance(context).getGeoPaparazziDir();
        File mixarefile = new File(geoPaparazziDir, "mixare.json");
        FileUtils.writefile(mixareJson, mixarefile);
        String mixareUri = "file://" + mixarefile.getAbsolutePath();

        Intent i = new Intent();
        i.setAction(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse(mixareUri), "application/mixare-json");
        context.startActivity(i);
    }

    /**
     * Generate the json string needed by mixare.
     * 
     * <p>This is done via strign concatenation since at some point this 
     * could go directly to files in stream to avoid memory problems.
     * 
     * @param context
     * @param n
     * @param s
     * @param w
     * @param e
     * @return
     * @throws Exception
     */
    private String generateMixareDataFromViewPort( Context context, float n, float s, float w, float e ) throws Exception {
        int id = 0;
        StringBuilder sb = new StringBuilder();

        List<Bookmark> bookmarksList = DaoBookmarks.getBookmarksInWorldBounds(context, n, s, w, e);
        for( Bookmark bookmark : bookmarksList ) {
            sb.append(",{\n");
            double lat = bookmark.getLat();
            double lon = bookmark.getLon();
            String title = bookmark.getName();
            String json = dataToString(id++, lat, lon, 0, title);
            sb.append(json);
            sb.append("}\n");
        }

        List<Note> notesList = DaoNotes.getNotesInWorldBounds(context, n, s, w, e);
        for( Note note : notesList ) {
            sb.append(",{\n");
            double lat = note.getLat();
            double lon = note.getLon();
            double elevation = note.getAltim();
            String title = note.getDescription(); // note.getName() + " (" + note.getDescription() + ")";
            String json = dataToString(id++, lat, lon, elevation, title);
            sb.append(json);
            sb.append("}\n");
        }

        String jsonString = sb.substring(1);

        StringBuilder finalSb = new StringBuilder();
        finalSb.append("{\n");
        finalSb.append("\"status\": \"OK\",\n");
        finalSb.append("\"num_results\":").append(id).append(",\n");
        finalSb.append("\"results\": [\n");
        finalSb.append(jsonString);
        finalSb.append("]\n");
        finalSb.append("}\n");
        return finalSb.toString();
    }

    private String dataToString( int id, double lat, double lon, double elev, String title ) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"id\": \"").append(id).append("\",").append("\n");
        sb.append("\"lat\": \"").append(lat).append("\",").append("\n");
        sb.append("\"lng\": \"").append(lon).append("\",").append("\n");
        sb.append("\"elevation\": \"").append(0).append("\",").append("\n");
        sb.append("\"title\": \"").append(title).append("\"").append("\n");
        return sb.toString();
    }

    /**
     * Checks if Mixare is installed.
     * 
     * @param context
     * @return <code>true</code> if mixare is installed.
     */
    public boolean isMixareInstalled( Context context ) {
        // We try to locate mixare on the phone
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo("org.mixare", 0);
            if (pi.versionCode >= 1) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Lanches an intent that guides to the mixare installation.
     * 
     * @param context
     */
    public void installMixareFromMarket( Context context ) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_VIEW);
        i.setData(Uri.parse("market://search?q=pname:org.mixare"));
        context.startActivity(i);
    }
}
