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
package eu.hydrologis.geopaparazzi.osm.tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.osm.TagsManager;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * The form activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FormActivity extends Activity {
    private String formJsonString;
    private String formShortnameDefinition;
    private String formLongnameDefinition;

    private HashMap<String, View> key2WidgetMap = new HashMap<String, View>();
    private List<String> keyList = new ArrayList<String>();
    private JSONArray formItemsArray;
    private JSONObject jsonFormObject;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.form);

        key2WidgetMap.clear();
        keyList.clear();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            formJsonString = extras.getString(Constants.FORMJSON_KEY);
            formShortnameDefinition = extras.getString(Constants.FORMSHORTNAME_KEY);
            formLongnameDefinition = extras.getString(Constants.FORMLONGNAME_KEY);
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
                try {
                    storeNote();

                    ApplicationManager.openDialog(jsonFormObject.toString(2), FormActivity.this);
                } catch (JSONException e) {
                    e.printStackTrace();
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
            JSONObject formObj = jsonFormObject.getJSONObject(TagsManager.TAG_FORM);
            formItemsArray = formObj.getJSONArray(TagsManager.TAG_FORMITEMS);

            int length = formItemsArray.length();
            for( int i = 0; i < length; i++ ) {
                JSONObject jsonObject = formItemsArray.getJSONObject(i);

                String key = jsonObject.getString(TagsManager.TAG_KEY).trim();
                String value = "";
                if (jsonObject.has(TagsManager.TAG_VALUE)) {
                    value = jsonObject.getString(TagsManager.TAG_VALUE).trim();
                }
                String type = TagsManager.TYPE_STRING;
                if (jsonObject.has(TagsManager.TAG_TYPE)) {
                    type = jsonObject.getString(TagsManager.TAG_TYPE).trim();
                }

                if (type.equals(TagsManager.TYPE_STRING)) {
                    addTextView(mainView, key, value, jsonObject, false);
                } else if (type.equals(TagsManager.TYPE_DOUBLE)) {
                    addTextView(mainView, key, value, jsonObject, true);
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void storeNote() throws JSONException {
        // update the name with info
        jsonFormObject.put(TagsManager.TAG_LONGNAME, formLongnameDefinition);

        // update the items
        for( String key : keyList ) {
            View view = key2WidgetMap.get(key);
            String text = null;
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                text = textView.getText().toString();

            }

            try {
                if (text != null)
                    update(key, text);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }

    private void update( String key, String value ) throws JSONException {
        int length = formItemsArray.length();
        for( int i = 0; i < length; i++ ) {
            JSONObject itemObject = formItemsArray.getJSONObject(i);
            String objKey = itemObject.getString(TagsManager.TAG_KEY).trim();
            if (objKey.equals(key)) {
                itemObject.put(TagsManager.TAG_VALUE, value);
            }
        }
    }

    private void addTextView( LinearLayout mainView, String key, String value, JSONObject jsonObject, boolean numeric ) {
        LinearLayout textLayout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        // textLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.formitem_background));
        mainView.addView(textLayout);

        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key);
        textView.setTextColor(R.color.hydrogreen);

        textLayout.addView(textView);

        EditText editView = new EditText(this);
        editView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        editView.setPadding(5, 5, 5, 5);
        editView.setText(value);
        if (numeric) {
            editView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }

        textLayout.addView(editView);

        key2WidgetMap.put(key, editView);
        keyList.add(key);

    }


}
