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

/**
 * Class to get info from {@link GpsStatus}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsStatusInfo {

    private GpsStatus status;
    private int maxSatellites;
    private int satCount;
    private int satUsedInFixCount;
    private Iterator<GpsSatellite> satellites;

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

    public int getMaxSatellites() {
        analyze();
        return maxSatellites;
    }

    public int getSatCount() {
        analyze();
        return satCount;
    }

    public int getSatUsedInFixCount() {
        analyze();
        return satUsedInFixCount;
    }

}
