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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.NamedList;

import static eu.geopaparazzi.library.forms.FormUtilities.COLON;
import static eu.geopaparazzi.library.forms.FormUtilities.SEP;
import static eu.geopaparazzi.library.forms.FormUtilities.UNDERSCORE;

/**
 * A view that presents a {@link Spinner} connected to 2 others.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GOneToManyConnectedComboView extends View implements GView, OnItemSelectedListener {

    private Spinner mainComboSpinner;
    private ArrayList<Spinner> orderedSubCombosList;
    private LinkedHashMap<String, List<NamedList<String>>> dataMap;

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

        String[] valueSplit = value.split(SEP, -1);
        String _mainComboValue;
        List<String> _comboValues = new ArrayList<>();
        if (valueSplit.length > 1) {
            _mainComboValue = valueSplit[0];
            for (int i = 1; i < valueSplit.length; i++) {
                _comboValues.add(valueSplit[i]);
            }
        } else {
            _mainComboValue = "";
            for (int i = 1; i < dataMap.size(); i++) {
                _comboValues.add("");
            }
        }

        TextView textView = new TextView(context);
        LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        textViewParams.setMargins(15, 25, 15, 15);
        textView.setLayoutParams(textViewParams);
        textView.setPadding(2, 2, 2, 2);
        textView.setText(label.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(Compat.getColor(context, R.color.formcolor));
        parentView.addView(textView);

        LinearLayout combosLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        combosLayout.setLayoutParams(layoutParams);
        combosLayout.setOrientation(LinearLayout.VERTICAL);
        combosLayout.setBackground(Compat.getDrawable(context, R.drawable.thin_background_frame));
        parentView.addView(combosLayout);

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
        mainComboSpinner.setPopupBackgroundDrawable(Compat.getDrawable(context, R.drawable.thin_background_frame));
        mainComboSpinner.setBackground(Compat.getDrawable(context, R.drawable.thin_background_frame));
        int minHeight = getMinComboHeight(context);
        mainComboSpinner.setMinimumHeight(minHeight);

        if (_mainComboValue.length() > 0) {
            int indexOf = mainComboItems.indexOf(_mainComboValue.trim());
            if (indexOf != -1) {
                mainComboSpinner.setSelection(indexOf, false);
            }
        }
        combosLayout.addView(mainComboSpinner);

        orderedSubCombosList = new ArrayList<>();

        List<NamedList<String>> namedLists = dataMap.get(_mainComboValue);
        if (namedLists == null) {
            // use the first one for the names
            namedLists = dataMap.values().iterator().next();
        }
        int subCombosNum = namedLists.size();
        int subValuesSize = _comboValues.size();
        boolean hasValues = subCombosNum == subValuesSize;
        int index = 0;
        for (NamedList<String> namedList : namedLists) {
            TextView subTextView = new TextView(context);
            LinearLayout.LayoutParams subTextViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            subTextViewParams.setMargins(15, 25, 15, 15);
            subTextView.setLayoutParams(subTextViewParams);
            subTextView.setPadding(2, 2, 2, 2);
            subTextView.setText(namedList.name);
            subTextView.setTextColor(Compat.getColor(context, R.color.formcolor));
            combosLayout.addView(subTextView);

            Spinner subSpinner = new Spinner(context);
            LinearLayout.LayoutParams valueSpinnerParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            valueSpinnerParams.setMargins(15, 25, 15, 15);
            subSpinner.setLayoutParams(valueSpinnerParams);
            subSpinner.setPopupBackgroundDrawable(Compat.getDrawable(context, R.drawable.thin_background_frame));
            subSpinner.setBackground(Compat.getDrawable(context, R.drawable.thin_background_frame));
            subSpinner.setMinimumHeight(minHeight);

            ArrayAdapter<String> combo2ListAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, namedList.items);
            combo2ListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            subSpinner.setAdapter(combo2ListAdapter);

            orderedSubCombosList.add(subSpinner);
            combosLayout.addView(subSpinner);


            if (hasValues) {
                String subValue = _comboValues.get(index);
                index++;
                int position = combo2ListAdapter.getPosition(subValue);
                subSpinner.setSelection(position, false);
            }
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
        if (mainComboItem == null) mainComboItem = "";
        String result = mainComboItem.toString();
        for (Spinner spinner : orderedSubCombosList) {
            Object item = spinner.getSelectedItem();
            if (item == null) item = "";
            result += SEP + item;
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
        if (parent == mainComboSpinner) {
            String mainComboItem = mainComboSpinner.getSelectedItem().toString();

            List<NamedList<String>> namedLists = new ArrayList<>();
            if (mainComboItem.length() != 0) {
                namedLists = dataMap.get(mainComboItem);
            }
            for (int i = 0; i < namedLists.size(); i++) {
                NamedList<String> namedList = namedLists.get(i);
                Spinner subSpinner = orderedSubCombosList.get(i);
                ArrayAdapter<String> combo2ListAdapter = new ArrayAdapter<String>(parent.getContext(), android.R.layout.simple_spinner_dropdown_item, namedList.items);
                combo2ListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                subSpinner.setAdapter(combo2ListAdapter);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // ignore
    }

}