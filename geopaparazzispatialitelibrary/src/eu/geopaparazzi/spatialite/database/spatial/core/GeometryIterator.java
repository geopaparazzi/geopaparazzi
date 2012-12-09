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

import java.util.Iterator;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Class that iterates over Database geometries and doesn't keep everything in memory.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeometryIterator implements Iterator<Geometry> {
    private WKBReader wkbReader = new WKBReader();
    // private WKTReader wktReader = new WKTReader();
    private Stmt stmt;

    public GeometryIterator( Database database, String query ) {
        try {
            stmt = database.prepare(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        if (stmt == null)
            return false;
        try {
            return stmt.step();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Geometry next() {
        if (stmt == null)
            return null;
        try {
            byte[] geomBytes = stmt.column_bytes(0);
            Geometry geometry = wkbReader.read(geomBytes);
            return geometry;
        } catch (java.lang.Exception e) {
            e.printStackTrace();
            // String geomWKT = "";
            // try {
            // geomWKT = stmt.column_string(1);
            // Geometry geometry = wktReader.read(geomWKT);
            // return geometry;
            // } catch (java.lang.Exception e1) {
            // e1.printStackTrace();
            // Logger.i(this, "GEOM: " + geomWKT);
            // }
        }
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void reset() throws Exception {
        if (stmt != null)
            stmt.reset();
    }

    public void close() throws Exception {
        if (stmt != null)
            stmt.close();
    }
}
