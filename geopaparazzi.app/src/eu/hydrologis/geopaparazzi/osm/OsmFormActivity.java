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
package eu.hydrologis.geopaparazzi.osm;

import java.io.File;
import static eu.hydrologis.geopaparazzi.maps.tags.FormUtilities.*;
import java.io.IOException;
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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.maps.TagsManager;
import eu.hydrologis.geopaparazzi.maps.tags.FormUtilities;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.FileUtils;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * The osm form activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class OsmFormActivity extends Activity {
    private String formJsonString;
    // private String formShortnameDefinition;
    private String formLongnameDefinition;

    private HashMap<String, View> key2WidgetMap = new HashMap<String, View>();
    private List<String> keyList = new ArrayList<String>();
    private JSONArray formItemsArray;
    private JSONObject jsonFormObject;
    private float latitude;
    private float longitude;
    private String category;
    private String tagName;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.osm_form);

        key2WidgetMap.clear();
        keyList.clear();

        Bundle extras = getIntent().getExtras();
        category = extras.getString(Constants.OSM_CATEGORY_KEY);
        tagName = extras.getString(Constants.OSM_TAG_KEY);

        TextView textView = (TextView) findViewById(R.id.osmform_textview);
        StringBuilder sb = new StringBuilder();
        sb.append(tagName);
        sb.append(" (");
        sb.append(category);
        sb.append(")");
        textView.setText(sb.toString());

        try {
            File tagsFolderFile = OsmTagsManager.getInstance().getTagsFolderFile(this);

            sb = new StringBuilder();
            sb.append(category);
            sb.append("/");
            sb.append(tagName);
            sb.append(".json");
            File tagsJsonFile = new File(tagsFolderFile, sb.toString());
            if (!tagsJsonFile.exists()) {
                sb = new StringBuilder();
                sb.append(category);
                sb.append("/");
                sb.append(category);
                sb.append(".json");
                tagsJsonFile = new File(tagsFolderFile, sb.toString());
            }
            formJsonString = FileUtils.readfile(tagsJsonFile);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        latitude = preferences.getFloat(Constants.VIEW_CENTER_LAT, 0f);
        longitude = preferences.getFloat(Constants.VIEW_CENTER_LON, 0f);

        LinearLayout mainView = (LinearLayout) findViewById(R.id.osmform_linear);
        Button okButton = (Button) findViewById(R.id.osmform_ok);
        okButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                String endString = "";
                try {
                    storeNote();
                    endString = jsonFormObject.toString();
                    Date sqlDate = new Date(System.currentTimeMillis());
                    DaoNotes.addNote(OsmFormActivity.this, longitude, latitude, -1.0, sqlDate, formLongnameDefinition, endString);
                    finish();
                } catch (Exception e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    AlertDialog.Builder builder = new AlertDialog.Builder(OsmFormActivity.this);
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
        Button cancelButton = (Button) findViewById(R.id.osmform_cancel);
        cancelButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                finish();
            }
        });

        try {
            JSONArray tagArrayObj = new JSONArray(formJsonString);
            jsonFormObject = tagArrayObj.getJSONObject(0);
            formItemsArray = TagsManager.getFormItems(jsonFormObject);

            int length = formItemsArray.length();
            for( int i = 0; i < length; i++ ) {
                JSONObject jsonObject = formItemsArray.getJSONObject(i);

                String key = jsonObject.getString(TAG_KEY).trim();
                String value = "";
                if (jsonObject.has(TAG_VALUE)) {
                    value = jsonObject.getString(TAG_VALUE).trim();
                }
                String type = TYPE_STRING;
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
