package eu.geopaparazzi.library.forms;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import eu.geopaparazzi.library.R;

public class FragmentDetailActivity extends FragmentActivity {
    private String formName;
    private String sectionName;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        // Need to check if Activity has been switched to landscape mode
        // If yes, finished and go back to the start Activity
        // if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        // {
        // finish();
        // return;
        // }

        if (savedInstanceState != null) {
            formName = savedInstanceState.getString(FormUtilities.ATTR_FORMNAME);
        }
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            formName = extras.getString(FormUtilities.ATTR_FORMNAME);
            sectionName = extras.getString(FormUtilities.ATTR_SECTIONNAME);
        }

        setContentView(R.layout.details_activity_layout);
    }

    public String getFormName() {
        return formName;
    }

    public String getSectionName() {
        return sectionName;
    }

}