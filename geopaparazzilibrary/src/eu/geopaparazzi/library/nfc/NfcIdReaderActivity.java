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
package eu.geopaparazzi.library.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.bluetooth.BluetoothManager;
import eu.geopaparazzi.library.bluetooth.IBluetoothIOHandler;
import eu.geopaparazzi.library.bluetooth.IBluetoothListener;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;

/**
 * @author moovida
 *
 */
public class NfcIdReaderActivity extends Activity implements IBluetoothListener {

    private NfcAdapter nfcAdapter;
    private String lastReadNfcMessage;
    private EditText readMessageEditText;
    private TextView btActivityLabel;
    private TextView nfcActivityLabel;
    private boolean inReadMode = false;
    private IBluetoothIOHandler bluetoothDevice;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_id_reader);

        // ProgressBar progressBar = (ProgressBar) findViewById(R.id.scanning_progressbar);

        readMessageEditText = (EditText) findViewById(R.id.read_id_edittext);
        btActivityLabel = (TextView) findViewById(R.id.bluetoothActiveLabel);
        nfcActivityLabel = (TextView) findViewById(R.id.nfcActiveLabel);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled()) {
                Utilities.messageDialog(this, R.string.activate_nfc, new Runnable(){
                    public void run() {
                        startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                    }
                });
            }
        }

        checkScanners();
    }

    private void checkScanners() {
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            nfcActivityLabel.setBackgroundResource(R.layout.background_red);
        } else {
            nfcActivityLabel.setBackgroundResource(R.layout.background_green);
        }

        if (bluetoothDevice == null) {
            btActivityLabel.setBackgroundResource(R.layout.background_red);
        } else {
            btActivityLabel.setBackgroundResource(R.layout.background_green);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        bluetoothDevice = BluetoothManager.INSTANCE.getBluetoothDevice();
        if (bluetoothDevice != null) {
            bluetoothDevice.addListener(this);
        }
        checkScanners();

        if (!inReadMode) {
            if (nfcAdapter != null && nfcAdapter.isEnabled()) {

                // Handle all of our received NFC intents in this activity.
                Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

                // Intent filters for reading a note from a tag or exchanging over p2p.
                IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                IntentFilter[] ndefExchangeFilters = new IntentFilter[]{tagDetected};
                nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, ndefExchangeFilters, null);
            }
        }
    }

    @Override
    protected void onPause() {
        if (bluetoothDevice != null) {
            bluetoothDevice.removeListener(this);
        }

        if (isFinishing()) {
            if (nfcAdapter != null && nfcAdapter.isEnabled())
                nfcAdapter.disableForegroundDispatch(this);
            inReadMode = false;
        }
        super.onPause();
    }

    /**
     * Ok action.
     * 
     * @param view parent.
     */
    public void okPushed( View view ) {
        Intent intent = getIntent();
        intent.putExtra(LibraryConstants.PREFS_KEY_TEXT, lastReadNfcMessage);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /**
     * Cancel action.
     * 
     * @param view parent.
     */
    public void cancelPushed( View view ) {
        finish();
    }

    @Override
    protected void onNewIntent( Intent intent ) {
        // NDEF exchange mode
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] idBytes = null;
            if (tag != null) {
                idBytes = tag.getId();
            } else {
                idBytes = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            }
            String msg = getString(R.string.unable_to_read_tag_id);
            if (idBytes != null) {
                lastReadNfcMessage = Utilities.getHexString(idBytes, -1);
                msg = lastReadNfcMessage;
            } else {
                lastReadNfcMessage = ""; //$NON-NLS-1$
            }
            readMessageEditText.setText(msg);
        }

    }

    @Override
    public void onDataReceived( long time, final Object data ) {
        if (data != null) {
            runOnUiThread(new Runnable(){
                public void run() {
                    lastReadNfcMessage = data.toString().toLowerCase();
                    readMessageEditText.setText(lastReadNfcMessage);
                }
            });
        }
    }

}
