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
package eu.geopaparazzi.spatialite.database.spatial.core.enums;

import android.annotation.SuppressLint;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Geometry types.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings({"nls", "javadoc"})
public enum GeometryType {
    /*
     * XY
     */
    GEOMETRY_XY(0, "geometry_xy", null, "CastToXY", null), //
    POINT_XY(1, "point_xy", "CastToPoint", "CastToXY", "CastToSingle"), //
    LINESTRING_XY(2, "linestring_xy", "CastToLinestring", "CastToXY", "CastToSingle"), //
    POLYGON_XY(3, "polygon_xy", "CastToPolygon", "CastToXY", "CastToSingle"), //
    MULTIPOINT_XY(4, "multipoint_xy", "CastToMultiPoint", "CastToXY", "CastToMulti"), //
    MULTILINESTRING_XY(5, "multilinestring_xy", "CastToMultiLinestring", "CastToXY", "CastToMulti"), //
    MULTIPOLYGON_XY(6, "multipolygon_xy", "CastToMultiPolygon", "CastToXY", "CastToMulti"), //
    GEOMETRYCOLLECTION_XY(7, "geometrycollection_xy", "CastToGeometyCollection", "CastToXY", null), //
    /*
     * XYZ
     */
    GEOMETRY_XYZ(1000, "geometry_xyz", null, "CastToXYZ", null), //
    POINT_XYZ(1001, "point_xyz", "CastToPoint", "CastToXYZ", "CastToSingle"), //
    LINESTRING_XYZ(1002, "linestring_xyz", "CastToLinestring", "CastToXYZ", "CastToSingle"), //
    POLYGON_XYZ(1003, "polygon_xyz", "CastToPolygon", "CastToXYZ", "CastToSingle"), //
    MULTIPOINT_XYZ(1004, "multipoint_xyz", "CastToMultiPoint", "CastToXYZ", "CastToMulti"), //
    MULTILINESTRING_XYZ(1005, "multilinestring_xyz", "CastToMultiLinestring", "CastToXYZ", "CastToMulti"), //
    MULTIPOLYGON_XYZ(1006, "multipolygon_xyz", "CastToMultiPolygon", "CastToXYZ", "CastToMulti"), //
    GEOMETRYCOLLECTION_XYZ(1007, "geometrycollection_xyz", "CastToGeometyCollection", "CastToXYZ", null), //
    /*
     * XYM
     */
    GEOMETRY_XYM(2000, "geometry_xym", null, "CastToXYM", null), //
    POINT_XYM(2001, "point_xym", "CastToPoint", "CastToXYM", "CastToSingle"), //
    LINESTRING_XYM(2002, "linestring_xym", "CastToLinestring", "CastToXYM", "CastToSingle"), //
    POLYGON_XYM(2003, "polygon_xym", "CastToPolygon", "CastToXYM", "CastToSingle"), //
    MULTIPOINT_XYM(2004, "multipoint_xym", "CastToMultiPoint", "CastToXYM", "CastToMulti"), //
    MULTILINESTRING_XYM(2005, "multilinestring_xym", "CastToMultiLinestring", "CastToXYM", "CastToMulti"), //
    MULTIPOLYGON_XYM(2006, "multipolygon_xym", "CastToMultiPolygon", "CastToXYM", "CastToMulti"), //
    GEOMETRYCOLLECTION_XYM(2007, "geometrycollection_xym", "CastToGeometyCollection", "CastToXYM", null), //
    /*
     * XYZM
     */
    GEOMETRY_XYZM(3000, "geometry_xyzm", null, "CastToXYZM", null), //
    POINT_XYZM(3001, "point_xyzm", "CastToPoint", "CastToXYZM", "CastToSingle"), //
    LINESTRING_XYZM(3002, "linestring_xyzm", "CastToLinestring", "CastToXYZM", "CastToSingle"), //
    POLYGON_XYZM(3003, "polygon_xyzm", "CastToPolygon", "CastToXYZM", "CastToSingle"), //
    MULTIPOINT_XYZM(3004, "multipoint_xyzm", "CastToMultiPoint", "CastToXYZM", "CastToMulti"), //
    MULTILINESTRING_XYZM(3005, "multilinestring_xyzm", "CastToMultiLinestring", "CastToXYZM", "CastToMulti"), //
    MULTIPOLYGON_XYZM(3006, "multipolygon_xyzm", "CastToMultiPolygon", "CastToXYZM", "CastToMulti"), //
    GEOMETRYCOLLECTION_XYZM(3007, "geometrycollection_xyzm", "CastToGeometyCollection", "CastToXYZM", null);//

    private final int type;
    private final String description;
    private String geometryTypeCast;
    private String spaceDimensionsCast;
    private String multiSingleCast;

    /**
     * Create the type.
     * 
     * @param type the geometry type.
     * @param description the human readable description.
     * @param geometryTypeCast the geometry cast sql piece.
     * @param spaceDimensionsCast the space dimension cast sql piece.
     * @param multiSingleCast the cast sql piece for single or multi geom.
     */
    GeometryType( int type, String description, String geometryTypeCast, String spaceDimensionsCast, String multiSingleCast ) {
        this.type = type;
        this.description = description;
        this.geometryTypeCast = geometryTypeCast;
        this.spaceDimensionsCast = spaceDimensionsCast;
        this.multiSingleCast = multiSingleCast;
    }

    /**
     * @return the geometry type.
     */
    public int getType() {
        return type;
    }

    /**
     * @return the human readable description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the geometry cast sql piece.
     */
    public String getGeometryTypeCast() {
        return geometryTypeCast;
    }

