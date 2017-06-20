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
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteUtilities;
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
    private String themeFieldValue;

    /**
     * Returns Label String (if any)
     *
     * @return the label.
     */
    public String getLabelText() {
        return labelText;
    }

    /**
     * Get the theme field unique value if available, or null.
     *
     * @return the value or null.
     */
    public String getThemeFieldValue(){
        return themeFieldValue;
    }

    /**
     * Builds Label String (if any)
     * <p>
     * Assumes that column 0 is a Geometry, 1 label, 2 theme. The keyword 'dummy' means no label/theme.
     *
     * @param stmt statement being executed
     */
    private void setLabelAndThemeText(Stmt stmt) {
        labelText = "";
        int i = 1;
        int columnCount = 0;
        try {
            if (stmt != null) {
                columnCount = stmt.column_count();
                if (columnCount == 3) {
                    // get the label
                    String labelString = stmt.column_string(1);
                    if (!labelString.equals(SpatialiteUtilities.DUMMY)) {
                        labelText = labelString;
                    }
                    String themeString = stmt.column_string(2);
                    if (!themeString.equals(SpatialiteUtilities.DUMMY)) {
                        themeFieldValue = themeString;
                    }
//                        switch (stmt.column_type(1)) {
//                            case Constants.SQLITE_INTEGER: {
//                                labelText = labelText + stmt.column_int(1);
//                            }
//                            break;
//                            case Constants.SQLITE_FLOAT: {
//                                labelText += String.format("%.5f", stmt.column_double(i));
//                            }
//                            break;
//                            case Constants.SQLITE_BLOB: { // not supported
//                            }
//                            break;
//                            case Constants.SQLITE3_TEXT: {
//                                labelText += stmt.column_string(1);
//                            }
//                            break;
//                        }
                }
            }
        } catch (Exception e) {
            GPLog.error(this, "GeometryIterator.setLabelAndThemeText column_count[" + columnCount + "] column[" + i + "]", e);
        }
    }

    /**
     * Constructor.
     *
     * @param database the database to use.
     * @param query    the query to use.
     */
    public GeometryIterator(Database database, String query) {
        try {
            stmt = database.prepare(query);
        } catch (Exception e) {
            GPLog.error(this, "GeometryIterator.creation sql[" + query + "]", e);
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
            GPLog.error(this, "GeometryIterator.hasNext()[stmt.step() failed]", e);
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
            setLabelAndThemeText(stmt);
            return geometry;
        } catch (java.lang.Exception e) {
            GPLog.error(this, "GeometryIterator.next()[wkbReader.read() failed]", e);
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
     * @throws Exception if something goes wrong.
     */
    public void reset() throws Exception {
        if (stmt != null)
            stmt.reset();
    }

    /**
     * Close the iterator.
     *
     * @throws Exception if something goes wrong.
     */
    public void close() throws Exception {
        if (stmt != null)
            stmt.close();
    }
}
