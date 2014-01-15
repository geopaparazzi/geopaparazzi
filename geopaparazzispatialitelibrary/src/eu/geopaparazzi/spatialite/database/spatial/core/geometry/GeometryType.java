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
package eu.geopaparazzi.spatialite.database.spatial.core.geometry;

import android.annotation.SuppressLint;

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
    GEOMETRY_XY(0, "geometry_xy"), //
    POINT_XY(1, "point_xy"), //
    LINESTRING_XY(2, "linestring_xy"), //
    POLYGON_XY(3, "polygon_xy"), //
    MULTIPOINT_XY(4, "multipoint_xy"), //
    MULTILINESTRING_XY(5, "multilinestring_xy"), //
    MULTIPOLYGON_XY(6, "multipolygon_xy"), //
    GEOMETRYCOLLECTION_XY(7, "geometrycollection_xy"), //
    /*
     * XYZ
     */
    GEOMETRY_XYZ(1000, "geometry_xyz"), //
    POINT_XYZ(1001, "point_xyz"), //
    LINESTRING_XYZ(1002, "linestring_xyz"), //
    POLYGON_XYZ(1003, "polygon_xyz"), //
    MULTIPOINT_XYZ(1004, "multipoint_xyz"), //
    MULTILINESTRING_XYZ(1005, "multilinestring_xyz"), //
    MULTIPOLYGON_XYZ(1006, "multipolygon_xyz"), //
    GEOMETRYCOLLECTION_XYZ(1007, "geometrycollection_xyz"), //
    /*
     * XYM
     */
    GEOMETRY_XYM(2000, "geometry_xym"), //
    POINT_XYM(2001, "point_xym"), //
    LINESTRING_XYM(2002, "linestring_xym"), //
    POLYGON_XYM(2003, "polygon_xym"), //
    MULTIPOINT_XYM(2004, "multipoint_xym"), //
    MULTILINESTRING_XYM(2005, "multilinestring_xym"), //
    MULTIPOLYGON_XYM(2006, "multipolygon_xym"), //
    GEOMETRYCOLLECTION_XYM(2007, "geometrycollection_xym"), //
    /*
     * XYZM
     */
    GEOMETRY_XYZM(3000, "geometry_xyzm"), //
    POINT_XYZM(3001, "point_xyzm"), //
    LINESTRING_XYZM(3002, "linestring_xyzm"), //
    POLYGON_XYZM(3003, "polygon_xyzm"), //
    MULTIPOINT_XYZM(3004, "multipoint_xyzm"), //
    MULTILINESTRING_XYZM(3005, "multilinestring_xyzm"), //
    MULTIPOLYGON_XYZM(3006, "multipolygon_xyzm"), //
    GEOMETRYCOLLECTION_XYZM(3007, "geometrycollection_xyzm");//

    private final int type;
    private final String description;

    GeometryType( int type, String description ) {
        this.type = type;
        this.description = description;
    }

    public int getType() {
        return type;
    }

    public String getDescription() {
        return description;
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
