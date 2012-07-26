package eu.geopaparazzi.library.bluetooth2;

import android.bluetooth.BluetoothAdapter;

/**
 * Interface for BT status change listeners.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface IBluetoothStatusChangeListener {

    /**
     * Called when a bt status change occurrs.
     * 
     * @param oldStatus the previous status.
     * @param newStatus the status after the change (one of {@link BluetoothAdapter#STATE_TURNING_ON} and similar).
     */
    public void bluetoothStatusChanged( int oldStatus, int newStatus );
}
