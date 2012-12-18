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

/**
 * Geometry types.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum GeometryType {
    /*
     * XY
     */
    GEOMETRY_XY(0), //
    POINT_XY(1), //
    LINESTRING_XY(2), //
    POLYGON_XY(3), //
    MULTIPOINT_XY(4), //
    MULTILINESTRING_XY(5), //
    MULTIPOLYGON_XY(6), //
    GEOMETRYCOLLECTION_XY(7), //
    /*
     * XYZ
     */
    GEOMETRY_XYZ(1000), //
    POINT_XYZ(1001), //
    LINESTRING_XYZ(1002), //
    POLYGON_XYZ(1003), //
    MULTIPOINT_XYZ(1004), //
    MULTILINESTRING_XYZ(1005), //
    MULTIPOLYGON_XYZ(1006), //
    GEOMETRYCOLLECTION_XYZ(1007), //
    /*
     * XYM
     */
    GEOMETRY_XYM(2000), //
    POINT_XYM(2001), //
    LINESTRING_XYM(2002), //
    POLYGON_XYM(2003), //
    MULTIPOINT_XYM(2004), //
    MULTILINESTRING_XYM(2005), //
    MULTIPOLYGON_XYM(2006), //
    GEOMETRYCOLLECTION_XYM(2007), //
    /*
     * XYZM
     */
    GEOMETRY_XYZM(3000), //
    POINT_XYZM(3001), //
    LINESTRING_XYZM(3002), //
    POLYGON_XYZM(3003), //
    MULTIPOINT_XYZM(3004), //
    MULTILINESTRING_XYZM(3005), //
    MULTIPOLYGON_XYZM(3006), //
    GEOMETRYCOLLECTION_XYZM(3007);//

    private final int type;

    GeometryType( int type ) {
        this.type = type;
    }

    public int getType() {
        return type;
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
        }
        throw new IllegalArgumentException("No geometry type of value: " + name);
    }
}
