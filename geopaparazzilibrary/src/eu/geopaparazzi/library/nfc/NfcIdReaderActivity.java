package eu.geopaparazzi.library.nfc;

import android.app.Activity;
import android.os.Bundle;
import eu.geopaparazzi.library.R;

public class NfcIdReaderActivity extends Activity {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_id_reader);
    }

}
