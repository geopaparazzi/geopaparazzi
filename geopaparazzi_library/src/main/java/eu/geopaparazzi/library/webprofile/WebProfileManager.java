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
package eu.geopaparazzi.library.webprofile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.util.CompressionUtilities;
import eu.geopaparazzi.library.core.ResourcesManager;

/**
 * Singleton to handle profiles to/from cloud up- and download.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum WebProfileManager {
    /**
     * Singleton instance.
     */
    INSTANCE;

    public static String UPLOADPATH = "";

    /**
     * Uploads a project folder as zip to the given server via POST.
     *
     * @param context the {@link Context} to use.
     * @param server  the server to which to upload.
     * @param user    the username for authentication.
     * @param passwd  the password for authentication.
     * @return the return message.
     */
    public String uploadProfile(Context context, String server, String user, String passwd) {
        try {
            ResourcesManager resourcesManager = ResourcesManager.getInstance(context);
            File databaseFile = resourcesManager.getDatabaseFile();

            server = addActionPath(server, UPLOADPATH);
            String result = NetworkUtilities.sendFilePost(context, server, databaseFile, user, passwd);
            if (GPLog.LOG) {
                GPLog.addLogEntry(this, result);
            }
            return result;
        } catch (Exception e) {
            GPLog.error(this, null, e);
            return e.getLocalizedMessage();
        }
    }

    private String addActionPath(String server, String path) {
        if (server.endsWith("/")) {
            return server + path;
        } else {
            return server + "/" + path;
        }
    }


    /**
     * Downloads the profile list from the given server via GET.
     *
     * @param context the {@link Context} to use.
     * @param server  the server from which to download.
     * @param user    the username for authentication.
     * @param passwd  the password for authentication.
     * @return the profile list.
     * @throws Exception if something goes wrong.
     */
    public List<Profile> downloadProfileList(Context context, String server, String user, String passwd) throws Exception {
        String jsonString = "[]";
        if (server.equals("test")) {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("tags/profiletest.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            jsonString = sb.toString();
        } else {
//            server = addActionPath(server, DOWNLOADLISTPATH);
            jsonString = NetworkUtilities.sendGetRequest(server, null, user, passwd);
        }
        List<Profile> webprofilesList = json2WebprofilesList(jsonString);
        return webprofilesList;
    }

    /**
     * Transform a json string to a list of profiles.
     *
     * @param json the json string.
     * @return the list of {@link Profile}.
     * @throws Exception if something goes wrong.
     */
    public static List<Profile> json2WebprofilesList(String json) throws Exception {
        List<Profile> wpList = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(json);
        JSONArray profilesArray = jsonObject.getJSONArray(ProfilesHandler.PROFILES);
        int profileNum = profilesArray.length();
        for (int i = 0; i < profileNum; i++) {
            JSONObject profileObject = profilesArray.getJSONObject(i);
            Profile profile = ProfilesHandler.INSTANCE.getProfileFromJson(profileObject);
            wpList.add(profile);
        }
        return wpList;
    }

}
