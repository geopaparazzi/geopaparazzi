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
package eu.geopaparazzi.library.forms.views;

import android.app.Activity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.NamedList;

import static eu.geopaparazzi.library.forms.FormUtilities.COLON;
import static eu.geopaparazzi.library.forms.FormUtilities.UNDERSCORE;

/**
 * A view that presents a {@link Spinner} connected to 2 others.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GOneToManyConnectedComboView extends View implements GView, OnItemSelectedListener {

    private Spinner mainComboSpinner;
    private ArrayList<Spinner> name2ComboMap;
    private LinkedHashMap<String, List<NamedList<String>>> dataMap;
    private LinearLayout combosLayout;
    private String _mainComboValue;
    private List<String> _comboValues = new ArrayList<>();

    private String sep = "#";

    /**
     * @param context  the context to use.
     * @param attrs    attributes.
     * @param defStyle def style.
     */
    public GOneToManyConnectedComboView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context the context to use.
     * @param attrs   attributes.
     */
    public GOneToManyConnectedComboView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context               the context to use.
     * @param attrs                 attributes.
     * @param parentView            parent
     * @param label                 label
     * @param value                 _value
     * @param dataMap               the map of the data.
     * @param constraintDescription constraints
     */
    public GOneToManyConnectedComboView(Context context, AttributeSet attrs, LinearLayout parentView, String label, String value,
                                        LinkedHashMap<String, List<NamedList<String>>> dataMap, String constraintDescription) {
        super(context, attrs);
        this.dataMap = dataMap;

        if (value == null)
            value = "";

        String[] valueSplit = value.split(sep);
        if (valueSplit.length > 1) {
            _mainComboValue = valueSplit[0];
            for (int i = 1; i < valueSplit.length; i++) {
                _comboValues.add(valueSplit[i]);
            }
        } else {
            _mainComboValue = "";
            for (int i = 1; i < dataMap.size(); i++) {
                _comboValues.add(valueSplit[i]);
            }
        }

        combosLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        combosLayout.setLayoutParams(layoutParams);
        combosLayout.setOrientation(LinearLayout.VERTICAL);
        parentView.addView(combosLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(label.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(Compat.getColor(context, R.color.formcolor));
        combosLayout.addView(textView);

        mainComboSpinner = new Spinner(context);
        LinearLayout.LayoutParams titleSpinnerParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleSpinnerParams.setMargins(15, 25, 15, 15);
        mainComboSpinner.setLayoutParams(titleSpinnerParams);
        Set<String> mainNamesSet = dataMap.keySet();
        ArrayList<String> mainComboItems = new ArrayList<>(mainNamesSet);
        mainComboItems.add(0, "");
        ArrayAdapter<String> titleListAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, mainComboItems);
        titleListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mainComboSpinner.setAdapter(titleListAdapter);
        mainComboSpinner.setPopupBackgroundDrawable(Compat.getDrawable(context, R.drawable.background_spinner));
        mainComboSpinner.setBackground(Compat.getDrawable(context, R.drawable.background_spinner));
        int minHeight = getMinComboHeight(context);
        mainComboSpinner.setMinimumHeight(minHeight);

        List<NamedList<String>> otherComboItemsList = new ArrayList<>();
        if (_mainComboValue.length() > 0) {
            otherComboItemsList = dataMap.get(_mainComboValue);
            int indexOf = mainComboItems.indexOf(_mainComboValue.trim());
            if (indexOf != -1) {
                mainComboSpinner.setSelection(indexOf, false);
            }
        }
        combosLayout.addView(mainComboSpinner);

        name2ComboMap = new ArrayList<>();

        List<NamedList<String>> namedLists = dataMap.get(_mainComboValue);
        if (namedLists == null) {
            // use the first one for the names
            namedLists = dataMap.values().iterator().next();
        }
        for (NamedList<String> namedList : namedLists) {
            TextView subTextView = new TextView(context);
            subTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            subTextView.setPadding(2, 2, 2, 2);
            subTextView.setText(namedList.name);
            subTextView.setTextColor(Compat.getColor(context, R.color.formcolor));
            combosLayout.addView(subTextView);

            Spinner subSpinner = new Spinner(context);
            LinearLayout.LayoutParams valueSpinnerParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            valueSpinnerParams.setMargins(15, 25, 15, 15);
            subSpinner.setLayoutParams(valueSpinnerParams);
            subSpinner.setPopupBackgroundDrawable(Compat.getDrawable(context, R.drawable.background_spinner));
            subSpinner.setBackground(Compat.getDrawable(context, R.drawable.background_spinner));
            subSpinner.setMinimumHeight(minHeight);

            ArrayAdapter<String> combo2ListAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, namedList.items);
            combo2ListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            subSpinner.setAdapter(combo2ListAdapter);

            name2ComboMap.add(subSpinner);
            combosLayout.addView(subSpinner);
//            if (_combo2Value.length() > 0) {
////            int indexOf = combo2ItemsList.indexOf(_combo2Value.trim());
////            if (indexOf != -1) {
////            }
//                int position = combo2ListAdapter.getPosition(_combo2Value);
//                combo2Spinner.setSelection(position, false);
//            }
        }


        mainComboSpinner.setOnItemSelectedListener(this);
    }

    private int getMinComboHeight(Context context) {
        int minHeight = 48;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            android.util.TypedValue tv = new android.util.TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, tv, true);
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            float ret = tv.getDimension(metrics);

            minHeight = (int) (ret - 1 * metrics.density);
        }
        return minHeight;
    }


    public String getValue() {
        Object mainComboItem = mainComboSpinner.getSelectedItem();
        String result = mainComboItem.toString();
        for (Spinner spinner : name2ComboMap) {
            Object item = spinner.getSelectedItem();
            result += sep + item;
        }
        return result;
    }

    @Override
    public void setOnActivityResult(Intent data) {
        // ignore
    }

    @Override
    public void refresh(Context context) {
        // ignore
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View callingView, int pos, long arg3) {
//        if (parent == combo1Spinner) {
//            String combo1Item = combo1Spinner.getSelectedItem().toString();
//            List<String> valuesList = new ArrayList<>();
//            if (combo1Item.length() != 0) {
//                valuesList = dataMap.get(combo1Item);
//            }
//            ArrayAdapter<String> valuesListAdapter = new ArrayAdapter<>(parent.getContext(), android.R.layout.simple_spinner_dropdown_item, valuesList);
//            valuesListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            combo2Spinner.setAdapter(valuesListAdapter);
//        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // ignore
    }

}