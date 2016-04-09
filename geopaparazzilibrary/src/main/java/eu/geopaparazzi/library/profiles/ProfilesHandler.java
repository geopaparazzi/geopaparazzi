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

package eu.geopaparazzi.library.profiles;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrea Antonello
 */
public enum ProfilesHandler {
    INSTANCE;

    public static final String KEY_PROFILES_PREFERENCES = "KEY_PROFILES_PREFERENCES";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String CREATIONDATE = "creationdate";
    public static final String COLOR = "color";
    public static final String ACTIVE = "active";
    public static final String TAGS_PATH = "tagsPath";
    public static final String BASEMAPS = "basemaps";
    public static final String PATH = "path";
    public static final String SPATIALITE = "spatialitedbs";
    public static final String PROFILES = "profiles";

    public static final String[] CONTENT_PROVIDER_FIELDS = new String[]{"name", "isactive", "json"};
    public static final String AUTHORITY = "eu.geopaparazzi.provider.profiles";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String PROFILE = "profile";
    public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PROFILE).build();

    /**
     * Get the list of stored profiles from the preferences.
     *
     * @param preferences the preferences object.
     * @return the list of Profile objects.
     * @throws JSONException
     */
    public List<Profile> getProfilesFromPreferences(SharedPreferences preferences) throws JSONException {
        String profilesJson = preferences.getString(KEY_PROFILES_PREFERENCES, "");
        return getProfilesFromJson(profilesJson);
    }

    /**
     * Save the list of profiles to preferences.
     *
     * @param preferences the preferences object.
     * @return the list of Profile objects.
     * @throws JSONException
     */
    public void saveProfilesToPreferences(SharedPreferences preferences, List<Profile> profilesList) throws JSONException {
        String jsonProfilesString = getJsonFromProfilesList(profilesList, false);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_PROFILES_PREFERENCES, jsonProfilesString);
        editor.apply();
    }

    /**
     * Get the list of profiles from a json text.
     *
     * @param profilesJson the json text.
     * @return the list of Profile objects.
     * @throws JSONException
     */
    @NonNull
    public List<Profile> getProfilesFromJson(String profilesJson) throws JSONException {
        List<Profile> profilesList = new ArrayList<>();

        if (profilesJson.trim().length() == 0)
            return profilesList;

        JSONObject root = new JSONObject(profilesJson);
        JSONArray profilesArray = root.getJSONArray(PROFILES);
        for (int i = 0; i < profilesArray.length(); i++) {
            JSONObject profileObject = profilesArray.getJSONObject(i);
            Profile profile = new Profile();
            if (profileObject.has(NAME)) {
                profile.name = profileObject.getString(NAME);
            }
            if (profileObject.has(DESCRIPTION)) {
                profile.description = profileObject.getString(DESCRIPTION);
            }
            if (profileObject.has(CREATIONDATE)) {
                profile.creationdate = profileObject.getString(CREATIONDATE);
            }
            if (profileObject.has(COLOR)) {
                profile.color = profileObject.getString(COLOR);
            }
            if (profileObject.has(ACTIVE)) {
                profile.active = profileObject.getBoolean(ACTIVE);
            }
            if (profileObject.has(TAGS_PATH)) {
                profile.tagsPath = profileObject.getString(TAGS_PATH);
            }
            if (profileObject.has(BASEMAPS)) {
                JSONArray basemapsArray = profileObject.getJSONArray(BASEMAPS);
                for (int j = 0; j < basemapsArray.length(); j++) {
                    JSONObject baseMapObject = basemapsArray.getJSONObject(j);
                    if (baseMapObject.has(PATH)) {
                        String path = baseMapObject.getString(PATH);
                        profile.basemapsList.add(path);
                    }
                }
            }
            if (profileObject.has(SPATIALITE)) {
                JSONArray spatialiteDbsArray = profileObject.getJSONArray(SPATIALITE);
                for (int j = 0; j < spatialiteDbsArray.length(); j++) {
                    JSONObject spatialiteDbsObject = spatialiteDbsArray.getJSONObject(j);
                    if (spatialiteDbsObject.has(PATH)) {
                        String path = spatialiteDbsObject.getString(PATH);
                        profile.spatialiteList.add(path);
                    }
                }
            }
            profilesList.add(profile);
        }
        return profilesList;
    }


    /**
     * Get the json text from a list of profiles.
     *
     * @param profilesList the list of profiles.
     * @param indent       optional json indenting flag.
     * @return the json string.
     * @throws JSONException
     */
    public String getJsonFromProfilesList(List<Profile> profilesList, boolean indent) throws JSONException {
        JSONObject root = new JSONObject();
        JSONArray profilesArray = new JSONArray();
        root.put(PROFILES, profilesArray);

        for (Profile profile : profilesList) {
            JSONObject profileObject = getJsonFromProfile(profile);
            profilesArray.put(profileObject);
        }
        if (indent)
            return root.toString(2);
        else
            return root.toString();

    }

    public JSONObject getJsonFromProfile(Profile profile) throws JSONException {
        JSONObject profileObject = new JSONObject();
        profileObject.put(NAME, profile.name);
        profileObject.put(DESCRIPTION, profile.description);
        profileObject.put(CREATIONDATE, profile.creationdate);
        profileObject.put(COLOR, profile.color);
        profileObject.put(ACTIVE, profile.active);
        profileObject.put(TAGS_PATH, profile.tagsPath);

        if (profile.basemapsList.size() > 0) {
            JSONArray basemapsArray = new JSONArray();
            for (int j = 0; j < profile.basemapsList.size(); j++) {
                JSONObject baseMapObject = new JSONObject();
                baseMapObject.put(PATH, profile.basemapsList.get(j));
                basemapsArray.put(j, baseMapObject);
            }
            profileObject.put(BASEMAPS, basemapsArray);
        }

        if (profile.spatialiteList.size() > 0) {
            JSONArray spatialiteDbsArray = new JSONArray();
            for (int j = 0; j < profile.spatialiteList.size(); j++) {
                JSONObject spatialiteDbObject = new JSONObject();
                spatialiteDbObject.put(PATH, profile.spatialiteList.get(j));
                spatialiteDbsArray.put(spatialiteDbObject);
            }
            profileObject.put(SPATIALITE, spatialiteDbsArray);
        }
        return profileObject;
    }


    public Profile getActiveProfile(ContentResolver contentResolver) {

        String[] projection = CONTENT_PROVIDER_FIELDS;
        Cursor cursor = contentResolver.query(CONTENT_URI,
                projection,
                null,
                null,
                null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                boolean active = cursor.getInt(1) != 0;
                if (active) {
                    String json = cursor.getString(2);
                    ProfilesHandler.INSTANCE.
                }
            } while (cursor.moveToNext());
        }

        return null;
    }
}
