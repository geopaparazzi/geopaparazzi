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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IImagesDbHelper;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.forms.FormListFragment.IFragmentListSupporter;
import eu.geopaparazzi.library.forms.constraints.Constraints;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.share.ShareUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.UrlUtilities;
import eu.geopaparazzi.library.util.Utilities;

import static eu.geopaparazzi.library.forms.FormUtilities.TAG_IS_RENDER_LABEL;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_KEY;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_VALUE;

/**
 * The form activity.
 * <p/>
 * <p>This returns an array of {@link String} data that can be retrieved
 * through: {@link LibraryConstants#PREFS_KEY_FORM} and contain:</p>
 * <ul>
 * <li>longitude</li>
 * <li>latitude</li>
 * <li>elevation (or -1.0)</li>
 * <li>timestamp</li>
 * <li>a name for the form</li>
 * <li>the filled form data json</li>
 * </ul>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class FormActivity extends AppCompatActivity implements IFragmentListSupporter {
    //    private final int RETURNCODE_DETAILACTIVITY = 665;
    public static final String USE_MAPCENTER_POSITION = "USE_MAPCENTER_POSITION";

    private double latitude = -9999.0;
    private double longitude = -9999.0;
    private double elevation = -9999.0;
    private String mSectionName;
    private String mFormName;
    private JSONObject sectionObject;
    private List<String> formNames4Section = new ArrayList<String>();
    private long noteId = -1;
    private boolean noteIsNew = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        try {
            Bundle extras = getIntent().getExtras();
            if (savedInstanceState != null) {
                // state has been restored
                readBundle(savedInstanceState);
                FormInfoHolder formInfoHolder = new FormInfoHolder();
                formInfoHolder.sectionName = mSectionName;
                formInfoHolder.formName = mFormName;
                if (formInfoHolder.formName == null)
                    formInfoHolder.formName = formNames4Section.get(0);
                formInfoHolder.noteId = noteId;
                formInfoHolder.longitude = longitude;
                formInfoHolder.latitude = latitude;
                formInfoHolder.sectionObjectString = sectionObject.toString();


                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.detailFragment);
                if (currentFragment instanceof FormDetailFragment) {
                    FormDetailFragment formDetailFragment = (FormDetailFragment) currentFragment;
                    formDetailFragment.refreshView(formInfoHolder);
                }
            } else if (extras != null) {
                readBundle(extras);

                if (formNames4Section.size() > 0) {
                    FormInfoHolder formInfoHolder = new FormInfoHolder();
                    if (formInfoHolder.formName == null)
                        formInfoHolder.formName = formNames4Section.get(0);
                    mFormName = formInfoHolder.formName;
                    formInfoHolder.noteId = noteId;
                    formInfoHolder.longitude = longitude;
                    formInfoHolder.latitude = latitude;
                    formInfoHolder.sectionObjectString = sectionObject.toString();

                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.detailFragment);
                    if (currentFragment instanceof FormDetailFragment) {
                        FormDetailFragment formDetailFragment = (FormDetailFragment) currentFragment;
                        formDetailFragment.refreshView(formInfoHolder);
                    }
                }

            }


            String titleString = toolbar.getTitle().toString();
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean useMapCenterPosition = preferences.getBoolean(USE_MAPCENTER_POSITION, false);
            if (mSectionName != null && mSectionName.length() > 0)
                titleString = mSectionName;
            if (useMapCenterPosition) {
                titleString += " (" + getString(R.string.note_in_map_center) + ")";
            } else {
                titleString += " (" + getString(R.string.note_in_gps) + ")";
            }
            getSupportActionBar().setTitle(titleString);
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    private void readBundle(Bundle bundle) {
        if (bundle != null) {
            Serializable formInfoHolder = bundle.getSerializable(FormInfoHolder.BUNDLE_KEY_INFOHOLDER);
            if (formInfoHolder instanceof FormInfoHolder) {
                FormInfoHolder formInfo = (FormInfoHolder) formInfoHolder;

                extractVariablesFromForminfo(formInfo);
            }
        }


    }

    private void extractVariablesFromForminfo(FormInfoHolder formInfo) {
        mSectionName = formInfo.sectionName;
        mFormName = formInfo.formName;
        latitude = formInfo.latitude;
        longitude = formInfo.longitude;
        elevation = formInfo.elevation;
        noteId = formInfo.noteId;
        noteIsNew = !formInfo.objectExists;

        try {
            if (formInfo.sectionObjectString == null) {
                sectionObject = TagsManager.getInstance(this).getSectionByName(mSectionName);
                // copy the section object, which will be kept around along the activity
                formInfo.sectionObjectString = sectionObject.toString();
            }

            sectionObject = new JSONObject(formInfo.sectionObjectString);
            formNames4Section = TagsManager.getFormNames4Section(sectionObject);
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        try {
            Fragment detailFragment = getSupportFragmentManager().findFragmentById(R.id.detailFragment);
            if (detailFragment instanceof FormDetailFragment) {
                FormDetailFragment fdFragment = (FormDetailFragment) detailFragment;
                fdFragment.storeFormItems(false);

                FormInfoHolder formInfo = fdFragment.getFormInfoHolder();
                sectionObject = new JSONObject(formInfo.sectionObjectString);


                FormInfoHolder formInfoHolder = new FormInfoHolder();
                formInfoHolder.sectionName = mSectionName;
                formInfoHolder.formName = mFormName;
                formInfoHolder.sectionObjectString = sectionObject.toString();
                formInfoHolder.noteId = noteId;
                formInfoHolder.longitude = longitude;
                formInfoHolder.latitude = latitude;
                formInfoHolder.elevation = elevation;
                formInfoHolder.objectExists = !noteIsNew;

                outState.putSerializable(FormInfoHolder.BUNDLE_KEY_INFOHOLDER, formInfoHolder);
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_form, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            try {
                shareAction();
            } catch (Exception e) {
                GPLog.error(this, null, e);
                GPDialogs.warningDialog(this, e.getLocalizedMessage(), null);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void save(View view) {
        try {
            saveAction();
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.warningDialog(this, e.getLocalizedMessage(), null);
        }
    }

    @Override
    public void onBackPressed() {
        GPDialogs.yesNoMessageDialog(this, getString(R.string.form_exit_prompt),
                new Runnable() {
                    @Override
                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    saveAction();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                    }
                }, new Runnable() {
                    @Override
                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (noteIsNew) {
                                    cancelResult();
                                }
                                FormActivity.super.onBackPressed();
                            }
                        });

                    }
                }
        );
    }

    private void cancelResult() {
        Intent intent = getIntent();
        intent.putExtra(LibraryConstants.DATABASE_ID, noteId);
        setResult(Activity.RESULT_CANCELED, intent);
    }

    private void shareAction() throws Exception {
        String form = sectionObject.toString();
        float lat = (float) latitude;
        float lon = (float) longitude;
        String osmUrl = UrlUtilities.osmUrlFromLatLong(lat, lon, true, false);

        IImagesDbHelper imageHelper = DefaultHelperClasses.getDefaulfImageHelper();
        File tempDir = ResourcesManager.getInstance(this).getTempDir();

        // for now only one image is shared
        List<String> imageIds = FormUtilities.getImageIds(form);
        File imageFile = null;
        if (imageIds.size() > 0) {
            String imageId = imageIds.get(0);

            Image image = imageHelper.getImage(Long.parseLong(imageId));

            String name = image.getName();
            imageFile = new File(tempDir, name);

            byte[] imageData = imageHelper.getImageDataById(image.getImageDataId(), null);
            ImageUtilities.writeImageDataToFile(imageData, imageFile.getAbsolutePath());

        }
        String formText = FormUtilities.formToPlainText(form, false);
        formText = formText + "\n" + osmUrl;
        String shareNoteMsg = getResources().getString(R.string.share_note_with);
        if (imageFile != null) {
            ShareUtilities.shareTextAndImage(this, shareNoteMsg, formText, imageFile);
        } else {
            ShareUtilities.shareText(this, shareNoteMsg, formText);
        }

    }

    private void saveAction() throws Exception {
        // if in landscape mode store last inserted info, since that fragment has not been stored
        Fragment detailFragment = getSupportFragmentManager().findFragmentById(R.id.detailFragment);
        if (detailFragment instanceof FormDetailFragment) {
            FormDetailFragment fdFragment = (FormDetailFragment) detailFragment;
            fdFragment.storeFormItems(false);
            FormInfoHolder formInfo = fdFragment.getFormInfoHolder();
            sectionObject = new JSONObject(formInfo.sectionObjectString);
        }

        // extract and check constraints
        List<String> availableFormNames = TagsManager.getFormNames4Section(sectionObject);
        String renderingLabel = null;
        for (String formName : availableFormNames) {
            JSONObject formObject = TagsManager.getForm4Name(formName, sectionObject);

            JSONArray formItemsArray = TagsManager.getFormItems(formObject);

            int length = formItemsArray.length();
            String value = null;
            for (int i = 0; i < length; i++) {
                JSONObject jsonObject = formItemsArray.getJSONObject(i);

                String key = "-";
                if (jsonObject.has(TAG_KEY))
                    key = jsonObject.getString(TAG_KEY).trim();

                if (jsonObject.has(TAG_VALUE)) {
                    value = jsonObject.getString(TAG_VALUE).trim();
                }
                if (jsonObject.has(TAG_IS_RENDER_LABEL)) {
                    String isRenderingLabelStr = jsonObject.getString(TAG_IS_RENDER_LABEL).trim();
                    boolean isRenderingLabel = Boolean.parseBoolean(isRenderingLabelStr);
                    if (isRenderingLabel)
                        renderingLabel = value;
                }

                // inject latitude
                if (key.equals(LibraryConstants.LATITUDE)) {
                    String latitudeString = String.valueOf(latitude);
                    value = latitudeString;
                    jsonObject.put(TAG_VALUE, latitudeString);
                }
                // inject longitude
                if (key.equals(LibraryConstants.LONGITUDE)) {
                    String longitudeString = String.valueOf(longitude);
                    value = longitudeString;
                    jsonObject.put(TAG_VALUE, longitudeString);
                }

                Constraints constraints = FormUtilities.handleConstraints(jsonObject, null);
                if (value == null || !constraints.isValid(value)) {
                    String constraintDescription = constraints.getDescription();
                    String validfieldMsg = getString(R.string.form_field_check);
                    String msg = Utilities.format(validfieldMsg, key, formName, constraintDescription);
                    GPDialogs.infoDialog(this, msg, null);
                    return;
                }
            }
        }

        // finally store data
        String sectionObjectString = sectionObject.toString();
        long timestamp = System.currentTimeMillis();

        if (renderingLabel == null) {
            renderingLabel = mSectionName;
        }

        FormInfoHolder formInfoHolder = new FormInfoHolder();
        formInfoHolder.sectionName = mSectionName;
        formInfoHolder.formName = mFormName;
        formInfoHolder.noteId = noteId;
        formInfoHolder.longitude = longitude;
        formInfoHolder.latitude = latitude;
        formInfoHolder.elevation = elevation;
        formInfoHolder.sectionObjectString = sectionObjectString;

        formInfoHolder.lastTimestamp = timestamp;
        formInfoHolder.renderingLabel = renderingLabel;

        Intent intent = getIntent();
        intent.putExtra(FormInfoHolder.BUNDLE_KEY_INFOHOLDER, formInfoHolder);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onListItemSelected(String selectedItemName) {
        // depending on the mode, set the detail fragment or launch the detail activity

        FormDetailFragment currentFragment = (FormDetailFragment) getSupportFragmentManager().findFragmentById(R.id.detailFragment);
        FormInfoHolder formInfoHolder = new FormInfoHolder();
        formInfoHolder.sectionName = mSectionName;
        mFormName = selectedItemName;
        formInfoHolder.formName = mFormName;
        formInfoHolder.noteId = noteId;
        formInfoHolder.longitude = longitude;
        formInfoHolder.latitude = latitude;
        try {
            currentFragment.storeFormItems(false);
            FormInfoHolder formInfo = currentFragment.getFormInfoHolder();
            formInfoHolder.sectionObjectString = formInfo.sectionObjectString;
            sectionObject = new JSONObject(formInfo.sectionObjectString);

            currentFragment.refreshView(formInfoHolder);
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    @Override
    public List<String> getListTitles() {
        return formNames4Section;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        result = new Result();
//        result.requestCode = requestCode;
//        result.resultCode = resultCode;
//        result.data = data;
//
//        FormDetailFragment currentFragment = (FormDetailFragment) getSupportFragmentManager().findFragmentById(R.id.detailFragmentContainer);
//        if (currentFragment != null)
//            currentFragment.onActivityResult(requestCode, resultCode, data);

//        switch (requestCode) {
//            case (RETURNCODE_DETAILACTIVITY): {
//                if (resultCode == AppCompatActivity.RESULT_OK) {
//                    FormInfoHolder formInfoHolder = (FormInfoHolder) data.getSerializableExtra(FormInfoHolder.BUNDLE_KEY_INFOHOLDER);
//                    extractVariablesFromForminfo(formInfoHolder);
//                }
//                break;
//            }
//        }
    }

}
