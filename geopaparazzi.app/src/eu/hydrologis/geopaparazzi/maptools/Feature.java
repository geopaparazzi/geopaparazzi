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
package eu.hydrologis.geopaparazzi.maptools;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A spatial feature container.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Feature implements Parcelable {

    String id;

    String tableName;

    Geometry defaultGeometry;

    List<String> attributeNames = new ArrayList<String>();
    List<String> attributeValuesStrings = new ArrayList<String>();
    List<String> attributeClasses = new ArrayList<String>();

    Feature() {
    }

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Get an attribute's string representation by its name.
     * 
     * @param name the name.
     * @return the attribute.
     */
    public String getAttributeAsString( String name ) {
        int indexOf = attributeNames.indexOf(name);
        if (indexOf != -1) {
            return attributeValuesStrings.get(indexOf);
        }
        return null;
    }

    /**
     * @return the default geometry.
     */
    public Geometry getDefaultGeometry() {
        return defaultGeometry;
    }

    /**
     * @return the list of attributes names.
     */
    public List<String> getAttributeNames() {
        return attributeNames;
    }

    /**
     * @return the list of attribute values in string representation.
     */
    public List<String> getAttributeValuesStrings() {
        return attributeValuesStrings;
    }

    /**
     * @return the list of attributes classes.
     */
    public List<String> getAttributeClasses() {
        return attributeClasses;
    }

    /**
     * @return the name of the table the feature is part of.
     */
    public String getTableName() {
        return tableName;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel( Parcel dest, int flags ) {
        dest.writeString(id);
        dest.writeString(tableName);
        dest.writeList(attributeNames);
        dest.writeList(attributeValuesStrings);
        dest.writeList(attributeClasses);
        // TODO add geometry
    }

    @SuppressWarnings("javadoc")
    public static final Parcelable.Creator<Feature> CREATOR = new Parcelable.Creator<Feature>(){
        @SuppressWarnings("unchecked")
        public Feature createFromParcel( Parcel in ) {
            Feature feature = new Feature();
            feature.id = in.readString();
            feature.tableName = in.readString();
            feature.attributeNames = in.readArrayList(String.class.getClassLoader());
            feature.attributeValuesStrings = in.readArrayList(String.class.getClassLoader());
            feature.attributeClasses = in.readArrayList(String.class.getClassLoader());

            return feature;
        }

        public Feature[] newArray( int size ) {
            return new Feature[size];
        }
    };
}
