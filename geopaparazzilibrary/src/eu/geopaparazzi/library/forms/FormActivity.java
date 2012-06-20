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

import static eu.geopaparazzi.library.forms.FormUtilities.CONSTRAINT_MANDATORY;
import static eu.geopaparazzi.library.forms.FormUtilities.CONSTRAINT_RANGE;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_KEY;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_VALUE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.forms.constraints.Constraints;
import eu.geopaparazzi.library.forms.constraints.MandatoryConstraint;
import eu.geopaparazzi.library.forms.constraints.RangeConstraint;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;

/**
 * The form activity.
 * 
 * <p>This returns an array of {@link String} data that can be retrieved
 * through: {@link LibraryConstants#PREFS_KEY_FORM} and contain:</p>
 * <ul>
 *   <li>longitude</li>
 *   <li>latitude</li>
 *   <li>elevation (or -1.0)</li>
 *   <li>timestamp</li>
 *   <li>a name for the form</li>
 *   <li>the filled form data json</li>
 * </ul>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class FormActivity extends FragmentActivity {

    private static final int MENU_SAVE = Menu.FIRST;
    private static final int MENU_CANCEL = 2;

    private double latitude;
    private double longitude;
    private String sectionName;
    private JSONObject sectionObject;
    private List<String> formNames4Section = new ArrayList<String>();

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sectionName = extras.getString(LibraryConstants.PREFS_KEY_FORM_NAME);
            latitude = extras.getDouble(LibraryConstants.LATITUDE);
            longitude = extras.getDouble(LibraryConstants.LONGITUDE);
        }

        try {
            sectionObject = TagsManager.getInstance(this).getSectionByName(sectionName);
            // copy the section object, which will be kept around along te activity
            String sectionObjectString = sectionObject.toString();
            sectionObjectString = sectionObjectString.replaceAll(LibraryConstants.LATITUDE, String.valueOf(latitude));
            sectionObjectString = sectionObjectString.replaceAll(LibraryConstants.LONGITUDE, String.valueOf(longitude));

            sectionObject = new JSONObject(sectionObjectString);
            formNames4Section = TagsManager.getInstance(this).getFormNames4Section(sectionObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setContentView(R.layout.form);
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_SAVE, 1, "Save").setIcon(android.R.drawable.ic_menu_save);
        menu.add(Menu.NONE, MENU_CANCEL, 2, "Cancel").setIcon(android.R.drawable.ic_menu_close_clear_cancel);

        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MENU_SAVE:
            try {
                saveAction();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        case MENU_CANCEL:
            finish();
            return true;
        default: {
        }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public List<String> getFragmentTitles() {
        return formNames4Section;
    }

    public String getSectionName() {
        return sectionName;
    }

    public JSONObject getSectionObject() {
        return sectionObject;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    private void saveAction() throws Exception {
        // extract constraints
        List<String> availableFormNames = TagsManager.getInstance(this).getFormNames4Section(sectionObject);
        for( String formName : availableFormNames ) {
            JSONObject formObject = TagsManager.getInstance(this).getForm4Name(formName, sectionObject);

            JSONArray formItemsArray = TagsManager.getFormItems(formObject);

            int length = formItemsArray.length();
            Constraints constraints = new Constraints();
            String value = null;
            for( int i = 0; i < length; i++ ) {
                JSONObject jsonObject = formItemsArray.getJSONObject(i);

                String key = "-";
                if (jsonObject.has(TAG_KEY))
                    key = jsonObject.getString(TAG_KEY).trim();

                if (jsonObject.has(TAG_VALUE)) {
                    value = jsonObject.getString(TAG_VALUE).trim();
                }
                if (jsonObject.has(CONSTRAINT_MANDATORY)) {
                    String mandatory = jsonObject.getString(CONSTRAINT_MANDATORY).trim();
                    if (mandatory.trim().equals("yes")) {
                        constraints.addConstraint(new MandatoryConstraint());
                    }
                }
                if (jsonObject.has(CONSTRAINT_RANGE)) {
                    String range = jsonObject.getString(CONSTRAINT_RANGE).trim();
                    String[] rangeSplit = range.split(",");
                    if (rangeSplit.length == 2) {
                        boolean lowIncluded = rangeSplit[0].startsWith("[") ? true : false;
                        String lowStr = rangeSplit[0].substring(1);
                        Double low = Utilities.adapt(lowStr, Double.class);
                        boolean highIncluded = rangeSplit[1].endsWith("]") ? true : false;
                        String highStr = rangeSplit[1].substring(0, rangeSplit[1].length() - 1);
                        Double high = Utilities.adapt(highStr, Double.class);
                        constraints.addConstraint(new RangeConstraint(low, lowIncluded, high, highIncluded));
                    }
                }
                if (value == null || !constraints.isValid(value)) {
                    String constraintDescription = constraints.getDescription();
                    String validfieldMsg = getString(R.string.form_field_check);
                    String msg = MessageFormat.format(validfieldMsg, key, formName, constraintDescription);
                    Utilities.messageDialog(this, msg, null);
                    return;
                }
            }
        }
    }

    public boolean onKeyDown( int keyCode, KeyEvent event ) {
        // force to exit through the exit button, in order to avoid losing info
        switch( keyCode ) {
        case KeyEvent.KEYCODE_BACK:
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // public void onCreate( Bundle icicle ) {
    // super.onCreate(icicle);
    // setContentView(R.layout.form);
    //
    // key2WidgetMap.clear();
    // keyList.clear();
    // key2ConstraintsMap.clear();
    //
    // Bundle extras = getIntent().getExtras();
    // if (extras != null) {
    // formJsonString = extras.getString(LibraryConstants.PREFS_KEY_FORM_JSON);
    // formNameDefinition = extras.getString(LibraryConstants.PREFS_KEY_FORM_NAME);
    // formCategoryDefinition = extras.getString(LibraryConstants.PREFS_KEY_FORM_CAT);
    // latitude = extras.getDouble(LibraryConstants.LATITUDE);
    // longitude = extras.getDouble(LibraryConstants.LONGITUDE);
    // }
    //
    // if (formJsonString == null) {
    // // TODO
    // return;
    // }
    //
    // TextView tagTextView = (TextView) findViewById(R.id.form_tag);
    // tagTextView.setText(formNameDefinition);
    //
    // LinearLayout mainView = (LinearLayout) findViewById(R.id.form_linear);
    // Button okButton = (Button) findViewById(R.id.form_ok);
    // okButton.setOnClickListener(new Button.OnClickListener(){
    // public void onClick( View v ) {
    // String finalJsonString = "";
    // try {
    // String result = storeNote();
    // if (result == null) {
    // finalJsonString = jsonFormObject.toString();
    // Date sqlDate = new Date(System.currentTimeMillis());
    // String timestamp = LibraryConstants.TIME_FORMATTER_SQLITE.format(sqlDate);
    // // DaoNotes.addNote(FormActivity.this, longitude, latitude, -1.0, sqlDate,
    // // formNameDefinition,
    // // finalJsonString, NoteType.SIMPLE.getTypeNum());
    //
    // String[] formDataArray = {//
    // String.valueOf(longitude), //
    // String.valueOf(latitude), //
    // "-1.0", //
    // timestamp, //
    // formNameDefinition, //
    // formCategoryDefinition, //
    // finalJsonString};
    //
    // Intent intent = getIntent();
    // intent.putExtra(LibraryConstants.PREFS_KEY_FORM, formDataArray);
    // setResult(Activity.RESULT_OK, intent);
    //
    // finish();
    // } else {
    // String msg = MessageFormat.format(getString(R.string.check_valid_field), result);
    // Utilities.messageDialog(FormActivity.this, msg, null);
    // }
    // } catch (Exception e) {
    // Logger.e(this, e.getLocalizedMessage(), e);
    // Utilities.messageDialog(FormActivity.this, "An error occurred while saving:\n" +
    // finalJsonString,
    // new Runnable(){
    // public void run() {
    // finish();
    // }
    // });
    // }
    // }
    //
    // });
    // Button cancelButton = (Button) findViewById(R.id.form_cancel);
    // cancelButton.setOnClickListener(new Button.OnClickListener(){
    // public void onClick( View v ) {
    // finish();
    // }
    // });
    //
    // try {
    // jsonFormObject = new JSONObject(formJsonString);
    // formItemsArray = TagsManager.getFormItems(jsonFormObject);
    //
    // int length = formItemsArray.length();
    // for( int i = 0; i < length; i++ ) {
    // JSONObject jsonObject = formItemsArray.getJSONObject(i);
    //
    // String key = "-";
    // if (jsonObject.has(TAG_KEY))
    // key = jsonObject.getString(TAG_KEY).trim();
    //
    // String value = "";
    // if (jsonObject.has(TAG_VALUE)) {
    // value = jsonObject.getString(TAG_VALUE).trim();
    // }
    // String type = FormUtilities.TYPE_STRING;
    // if (jsonObject.has(TAG_TYPE)) {
    // type = jsonObject.getString(TAG_TYPE).trim();
    // }
    //
    // Constraints constraints = new Constraints();
    // if (jsonObject.has(CONSTRAINT_MANDATORY)) {
    // String mandatory = jsonObject.getString(CONSTRAINT_MANDATORY).trim();
    // if (mandatory.trim().equals("yes")) {
    // constraints.addConstraint(new MandatoryConstraint());
    // }
    // }
    // if (jsonObject.has(CONSTRAINT_RANGE)) {
    // String range = jsonObject.getString(CONSTRAINT_RANGE).trim();
    // String[] rangeSplit = range.split(",");
    // if (rangeSplit.length == 2) {
    // boolean lowIncluded = rangeSplit[0].startsWith("[") ? true : false;
    // String lowStr = rangeSplit[0].substring(1);
    // Double low = Utilities.adapt(lowStr, Double.class);
    // boolean highIncluded = rangeSplit[1].endsWith("]") ? true : false;
    // String highStr = rangeSplit[1].substring(0, rangeSplit[1].length() - 1);
    // Double high = Utilities.adapt(highStr, Double.class);
    // constraints.addConstraint(new RangeConstraint(low, lowIncluded, high, highIncluded));
    // }
    // }
    // key2ConstraintsMap.put(key, constraints);
    // String constraintDescription = constraints.getDescription();
    //
    // View addedView = null;
    // if (type.equals(TYPE_STRING)) {
    // addedView = FormUtilities.addEditText(this, mainView, key, value, 0, constraintDescription);
    // } else if (type.equals(TYPE_DOUBLE)) {
    // addedView = FormUtilities.addEditText(this, mainView, key, value, 1, constraintDescription);
    // } else if (type.equals(TYPE_DATE)) {
    // addedView = FormUtilities.addEditText(this, mainView, key, value, 3, constraintDescription);
    // } else if (type.equals(TYPE_LABEL)) {
    // String size = jsonObject.getString("size");
    // addedView = FormUtilities.addTextView(this, mainView, value, size, false);
    // } else if (type.equals(TYPE_LABELWITHLINE)) {
    // String size = jsonObject.getString("size");
    // addedView = FormUtilities.addTextView(this, mainView, value, size, true);
    // } else if (type.equals(TYPE_BOOLEAN)) {
    // addedView = FormUtilities.addBooleanView(this, mainView, key, value, constraintDescription);
    // } else if (type.equals(TYPE_STRINGCOMBO)) {
    // JSONArray comboItems = TagsManager.getComboItems(jsonObject);
    // String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
    // addedView = FormUtilities.addComboView(this, mainView, key, value, itemsArray,
    // constraintDescription);
    // } else if (type.equals(TYPE_STRINGMULTIPLECHOICE)) {
    // JSONArray comboItems = TagsManager.getComboItems(jsonObject);
    // String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
    // addedView = FormUtilities
    // .addMultiSelectionView(this, mainView, key, value, itemsArray, constraintDescription);
    // } else {
    // System.out.println("Type non implemented yet: " + type);
    // }
    // key2WidgetMap.put(key, addedView);
    // keyList.add(key);
    //
    // }
    //
    // } catch (JSONException e) {
    // Logger.e(this, e.getLocalizedMessage(), e);
    // e.printStackTrace();
    // }
    //
    // }

}
