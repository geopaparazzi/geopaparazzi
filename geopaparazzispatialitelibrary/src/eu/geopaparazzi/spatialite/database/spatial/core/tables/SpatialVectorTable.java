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
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.DataType;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.TableTypes;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteUtilities;
import eu.geopaparazzi.spatialite.database.spatial.util.Style;

import jsqlite.Database;

// https://www.gaia-gis.it/fossil/libspatialite/wiki?name=metadata-4.0

/**
 * A vector table from the spatial db.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SpatialVectorTable extends AbstractSpatialTable implements Serializable {
    private static final long serialVersionUID = 1L;

    private Style style;

    private boolean checkDone = false;
    private boolean isPolygon = false;
    private boolean isLine = false;
    private boolean isPoint = false;
    private boolean isGeometryCollection = false;

    private String uniqueNameBasedOnDbFilePath = "";
    // private String uniqueNameBasedOnDbFileName = "";

    /**
     * Constructor.
     *
     * @param spatialite_db the class 'Database' connection [will be null].
     * @param vector_key major Parameters  needed for creation in AbstractSpatialTable.
     * @param vector_value minor Parameters needed for creation in AbstractSpatialTable.
     */
    public SpatialVectorTable(Database spatialite_db,String vector_key,String  vector_value) 
    {
        super(spatialite_db,vector_key,vector_value);
        if (isValid())
        { //only continue if valid
         createUniqueNames();
         checkType();
        }
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
            GPLog.error(this, null, e);
            // ignore and use absolute path
            uniqueNameBasedOnDbFilePath = databasePath + SEP + tableName + SEP + geometryColumn;
        }
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
     * Defines if the layer is enabled.
     *
     * @return 1 is enabled.
     */
    public int isTableEnabled() {
        return style.enabled;
    }

    /**
     * Getter for the geopaparazzi-style.
     *
     * @return the geopaparazzi-style of this table.
     */
    public Style getStyle() {
        return style;
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
                throw new IllegalArgumentException("["+getLayerTypeDescription()+"."+getTableName()+"."+getGeomName()+"] No geom type for: " + TYPE + " isGeometryCollection[" + isGeometryCollection
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
    public double[] longLat2Srid(double lon, double lat) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean isEditable() {
        return view_read_only < 0;
    }

}
