package eu.geopaparazzi.library.nfc;

import java.util.Set;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.bluetooth.BluetoothManager;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

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
            Toast.makeText(getApplicationContext(), "Please activate NFC and press Back to return to the application!",
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
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
            lastReadNfcMessage = "Unable to read tag id";
            if (idBytes != null) {
                lastReadNfcMessage = getHexString(idBytes);
            }
            readMessageEditText.setText(lastReadNfcMessage);
        }

    }

    public String getHexString( byte[] b ) {
        StringBuffer sb = new StringBuffer();
        for( int i = b.length - 1; i >= 0; i-- ) {
            if (i >= 0)
                sb.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    // public String convertbyteArrayToHexString( byte in[] ) {
    //
    // byte ch = 0x00;
    // int i = in.length - 1;
    //
    // if (in == null)
    // return null;
    //
    // String HEXSET[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E",
    // "F"};
    // // Double length, as you're converting an array of 8 bytes, to 16 characters for hexadecimal
    // StringBuffer out = new StringBuffer(in.length * 2);
    //
    // // You need to iterate from msb to lsb, in the case of using iCode SLI rfid
    // while( i >= 0 ) {
    // ch = (byte) (in[i] & 0xF0); // Strip off high nibble
    // ch = (byte) (ch >>> 4); // shift the bits down
    // ch = (byte) (ch & 0x0F); // must do this is high order bit is on!
    // out.append(HEXSET[(int) ch]); // convert the nibble to a String Character
    // ch = (byte) (in[i] & 0x0F); // Strip off low nibble
    // out.append(HEXSET[(int) ch]); // convert the nibble to a String Character
    // i--;
    // }
    // return (new String(out));
    // }

}
