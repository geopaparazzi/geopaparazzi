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
package eu.geopaparazzi.library.features;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import eu.geopaparazzi.library.util.DataType;

/**
 * A spatial feature container.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Feature implements Parcelable {

    private String id;

    private String readableTableName;
    private String uniqueTableName;

    private byte[] defaultGeometry;

    private List<String> attributeNames = new ArrayList<String>();
    private List<String> attributeValuesStrings = new ArrayList<String>();
    private List<String> attributeTypes = new ArrayList<String>();

    private double originalArea = -1;
    private double originalLength = -1;

    private boolean isDirty = false;

    /**
     * Constructor.
     * 
     * @param tableName the table the feature belongs to.
     * @param uniqueTableName the unique table name through which get the spatialtable.
     * @param id the unique id of the feature.
     */
    public Feature( String tableName, String uniqueTableName, String id ) {
        this.readableTableName = tableName;
        this.uniqueTableName = uniqueTableName;
        this.id = id;
    }

    /**
     * Constructor for case with geometry.
     * 
     * @param tableName the table the feature belongs to.
     * @param uniqueTableName the unique table name through which get the spatialtable.
     * @param id the unique id of the feature.
     * @param geometry the default geometry.
     */
    public Feature( String tableName, String uniqueTableName, String id, byte[] geometry ) {
        this.readableTableName = tableName;
        this.uniqueTableName = uniqueTableName;
        this.id = id;
        defaultGeometry = geometry;
    }

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Add a new attribute.
     * 
     * @param name name of the field.
     * @param value value of the field as string.
     * @param type type of the field as of {@link DataType#name()}.
     */
    public synchronized void addAttribute( String name, String value, String type ) {
        if (attributeNames.contains(name)) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Attribute already present: {0}. Use setAttribute instead.", name)); //$NON-NLS-1$
        }
        attributeNames.add(name);
        attributeValuesStrings.add(value);
        attributeTypes.add(type);
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
    public void setAttribute( int index, String value ) {
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
    public byte[] getDefaultGeometry() {
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
        return readableTableName;
    }

    /**
     * @return the unique table name.
     */
    public String getUniqueTableName() {
        return uniqueTableName;
    }

    /**
     * @return <code>true</code> if the features has been modified.
     */
    public boolean isDirty() {
        return isDirty;
    }

    public double getOriginalArea() {
        return originalArea;
    }

    public void setOriginalArea(double originalArea) {
        this.originalArea = originalArea;
    }

    public double getOriginalLength() {
        return originalLength;
    }

    public void setOriginalLength(double originalLength) {
        this.originalLength = originalLength;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel( Parcel dest, int flags ) {
        dest.writeString(id);
        dest.writeString(readableTableName);
        dest.writeString(uniqueTableName);
        dest.writeList(attributeNames);
        dest.writeList(attributeValuesStrings);
        dest.writeList(attributeTypes);
        dest.writeDouble(originalArea);
        dest.writeDouble(originalLength);
        dest.writeByteArray(defaultGeometry);
    }

    @SuppressWarnings("javadoc")
    public static final Parcelable.Creator<Feature> CREATOR = new Parcelable.Creator<Feature>(){
        @SuppressWarnings("unchecked")
        public Feature createFromParcel( Parcel in ) {
            String id = in.readString();
            String tableName = in.readString();
            String uniqueTableName = in.readString();
            Feature feature = new Feature(tableName, uniqueTableName, id);
            feature.attributeNames = in.readArrayList(String.class.getClassLoader());
            feature.attributeValuesStrings = in.readArrayList(String.class.getClassLoader());
            feature.attributeTypes = in.readArrayList(String.class.getClassLoader());
            double area = in.readDouble();
            double length = in.readDouble();
            feature.setOriginalArea(area);
            feature.setOriginalLength(length);
            byte[] geomBytes = in.createByteArray();
            feature.defaultGeometry = geomBytes;
            return feature;
        }

        public Feature[] newArray( int size ) {
            return new Feature[size];
        }
    };
}
