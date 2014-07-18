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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

import java.util.Iterator;

import eu.geopaparazzi.library.database.GPLog;
import jsqlite.Constants;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * Class that iterates over Database geometries and doesn't keep everything in memory.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class GeometryIterator implements Iterator<Geometry> {
    private WKBReader wkbReader = new WKBReader();
    private Stmt stmt;
    private String labelText = "";
    /**
     * Returns Label String (if any)
     *
     *  <p>
     * - if any label is being supported, will build s_label from column 1 to end<br>
     * -- each column (after 1) will have a ', ' inserted<br>
     * -- s_label will be empty if no label was requested<br>
     * @return s_label
    */
    public String getLabelText() {
        return labelText;
    }
    /**
     * Builds Label String (if any)
     *
     *  <p>
     * - assumes that column 0 is ALWAYS a Geometry<br>
     * - if any label is being supported, will build s_label from column 1 to end<br>
     * -- each column (after 1) will have a ', ' inserted<br>
     * -- s_label will be set to blank before filling<br>
     * @param stmt statement being executed
     */
    private void setLabelText( Stmt stmt ) {
        labelText = "";
        int i = 1;
        int columnCount = 0;
        try {
            if ((stmt != null) && (columnCount = stmt.column_count()) > 1) {
                for( i = 1; i < columnCount; i++ ) {
                    if (!labelText.equals("")) {
                        labelText += ", ";
                    }
                    switch( stmt.column_type(i) ) {
                    case Constants.SQLITE_INTEGER: {
                        labelText = labelText + stmt.column_int(i);
                    }
                        break;
                    case Constants.SQLITE_FLOAT: {
                        labelText += String.format("%.5f", stmt.column_double(i));
                    }
                        break;
                    case Constants.SQLITE_BLOB: { // not supported
                    }
                        break;
                    case Constants.SQLITE3_TEXT: {
                        labelText += stmt.column_string(i);
                    }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            GPLog.androidLog(4, "GeometryIterator.setLabelText column_count[" + columnCount + "] column[" + i + "]", e);
        }
    }
    /**
     * Constructor.
     *
     * @param database the database to use.
     * @param query the query to use.
     */
    public GeometryIterator( Database database, String query ) {
        try {
            stmt = database.prepare(query);
        } catch (Exception e) {
            GPLog.androidLog(4, "GeometryIterator.creation sql[" + query + "]", e);
        }
    }

    @Override
    public boolean hasNext() {
        if (stmt == null) {
            return false;
        }
        try { // sqlite-amalgamation-3080100 allways returns false with BLOBS
            return stmt.step();
        } catch (Exception e) {
            GPLog.androidLog(4, "GeometryIterator.hasNext()[stmt.step() failed]", e);
            return false;
        }
    }

    @Override
    public Geometry next() {
        if (stmt == null) {
            GPLog.androidLog(4, "GeometryIterator.next() [stmt=null]");
            return null;
        }
        try {
            byte[] geomBytes = stmt.column_bytes(0);
            Geometry geometry = wkbReader.read(geomBytes);
            setLabelText(stmt);
            return geometry;
        } catch (java.lang.Exception e) {
            GPLog.androidLog(4, "GeometryIterator.next()[wkbReader.read() failed]", e);
        }
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reset the iterator.
     *
     * @throws Exception  if something goes wrong.
     */
    public void reset() throws Exception {
        if (stmt != null)
            stmt.reset();
    }

    /**
     * Close the iterator.
     *
     * @throws Exception  if something goes wrong.
     */
    public void close() throws Exception {
        if (stmt != null)
            stmt.close();
    }
}
