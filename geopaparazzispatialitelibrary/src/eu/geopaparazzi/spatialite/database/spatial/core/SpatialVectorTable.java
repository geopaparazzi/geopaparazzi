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
 * A vector table from the spatial db.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialVectorTable {

    private final String name;
    private final String geomName;
    private final int geomType;
    private final String srid;
    private Style style;

    private boolean checkDone = false;
    private boolean isPolygon = false;
    private boolean isLine = false;
    private boolean isPoint = false;

    public SpatialVectorTable( String name, String geomName, int geomType, String srid ) {
        this.name = name;
        this.geomName = geomName;
        this.geomType = geomType;
        this.srid = srid;
        checkType();
    }

    public String getName() {
        return name;
    }

    public String getGeomName() {
        return geomName;
    }

    public int getGeomType() {
        return geomType;
    }

    public String getSrid() {
        return srid;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle( Style style ) {
        this.style = style;
    }

    public boolean isPolygon() {
        return isPolygon;
    }
    public boolean isLine() {
        return isLine;
    }
    public boolean isPoint() {
        return isPoint;
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
        default:
            throw new IllegalArgumentException("No geom type for: " + TYPE);
        }
        checkDone = true;
    }

    public void makeDefaultStyle() {
        style = new Style();
        style.name = name;
    }
}
