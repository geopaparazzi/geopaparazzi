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

import android.database.sqlite.SQLiteDatabase;

/**
 * Interface that helps adding points to external databases. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IGpsLogDbHelper {

    /**
     * Get the database.
     * 
     * @return a writable database.
     * @throws Exception  if something goes wrong.
     */
    public SQLiteDatabase getDatabase() throws Exception;

    /**
     * Creates a new gpslog entry and returns the log's new id.
     * 
     * @param startTs the start timestamp.
     * @param endTs the end timestamp.
     * @param lengthm the length of the log in meters
     * @param text a description or null.
     * @param width the width of the rendered log. 
     * @param color the color of the rendered log.
     * @param visible if <code>true</code>, it will be visible.
     * @return the id of the new created log.
     * @throws IOException  if something goes wrong. 
     */
    public long addGpsLog( Date startTs, Date endTs, double lengthm, String text, float width, String color, boolean visible )
            throws IOException;

    /**
     * Adds a single gps log point to a log.
     * 
     * <p>Transactions have to be opened and closed if necessary.</p>
     * 
     * @param sqliteDatabase the db to use.
     * @param gpslogId the log id to which to add to.
     * @param lon the lon coordinate.
     * @param lat the lat coordinate.
     * @param altim the elevation of the point.
     * @param timestamp the timestamp of the point.
     * @throws IOException  if something goes wrong.
     */
    public void addGpsLogDataPoint( SQLiteDatabase sqliteDatabase, long gpslogId, double lon, double lat, double altim,
            Date timestamp ) throws IOException;

    /**
     * Deletes a gps log from the database. 
     * 
     * @param id the log's id.
     * @throws IOException  if something goes wrong.
     */
    public void deleteGpslog( long id ) throws IOException;

    /**
     * Re-sets the end timestamp, in case it changed because points were added.
     * 
     * @param logid the log to change. 
     * @param end the end timestamp.
     * @throws IOException  if something goes wrong.
     */
    public void setEndTs( long logid, Date end ) throws IOException;

    /**
     * Re-sets the log (track) length.
     * 
     * @param logid the log to change. 
     * @param length the length of the track log
     * @throws IOException  if something goes wrong.
     */
    public void setTrackLengthm( long logid, double length ) throws IOException;

}
