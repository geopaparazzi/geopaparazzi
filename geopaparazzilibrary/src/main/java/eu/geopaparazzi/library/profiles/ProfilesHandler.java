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

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import eu.geopaparazzi.library.core.maps.BaseMap;
import eu.geopaparazzi.library.profiles.objects.ProfileBasemaps;
import eu.geopaparazzi.library.profiles.objects.ProfileOtherfiles;
import eu.geopaparazzi.library.profiles.objects.ProfileProjects;
import eu.geopaparazzi.library.profiles.objects.ProfileSpatialitemaps;
import eu.geopaparazzi.library.profiles.objects.ProfileTags;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.types.ESpatialDataSources;

/**
 * Profile handling related stuff.
 *
 * @author Andrea Antonello
 */
public enum ProfilesHandler {
    INSTANCE;

    public static final String KEY_PROFILES_PREFERENCES = "KEY_PROFILES_PREFERENCES";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String CREATIONDATE = "creationdate";
    public static final String MODIFIEDDATE = "modifieddate";
    public static final String PATH = "path";
    public static final String URL = "url";
    public static final String UPLOADURL = "uploadurl";
    public static final String SIZE = "size";
    public static final String VISIBLE = "visible";

    public static final String ACTIVE = "active";
    public static final String COLOR = "color";
    public static final String MAPVIEW = "mapView";
    public static final String TAGS = "tags";
    public static final String PROJECT = "project";
    public static final String SDCARD_PATH = "sdcardPath";
    public static final String BASEMAPS = "basemaps";
    public static final String SPATIALITE = "spatialitedbs";
    public static final String OTHERFILES = "otherfiles";
    public static final String PROFILES = "profiles";

