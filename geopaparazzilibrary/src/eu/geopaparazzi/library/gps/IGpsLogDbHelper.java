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
package eu.geopaparazzi.library.gps;

import java.io.IOException;
import java.sql.Date;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Interface that helps making the {@link GpsDatabaseLogger} add point to external databases. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IGpsLogDbHelper {

    /**
     * Get the database.
     * 
     * @param context the {@link Context} to use.
     * @return a writable database.
     */
    public SQLiteDatabase getDatabase( Context context ) throws Exception;

    /**
     * Creates a new gpslog entry and returns the log's new id.
     * 
     * @param context the {@link Context} to use.
     * @param startTs the start timestamp.
     * @param endTs the end timestamp.
     * @param text a description or null.
     * @param width the width of the rendered log. 
     * @param color the color of the rendered log.
     * @param visible if <code>true</code>, it will be visible.
     * @return the id of the new created log.
     * @throws IOException 
     */
    public long addGpsLog( Context context, Date startTs, Date endTs, String text, float width, String color, boolean visible )
            throws IOException;

    /**
     * Adds a single gps log point to a log.
     * 
     * <p>Transactions have to be opened and closed.</p>
     * 
     * @param sqliteDatabase the db to use.
     * @param gpslogId the log id to which to add to.
     * @param lon the lon coordinate.
     * @param lat the lat coordinate.
     * @param altim the elevation of the point.
     * @param timestamp the timestamp of the point.
     * @throws IOException
     */
    public void addGpsLogDataPoint( SQLiteDatabase sqliteDatabase, long gpslogId, double lon, double lat, double altim,
            Date timestamp ) throws IOException;

    /**
     * Deletes a gps log from the database. 
     * 
     * @param context the {@link Context} to use.
     * @param id the log's id.
     * @throws IOException
     */
    public void deleteGpslog( Context context, long id ) throws IOException;

    /**
     * Re-sets the end timestamp, in case it changed because points were added.
     * 
     * @param context the {@link Context} to use.
     * @param logid the log to change. 
     * @param end the end timestamp.
     * @throws IOException
     */
    public void setEndTs( Context context, long logid, Date end ) throws IOException;
}
