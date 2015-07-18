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

import com.vividsolutions.jts.geom.Envelope;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.TableTypes;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteUtilities;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite;
import eu.geopaparazzi.library.util.LibraryConstants;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * Spatial table interface.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public abstract class AbstractSpatialTable implements Serializable {
    private static final long serialVersionUID = 1L;
    protected String layerTypeDescription = "geometry";
    protected String geometryColumn;
    protected int geomType;
    // only non-geometry fields [name]
    protected List<String> labelList = null;
    // Table field to be used as a label
    protected String labelField = "";
    // list of possible primary keys - for more that one: seperated with ';'
    protected String primaryKeyFields = "";
        /**
     * {@link HashMap} of all fields of this table [name,type]
     */
    protected HashMap<String, String> fieldName2TypeMap = null;

    /**
     * {@link HashMap} of non-geometry fields [name,type]
     */
    protected HashMap<String, String> fields_list_non_vector = null;
    /**
     * Active Connection to a  Spatialite Database
    * <p/>
     * <p>Indented for general use for Transformation etc.
     */
    private Database dbSpatialite=null;
    /**
     * Table type description.
     */
    protected String tableTypeDescription;
    
    /**
     * Flag to define the validity of the tavle.
     */
    protected boolean isTableValid = false;

    /**
     * The database path.
     */
    protected String databasePath;
    /**
     * The database file.
     */
    protected File databaseFile;
    /**
     * The database file name.
     */
    protected String databaseFileName;
    /**
     * The database file name without extension.
     */
    protected String databaseFileNameNoExtension;
    /**
     * A name for the table.
     */
    protected String tableName;
    /**
     * A name for the table-style name.
     */
    protected String styleName;
    /**
     * A description.
     */
    protected String description;
    /**
     * A title.
     */
    protected String title;
    /**
     * The table srid.
     */
    protected String srid;
    /**
     * The table center longitude.
     */
    protected double centerX;
    /**
     * The table center latitude.
     */
    protected double centerY;
    /**
     * Western table bound.
     */
    protected double boundsWest;
    /**
     * Southern table bound.
     */
    protected double boundsSouth;
    /**
     * Eastern table bound.
     */
    protected double boundsEast;
    /**
     * Northern table bound.
     */
    protected double boundsNorth;
    /**
     * Min allowed zoom.
     */
    protected int minZoom = 0;
    /**
     * Max allowed zoom.
     */
    protected int maxZoom = 22;
    /**
     * The default zoom for the table.
     */
    protected int defaultZoom = 17;
    
    protected String tileQuery;

    /**
     * The map type.
     */
    protected String mapType;

    /**
     * If true, then the table is a view.
     */
    protected boolean isView = false;
    
    // AbstractSpatialTable=always ROWID ; SpatialView: can also be ROWID - but something else
    protected String ROWID_PK = "ROWID";
    // AbstractSpatialTable=-1 ; SpatialView: read_only=0 ; writable=1
    protected int view_read_only = -1;

    /**
     * Constructor.
     *
     * @param databasePath the db path.
     * @param tableName    a name for the table.
     * @param styleName    a name for the table-style.
     * @param mapType      the map type.
     * @param srid         srid of the table.
     * @param minZoom      min zoom.
     * @param maxZoom      max zoom.
     * @param centerX      center x.
     * @param centerY      center y.
     * @param bounds       the bounds as [w,s,e,n]
     */
    public AbstractSpatialTable(Database spatialite_db, String mapType, String vector_key,String  vector_value) 
    {
        setDatabase(spatialite_db);
        this.mapType = mapType;
        int i_rc=parse_vector_key_value(vector_key,vector_value);
        if (i_rc != 0)
        { // error
         // i_rc=1 : unexpected size for 'vector_key'
         // i_rc=2 : unknown 'layerTypeDescription'
         // i_rc=3 : unexpected size for 'vector_value', to small
         // i_rc=4 : unexpected size for 'bounds', not 4
         // i_rc=5 : 'spatial_index' not 1 where needed for 'SpatialTable'+'SpatialView'
         // i_rc=6 : no spatialite Database connection  for 'SpatialTable'+'SpatialView'
         isTableValid=false;
        }
     }
    /**
     * Retrieve an active spatialite Connection
     * 
     * @param i_parm 0:[default] retrieve only ; 1=create Memory-db if not active
     * @return dbSpatialite for general use
     */
    public Database getDatabase(int i_parm) {
        if (dbSpatialite == null)
        { 
         if (i_parm == 1)
         { // TODO: create Memory Database with Spatialite 'SELECT InitSpatialMetadata(1);'
         }
        }
        return dbSpatialite;
    }
    
    /**
     * Set an active spatialite Connection for genral use
     * - should only be done when sure that it a valid Spatialite Database ( version >= 4)
     * -- from MbTiles, this will be null
     * @return dbSpatialite
     */
    public void setDatabase(Database spatialite_db) {
        if ((this.dbSpatialite == null) && (spatialite_db != null))
        { 
         this.dbSpatialite=spatialite_db;
         this.databasePath = this.dbSpatialite.getFilename() ;
         this.databaseFile = new File(databasePath);
         this.databaseFileName = databaseFile.getName();
         this.databaseFileNameNoExtension = databaseFileName.substring(0, databaseFileName.lastIndexOf("."));
        }
    }
    /**
     * Parse the given vector key and values
     * Goal is to replace the multiple parsing that was done in about 4-5 different functions
     * <p/>
     * <p>[GeneralQueriesPreparer] Documentation of vector_key /  vector_value   
     * <p>[SpatialRasterTable] 'RasterLite2'
     * <p>[SpatialVectorTable] 'SpatialTable'/ 'SpatialView'
     * <p>[SpatialRasterTable] 'MBTiles' 
     * @return integer i_rc.
     */
    protected int parse_vector_key_value(String vector_key,String  vector_value) 
    {
     int i_rc=2; // unknown 'layerTypeDescription'
     double[] boundsCoordinates = new double[]{0.0, 0.0, 0.0, 0.0};
     double[] centerCoordinate = new double[]{0.0, 0.0};
     HashMap<String, String> fields_list = new HashMap<String, String>();
     int i_geometry_type = 0;
     int i_view_read_only = -1;
     double horz_resolution = 0.0;
     int i_spatial_index_enabled=0;
     int i_default_zoom=this.defaultZoom;
     int i_min_zoom=this.minZoom;
     int i_max_zoom=this.maxZoom;
     String s_view_read_only = "";
     String[] sa_string = vector_key.split(";");
     this.tileQuery="";
     if (sa_string.length == 6) 
     {
      String layerType = sa_string[2];
      if ((this.layerTypeDescription.equals(TableTypes.SPATIALTABLE.getDescription())) ||
           (this.layerTypeDescription.equals(TableTypes.SPATIALVIEW.getDescription())) ||
           (this.layerTypeDescription.equals(TableTypes.GPKGVECTOR.getDescription())) ||
           (this.layerTypeDescription.equals(TableTypes.GPKGRASTER.getDescription())) ||
           (this.layerTypeDescription.equals(SpatialDataType.MBTILES.getTypeName())) ||
           (this.layerTypeDescription.equals(SpatialDataType.MAP.getTypeName())) ||
           (this.layerTypeDescription.equals(SpatialDataType.MAPURL.getTypeName())))
      {
       this.layerTypeDescription=layerType;
       this.isTableValid = true;
       i_rc=0;
      }
      else
       return  i_rc;
      this.tableName = sa_string[0];
      String geometry_column = sa_string[1];      
      // For SpatialTable/Views
      String s_ROWID_PK = sa_string[3];
      // For SpatialTable/Views
      s_view_read_only = sa_string[4];
      // TODO: remove this from vector_key and store in vector_value
      String s_style_name = sa_string[5]; // style_name [for RasterLite2, otherwise reserved for future use]
      sa_string = vector_value.split(";");
      if (sa_string.length >= 7) 
      { // We may be overriding some of these values, before setting the final values
      // Warning: to NOT reuse 'sa_string', we may be retrieng more data
       String s_geometry_type = sa_string[0];  // 1= POINT / OR min_zoom
       String s_coord_dimension = sa_string[1]; // 2= XY / OR max_zoom
       String s_srid = sa_string[2]; // 4326
       String s_spatial_index_enabled = sa_string[3]; // 0
       // -1;-75.5;18.0;-71.06667;20.08333;2013-12-24T16:32:14.000000Z
       // 0 = not possible as sub-query - but also not needed
       String s_row_count = sa_string[4]; 
       String s_bounds = sa_string[5]; // -75.5;18.0;-71.06667;20.08333
       // For 'MbTiles' : TileQuery
       String s_last_verified = sa_string[6]; // 2013-12-24T16:32:14.000000Z
       String[] sa_bounds = s_bounds.split(",");
       if ((sa_bounds.length == 4)  || (sa_bounds.length == 6))
       {
        try 
        {
         boundsCoordinates[0] = Double.parseDouble(sa_bounds[0]);
         boundsCoordinates[1] = Double.parseDouble(sa_bounds[1]);
         boundsCoordinates[2] = Double.parseDouble(sa_bounds[2]);
         boundsCoordinates[3] = Double.parseDouble(sa_bounds[3]);
         if ((sa_string.length == 7) && 
              ((this.layerTypeDescription.equals(SpatialDataType.MAP.getTypeName())) ||
               (this.layerTypeDescription.equals(SpatialDataType.MAPURL.getTypeName())) ||
               (this.layerTypeDescription.equals(SpatialDataType.MBTILES.getTypeName()))))
         { // MbTiles only
          centerCoordinate[0] = Double.parseDouble(sa_bounds[4]);
          centerCoordinate[1] = Double.parseDouble(sa_bounds[5]);
          i_default_zoom = Integer.parseInt(sa_bounds[6]);
         }
        } 
        catch (NumberFormatException e) 
        {
        }
        if (!s_srid.equals(LibraryConstants.SRID_WGS84_4326)) 
        { // Transform into wsg84 if needed
         SpatialiteUtilities.collectBoundsAndCenter(null, s_srid, centerCoordinate, boundsCoordinates);
        } 
        else 
        {
         if ((centerCoordinate[0]  == 0.0) && (centerCoordinate[1] == 0.0))
         {
          centerCoordinate[0] = boundsCoordinates[0] + (boundsCoordinates[2] - boundsCoordinates[0]) / 2;
          centerCoordinate[1] = boundsCoordinates[1] + (boundsCoordinates[3] - boundsCoordinates[1]) / 2;
         }
        }
       }
       else
        i_rc=4;
       if (i_rc == 0)
       {
        checkAndAdaptDatabaseBounds(boundsCoordinates, null);
        // Vector-Specfic tasks
        if ((this.layerTypeDescription.equals(TableTypes.SPATIALTABLE.getDescription())) ||
             (this.layerTypeDescription.equals(TableTypes.SPATIALVIEW.getDescription())) ||
             (this.layerTypeDescription.equals(TableTypes.GPKGVECTOR.getDescription())))
        {
         this.geometryColumn = geometry_column;
         this.geomType = geomType;
         i_view_read_only = Integer.parseInt(s_view_read_only);
         i_spatial_index_enabled = Integer.parseInt(s_spatial_index_enabled); // 0=no 1=yes
         i_geometry_type = Integer.parseInt(s_geometry_type);
         GeometryType geometry_type = GeometryType.forValue(i_geometry_type);
         s_geometry_type = geometry_type.toString();  
         if ((this.layerTypeDescription.equals(TableTypes.SPATIALTABLE.getDescription())) ||
              (this.layerTypeDescription.equals(TableTypes.SPATIALVIEW.getDescription())))
         {
          if (this.layerTypeDescription.equals(TableTypes.SPATIALVIEW.getDescription()))
          {
           isView = true;
          }
         }
         if (this.layerTypeDescription.equals(TableTypes.GPKGVECTOR.getDescription()))
         { // TODO: ?? SpatialIndex ?? - has its own version now - this may change
          i_view_read_only = 0; // always       
         }
         if (i_spatial_index_enabled  != 1)
         {
          i_rc=5; // 'spatial_index' not 1 where needed for 'SpatialTable'+'SpatialView' 
         }   
         if (this.dbSpatialite  == null)
         {
          i_rc=6; // no spatialite Database connection  for 'SpatialTable'+'SpatialView'
         }  
        }
        // Raster-Specfic tasks
        if (this.layerTypeDescription.equals(SpatialDataType.RASTERLITE2.getTypeName()))
        {
        }
        if ((this.layerTypeDescription.equals(TableTypes.GPKGRASTER.getDescription())) ||
             (this.layerTypeDescription.equals(SpatialDataType.MAP.getTypeName())) ||
             (this.layerTypeDescription.equals(SpatialDataType.MAPURL.getTypeName())) ||
             (this.layerTypeDescription.equals(SpatialDataType.MBTILES.getTypeName())))
        {
         this.minZoom = Integer.parseInt(s_geometry_type);
         this.maxZoom = Integer.parseInt(s_coord_dimension);
         if (this.layerTypeDescription.equals(TableTypes.GPKGRASTER.getDescription()))
         {
         }
         if ((this.layerTypeDescription.equals(SpatialDataType.MAP.getTypeName())) ||
             (this.layerTypeDescription.equals(SpatialDataType.MAPURL.getTypeName())) ||
             (this.layerTypeDescription.equals(SpatialDataType.MBTILES.getTypeName())))
         {
          if (this.dbSpatialite  == null)
          {
            // TODO: ? create MemoryDB ?
          } 
          if (sa_string.length > 7) 
          { // MbTiles does NOT use class 'Database' ; MAP and MAPURL uses any Database-Connection
           this.databasePath = sa_string[7] ;
           this.databaseFile = new File(this.databasePath);
           this.databaseFileName = this.databaseFile.getName();
           this.databaseFileNameNoExtension = this.databaseFileName.substring(0, this.databaseFileName.lastIndexOf("."));
          }
         }
         if (this.layerTypeDescription.equals(SpatialDataType.MBTILES.getTypeName()))
         { //vector_value=this.minZoom+";"+this.maxZoom+";4326;"+defaultZoom+";0;"+s_bounds+";?,?,?";
          this.tileQuery=s_last_verified;
          this.defaultZoom = Integer.parseInt(s_spatial_index_enabled);
          s_srid="3857"; // why?
          // else: this.tileQuery = "select " + name + " from " + dbPath + " where zoom_level = ? AND tile_column = ? AND tile_row = ?";
         }
        }
        if (this.layerTypeDescription.equals(SpatialDataType.MAP.getTypeName()))
        {
         s_srid="3857"; // why?
        }         
        if (this.layerTypeDescription.equals(SpatialDataType.MAPURL.getTypeName()))
        {
         s_srid="3857"; // why?
        }   
        if (i_rc == 0)
        { // Set final results
         this.srid = s_srid;
         this.view_read_only=i_view_read_only;
         this.ROWID_PK=s_ROWID_PK;
         this.centerX = centerCoordinate[0];
         this.centerY = centerCoordinate[1];
         this.boundsWest = boundsCoordinates[0];
         this.boundsSouth = boundsCoordinates[1];
         this.boundsEast = boundsCoordinates[2];
         this.boundsNorth = boundsCoordinates[3];
         this.defaultZoom=i_default_zoom;
         this.minZoom=i_min_zoom;
         this.maxZoom=i_max_zoom;
         this.styleName = s_style_name; // for RasterLite2: Style-Name (only 1) or empty, resurved for others
         if ((this.layerTypeDescription.equals(TableTypes.SPATIALTABLE.getDescription())) ||
             (this.layerTypeDescription.equals(TableTypes.SPATIALVIEW.getDescription())) ||
             (this.layerTypeDescription.equals(TableTypes.GPKGVECTOR.getDescription())))
         { // this will set the final values needed - NEVER for MbTiles,Map,Mapurl
          try 
          {
           setFieldsList(DaoSpatialite.collectTableFields(this.dbSpatialite, this.tableName), this.ROWID_PK, this.view_read_only);
          } 
          catch (jsqlite.Exception e_stmt) 
          {
           GPLog.error("AbstractSpatialTable", "parse_vector_key_value table[" +this.tableName+ "] db[" + dbSpatialite.getFilename() + "]", e_stmt);
          }          
         }
        }
       }  
      }
      else
       i_rc=3;
     }
     else
      i_rc=1;
     return i_rc;
    }
    
    /**
     * Is the table file considered valid?
     * <p/>
     * <br>- metadata table exists and has data
     * <br>- 'tiles' is either a table or a view and the correct fields exist
     * <br>-- if a view: do the tables map and images exist with the correct fields
     * <br>checking is done once when the 'metadata' is retrieved the first time [fetchMetadata()]
     *
     * @return true if valid, otherwise false
     */
    public boolean isValid() {
        return isTableValid;
    }
        /**
     * Checks (and adapts) the overall database bounds based on the passed coordinates.
     * <p/>
     * <p>Goal: when painting the Geometries: check of viewport is inside these bounds.
     * <br>- if the Viewport is outside these Bounds: all Tables can be ignored
     * <br>-- this is called when the Tables are created
     *
     * @param boundsCoordinates bounds to check against the overall.
     */
    private void checkAndAdaptDatabaseBounds(double[] boundsCoordinates, int[] zoomLevels) {
        if ((this.boundsWest == 0.0) && (this.boundsSouth == 0.0) && (this.boundsEast == 0.0) && (this.boundsNorth == 0.0)) {
            this.boundsWest = boundsCoordinates[0];
            this.boundsSouth = boundsCoordinates[1];
            this.boundsEast = boundsCoordinates[2];
            this.boundsNorth = boundsCoordinates[2];
        } else {
            if (boundsCoordinates[0] < this.boundsWest)
                this.boundsWest = boundsCoordinates[0];
            if (boundsCoordinates[1] < this.boundsSouth)
                this.boundsSouth = boundsCoordinates[1];
            if (boundsCoordinates[2] > this.boundsEast)
                this.boundsEast = boundsCoordinates[2];
            if (boundsCoordinates[3] < this.boundsNorth)
                this.boundsNorth = boundsCoordinates[3];
        }
        centerX = this.boundsWest + (this.boundsEast - this.boundsWest) / 2;
        centerY = this.boundsSouth + (this.boundsNorth - this.boundsSouth) / 2;
        if ((zoomLevels != null) && (zoomLevels.length == 2)) {
            if ((this.minZoom == 0) && (this.maxZoom == 0)) {
                this.minZoom = zoomLevels[0];
                this.maxZoom = zoomLevels[1];
            } else {
                if (zoomLevels[0] < this.minZoom)
                    this.minZoom = zoomLevels[0];
                if (zoomLevels[1] > this.maxZoom)
                    this.maxZoom = zoomLevels[1];
            }
        }
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
     * Returns a list of non-geometry fields of this table.
     *
     * @return list of field names.
     */
    public List<String> getTableFieldNamesList() {
        return labelList;
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
     * Return layerTypeDescription
     * <p/>
     * <p>[SpatialRasterTable] 'SpatialTable'
     * <p>[SpatialVectorTable] 'SpatialView'
     * <p>[SpatialRasterTable] 'MBTiles'    
     * <p>[MapTable] (Mapsforge) 'Map'   
     * <p>[CustomTileTable] 'Mapurl'       
     * TODO: ? add to 'TableTypes':
     * - added as GPKGRASTER/GPKGVECTOR
     * <p>[SpatialRasterTable] 'GeoPackage_tiles'
     * <p>[SpatialVectorTable] 'GeoPackage_features'
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
     * @return the geometry type.
     */
    public int getGeomType() {
        return geomType;
    }
    /**
     * Get the tile retrieve query with place holders for zoom, column and row.
     *
     * @return the query to use for this raster set.
     */
    public String getTileQuery() {
        return tileQuery;
    }
    /**
     * Return the absolute path of the database.
     * <p/>
     * <p>default: file name with path and extention
     * <p>mbtiles : will be a '.mbtiles' sqlite-file-name
     * <p>map : will be a mapforge '.map' file-name
     *
     * @return the absolute database path.
     */
    public String getDatabasePath() {
        return databasePath;
    }

    /**
     * Return database {@link File}.
     *
     * @return the database file.
     */
    public File getDatabaseFile() {
        return databaseFile;
    }

    /**
     * Getter for the table's srid.
     *
     * @return the table srid.
     */
    public String getSrid() {
        return srid;
    }

    /*
     * Convert a longitude and latitude to the table's
     * original SRID.
     */
    public abstract double[] longLat2Srid(double lon, double lat);

    /**
     * Returns the database file name with extension.
     *
     * @return the database file name with extension.
     */
    public String getFileName() {
        return databaseFileName;
    }

    /**
     * Returns the database file name without extension.
     *
     * @return the database file name without extension.
     */
    public String getFileNameNoExtension() {
        return databaseFileNameNoExtension;
    }

    /**
     * Getter for the table name.
     *
     * @return the name of the {@link AbstractSpatialTable}.
     */
    public String getTableName() {
        return tableName;
    }
    /**
     * Getter for the style name [for RasterLite2].
     * <p/>
     * <p> RasterLite2 Style logic: basic rules
     * <p> - only one style supported. Where than more than one: others will be ignored
     * <p>default :  '%' [Wildcard: RasterLite2 will return the first Style for this Raster] - expermintal changes in RasterLite2 lib
     * <p>TODO : set to '', if Raster should be shown without a Style [rl2_GetMapImageFromRaster will set this to 'default']
     * <p>Both Raster and Vectors can have a table-style]
     *
     * @return the name of the {@link AbstractSpatialTable}.
     */
    public String getStyleName() {
        return styleName;
    }

    /**
     * Return type of map/file
     * <p/>
     * <p>raster: can be different: mbtiles,db,sqlite,gpkg
     * <p>mbtiles : mbtiles
     * <p>map : map
     *
     * @return s_name as short name of map/file
     */
    public String getMapType() {
        return mapType;
    }

    /**
     * Returns the title
     *
     * @return a title.
     */
    public String getTitle() {
        if (title != null) {
            return title;
        }
        return getTableName();
    }

    /**
     * Returns a description.
     *
     * @return a description.
     */
    public String getDescription() {
        if (description != null) {
            return description;
        }
        return "map_type[" + getMapType() + "] table_name[" + getTableName() + "] srid[" + getSrid() + "] bounds["
                + getBoundsAsString() + "] center[" + getCenterAsString() + "]";
    }

    /**
     * Return Min Zoom.
     *
     * @return integer minzoom.
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     * Return Max Zoom.
     *
     * @return integer maxzoom.
     */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
     * Retrieve Zoom level
     *
     * @return defaultZoom
     */
    public int getDefaultZoom() {
        return defaultZoom;
    }

    /**
     * Set default Zoom level
     *
     * @param defaultZoom desired Zoom level
     */
    public void setDefaultZoom(int defaultZoom) {
        this.defaultZoom = defaultZoom;
    }

    /**
     * Return Min/Max Zoom as string
     *
     * @return String min/maxzoom
     */
    public String getMinMaxZoomLevelsAsString() {
        return getMinZoom() + "-" + getMaxZoom();
    }

    /**
     * Return West X Value [Longitude]
     * <p/>
     * <p>default :  -180.0 [if not otherwise set]
     * <p>mbtiles : taken from 1st value of metadata 'bounds'
     *
     * @return double of West X Value [Longitude]
     */
    public double getMinLongitude() {
        return boundsWest;
    }

    /**
     * Return South Y Value [Latitude]
     * <p/>
     * <p>default :  -85.05113 [if not otherwise set]
     * <p>mbtiles : taken from 2nd value of metadata 'bounds'
     *
     * @return double of South Y Value [Latitude]
     */
    public double getMinLatitude() {
        return boundsSouth;
    }

    /**
     * Return East X Value [Longitude]
     * <p/>
     * <p>default :  180.0 [if not otherwise set]
     * <p>mbtiles : taken from 3th value of metadata 'bounds'
     *
     * @return double of East X Value [Longitude]
     */
    public double getMaxLongitude() {
        return boundsEast;
    }

    /**
     * Return North Y Value [Latitude]
     * <p/>
     * <p>default :  85.05113 [if not otherwise set]
     * <p>mbtiles : taken from 4th value of metadata 'bounds'
     *
     * @return double of North Y Value [Latitude]
     */
    public double getMaxLatitude() {
        return boundsNorth;
    }

    /**
     * Return Center X Value [Longitude]
     * <p/>
     * <p>default : center of bounds
     * <p>mbtiles : taken from 1st value of metadata 'center'
     *
     * @return double of X Value [Longitude]
     */
    public double getCenterX() {
        return centerX;
    }

    /**
     * Return Center Y Value [Latitude]
     * <p/>
     * <p>default : center of bounds
     * <p>mbtiles : taken from 2nd value of metadata 'center'
     *
     * @return double of Y Value [Latitude]
     */
    public double getCenterY() {
        return centerY;
    }

    /**
     * Get table bounds.
     *
     * @return the table bounds as [n, s, e, w].
     */
    public float[] getTableBounds() {
        float w = (float) boundsWest;
        float s = (float) boundsSouth;
        float e = (float) boundsEast;
        float n = (float) boundsNorth;
        return new float[]{n, s, e, w};
    }

    /**
     * Get table envelope.
     *
     * @return the {@link Envelope}.
     */
    public Envelope getTableEnvelope() {
        float w = (float) boundsWest;
        float s = (float) boundsSouth;
        float e = (float) boundsEast;
        float n = (float) boundsNorth;
        return new Envelope(w, e, s, n);
    }

    /**
     * Check the supplied bounds against the current table bounds.
     *
     * @param boundsCoordinates as wsg84 [w,s,e,n]
     * @return <code>true</code> if the given bounds are inside the bounds the current table.
     */
    public boolean checkBounds(double[] boundsCoordinates) {
        if ((boundsCoordinates[0] >= boundsWest) && (boundsCoordinates[1] >= this.boundsSouth)
                && (boundsCoordinates[2] <= boundsEast) && (boundsCoordinates[3] <= this.boundsNorth)) {
            return true;
        }
        return false;
    }

    /**
     * Return String of bounds [wms-format]
     * <p/>
     * <p>x_min,y_min,x_max,y_max
     *
     * @return bounds formatted using wms format [w,s,e,n]
     */
    public String getBoundsAsString() {
        return boundsWest + "," + boundsSouth + "," + boundsEast + "," + boundsNorth;
    }

    /**
     * Return String of Map-Center with default Zoom
     * <p/>
     * <p>x_position,y_position,default_zoom
     *
     * @return center formatted using mbtiles format
     */
    public String getCenterAsString() {
        return centerX + "," + centerY + "," + defaultZoom;
    }

    /**
     * @return true if the table is editable.
     */
    public abstract boolean isEditable();

    /**
     * 'SpatialTable' = false [SpatialVectorTable]
     * 'RasterLite2' = false [SpatialRasterTable]
     * 'GeoPackage_features' = false [SpatialVectorTable]
     * 'GeoPackage_tiles' = false [SpatialRasterTable]
     * 'SpatialView' = true [SpatialVectorTable]
     *
     * @return true if this is a SpatialView
     */
    public boolean isView(){
        return isView;
    }

}
