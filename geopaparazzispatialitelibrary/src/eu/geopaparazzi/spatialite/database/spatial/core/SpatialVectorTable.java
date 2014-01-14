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
package eu.geopaparazzi.spatialite.database.spatial.core;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteContextHolder;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryType;
import eu.geopaparazzi.spatialite.util.Style;

// https://www.gaia-gis.it/fossil/libspatialite/wiki?name=metadata-4.0

/**
 * A vector table from the spatial db.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialVectorTable {

    private final String tableName;
    private final String geometryColumn;
    private final int geomType;
    private final String srid;
    private Style style;

    private File databaseFile;

    /**
     * The database path. 
     * 
     * <p>all DatabaseHandler/Table classes should use these names
     */
    private String databasePath;

    /**
     * The database file name.
     * 
     * <p>all DatabaseHandler/Table classes should use these
     *   names
     */
    private String databaseFileName;
    
    private String s_description = "";
    // all DatabaseHandler/Table classes should use these
    // names
    private String geometryTypeDescription = "geometry";
    private double centerX; // wsg84
    private double centerY; // wsg84
    private double bounds_west; // wsg84
    private double bounds_east; // wsg84
    private double bounds_north; // wsg84
    private double bounds_south; // wsg84
    private int minZoom = 0;
    private int maxZoom = 22;
    private int defaultZoom = 17;
    private boolean checkDone = false;
    private boolean isPolygon = false;
    private boolean isLine = false;
    private boolean isPoint = false;
    private boolean isGeometryCollection = false;
    /**
     * {@link HashMap} of all fields of this table [name,type]
     */
    private HashMap<String, String> fields_list = null;

    /**
     * {@link HashMap} of non-geometry fields [name,type]
     */
    private HashMap<String, String> fields_list_non_vector = null;

    // only non-geometry fields [name]
    private List<String> label_list = null;
    // Table field to be used as a label
    private String s_label_field = "";
    // list of possible primary keys - for more that one: seperated with ';'
    private String s_primary_key_fields = "";
    private String uniqueName = ""; // file-name+table-name+field-name
    private String s_unique_name_base = ""; // file-name+table-name+field-name

    /**
     * Constructor.
     * 
     * @param databasePath the database file the table belongs to.
     * @param tableName the name of the table to use. 
     * @param geometryColumn the name of the geometry column.
     * @param geomType the geometry type as off {@link GeometryType}.
     * @param srid the srid code.
     * @param center the wgs84 center coordinates.
     * @param bounds the table bounds in wgs84.
     * @param geometryTypeDescription 
     * @param i_row_count TODO Mark, is this necessary, it seems to be unused.
     * @param i_coord_dimension TODO Mark, is this necessary, it seems to be unused.
     * @param i_spatial_index_enabled TODO Mark, is this necessary, it seems to be unused.
     * @param lastVerified TODO Mark, is this necessary, it seems to be unused.
     */
    public SpatialVectorTable( String databasePath, String tableName, String geometryColumn, int geomType, String srid,
            double[] center, double[] bounds, String geometryTypeDescription, int i_row_count, int i_coord_dimension,
            int i_spatial_index_enabled, String lastVerified ) {
        this.databasePath = databasePath;
        this.databaseFile = new File(databasePath);
        databaseFileName = databaseFile.getName();
        this.tableName = tableName;
        this.geometryColumn = geometryColumn;
        this.uniqueName = createUniqueName();

        this.geomType = geomType;
        this.geometryTypeDescription = geometryTypeDescription;

        this.srid = srid;
        this.centerX = center[0];
        this.centerY = center[1];
        this.bounds_west = bounds[0];
        this.bounds_south = bounds[1];
        this.bounds_east = bounds[2];
        this.bounds_north = bounds[3];
        checkType();
        // String s_dump = "isPoint[" + isPoint + "] isLine[" + isLine + "] isPolygon[" + isPolygon
        // + "] isGeometryCollection["
        // + isGeometryCollection + "]";
        // GPLog.androidLog(-1,"SpatialVectorTable unique_name[" + this.s_unique_name +
        // "] types["+s_dump+"]");
    }

    /**
     * Create a unique name for the table based on db path and geometry.
     *
     * @return
     */
    private String createUniqueName() {
        try {
            Context context = SpatialiteContextHolder.INSTANCE.getContext();
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
                s_unique_name_base = this.databaseFileName + File.separator + tableName + File.separator + geometryColumn;
                sb.append(File.separator);
                sb.append(tableName);
                sb.append(File.separator);
                sb.append(geometryColumn);
                return sb.toString();
            } else {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            // ignore and use absolute path
            return this.databasePath + File.separator + tableName + File.separator + geometryColumn;
        }
    }
    // -----------------------------------------------
    /**
      * Check of the Bounds of all the Vector-Tables collected in this class
      * Goal: when painting the Geometries: check of viewport is inside these bounds
      * - if the Viewport is outside these Bounds: all Tables can be ignored
      * -- this is called when the Tables are created
      * @param boundsCoordinates [as wsg84]
      * @return true if the given bounds are inside the bounds of the all the Tables ; otherwise false
      */
    public boolean checkBounds( double[] boundsCoordinates ) {
        boolean b_rc = false;
        if ((boundsCoordinates[0] >= this.bounds_west) && (boundsCoordinates[1] >= this.bounds_south)
                && (boundsCoordinates[2] <= this.bounds_east) && (boundsCoordinates[3] <= this.bounds_north)) {
            b_rc = true;
        }
        return b_rc;
    }
    // -----------------------------------------------
    public float[] getTableBounds() {
        float w = (float) this.bounds_west;
        float s = (float) this.bounds_south;
        float e = (float) this.bounds_east;
        float n = (float) this.bounds_north;
        return new float[]{n, s, e, w};
    }
    // -----------------------------------------------
    /**
      * Return long name of map/file
      *
      * <p>default: file name with path and extention
      * <p>mbtiles : will be a '.mbtiles' sqlite-file-name
      * <p>map : will be a mapforge '.map' file-name
      *
      * @return file_map as File
      */
    public String getFileNamePath() {
        return this.databasePath; // file_map.getAbsolutePath();
    }
    // -----------------------------------------------
    /**
      * Return short name of map/file
      *
      * <p>default: file name without path but with extention
      *
      * @return file_map.getAbsolutePath();
      */
    public String getFileName() {
        return this.databaseFileName; // file_map.getName();
    }
    // -----------------------------------------------
    /**
      * Return geometryTypeDescription
      *
      * @return the geometryTypeDescription
      */
    public String getGeometryTypeDescription() {
        return this.geometryTypeDescription;
    }
    // -----------------------------------------------
    /**
      * Return String of bounds [wms-format]
      *
      * <p>x_min,y_min,x_max,y_max
      *
      * @return bounds formatted using wms format
      */
    public String getBounds_toString() {
        return bounds_west + "," + bounds_south + "," + bounds_east + "," + bounds_north;
    }
    // -----------------------------------------------
    /**
      * Return String of Map-Center with default Zoom
      *
      * <p>x_position,y_position,default_zoom
      *
      * @return center formatted using mbtiles format
      */
    public String getCenter_toString() {
        return centerX + "," + centerY + "," + defaultZoom;
    }

    /**
      * Return map-file as 'File'
      *
      * <p>if the class does not fail, this file exists
      * <p>mbtiles : will be a '.mbtiles' sqlite-file
      * <p>map : will be a mapforge '.map' file
      *
      * @return file_map as File
      */
    public File getFile() {
        return this.databaseFile;
    }
    // -----------------------------------------------
    /**
      * Return Min Zoom
      *
      * <p>default :  0
      * <p>mbtiles : taken from value of metadata 'minzoom'
      * <p>map : value is given in 'StartZoomLevel'
      *
      * @return integer minzoom
      */
    public int getMinZoom() {
        return minZoom;
    }
    // -----------------------------------------------
    /**
      * Return Max Zoom
      *
      * <p>default :  22
      * <p>mbtiles : taken from value of metadata 'maxzoom'
      * <p>map : value not defined, seems to calculate bitmap from vector data [18]
      *
      * @return integer maxzoom
      */
    public int getMaxZoom() {
        return maxZoom;
    }
    // -----------------------------------------------
    /**
      * Return Min/Max Zoom as string
      *
      * <p>default :  1-22
      * <p>mbtiles : taken from value of metadata 'min/maxzoom'
      *
      * @return String min/maxzoom
      */
    public String getZoom_Levels() {
        return getMinZoom() + "-" + getMaxZoom();
    }
    public String getName() {
        return tableName; // table_name
    }

    public String getGeomName() {
        return geometryColumn;
    }
    /**
      * Unique-Name of Geometry Field inside 'sdcard/maps' directory
      * 
      * <ul>
      * <li>- needed to identify one specfic field inside the whole Maps-Directory
      * <li>-- Sample: '/storage/emulated/0/maps/aurina/aurina.sqlite/topcloud/Geometry'
      * <li>--- Maps-Directory:  ''/storage/emulated/0/maps/'
      * <li>--- Directory inside the Maps-Directory: 'aurina/'
      * <li>--- UniqueNameBase : 'aurina.sqlite/topcloud/Geometry'
      * <li>-- Result : 'aurina/aurina.sqlite/topcloud/Geometry'
      * </ul>
      */
    public String getUniqueName() {
        return this.uniqueName;
    }
    /**
      * Unique-Name-Base of Database inside 'sdcard/maps' directory
      * 
      * <ul>
      * <li>- needed to Directory portion if the Database has been moved
      * <li>-- Sample: '/storage/emulated/0/maps/aurina/aurina.sqlite/topcloud/Geometry'
      * <li>--- Maps-Directory:  ''/storage/emulated/0/maps/'
      * <li>--- Directory inside the Maps-Directory: 'aurina/'
      * <li>-- Result : 'aurina.sqlite/topcloud/Geometry'
      * </ul>
      */
    public String getUniqueNameBase() {
        return this.s_unique_name_base;
    }
    public int getGeomType() {
        return geomType;
    }
    public String getSrid() {
        return srid;
    }
    public int IsStyle() {
        return style.enabled;
    }
    public Style getStyle() {
        return style;
    }

    /**
      * Returns a list of non-vector fields of this table
      * 
      * <p>to fill a ComboBox
      */
    public List<String> getLabelList() {
        return label_list;
    }

    // -----------------------------------------------
    /**
      * Returns Primary Key Fields
      * 
      * <p>- seperated with ';' when more than one
      */
    public String getPrimaryKeyFields() {
        return s_primary_key_fields;
    }
    // -----------------------------------------------
    /**
      * Returns selected label field of this table
      * 
      * <p>- to help retrieve the value for a label
      */
    public String getLabelField() {
        return s_label_field;
    }
    // -----------------------------------------------
    /**
      * Set selected label field of this table
      * 
      * <p>- as a result of a ComboBox selection
      * <p>- to help retrieve the value for a label
      */
    public void setLabelField( String s_label_field ) {
        this.s_label_field = s_label_field;
    }
    // -----------------------------------------------
    /**
      * Set Fields list of table
      * 
      * <ul>
      * <li>- a second fields list will be created from non-vector Fields
      * <li>-- fields_list_non_vector [with name,type]
      * <li>-- label_list [with name]
      * <li>- sets selected field from fir charater field
      * <li>-- if none are found, first field
      * </ul>
      */
    public void setFieldsList( HashMap<String, String> fields_list ) {
        this.fields_list = fields_list;
        s_label_field = "";
        String s_label_field_alt = "";
        if (label_list != null) {
            label_list.clear();
        } else {
            label_list = new ArrayList<String>();
        }
        if (fields_list_non_vector != null) {
            fields_list_non_vector.clear();
        } else {
            fields_list_non_vector = new LinkedHashMap<String, String>();
        }
        if (this.fields_list != null) {
            for( Map.Entry<String, String> field_list : this.fields_list.entrySet() ) {
                String s_field_name = field_list.getKey();
                // pk: 0 || 1;Data-TypeTEXT || DOUBLE || INTEGER || REAL || DATE || BLOB ||
                // geometry-types
                // field is a primary-key = '1;Data-Type'
                // field is NOT a primary-key = '0;Data-Type'
                String s_field_type = field_list.getValue();
                // GPLog.androidLog(-1,"SpatialVectorTable.setFieldsList["+getName()+"] field_name["+s_field_name+"] field_type["+s_field_type+"]");
                if ((s_field_type.indexOf("POINT") == -1) && (s_field_type.indexOf("LINESTRING") == -1)
                        && (s_field_type.indexOf("POLYGON") == -1) && (s_field_type.indexOf("GEOMETRYCOLLECTION") == -1)) {
                    fields_list_non_vector.put(s_field_name, s_field_type);
                    label_list.add(s_field_name);
                    if (s_label_field_alt.equals("")) {
                        s_label_field_alt = s_field_name;
                    }
                    // s_primary_key_fields
                    if (s_field_type.indexOf("1;") != -1) { // list of possible primary keys - for
                                                            // more that one: seperated with ';'
                        if (!s_primary_key_fields.equals(""))
                            s_primary_key_fields += ";";
                        s_primary_key_fields += s_field_name;
                    }
                    if ((s_field_type.indexOf("TEXT") != -1) && (s_label_field.equals(""))) {
                        // set a charater field as default
                        s_label_field = s_field_name;
                    }
                }
            }
            if (s_label_field.equals("")) {
                // if no charater field was found, set first field as default
                s_label_field = s_label_field_alt;
            }
            // GPLog.androidLog(-1,"SpatialVectorTable.setFieldsList["+getName()+"] label_list["+label_list.size()+"] fields_list_non_vector["+fields_list_non_vector.size()+"] fields_list["+this.fields_list.size()+"]  selected_name["+s_label_field+"] field_type["+s_primary_key_fields+"]");
        }
    }
    public void setStyle( Style style ) {
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
        switch( TYPE ) {
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
        geometryTypeDescription = TYPE.getDescription();
        checkDone = true;
    }

    public void makeDefaultStyle() {
        style = new Style();
        style.name = getUniqueName();
    }
}
