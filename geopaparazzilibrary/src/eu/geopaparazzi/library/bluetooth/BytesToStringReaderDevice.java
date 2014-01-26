/*
 * Geopaparazzi - Digital field mapping on Android based devices
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import eu.geopaparazzi.library.database.GPLog;

/**
 * A generic binary device that reads the raw stream and converts it to hex strings.   
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class BytesToStringReaderDevice implements IBluetoothIOHandler {

    private BluetoothSocket socket;
    private InputStream in;
    private OutputStream out;

    private boolean ready = false;
    private boolean enabled;

    private List<IBluetoothListener> bluetoothListeners = new ArrayList<IBluetoothListener>();

    private final int length;

    /**
     * Constructor.
     * 
     * @param length buffer length.
     */
    public BytesToStringReaderDevice( int length ) {
        this.length = length;
    }

    @Override
    public void initialize( BluetoothSocket socket ) {
        this.socket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            if (GPLog.LOG)
                GPLog.addLogEntry(this, null, null, "error while getting socket streams");
        }
        in = tmpIn;
        out = tmpOut;

        ready = true;
        enabled = true;
        new Thread(new Runnable(){
            public void run() {
                startDevice();
            }
        }).start();
    }

    private void startDevice() {
        run();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }

    @Override
    public BluetoothSocket getSocket() {
        return socket;
    }

    public void run() {
        try {
            final byte[] data = new byte[length];
            long now = SystemClock.uptimeMillis();
            long lastRead = now;
            while( (enabled) && (now < lastRead + 5000) ) {
                int index = 0;
                int value = 0;
                while( enabled && (value = in.read()) != -1 ) {
                    data[index++] = (byte) value;
                    if (index == length) {
                        String str = new String(data, 0, length).trim();
                        if (GPLog.LOG_HEAVY)
                            GPLog.addLogEntry(this, null, null, "data read: " + str);
                        notifyBytes(str);
                        index = 0;
                    }
                }
                // Logger.i(this, "Exit");

                ready = true;
                lastRead = SystemClock.uptimeMillis();
                now = SystemClock.uptimeMillis();
            }
        } catch (IOException e) {
            GPLog.error(this, null, e);
        } finally {
            // cleanly closing everything...
            this.close();
        }
    }

    private void notifyBytes( final String hexString ) {
        if (enabled) {
            final long timestamp = System.currentTimeMillis();
            if (hexString != null) {
                for( final IBluetoothListener listener : bluetoothListeners ) {
                    listener.onDataReceived(timestamp, hexString);
                }
            }
        }
    }

    @Override
    public void close() {
        ready = false;
        try {
            if (GPLog.LOG)
                GPLog.addLogEntry(this, null, null, "closing output sream");
            in.close();
        } catch (IOException e) {
            GPLog.error(this, null, e);
        } finally {
            try {
                if (GPLog.LOG)
                    GPLog.addLogEntry(this, null, null, "closing Bluetooth input streams");
                out.close();
            } catch (IOException e) {
                GPLog.error(this, null, e);
            } finally {
                try {
                    if (GPLog.LOG)
                        GPLog.addLogEntry(this, null, null, "closing Bluetooth socket");
                    socket.close();
                } catch (IOException e) {
                    GPLog.error(this, null, e);
                }
            }
            bluetoothListeners.clear();
        }
    }

    @Override
    public boolean addListener( IBluetoothListener listener ) {
        if (!bluetoothListeners.contains(listener)) {
            bluetoothListeners.add(listener);
            return true;
        }
        return false;
    }

    @Override
    public void removeListener( IBluetoothListener listener ) {
        bluetoothListeners.remove(listener);
    }

    @Override
    public String checkRequirements() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public <T> T adapt( Class<T> adaptee ) {
        if (adaptee.isAssignableFrom(BytesToStringReaderDevice.class)) {
            return adaptee.cast(this);
        }
        return null;
    }
}
