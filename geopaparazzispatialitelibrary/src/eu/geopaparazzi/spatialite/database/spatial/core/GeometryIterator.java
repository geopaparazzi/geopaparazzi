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
import jsqlite.Constants;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;
import eu.geopaparazzi.library.database.GPLog;

/**
 * Class that iterates over Database geometries and doesn't keep everything in memory.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeometryIterator implements Iterator<Geometry> {
    private WKBReader wkbReader = new WKBReader();
    // private WKTReader wktReader = new WKTReader();
    private Stmt stmt;
    private String s_label="";
    /**
     * Returns Label String (if any)
     * - if any label is being supported, will build s_label from column 1 to end
     * -- each column (after 1) will have a ', ' inserted
     * -- s_label will be empty if no label was requested
     * @param stmt statement being executed
     * @return s_label
     */
    public String get_label_text()
    {
     return s_label;
    }
    /**
     * Builds Label String (if any)
     * - assumes that column 0 is ALWAYS a Geometry
     * - if any label is being supported, will build s_label from column 1 to end
     * -- each column (after 1) will have a ', ' inserted
     * -- s_label will be set to blank before filling
     * @param stmt statement being executed
     * @return nothing
     */
    private void set_label_text(Stmt stmt)
    {
     s_label="";
     int i=1;
     int i_column_count=0;
     try
     {
      if ((stmt != null) && (stmt.column_count() > 1))
      {
       i_column_count=stmt.column_count();
       for (i=1;i<i_column_count;i++)
       {
        if (!s_label.equals(""))
        {
         s_label+=", ";
        }
        switch (stmt.column_type(i))
        {
         case Constants.SQLITE_INTEGER:
         {
          s_label=s_label+stmt.column_int(i);
         }
         break;
         case Constants.SQLITE_FLOAT:
         {
          s_label+=String.format( "%.5f",stmt.column_double(i));
         }
         break;
         case Constants.SQLITE_BLOB:
         { // not supported
         }
         break;
         case Constants.SQLITE3_TEXT:
         {
          s_label+=stmt.column_string(i);
         }
         break;
        }
       }
      }
     }
     catch (Exception e)
     {
      GPLog.androidLog(4,"GeometryIterator.set_label_text column_count["+i_column_count+"] column["+i+"]",e);
     }
    }
    public GeometryIterator( Database database, String query ) {
        try {
            stmt = database.prepare(query);
        } catch (Exception e) {
            GPLog.androidLog(4,"GeometryIterator.creation sql["+query+"]",e);
        }
    }

    @Override
    public boolean hasNext() {
        if (stmt == null)
        {
         return false;
        }
        try
        { // sqlite-amalgamation-3080100 allways returns false with BLOBS
            return stmt.step();
        } catch (Exception e) {
         GPLog.androidLog(4,"GeometryIterator.hasNext()[stmt.step() failed]",e);
            return false;
        }
    }

    @Override
    public Geometry next() {
        s_label="";
        if (stmt == null)
        {
          GPLog.androidLog(4,"GeometryIterator.next() [stmt=null]");
            return null;
         }
        try {
            byte[] geomBytes = stmt.column_bytes(0);
            Geometry geometry = wkbReader.read(geomBytes);
            set_label_text(stmt);
            return geometry;
        } catch (java.lang.Exception e) {
            GPLog.androidLog(4,"GeometryIterator.next()[wkbReader.read() failed]",e);
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
