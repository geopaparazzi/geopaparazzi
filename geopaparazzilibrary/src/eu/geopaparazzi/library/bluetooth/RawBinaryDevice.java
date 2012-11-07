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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothSocket;
import android.os.SystemClock;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * A generic binary device that reads the raw stream.   
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class RawBinaryDevice implements IBluetoothIOHandler {
    private BluetoothSocket socket;
    private InputStream in;
    private OutputStream out;
    private PrintStream out2;

    private boolean ready = false;
    private boolean enabled;

    private List<IBluetoothListener> bluetoothListeners = new ArrayList<IBluetoothListener>();

    public RawBinaryDevice() {
    }

    @Override
    public void initialize( BluetoothSocket socket ) {
        this.socket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        PrintStream tmpOut2 = null;
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
            if (tmpOut != null) {
                tmpOut2 = new PrintStream(tmpOut, false, "US-ASCII");
            }
        } catch (IOException e) {
            if (Debug.D)
                Logger.e(this, "error while getting socket streams", e);
        }
        in = tmpIn;
        out = tmpOut;
        out2 = tmpOut2;

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
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String s;
            long now = SystemClock.uptimeMillis();
            long lastRead = now;
            while( (enabled) && (now < lastRead + 5000) ) {
                if (reader.ready()) {
                    s = reader.readLine();
                    notifySentence(s + "\r\n");
                    ready = true;
                    lastRead = SystemClock.uptimeMillis();
                } else {
                    // if (Debug.D)
                    // Logger.d(LOG_TAG, "data: not ready " + System.currentTimeMillis());
                    SystemClock.sleep(50);
                }
                now = SystemClock.uptimeMillis();
            }
        } catch (IOException e) {
            if (Debug.D)
                Logger.e(this, "error while getting data", e);
        } finally {
            // cleanly closing everything...
            this.close();
        }
    }

    /**
     * Notifies the reception of a string from the bluetooth device to registered {@link IBluetoothListener}s.
     * 
     * @param sentence  the complete NMEA sentence received from the bluetooth GPS (i.e. $....*XY where XY is the checksum)
     */
    private void notifySentence( final String sentence ) {
        if (enabled) {
            final long timestamp = System.currentTimeMillis();
            if (sentence != null) {
                for( final IBluetoothListener listener : bluetoothListeners ) {
                    listener.onDataReceived(timestamp, sentence);
                }
            }
        }
    }

    @Override
    public void close() {
        ready = false;
        try {
            if (Debug.D)
                Logger.d(this, "closing output sream");
            in.close();
        } catch (IOException e) {
            if (Debug.D)
                Logger.e(this, "error while closing output stream", e);
        } finally {
            try {
                if (Debug.D)
                    Logger.d(this, "closing Bluetooth input streams");
                out2.close();
                out.close();
            } catch (IOException e) {
                if (Debug.D)
                    Logger.e(this, "error while closing input streams", e);
            } finally {
                try {
                    if (Debug.D)
                        Logger.d(this, "closing Bluetooth socket");
                    socket.close();
                } catch (IOException e) {
                    if (Debug.D)
                        Logger.e(this, "error while closing socket", e);
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
        if (adaptee.isAssignableFrom(RawBinaryDevice.class)) {
            return adaptee.cast(this);
        }
        return null;
    }
}
