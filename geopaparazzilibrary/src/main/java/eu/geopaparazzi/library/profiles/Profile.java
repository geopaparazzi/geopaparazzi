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

import java.util.ArrayList;
import java.util.List;

/**
 * Class providing a wrapper for profile infos.
 *
 * @author Andrea Antonello
 */
public class Profile implements Parcelable {
    public String name = "new profile";
    public String description = "new profile description";
    public String creationdate = "";
    public boolean active = false;
    public String color = "#FFFFFF";
    public String tagsPath = "";
    public String projectPath = "";
    public List<String> basemapsList = new ArrayList<>();
    public List<String> spatialiteList = new ArrayList<>();

    @Override
    public int describeContents() {
        return 0;
    }

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
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(creationdate);
        dest.writeBooleanArray(new boolean[]{active});
        dest.writeString(color);
        dest.writeString(tagsPath);
        dest.writeString(projectPath);
        dest.writeList(basemapsList);
        dest.writeList(spatialiteList);
    }


    @SuppressWarnings("javadoc")
    public static final Creator<Profile> CREATOR = new Creator<Profile>() {
        @SuppressWarnings("unchecked")
        public Profile createFromParcel(Parcel in) {
            Profile profile = new Profile();
            profile.name = in.readString();
            profile.description = in.readString();
            profile.creationdate = in.readString();

            boolean[] activeArray = new boolean[1];
            in.readBooleanArray(activeArray);
            profile.active = activeArray[0];

            profile.color = in.readString();
            profile.tagsPath = in.readString();
            profile.projectPath = in.readString();
            profile.basemapsList = in.readArrayList(String.class.getClassLoader());
            profile.spatialiteList = in.readArrayList(String.class.getClassLoader());

            return profile;
        }

        public Profile[] newArray(int size) {
            return new Profile[size];
        }
    };
}
