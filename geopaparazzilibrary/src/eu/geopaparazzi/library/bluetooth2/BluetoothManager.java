package eu.geopaparazzi.library.bluetooth2;

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
import eu.geopaparazzi.library.util.debug.Logger;

public enum BluetoothManager {
    INSTANCE;

    private BluetoothAdapter _bluetooth;

    /**
     * Registered {@link IBluetoothStatusChangeListener status change listeners}.
     */
    private Set<IBluetoothStatusChangeListener> _statusChangeListeners = new HashSet<IBluetoothStatusChangeListener>();

    private BroadcastReceiver _bluetoothState;

    private BluetoothSocket _bluetoothSocket;

    private BluetoothDevice _bluetoothDevice;

    private BluetoothManager() {
        _bluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Checks if bt is supported.
     * 
     * <p>Might not be available on certain devices.</p>
     *
     * @return <code>true</code> if the device is supported.
     */
    public boolean isSupported() {
        return _bluetooth == null;
    }

    /**
     * Checks if the bt device is turned on.
     * 
     * @return <code>true</code> if the bt device is truned on.
     */
    public boolean isEnabled() {
        if (isSupported() && _bluetooth.isEnabled()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the device's hardware address.
     */
    public String getAddress() {
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
        if (isEnabled()) {
            return _bluetooth.getName();
        } else {
            return null;
        }
    }

    /**
     * Polls te state of the bt device.
     * 
     * <p>Use register to listen to {@link #startStateChangedListening(Context)} instead 
     * of using this.</p> 
     * 
     * @return the state of the bt device.
     */
    public int getState() {
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

                for( IBluetoothStatusChangeListener listener : _statusChangeListeners ) {
                    listener.bluetoothStatusChanged(previousState, state);
                }

                String tt = "";
                switch( state ) {
                case BluetoothAdapter.STATE_TURNING_ON:
                    tt = "Bluetooth turning on...";
                    break;
                case BluetoothAdapter.STATE_ON:
                    tt = "Bluetooth on...";
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    tt = "Bluetooth turning off...";
                    break;
                case BluetoothAdapter.STATE_OFF:
                    tt = "Bluetooth off...";
                    break;
                default:
                    break;
                }
                Logger.d(this, tt);
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
     * Set the {@link BluetoothDevice}.
     * 
     * <p>If another one is available, its socket will be closed and 
     * a new connection is made with the new device.</p>
     * 
     * @param bluetoothDevice the device to use.
     * @throws IOException 
     */
    public synchronized void setBluetoothDevice( BluetoothDevice bluetoothDevice ) throws IOException {
        if (_bluetoothSocket != null) {
            _bluetoothSocket.close();
        }
        _bluetoothDevice = bluetoothDevice;
    }

    /**
     * Get the bt socket.
     * 
     * <p>The socket is defined when the device is chosen.</p>
     * 
     * @return the active bt socket or <code>null</code>.
     * @throws Exception 
     */
    public synchronized BluetoothSocket getSocket() throws Exception {
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
     * Create a bluetooth (rfcomm) socket.
     * 
     * @param bluetoothDevice the device for which to create the socket for.
     * @return the created socket or <code>null</code>.
     * @throws Exception
     */
    private void createSocket() throws Exception {
        Method m = _bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}); //$NON-NLS-1$
        _bluetoothSocket = (BluetoothSocket) m.invoke(_bluetoothDevice, 1);
    }
}
