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

/**
 * Enum defining possible status of the {@link GpsService}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum GpsServiceStatus {
    /**
     * GPS is switched off.
     */
    GPS_OFF(0), //
    /**
     * GPS is switched on but not register to location updates.
     */
    GPS_ON__NO_LISTENING(1), //
    /**
     * GPS is registered for location updates but has no fix.
     */
    GPS_LISTENING__NO_FIX(2), //
    /**
     * GPS has fix.
     */
    GPS_FIX(3);

    private int code;

    private GpsServiceStatus( int code ) {
        this.code = code;
    }

    /**
     * @return the code of this status.
     */
    public int getCode() {
        return code;
    }

    /**
     * Get the {@link GpsServiceStatus} for a given code.
     * 
     * @param code the code to check.
     * @return the status.
     */
    public static GpsServiceStatus getStatusForCode( int code ) {
        GpsServiceStatus[] values = values();
        for( GpsServiceStatus gpsServiceStatus : values ) {
            if (code == gpsServiceStatus.getCode()) {
                return gpsServiceStatus;
            }
        }
        throw new IllegalArgumentException("No service status available for code: " + code); //$NON-NLS-1$
    }
}
