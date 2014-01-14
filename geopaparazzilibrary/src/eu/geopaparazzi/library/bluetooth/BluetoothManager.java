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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import eu.geopaparazzi.library.database.GPLog;

/**
 * A singleton to manage bluetooth.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum BluetoothManager {
    /**
     * 
     */
    INSTANCE;

    private BluetoothAdapter _bluetooth;

    /**
     * Registered {@link IBluetoothStatusChangeListener status change listeners}.
     */
    private Set<IBluetoothStatusChangeListener> _statusChangeListeners = new HashSet<IBluetoothStatusChangeListener>();

    private BroadcastReceiver _bluetoothState;

    private BluetoothSocket _bluetoothSocket;

    private BluetoothDevice _bluetoothDevice;

    private boolean isSocketConnected = false;

    private boolean isDummy = false;

    private IBluetoothIOHandler iBluetoothDevice;

    private BluetoothManager() {
        _bluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Create dummy instance.
     */
    public void makeDummy() {
        try {
            reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // needs to be set afterwards, since reset puts it to false
        isDummy = true;
    }

    /**
     * Checks if bt is supported.
     * 
     * <p>Might not be available on certain devices.</p>
     *
     * @return <code>true</code> if the device is supported.
     */
    public boolean isSupported() {
        if (isDummy)
            return true;
        return _bluetooth != null;
    }

    /**
     * Checks if the bt device is turned on.
     * 
     * @return <code>true</code> if the bt device is truned on.
     */
    public boolean isEnabled() {
        if (isDummy)
            return true;
        boolean supported = isSupported();
        boolean enabled = _bluetooth.isEnabled();
        if (supported && enabled) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the device's hardware address.
     */
    public String getAddress() {
        if (isDummy)
            return "dummyaddress"; //$NON-NLS-1$
        if (isEnabled()) {
            return _bluetooth.getAddress();
        } else {
            return null;
        }
    }

    /**
     * @return the device's userfriendly name.
     */
    public String getName() {
        if (isDummy)
            return "dummy device"; //$NON-NLS-1$
        if (isEnabled()) {
            return _bluetooth.getName();
        } else {
            return null;
        }
    }

    /**
     * Polls te state of the bt device.
     * 
     * @return the state of the bt device.
     */
    public int getState() {
        if (isDummy)
            return BluetoothAdapter.STATE_ON;
        if (isEnabled()) {
            return _bluetooth.getState();
        } else {
            return BluetoothAdapter.STATE_OFF;
        }
    }

    /**
     * Enable the bluetooth adapter.
     * 
     * <p>
     * This launches an activity to do so and returns when the bt device has
     * been swiced on.
     * </p>
     * <p>
     * The parentActivity will have to check the onActivityResult method like:
     * <pre>
     * if(requestCode == myCode){
     *      if(resultCode == RESULT_OK){
     *          // BT enabled
     *      }
     * }
     * </pre>
     * </p>
     * 
     * @param parentActivity the {@link Activity} to use for the bt activity to start.
     * @param requestCode the request code.
     */
    public void enable( Activity parentActivity, int requestCode ) {
        parentActivity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), requestCode);
    }

    /**
     * Adds a status change listener.
     * 
     * @param listener the {@link IBluetoothStatusChangeListener listener} to add.
     */
    public void addStatusChangedListener( IBluetoothStatusChangeListener listener ) {
        _statusChangeListeners.add(listener);
    }

    /**
     * Removes a status change listener.
     * 
     * @param listener the {@link IBluetoothStatusChangeListener listener} to remove.
     */
    public void removeStatusChangedListener( IBluetoothStatusChangeListener listener ) {
        _statusChangeListeners.remove(listener);
    }

    /**
     * Starts to listen to status changes.
     * 
     * @param context the {@link Context} to use.
     */
    public void startStatusChangeListening( Context context ) {
        _bluetoothState = new BroadcastReceiver(){
            @Override
            public void onReceive( Context context, Intent intent ) {
                String prevStateExtra = BluetoothAdapter.EXTRA_PREVIOUS_STATE;
                String stateExtra = BluetoothAdapter.EXTRA_STATE;
                int state = intent.getIntExtra(stateExtra, -1);
                int previousState = intent.getIntExtra(prevStateExtra, -1);

                if (state != previousState) {
                    for( IBluetoothStatusChangeListener listener : _statusChangeListeners ) {
                        listener.bluetoothStatusChanged(previousState, state);
                    }
                }

                String tt = ""; //$NON-NLS-1$
                switch( state ) {
                case BluetoothAdapter.STATE_TURNING_ON:
                    tt = "Bluetooth turning on..."; //$NON-NLS-1$
                    break;
                case BluetoothAdapter.STATE_ON:
                    tt = "Bluetooth on..."; //$NON-NLS-1$
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    tt = "Bluetooth turning off..."; //$NON-NLS-1$
                    break;
                case BluetoothAdapter.STATE_OFF:
                    tt = "Bluetooth off..."; //$NON-NLS-1$
                    break;
                default:
                    break;
                }
                if (GPLog.LOG)
                    GPLog.addLogEntry(this, null, null, tt);
            }
        };

        context.registerReceiver(_bluetoothState, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    /**
     * Stop listening to status changes.
     * 
     * @param context the {@link Context} to use.
     */
    public void stopStatusChangeListening( Context context ) {
        if (_bluetoothState != null) {
            context.unregisterReceiver(_bluetoothState);
        }
    }

    /**
     * Get the set of paired devices.
     * 
     * @return the set of paired devices or an empty set.
     */
    public Set<BluetoothDevice> getBondedDevices() {
        if (isEnabled()) {
            return _bluetooth.getBondedDevices();
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Get a {@link BluetoothDevice} by its address.
     * 
     * @param address te bt device address.
     * @return the device or <code>null</code>.
     */
    public BluetoothDevice getBluetoothDeviceByAddress( String address ) {
        if (isEnabled()) {
            return _bluetooth.getRemoteDevice(address);
        } else {
            return null;
        }
    }

    /**
     * Get a {@link BluetoothDevice} by its name.
     * 
     * @param name te bt device name.
     * @return the device or <code>null</code>.
     */
    public BluetoothDevice getBluetoothDeviceByName( String name ) {
        if (isEnabled()) {
            Set<BluetoothDevice> bondedDevices = getBondedDevices();
            for( BluetoothDevice bluetoothDevice : bondedDevices ) {
                if (bluetoothDevice.getName().equals(name)) {
                    return bluetoothDevice;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    /**
     * Set the {@link BluetoothDevice}.
     * 
     * <p>If another one is available, its socket will be closed and 
     * a new connection is made with the new device.</p>
     * 
     * @param bluetoothDevice the device to use.
     * @param connect if <code>true</code>, also connect to the socket.
     * @throws Exception  if something goes wrong. 
     */
    public synchronized void setBluetoothDevice( BluetoothDevice bluetoothDevice, boolean connect ) throws Exception {
        reset();
        _bluetoothDevice = bluetoothDevice;
        // reset
        if (iBluetoothDevice != null) {
            iBluetoothDevice.close();
            iBluetoothDevice = null;
        }
        if (connect) {
            getSocket();
        }
    }

    /**
     * Reset the current bluetooth socket and device.
     * 
     * @throws Exception  if something goes wrong.
     */
    public synchronized void reset() throws Exception {
        if (_bluetoothSocket != null) {
            _bluetoothSocket.close();
            isSocketConnected = false;
            _bluetoothSocket = null;
        }
        _bluetoothDevice = null;
        isDummy = false;
    }

    /**
     * Get the bt socket.
     * 
     * <p>The socket is defined when the device is chosen.</p>
     * 
     * @return the active bt socket or <code>null</code>.
     * @throws Exception  if something goes wrong. 
     */
    public synchronized BluetoothSocket getSocket() throws Exception {
        if (isDummy)
            return null;
        if (_bluetoothSocket == null) {
            if (isEnabled()) {
                createSocket();
            } else {
                throw new RuntimeException();
            }
        }
        return _bluetoothSocket;
    }

    /**
     * @return the current bluetooth device.
     */
    public synchronized BluetoothDevice getCurrentBluetoothDevice() {
        return _bluetoothDevice;
    }

    /**
     * Create a bluetooth (rfcomm) socket and connect to it.
     * 
     * @throws Exception
     */
    private void createSocket() throws Exception {
        Method m = _bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}); //$NON-NLS-1$
        _bluetoothSocket = (BluetoothSocket) m.invoke(_bluetoothDevice, 1);
        _bluetoothSocket.connect();
        isSocketConnected = true;
    }

    /**
     * @return <code>true</code>, if the device is ready to transfer data through the socket. 
     */
    public boolean isIOReady() {
        if (isDummy)
            return true;
        return isSocketConnected;
    }

    /**
     * Initializes 
     * 
     * @param iBluetoothDevice the bt handler.
     * @throws IOException  if something goes wrong.
     */
    public void initializeIBluetoothDeviceInternal( IBluetoothIOHandler iBluetoothDevice ) throws IOException {
        this.iBluetoothDevice = iBluetoothDevice;
        if (isDummy)
            return;
        if (isSocketConnected) {
            iBluetoothDevice.initialize(_bluetoothSocket);
        } else {
            throw new IOException("No socket connected."); //$NON-NLS-1$
        }
    }

    /**
     * @return the bt device.
     */
    public IBluetoothIOHandler getBluetoothDevice() {
        return iBluetoothDevice;
    }
}
