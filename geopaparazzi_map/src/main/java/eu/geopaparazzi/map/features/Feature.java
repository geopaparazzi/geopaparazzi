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

package eu.geopaparazzi.map.features;

import android.os.Parcel;
import android.os.Parcelable;

import org.hortonmachine.dbs.datatypes.EDataType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;

/**
 * A spatial feature container.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Feature implements Parcelable {

    private int idIndex = -1;
    private int geometryIndex = -1;

    private String tableName;
    private String databasePath;

    private List<String> attributeNames = new ArrayList<>();
    private List<Object> attributeValues = new ArrayList<>();
    private List<String> attributeTypes = new ArrayList<>();

    private boolean isDirty = false;

    /**
     * Constructor for case with geometry.
     *
     * @param tableName     the table the feature belongs to.
     * @param databasePath  the path to the containing db.
     * @param idIndex       the index of the primary key of the feature, which can be used for example for updates.
     * @param geometryIndex the index of the geometry in the data.
     */
    public Feature(String tableName, String databasePath, int idIndex, int geometryIndex) {
        this.tableName = tableName;
        this.databasePath = databasePath;
        this.idIndex = idIndex;
        this.geometryIndex = geometryIndex;
    }

    public int getIdIndex() {
        return idIndex;
    }

    public String getIdFieldName() {
        if (idIndex == -1) return null;
        return attributeNames.get(idIndex);
    }

    public long getIdFieldValue() {
        Object o = attributeValues.get(idIndex);
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return -1;
    }


    public int getGeometryIndex() {
        return geometryIndex;
    }


    /**
     * Add a new attribute.
     *
     * @param name  name of the field.
     * @param value value of the field as string.
     * @param type  type of the field.
     */
    public synchronized void addAttribute(String name, Object value, String type) {
        if (attributeNames.contains(name)) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Attribute already present: {0}. Use setAttribute instead.", name)); //$NON-NLS-1$
        }
        attributeNames.add(name);
        attributeValues.add(value);
        attributeTypes.add(type);
    }

    /**
     * Change an attribute through its field name.
     *
     * @param field the field name to change.
     * @param value the new value to set.
     */
    public void setAttribute(String field, Object value) {
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
    public void setAttribute(int index, Object value) {
        attributeValues.set(index, value);
        isDirty = true;
    }

    /**
     * Get an attribute's string representation by its name.
     *
     * @param name the name.
     * @return the attribute.
     */
    public Object getAttribute(String name) {
        int indexOf = attributeNames.indexOf(name);
        if (indexOf != -1) {
            return attributeValues.get(indexOf);
        }
        return null;
    }

    /**
     * @return the default geometry.
     */
    public Geometry getDefaultGeometry() {
        if (geometryIndex == -1)
            return null;
        return (Geometry) attributeValues.get(geometryIndex);
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
    public List<Object> getAttributeValues() {
        return attributeValues;
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
     * @return the path to the containing database.
     */
    public String getDatabasePath() {
        return databasePath;
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

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(idIndex);
        dest.writeInt(geometryIndex);
        dest.writeString(tableName);
        dest.writeString(databasePath);
        dest.writeList(attributeNames);
        dest.writeList(attributeTypes);
        for (int i = 0; i < attributeValues.size(); i++) {
            Object obj = attributeValues.get(i);
            if (i == geometryIndex) {
                if (obj == null) {
                    dest.writeByteArray(null);
                } else {
                    Geometry geom = (Geometry) obj;
                    WKBWriter wkbWriter = new WKBWriter();
                    dest.writeByteArray(wkbWriter.write(geom));
                }
            } else {
                String type = attributeTypes.get(i);
                EDataType type4Name = EDataType.getType4Name(type);
                switch (type4Name) {
                    case TEXT: {
                        if (obj == null) {
                            dest.writeString("null");
                        } else {
                            dest.writeString((String) obj);
                        }
                        break;
                    }
                    case INTEGER: {
                        if (obj == null) {
                            dest.writeInt(Integer.MIN_VALUE);
                        } else {
                            dest.writeInt(((Number) obj).intValue());
                        }
                        break;
                    }
                    case FLOAT: {
                        if (obj == null) {
                            dest.writeFloat(Float.NaN);
                        } else {
                            dest.writeFloat(((Number) obj).floatValue());
                        }
                        break;
                    }
                    case DOUBLE: {
                        if (obj == null) {
                            dest.writeDouble(Double.NaN);
                        } else {
                            dest.writeDouble(((Number) obj).doubleValue());
                        }
                        break;
                    }
                    case LONG: {
                        if (obj == null) {
                            dest.writeLong(Long.MIN_VALUE);
                        } else {
                            dest.writeLong(((Number) obj).longValue());
                        }
                        break;
                    }
                    case BLOB: {
                        dest.writeValue(obj);
                        break;
                    }
                }

            }
        }
    }

    @SuppressWarnings("javadoc")
    public static final Creator<Feature> CREATOR = new Creator<Feature>() {
        @SuppressWarnings("unchecked")
        public Feature createFromParcel(Parcel in) {
            int idIndex = in.readInt();
            int geometryIndex = in.readInt();
            String tableName = in.readString();
            String databasePath = in.readString();

            List<String> attributeNames = in.readArrayList(String.class.getClassLoader());
            List<String> attributeTypes = in.readArrayList(String.class.getClassLoader());
            List<Object> attributeValues = new ArrayList<>();
            for (int i = 0; i < attributeNames.size(); i++) {
                if (i == geometryIndex) {
                    WKBReader wkbReader = new WKBReader();
                    Geometry defaultGeometry = null;
                    try {
                        defaultGeometry = wkbReader.read(in.createByteArray());
                    } catch (ParseException e) {
                        GPLog.error(this, null, e);
                    }
                    attributeValues.add(defaultGeometry);
                } else {
                    String type = attributeTypes.get(i);
                    EDataType type4Name = EDataType.getType4Name(type);
                    switch (type4Name) {
                        case TEXT: {
                            String o = in.readString();
                            attributeValues.add(o);
                            break;
                        }
                        case INTEGER: {
                            int o = in.readInt();
                            attributeValues.add(o);
                            break;
                        }
                        case FLOAT: {
                            float o = in.readFloat();
                            attributeValues.add(o);
                            break;
                        }
                        case DOUBLE: {
                            double o = in.readDouble();
                            attributeValues.add(o);
                            break;
                        }
                        case LONG: {
                            long o = in.readLong();
                            attributeValues.add(o);
                            break;
                        }
                        case BLOB: {
                            Object byteArray = in.readValue(Object.class.getClassLoader());
                            attributeValues.add(byteArray);
                            break;
                        }
                    }

                }
            }

            Feature feature = new Feature(tableName, databasePath, idIndex, geometryIndex);
            feature.attributeNames = attributeNames;
            feature.attributeValues = attributeValues;
            feature.attributeTypes = attributeTypes;
            return feature;
        }

        public Feature[] newArray(int size) {
            return new Feature[size];
        }
    };

}
