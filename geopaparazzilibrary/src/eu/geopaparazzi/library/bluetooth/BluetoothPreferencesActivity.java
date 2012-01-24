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

import java.util.HashSet;
import java.util.Set;

import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Utilities;

/**
 * A PreferenceActivity Class used to configure, start and stop the NMEA tracker service.
 * 
 * @author Herbert von Broeuschmeul
 *
 */
public class BluetoothPreferencesActivity extends PreferenceActivity
        implements
            OnPreferenceChangeListener,
            OnSharedPreferenceChangeListener {

    /**
     * Tag used for log messages
     */
    private static final String LOG_TAG = "BlueGPS";

    private SharedPreferences sharedPref;
    private BluetoothAdapter bluetoothAdapter = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        this.updateDevicePreferenceList();
        super.onResume();
    }

    private void updateDevicePreferenceSummary() {
        // update bluetooth device summary
        String deviceName = "";
        ListPreference prefDevices = (ListPreference) findPreference(IBluetoothListener.PREF_BLUETOOTH_DEVICE);
        String deviceAddress = sharedPref.getString(IBluetoothListener.PREF_BLUETOOTH_DEVICE, null);
        if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            deviceName = bluetoothAdapter.getRemoteDevice(deviceAddress).getName();
        }
        prefDevices.setSummary(getString(R.string.pref_bluetooth_device_summary, deviceName));
    }

    private void updateDevicePreferenceList() {
        // update bluetooth device summary
        updateDevicePreferenceSummary();
        // update bluetooth device list
        ListPreference prefDevices = (ListPreference) findPreference(IBluetoothListener.PREF_BLUETOOTH_DEVICE);
        Set<BluetoothDevice> pairedDevices = new HashSet<BluetoothDevice>();
        if (bluetoothAdapter != null) {
            pairedDevices = bluetoothAdapter.getBondedDevices();
        }
        String[] entryValues = new String[pairedDevices.size()];
        String[] entries = new String[pairedDevices.size()];
        int i = 0;
        // Loop through paired devices
        for( BluetoothDevice device : pairedDevices ) {
            // Add the name and address to the ListPreference enties and entyValues
            Log.v(LOG_TAG, "device: " + device.getName() + " -- " + device.getAddress());
            entryValues[i] = device.getAddress();
            entries[i] = device.getName();
            i++;
        }
        prefDevices.setEntryValues(entryValues);
        prefDevices.setEntries(entries);
        Preference pref = (Preference) findPreference(IBluetoothListener.PREF_TRACK_RECORDING);
        pref.setEnabled(sharedPref.getBoolean(IBluetoothListener.PREF_START_GPS_PROVIDER, false));
        pref = (Preference) findPreference(IBluetoothListener.PREF_MOCK_GPS_NAME);
        String mockProvider = sharedPref.getString(IBluetoothListener.PREF_MOCK_GPS_NAME, getString(R.string.defaultMockGpsName));
        pref.setSummary(getString(R.string.pref_mock_gps_name_summary, mockProvider));
        pref = (Preference) findPreference(IBluetoothListener.PREF_CONNECTION_RETRIES);
        String maxConnRetries = sharedPref.getString(IBluetoothListener.PREF_CONNECTION_RETRIES,
                getString(R.string.defaultConnectionRetries));
        pref.setSummary(getString(R.string.pref_connection_retries_summary, maxConnRetries));
        pref = (Preference) findPreference(IBluetoothListener.PREF_GPS_LOCATION_PROVIDER);
        if (sharedPref.getBoolean(IBluetoothListener.PREF_REPLACE_STD_GPS, true)) {
            String s = getString(R.string.pref_gps_location_provider_summary);
            pref.setSummary(s);
            Log.v(LOG_TAG, "loc. provider: " + s);
            Log.v(LOG_TAG, "loc. provider: " + pref.getSummary());
        } else {
            String s = getString(R.string.pref_mock_gps_name_summary, mockProvider);
            pref.setSummary(s);
            Log.v(LOG_TAG, "loc. provider: " + s);
            Log.v(LOG_TAG, "loc. provider: " + pref.getSummary());
        }
        this.onContentChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange( Preference preference, Object newValue ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key ) {
        if (IBluetoothListener.PREF_START_GPS_PROVIDER.equals(key)) {
            boolean val = sharedPreferences.getBoolean(key, false);
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            if (pref.isChecked() != val) {
                pref.setChecked(val);
            } else if (val) {
                start();
                // startService(new Intent(IBluetoothListener.ACTION_START_GPS_PROVIDER));
            } else {
                stop();
                // startService(new Intent(IBluetoothListener.ACTION_STOP_GPS_PROVIDER));
            }
        } else if (IBluetoothListener.PREF_TRACK_RECORDING.equals(key)) {
            boolean val = sharedPreferences.getBoolean(key, false);
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            if (pref.isChecked() != val) {
                pref.setChecked(val);
            } else if (val) {
                startService(new Intent(IBluetoothListener.ACTION_START_TRACK_RECORDING));
            } else {
                startService(new Intent(IBluetoothListener.ACTION_STOP_TRACK_RECORDING));
            }
        } else if (IBluetoothListener.PREF_BLUETOOTH_DEVICE.equals(key)) {
            updateDevicePreferenceSummary();
        } else if (IBluetoothListener.PREF_SIRF_ENABLE_GLL.equals(key) //
                || IBluetoothListener.PREF_SIRF_ENABLE_GGA.equals(key)//
                || IBluetoothListener.PREF_SIRF_ENABLE_RMC.equals(key)//
                || IBluetoothListener.PREF_SIRF_ENABLE_VTG.equals(key)//
                || IBluetoothListener.PREF_SIRF_ENABLE_GSA.equals(key)//
                || IBluetoothListener.PREF_SIRF_ENABLE_GSV.equals(key)//
                || IBluetoothListener.PREF_SIRF_ENABLE_ZDA.equals(key)//
                || IBluetoothListener.PREF_SIRF_ENABLE_SBAS.equals(key)//
                || IBluetoothListener.PREF_SIRF_ENABLE_NMEA.equals(key)//
                || IBluetoothListener.PREF_SIRF_ENABLE_STATIC_NAVIGATION.equals(key)) {
            enableSirfFeature(key);
        }
        this.updateDevicePreferenceList();
    }

    private void start() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        String deviceAddress = sharedPreferences.getString(IBluetoothListener.PREF_BLUETOOTH_DEVICE, null);
        int maxConRetries = Integer.parseInt(sharedPreferences.getString(IBluetoothListener.PREF_CONNECTION_RETRIES,
                this.getString(R.string.defaultConnectionRetries)));
        BluetoothManager2 gpsManager = BluetoothManager2.BLUETOOTHMANAGER;
        if (!gpsManager.isEnabled()) {
            if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
                // String mockProvider = LocationManager.GPS_PROVIDER;
                // if (!sharedPreferences.getBoolean(PREF_REPLACE_STD_GPS, true)) {
                // mockProvider = sharedPreferences.getString(PREF_MOCK_GPS_NAME,
                // getString(R.string.defaultMockGpsName));
                // }
                // gpsManager = new BluetoothManager(this, deviceAddress, maxConRetries);
                gpsManager.connect(this, deviceAddress, maxConRetries);
                boolean enabled = gpsManager.enable();
                // Bundle extras = intent.getExtras();
                if (sharedPreferences.getBoolean(IBluetoothListener.PREF_START_GPS_PROVIDER, false) != enabled) {
                    edit.putBoolean(IBluetoothListener.PREF_START_GPS_PROVIDER, enabled);
                    edit.commit();
                }
                if (enabled) {
                    // gpsManager.enableMockLocationProvider(mockProvider);
                    Notification notification = new Notification(R.drawable.ic_stat_notify,
                            this.getString(R.string.foreground_gps_provider_started_notification), System.currentTimeMillis());
                    Intent myIntent = new Intent(this, BluetoothPreferencesActivity.class);
                    PendingIntent myPendingIntent = PendingIntent.getActivity(this, 0, myIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    notification.setLatestEventInfo(getApplicationContext(),
                            this.getString(R.string.foreground_service_started_notification_title),
                            this.getString(R.string.foreground_gps_provider_started_notification), myPendingIntent);
                    // startForeground(R.string.foreground_gps_provider_started_notification,
                    // notification);
                    // if (sharedPreferences.getBoolean(IBluetoothListener.PREF_SIRF_GPS, false)) {
                    // enableSirfConfig(sharedPreferences);
                    // }
                    Utilities.toast(this, this.getString(R.string.msg_gps_provider_started), Toast.LENGTH_LONG);
                }
            }
        } else {
            Utilities.toast(this, this.getString(R.string.msg_gps_provider_already_started), Toast.LENGTH_LONG);
        }
    }

    private void stop() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = sharedPreferences.edit();

        if (sharedPreferences.getBoolean(IBluetoothListener.PREF_START_GPS_PROVIDER, true)) {
            edit.putBoolean(IBluetoothListener.PREF_START_GPS_PROVIDER, false);
            edit.commit();
        }
        
        BluetoothManager2.BLUETOOTHMANAGER.disable();
    }

    private void enableSirfFeature( String key ) {
        CheckBoxPreference pref = (CheckBoxPreference) (findPreference(key));
        if (pref.isChecked() != sharedPref.getBoolean(key, false)) {
            pref.setChecked(sharedPref.getBoolean(key, false));
        } else {
            Intent configIntent = new Intent(IBluetoothListener.ACTION_CONFIGURE_SIRF_GPS);
            configIntent.putExtra(key, pref.isChecked());
            startService(configIntent);
        }
    }
}
