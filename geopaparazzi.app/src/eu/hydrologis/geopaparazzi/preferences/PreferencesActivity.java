package eu.hydrologis.geopaparazzi.preferences;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import eu.hydrologis.geopaparazzi.R;

public class PreferencesActivity extends PreferenceActivity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.my_preferences);
    }

}
