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

    public String name;
    public String geomName;
    public String geomType;
    public String srid;
    public Style style;

    private boolean checkDone = false;
    private boolean isPolygon = false;
    private boolean isLine = false;
    private boolean isPoint = false;

    public boolean isPolygon() {
        checkType();
        return isPolygon;
    }
    public boolean isLine() {
        checkType();
        return isLine;
    }
    public boolean isPoint() {
        checkType();
        return isPoint;
    }

    private void checkType() {
        if (checkDone) {
            return;
        }
        if (geomType.toUpperCase().endsWith("POLYGON")) {
            isPolygon = true;
        } else if (geomType.toUpperCase().endsWith("LINESTRING")) {
            isLine = true;
        } else if (geomType.toUpperCase().endsWith("POINT")) {
            isPoint = true;
        }
    }
    public void makeDefaultStyle() {
        style = new Style();
        style.name = name;
    }
}
