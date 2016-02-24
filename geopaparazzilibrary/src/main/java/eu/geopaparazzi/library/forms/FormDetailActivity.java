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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * Fragment detail view activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FormDetailActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_form_detail);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            FormInfoHolder formInfoHolder = (FormInfoHolder) extras.getSerializable(FormInfoHolder.BUNDLE_KEY_INFOHOLDER);

            FormDetailFragment formDetailFragment = FormDetailFragment.newInstance(formInfoHolder);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.detailFragmentContainer, formDetailFragment);
            transaction.commit();
        } else {
            GPDialogs.warningDialog(this, "No FormInfoHolder supplied.", new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }

    }

    @Override
    public void onBackPressed() {
        FormDetailFragment currentFragment = (FormDetailFragment) getSupportFragmentManager().findFragmentById(R.id.detailFragmentContainer);
        if (currentFragment != null) {
            try {
                currentFragment.storeFormItems(false);
            } catch (Exception e) {
                GPLog.error(this, null, e);
            }
            JSONObject returnSectionObject = currentFragment.getSectionObject();
            Intent intent = getIntent();
            intent.putExtra(FormUtilities.ATTR_SECTIONOBJECTSTR, returnSectionObject.toString());
            if(getParent() != null)
                getParent().setResult(AppCompatActivity.RESULT_OK, intent);
            else
                setResult(AppCompatActivity.RESULT_OK, intent);
        }

        super.onBackPressed();
    }

}