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
package eu.hydrologis.geopaparazzi.maps.tags;

import static eu.hydrologis.geopaparazzi.maps.tags.FormUtilities.TAG_KEY;
import static eu.hydrologis.geopaparazzi.maps.tags.FormUtilities.TAG_LONGNAME;
import static eu.hydrologis.geopaparazzi.maps.tags.FormUtilities.TAG_TYPE;
import static eu.hydrologis.geopaparazzi.maps.tags.FormUtilities.TAG_VALUE;
import static eu.hydrologis.geopaparazzi.maps.tags.FormUtilities.TYPE_BOOLEAN;
import static eu.hydrologis.geopaparazzi.maps.tags.FormUtilities.TYPE_DOUBLE;
import static eu.hydrologis.geopaparazzi.maps.tags.FormUtilities.TYPE_STRING;
import static eu.hydrologis.geopaparazzi.maps.tags.FormUtilities.TYPE_STRINGCOMBO;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.maps.TagsManager;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * The form activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class FormActivity extends Activity {
    private String formJsonString;
    // private String formShortnameDefinition;
    private String formLongnameDefinition;

    private HashMap<String, View> key2WidgetMap = new HashMap<String, View>();
    private List<String> keyList = new ArrayList<String>();
    private JSONArray formItemsArray;
    private JSONObject jsonFormObject;
    private float latitude;
    private float longitude;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.form);

        key2WidgetMap.clear();
        keyList.clear();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            formJsonString = extras.getString(Constants.FORMJSON_KEY);
            // formShortnameDefinition = extras.getString(Constants.FORMSHORTNAME_KEY);
            formLongnameDefinition = extras.getString(Constants.FORMLONGNAME_KEY);
            latitude = extras.getFloat(Constants.PREFS_KEY_MAPCENTER_LAT);
            longitude = extras.getFloat(Constants.PREFS_KEY_MAPCENTER_LON);
        }

        if (formJsonString == null) {
            // TODO
            return;
        }

        TextView tagTextView = (TextView) findViewById(R.id.form_tag);
        tagTextView.setText(formLongnameDefinition);

        LinearLayout mainView = (LinearLayout) findViewById(R.id.form_linear);
        Button okButton = (Button) findViewById(R.id.form_ok);
        okButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                String endString = "";
                try {
                    storeNote();
                    endString = jsonFormObject.toString();
                    Date sqlDate = new Date(System.currentTimeMillis());
                    DaoNotes.addNote(FormActivity.this, longitude, latitude, -1.0, sqlDate, formLongnameDefinition, endString);
                    finish();
                } catch (Exception e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    AlertDialog.Builder builder = new AlertDialog.Builder(FormActivity.this);
                    builder.setMessage("An error occurred while saving:\n" + endString).setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                                public void onClick( DialogInterface dialog, int id ) {
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
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

                View addedView = null;
                if (type.equals(TYPE_STRING)) {
                    addedView = FormUtilities.addTextView(this, mainView, key, value, 0);
                } else if (type.equals(TYPE_DOUBLE)) {
                    addedView = FormUtilities.addTextView(this, mainView, key, value, 1);
                } else if (type.equals(TYPE_BOOLEAN)) {
                    addedView = FormUtilities.addBooleanView(this, mainView, key, value);
                } else if (type.equals(TYPE_STRINGCOMBO)) {
                    JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                    String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                    addedView = FormUtilities.addComboView(this, mainView, key, value, itemsArray);
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

    private void storeNote() throws JSONException {
        // update the name with info
        jsonFormObject.put(TAG_LONGNAME, formLongnameDefinition);

        // update the items
        for( String key : keyList ) {
            View view = key2WidgetMap.get(key);
            String text = null;
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                text = textView.getText().toString();
            } else if (view instanceof Spinner) {
                Spinner spinner = (Spinner) view;
                text = spinner.getSelectedItem().toString();
            }

            try {
                if (text != null)
                    FormUtilities.update(formItemsArray, key, text);
            } catch (JSONException e) {
                Logger.e(this, e.getLocalizedMessage(), e);
                e.printStackTrace();
            }
        }
    }

}