    public static final String[] CONTENT_PROVIDER_FIELDS = new String[]{"name", "isactive", "json"};
    public static final String AUTHORITY = "eu.geopaparazzi.provider.profiles";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String PROFILE = "profile";
    public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PROFILE).build();
    public static final String MAINSTORAGE = "MAINSTORAGE";
    public static final String SECONDARYSTORAGE = "SECONDARYSTORAGE";

    /**
     * The active profile.
     */
    private Profile activeProfile;

    /**
     * Get the list of stored profiles from the preferences.
     *
     * @param preferences the preferences object.
     * @return the list of Profile objects.
     * @throws JSONException
     */
    public List<Profile> getProfilesFromPreferences(SharedPreferences preferences) throws JSONException {
        String profilesJson = preferences.getString(KEY_PROFILES_PREFERENCES, "");
        return getProfilesFromJson(profilesJson, false);
    }
    public List<Profile> getProfilesFromPreferences(SharedPreferences preferences, boolean bWebOnly) throws JSONException {
        String profilesJson = preferences.getString(KEY_PROFILES_PREFERENCES, "");
        return getProfilesFromJson(profilesJson, bWebOnly);
    }

    /**
     * Save the list of profiles to preferences.
     *
     * @param preferences the preferences object.
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
    public List<Profile> getProfilesFromJson(String profilesJson, boolean bWebOnly) throws JSONException {
        List<Profile> profilesList = new ArrayList<>();

        if (profilesJson.trim().length() == 0)
            return profilesList;

        JSONObject root = new JSONObject(profilesJson);
        JSONArray profilesArray = root.getJSONArray(PROFILES);
        for (int i = 0; i < profilesArray.length(); i++) {
            JSONObject profileObject = profilesArray.getJSONObject(i);
            Profile profile = getProfileFromJson(profileObject, bWebOnly);
            if (profile != null) profilesList.add(profile);
        }
        return profilesList;
    }

    /**
     * Extract a profile from a json object.
     *
     * @param profileObject the json object.
     * @return the profile.
     * @throws JSONException
     */
    public Profile getProfileFromJson(JSONObject profileObject)throws JSONException{
        return getProfileFromJson( profileObject,false);
    }

    public Profile getProfileFromJson(JSONObject profileObject, boolean bWebOnly) throws JSONException {
        Profile profile = new Profile();
        boolean isWebProfile = false;

        Iterator<String> attributesNameIterator = profileObject.keys();
        while (attributesNameIterator.hasNext()) {
            String attributeName = attributesNameIterator.next();

            if (attributeName.equals(NAME)) {
                profile.name = profileObject.getString(NAME);
            } else if (attributeName.equals(DESCRIPTION)) {
                profile.description = profileObject.getString(DESCRIPTION);
            } else if (attributeName.equals(CREATIONDATE)) {
                profile.creationdate = profileObject.getString(CREATIONDATE);
            } else if (attributeName.equals(MODIFIEDDATE)) {
                profile.modifieddate = profileObject.getString(MODIFIEDDATE);
            } else if (attributeName.equals(COLOR)) {
                profile.color = profileObject.getString(COLOR);
            } else if (attributeName.equals(MAPVIEW)) {
                profile.mapView = profileObject.getString(MAPVIEW);
            } else if (attributeName.equals(ACTIVE)) {
                profile.active = profileObject.getBoolean(ACTIVE);
            } else if (attributeName.equals(SDCARD_PATH)) {
                String sdcardPath = profileObject.getString(SDCARD_PATH);
                profile.setSdcardPath(sdcardPath);
            } else if (attributeName.equals(TAGS)) {
                JSONObject tagsObject = profileObject.getJSONObject(TAGS);
                if (tagsObject.has(PATH)) {
                    String path = tagsObject.getString(PATH);
                    ProfileTags profileTags = new ProfileTags();
                    profileTags.setRelativePath(path);
                    if (tagsObject.has(MODIFIEDDATE)) {
                        String modifieddate = tagsObject.getString(MODIFIEDDATE);
                        profileTags.tagsModifiedDate = modifieddate;
                    }
                    if (tagsObject.has(URL)) {
                        String url = tagsObject.getString(URL);
                        profileTags.tagsUrl = url;
                    }
                    if (tagsObject.has(SIZE)) {
                        long size = tagsObject.getLong(SIZE);
                        profileTags.tagsSize = size;
                    }
                    profile.profileTags = profileTags;
                }
            } else if (attributeName.equals(PROJECT)) {
                JSONObject projectObject = profileObject.getJSONObject(PROJECT);
                if (projectObject.has(PATH)) {
                    ProfileProjects profileProject = new ProfileProjects();
                    profileProject.setRelativePath(projectObject.getString(PATH));
                    if (projectObject.has(MODIFIEDDATE)) {
                        String modifieddate = projectObject.getString(MODIFIEDDATE);
                        profileProject.projectModifiedDate = modifieddate;
                    }
                    if (projectObject.has(URL)) {
                        String url = projectObject.getString(URL);
                        profileProject.projectUrl = url;
                    }
                    if (projectObject.has(UPLOADURL)) {
                        String uploadurl = projectObject.getString(UPLOADURL);
                        profileProject.projectUploadUrl = uploadurl;
                        if ( uploadurl != null && !uploadurl.isEmpty() ) isWebProfile = true;
                    }
                    if (projectObject.has(SIZE)) {
                        long size = projectObject.getLong(SIZE);
                        profileProject.projectSize = size;
                    }
                    profile.profileProject = profileProject;
                }
            } else if (attributeName.equals(BASEMAPS)) {
                JSONArray basemapsArray = profileObject.getJSONArray(BASEMAPS);
                for (int j = 0; j < basemapsArray.length(); j++) {
                    JSONObject baseMapObject = basemapsArray.getJSONObject(j);
                    if (baseMapObject.has(PATH)) {
                        String path = baseMapObject.getString(PATH);
                        ProfileBasemaps basemap = new ProfileBasemaps();
                        basemap.setRelativePath(path);
                        if (baseMapObject.has(MODIFIEDDATE)) {
                            String modifieddate = baseMapObject.getString(MODIFIEDDATE);
                            basemap.modifiedDate = modifieddate;
                        }
                        if (baseMapObject.has(URL)) {
                            String url = baseMapObject.getString(URL);
                            basemap.url = url;
                        }
                        if (baseMapObject.has(SIZE)) {
                            long size = baseMapObject.getLong(SIZE);
                            basemap.size = size;
                        }
                        profile.basemapsList.add(basemap);
                    }
                }
            } else if (attributeName.equals(SPATIALITE)) {
                JSONArray spatialiteDbsArray = profileObject.getJSONArray(SPATIALITE);
                for (int j = 0; j < spatialiteDbsArray.length(); j++) {
                    JSONObject spatialiteDbsObject = spatialiteDbsArray.getJSONObject(j);
                    if (spatialiteDbsObject.has(PATH)) {
                        String path = spatialiteDbsObject.getString(PATH);

                        ProfileSpatialitemaps spatialitemap = new ProfileSpatialitemaps();
                        spatialitemap.setRelativePath(path);

                        if (spatialiteDbsObject.has(MODIFIEDDATE)) {
                            String modifieddate = spatialiteDbsObject.getString(MODIFIEDDATE);
                            spatialitemap.modifiedDate = modifieddate;
                        }
                        if (spatialiteDbsObject.has(URL)) {
                            String url = spatialiteDbsObject.getString(URL);
                            spatialitemap.url = url;
                        }
                        if (spatialiteDbsObject.has(UPLOADURL)) {
                            String url = spatialiteDbsObject.getString(UPLOADURL);
                            spatialitemap.uploadUrl = url;
                        }
                        if (spatialiteDbsObject.has(SIZE)) {
                            long size = spatialiteDbsObject.getLong(SIZE);
                            spatialitemap.size = size;
                        }
                        if (spatialiteDbsObject.has(VISIBLE)) {
                            JSONArray visibleArray = spatialiteDbsObject.getJSONArray(VISIBLE);
                            int length = visibleArray.length();
                            spatialitemap.visibleLayerNames = new String[length];
                            for (int i = 0; i < length; i++) {
                                String layerName = visibleArray.getString(i);
                                spatialitemap.visibleLayerNames[i] = layerName;
                            }
                        }

                        profile.spatialiteList.add(spatialitemap);
                    }
                }
            } else if (attributeName.equals(OTHERFILES)) {
                JSONArray otherFilesArray = profileObject.getJSONArray(OTHERFILES);
                for (int j = 0; j < otherFilesArray.length(); j++) {
                    JSONObject otherFileObject = otherFilesArray.getJSONObject(j);
                    if (otherFileObject.has(PATH)) {
                        String path = otherFileObject.getString(PATH);
                        ProfileOtherfiles otherfiles = new ProfileOtherfiles();
                        otherfiles.setRelativePath(path);
                        if (otherFileObject.has(MODIFIEDDATE)) {
                            String modifieddate = otherFileObject.getString(MODIFIEDDATE);
                            otherfiles.modifiedDate = modifieddate;
                        }
                        if (otherFileObject.has(URL)) {
                            String url = otherFileObject.getString(URL);
                            otherfiles.url = url;
                        }
                        if (otherFileObject.has(SIZE)) {
                            long size = otherFileObject.getLong(SIZE);
                            otherfiles.size = size;
                        }
                        profile.otherFilesList.add(otherfiles);
                    }
                }
            } else {
                Object object = profileObject.get(attributeName);
                profile.vendorAttributes.put(attributeName, object.toString());
            }
        }

        if (bWebOnly) {
            if (isWebProfile){
                return profile;
            } else {
                return null;
            }
        } else {
            return profile;
        }
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

    /**
     * Converts a profile object to a json object.
     *
     * @param profile the profile.
     * @return the json obj.
     * @throws JSONException
     */
    public JSONObject getJsonFromProfile(Profile profile) throws JSONException {
        JSONObject profileObject = new JSONObject();
        profileObject.put(NAME, profile.name);
        profileObject.put(DESCRIPTION, profile.description);
        profileObject.put(CREATIONDATE, profile.creationdate);
        profileObject.put(MODIFIEDDATE, profile.modifieddate);
        profileObject.put(COLOR, profile.color);
        profileObject.put(MAPVIEW, profile.mapView);
        profileObject.put(ACTIVE, profile.active);
        profileObject.put(SDCARD_PATH, profile.getSdcardPathRaw());

        if (profile.profileTags != null) {
            ProfileTags profileTags = profile.profileTags;
            JSONObject tagsObject = new JSONObject();
            tagsObject.put(PATH, profileTags.getRelativePath());
            tagsObject.put(SIZE, profileTags.tagsSize);
            if (profileTags.tagsModifiedDate != null) {
                tagsObject.put(MODIFIEDDATE, profileTags.tagsModifiedDate);
            }
            if (profileTags.tagsUrl != null) {
                tagsObject.put(URL, profileTags.tagsUrl);
            }
            profileObject.put(TAGS, tagsObject);
        }
        if (profile.profileProject != null) {
            ProfileProjects profileProject = profile.profileProject;
            JSONObject tagsObject = new JSONObject();
            tagsObject.put(PATH, profileProject.getRelativePath());
            tagsObject.put(SIZE, profileProject.projectSize);
            if (profileProject.projectModifiedDate != null) {
                tagsObject.put(MODIFIEDDATE, profileProject.projectModifiedDate);
            }
            if (profileProject.projectUrl != null) {
                tagsObject.put(URL, profileProject.projectUrl);
            }
            if (profileProject.projectUploadUrl != null) {
                tagsObject.put(UPLOADURL, profileProject.projectUploadUrl);
            }
            profileObject.put(PROJECT, tagsObject);
        }

        if (!profile.basemapsList.isEmpty()) {
            JSONArray basemapsArray = new JSONArray();
            for (int j = 0; j < profile.basemapsList.size(); j++) {
                ProfileBasemaps basemaps = profile.basemapsList.get(j);
                JSONObject baseMapObject = new JSONObject();
                baseMapObject.put(PATH, basemaps.getRelativePath());
                baseMapObject.put(SIZE, basemaps.size);
                if (basemaps.modifiedDate != null) {
                    baseMapObject.put(MODIFIEDDATE, basemaps.modifiedDate);
                }
                if (basemaps.url != null) {
                    baseMapObject.put(URL, basemaps.url);
                }
                basemapsArray.put(j, baseMapObject);
            }
            profileObject.put(BASEMAPS, basemapsArray);
        }

        if (!profile.spatialiteList.isEmpty()) {
            JSONArray spatialiteDbsArray = new JSONArray();
            for (int j = 0; j < profile.spatialiteList.size(); j++) {
                ProfileSpatialitemaps spatialitemap = profile.spatialiteList.get(j);

                JSONObject spatialiteDbObject = new JSONObject();
                spatialiteDbObject.put(PATH, spatialitemap.getRelativePath());
                spatialiteDbObject.put(SIZE, spatialitemap.size);

                if (spatialitemap.modifiedDate != null) {
                    spatialiteDbObject.put(MODIFIEDDATE, spatialitemap.modifiedDate);
                }
                if (spatialitemap.url != null) {
                    spatialiteDbObject.put(URL, spatialitemap.url);
                }
                if (spatialitemap.uploadUrl != null) {
                    spatialiteDbObject.put(UPLOADURL, spatialitemap.uploadUrl);
                }
                if (spatialitemap.visibleLayerNames != null) {
                    JSONArray visibleArray = new JSONArray();
                    for (int i = 0; i < spatialitemap.visibleLayerNames.length; i++) {
                        visibleArray.put(spatialitemap.visibleLayerNames[i]);
                    }
                    spatialiteDbObject.put(VISIBLE, visibleArray);
                }
                spatialiteDbsArray.put(spatialiteDbObject);
            }
            profileObject.put(SPATIALITE, spatialiteDbsArray);
        }

        if (!profile.otherFilesList.isEmpty()) {
            JSONArray otherfilesArray = new JSONArray();
            for (int j = 0; j < profile.otherFilesList.size(); j++) {
                ProfileOtherfiles otherfile = profile.otherFilesList.get(j);
                JSONObject otherfileObject = new JSONObject();
                otherfileObject.put(PATH, otherfile.getRelativePath());
                otherfileObject.put(SIZE, otherfile.size);
                if (otherfile.modifiedDate != null) {
                    otherfileObject.put(MODIFIEDDATE, otherfile.modifiedDate);
                }
                if (otherfile.url != null) {
                    otherfileObject.put(URL, otherfile.url);
                }
                otherfilesArray.put(j, otherfileObject);
            }
            profileObject.put(OTHERFILES, otherfilesArray);
        }

        if (!profile.vendorAttributes.isEmpty()) {
            for (Entry<String, String> entry : profile.vendorAttributes.entrySet()) {
                String value = entry.getValue();
                if (value != null)
                    profileObject.put(entry.getKey(), value);
            }
        }

        return profileObject;
    }


    /**
     * Getter for the active profile.
     *
     * @return the active profile or null if there is none.
     */
    public Profile getActiveProfile() {
        return activeProfile;
    }

    /**
     * Get the active profile from a contentresolver.
     *
     * @param contentResolver the resolver to use.
     * @throws JSONException
     */
    public void checkActiveProfile(ContentResolver contentResolver) throws JSONException {
        activeProfile = null;
        String[] projection = CONTENT_PROVIDER_FIELDS;
        Cursor cursor = contentResolver.query(CONTENT_URI,
                projection,
                null,
                null,
                null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                boolean active = cursor.getInt(1) != 0;
                if (name != null && active) {
                    String json = cursor.getString(2);
                    JSONObject profileObject = new JSONObject(json);
                    activeProfile = ProfilesHandler.INSTANCE.getProfileFromJson(profileObject);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
    }


    public List<BaseMap> getBaseMaps() {
        List<BaseMap> baseMaps = new ArrayList<>();
        if (activeProfile != null) {
            for (ProfileBasemaps baseMap : activeProfile.basemapsList) {
                BaseMap map = new BaseMap();
                File databaseFile = activeProfile.getFile(baseMap.getRelativePath());
                if (databaseFile.exists()) {
                    map.parentFolder = databaseFile.getParentFile().getAbsolutePath();
                    map.databasePath = databaseFile.getAbsolutePath();
                    map.title = FileUtilities.getNameWithoutExtention(databaseFile);
                    map.mapType = ESpatialDataSources.getTypeName4FileName(databaseFile.getName());
                    baseMaps.add(map);
                }
            }
        }

        return baseMaps;
    }


    public List<Profile> addJsonProfile(JSONObject profileObject, List<Profile> profileList) throws JSONException {
        Profile newProfile = getProfileFromJson(profileObject);
        profileList.add(newProfile);
        return profileList;
    }

}
