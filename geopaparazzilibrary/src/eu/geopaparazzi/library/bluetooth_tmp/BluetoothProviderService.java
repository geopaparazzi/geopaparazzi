package eu.geopaparazzi.library.bluetooth_tmp;
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
///**
// * 
// */
//package eu.geopaparazzi.library.bluetooth_tmp;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//import android.app.Notification;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.bluetooth.BluetoothAdapter;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.preference.PreferenceManager;
//import android.util.Config;
//import android.util.Log;
//import android.widget.Toast;
//import eu.geopaparazzi.library.R;
//
///**
// * A Service used to replace Android internal GPS with a bluetooth GPS and/or write GPS NMEA data in a File.
// * 
// * @author Herbert von Broeuschmeul
// *
// */
//public class BluetoothProviderService extends Service implements IBluetoothListener {
//
//    public static final String ACTION_START_TRACK_RECORDING = "org.broeuschmeul.android.gps.bluetooth.tracker.nmea.intent.action.START_TRACK_RECORDING";
//    public static final String ACTION_STOP_TRACK_RECORDING = "org.broeuschmeul.android.gps.bluetooth.tracker.nmea.intent.action.STOP_TRACK_RECORDING";
//    public static final String ACTION_START_GPS_PROVIDER = "org.broeuschmeul.android.gps.bluetooth.provider.nmea.intent.action.START_GPS_PROVIDER";
//    public static final String ACTION_STOP_GPS_PROVIDER = "org.broeuschmeul.android.gps.bluetooth.provider.nmea.intent.action.STOP_GPS_PROVIDER";
//    public static final String ACTION_CONFIGURE_SIRF_GPS = "org.broeuschmeul.android.gps.bluetooth.provider.nmea.intent.action.CONFIGURE_SIRF_GPS";
//    public static final String PREF_START_GPS_PROVIDER = "startGps";
//    public static final String PREF_GPS_LOCATION_PROVIDER = "gpsLocationProviderKey";
//    public static final String PREF_REPLACE_STD_GPS = "replaceStdtGps";
//    public static final String PREF_FORCE_ENABLE_PROVIDER = "forceEnableProvider";
//    public static final String PREF_MOCK_GPS_NAME = "mockGpsName";
//    public static final String PREF_CONNECTION_RETRIES = "connectionRetries";
//    public static final String PREF_TRACK_RECORDING = "trackRecording";
//    public static final String PREF_TRACK_FILE_DIR = "trackFileDirectory";
//    public static final String PREF_TRACK_FILE_PREFIX = "trackFilePrefix";
//    public static final String PREF_BLUETOOTH_DEVICE = "bluetoothDevice";
//    public static final String PREF_ABOUT = "about";
//
//    /**
//     * Tag used for log messages
//     */
//    private static final String LOG_TAG = "BlueGPS";
//
//    public static final String PREF_SIRF_GPS = "sirfGps";
//    public static final String PREF_SIRF_ENABLE_GGA = "enableGGA";
//    public static final String PREF_SIRF_ENABLE_RMC = "enableRMC";
//    public static final String PREF_SIRF_ENABLE_GLL = "enableGLL";
//    public static final String PREF_SIRF_ENABLE_VTG = "enableVTG";
//    public static final String PREF_SIRF_ENABLE_GSA = "enableGSA";
//    public static final String PREF_SIRF_ENABLE_GSV = "enableGSV";
//    public static final String PREF_SIRF_ENABLE_ZDA = "enableZDA";
//    public static final String PREF_SIRF_ENABLE_SBAS = "enableSBAS";
//    public static final String PREF_SIRF_ENABLE_NMEA = "enableNMEA";
//    public static final String PREF_SIRF_ENABLE_STATIC_NAVIGATION = "enableStaticNavigation";
//
//    private BluetoothManager gpsManager = null;
//    private PrintWriter writer;
//    private File trackFile;
//    private boolean preludeWritten = false;
//    private Toast toast;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        toast = Toast.makeText(getApplicationContext(), "NMEA track recording... on", Toast.LENGTH_SHORT);
//    }
//
//    @Override
//    public void onStart( Intent intent, int startId ) {
//        // super.onStart(intent, startId);
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor edit = sharedPreferences.edit();
//        String deviceAddress = sharedPreferences.getString(PREF_BLUETOOTH_DEVICE, null);
//        int maxConRetries = Integer.parseInt(sharedPreferences.getString(PREF_CONNECTION_RETRIES,
//                this.getString(R.string.defaultConnectionRetries)));
//        if (Config.LOGD) {
//            Log.d(LOG_TAG, "prefs device addr: " + deviceAddress);
//        }
//        if (ACTION_START_GPS_PROVIDER.equals(intent.getAction())) {
//            if (gpsManager == null) {
//                if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
//                    // String mockProvider = LocationManager.GPS_PROVIDER;
//                    // if (!sharedPreferences.getBoolean(PREF_REPLACE_STD_GPS, true)) {
//                    // mockProvider = sharedPreferences.getString(PREF_MOCK_GPS_NAME,
//                    // getString(R.string.defaultMockGpsName));
//                    // }
//                    // gpsManager = new BluetoothManager(this, deviceAddress, maxConRetries);
//                    gpsManager = BluetoothManager.BLUETOOTHMANAGER;
//                    gpsManager.connect(this, deviceAddress, maxConRetries);
//                    boolean enabled = gpsManager.enable();
//                    // Bundle extras = intent.getExtras();
//                    if (sharedPreferences.getBoolean(PREF_START_GPS_PROVIDER, false) != enabled) {
//                        edit.putBoolean(PREF_START_GPS_PROVIDER, enabled);
//                        edit.commit();
//                    }
//                    if (enabled) {
//                        // gpsManager.enableMockLocationProvider(mockProvider);
//                        Notification notification = new Notification(R.drawable.ic_stat_notify,
//                                this.getString(R.string.foreground_gps_provider_started_notification), System.currentTimeMillis());
//                        Intent myIntent = new Intent(this, BluetoothPreferencesActivity.class);
//                        PendingIntent myPendingIntent = PendingIntent.getActivity(this, 0, myIntent,
//                                PendingIntent.FLAG_CANCEL_CURRENT);
//                        notification.setLatestEventInfo(getApplicationContext(),
//                                this.getString(R.string.foreground_service_started_notification_title),
//                                this.getString(R.string.foreground_gps_provider_started_notification), myPendingIntent);
//                        startForeground(R.string.foreground_gps_provider_started_notification, notification);
//                        if (sharedPreferences.getBoolean(PREF_SIRF_GPS, false)) {
//                            enableSirfConfig(sharedPreferences);
//                        }
//                        toast.setText(this.getString(R.string.msg_gps_provider_started));
//                        toast.show();
//                    } else {
//                        stopSelf();
//                    }
//                } else {
//                    stopSelf();
//                }
//            } else {
//                toast.setText(this.getString(R.string.msg_gps_provider_already_started));
//                toast.show();
//            }
//        } else if (ACTION_START_TRACK_RECORDING.equals(intent.getAction())) {
//            if (trackFile == null) {
//                if (gpsManager != null) {
//                    beginTrack();
//                    gpsManager.addListener(this);
//                    if (!sharedPreferences.getBoolean(PREF_TRACK_RECORDING, false)) {
//                        edit.putBoolean(PREF_TRACK_RECORDING, true);
//                        edit.commit();
//                    }
//                    toast.setText(this.getString(R.string.msg_nmea_recording_started));
//                    toast.show();
//                } else {
//                    endTrack();
//                    if (sharedPreferences.getBoolean(PREF_TRACK_RECORDING, true)) {
//                        edit.putBoolean(PREF_TRACK_RECORDING, false);
//                        edit.commit();
//                    }
//                }
//            } else {
//                toast.setText(this.getString(R.string.msg_nmea_recording_already_started));
//                toast.show();
//            }
//        } else if (ACTION_STOP_TRACK_RECORDING.equals(intent.getAction())) {
//            if (gpsManager != null) {
//                gpsManager.removeListener(this);
//                endTrack();
//                toast.setText(this.getString(R.string.msg_nmea_recording_stopped));
//                toast.show();
//            }
//            if (sharedPreferences.getBoolean(PREF_TRACK_RECORDING, true)) {
//                edit.putBoolean(PREF_TRACK_RECORDING, false);
//                edit.commit();
//            }
//        } else if (ACTION_STOP_GPS_PROVIDER.equals(intent.getAction())) {
//            if (sharedPreferences.getBoolean(PREF_START_GPS_PROVIDER, true)) {
//                edit.putBoolean(PREF_START_GPS_PROVIDER, false);
//                edit.commit();
//            }
//            stopSelf();
//        } else if (ACTION_CONFIGURE_SIRF_GPS.equals(intent.getAction())) {
//            if (gpsManager != null) {
//                Bundle extras = intent.getExtras();
//                enableSirfConfig(extras);
//            }
//        }
//    }
//
//    private void enableSirfConfig( Bundle extras ) {
//        if (extras.containsKey(PREF_SIRF_ENABLE_GGA)) {
//            enableNmeaGGA(extras.getBoolean(PREF_SIRF_ENABLE_GGA, true));
//        }
//        if (extras.containsKey(PREF_SIRF_ENABLE_RMC)) {
//            enableNmeaRMC(extras.getBoolean(PREF_SIRF_ENABLE_RMC, true));
//        }
//        if (extras.containsKey(PREF_SIRF_ENABLE_GLL)) {
//            enableNmeaGLL(extras.getBoolean(PREF_SIRF_ENABLE_GLL, false));
//        }
//        if (extras.containsKey(PREF_SIRF_ENABLE_VTG)) {
//            enableNmeaVTG(extras.getBoolean(PREF_SIRF_ENABLE_VTG, false));
//        }
//        if (extras.containsKey(PREF_SIRF_ENABLE_GSA)) {
//            enableNmeaGSA(extras.getBoolean(PREF_SIRF_ENABLE_GSA, false));
//        }
//        if (extras.containsKey(PREF_SIRF_ENABLE_GSV)) {
//            enableNmeaGSV(extras.getBoolean(PREF_SIRF_ENABLE_GSV, false));
//        }
//        if (extras.containsKey(PREF_SIRF_ENABLE_ZDA)) {
//            enableNmeaZDA(extras.getBoolean(PREF_SIRF_ENABLE_ZDA, false));
//        }
//        if (extras.containsKey(PREF_SIRF_ENABLE_STATIC_NAVIGATION)) {
//            enableStaticNavigation(extras.getBoolean(PREF_SIRF_ENABLE_STATIC_NAVIGATION, false));
//        } else if (extras.containsKey(PREF_SIRF_ENABLE_NMEA)) {
//            enableNMEA(extras.getBoolean(PREF_SIRF_ENABLE_NMEA, true));
//        }
//        if (extras.containsKey(PREF_SIRF_ENABLE_SBAS)) {
//            enableSBAS(extras.getBoolean(PREF_SIRF_ENABLE_SBAS, true));
//        }
//    }
//
//    private void enableSirfConfig( SharedPreferences extras ) {
//        if (extras.contains(PREF_SIRF_ENABLE_GLL)) {
//            enableNmeaGLL(extras.getBoolean(PREF_SIRF_ENABLE_GLL, false));
//        }
//        if (extras.contains(PREF_SIRF_ENABLE_VTG)) {
//            enableNmeaVTG(extras.getBoolean(PREF_SIRF_ENABLE_VTG, false));
//        }
//        if (extras.contains(PREF_SIRF_ENABLE_GSA)) {
//            enableNmeaGSA(extras.getBoolean(PREF_SIRF_ENABLE_GSA, false));
//        }
//        if (extras.contains(PREF_SIRF_ENABLE_GSV)) {
//            enableNmeaGSV(extras.getBoolean(PREF_SIRF_ENABLE_GSV, false));
//        }
//        if (extras.contains(PREF_SIRF_ENABLE_ZDA)) {
//            enableNmeaZDA(extras.getBoolean(PREF_SIRF_ENABLE_ZDA, false));
//        }
//        if (extras.contains(PREF_SIRF_ENABLE_STATIC_NAVIGATION)) {
//            enableStaticNavigation(extras.getBoolean(PREF_SIRF_ENABLE_STATIC_NAVIGATION, false));
//        } else if (extras.contains(PREF_SIRF_ENABLE_NMEA)) {
//            enableNMEA(extras.getBoolean(PREF_SIRF_ENABLE_NMEA, true));
//        }
//        if (extras.contains(PREF_SIRF_ENABLE_SBAS)) {
//            enableSBAS(extras.getBoolean(PREF_SIRF_ENABLE_SBAS, true));
//        }
//        gpsManager.sendStringCommand(this.getString(R.string.sirf_nmea_gga_on));
//        gpsManager.sendStringCommand(this.getString(R.string.sirf_nmea_rmc_on));
//        if (extras.contains(PREF_SIRF_ENABLE_GGA)) {
//            enableNmeaGGA(extras.getBoolean(PREF_SIRF_ENABLE_GGA, true));
//        }
//        if (extras.contains(PREF_SIRF_ENABLE_RMC)) {
//            enableNmeaRMC(extras.getBoolean(PREF_SIRF_ENABLE_RMC, true));
//        }
//    }
//
//    private void enableNmeaGGA( boolean enable ) {
//        if (gpsManager != null) {
//            if (enable) {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_gga_on));
//            } else {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_gga_off));
//            }
//        }
//    }
//
//    private void enableNmeaRMC( boolean enable ) {
//        if (gpsManager != null) {
//            if (enable) {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_rmc_on));
//            } else {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_rmc_off));
//            }
//        }
//    }
//
//    private void enableNmeaGLL( boolean enable ) {
//        if (gpsManager != null) {
//            if (enable) {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_gll_on));
//            } else {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_gll_off));
//            }
//        }
//    }
//
//    private void enableNmeaVTG( boolean enable ) {
//        if (gpsManager != null) {
//            if (enable) {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_vtg_on));
//            } else {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_vtg_off));
//            }
//        }
//    }
//
//    private void enableNmeaGSA( boolean enable ) {
//        if (gpsManager != null) {
//            if (enable) {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_gsa_on));
//            } else {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_gsa_off));
//            }
//        }
//    }
//
//    private void enableNmeaGSV( boolean enable ) {
//        if (gpsManager != null) {
//            if (enable) {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_gsv_on));
//            } else {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_gsv_off));
//            }
//        }
//    }
//
//    private void enableNmeaZDA( boolean enable ) {
//        if (gpsManager != null) {
//            if (enable) {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_zda_on));
//            } else {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_zda_off));
//            }
//        }
//    }
//
//    private void enableSBAS( boolean enable ) {
//        if (gpsManager != null) {
//            if (enable) {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_sbas_on));
//            } else {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_sbas_off));
//            }
//        }
//    }
//
//    private void enableNMEA( boolean enable ) {
//        if (gpsManager != null) {
//            if (enable) {
//                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//                int gll = (sharedPreferences.getBoolean(PREF_SIRF_ENABLE_GLL, false)) ? 1 : 0;
//                int vtg = (sharedPreferences.getBoolean(PREF_SIRF_ENABLE_VTG, false)) ? 1 : 0;
//                int gsa = (sharedPreferences.getBoolean(PREF_SIRF_ENABLE_GSA, false)) ? 5 : 0;
//                int gsv = (sharedPreferences.getBoolean(PREF_SIRF_ENABLE_GSV, false)) ? 5 : 0;
//                int zda = (sharedPreferences.getBoolean(PREF_SIRF_ENABLE_ZDA, false)) ? 1 : 0;
//                int mss = 0;
//                int epe = 0;
//                int gga = 1;
//                int rmc = 1;
//                String command = getString(R.string.sirf_bin_to_nmea_38400_alt, gga, gll, gsa, gsv, rmc, vtg, mss, epe, zda);
//                // FIXME gpsManager.sendBinaryCommand(command);
//            } else {
//                gpsManager.sendStringCommand(getString(R.string.sirf_nmea_to_binary));
//            }
//        }
//    }
//
//    private void enableStaticNavigation( boolean enable ) {
//        if (gpsManager != null) {
//            // FIXME
//            // SharedPreferences sharedPreferences =
//            // PreferenceManager.getDefaultSharedPreferences(this);
//            // boolean isInNmeaMode = sharedPreferences.getBoolean(PREF_SIRF_ENABLE_NMEA, true);
//            // if (isInNmeaMode) {
//            // enableNMEA(false);
//            // }
//            // if (enable) {
//            // gpsManager.sendSirfCommand(getString(R.string.sirf_bin_static_nav_on));
//            // } else {
//            // gpsManager.sendSirfCommand(getString(R.string.sirf_bin_static_nav_off));
//            // }
//            // if (isInNmeaMode) {
//            // enableNMEA(true);
//            // }
//        }
//    }
//
//    /* (non-Javadoc)
//     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
//     */
//    @Override
//    public int onStartCommand( Intent intent, int flags, int startId ) {
//        onStart(intent, startId);
//        return Service.START_NOT_STICKY;
//    }
//
//    @Override
//    public void onDestroy() {
//        BluetoothManager manager = gpsManager;
//        gpsManager = null;
//        if (manager != null) {
//            if (manager.getDisableReason() != 0) {
//                toast.setText(getString(R.string.msg_gps_provider_stopped_by_problem, getString(manager.getDisableReason())));
//                toast.show();
//            } else {
//                toast.setText(R.string.msg_gps_provider_stopped);
//                toast.show();
//            }
//            manager.removeListener(this);
//            manager.disable();
//        }
//        endTrack();
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor edit = sharedPreferences.edit();
//        if (sharedPreferences.getBoolean(PREF_TRACK_RECORDING, true)) {
//            edit.putBoolean(PREF_TRACK_RECORDING, false);
//            edit.commit();
//        }
//        if (sharedPreferences.getBoolean(PREF_START_GPS_PROVIDER, true)) {
//            edit.putBoolean(PREF_START_GPS_PROVIDER, false);
//            edit.commit();
//        }
//        super.onDestroy();
//    }
//
//    private void beginTrack() {
//        SimpleDateFormat fmt = new SimpleDateFormat("_yyyy-MM-dd_HH-mm-ss'.nmea'");
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        String trackDirName = sharedPreferences
//                .getString(PREF_TRACK_FILE_DIR, this.getString(R.string.defaultTrackFileDirectory));
//        String trackFilePrefix = sharedPreferences.getString(PREF_TRACK_FILE_PREFIX,
//                this.getString(R.string.defaultTrackFilePrefix));
//        trackFile = new File(trackDirName, trackFilePrefix + fmt.format(new Date()));
//        Log.d(LOG_TAG, "Writing the prelude of the NMEA file: " + trackFile.getAbsolutePath());
//        File trackDir = trackFile.getParentFile();
//        try {
//            if ((!trackDir.mkdirs()) && (!trackDir.isDirectory())) {
//                Log.e(LOG_TAG, "Error while creating parent dir of NMEA file: " + trackDir.getAbsolutePath());
//            }
//            writer = new PrintWriter(new BufferedWriter(new FileWriter(trackFile)));
//            preludeWritten = true;
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "Error while writing the prelude of the NMEA file: " + trackFile.getAbsolutePath(), e);
//            // there was an error while writing the prelude of the NMEA file, stopping the
//            // service...
//            stopSelf();
//        }
//    }
//    private void endTrack() {
//        if (trackFile != null && writer != null) {
//            Log.d(LOG_TAG, "Ending the NMEA file: " + trackFile.getAbsolutePath());
//            preludeWritten = false;
//            writer.close();
//            trackFile = null;
//        }
//    }
//    private void addNMEAString( String data ) {
//        if (!preludeWritten) {
//            beginTrack();
//        }
//        Log.v(LOG_TAG, "Adding data in the NMEA file: " + data);
//        if (trackFile != null && writer != null) {
//            writer.print(data);
//        }
//    }
//    /* (non-Javadoc)
//     * @see android.app.Service#onBind(android.content.Intent)
//     */
//    @Override
//    public IBinder onBind( Intent intent ) {
//        if (Config.LOGD) {
//            Log.d(LOG_TAG, "trying access IBinder");
//        }
//        return null;
//    }
//
//    @Override
//    public void onStringDataReceived( long timestamp, String data ) {
//        addNMEAString(data);
//    }
//}
