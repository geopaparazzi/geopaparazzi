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
package eu.geopaparazzi.library.bluetooth;

/**
 * A bluetooth listener interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface IBluetoothListener {

    /**
     * Method triggered when string data are recieved by the bluetooth.
     * 
     * @param time the timestamp.
     * @param data the data retrieved.
     */
    public void onDataReceived( long time, Object data );
}
