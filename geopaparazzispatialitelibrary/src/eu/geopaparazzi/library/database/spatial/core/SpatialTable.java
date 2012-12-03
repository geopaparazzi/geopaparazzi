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
package eu.geopaparazzi.library.database.spatial.core;

/**
 * A table from the spatial db.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialTable {

    public final String name;
    public final String geomName;
    public final String geomType;
    public final String srid;
    public Style style;

    private boolean checkDone = false;
    private boolean isPolygon = false;
    private boolean isLine = false;
    private boolean isPoint = false;

    public SpatialTable( String name, String geomName, String geomType, String srid ) {
        this.name = name;
        this.geomName = geomName;
        this.geomType = geomType;
        this.srid = srid;
        checkType();
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
        if (geomType.toUpperCase().contains("POLYGON")) {
            isPolygon = true;
        } else if (geomType.toUpperCase().contains("LINESTRING")) {
            isLine = true;
        } else if (geomType.toUpperCase().contains("POINT")) {
            isPoint = true;
        }
        checkDone = true;
    }
    
    public void makeDefaultStyle() {
        style = new Style();
        style.name = name;
    }
}
