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
package eu.geopaparazzi.spatialite.database.spatial.core.tables;

import android.content.Context;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.util.DataType;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.TableTypes;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteUtilities;
import eu.geopaparazzi.spatialite.database.spatial.util.Style;

// https://www.gaia-gis.it/fossil/libspatialite/wiki?name=metadata-4.0

/**
 * A vector table from the spatial db.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SpatialVectorTable extends AbstractSpatialTable implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String geometryColumn;
    private final int geomType;

    private Style style;
    private String layerTypeDescription = "geometry";

    private boolean checkDone = false;
    private boolean isPolygon = false;
    private boolean isLine = false;
    private boolean isPoint = false;
    private boolean isGeometryCollection = false;
    /**
     * {@link HashMap} of all fields of this table [name,type]
     */
    private HashMap<String, String> fieldName2TypeMap = null;

    /**
     * {@link HashMap} of non-geometry fields [name,type]
     */
    private HashMap<String, String> fields_list_non_vector = null;

    // only non-geometry fields [name]
    private List<String> labelList = null;
    // Table field to be used as a label
    private String labelField = "";
    // list of possible primary keys - for more that one: seperated with ';'
    private String primaryKeyFields = "";
    // AbstractSpatialTable=always ROWID ; SpatialView: can also be ROWID - but something else
    private String ROWID_PK = "ROWID";
    // AbstractSpatialTable=-1 ; SpatialView: read_only=0 ; writable=1
    private int view_read_only = -1;
    private String uniqueNameBasedOnDbFilePath = "";
    // private String uniqueNameBasedOnDbFileName = "";

    /**
     * Constructor.
     *
     * @param databasePath            the database file the table belongs to.
     * @param tableName               the name of the table to use.
     * @param geometryColumn          the name of the geometry column.
     * @param geomType                the geometry type as off {@link GeometryType}.
     * @param srid                    the srid code.
     * @param center                  the wgs84 center coordinates.
     * @param bounds                  the table bounds in wgs84.
     * @param layerTypeDescription    the layer type description.
     */
    public SpatialVectorTable(String databasePath, String tableName, String geometryColumn, int geomType, String srid,
                              double[] center, double[] bounds, String layerTypeDescription) {
        super(databasePath, tableName, SpatialDataType.SQLITE.getTypeName(), srid, 0, 22, center[0], center[1], bounds);

        this.geometryColumn = geometryColumn;
        this.geomType = geomType;
        this.layerTypeDescription = layerTypeDescription;

        if (this.layerTypeDescription.equals(TableTypes.SPATIALVIEW.getDescription())) {
            isView = true;
        }

        createUniqueNames();

        checkType();
    }

    /**
     * Create a unique names for the table based on db path/name, table and geometry.
     */
    private void createUniqueNames() {
        String SEP = SpatialiteUtilities.UNIQUENAME_SEPARATOR;
        try {
            // uniqueNameBasedOnDbFileName = this.databaseFileName + SEP + tableName + SEP +
            // geometryColumn;
            Context context = GPApplication.getInstance();
            ResourcesManager resourcesManager = ResourcesManager.getInstance(context);
            File mapsDir = resourcesManager.getMapsDir();
            String mapsPath = mapsDir.getAbsolutePath();
            if (databasePath.startsWith(mapsPath)) {
                String relativePath = databasePath.substring(mapsPath.length());
                StringBuilder sb = new StringBuilder();
                if (relativePath.startsWith(File.separator)) {
                    relativePath = relativePath.substring(1);
                }
                sb.append(relativePath);
                sb.append(SEP);
                sb.append(tableName);
                sb.append(SEP);
                sb.append(geometryColumn);
                uniqueNameBasedOnDbFilePath = sb.toString();

            } else {
                uniqueNameBasedOnDbFilePath = databasePath + SEP + tableName + SEP + geometryColumn;
                throw new RuntimeException("SpatialVectorTable.createUniqueNames mapsPath[" + mapsPath + "] ["
                        + uniqueNameBasedOnDbFilePath + "]");
            }
        } catch (Exception e) {
            // ignore and use absolute path
            uniqueNameBasedOnDbFilePath = databasePath + SEP + tableName + SEP + geometryColumn;
        }
    }

    /**
     * Return layerTypeDescription
     *
     * @return the layerTypeDescription
     */
    public String getLayerTypeDescription() {
        return this.layerTypeDescription;
    }

    /**
     * @return the geometry column name.
     */
    public String getGeomName() {
        return geometryColumn;
    }

    /**
     * Unique name for the spatial table.
     * <p/>
     * <p><b>This is the one that should be used for the properties table (name).</b>
     * <p/>
     * <ul>
     * <li>- needed to identify one specfic field inside the whole Maps-Directory
     * <li>--- Maps-Directory:  '/storage/emulated/0/maps/'
     * <li>--- Directory inside the Maps-Directory: 'aurina/'
     * <li>-- Result : 'aurina/aurina.sqlite#topcloud#Geometry'
     * </ul>
     *
     * @return the unique name.
     */
    public String getUniqueNameBasedOnDbFilePath() {
        return uniqueNameBasedOnDbFilePath;
    }

    // /**
    // * Unique name for the spatial table based on the db file name..
    // *
    // * <ul>
    // * <li>- needed to Directory portion if the Database has been moved
    // * <li>--- Maps-Directory: ''/storage/emulated/0/maps/'
    // * <li>--- Directory inside the Maps-Directory: 'aurina/'
    // * <li>-- Result : 'aurina.sqlite#topcloud#Geometry'
    // * </ul>
    // * @return the unique name base.
    // */
    // public String getUniqueNameBasedOnDbFileName() {
    // return uniqueNameBasedOnDbFileName;
    // }

    /**
     * @return the geometry type.
     */
    public int getGeomType() {
        return geomType;
    }

    /**
     * Defines if the layer is enabled.
     *
     * @return 1 is enabled.
     */
    public int isTableEnabled() {
        return style.enabled;
    }

    /**
     * Getter for the style.
     *
     * @return the style of this table.
     */
    public Style getStyle() {
        return style;
    }

    /**
     * Returns a list of non-geometry fields of this table.
     *
     * @return list of field names.
     */
    public List<String> getTableFieldNamesList() {
        return labelList;
    }

    /**
     * Get the data type for a given field name.
     *
     * @param fieldName the field name.
     * @return the {@link DataType} or <code>null</code>.
     */
    public DataType getTableFieldType(String fieldName) {
        String type = fieldName2TypeMap.get(fieldName);
        // 1;Data-TypeTEXT || DOUBLE || INTEGER || REAL || DATE || BLOB ||
        if (type != null) {
            type = type.toUpperCase(Locale.US);
            if (type.contains("TEXT")) {
                return DataType.TEXT;
            } else if (type.contains("DOUBLE")) {
                return DataType.DOUBLE;
            } else if (type.contains("INTEGER")) {
                return DataType.INTEGER;
            } else if (type.contains("REAL")) {
                return DataType.DOUBLE;
            } else if (type.contains("DATE")) {
                return DataType.DATE;
            } else if (type.contains("BLOB")) {
                return DataType.BLOB;
            }
        }
        return null;
    }

    /**
     * Returns Primary Key Fields
     * <p/>
     * <p>- separated with ';' when more than one
     *
     * @return primary key fields.
     */
    public String getPrimaryKeyFields() {
        return primaryKeyFields;
    }

    /**
     * Returns Primary Key Field or ROWID for tables
     * <p/>
     * <p>- used in SpatialiteUtilities.buildGeometriesInBoundsQuery
     *
     * @return primary key field used as ROWID.
     */
    public String getROWID() {
        return ROWID_PK;
    }

    /**
     * Returns selected label field of this table
     * <p/>
     * <p>- to help retrieve the value for a label
     *
     * @return the label field.
     */
    public String getLabelField() {
        return labelField;
    }

    /**
     * Set selected label field of this table
     * <p/>
     * <p>- as a result of a ComboBox selection
     * <p>- to help retrieve the value for a label
     *
     * @param labelField the labe field to use.
     */
    public void setLabelField(String labelField) {
        this.labelField = labelField;
    }

    /**
     * Set Fields map of table
     * <p/>
     * <ul>
     * <li>- a second fields list will be created from non-vector Fields
     * <li>-- fields_list_non_vector [with name,type]
     * <li>-- label_list [with name]
     * <li>- sets selected field from fir charater field
     * <li>-- if none are found, first field
     * </ul>
     *
     * @param fieldName2TypeMap the fields map to set.
     * @param s_ROWID_PK        the field to replace the default ROWID when SpatialView.
     */
    public void setFieldsList(HashMap<String, String> fieldName2TypeMap, String s_ROWID_PK, int i_view_read_only) {
        this.fieldName2TypeMap = fieldName2TypeMap;
        labelField = "";
        String s_label_field_alt = "";
        if (labelList != null) {
            labelList.clear();
        } else {
            labelList = new ArrayList<String>();
        }
        if (fields_list_non_vector != null) {
            fields_list_non_vector.clear();
        } else {
            fields_list_non_vector = new LinkedHashMap<String, String>();
        }
        if (this.fieldName2TypeMap != null) {
            for (Map.Entry<String, String> fieldList : this.fieldName2TypeMap.entrySet()) {
                String s_field_name = fieldList.getKey();
                // pk: 0 || 1;Data-TypeTEXT || DOUBLE || INTEGER || REAL || DATE || BLOB ||
                // geometry-types
                // field is a primary-key = '1;Data-Type'
                // field is NOT a primary-key = '0;Data-Type'
                String s_field_type = fieldList.getValue();
                // GPLog.androidLog(-1,"SpatialVectorTable.setFieldsList["+getName()+"] field_name["+s_field_name+"] field_type["+s_field_type+"]");
                if ((!s_field_type.contains("BLOB")) && (!s_field_type.contains("POINT"))
                        && (!s_field_type.contains("LINESTRING")) && (!s_field_type.contains("POLYGON"))
                        && (!s_field_type.contains("GEOMETRYCOLLECTION"))) {
                    fields_list_non_vector.put(s_field_name, s_field_type);
                    labelList.add(s_field_name);
                    if (s_label_field_alt.equals("")) {
                        s_label_field_alt = s_field_name;
                    }
                    // s_primary_key_fields
                    if (s_field_type.contains("1;")) { // list of possible primary keys - for
                        // more that one: seperated with ';'
                        if (!primaryKeyFields.equals(""))
                            primaryKeyFields += ";";
                        primaryKeyFields += s_field_name;
                    }
                    if ((s_field_type.contains("TEXT")) && (labelField.equals(""))) {
                        // set a charater field as default
                        labelField = s_field_name;
                    }
                }
            }
            if (labelField.equals("")) {
                // if no charater field was found, set first field as default
                labelField = s_label_field_alt;
            }
            setLabelField(labelField);
            // GPLog.androidLog(-1,"SpatialVectorTable.setFieldsList["+getName()+"] label_list["+label_list.size()+"] fields_list_non_vector["+fields_list_non_vector.size()+"] fields_list["+this.fields_list.size()+"]  selected_name["+s_label_field+"] field_type["+s_primary_key_fields+"]");
        }
        // GPLog.androidLog(-1,"SpatialVectorTable.setFieldsList s_ROWID_PK["+s_ROWID_PK+"] view_read_only["+i_view_read_only
        // +"] primaryKeyFields["+primaryKeyFields+"]");
        if ((i_view_read_only == 0) || (i_view_read_only == 1))
            view_read_only = i_view_read_only; // -1=AbstractSpatialTable otherwise SpatialView
        if ((!s_ROWID_PK.equals("")) && (!s_ROWID_PK.contains("ROWID"))) {
            ROWID_PK = s_ROWID_PK;
        }
    }

    /**
     * @param style the style to use.
     */
    public void setStyle(Style style) {
        this.style = style;
        maxZoom = style.maxZoom;
        minZoom = style.minZoom;
    }

    /**
     * @return <code>true</code> if it is polygon or multipolygon.
     */
    public boolean isPolygon() {
        return isPolygon;
    }

    /**
     * @return <code>true</code> if it is line or multiline.
     */
    public boolean isLine() {
        return isLine;
    }

    /**
     * @return <code>true</code> if it is point or multipoint.
     */
    public boolean isPoint() {
        return isPoint;
    }

    /**
     * @return <code>true</code> if it is a geometrycollection.
     */
    public boolean isGeometryCollection() {
        return isGeometryCollection;
    }

    private void checkType() {
        if (checkDone) {
            return;
        }
        GeometryType TYPE = GeometryType.forValue(geomType);
        switch (TYPE) {
            case POLYGON_XY:
            case POLYGON_XYM:
            case POLYGON_XYZ:
            case POLYGON_XYZM:
            case MULTIPOLYGON_XY:
            case MULTIPOLYGON_XYM:
            case MULTIPOLYGON_XYZ:
            case MULTIPOLYGON_XYZM:
                isPolygon = true;
                break;
            case POINT_XY:
            case POINT_XYM:
            case POINT_XYZ:
            case POINT_XYZM:
            case MULTIPOINT_XY:
            case MULTIPOINT_XYM:
            case MULTIPOINT_XYZ:
            case MULTIPOINT_XYZM:
                isPoint = true;
                break;
            case LINESTRING_XY:
            case LINESTRING_XYM:
            case LINESTRING_XYZ:
            case LINESTRING_XYZM:
            case MULTILINESTRING_XY:
            case MULTILINESTRING_XYM:
            case MULTILINESTRING_XYZ:
            case MULTILINESTRING_XYZM:
                isLine = true;
                break;
            case GEOMETRYCOLLECTION_XY:
            case GEOMETRYCOLLECTION_XYM:
            case GEOMETRYCOLLECTION_XYZ:
            case GEOMETRYCOLLECTION_XYZM:
                isGeometryCollection = true;
                break;
            default:
                throw new IllegalArgumentException("No geom type for: " + TYPE + " isGeometryCollection[" + isGeometryCollection
                        + "]");
        }
        layerTypeDescription = TYPE.getDescription();
        checkDone = true;
    }

    /**
     * Create a default style.
     */
    public void makeDefaultStyle() {
        style = new Style();
        style.name = getUniqueNameBasedOnDbFilePath();
    }

    @Override
    public boolean isEditable() {
        return view_read_only < 0;
    }

}
