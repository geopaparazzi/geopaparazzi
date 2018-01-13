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
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Compat;

import static eu.geopaparazzi.library.forms.FormUtilities.COLON;
import static eu.geopaparazzi.library.forms.FormUtilities.SEP;
import static eu.geopaparazzi.library.forms.FormUtilities.UNDERSCORE;

/**
 * A view that presents 2 connected {@link Spinner}s.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GTwoAutoCompleteConnectedTextView extends View implements GView, OnItemSelectedListener {

    private LinkedHashMap<String, List<String>> dataMap;
    private AutoCompleteTextView autoCompleteTextView1;
    private String selectedCombo1Entry;
    private String selectedCombo2Entry;
    private AutoCompleteTextView autoCompleteTextView2;
    private ArrayAdapter<String> combo2ArrayAdapter;

    /**
     * @param context  the context to use.
     * @param attrs    attributes.
     * @param defStyle def style.
     */
    public GTwoAutoCompleteConnectedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context the context to use.
     * @param attrs   attributes.
     */
    public GTwoAutoCompleteConnectedTextView(Context context, AttributeSet attrs) {
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
    public GTwoAutoCompleteConnectedTextView(Context context, AttributeSet attrs, LinearLayout parentView, String label, String value,
                                             LinkedHashMap<String, List<String>> dataMap, String constraintDescription) {
        super(context, attrs);
        this.dataMap = dataMap;

        if (value == null)
            value = "";

        String[] valueSplit = value.split(SEP);
        String _combo1Value;
        String _combo2Value;
        if (valueSplit.length == 2) {
            _combo1Value = valueSplit[0];
            _combo2Value = valueSplit[1];
        } else {
            _combo1Value = "";
            _combo2Value = "";
        }

        selectedCombo1Entry = _combo1Value;
        selectedCombo2Entry = _combo2Value;

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

        createCombo1(context, _combo1Value, dataMap);

        List<String> combo2ItemsList = new ArrayList<>();
        if (_combo1Value.length() > 0) {
            combo2ItemsList = dataMap.get(_combo1Value);
        }

        createCombo2(context, _combo2Value, combo2ItemsList);

        combosLayout.addView(autoCompleteTextView1);
        combosLayout.addView(autoCompleteTextView2);
    }

    private void createCombo2(Context context, String _combo2Value, List<String> combo2ItemsList) {
        autoCompleteTextView2 = new AutoCompleteTextView(context);

        combo2ArrayAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_dropdown_item_1line, combo2ItemsList);
        autoCompleteTextView2.setAdapter(combo2ArrayAdapter);
        if (_combo2Value != null)
            autoCompleteTextView2.setText(_combo2Value);
        autoCompleteTextView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View arg0) {
                autoCompleteTextView2.showDropDown();
            }
        });
        autoCompleteTextView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedCombo2Entry = combo2ArrayAdapter.getItem(position);
            }
        });
        autoCompleteTextView2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedCombo2Entry = null;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        spinnerParams.setMargins(15, 25, 15, 15);
        autoCompleteTextView2.setLayoutParams(spinnerParams);
        autoCompleteTextView2.setTextColor(Compat.getColor(context, R.color.formcolor));
    }

    @NonNull
    private void createCombo1(Context context, String _combo1Value, final LinkedHashMap<String, List<String>> dataMap) {
        autoCompleteTextView1 = new AutoCompleteTextView(context);
        Set<String> titlesSet = dataMap.keySet();
        ArrayList<String> combo1Items = new ArrayList<>(titlesSet);
        combo1Items.add(0, "");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_dropdown_item_1line, combo1Items);
        autoCompleteTextView1.setAdapter(arrayAdapter);
        if (_combo1Value != null)
            autoCompleteTextView1.setText(_combo1Value);
        autoCompleteTextView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View arg0) {
                autoCompleteTextView1.showDropDown();
            }
        });
        autoCompleteTextView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedCombo1Entry = arrayAdapter.getItem(position);

                List<String> valuesList = new ArrayList<>();
                if (selectedCombo1Entry.length() != 0) {
                    valuesList = dataMap.get(selectedCombo1Entry);
                }
                combo2ArrayAdapter = new ArrayAdapter<>(parent.getContext(), android.R.layout.simple_spinner_dropdown_item, valuesList);
//                combo2ArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                autoCompleteTextView2.setAdapter(combo2ArrayAdapter);
                autoCompleteTextView2.setText("");
            }
        });
        autoCompleteTextView1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedCombo1Entry = null;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        spinnerParams.setMargins(15, 25, 15, 15);
        autoCompleteTextView1.setLayoutParams(spinnerParams);
        autoCompleteTextView1.setTextColor(Compat.getColor(context, R.color.formcolor));
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
        String result = selectedCombo1Entry + SEP + selectedCombo2Entry;
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
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // ignore
    }

}