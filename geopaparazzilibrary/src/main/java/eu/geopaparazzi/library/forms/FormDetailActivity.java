/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;

/**
 * Fragment detail view activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FormDetailActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.details_activity_layout);
    }

    @Override
    protected void onPause() {
        FormDetailFragment currentFragment = (FormDetailFragment) getSupportFragmentManager().findFragmentById(R.id.detailFragment);
        if (currentFragment != null) {
            try {
                currentFragment.storeFormItems(false);
            } catch (Exception e) {
                GPLog.error(this, null, e);
            }
            JSONObject returnSectionObject = currentFragment.getSectionObject();
            Intent intent = getIntent();
            intent.putExtra(FormUtilities.ATTR_SECTIONOBJECTSTR, returnSectionObject.toString());
            setResult(Activity.RESULT_OK, intent);
        }

        super.onPause();
    }
}