package eu.geopaparazzi.library.nfc;

import java.util.Set;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
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
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;

public class NfcIdReaderActivity extends Activity {

    private NfcAdapter nfcAdapter;
    private String lastReadNfcMessage;
    private EditText readMessageEditText;
    private TextView btActivityLabel;
    private TextView nfcActivityLabel;
    private boolean inReadMode = false;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_id_reader);

        // ProgressBar progressBar = (ProgressBar) findViewById(R.id.scanning_progressbar);

        readMessageEditText = (EditText) findViewById(R.id.read_id_edittext);
        btActivityLabel = (TextView) findViewById(R.id.bluetoothActiveLabel);
        nfcActivityLabel = (TextView) findViewById(R.id.nfcActiveLabel);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (!nfcAdapter.isEnabled()) {
            Utilities.messageDialog(this, R.string.activate_nfc, new Runnable(){
                public void run() {
                    startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                }
            });
        }

        checkScanners();
    }

    private void checkScanners() {
        if (!nfcAdapter.isEnabled()) {
            nfcActivityLabel.setBackgroundResource(R.layout.background_red);
        } else {
            nfcActivityLabel.setBackgroundResource(R.layout.background_green);
        }

        Set<BluetoothDevice> bondedDevices = BluetoothManager.INSTANCE.getBondedDevices();
        if (bondedDevices.size() == 0) {
            btActivityLabel.setBackgroundResource(R.layout.background_red);
        } else {
            btActivityLabel.setBackgroundResource(R.layout.background_green);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkScanners();

        if (!inReadMode) {
            if (nfcAdapter.isEnabled()) {

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
        if (isFinishing()) {
            if (nfcAdapter.isEnabled())
                nfcAdapter.disableForegroundDispatch(this);
            inReadMode = false;
        }
        super.onPause();
    }

    public void okPushed( View view ) {
        Intent intent = getIntent();
        intent.putExtra(LibraryConstants.PREFS_KEY_TEXT, lastReadNfcMessage);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

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
                lastReadNfcMessage = Utilities.getHexString(idBytes);
                msg = lastReadNfcMessage;
            } else {
                lastReadNfcMessage = ""; //$NON-NLS-1$
            }
            readMessageEditText.setText(msg);
        }

    }

}
