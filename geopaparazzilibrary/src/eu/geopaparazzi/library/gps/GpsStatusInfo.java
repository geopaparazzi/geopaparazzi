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

import java.util.Iterator;

import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.os.SystemClock;
import eu.geopaparazzi.library.database.GPLog;

/**
 * Class to get info from {@link GpsStatus}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsStatusInfo {

    /**
     * The accepted time between a point and the other to say it has fix.
     * 
     * <p>This is 10 secs right now, because the pick interval is 1 sec.
     * If that changes, it should be related to the pick interval of the GPS.
     */
    private static final long FIX_TIME_INTERVAL_CHECK = 10000l;

    private GpsStatus status;
    private int maxSatellites;
    private int satCount;
    private int satUsedInFixCount;
    private Iterator<GpsSatellite> satellites;

    /**
     * @param status the status.
     */
    public GpsStatusInfo( GpsStatus status ) {
        this.status = status;
    }

    private void analyze() {
        if (satellites != null) {
            return;
        }
        satellites = status.getSatellites().iterator();
        maxSatellites = status.getMaxSatellites();

        satCount = 0;
        satUsedInFixCount = 0;
        while( satellites.hasNext() ) {
            GpsSatellite satellite = satellites.next();
            satCount++;
            if (satellite.usedInFix()) {
                satUsedInFixCount++;
            }
        }
    }

    /**
     * @return max satellites num.
     */
    public int getMaxSatellites() {
        analyze();
        return maxSatellites;
    }

    /**
     * @return sat count.
     */
    public int getSatCount() {
        analyze();
        return satCount;
    }

    /**
     * @return sat used in fix count.
     */
    public int getSatUsedInFixCount() {
        analyze();
        return satUsedInFixCount;
    }

    /**
     * Checks if there fix is still there based on the last picked location.
     * 
     * @param hasFix fix state previous to the check. 
     * @param lastLocationUpdateMillis the millis of the last picked location.
     * @param event the Gps status event triggered.
     * @return <code>true</code>, if it has fix.
     */
    @SuppressWarnings("nls")
    public static boolean checkFix( boolean hasFix, long lastLocationUpdateMillis, int event ) {
        switch( event ) {
        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            long diff = SystemClock.elapsedRealtime() - lastLocationUpdateMillis;
            if (GPLog.LOG_ABSURD)
                GPLog.addLogEntry("GPSSTATUSINFO", "gps event diff: " + diff);
            if (diff < FIX_TIME_INTERVAL_CHECK) {
                if (!hasFix) {
                    hasFix = true;
                }
            } else {
                if (hasFix) {
                    hasFix = false;
                }
            }
            break;
        }
        return hasFix;
    }

}
