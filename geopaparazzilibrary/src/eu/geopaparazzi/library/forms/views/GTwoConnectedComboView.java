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
package eu.geopaparazzi.library.forms.views;

import static eu.geopaparazzi.library.forms.FormUtilities.COLON;
import static eu.geopaparazzi.library.forms.FormUtilities.UNDERSCORE;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.geopaparazzi.library.R;

/**
 * A view that presents 2 connected {@link Spinner}s.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GTwoConnectedComboView extends View implements GView, OnItemSelectedListener {

    private Spinner titleSpinner;
    private Spinner valuesSpinner;
    private LinkedHashMap<String, List<String>> dataMap;
    private LinearLayout textLayout;
    private String value;

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param defStyle def style.
     */
    public GTwoConnectedComboView( Context context, AttributeSet attrs, int defStyle ) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     */
    public GTwoConnectedComboView( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param parentView parent
     * @param key key
     * @param value value
     * @param dataMap the map of the data.
     * @param constraintDescription constraints
     */
    public GTwoConnectedComboView( Context context, AttributeSet attrs, LinearLayout parentView, String key, String value,
            LinkedHashMap<String, List<String>> dataMap, String constraintDescription ) {
        super(context, attrs);
        this.value = value;
        this.dataMap = dataMap;

        textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        parentView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(context.getResources().getColor(R.color.formcolor));
        textLayout.addView(textView);

        titleSpinner = new Spinner(context);
        titleSpinner.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        titleSpinner.setPadding(15, 5, 15, 5);
        String[] titlesArray = dataMap.keySet().toArray(new String[0]);
        String[] titlesArray2 = new String[titlesArray.length + 1];
        System.arraycopy(titlesArray, 0, titlesArray2, 1, titlesArray.length);
        titlesArray2[0] = "";
        ArrayAdapter<String> titleListAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
                titlesArray2);
        titleListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        titleSpinner.setAdapter(titleListAdapter);
        titleSpinner.setOnItemSelectedListener(this);

        List<String> valuesList = null;
        if (value != null) {
            String titleString = null;
            if (value.length() == 0) {
                titleString = "";
            } else {
                Set<Entry<String, List<String>>> entrySet = dataMap.entrySet();
                for( Entry<String, List<String>> entry : entrySet ) {
                    List<String> tmpValuesList = entry.getValue();
                    if (tmpValuesList.contains(value.trim())) {
                        titleString = entry.getKey();
                        break;
                    }
                }
            }
            if (titleString != null) {
                valuesList = dataMap.get(titleString);
                for( int i = 0; i < titlesArray2.length; i++ ) {
                    if (titlesArray2[i].equals(titleString.trim())) {
                        titleSpinner.setSelection(i);
                        break;
                    }
                }
            }
        }

        valuesSpinner = new Spinner(context);
        valuesSpinner.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        valuesSpinner.setPadding(15, 5, 15, 5);
        List<String> dummyValuesList = new ArrayList<String>();
        if (valuesList != null) {
            dummyValuesList = valuesList;
        }
        ArrayAdapter<String> valuesListAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
                dummyValuesList);
        valuesListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        valuesSpinner.setAdapter(valuesListAdapter);
        // valuesSpinner.setOnItemSelectedListener(this);
        checkValueSpinnerSelection(valuesList);
        textLayout.addView(titleSpinner);
        textLayout.addView(valuesSpinner);

    }

    private void checkValueSpinnerSelection( List<String> valuesList ) {
        if (valuesList != null) {
            for( int i = 0; i < valuesList.size(); i++ ) {
                if (valuesList.get(i).equals(value.trim())) {
                    valuesSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    public String getValue() {
        Object selectedItem = valuesSpinner.getSelectedItem();
        if (selectedItem == null) {
            return "";
        }
        return selectedItem.toString();
    }

    @Override
    public void setOnActivityResult( Intent data ) {
        // ignore
    }

    @Override
    public void refresh( Context context ) {
        // ignore
    }

    @Override
    public void onItemSelected( AdapterView< ? > parent, View callingView, int pos, long arg3 ) {
        if (parent == titleSpinner) {
            String title = titleSpinner.getSelectedItem().toString();
            List<String> valuesList;
            if (title.length() == 0) {
                // nothing selected yet
                valuesList = new ArrayList<String>();
            } else {
                valuesList = dataMap.get(title);
            }
            ArrayAdapter<String> valuesListAdapter = new ArrayAdapter<String>(parent.getContext(),
                    android.R.layout.simple_spinner_item, valuesList);
            valuesListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            valuesSpinner.setAdapter(valuesListAdapter);
            checkValueSpinnerSelection(valuesList);
        }
    }

    @Override
    public void onNothingSelected( AdapterView< ? > arg0 ) {
        // ignore
    }

}