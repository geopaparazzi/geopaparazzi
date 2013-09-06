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

import android.location.GpsStatus;
import android.location.LocationListener;

/**
 * Listener for GPS.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface GpsManagerListener extends LocationListener {

    public void gpsStart();

    public void gpsStop();

    /**
     * Handles gps status changes.
     * 
     * <p>Here the fix can be checked through something like:<br>
     * 
     * <pre>
     *  switch( event ) {
     *   case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
     *       if ((SystemClock.elapsedRealtime() - lastLocationupdateMillis) < (GpsManager.WAITSECONDS * 2000l)) {
     *           if (!gotFix) {
     *               gotFix = true;
     *           }
     *       } else {
     *           if (gotFix) {
     *               gotFix = false;
     *           }
     *       }
     *       break;
     *   }
     * </pre>
     * 
     * <p>where <code>lastLocationupdateMillis</code> should be set in 
     * the {@link LocationListener#onLocationChanged(android.location.Location)} method 
     * like: 
     * <pre>
     *   lastLocationupdateMillis = SystemClock.elapsedRealtime();
     * </pre>
     * 
     * @param event
     * @param status
     */
    public void onGpsStatusChanged( int event, GpsStatus status );

    /**
     * Check on available fix and data.
     * 
     * @return <code>true</code> if fix is available and data are valid.
     */
    public boolean hasFix();
}