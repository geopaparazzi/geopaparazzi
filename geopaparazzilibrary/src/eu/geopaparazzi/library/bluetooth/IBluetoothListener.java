/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
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

/**
 * A bluetooth listener interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
@SuppressWarnings("nls")
public interface IBluetoothListener {
    public static final String PREF_START_GPS_PROVIDER = "startGps";
    public static final String PREF_GPS_LOCATION_PROVIDER = "gpsLocationProviderKey";
    public static final String PREF_REPLACE_STD_GPS = "replaceStdtGps";
    public static final String PREF_FORCE_ENABLE_PROVIDER = "forceEnableProvider";
    public static final String PREF_MOCK_GPS_NAME = "mockGpsName";
    public static final String PREF_CONNECTION_RETRIES = "connectionRetries";
    public static final String PREF_TRACK_RECORDING = "trackRecording";
    public static final String PREF_TRACK_FILE_DIR = "trackFileDirectory";
    public static final String PREF_TRACK_FILE_PREFIX = "trackFilePrefix";
    public static final String PREF_BLUETOOTH_DEVICE = "bluetoothDevice";
    public static final String PREF_ABOUT = "about";

    public static final String ACTION_START_TRACK_RECORDING = "org.broeuschmeul.android.gps.bluetooth.tracker.nmea.intent.action.START_TRACK_RECORDING";
    public static final String ACTION_STOP_TRACK_RECORDING = "org.broeuschmeul.android.gps.bluetooth.tracker.nmea.intent.action.STOP_TRACK_RECORDING";
    public static final String ACTION_START_GPS_PROVIDER = "org.broeuschmeul.android.gps.bluetooth.provider.nmea.intent.action.START_GPS_PROVIDER";
    public static final String ACTION_STOP_GPS_PROVIDER = "org.broeuschmeul.android.gps.bluetooth.provider.nmea.intent.action.STOP_GPS_PROVIDER";
    public static final String ACTION_CONFIGURE_SIRF_GPS = "org.broeuschmeul.android.gps.bluetooth.provider.nmea.intent.action.CONFIGURE_SIRF_GPS";

    public static final String PREF_SIRF_GPS = "sirfGps";
    public static final String PREF_SIRF_ENABLE_GGA = "enableGGA";
    public static final String PREF_SIRF_ENABLE_RMC = "enableRMC";
    public static final String PREF_SIRF_ENABLE_GLL = "enableGLL";
    public static final String PREF_SIRF_ENABLE_VTG = "enableVTG";
    public static final String PREF_SIRF_ENABLE_GSA = "enableGSA";
    public static final String PREF_SIRF_ENABLE_GSV = "enableGSV";
    public static final String PREF_SIRF_ENABLE_ZDA = "enableZDA";
    public static final String PREF_SIRF_ENABLE_SBAS = "enableSBAS";
    public static final String PREF_SIRF_ENABLE_NMEA = "enableNMEA";
    public static final String PREF_SIRF_ENABLE_STATIC_NAVIGATION = "enableStaticNavigation";

    /**
     * Method triggered when string data are recieved by the bluetooth.
     * 
     * @param time the timestamp.
     * @param data the data retrieved.
     */
    public void onStringDataReceived( long time, String data );
}
