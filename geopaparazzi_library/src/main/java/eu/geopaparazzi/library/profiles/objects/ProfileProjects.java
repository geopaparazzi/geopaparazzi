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
package eu.geopaparazzi.library.profiles.objects;

import android.os.Parcel;
import android.os.Parcelable;

import eu.geopaparazzi.library.network.download.IDownloadable;
import eu.geopaparazzi.library.network.upload.IUploadable;

/**
 * Created by hydrologis on 19/03/18.
 */
public class ProfileProjects extends ARelativePathResource implements Parcelable, IDownloadable, IUploadable {
    public String projectUrl = "";
    public String projectUploadUrl = "";
    public String projectModifiedDate = "";
    public long projectSize = -1;
    private String destinationPath = "";

    public ProfileProjects() {
    }


    protected ProfileProjects(Parcel in) {
        projectUrl = in.readString();
        projectUploadUrl = in.readString();
        projectModifiedDate = in.readString();
        projectSize = in.readLong();
        destinationPath = in.readString();
    }

    public static final Creator<ProfileProjects> CREATOR = new Creator<ProfileProjects>() {
        @Override
        public ProfileProjects createFromParcel(Parcel in) {
            return new ProfileProjects(in);
        }

        @Override
        public ProfileProjects[] newArray(int size) {
            return new ProfileProjects[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfileProjects that = (ProfileProjects) o;

        if (!relativePath.equals(that.relativePath)) return false;
        return projectModifiedDate != null ? projectModifiedDate.equals(that.projectModifiedDate) : that.projectModifiedDate == null;
    }

    @Override
    public int hashCode() {
        int result = relativePath.hashCode();
        result = 31 * result + (projectModifiedDate != null ? projectModifiedDate.hashCode() : 0);
        return result;
    }

    @Override
    public long getSize() {
        return projectSize;
    }

    @Override
    public String getUrl() {
        return projectUrl;
    }

    @Override
    public String getUploadUrl() {
        return projectUploadUrl;
    }

    @Override
    public String getDestinationPath() {
        return destinationPath;
    }

    @Override
    public void setDestinationPath(String path) {
        destinationPath = path;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(projectUrl);
        dest.writeString(projectUploadUrl);
        dest.writeString(projectModifiedDate);
        dest.writeLong(projectSize);
        dest.writeString(destinationPath);
    }
}
