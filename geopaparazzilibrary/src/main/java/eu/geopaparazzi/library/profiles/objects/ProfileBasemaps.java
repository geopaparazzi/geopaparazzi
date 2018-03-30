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

/**
 * Created by hydrologis on 19/03/18.
 */
public class ProfileBasemaps extends ARelativePathResource implements Parcelable, IDownloadable {
    public String url = "";
    public String modifiedDate = "";
    public long size = -1;
    private String destinationPath = "";

    public ProfileBasemaps() {
    }


    protected ProfileBasemaps(Parcel in) {
        url = in.readString();
        modifiedDate = in.readString();
        size = in.readLong();
        destinationPath = in.readString();
    }

    public static final Creator<ProfileBasemaps> CREATOR = new Creator<ProfileBasemaps>() {
        @Override
        public ProfileBasemaps createFromParcel(Parcel in) {
            return new ProfileBasemaps(in);
        }

        @Override
        public ProfileBasemaps[] newArray(int size) {
            return new ProfileBasemaps[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfileBasemaps basemaps = (ProfileBasemaps) o;

        if (!relativePath.equals(basemaps.relativePath)) return false;
        return modifiedDate != null ? modifiedDate.equals(basemaps.modifiedDate) : basemaps.modifiedDate == null;
    }

    @Override
    public int hashCode() {
        int result = relativePath.hashCode();
        result = 31 * result + (modifiedDate != null ? modifiedDate.hashCode() : 0);
        return result;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public String getUrl() {
        return url;
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
        dest.writeString(url);
        dest.writeString(modifiedDate);
        dest.writeLong(size);
        dest.writeString(destinationPath);
    }
}
