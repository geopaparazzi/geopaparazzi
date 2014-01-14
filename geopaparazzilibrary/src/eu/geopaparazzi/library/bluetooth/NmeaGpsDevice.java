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
import eu.geopaparazzi.library.database.GPLog;

/**
 * A utility class used to manage the communication with the bluetooth GPS whn the connection has been established.
 * It is used to read NMEA data from the GPS or to send SIRF III binary commands or SIRF III NMEA commands to the GPS.
 * You should run the main read loop in one thread and send the commands in a separate one.   
 * 
 * @author Herbert von Broeuschmeul
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class NmeaGpsDevice implements IBluetoothIOHandler {
    /**
     * GPS bluetooth socket used for communication. 
     */
    private BluetoothSocket socket;
    /**
     * GPS InputStream from which we read data. 
     */
    private InputStream in;
    /**
     * GPS output stream to which we send data (SIRF III binary commands). 
     */
    private OutputStream out;
    /**
     * GPS output stream to which we send data (SIRF III NMEA commands). 
     */
    private PrintStream out2;

    /**
     * A boolean which indicates if the GPS is ready to receive data. 
     * In fact we consider that the GPS is ready when it begins to sends data...
     */
    private boolean ready = false;
    private boolean enabled;

    private List<IBluetoothListener> bluetoothListeners = new ArrayList<IBluetoothListener>();

    /* (non-Javadoc)
     * @see eu.geopaparazzi.library.bluetooth_tmp.IBluetoothDevice#prepare(android.bluetooth.BluetoothSocket, eu.geopaparazzi.library.bluetooth_tmp.BluetoothEnablementHandler)
     */
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
            error("error while getting socket streams", e);
        }
        in = tmpIn;
        out = tmpOut;
        out2 = tmpOut2;
    }

    private void error( String msg, Exception e ) {
        GPLog.error(this, msg, e);
    }
    private void log( String msg ) {
        if (GPLog.LOG)
            GPLog.addLogEntry(this, null, null, msg);
    }

    /* (non-Javadoc)
     * @see eu.geopaparazzi.library.bluetooth_tmp.IBluetoothDevice#isReady()
     */
    @Override
    public boolean isReady() {
        return ready;
    }

    /* (non-Javadoc)
     * @see eu.geopaparazzi.library.bluetooth_tmp.IBluetoothDevice#setEnabled(boolean)
     */
    @Override
    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }

    /* (non-Javadoc)
     * @see eu.geopaparazzi.library.bluetooth_tmp.IBluetoothDevice#getSocket()
     */
    @Override
    public BluetoothSocket getSocket() {
        return socket;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "US-ASCII"));
            String s;
            long now = SystemClock.uptimeMillis();
            long lastRead = now;
            while( (enabled) && (now < lastRead + 5000) ) {
                if (reader.ready()) {
                    s = reader.readLine();
                    // if (Debug.D)
                    // Logger.i(LOG_TAG, "data: " + System.currentTimeMillis() + " " + s);
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
            error("error while getting data", e);
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

    /**
     * Write to the connected OutStream.
     * 
     * @param buffer the bytes to write.
     */
    public void write( byte[] buffer ) {
        try {
            do {
                Thread.sleep(100);
            } while( (enabled) && (!ready) );
            if ((enabled) && (ready)) {
                out.write(buffer);
                out.flush();
            }
        } catch (IOException e) {
            error("Exception during write", e);
        } catch (InterruptedException e) {
            error("Exception during write", e);
        }
    }

    /**
     * Write to the connected OutStream.
     * 
     * @param buffer the data to write.
     */
    public void write( String buffer ) {
        try {
            do {
                Thread.sleep(100);
            } while( (enabled) && (!ready) );
            if ((enabled) && (ready)) {
                out2.print(buffer);
                out2.flush();
            }
        } catch (InterruptedException e) {
            error("Exception during write", e);
        }
    }

    /* (non-Javadoc)
     * @see eu.geopaparazzi.library.bluetooth_tmp.IBluetoothDevice#close()
     */
    @Override
    public void close() {
        ready = false;
        try {
            log("closing Bluetooth GPS output sream");
            in.close();
        } catch (IOException e) {
            error("error while closing GPS NMEA output stream", e);
        } finally {
            try {
                log("closing Bluetooth GPS input streams");
                out2.close();
                out.close();
            } catch (IOException e) {
                error("error while closing GPS input streams", e);
            } finally {
                try {
                    log("closing Bluetooth GPS socket");
                    socket.close();
                } catch (IOException e) {
                    error("error while closing GPS socket", e);
                }
            }
            bluetoothListeners.clear();
        }
    }

    /* (non-Javadoc)
     * @see eu.geopaparazzi.library.bluetooth_tmp.IBluetoothDevice#addListener(eu.geopaparazzi.library.bluetooth_tmp.IBluetoothListener)
     */
    @Override
    public boolean addListener( IBluetoothListener listener ) {
        if (!bluetoothListeners.contains(listener)) {
            bluetoothListeners.add(listener);
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see eu.geopaparazzi.library.bluetooth_tmp.IBluetoothDevice#removeListener(eu.geopaparazzi.library.bluetooth_tmp.IBluetoothListener)
     */
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
        if (adaptee.isAssignableFrom(NmeaGpsDevice.class)) {
            return adaptee.cast(this);
        }
        return null;
    }
}
