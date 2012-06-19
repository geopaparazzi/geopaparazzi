package eu.geopaparazzi.library.forms;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.LibraryConstants;

public class FragmentDetailActivity extends FragmentActivity {
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        
        // Need to check if Activity has been switched to landscape mode
        // If yes, finished and go back to the start Activity
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            finish();
            return;
        }

        setContentView(R.layout.details_activity_layout);
        if (savedInstanceState != null) {
            String formJsonString = savedInstanceState.getString(LibraryConstants.PREFS_KEY_FORM_JSON);
            String formNameDefinition = savedInstanceState.getString(LibraryConstants.PREFS_KEY_FORM_NAME);
            String formCategoryDefinition = savedInstanceState.getString(LibraryConstants.PREFS_KEY_FORM_CAT);
            double latitude = savedInstanceState.getDouble(LibraryConstants.LATITUDE);
            double longitude = savedInstanceState.getDouble(LibraryConstants.LONGITUDE);
        }
    }
}