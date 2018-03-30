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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.CompressionUtilities;
import eu.geopaparazzi.library.core.ResourcesManager;

/**
 * Singleton to handle projects to/from cloud up- and download.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum WebProjectManager {
    /**
     * Singleton instance.
     */
    INSTANCE;

    /**
     * The relative relativePath appended to the server url to compose the upload url.
     */
    public static String UPLOADPATH = "stage_gpproject_upload";

    /**
     * The relative relativePath appended to the server url to compose the download projects list url.
     */
    public static String DOWNLOADLISTPATH = "stage_gplist_download";

    public static String DOWNLOADPROJECTPATH = "stage_gpproject_download";

    /**
     * The id parameter name to use in the server url.
     */
    public static String ID = "id";

    /**
     * Uploads a project folder as zip to the given server via POST.
     *
     * @param context the {@link Context} to use.
     * @param server  the server to which to upload.
     * @param user    the username for authentication.
     * @param passwd  the password for authentication.
     * @return the return message.
     */
    public String uploadProject(Context context, String server, String user, String passwd) {
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
     * Downloads a project from the given server via GET.
     *
     * @param context    the {@link Context} to use.
     * @param server     the server from which to download.
     * @param user       the username for authentication.
     * @param passwd     the password for authentication.
     * @param webproject the project to download.
     * @return the return code.
     */
    public String downloadProject(Context context, String server, String user, String passwd, Webproject webproject) {
        String downloadedProjectFileName = "no information available";
        try {
            ResourcesManager resourcesManager = ResourcesManager.getInstance(context);
            File sdcardDir = resourcesManager.getMainStorageDir();
            File downloadedProjectFile = new File(sdcardDir, webproject.id);
            if (downloadedProjectFile.exists()) {
                String wontOverwrite = context.getString(R.string.the_file_exists_wont_overwrite) + " " + downloadedProjectFile.getName();
                return wontOverwrite;
            }
            server = addActionPath(server, DOWNLOADPROJECTPATH);
            NetworkUtilities.sendGetRequest4File(server, downloadedProjectFile, "id=" + webproject.id, user, passwd);

            return context.getString(R.string.project_successfully_downloaded);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            String message = e.getMessage();
            if (message.equals(CompressionUtilities.FILE_EXISTS)) {
                String wontOverwrite = context.getString(R.string.the_file_exists_wont_overwrite) + " " + downloadedProjectFileName;
                return wontOverwrite;
            }
            return e.getLocalizedMessage();
        }
    }

    /**
     * Downloads the project list from the given server via GET.
     *
     * @param context the {@link Context} to use.
     * @param server  the server from which to download.
     * @param user    the username for authentication.
     * @param passwd  the password for authentication.
     * @return the project list.
     * @throws Exception if something goes wrong.
     */
    public List<Webproject> downloadProjectList(Context context, String server, String user, String passwd) throws Exception {
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
            server = addActionPath(server, DOWNLOADLISTPATH);
            jsonString = NetworkUtilities.sendGetRequest(server, null, user, passwd);
        }
        List<Webproject> webprojectsList = json2WebprojectsList(jsonString);
        return webprojectsList;
    }

    /**
     * Transform a json string to a list of webprojects.
     *
     * @param json the json string.
     * @return the list of {@link Webproject}.
     * @throws Exception if something goes wrong.
     */
    public static List<Webproject> json2WebprojectsList(String json) throws Exception {
        List<Webproject> wpList = new ArrayList<>();

        JSONObject jsonObject = new JSONObject(json);
        JSONArray projectsArray = jsonObject.getJSONArray("projects");
        int projectNum = projectsArray.length();
        for (int i = 0; i < projectNum; i++) {
            JSONObject projectObject = projectsArray.getJSONObject(i);
            String id = projectObject.getString("id");
            String title = projectObject.getString("title");
            String date = projectObject.getString("date");
            String author = projectObject.getString("author");
            String name = projectObject.getString("name");
            String size = projectObject.getString("size");

            Webproject wp = new Webproject();
            wp.author = author;
            wp.date = date;
            wp.name = name;
            wp.title = title;
            wp.id = id;
            try {
                wp.size = Long.parseLong(size);
            } catch (Exception e) {
                // unused for now
            }
            wpList.add(wp);
        }
        return wpList;
    }

}
