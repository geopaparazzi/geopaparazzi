/*
 * Copyright (C) 2010, 2011 Herbert von Broeuschmeul
 * Copyright (C) 2010, 2011 BluetoothGPS4Droid Project
 * 
 * This file is part of BluetoothGPS4Droid.
 *
 * BluetoothGPS4Droid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * BluetoothGPS4Droid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with BluetoothGPS4Droid. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.geopaparazzi.library.bluetooth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.location.GpsStatus.NmeaListener;
import android.os.SystemClock;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * This class is used to establish and manage the connection with the bluetooth GPS.
 * 
 * @author Herbert von Broeuschmeul
 *
 */
public enum BluetoothManager2 {
    BLUETOOTHMANAGER;

    /**
     * Tag used for log messages
     */
    private static final String LOG_TAG = "BluetoothManager";

    /**
     * A utility class used to manage the communication with the bluetooth GPS whn the connection has been established.
     * It is used to read NMEA data from the GPS or to send SIRF III binary commands or SIRF III NMEA commands to the GPS.
     * You should run the main read loop in one thread and send the commands in a separate one.   
     * 
     * @author Herbert von Broeuschmeul
     *
     */
    private class ConnectedGps extends Thread {
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

        public ConnectedGps( BluetoothSocket socket ) {
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
                        if (Debug.D)
                            Logger.d(LOG_TAG, "data: not ready " + System.currentTimeMillis());
                        SystemClock.sleep(500);
                    }
                    now = SystemClock.uptimeMillis();
                }
            } catch (IOException e) {
                if (Debug.D)
                    Logger.e(LOG_TAG, "error while getting data", e);
            } finally {
                // cleanly closing everything...
                this.close();
                disableIfNeeded();
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
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
         * @param buffer  The data to write
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
            }
        }
    }

    private BluetoothSocket gpsSocket;
    private String gpsDeviceAddress;
    private boolean enabled = false;
    private ScheduledExecutorService connectionAndReadingPool;
    private List<IBluetoothListener> bluetoothListeners = new ArrayList<IBluetoothListener>();
    private ConnectedGps connectedGps;
    private int disableReason = 0;
    private Notification connectionProblemNotification;
    private Notification serviceStoppedNotification;
    private Context appContext;
    private NotificationManager notificationManager;
    private int maxConnectionRetries;
    private int nbRetriesRemaining;
    private boolean connected = false;

    /**
     * @param callingService
     * @param deviceAddress
     * @param maxRetries
     */
    public void connect( Context context, String deviceAddress, int maxRetries ) {
        this.gpsDeviceAddress = deviceAddress;
        this.maxConnectionRetries = maxRetries;
        this.nbRetriesRemaining = 1 + maxRetries;
        this.appContext = context;
        // sharedPreferences = PreferenceManager.getDefaultSharedPreferences(callingService);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // connectionProblemNotification = new Notification();
        // connectionProblemNotification.icon = R.drawable.ic_stat_notify;
        // Intent stopIntent = new Intent(BluetoothProviderService.ACTION_STOP_GPS_PROVIDER);
        // // PendingIntent stopPendingIntent = PendingIntent.getService(appContext, 0, stopIntent,
        // // PendingIntent.FLAG_CANCEL_CURRENT);
        // PendingIntent stopPendingIntent = PendingIntent.getService(appContext, 0, stopIntent,
        // PendingIntent.FLAG_CANCEL_CURRENT);
        // connectionProblemNotification.contentIntent = stopPendingIntent;
        //
        // serviceStoppedNotification = new Notification();
        // serviceStoppedNotification.icon = R.drawable.ic_stat_notify;
        // Intent restartIntent = new Intent(BluetoothProviderService.ACTION_START_GPS_PROVIDER);
        // PendingIntent restartPendingIntent = PendingIntent.getService(appContext, 0,
        // restartIntent,
        // PendingIntent.FLAG_CANCEL_CURRENT);
        // serviceStoppedNotification.setLatestEventInfo(appContext,
        // appContext.getString(R.string.service_closed_because_connection_problem_notification_title),
        // appContext.getString(R.string.service_closed_because_connection_problem_notification),
        // restartPendingIntent);
    }

    /**
     * Notifies the reception of a NMEA sentence from the bluetooth GPS to registered NMEA listeners.
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

    private void setDisableReason( int reasonId ) {
        disableReason = reasonId;
    }

    /**
     * @return
     */
    public int getDisableReason() {
        return disableReason;
    }

    /**
     * @return true if the bluetooth GPS is enabled
     */
    public synchronized boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables the bluetooth GPS Provider.
     * @return
     */
    public synchronized boolean enable() {
        notificationManager.cancel(R.string.service_closed_because_connection_problem_notification_title);
        if (!enabled) {
            if (Debug.D)
                Logger.d(LOG_TAG, "enabling Bluetooth GPS manager");
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                // Device does not support Bluetooth
                if (Debug.D)
                    Logger.w(LOG_TAG, "Device does not support Bluetooth");
                disable(R.string.msg_bluetooth_unsupported);
            } else if (!bluetoothAdapter.isEnabled()) {
                // Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                if (Debug.D)
                    Logger.w(LOG_TAG, "Bluetooth is not enabled");
                disable(R.string.msg_bluetooth_disabled);
                // } else if (Settings.Secure.getInt(callingService.getContentResolver(),
                // Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 0) {
                // if(Debug.D) Logger.e(LOG_TAG, "Mock location provider OFF");
                // disable(R.string.msg_mock_location_disabled);
                // } else if ( (! locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                // &&
                // (sharedPreferences.getBoolean(BluetoothProviderService.PREF_REPLACE_STD_GPS,
                // true))
                // ) {
                // if(Debug.D) Logger.e(LOG_TAG, "GPS location provider OFF");
                // disable(R.string.msg_gps_provider_disabled);
            } else {
                final BluetoothDevice gpsDevice = bluetoothAdapter.getRemoteDevice(gpsDeviceAddress);
                if (gpsDevice == null) {
                    if (Debug.D)
                        Logger.w(LOG_TAG, "GPS device not found");
                    disable(R.string.msg_bluetooth_gps_unavaible);
                } else {
                    if (Debug.D)
                        Logger.w(LOG_TAG, "current device: " + gpsDevice.getName() + " -- " + gpsDevice.getAddress());
                    try {
                        // gpsSocket =
                        // gpsDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                        // UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                        Method m = gpsDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                        gpsSocket = (BluetoothSocket) m.invoke(gpsDevice, 1);
                    } catch (Exception e) {
                        if (Debug.D)
                            Logger.e(LOG_TAG, "Error during connection", e);
                        gpsSocket = null;
                    }
                    if (gpsSocket == null) {
                        if (Debug.D)
                            Logger.w(LOG_TAG, "Error while establishing connection: no socket");
                        disable(R.string.msg_bluetooth_gps_unavaible);
                    } else {
                        Runnable connectThread = new Runnable(){
                            @Override
                            public void run() {
                                try {
                                    connected = false;
                                    if (Debug.D)
                                        Logger.i(LOG_TAG,
                                                "current device: " + gpsDevice.getName() + " -- " + gpsDevice.getAddress());
                                    if ((bluetoothAdapter.isEnabled()) && (nbRetriesRemaining > 0)) {
                                        try {
                                            if (connectedGps != null) {
                                                connectedGps.close();
                                            }
                                            if ((gpsSocket != null)
                                                    && ((connectedGps == null) || (connectedGps.socket != gpsSocket))) {
                                                if (Debug.D)
                                                    Logger.d(LOG_TAG, "trying to close old socket");
                                                gpsSocket.close();
                                            }
                                        } catch (IOException e) {
                                            if (Debug.D)
                                                Logger.e(LOG_TAG, "Error during disconnection", e);
                                        }
                                        try {
                                            // gpsSocket =
                                            // gpsDevice.createRfcommSocketToServiceRecord(UUID
                                            // .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                                            Method m = gpsDevice.getClass().getMethod("createRfcommSocket",
                                                    new Class[]{int.class});
                                            gpsSocket = (BluetoothSocket) m.invoke(gpsDevice, 1);
                                        } catch (Exception e) {
                                            if (Debug.D)
                                                Logger.e(LOG_TAG, "Error during connection", e);
                                            gpsSocket = null;
                                        }
                                        if (gpsSocket == null) {
                                            if (Debug.D)
                                                Logger.w(LOG_TAG, "Error while establishing connection: no socket");
                                            disable(R.string.msg_bluetooth_gps_unavaible);
                                        } else {
                                            // Cancel discovery because it will slow down the
                                            // connection
                                            bluetoothAdapter.cancelDiscovery();
                                            // we increment the number of connection tries
                                            // Connect the device through the socket. This will
                                            // block
                                            // until it succeeds or throws an exception
                                            if (Debug.D)
                                                Logger.i(LOG_TAG, "connecting to socket");
                                            gpsSocket.connect();
                                            if (Debug.D)
                                                Logger.d(LOG_TAG, "connected to socket");
                                            connected = true;
                                            // reset eventual disabling cause
                                            // setDisableReason(0);
                                            // connection obtained so reset the number of connection
                                            // try
                                            nbRetriesRemaining = 1 + maxConnectionRetries;
                                            notificationManager.cancel(R.string.connection_problem_notification_title);
                                            if (Debug.D)
                                                Logger.i(LOG_TAG, "starting socket reading task");
                                            connectedGps = new ConnectedGps(gpsSocket);
                                            connectionAndReadingPool.execute(connectedGps);
                                            if (Debug.D)
                                                Logger.i(LOG_TAG, "socket reading thread started");
                                        }
                                        // } else if (! bluetoothAdapter.isEnabled()) {
                                        // setDisableReason(R.string.msg_bluetooth_disabled);
                                    }
                                } catch (IOException connectException) {
                                    // Unable to connect
                                    if (Debug.D)
                                        Logger.e(LOG_TAG, "error while connecting to socket", connectException);
                                    disable(R.string.msg_bluetooth_gps_unavaible);
                                } finally {
                                    nbRetriesRemaining--;
                                    if (!connected) {
                                        disableIfNeeded();
                                    }
                                }
                            }
                        };
                        this.enabled = true;
                        if (Debug.D)
                            Logger.d(LOG_TAG, "Bluetooth GPS manager enabled");
                        if (Debug.D)
                            Logger.d(LOG_TAG, "starting notification thread");
                        if (Debug.D)
                            Logger.d(LOG_TAG, "starting connection and reading thread");
                        connectionAndReadingPool = Executors.newSingleThreadScheduledExecutor();
                        if (Debug.D)
                            Logger.d(LOG_TAG, "starting connection to socket task");
                        connectionAndReadingPool.scheduleWithFixedDelay(connectThread, 5000, 60000, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
        return this.enabled;
    }

    /**
     * Disables the bluetooth GPS Provider if the maximal number of connection retries is exceeded.
     * This is used when there are possibly non fatal connection problems. 
     * In these cases the provider will try to reconnect with the bluetooth device 
     * and only after a given retries number will give up and shutdown the service.
     */
    private synchronized void disableIfNeeded() {
        if (enabled) {
            if (nbRetriesRemaining > 0) {
                // Unable to connect
                if (Debug.D)
                    Logger.w(LOG_TAG, "Unable to establish connection");
                connectionProblemNotification.when = System.currentTimeMillis();
                String pbMessage = appContext.getResources().getQuantityString(R.plurals.connection_problem_notification,
                        nbRetriesRemaining, nbRetriesRemaining);
                connectionProblemNotification.setLatestEventInfo(appContext,
                        appContext.getString(R.string.connection_problem_notification_title), pbMessage,
                        connectionProblemNotification.contentIntent);
                connectionProblemNotification.number = 1 + maxConnectionRetries - nbRetriesRemaining;
                notificationManager.notify(R.string.connection_problem_notification_title, connectionProblemNotification);
            } else {
                // notificationManager.cancel(R.string.connection_problem_notification_title);
                // serviceStoppedNotification.when = System.currentTimeMillis();
                // notificationManager.notify(R.string.service_closed_because_connection_problem_notification_title,
                // serviceStoppedNotification);
                disable(R.string.msg_two_many_connection_problems);
            }
        }
    }

    /**
     * Disables the bluetooth GPS provider.
     * 
     * It will: 
     * <ul>
     * 	<li>close the connection with the bluetooth device</li>
     * 	<li>disable the Mock Location Provider used for the bluetooth GPS</li>
     * 	<li>stop the BlueGPS4Droid service</li>
     * </ul>
     * The reasonId parameter indicates the reason to close the bluetooth provider. 
     * If its value is zero, it's a normal shutdown (normally, initiated by the user).
     * If it's non-zero this value should correspond a valid localized string id (res/values..../...) 
     * which will be used to display a notification.
     * 
     * @param reasonId	the reason to close the bluetooth provider.
     */
    public synchronized void disable( int reasonId ) {
        if (Debug.D)
            Logger.d(LOG_TAG, "disabling Bluetooth GPS manager reason: " + reasonId);
        setDisableReason(reasonId);
        disable();
    }

    /**
     * Disables the bluetooth GPS provider.
     * 
     * It will: 
     * <ul>
     * 	<li>close the connection with the bluetooth device</li>
     * 	<li>disable the Mock Location Provider used for the bluetooth GPS</li>
     * 	<li>stop the BlueGPS4Droid service</li>
     * </ul>
     * If the bluetooth provider is closed because of a problem, a notification is displayed.
     */
    public synchronized void disable() {
        notificationManager.cancel(R.string.connection_problem_notification_title);
        if (getDisableReason() != 0) {
            serviceStoppedNotification.when = System.currentTimeMillis();
            serviceStoppedNotification.setLatestEventInfo(
                    appContext,
                    appContext.getString(R.string.service_closed_because_connection_problem_notification_title),
                    appContext.getString(R.string.service_closed_because_connection_problem_notification,
                            appContext.getString(getDisableReason())), serviceStoppedNotification.contentIntent);
            notificationManager.notify(R.string.service_closed_because_connection_problem_notification_title,
                    serviceStoppedNotification);
        }
        if (enabled) {
            if (Debug.D)
                Logger.d(LOG_TAG, "disabling Bluetooth GPS manager");
            enabled = false;
            connectionAndReadingPool.shutdown();
            Runnable closeAndShutdown = new Runnable(){
                @Override
                public void run() {
                    try {
                        connectionAndReadingPool.awaitTermination(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!connectionAndReadingPool.isTerminated()) {
                        connectionAndReadingPool.shutdownNow();
                        if (connectedGps != null) {
                            connectedGps.close();
                        }
                        if ((gpsSocket != null) && ((connectedGps == null) || (connectedGps.socket != gpsSocket))) {
                            try {
                                if (Debug.D)
                                    Logger.d(LOG_TAG, "closing Bluetooth GPS socket");
                                gpsSocket.close();
                            } catch (IOException closeException) {
                                if (Debug.D)
                                    Logger.e(LOG_TAG, "error while closing socket", closeException);
                            }
                        }
                    }
                }
            };
            new Thread(closeAndShutdown).start();
            bluetoothListeners.clear();
            if (Debug.D)
                Logger.d(LOG_TAG, "Bluetooth GPS manager disabled");
        }
    }

    /**
     * Adds an NMEA listener.
     * In fact, it delegates to the NMEA parser. 
     * 
     * @see NmeaParser#addNmeaListener(NmeaListener)
     * @param listener	a {@link NmeaListener} object to register
     * @return	true if the listener was successfully added
     */
    public boolean addListener( IBluetoothListener listener ) {
        if (!bluetoothListeners.contains(listener)) {
            if (Debug.D)
                Logger.d(LOG_TAG, "adding new listener");
            bluetoothListeners.add(listener);
        }
        return true;
    }

    /**
     * Removes an NMEA listener.
     * In fact, it delegates to the NMEA parser. 
     * 
     * @see NmeaParser#removeNmeaListener(NmeaListener)
     * @param listener	a {@link NmeaListener} object to remove 
     */
    public void removeListener( IBluetoothListener listener ) {
        if (Debug.D)
            Logger.d(LOG_TAG, "removing listener");
        bluetoothListeners.remove(listener);
    }

    /**
     * Sends a NMEA sentence to the bluetooth GPS.
     * 
     * @param command	the complete NMEA sentence (i.e. $....*XY where XY is the checksum).
     */
    public void sendStringCommand( final String command ) {
        if (Debug.D)
            Logger.d(LOG_TAG, "sending string sentence: " + command);
        if (isEnabled()) {
            while( (enabled) && ((!connected) || (connectedGps == null) || (!connectedGps.isReady())) ) {
                if (Debug.D)
                    Logger.i(LOG_TAG, "writing thread is not ready");
                SystemClock.sleep(500);
            }
            if (isEnabled() && (connectedGps != null)) {
                connectedGps.write(command);
                if (Debug.D)
                    Logger.d(LOG_TAG, "sent string sentence: " + command);
            }
        }
    }

    /**
     * Sends a SIRF III binary command to the bluetooth GPS.
     * 
     * @param commandHexa	an hexadecimal string representing a complete binary command 
     * (i.e. with the <em>Start Sequence</em>, <em>Payload Length</em>, <em>Payload</em>, <em>Message Checksum</em> and <em>End Sequence</em>).
     */
    public void sendBinaryCommand( final byte[] command ) {
        if (Debug.D)
            Logger.d(LOG_TAG, "sending binary sentence");
        if (isEnabled()) {
            while( (enabled) && ((!connected) || (connectedGps == null) || (!connectedGps.isReady())) ) {
                if (Debug.D)
                    Logger.i(LOG_TAG, "writing thread is not ready");
                SystemClock.sleep(500);
            }
            if (isEnabled() && (connectedGps != null)) {
                connectedGps.write(command);
            }
        }
    }

}
