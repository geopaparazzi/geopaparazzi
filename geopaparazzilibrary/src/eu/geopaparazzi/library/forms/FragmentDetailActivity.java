package eu.geopaparazzi.library.forms;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import eu.geopaparazzi.library.R;

public class FragmentDetailActivity extends FragmentActivity {
    private String formName;
    private String sectionObjectString;
    private JSONObject sectionObject;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        // Need to check if Activity has been switched to landscape mode
        // If yes, finished and go back to the start Activity
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            finish();
            return;
        }

        if (savedInstanceState != null) {
            formName = savedInstanceState.getString(FormUtilities.ATTR_FORMNAME);
        }
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            formName = extras.getString(FormUtilities.ATTR_FORMNAME);
            sectionObjectString = extras.getString(FormUtilities.ATTR_SECTIONOBJECTSTR);
            try {
                sectionObject = new JSONObject(sectionObjectString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        setContentView(R.layout.details_activity_layout);
    }

    public String getFormName() {
        return formName;
    }
    public JSONObject getSectionObject() {
        return sectionObject;
    }
    public String getSectionName() {
        return sectionObjectString;
    }

    public boolean onKeyDown( int keyCode, KeyEvent event ) {
        // force to exit through the exit button, in order to avoid losing info
        switch( keyCode ) {
        case KeyEvent.KEYCODE_BACK:
            FragmentDetail currentFragment = (FragmentDetail) getSupportFragmentManager().findFragmentById(R.id.detailFragment);
            if (currentFragment != null) {
                try {
                    currentFragment.storeFormItems(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JSONObject returnSectionObject = currentFragment.getSectionObject();
                Intent intent = getIntent();
                intent.putExtra(FormUtilities.ATTR_SECTIONOBJECTSTR, returnSectionObject.toString());
                setResult(Activity.RESULT_OK, intent);
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}