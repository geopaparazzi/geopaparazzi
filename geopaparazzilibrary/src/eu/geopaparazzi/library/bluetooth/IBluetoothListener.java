package eu.geopaparazzi.library.bluetooth;

/**
 * A bluetooth listener interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
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
