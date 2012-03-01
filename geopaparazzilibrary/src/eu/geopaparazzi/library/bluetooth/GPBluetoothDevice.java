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
 * A utility class used to manage the communication with the bluetooth GPS whn the connection has been established.
 * It is used to read NMEA data from the GPS or to send SIRF III binary commands or SIRF III NMEA commands to the GPS.
 * You should run the main read loop in one thread and send the commands in a separate one.   
 * 
 * @author Herbert von Broeuschmeul
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class GPBluetoothDevice extends Thread {
    private static final String LOG_TAG = "GPBluetoothDevice";
    /**
     * GPS bluetooth socket used for communication. 
     */
    private final BluetoothSocket socket;
    /**
     * GPS InputStream from which we read data. 
     */
    private final InputStream in;
    /**
     * GPS output stream to which we send data (SIRF III binary commands). 
     */
    private final OutputStream out;
    /**
     * GPS output stream to which we send data (SIRF III NMEA commands). 
     */
    private final PrintStream out2;

    /**
     * A boolean which indicates if the GPS is ready to receive data. 
     * In fact we consider that the GPS is ready when it begins to sends data...
     */
    private boolean ready = false;
    private boolean enabled;

    private List<IBluetoothListener> bluetoothListeners = new ArrayList<IBluetoothListener>();
    private final BluetoothNotificationHandler notificationHandler;

    public GPBluetoothDevice( BluetoothSocket socket, BluetoothNotificationHandler notificationHandler ) {
        this.socket = socket;
        this.notificationHandler = notificationHandler;
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
                Logger.e(LOG_TAG, "error while getting socket streams", e);
        }
        in = tmpIn;
        out = tmpOut;
        out2 = tmpOut2;
    }

    public boolean isReady() {
        return ready;
    }

    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }

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
                    if (Debug.D)
                        Logger.i(LOG_TAG, "data: " + System.currentTimeMillis() + " " + s);
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
                Logger.e(LOG_TAG, "error while getting data", e);
        } finally {
            // cleanly closing everything...
            this.close();
            notificationHandler.disable();
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
                    listener.onStringDataReceived(timestamp, sentence);
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
            if (Debug.D)
                Logger.e(LOG_TAG, "Exception during write", e);
        } catch (InterruptedException e) {
            if (Debug.D)
                Logger.e(LOG_TAG, "Exception during write", e);
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
            if (Debug.D)
                Logger.e(LOG_TAG, "Exception during write", e);
        }
    }

    /**
     * closing the connection to the device.
     */
    public void close() {
        ready = false;
        try {
            if (Debug.D)
                Logger.d(LOG_TAG, "closing Bluetooth GPS output sream");
            in.close();
        } catch (IOException e) {
            if (Debug.D)
                Logger.e(LOG_TAG, "error while closing GPS NMEA output stream", e);
        } finally {
            try {
                if (Debug.D)
                    Logger.d(LOG_TAG, "closing Bluetooth GPS input streams");
                out2.close();
                out.close();
            } catch (IOException e) {
                if (Debug.D)
                    Logger.e(LOG_TAG, "error while closing GPS input streams", e);
            } finally {
                try {
                    if (Debug.D)
                        Logger.d(LOG_TAG, "closing Bluetooth GPS socket");
                    socket.close();
                } catch (IOException e) {
                    if (Debug.D)
                        Logger.e(LOG_TAG, "error while closing GPS socket", e);
                }
            }
            bluetoothListeners.clear();
        }
    }

    /**
     * Adds an {@link IBluetoothListener}.
     * 
     * @param listener the listener to add.
     * @return true if the listener was added.
     */
    public boolean addListener( IBluetoothListener listener ) {
        if (!bluetoothListeners.contains(listener)) {
            bluetoothListeners.add(listener);
            return true;
        }
        return false;
    }

    /**
     * Removes an {@link IBluetoothListener}.
     * 
     * @param listener the listener to remove.
     */
    public void removeListener( IBluetoothListener listener ) {
        bluetoothListeners.remove(listener);
    }
}
