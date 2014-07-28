/** @author Simon Thépot aka djcoin <simon.thepot@gmail.com, simon.thepot@makina-corpus.com>
  * adapted to create and fill mbtiles databases Mark Johnson (www.mj10777.de)
 */
package eu.geopaparazzi.spatialite.database.spatial.core.mbtiles;

import android.database.Cursor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class MbTilesMetadata {
    public final String name, description, type, version, format;
    public final float[] bounds;
    public final float[] center;
    public final int minZoom;
    public final int maxZoom;
    public final int defaultZoom;
    public String s_tile_row_type = "tms";
    public String s_center_parm = "";
    public final Map<String, String> extra;
    public static final MetadataValidatorFactory metadataValidatorFactory = new MetadataValidatorFactory();
    // -----------------------------------------------
    /**
      * Constructor MbTilesMetadata
      *
      * <ul>
      * <li>if the file does not exist, a valid mbtile database will be created</li>
      * <li>if the parent directory does not exist, it will be created</li>
      * </ul>
      *
      * @param name The name of the tileset.
      * @param description A description of the layer as plain text.
      * @param version The version of the tileset, as a plain number.
      * @param format The image file format of the tile data: png or jpg
      * @param bounds Should be latitude and longitude values in OpenLayers Bounds format - left, bottom, right, top.
      * @param center  A default position and Zoom that can be set by the MBTiles designer
      * @param minZoom  minimum Zoom level
      * @param maxZoom  maximum Zoom level
      * @param s_tile_row_type how the y tile-position is to be interpreted ['tms' or 'osm']
      * @param extra any other values found in the metadata table
      */
    public MbTilesMetadata( String name, String description, String type, String version, String format, float[] bounds,
            float[] center, int minZoom, int maxZoom, String s_tile_row_type, Map<String, String> extra ) {
        this.name = name;
        this.type = type;
        this.version = version;
        this.description = description;
        this.format = format;
        if (bounds == null) { // -180.0,-85,180,85
            bounds = new float[]{-180.0f, -85.05113f, 180.0f, 85.05113f};
        }
        this.bounds = bounds;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.extra = extra;
        if (center != null) {
            if ((center[0] < this.bounds[0]) || (center[0] > this.bounds[2])) {
                center[0] = this.bounds[0] + (this.bounds[2] - this.bounds[0]) / 2f;
            }
            if ((center[1] < this.bounds[1]) || (center[1] > this.bounds[3])) {
                center[1] = this.bounds[1] + (this.bounds[3] - this.bounds[1]) / 2f;
            }
            center[2] = (int) center[2];
            if ((center[2] < minZoom) || (center[2] > maxZoom))
                center[2] = minZoom; // .map files only have a minZoom, this will be used so that
                                     // everything reacte in the same way
            this.center = center;
        } else {
            this.center = new float[]{this.bounds[0] + (this.bounds[2] - this.bounds[0]) / 2f,
                    this.bounds[1] + (this.bounds[3] - this.bounds[1]) / 2f, maxZoom};
        }
        if ((s_tile_row_type != "") && ((s_tile_row_type.equals("tms")) || (s_tile_row_type.equals("osm"))))
            this.s_tile_row_type = s_tile_row_type;
        this.defaultZoom = (int) this.center[2];
        this.s_center_parm = center[0] + "," + center[1] + "," + this.defaultZoom;
    }

    @Override
    public String toString() {
        String none = " - ";
        String separator = " | ";
        StringBuilder sb = new StringBuilder("Metadata:\n");
        sb.append("Name: " + (name == null ? none : name) + separator);
        sb.append("Type: " + (type == null ? none : type) + separator);
        sb.append("Version: " + (version == null ? none : version) + separator);
        sb.append("Description: " + (description == null ? none : description) + separator);
        sb.append("Format: " + (format == null ? none : format) + separator);
        sb.append("Bounds: " + (bounds == null ? none : bounds));
        sb.append("Zoom [min]: " + minZoom);
        sb.append("Zoom [max]: " + maxZoom);
        sb.append("Center: " + (center == null ? none : center));
        sb.append("Zoom [default]: " + defaultZoom);
        sb.append("tile_row_type: " + (s_tile_row_type == null ? none : s_tile_row_type));
        sb.append("\n\n");
        sb.append("Extra: " + this.extra.toString() + "");
        return sb.toString();
    }

    // -----------------------------------------------
    /**
      * createFromCursor
      * @param c: Sql Cursor being used
      * @param idx_col_key: index field of the metdata field 'name' [should be 0]
      * @param idx_col_value: index field of the metdata field 'value' [should be 1]
      * @return HashMap<String,String> with key,values to be validated
      */
    public static MbTilesMetadata createFromCursor( Cursor c, int idx_col_key, int idx_col_value, MetadataValidator validator )
            throws MetadataParseException {

        Map<String, String> dumped = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        c.moveToFirst();
        do {
            dumped.put(c.getString(idx_col_key), c.getString(idx_col_value));
        } while( c.moveToNext() );
        c.close();
        return validator.validate(dumped);
    }

    public static interface MetadataValidator {
        MbTilesMetadata validate( Map<String, String> hm ) throws MetadataParseException;
    }

    @SuppressWarnings("serial")
    public static class MetadataParseException extends Exception {
        public MetadataParseException( String errorMessage ) {
            super(errorMessage);
        }
    }

    public static class MetadataValidatorFactory {
        /* Validators for parsing Metadata */
        private final static MetadataValidator VALIDATOR_1_0 = new MetadataValidator_1_0();
        private final static MetadataValidator VALIDATOR_1_1 = new MetadataValidator_1_1();
        public static MetadataValidator getMetadataValidatorFromVersion( String version ) {
            if (version.equals("1.0"))
                return VALIDATOR_1_0;
            if (version.equals("1.1"))
                return VALIDATOR_1_1;
            return null;
        }
    }

    // -----------------------------------------------
    /**
      * validate MetadataValidator_1_1
      *
      * <p>https://github.com/mapbox/MbTiles.spec/blob/master/1.1/spec.md </br>
      * mj10777: since this is a reader, we will attempt to supply default
      * values for missing or incorrect values</p>
      *
      * <ul>
      * <li>at the moment, only 1.1 should be called</li>
      * <li>checking of mandatory fields should exist (to insure that this is really a mbtiles file)</li>
      * </ul>
      *
      * @param name: [mandatory] The name of the tileset.
      * @param description: [mandatory] A description of the layer as plain text.
      * @param version: [mandatory] The version of the tileset, as a plain number.
      * @param format: [mandatory]  The image file format of the tile data: png or jpg
      * @param bounds: [optional] Should be latitude and longitude values in OpenLayers Bounds format - left, bottom, right, top.
      * @param center:  [tilemill specific,unofficial] A default position and Zoom that can be set by the MBTiles designer
      * @param minZoom: [optional] minimum Zoom level
      * @param maxZoom: [optional] maximum Zoom level
      * @param s_tile_row_type:  [suggested,unofficial]  how the y tile-position is to be interpreted ['tms' or 'osm']
      * @param extra any other values found in the metadata table
      */
    public static class MetadataValidator_1_1 implements MetadataValidator {
        @Override
        public MbTilesMetadata validate( Map<String, String> hm ) throws MetadataParseException {
            String tmp;
            String name = hm.remove("name");
            if (name == null)
                throw new MetadataParseException("No mandatory field 'name'.");
            String description = hm.remove("description");
            if (description == null) { // gdal does not fill this value [it can have empty (NULL)
                                       // value]
                description = name;
                // throw new MetadataParseException("No mandatory field 'description'.");
            }
            String type = hm.remove("type");
            if (type == null || (!type.equals("overlay") && type.equals("baselayer"))) {
                // we suppose it is a baselayer by default, if not available
                type = "baselayer";
            }
            String version = "1.1";
            tmp = hm.remove("version");
            if (tmp == null)
                throw new MetadataParseException("No mandatory field 'version'");
            String format = hm.remove("format");
            if (format == null || (!format.equals("png") && !format.equals("jpg"))) {
                // This application does NOT need to know the image type to display
                format = "jpg";
            }
            // bounds: optional
            // - should be set to zoom to this area when first loading the map, if the present point
            // is out of range
            // -- user friendly, otherwise the user may be lost in a sea of white tiles.
            tmp = hm.remove("bounds");
            float[] bounds = {-180.0f, -85.05113f, 180.0f, 85.05113f};
            if (tmp != null) {
                bounds = ValidatorHelper.parseBounds(tmp);
                if (bounds == null) {
                    bounds = new float[]{-180.0f, -85.05113f, 180.0f, 85.05113f};
                    // throw new
                    // MetadataParseException("Invalid syntax for optional field 'bounds'."
                    // +
                    // "Should be latitude and longitude values in OpenLayers Bounds format - left, bottom, right, top."
                    // + "Example of the full earth: -180.0,-85,180,85");
                }
            }
            String minZoomStr = hm.remove("minzoom");
            int minZoom = 0;
            if (minZoomStr != null) {
                minZoom = Integer.parseInt(minZoomStr);
            }

            String maxZoomStr = hm.remove("maxzoom");
            int maxZoom = 22;
            if (maxZoomStr != null) {
                maxZoom = Integer.parseInt(maxZoomStr);
            }
            if (minZoom > maxZoom) {
                int i_zoom = minZoom;
                minZoom = maxZoom;
                maxZoom = i_zoom;
            }
            if ((minZoom < 0) || (minZoom > 22))
                minZoom = 0;
            if ((maxZoom < 0) || (maxZoom > 22))
                maxZoom = 22;
            // center: tilemill specific parameter
            // - not part of the specification, but usefull when first loading and the map is out of
            // the range
            // - the map designer can determin the 'main point of interest' and desired zoom level
            // -- which may NOT be the center of the map OR the minZoom [which are the default]
            // - the application will only use this point when the present point is outside the
            // bounds
            tmp = hm.remove("center");
            float[] center = {bounds[0] + (bounds[2] - bounds[0]) / 2f, bounds[1] + (bounds[3] - bounds[1]) / 2f, maxZoom};
            if (tmp != null) {
                center = ValidatorHelper.parseCenter(tmp);
                if (center == null) {
                    center = new float[]{bounds[0] + (bounds[2] - bounds[0]) / 2f, bounds[1] + (bounds[3] - bounds[1]) / 2f,
                            maxZoom};
                    // throw new
                    // MetadataParseException("Invalid syntax for optional field 'center'."
                    // +
                    // "Should be latitude and longitude values in as - center_x, center_y, rzoom."
                    // + "Example of the full earth: 0,0,1");
                }
            }
            // tile_row_type: possible support for non-tms numbering
            // - this value has been suggested, but not acepted and should be used with care
            String s_tile_row_type = hm.remove("tile_row_type");
            if (s_tile_row_type == null || (!s_tile_row_type.equals("tms") && !s_tile_row_type.equals("osm"))) {
                // Until this is accepted, other application will get 'confused'
                // when the non-tms numbering is used
                s_tile_row_type = "tms";
            }
            return new MbTilesMetadata(name, description, type, version, format, bounds, center, minZoom, maxZoom,
                    s_tile_row_type, hm);
        }
    }

    // -----------------------------------------------
    /**
      * validate MetadataValidator_1_0
      *
      * <p>https://github.com/mapbox/MbTiles.spec/blob/master/1.0/spec.md </br>
      * mj10777: since this is a reader, we will attempt to supply default
      * values for missing or incorrect values</p>
      *
      * <ul>
      * <li>at the moment, only 1.1 should be called</li>
      * <li>checking of mandatory fields should exist (to insure that this is really a mbtiles file)</li>
      * </ul>
      *
      * @param name: [mandatory] The name of the tileset.
      * @param description: [mandatory] A description of the layer as plain text.
      * @param version: [mandatory] The version of the tileset, as a plain number.
      * @param format: [mandatory]  The image file format of the tile data: png or jpg
      * @param bounds: [optional] Should be latitude and longitude values in OpenLayers Bounds format - left, bottom, right, top.
      * @param center:  [tilemill specific,unofficial] A default position and Zoom that can be set by the MBTiles designer
      * @param minZoom: [optional] minimum Zoom level
      * @param maxZoom: [optional] maximum Zoom level
      * @param s_tile_row_type:  [suggested,unofficial]  how the y tile-position is to be interpreted ['tms' or 'osm']
      * @param extra any other values found in the metadata table
      */
    public static class MetadataValidator_1_0 implements MetadataValidator {
        @Override
        public MbTilesMetadata validate( Map<String, String> hm ) throws MetadataParseException {
            String tmp;
            String name = hm.remove("name");
            if (name == null)
                throw new MetadataParseException("No mandatory field 'name'.");
            String description = hm.remove("description");
            if (description == null)
                throw new MetadataParseException("No mandatory field 'description'.");
            String type = hm.remove("type");
            if (type == null || (!type.equals("overlay") && type.equals("baselayer"))) {
                // we suppose it is a baselayer by default, if not available
                type = "baselayer";
                // throw new
                // MetadataParseException("No mandatory field 'type' or not in [ overlay, baselayer ].");
            }
            String version = "1.0";
            tmp = hm.remove("version");
            if (tmp == null)
                throw new MetadataParseException("No mandatory field 'version'");
            String format = hm.remove("format");
            if (format == null || (!format.equals("png") && !format.equals("jpg"))) {
                // This application does NOT need to know the image type to display
                format = "jpg";
            }
            // bounds: optional
            // - should be set to zoom to this area when first loading the map, if the present point
            // is out of range
            // -- user friendly, otherwise the user may be lost in a sea of white tiles.
            tmp = hm.remove("bounds");
            float[] bounds = {-180.0f, -85.05113f, 180.0f, 85.05113f};
            if (tmp != null) { // some tilemill db use this - despite the 1.0.0 version number
                bounds = ValidatorHelper.parseBounds(tmp);
                if (bounds == null) {
                    bounds = new float[]{-180.0f, -85.05113f, 180.0f, 85.05113f};
                    // throw new
                    // MetadataParseException("Invalid syntax for optional field 'bounds'."
                    // +
                    // "Should be latitude and longitude values in OpenLayers Bounds format - left, bottom, right, top."
                    // + "Example of the full earth: -180.0,-85,180,85");
                }
            }
            String minZoomStr = hm.remove("minzoom");
            int minZoom = 0;
            if (minZoomStr != null) {
                minZoom = Integer.parseInt(minZoomStr);
            }
            String maxZoomStr = hm.remove("maxzoom");
            int maxZoom = 22;
            if (maxZoomStr != null) {
                maxZoom = Integer.parseInt(maxZoomStr);
            }
            // center: tilemill specific parameter
            // - not part of the specification, but usefull when first loading and the map is out of
            // the range
            // - the map designer can determin the 'main point of interest' and desired zoom level
            // -- which may NOT be the center of the map OR the minZoom [which are the default]
            // - the application will only use this point when the present point is outside the
            // bounds
            tmp = hm.remove("center");
            float[] center = {bounds[0] + (bounds[2] - bounds[0]) / 2f, bounds[1] + (bounds[3] - bounds[1]) / 2f, maxZoom};
            if (tmp != null) {
                center = ValidatorHelper.parseCenter(tmp);
                if (center == null) {
                    center = new float[]{bounds[0] + (bounds[2] - bounds[0]) / 2f, bounds[1] + (bounds[3] - bounds[1]) / 2f,
                            maxZoom};
                    // throw new
                    // MetadataParseException("Invalid syntax for optional field 'center'."
                    // +
                    // "Should be latitude and longitude values in as - center_x, center_y, rzoom."
                    // + "Example of the full earth: 0,0,1");
                }
            }
            // tile_row_type: possible support for non-tms numbering
            // - this value has been suggested, but not acepted and should be used with care
            String s_tile_row_type = hm.remove("tile_row_type");
            if (s_tile_row_type == null || (!s_tile_row_type.equals("tms") && !s_tile_row_type.equals("osm"))) {
                // Until this is accepted, other application will get
                // 'confused' when the non-tms numbering is used
                s_tile_row_type = "tms";
            }
            return new MbTilesMetadata(name, description, type, version, format, bounds, center, minZoom, maxZoom,
                    s_tile_row_type, hm);
        }
    }

    public static class ValidatorHelper {
        // left, bottom, right, top | Full earth: -180.0,-85,180,85
        // -----------------------------------------------
        /**
          * Parse Bounds
          * - Format (Wsg84) : left/west bottom/south right/east top/north
          * - Full earth: -180.0,-85,180,85
          * -- if not set : Full earth: -180.0,-85,180,85 will be used
          * @param tmp value read by the Validator
          * @return float[] with the left/west bottom/south right/east top/north positions
          */
        public static float[] parseBounds( String tmp ) {
            float[] bounds;
            if (tmp == null)
                return null;
            String[] splitted = tmp.split(",");
            if (splitted.length != 4)
                return null;
            bounds = new float[4];
            try {
                for( int i = 0; i < splitted.length; i++ ) {
                    bounds[i] = Float.parseFloat(splitted[i]);
                }
            } catch (NumberFormatException e) {
                return null;
            }
            if ((bounds[0] >= -180.0f && bounds[0] <= 180.0f) && (bounds[2] >= -180.0f && bounds[2] <= 180.0f)
                    && (bounds[1] >= -85.05113f && bounds[1] <= 85.05113f) && (bounds[3] >= -85.05113f && bounds[3] <= 85.05113f))
                return bounds;
            else
                return null;
        }
        // -----------------------------------------------
        /**
          * Parse default Center Position and Zoom-Level
          * - Format (Wsg84) : center_x, center_y, zoom
          * - tilemill specific parameter
          * - the map designer can determin the 'main point of interest' and desired zoom level
          * -- which may NOT be the center of the map OR the minZoom [which are the default]
          * - the application will only use this point when the present point is outside the bounds
          * @param tmp value read by the Validator
          * @return float[] with the x,y,z position
          */
        public static float[] parseCenter( String tmp ) {
            if (tmp == null)
                return null;
            String[] splitted = tmp.split(",");
            if (splitted.length < 2)
                return null;
            float[] center = new float[3];
            center[2] = 0; // just in case a zoom parameter is missing
            try {
                for( int i = 0; i < splitted.length; i++ ) {
                    center[i] = Float.parseFloat(splitted[i]);
                }
            } catch (NumberFormatException e) {
                return null;
            }
            if ((center[0] >= -180.0f && center[0] <= 180.0f) && (center[1] >= -85.05113f && center[1] <= 85.05113f))
                return center;
            else
                return null;
        }
    }
    // -----------------------------------------------
    /**
      * Function to check if inserted tile is outside known bounds and min/max zoom level
      * @param tileBounds area to check - left/west bottom/south right/east top/north
      * @param i_zoom the value for zoom_level to check
      * @return 0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections ; 1= correct tileBounds values.
      */
    public HashMap<String, String> checkTileLocation( double[] tileBounds, int i_zoom ) {
        // mj10777: i_rc=0=inside valid area/zoom ; i_rc > 0 outside area or
        // zoom ; i_parm=0 no corrections ; 1= correct tileBounds values.
        // int i_rc = 0; // inside area
        HashMap<String, String> update_metadata = new LinkedHashMap<String, String>();
        double bounds_west = (double) bounds[0];
        double bounds_south = (double) bounds[1];
        double bounds_east = (double) bounds[2];
        double bounds_north = (double) bounds[3];
        double tile_west = tileBounds[0];
        double tile_south = tileBounds[1];
        double tile_east = tileBounds[2];
        double tile_north = tileBounds[3];
        int maxZoom = this.minZoom;
        int minZoom = this.maxZoom;
        if (((tile_west < bounds_west) || (tile_east > bounds_east))
                || ((tile_south < bounds_south) || (tile_north > bounds_north)) || ((i_zoom < minZoom) || (i_zoom > maxZoom))) {
            if (((tile_west >= bounds_west) && (tile_east <= bounds_east))
                    && ((tile_south >= bounds_south) && (tile_north <= bounds_north))) {
                // We are inside the Map-Area, but Zoom is not correct
                if (i_zoom < minZoom) {
                    update_metadata.put("minzoom", String.valueOf(i_zoom));
                }
                if (i_zoom > maxZoom) {
                    update_metadata.put("maxzoom", String.valueOf(i_zoom));
                }
            } else {
                if (i_zoom < minZoom) {
                    update_metadata.put("minzoom", String.valueOf(i_zoom));
                }
                if (i_zoom > maxZoom) {
                    update_metadata.put("maxzoom", String.valueOf(i_zoom));
                }
                if (tile_west < bounds_west) {
                    bounds_west = tile_west;
                }
                if (tile_east > bounds_east) {
                    bounds_east = tile_east;
                }
                if (tile_south < bounds_south) {
                    bounds_south = tile_south;
                }
                if (tile_north > bounds_north) {
                    bounds_north = tile_north;
                }
                update_metadata.put("bounds", bounds_west + "," + bounds_south + "," + bounds_east + "," + bounds_north);
            }
        }
        return update_metadata;
    }
}
