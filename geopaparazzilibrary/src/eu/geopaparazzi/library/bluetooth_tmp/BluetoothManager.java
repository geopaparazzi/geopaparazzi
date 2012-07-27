///*
// * Copyright (C) 2010, 2011 Herbert von Broeuschmeul
// * Copyright (C) 2010, 2011 BluetoothGPS4Droid Project
// * 
// * This file is part of BluetoothGPS4Droid.
// *
// * BluetoothGPS4Droid is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// * 
// * BluetoothGPS4Droid is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// * 
// *  You should have received a copy of the GNU General Public License
// *  along with BluetoothGPS4Droid. If not, see <http://www.gnu.org/licenses/>.
// */
//
//package eu.geopaparazzi.library.bluetooth_tmp;
//
//import java.io.IOException;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothSocket;
//import android.content.Context;
//import android.content.Intent;
//import eu.geopaparazzi.library.R;
//import eu.geopaparazzi.library.bluetooth.IBluetoothEnablementHandler;
//import eu.geopaparazzi.library.bluetooth.IBluetoothIOHandler;
//import eu.geopaparazzi.library.util.debug.Debug;
//import eu.geopaparazzi.library.util.debug.Logger;
//
///**
// * This class is used to establish and manage the connection with the bluetooth device.
// * 
// * @author Herbert von Broeuschmeul
// * @author Andrea Antonello (www.hydrologis.com)
// *
// */
//@SuppressWarnings("nls")
//public class BluetoothManager implements IBluetoothEnablementHandler {
//
//    /**
//     * Tag used for log messages
//     */
//    private static final String LOG_TAG = "BluetoothManager";
//
//    private static BluetoothManager bluetoothManager;
//
//    private BluetoothSocket bluetoothSocket;
//    private String deviceAddress;
//    private boolean enabled = false;
//    private ScheduledExecutorService connectionAndReadingPool;
//
//    private IBluetoothIOHandler connectedBluetoothDevice;
//    private int disableReason = 0;
//    private Notification connectionProblemNotification;
//    private Notification serviceStoppedNotification;
//    private Context appContext;
//    private NotificationManager notificationManager;
//    private int maxConnectionRetries;
//    private int nbRetriesRemaining;
//    private boolean connected = false;
//
//    private BluetoothManager() {
//    }
//
//    public static BluetoothManager getInstance() {
//        if (bluetoothManager == null) {
//            bluetoothManager = new BluetoothManager();
//        }
//        return bluetoothManager;
//    }
//
//    /**
//     * Reset the status of the {@link BluetoothManager}.
//     */
//    public static void reset() {
//        bluetoothManager = null;
//    }
//
//    /**
//     * check if the first instance has been created already.
//     * 
//     * @return <code>true</code> if the first instance has been created already.
//     */
//    public static boolean hasInstance() {
//        return bluetoothManager != null;
//    }
//
//    private void checkBluetoothDevice() {
//        if (connectedBluetoothDevice == null) {
//            throw new IllegalArgumentException("No bluetooth device avaliable. Did you call setBluetoothDevice?");
//        }
//    }
//
//    /**
//     * Set the bluetooth device to use.
//     * 
//     * <b>Note that this has to be called before doing anything else.</b>
//     * 
//     * @param connectedBluetoothDevice the device to connect.
//     */
//    public void setBluetoothDevice( IBluetoothIOHandler connectedBluetoothDevice ) {
//        if (this.connectedBluetoothDevice != null) {
//            this.connectedBluetoothDevice.close();
//            this.connectedBluetoothDevice = null;
//            throw new IllegalArgumentException(
//                    "The bluetoothdevice can't be set without resetting the manager. Call reset first.");
//        }
//        this.connectedBluetoothDevice = connectedBluetoothDevice;
//    }
//
//    /**
//     * Set device parameters.
//     * 
//     * <b>Note that this has to be called after the {@link #setBluetoothDevice(NmeaGpsDevice)} and
//     *  before doing anything else.</b>
//     * 
//     * @param context the {@link Context} to use.
//     * @param deviceAddress the address of the device.
//     * @param maxRetries maximum number of tries to do for connection.
//     */
//    public void setParameters( Context context, String deviceAddress, int maxRetries ) {
//        checkBluetoothDevice();
//        this.deviceAddress = deviceAddress;
//        this.maxConnectionRetries = maxRetries;
//        this.nbRetriesRemaining = 1 + maxRetries;
//        this.appContext = context;
//
//        prepare();
//    }
//
//    /**
//     * Prepare notifications and intents for start and stop of the device.
//     */
//    private void prepare() {
//        notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
//        connectionProblemNotification = new Notification();
//        connectionProblemNotification.icon = R.drawable.ic_stat_notify;
//        Intent stopIntent = new Intent(IBluetoothListener.ACTION_STOP_GPS_PROVIDER);
//        PendingIntent stopPendingIntent = PendingIntent.getService(appContext, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//        connectionProblemNotification.contentIntent = stopPendingIntent;
//
//        serviceStoppedNotification = new Notification();
//        serviceStoppedNotification.icon = R.drawable.ic_stat_notify;
//        Intent restartIntent = new Intent(IBluetoothListener.ACTION_START_GPS_PROVIDER);
//        PendingIntent restartPendingIntent = PendingIntent.getService(appContext, 0, restartIntent,
//                PendingIntent.FLAG_CANCEL_CURRENT);
//        serviceStoppedNotification.setLatestEventInfo(appContext,
//                appContext.getString(R.string.service_closed_because_connection_problem_notification_title),
//                appContext.getString(R.string.service_closed_because_connection_problem_notification), restartPendingIntent);
//    }
//
//    /**
//     * Set the reason for stopping.
//     * 
//     * @param reasonId the message id of the reason.
//     */
//    private void setDisableReason( int reasonId ) {
//        disableReason = reasonId;
//    }
//
//    /**
//     * Get the reason for stopping.
//     * 
//     * @return the message id of the reason.
//     */
//    public int getDisableReason() {
//        return disableReason;
//    }
//
//    /**
//     * Checks if the device is enabled.
//     * 
//     * @return <code>true</code> if the bluetooth device is enabled.
//     */
//    public synchronized boolean isEnabled() {
//        return enabled;
//    }
//
//    /**
//     * Enables the bluetooth device.
//     * 
//     * @return <code>true</code> if everything went well.
//     */
//    public synchronized boolean enable() {
//        checkBluetoothDevice();
//        notificationManager.cancel(R.string.service_closed_because_connection_problem_notification_title);
//        if (!enabled) {
//            if (Debug.D)
//                Logger.d(LOG_TAG, "enabling Bluetooth device manager");
//
//            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//            if (bluetoothAdapter == null) {
//                if (Debug.D)
//                    Logger.w(LOG_TAG, "Device does not support Bluetooth");
//                disable(R.string.msg_bluetooth_unsupported);
//            } else if (!bluetoothAdapter.isEnabled()) {
//                // Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//                if (Debug.D)
//                    Logger.w(LOG_TAG, "Bluetooth is not enabled");
//                disable(R.string.msg_bluetooth_disabled);
//                // } else if (Settings.Secure.getInt(callingService.getContentResolver(),
//                // Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 0) {
//                // if(Debug.D) Logger.e(LOG_TAG, "Mock location provider OFF");
//                // disable(R.string.msg_mock_location_disabled);
//                // } else if ( (! locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
//                // &&
//                // (sharedPreferences.getBoolean(BluetoothProviderService.PREF_REPLACE_STD_GPS,
//                // true))
//                // ) {
//                // if(Debug.D) Logger.e(LOG_TAG, "GPS location provider OFF");
//                // disable(R.string.msg_gps_provider_disabled);
//            } else {
//                final BluetoothDevice gpsDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
//                if (gpsDevice == null) {
//                    if (Debug.D)
//                        Logger.w(LOG_TAG, "GPS device not found");
//                    disable(R.string.msg_bluetooth_gps_unavaible);
//                } else {
//                    if (Debug.D)
//                        Logger.w(LOG_TAG, "current device: " + gpsDevice.getName() + " -- " + gpsDevice.getAddress());
//                    try {
//                        // gpsSocket =
//                        // gpsDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
//                        // UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//                        bluetoothSocket = createSocket(gpsDevice);
//                    } catch (Exception e) {
//                        if (Debug.D)
//                            Logger.e(LOG_TAG, "Error during connection", e);
//                        bluetoothSocket = null;
//                    }
//                    if (bluetoothSocket == null) {
//                        if (Debug.D)
//                            Logger.w(LOG_TAG, "Error while establishing connection: no socket");
//                        disable(R.string.msg_bluetooth_gps_unavaible);
//                    } else {
//                        Runnable connectThread = new Runnable(){
//                            @Override
//                            public void run() {
//                                try {
//                                    connected = false;
//                                    if (Debug.D)
//                                        Logger.i(LOG_TAG,
//                                                "current device: " + gpsDevice.getName() + " -- " + gpsDevice.getAddress());
//                                    if ((bluetoothAdapter.isEnabled()) && (nbRetriesRemaining > 0)) {
//                                        try {
//                                            if (connectedBluetoothDevice != null) {
//                                                connectedBluetoothDevice.close();
//                                                connectedBluetoothDevice = null;
//                                                if (Debug.D)
//                                                    Logger.d(LOG_TAG, "trying to close leftover device");
//                                            }
//                                            if ((bluetoothSocket != null)
//                                                    && ((connectedBluetoothDevice == null) || (connectedBluetoothDevice
//                                                            .getSocket() != bluetoothSocket))) {
//                                                if (Debug.D)
//                                                    Logger.d(LOG_TAG, "trying to close old socket");
//                                                bluetoothSocket.close();
//                                            }
//                                        } catch (Exception e) {
//                                            if (Debug.D)
//                                                Logger.e(LOG_TAG, "Error during disconnection", e);
//                                        }
//                                        try {
//                                            // gpsSocket =
//                                            // gpsDevice.createRfcommSocketToServiceRecord(UUID
//                                            // .fromString("00001101-0000-1000-8000-00805F9B34FB"));
//                                            Method m = gpsDevice.getClass().getMethod("createRfcommSocket",
//                                                    new Class[]{int.class});
//                                            bluetoothSocket = (BluetoothSocket) m.invoke(gpsDevice, 1);
//                                        } catch (Exception e) {
//                                            if (Debug.D)
//                                                Logger.e(LOG_TAG, "Error during connection", e);
//                                            bluetoothSocket = null;
//                                        }
//                                        if (bluetoothSocket == null) {
//                                            if (Debug.D)
//                                                Logger.w(LOG_TAG, "Error while establishing connection: no socket");
//                                            disable(R.string.msg_bluetooth_gps_unavaible);
//                                        } else {
//                                            // Cancel discovery because it will slow down the
//                                            // connection
//                                            bluetoothAdapter.cancelDiscovery();
//                                            // we increment the number of connection tries
//                                            // Connect the device through the socket. This will
//                                            // block
//                                            // until it succeeds or throws an exception
//                                            if (Debug.D)
//                                                Logger.i(LOG_TAG, "connecting to socket");
//                                            bluetoothSocket.connect();
//                                            if (Debug.D)
//                                                Logger.d(LOG_TAG, "connected to socket");
//                                            connected = true;
//                                            // reset eventual disabling cause
//                                            // setDisableReason(0);
//                                            // connection obtained so reset the number of connection
//                                            // try
//                                            nbRetriesRemaining = 1 + maxConnectionRetries;
//                                            notificationManager.cancel(R.string.connection_problem_notification_title);
//                                            if (Debug.D)
//                                                Logger.i(LOG_TAG, "starting socket reading task");
//                                            connectedBluetoothDevice.initialize(bluetoothSocket);
//                                            connectedBluetoothDevice.setEnabled(true);
//                                            connectionAndReadingPool.execute(connectedBluetoothDevice);
//                                            if (Debug.D)
//                                                Logger.i(LOG_TAG, "socket reading thread started");
//                                        }
//                                        // } else if (! bluetoothAdapter.isEnabled()) {
//                                        // setDisableReason(R.string.msg_bluetooth_disabled);
//                                    }
//                                } catch (IOException connectException) {
//                                    // Unable to connect
//                                    if (Debug.D)
//                                        Logger.e(LOG_TAG, "error while connecting to socket", connectException);
//                                    disable(R.string.msg_bluetooth_gps_unavaible);
//                                } finally {
//                                    nbRetriesRemaining--;
//                                    if (!connected) {
//                                        disableIfNeeded();
//                                    }
//                                }
//                            }
//                        };
//
//                        this.enabled = true;
//                        if (Debug.D)
//                            Logger.d(LOG_TAG, "Bluetooth GPS manager enabled");
//                        if (Debug.D)
//                            Logger.d(LOG_TAG, "starting notification thread");
//                        if (Debug.D)
//                            Logger.d(LOG_TAG, "starting connection and reading thread");
//                        connectionAndReadingPool = Executors.newSingleThreadScheduledExecutor();
//                        if (Debug.D)
//                            Logger.d(LOG_TAG, "starting connection to socket task");
//                        connectionAndReadingPool.scheduleWithFixedDelay(connectThread, 5000, 60000, TimeUnit.MILLISECONDS);
//                    }
//                }
//            }
//        }
//        return this.enabled;
//    }
//
//    /**
//     * Create the rfcomm socket.
//     * 
//     * @param bluetoothDevice
//     * @return the created socket or <code>null</code>.
//     * @throws NoSuchMethodException
//     * @throws IllegalAccessException
//     * @throws InvocationTargetException
//     */
//    private BluetoothSocket createSocket( final BluetoothDevice bluetoothDevice ) throws NoSuchMethodException,
//            IllegalAccessException, InvocationTargetException {
//        Method m = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
//        return (BluetoothSocket) m.invoke(bluetoothDevice, 1);
//    }
//
//    /**
//     * Disables the bluetooth GPS Provider if the maximal number of connection retries is exceeded.
//     * This is used when there are possibly non fatal connection problems. 
//     * In these cases the provider will try to reconnect with the bluetooth device 
//     * and only after a given retries number will give up and shutdown the service.
//     */
//    private synchronized void disableIfNeeded() {
//        if (enabled) {
//            if (nbRetriesRemaining > 0) {
//                // Unable to connect
//                if (Debug.D)
//                    Logger.w(LOG_TAG, "Unable to establish connection");
//                connectionProblemNotification.when = System.currentTimeMillis();
//                String pbMessage = appContext.getResources().getQuantityString(R.plurals.connection_problem_notification,
//                        nbRetriesRemaining, nbRetriesRemaining);
//                connectionProblemNotification.setLatestEventInfo(appContext,
//                        appContext.getString(R.string.connection_problem_notification_title), pbMessage,
//                        connectionProblemNotification.contentIntent);
//                connectionProblemNotification.number = 1 + maxConnectionRetries - nbRetriesRemaining;
//                notificationManager.notify(R.string.connection_problem_notification_title, connectionProblemNotification);
//            } else {
//                // notificationManager.cancel(R.string.connection_problem_notification_title);
//                // serviceStoppedNotification.when = System.currentTimeMillis();
//                // notificationManager.notify(R.string.service_closed_because_connection_problem_notification_title,
//                // serviceStoppedNotification);
//                disable(R.string.msg_two_many_connection_problems);
//            }
//        }
//    }
//
//    /**
//     * Disables the bluetooth GPS provider.
//     * 
//     * It will: 
//     * <ul>
//     * 	<li>close the connection with the bluetooth device</li>
//     * 	<li>disable the Mock Location Provider used for the bluetooth GPS</li>
//     * 	<li>stop the BlueGPS4Droid service</li>
//     * </ul>
//     * The reasonId parameter indicates the reason to close the bluetooth provider. 
//     * If its value is zero, it's a normal shutdown (normally, initiated by the user).
//     * If it's non-zero this value should correspond a valid localized string id (res/values..../...) 
//     * which will be used to display a notification.
//     * 
//     * @param reasonId	the reason to close the bluetooth provider.
//     */
//    public synchronized void disable( int reasonId ) {
//        if (Debug.D)
//            Logger.d(LOG_TAG, "disabling Bluetooth GPS manager reason: " + reasonId);
//        setDisableReason(reasonId);
//        disable();
//    }
//
//    /**
//     * Returns the {@link NmeaGpsDevice}. 
//     * 
//     * <p>Can be used to attach listeners to the device.
//     * 
//     * @return the device or <code>null</code>.
//     */
//    public IBluetoothIOHandler getConnectedBluetoothDevice() {
//        checkBluetoothDevice();
//        return connectedBluetoothDevice;
//    }
//
//    /**
//     * Disables the bluetooth GPS provider.
//     * 
//     * It will: 
//     * <ul>
//     * 	<li>close the connection with the bluetooth device</li>
//     * 	<li>disable the Mock Location Provider used for the bluetooth GPS</li>
//     * 	<li>stop the BlueGPS4Droid service</li>
//     * </ul>
//     * If the bluetooth provider is closed because of a problem, a notification is displayed.
//     */
//    public synchronized void disable() {
//        if (!enabled) {
//            return;
//        }
//        notificationManager.cancel(R.string.connection_problem_notification_title);
//        if (getDisableReason() != 0) {
//            serviceStoppedNotification.when = System.currentTimeMillis();
//            serviceStoppedNotification.setLatestEventInfo(
//                    appContext,
//                    appContext.getString(R.string.service_closed_because_connection_problem_notification_title),
//                    appContext.getString(R.string.service_closed_because_connection_problem_notification,
//                            appContext.getString(getDisableReason())), serviceStoppedNotification.contentIntent);
//            notificationManager.notify(R.string.service_closed_because_connection_problem_notification_title,
//                    serviceStoppedNotification);
//        }
//        if (enabled) {
//            if (Debug.D)
//                Logger.d(LOG_TAG, "disabling Bluetooth GPS manager");
//            connectedBluetoothDevice.setEnabled(false);
//            enabled = false;
//            connectionAndReadingPool.shutdown();
//            Runnable closeAndShutdown = new Runnable(){
//                @Override
//                public void run() {
//                    try {
//                        connectionAndReadingPool.awaitTermination(10, TimeUnit.SECONDS);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    if (!connectionAndReadingPool.isTerminated()) {
//                        connectionAndReadingPool.shutdownNow();
//                        if (connectedBluetoothDevice != null) {
//                            connectedBluetoothDevice.close();
//                            connectedBluetoothDevice = null;
//                        }
//                        if ((bluetoothSocket != null)
//                                && ((connectedBluetoothDevice == null) || (connectedBluetoothDevice.getSocket() != bluetoothSocket))) {
//                            try {
//                                if (Debug.D)
//                                    Logger.d(LOG_TAG, "closing Bluetooth GPS socket");
//                                bluetoothSocket.close();
//                            } catch (IOException closeException) {
//                                if (Debug.D)
//                                    Logger.e(LOG_TAG, "error while closing socket", closeException);
//                            }
//                        }
//                    }
//                }
//            };
//            new Thread(closeAndShutdown).start();
//            if (Debug.D)
//                Logger.d(LOG_TAG, "Bluetooth GPS manager disabled");
//        }
//    }
//
//    // /**
//    // * Sends a NMEA sentence to the bluetooth GPS.
//    // *
//    // * @param command the complete NMEA sentence (i.e. $....*XY where XY is the checksum).
//    // */
//    // public void sendStringCommand( final String command ) {
//    // checkBluetoothDevice();
//    // if (Debug.D)
//    // Logger.d(LOG_TAG, "sending string sentence: " + command);
//    // if (isEnabled()) {
//    // while( (enabled) && ((!connected) || (connectedBluetoothDevice == null) ||
//    // (!connectedBluetoothDevice.isReady())) ) {
//    // if (Debug.D)
//    // Logger.i(LOG_TAG, "writing thread is not ready");
//    // SystemClock.sleep(500);
//    // }
//    // if (isEnabled() && (connectedBluetoothDevice != null)) {
//    // connectedBluetoothDevice.write(command);
//    // if (Debug.D)
//    // Logger.d(LOG_TAG, "sent string sentence: " + command);
//    // }
//    // }
//    // }
//    //
//    // /**
//    // * Sends a SIRF III binary command to the bluetooth GPS.
//    // *
//    // * @param commandHexa an hexadecimal string representing a complete binary command
//    // * (i.e. with the <em>Start Sequence</em>, <em>Payload Length</em>, <em>Payload</em>,
//    // <em>Message Checksum</em> and <em>End Sequence</em>).
//    // */
//    // public void sendBinaryCommand( final byte[] command ) {
//    // checkBluetoothDevice();
//    // if (Debug.D)
//    // Logger.d(LOG_TAG, "sending binary sentence");
//    // if (isEnabled()) {
//    // while( (enabled) && ((!connected) || (connectedBluetoothDevice == null) ||
//    // (!connectedBluetoothDevice.isReady())) ) {
//    // if (Debug.D)
//    // Logger.i(LOG_TAG, "writing thread is not ready");
//    // SystemClock.sleep(500);
//    // }
//    // if (isEnabled() && (connectedBluetoothDevice != null)) {
//    // connectedBluetoothDevice.write(command);
//    // }
//    // }
//    // }
//
//}
