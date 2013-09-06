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

import android.bluetooth.BluetoothSocket;

/**
 * Represents an IO handler for bluetooth sockets.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IBluetoothIOHandler extends Runnable {

    /**
     * Get the vendor name of the device.
     * 
     * @return the name of the device.
     */
    public String getName();

    /**
     * Checks the requirements of the device against the operating system. 
     * 
     * @return <code>null</code> if requirements are fine. Summary of problems in case of warnings or errors.
     */
    public String checkRequirements();

    /**
     * Initialize the device by passing in the socket and an enablement handler.
     * 
     * @param socket the socket to use for dialog with the device.
     */
    public abstract void initialize( BluetoothSocket socket );

    /**
     * Close the connection to the device.
     */
    public abstract void close();

    /**
     * Checks if the device is ready to transmit data.
     * 
     * <p>Note that a device could have some latency from the moment it got switched on.</p>
     * 
     * @return <code>true</code> if the device is ready to transmit data.
     */
    public abstract boolean isReady();

    /**
     * Enable or disable the device.
     * 
     * @param enabled <code>true</code> if the device should be enabled.
     */
    public abstract void setEnabled( boolean enabled );

    /**
     * Getter for the {@link BluetoothSocket} used.
     * 
     * @return the {@link BluetoothSocket} used.
     */
    public abstract BluetoothSocket getSocket();

    /**
     * Adds an {@link IBluetoothListener}.
     * 
     * @param listener the listener to add.
     * @return true if the listener was added.
     */
    public abstract boolean addListener( IBluetoothListener listener );

    /**
     * Removes an {@link IBluetoothListener}.
     * 
     * @param listener the listener to remove.
     */
    public abstract void removeListener( IBluetoothListener listener );

    /**
     * Adapt the device.
     * 
     * @param adaptee the class to adapt to.
     * @return the adapted object.
     */
    public <T> T adapt( Class<T> adaptee );
}