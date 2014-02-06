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
package eu.geopaparazzi.library.forms;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * Fragment detail view activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FragmentDetailActivity extends FragmentActivity {
    private String formName;
    private String sectionObjectString;
    private JSONObject sectionObject;
    private double longitude;
    private double latitude;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        // don't permit rotation
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
            longitude = extras.getDouble(LibraryConstants.LONGITUDE);
            latitude = extras.getDouble(LibraryConstants.LATITUDE);
        }

        setContentView(R.layout.details_activity_layout);
    }

    /**
     * @return the form name.
     */
    public String getFormName() {
        return formName;
    }

    /**
     * @return the section object.
     */
    public JSONObject getSectionObject() {
        return sectionObject;
    }

    /**
     * @return the sectionname.
     */
    public String getSectionName() {
        return sectionObjectString;
    }

    /**
     * @return the latitude.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @return the longitude.
     */
    public double getLongitude() {
        return longitude;
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