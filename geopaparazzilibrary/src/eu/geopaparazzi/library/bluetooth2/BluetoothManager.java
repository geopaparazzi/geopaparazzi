package eu.geopaparazzi.library.bluetooth2;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import eu.geopaparazzi.library.util.debug.Logger;

public enum BluetoothManager {
    INSTANCE;

    private BluetoothAdapter bluetooth;

    /**
     * Registered {@link IBluetoothStatusChangeListener status change listeners}.
     */
    private Set<IBluetoothStatusChangeListener> statusChangeListeners = new HashSet<IBluetoothStatusChangeListener>();

    private BroadcastReceiver bluetoothState;

    private BluetoothManager() {
        bluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Checks if bt is supported.
     * 
     * <p>Might not be available on certain devices.</p>
     *
     * @return <code>true</code> if the device is supported.
     */
    public boolean isSupported() {
        return bluetooth == null;
    }

    /**
     * Checks if the bt device is turned on.
     * 
     * @return <code>true</code> if the bt device is truned on.
     */
    public boolean isEnabled() {
        if (isSupported() && bluetooth.isEnabled()) {
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
            return bluetooth.getAddress();
        } else {
            return null;
        }
    }

    /**
     * @return the device's userfriendly name.
     */
    public String getName() {
        if (isEnabled()) {
            return bluetooth.getName();
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
            return bluetooth.getState();
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
        statusChangeListeners.add(listener);
    }

    /**
     * Removes a status change listener.
     * 
     * @param listener the {@link IBluetoothStatusChangeListener listener} to remove.
     */
    public void removeStatusChangedListener( IBluetoothStatusChangeListener listener ) {
        statusChangeListeners.remove(listener);
    }

    /**
     * Starts to listen to status changes.
     * 
     * @param context the {@link Context} to use.
     */
    public void startStatusChangeListening( Context context ) {
        bluetoothState = new BroadcastReceiver(){
            @Override
            public void onReceive( Context context, Intent intent ) {
                String prevStateExtra = BluetoothAdapter.EXTRA_PREVIOUS_STATE;
                String stateExtra = BluetoothAdapter.EXTRA_STATE;
                int state = intent.getIntExtra(stateExtra, -1);
                int previousState = intent.getIntExtra(prevStateExtra, -1);

                for( IBluetoothStatusChangeListener listener : statusChangeListeners ) {
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

        context.registerReceiver(bluetoothState, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    /**
     * Stop listening to status changes.
     * 
     * @param context the {@link Context} to use.
     */
    public void stopStatusChangeListening( Context context ) {
        if (bluetoothState != null) {
            context.unregisterReceiver(bluetoothState);
        }
    }

}
