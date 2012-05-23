package eu.geopaparazzi.library.util.activities;

import android.app.Activity;
import android.os.Bundle;
import eu.geopaparazzi.library.util.Utilities;

public class PlaySoundActivity extends Activity {

    public static final String MESSAGE = "MESSAGE"; //$NON-NLS-1$

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        String msg = null;
        if (extras != null) {
            msg = extras.getString(MESSAGE);
        }

        Utilities.ring(this);
        
        if (msg != null) {
            Utilities.messageDialog(this, msg, null);
        }
    }
}