    /**
     * @return the space dimension cast sql piece.
     */
    public String getSpaceDimensionsCast() {
        return spaceDimensionsCast;
    }

    /**
     * @return the cast sql piece for single or multi geom. 
     */
    public String getMultiSingleCast() {
        return multiSingleCast;
    }

    /**
     * Get the type from the int value in spatialite 4.
     *
     * @param value the type.
     * @return the {@link GeometryType}.
     */
    public static GeometryType forValue( int value ) {
        switch( value ) {
        case 0:
            return GEOMETRY_XY;
        case 1:
            return POINT_XY;
        case 2:
            return LINESTRING_XY;
        case 3:
            return POLYGON_XY;
        case 4:
            return MULTIPOINT_XY;
        case 5:
            return MULTILINESTRING_XY;
        case 6:
            return MULTIPOLYGON_XY;
        case 7:
            return GEOMETRYCOLLECTION_XY;
            /*
             * XYZ
             */
        case 1000:
            return GEOMETRY_XYZ;
        case 1001:
            return POINT_XYZ;
        case 1002:
            return LINESTRING_XYZ;
        case 1003:
            return POLYGON_XYZ;
        case 1004:
            return MULTIPOINT_XYZ;
        case 1005:
            return MULTILINESTRING_XYZ;
        case 1006:
            return MULTIPOLYGON_XYZ;
        case 1007:
            return GEOMETRYCOLLECTION_XYZ;
            /*
             * XYM
             */
        case 2000:
            return GEOMETRY_XYM;
        case 2001:
            return POINT_XYM;
        case 2002:
            return LINESTRING_XYM;
        case 2003:
            return POLYGON_XYM;
        case 2004:
            return MULTIPOINT_XYM;
        case 2005:
            return MULTILINESTRING_XYM;
        case 2006:
            return MULTIPOLYGON_XYM;
        case 2007:
            return GEOMETRYCOLLECTION_XYM;
            /*
             * XYZM
             */
        case 3000:
            return GEOMETRY_XYZM;
        case 3001:
            return POINT_XYZM;
        case 3002:
            return LINESTRING_XYZM;
        case 3003:
            return POLYGON_XYZM;
        case 3004:
            return MULTIPOINT_XYZM;
        case 3005:
            return MULTILINESTRING_XYZM;
        case 3006:
            return MULTIPOLYGON_XYZM;
        case 3007:
            return GEOMETRYCOLLECTION_XYZM;
        default:
            break;
        }
        throw new IllegalArgumentException("No geometry type of value: " + value);
    }

    /**
     * Checks if the given geometry is compatible with this type.
     *
     * <p>Compatible means that the type is the same and a cast from multi to
     * single is not required.<p/>
     *
     * @param geometry the geometry to check.
     * @return <code>true</code>, if the geometry is compatible.
     */
    public boolean isGeometryCompatible(Geometry geometry){
        String geometryType = geometry.getGeometryType().toLowerCase();

        String description = getDescription().toLowerCase();
        if (!description.startsWith(geometryType)){
            return false;
        }
        /*
         * Geometry is not compatible if the type is single
         * and the geometry is multi.
         */
        String multiSingleCast = getMultiSingleCast().toLowerCase();
        if (multiSingleCast.contains("tosingle")){
            // layer is single geometry
            if (geometryType.contains("multi")){
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given geometry type is compatible with this type.
     *
     * <p>Compatible means that the type is the same and a cast from multi to
     * single is not required.<p/>
     *
     * @param geometryType the geometry type to check.
     * @return <code>true</code>, if the geometry is compatible.
     */
    public boolean isGeometryTypeCompatible(GeometryType geometryType){
        String otherDescription = geometryType.getDescription();
        String thisDescription = getDescription();
        /*
         * Geometry is not compatible if the type is single
         * and the geometry is multi.
         */
        String multiSingleCast = getMultiSingleCast().toLowerCase();
        if (multiSingleCast.contains("tosingle")){
            // layer is single geometry
            if (otherDescription.contains("multi")){
                return false;
            }
        }

        String otherBaseDescription = otherDescription.split("\\_")[0].replaceFirst("multi", "");
        String baseDescription = thisDescription.split("\\_")[0].replaceFirst("multi", "");

        return baseDescription.equals(otherBaseDescription);
    }

    /**
     * Get the {@link GeometryType} int value from the geometry type name as of spatialite 3.
     *
     * <b>WARNING: this returns just the basic geom types!</b>
     *
     * @param name the geometry type name.
     * @return the type.
     */
    @SuppressLint("DefaultLocale")
    public static int forValue( String name ) {
        if (name.toUpperCase().startsWith("POINT")) {
            return POINT_XY.getType();
        } else if (name.toUpperCase().startsWith("MULTIPOINT")) {
            return MULTIPOINT_XY.getType();
        } else if (name.toUpperCase().startsWith("LINESTRING")) {
            return LINESTRING_XY.getType();
        } else if (name.toUpperCase().startsWith("MULTILINESTRING")) {
            return MULTILINESTRING_XY.getType();
        } else if (name.toUpperCase().startsWith("POLYGON")) {
            return POLYGON_XY.getType();
        } else if (name.toUpperCase().startsWith("MULTIPOLYGON")) {
            return MULTIPOLYGON_XY.getType();
        } else if (name.toUpperCase().startsWith("GEOMETRYCOLLECTION")) {
            return GEOMETRYCOLLECTION_XY.getType();
        }
        throw new IllegalArgumentException("No geometry type of value: " + name);
    }
}
