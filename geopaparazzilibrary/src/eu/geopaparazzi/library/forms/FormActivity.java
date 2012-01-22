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
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_LONGNAME;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_TYPE;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_VALUE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_BOOLEAN;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_DOUBLE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_STRING;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_STRINGCOMBO;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_STRINGMULTIPLECHOICE;

import java.sql.Date;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.forms.constraints.Constraints;
import eu.geopaparazzi.library.forms.constraints.MandatoryConstraint;
import eu.geopaparazzi.library.forms.constraints.RangeConstraint;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Logger;

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
public class FormActivity extends Activity {
    private String formJsonString;
    // private String formShortnameDefinition;
    private String formNameDefinition;

    private HashMap<String, View> key2WidgetMap = new HashMap<String, View>();
    private HashMap<String, Constraints> key2ConstraintsMap = new HashMap<String, Constraints>();
    private List<String> keyList = new ArrayList<String>();
    private JSONArray formItemsArray;
    private JSONObject jsonFormObject;
    private double latitude;
    private double longitude;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.form);

        key2WidgetMap.clear();
        keyList.clear();
        key2ConstraintsMap.clear();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            formJsonString = extras.getString(LibraryConstants.PREFS_KEY_FORM_JSON);
            formNameDefinition = extras.getString(LibraryConstants.PREFS_KEY_FORM_NAME);
            latitude = extras.getDouble(LibraryConstants.LATITUDE);
            longitude = extras.getDouble(LibraryConstants.LONGITUDE);
        }

        if (formJsonString == null) {
            // TODO
            return;
        }

        TextView tagTextView = (TextView) findViewById(R.id.form_tag);
        tagTextView.setText(formNameDefinition);

        LinearLayout mainView = (LinearLayout) findViewById(R.id.form_linear);
        Button okButton = (Button) findViewById(R.id.form_ok);
        okButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                String finalJsonString = "";
                try {
                    String result = storeNote();
                    if (result == null) {
                        finalJsonString = jsonFormObject.toString();
                        Date sqlDate = new Date(System.currentTimeMillis());
                        String timestamp = LibraryConstants.TIME_FORMATTER_SQLITE.format(sqlDate);
                        // DaoNotes.addNote(FormActivity.this, longitude, latitude, -1.0, sqlDate,
                        // formNameDefinition,
                        // finalJsonString, NoteType.SIMPLE.getTypeNum());

                        String[] formDataArray = {//
                        String.valueOf(longitude), //
                                String.valueOf(latitude), //
                                "-1.0", //
                                timestamp, formNameDefinition, //
                                finalJsonString};

                        Intent intent = getIntent();
                        intent.putExtra(LibraryConstants.PREFS_KEY_FORM, formDataArray);
                        setResult(Activity.RESULT_OK, intent);

                        finish();
                    } else {
                        String msg = MessageFormat.format(getString(R.string.check_valid_field), result);
                        Utilities.messageDialog(FormActivity.this, msg, null);
                    }
                } catch (Exception e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                    Utilities.messageDialog(FormActivity.this, "An error occurred while saving:\n" + finalJsonString,
                            new Runnable(){
                                public void run() {
                                    finish();
                                }
                            });
                }
            }

        });
        Button cancelButton = (Button) findViewById(R.id.form_cancel);
        cancelButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                finish();
            }
        });

        try {
            jsonFormObject = new JSONObject(formJsonString);
            formItemsArray = TagsManager.getFormItems(jsonFormObject);

            int length = formItemsArray.length();
            for( int i = 0; i < length; i++ ) {
                JSONObject jsonObject = formItemsArray.getJSONObject(i);

                String key = jsonObject.getString(TAG_KEY).trim();
                String value = "";
                if (jsonObject.has(TAG_VALUE)) {
                    value = jsonObject.getString(TAG_VALUE).trim();
                }
                String type = FormUtilities.TYPE_STRING;
                if (jsonObject.has(TAG_TYPE)) {
                    type = jsonObject.getString(TAG_TYPE).trim();
                }

                Constraints constraints = new Constraints();
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
                key2ConstraintsMap.put(key, constraints);
                String constraintDescription = constraints.getDescription();

                View addedView = null;
                if (type.equals(TYPE_STRING)) {
                    addedView = FormUtilities.addTextView(this, mainView, key, value, 0, constraintDescription);
                } else if (type.equals(TYPE_DOUBLE)) {
                    addedView = FormUtilities.addTextView(this, mainView, key, value, 1, constraintDescription);
                } else if (type.equals(TYPE_BOOLEAN)) {
                    addedView = FormUtilities.addBooleanView(this, mainView, key, value, constraintDescription);
                } else if (type.equals(TYPE_STRINGCOMBO)) {
                    JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                    String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                    addedView = FormUtilities.addComboView(this, mainView, key, value, itemsArray, constraintDescription);
                } else if (type.equals(TYPE_STRINGMULTIPLECHOICE)) {
                    JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                    String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                    addedView = FormUtilities
                            .addMultiSelectionView(this, mainView, key, value, itemsArray, constraintDescription);
                } else {
                    System.out.println("Type non implemented yet: " + type);
                }
                key2WidgetMap.put(key, addedView);
                keyList.add(key);

            }

        } catch (JSONException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

    }

    private String storeNote() throws JSONException {
        // update the name with info
        jsonFormObject.put(TAG_LONGNAME, formNameDefinition);

        // update the items
        for( String key : keyList ) {
            Constraints constraints = key2ConstraintsMap.get(key);

            View view = key2WidgetMap.get(key);
            String text = null;
            if (view instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) view;
                boolean checked = checkBox.isChecked();
                text = checked ? "true" : "false";
            } else if (view instanceof Button) {
                Button button = (Button) view;
                text = button.getText().toString();
                if (text.trim().equals("...")) {
                    text = "";
                }
            } else if (view instanceof TextView) {
                TextView textView = (TextView) view;
                text = textView.getText().toString();
            } else if (view instanceof Spinner) {
                Spinner spinner = (Spinner) view;
                text = spinner.getSelectedItem().toString();
            }

            if (!constraints.isValid(text)) {
                return key;
            }

            try {
                if (text != null)
                    FormUtilities.update(formItemsArray, key, text);
            } catch (JSONException e) {
                Logger.e(this, e.getLocalizedMessage(), e);
                e.printStackTrace();
            }
        }

        return null;
    }

}
