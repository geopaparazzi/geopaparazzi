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

import java.util.Date;

import android.location.Location;
import eu.geopaparazzi.library.util.TimeUtilities;

/**
 * Extends the location with some infos.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsLocation extends Location {

    private Location previousLoc = null;

    /**
     * @param l {@link Location} object to wrap.
     */
    public GpsLocation( Location l ) {
        super(l);
    }

    /**
     * @return the previous location reference.
     */
    public Location getPreviousLoc() {
        return previousLoc;
    }

    /**
     * @param previousLoc sets previous location.
     */
    public void setPreviousLoc( Location previousLoc ) {
        this.previousLoc = previousLoc;
    }

    /**
     * @return the time string in UTC.
     */
    public String getTimeString() {
        String timeString = TimeUtilities.INSTANCE.TIME_FORMATTER_UTC.format(new Date(getTime()));
        return timeString;
    }

    /**
     * @return the sql time string in UTC.
     */
    public String getTimeStringSql() {
        String timeString = TimeUtilities.INSTANCE.TIME_FORMATTER_SQLITE_UTC.format(new Date(getTime()));
        return timeString;
    }

    /**
     * @return the timestamp.
     */
    public Date getTimeDate() {
        return new Date(getTime());
    }

    /**
     * @return the sql date.
     */
    public java.sql.Date getSqlDate() {
        return new java.sql.Date(getTime());
    }

    /**
     * @return the distance to the previous location.
     */
    public float distanceToPrevious() {
        if (previousLoc == null) {
            return 0;
        }
        return distanceTo(previousLoc);
    }
}
