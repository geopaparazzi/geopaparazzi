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
package eu.geopaparazzi.library.webproject;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.CompressionUtilities;

/**
 * Singleton to handle cloud up- and download.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum WebDataManager {
    /**
     * Singleton instance.
     */
    INSTANCE;

    /**
     * The relative path appended to the server url to compose the get layers info url.
     */
    public static String GET_LAYERS_INFO = "sync/layerinfo/";

    /**
     * The relative path appended to the server url to compose the download data url.
     */
    public static String DOWNLOAD_DATA = "sync/download/";

    public static String UPLOAD_DATA = "sync/upload/";

    public static String LOGIN_URL = "auth/login_user/";

    /**
     * Uploads a project folder as zip to the given server via POST.
     *
     * @param context the {@link Context} to use.
     * @param fileToUpload  the file to upload.
     * @param server  the server to which to upload.
     * @param user    the username for authentication.
     * @param passwd  the password for authentication.
     * @return the return message.
     */
    public String uploadData(Context context, File fileToUpload, String server, String user, String passwd) {
        try {

            String loginUrl = addActionPath(server, LOGIN_URL);
            server = addActionPath(server, UPLOAD_DATA);
            String result = NetworkUtilities.sendFilePost(context, server, fileToUpload, user, passwd, loginUrl);
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
     * Downloads a project from the given server via GET.
     *
     * @param context the {@link Context} to use.
     * @param server  the server from which to download.
     * @param user    the username for authentication.
     * @param passwd  the password for authentication.
     * @return the error message or null.
     */
    public String downloadData(Context context, String server, String user, String passwd, String postJson, String outputFileName) {
        String downloadedProjectFileName = "no information available";
        try {
            ResourcesManager resourcesManager = ResourcesManager.getInstance(context);
            File sdcardDir = resourcesManager.getSdcardDir();
            File downloadeddataFile = new File(sdcardDir, outputFileName);
            if (downloadeddataFile.exists()) {
                String wontOverwrite = context.getString(R.string.the_file_exists_wont_overwrite) + " " + downloadeddataFile.getName();
                return wontOverwrite;
            }
            String loginUrl = addActionPath(server, LOGIN_URL);
            server = addActionPath(server, DOWNLOAD_DATA);
            NetworkUtilities.sendPostForFile(context, server, postJson, user, passwd, downloadeddataFile, loginUrl);

            long fileLength = downloadeddataFile.length();
            if (fileLength == 0) {
                throw new RuntimeException("Error in downloading file.");
            }

            return null;
        } catch (Exception e) {
            GPLog.error(this, null, e);
            String message = e.getMessage();
            if (message.equals(CompressionUtilities.FILE_EXISTS)) {
                throw new RuntimeException(context.getString(R.string.the_file_exists_wont_overwrite) + " " + downloadedProjectFileName);
            }
            return e.getLocalizedMessage();
        }
    }

    /**
     * Downloads the data layers list from the given server via GET.
     *
     * @param context the {@link Context} to use.
     * @param server  the server from which to download.
     * @param user    the username for authentication.
     * @param passwd  the password for authentication.
     * @return the project list.
     * @throws Exception if something goes wrong.
     */
    public List<WebDataLayer> downloadDataLayersList(Context context, String server, String user, String passwd) throws Exception {
        String jsonString = "[]";
        if (server.equals("test")) {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("tags/cloudtest.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            jsonString = sb.toString();
        } else {
            String loginUrl = addActionPath(server, LOGIN_URL);
            server = addActionPath(server, GET_LAYERS_INFO);
            jsonString = NetworkUtilities.sendGetRequest(server, null, user, passwd, loginUrl);
        }
        List<WebDataLayer> webDataList = json2WebDataList(jsonString);
        return webDataList;
    }

    /**
     * Transform a json string to a list of WebDataLayer.
     *
     * @param json the json string.
     * @return the list of {@link WebDataLayer}.
     * @throws Exception if something goes wrong.
     */
    public static List<WebDataLayer> json2WebDataList(String json) throws Exception {
        List<WebDataLayer> webDataList = new ArrayList<>();

        JSONArray projectsArray = new JSONArray(json);
        int projectNum = projectsArray.length();
        for (int i = 0; i < projectNum; i++) {
            JSONObject projectObject = projectsArray.getJSONObject(i);
            String name = projectObject.getString("name");
            String title = projectObject.getString("title");
            String abstractStr = projectObject.getString("abstract");
            String geomtype = projectObject.getString("geomtype");
            int srid = projectObject.getInt("srid");
            String permissions = projectObject.getString("permissions");
            Long lastEdited = null;
            if (projectObject.has("last-modified")) {
                 lastEdited = projectObject.getLong("last-modified");
            }

            WebDataLayer wdl = new WebDataLayer();
            wdl.name = name;
            wdl.title = title;
            wdl.abstractStr = abstractStr;
            wdl.geomtype = geomtype;
            wdl.srid = srid;
            wdl.permissions = permissions;
            wdl.lastEdited = lastEdited;
            webDataList.add(wdl);
        }
        return webDataList;
    }

}
