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

import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maps.TagsManager;

/**
 * Utilities methods for form stuff.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 2.6
 */
public class FormUtilities {

    /**
     * Adds a {@link TextView} to the supplied mainView.
     * 
     * @param context the context.
     * @param mainView the main view to which to add the new widget to.
     * @param key the key identifying the widget.
     * @param value the value to put in the widget.
     * @param type the text type:
     *          <ul>
     *             <li>0: text</li>
     *             <li>1: numeric</li>
     *             <li>2: phone</li>
     *             <li>3: date</li>
     *          </ul>
     * @return
     */
    public static View addTextView( Context context, LinearLayout mainView, String key, String value, int type ) {
        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        // textLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.formitem_background));
        mainView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key);
        textView.setTextColor(context.getResources().getColor(R.color.hydrogreen));

        textLayout.addView(textView);

        EditText editView = new EditText(context);
        editView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        editView.setPadding(15, 5, 15, 5);
        editView.setText(value);

        switch( type ) {
        case 1:
            editView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            break;
        case 2:
            editView.setInputType(InputType.TYPE_CLASS_PHONE);
            break;
        case 3:
            editView.setInputType(InputType.TYPE_CLASS_DATETIME);
            break;
        default:
            break;
        }

        textLayout.addView(editView);
        return editView;
    }

    public static void addBooleanView( Context context, HashMap<String, View> key2WidgetMap, List<String> keyList,
            LinearLayout mainView, String key, String value ) {
        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        mainView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key);
        textView.setTextColor(context.getResources().getColor(R.color.hydrogreen));

        textLayout.addView(textView);

        Spinner spinner = new Spinner(context);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        spinner.setPadding(15, 5, 15, 5);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.true_false,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (value != null) {
            String[] stringArray = context.getResources().getStringArray(R.array.true_false);
            for( int i = 0; i < stringArray.length; i++ ) {
                if (stringArray[i].equals(value.trim())) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }

        textLayout.addView(spinner);

        key2WidgetMap.put(key, spinner);
        keyList.add(key);

    }

    public static void addComboView( Context context, HashMap<String, View> key2WidgetMap, List<String> keyList,
            LinearLayout mainView, String key, String value, JSONObject jsonObject ) throws JSONException {
        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        mainView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key);
        textView.setTextColor(context.getResources().getColor(R.color.hydrogreen));
        textLayout.addView(textView);

        JSONArray comboItems = TagsManager.getComboItems(jsonObject);
        String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);

        Spinner spinner = new Spinner(context);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        spinner.setPadding(15, 5, 15, 5);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, itemsArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (value != null) {
            for( int i = 0; i < itemsArray.length; i++ ) {
                if (itemsArray[i].equals(value.trim())) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }

        textLayout.addView(spinner);

        key2WidgetMap.put(key, spinner);
        keyList.add(key);
    }

}
