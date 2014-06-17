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
    List<String> attributeTypes = new ArrayList<String>();

    boolean isDirty = false;

    Feature() {
    }

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Change an attribute through its field name.
     * 
     * @param field the field name to change.
     * @param value the new value to set.
     */
    public void setAttribute( String field, String value ) {
        int indexOf = attributeNames.indexOf(field);
        if (indexOf != -1) {
            setAttribute(indexOf, value);
        }
    }

    /**
     * Change an attribute through its index.
     * 
     * @param index the index.
     * @param value the new value to set.
     */
    private void setAttribute( int index, String value ) {
        attributeValuesStrings.set(index, value);
        isDirty = true;
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
    public List<String> getAttributeTypes() {
        return attributeTypes;
    }

    /**
     * @return the name of the table the feature is part of.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @return <code>true</code> if the features has been modified.
     */
    public boolean isDirty() {
        return isDirty;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel( Parcel dest, int flags ) {
        dest.writeString(id);
        dest.writeString(tableName);
        dest.writeList(attributeNames);
        dest.writeList(attributeValuesStrings);
        dest.writeList(attributeTypes);
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
            feature.attributeTypes = in.readArrayList(String.class.getClassLoader());

            return feature;
        }

        public Feature[] newArray( int size ) {
            return new Feature[size];
        }
    };
}
