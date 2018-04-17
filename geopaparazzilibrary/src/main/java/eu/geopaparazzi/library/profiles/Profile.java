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

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.profiles.objects.ProfileBasemaps;
import eu.geopaparazzi.library.profiles.objects.ProfileOtherfiles;
import eu.geopaparazzi.library.profiles.objects.ProfileProjects;
import eu.geopaparazzi.library.profiles.objects.ProfileSpatialitemaps;
import eu.geopaparazzi.library.profiles.objects.ProfileTags;

/**
 * Class providing a wrapper for profile infos.
 *
 * @author Andrea Antonello
 */
public class Profile implements Parcelable {
    public String name = "new profile";
    public String description = "new profile description";
    public String creationdate = "";
    public String modifieddate = "";
    public String color = "#FFFFFF";
    public String mapView = "";
    public boolean active = false;
    private String sdcardPath = "";

    public ProfileTags profileTags;
    public ProfileProjects profileProject;

    public List<ProfileBasemaps> basemapsList = new ArrayList<>();
    public List<ProfileSpatialitemaps> spatialiteList = new ArrayList<>();
    public List<ProfileOtherfiles> otherFilesList = new ArrayList<>();

    public HashMap<String, String> vendorAttributes = new HashMap<>();

    public Profile() {
    }

    protected Profile(Parcel in) {
        name = in.readString();
        description = in.readString();
        creationdate = in.readString();
        modifieddate = in.readString();
        color = in.readString();
        mapView = in.readString();
        active = in.readByte() != 0;
        sdcardPath = in.readString();
        profileTags = in.readParcelable(ProfileTags.class.getClassLoader());
        profileProject = in.readParcelable(ProfileProjects.class.getClassLoader());
        basemapsList = in.createTypedArrayList(ProfileBasemaps.CREATOR);
        spatialiteList = in.createTypedArrayList(ProfileSpatialitemaps.CREATOR);
        otherFilesList = in.createTypedArrayList(ProfileOtherfiles.CREATOR);
        vendorAttributes = in.readHashMap(HashMap.class.getClassLoader());
    }

    /**
     * @return the absolute path of the used sdcard.
     */
    public String getSdcardPath() {
        try {
            ResourcesManager resourcesManager = ResourcesManager.getInstance(null);
            if (sdcardPath.equals(ProfilesHandler.MAINSTORAGE)) {
                return resourcesManager.getMainStorageDir().getAbsolutePath();
            } else if (sdcardPath.equals(ProfilesHandler.SECONDARYSTORAGE)) {
                List<File> otherStorageDirs = resourcesManager.getOtherStorageDirs();
                for (File f : otherStorageDirs) {
                    if (f != null && f.exists()) {
                        return f.getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
        return sdcardPath;
    }

    public String getSdcardPathRaw() {
        return sdcardPath;
    }

    public void setSdcardPath(String absoluteSdcardPath) {
        try {
            ResourcesManager resourcesManager = ResourcesManager.getInstance(null);
            if (absoluteSdcardPath.equals(resourcesManager.getMainStorageDir().getAbsolutePath())) {
                sdcardPath = ProfilesHandler.MAINSTORAGE;
            } else {
                List<File> otherStorageDirs = resourcesManager.getOtherStorageDirs();
                for (File f : otherStorageDirs) {
                    if (f != null && f.exists()) {
                        String absolutePath = f.getAbsolutePath();
                        if (absoluteSdcardPath.equals(absolutePath)) {
                            sdcardPath = ProfilesHandler.SECONDARYSTORAGE;
                            return;
                        }
                    }
                }
                sdcardPath = absoluteSdcardPath;
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    /**
     * Create a file basing on the sdcard path position.
     *
     * @param relativePath the relative path to add to the used sdcard path.
     * @return the existing file.
     */
    public File getFile(String relativePath) {
        return new File(getSdcardPath() + File.separator + relativePath);
    }

    public static final Creator<Profile> CREATOR = new Creator<Profile>() {
        @Override
        public Profile createFromParcel(Parcel in) {
            return new Profile(in);
        }

        @Override
        public Profile[] newArray(int size) {
            return new Profile[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Profile)) {
            return false;
        }
        Profile p = (Profile) o;
        if (p.name != null && name != null && !p.name.equals(name)) {
            return false;
        }
        if (p.description != null && description != null && !p.description.equals(description)) {
            return false;
        }
        if (p.creationdate != null && creationdate != null && !p.creationdate.equals(creationdate)) {
            return false;
        }
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(creationdate);
        dest.writeString(modifieddate);
        dest.writeString(color);
        dest.writeString(mapView);
        dest.writeByte((byte) (active ? 1 : 0));
        dest.writeString(sdcardPath);
        dest.writeParcelable(profileTags, flags);
        dest.writeParcelable(profileProject, flags);
        dest.writeTypedList(basemapsList);
        dest.writeTypedList(spatialiteList);
        dest.writeTypedList(otherFilesList);
        dest.writeMap(vendorAttributes);
    }

    public boolean matches(String filterText) {
        filterText = filterText.toLowerCase();
        if (name.toLowerCase().contains(filterText)) {
            return true;
        } else if (description.toLowerCase().contains(filterText)) {
            return true;
        } else if (creationdate.toLowerCase().contains(filterText)) {
            return true;
        } else if (modifieddate.toLowerCase().contains(filterText)) {
            return true;
        }
        return false;
    }


    /**
     * Corrects the sdcard in all paths if necessary.
     *
     * @param newSdcard the current sdcard
     */
//    public void correctPaths(String newSdcard) {
//        boolean hasChanged = false;
//        if (projectRelativePath.startsWith(sdcardPath)) {
//            projectRelativePath = projectRelativePath.replace(sdcardPath, newSdcard);
//            hasChanged = true;
//        }
//        if (tagsRelativePath.startsWith(sdcardPath)) {
//            tagsRelativePath = tagsRelativePath.replace(sdcardPath, newSdcard);
//            hasChanged = true;
//        }
//
//        List<String> newBasemapsList = new ArrayList<>();
//        for (int i = 0; i < basemapsList.size(); i++) {
//            String basemap = basemapsList.get(i);
//            if (basemap.startsWith(sdcardPath)) {
//                basemap = basemap.replace(sdcardPath, newSdcard);
//                newBasemapsList.add(basemap);
//                hasChanged = true;
//            }
//        }
//        basemapsList.clear();
//        basemapsList.addAll(newBasemapsList);
//
//        List<String> newSpatialitedbList = new ArrayList<>();
//        for (String spatialitedb : spatialiteList) {
//            if (spatialitedb.startsWith(sdcardPath)) {
//                spatialitedb = spatialitedb.replace(sdcardPath, newSdcard);
//                newSpatialitedbList.add(spatialitedb);
//                hasChanged = true;
//            }
//        }
//        spatialiteList.clear();
//        spatialiteList.addAll(newSpatialitedbList);
//        if (hasChanged) {
//            sdcardPath = newSdcard;
//        }
//    }
}
