package eu.geopaparazzi.library.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

public class NfcIdReaderActivity extends Activity {

    private NfcAdapter nfcAdapter;
    private String lastReadNfcMessage;
    private EditText readMessageEditText;
    private PendingIntent nfcPendingIntent;
    private IntentFilter[] ndefExchangeFilters;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_id_reader);

        // ProgressBar progressBar = (ProgressBar) findViewById(R.id.scanning_progressbar);

        readMessageEditText = (EditText) findViewById(R.id.read_id_edittext);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Please activate NFC and press Back to return to the application!",
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
        }
        if (!nfcAdapter.isEnabled()) {
            // Handle all of our received NFC intents in this activity.
            nfcPendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            // Intent filters for reading a note from a tag or exchanging over p2p.
            IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            try {
                ndefDetected.addDataType("text/plain");
            } catch (MalformedMimeTypeException e) {
            }
            ndefExchangeFilters = new IntentFilter[]{ndefDetected};
        }

    }
    public void okPushed( View view ) {

    }

    public void cancelPushed( View view ) {
        finish();
    }

    @Override
    protected void onNewIntent( Intent intent ) {
        // NDEF exchange mode
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            NdefMessage[] msgs = getNdefMessages(intent);
            lastReadNfcMessage = new String(msgs[0].getRecords()[0].getPayload());
        }

    }

    private void enableNdefExchangeMode() {
        if (nfcAdapter.isEnabled())
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, ndefExchangeFilters, null);
    }

    private void disableNdefExchangeMode() {
        if (nfcAdapter.isEnabled())
            nfcAdapter.disableForegroundDispatch(this);
    }

    private NdefMessage[] getNdefMessages( Intent intent ) {
        // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for( int i = 0; i < rawMsgs.length; i++ ) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                msgs = new NdefMessage[]{msg};
            }
        } else {
            if (Debug.D)
                Logger.d(this, "Unknown intent.");
            finish();
        }
        return msgs;
    }
}
