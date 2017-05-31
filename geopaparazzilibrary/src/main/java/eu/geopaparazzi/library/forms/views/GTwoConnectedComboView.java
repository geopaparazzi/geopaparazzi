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

import static eu.geopaparazzi.library.forms.FormUtilities.COLON;
import static eu.geopaparazzi.library.forms.FormUtilities.SEP;
import static eu.geopaparazzi.library.forms.FormUtilities.UNDERSCORE;

/**
 * A view that presents 2 connected {@link Spinner}s.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GTwoConnectedComboView extends View implements GView, OnItemSelectedListener {

    private Spinner combo1Spinner;
    private Spinner combo2Spinner;
    private LinkedHashMap<String, List<String>> dataMap;

    /**
     * @param context  the context to use.
     * @param attrs    attributes.
     * @param defStyle def style.
     */
    public GTwoConnectedComboView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context the context to use.
     * @param attrs   attributes.
     */
    public GTwoConnectedComboView(Context context, AttributeSet attrs) {
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
    public GTwoConnectedComboView(Context context, AttributeSet attrs, LinearLayout parentView, String label, String value,
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

        combo1Spinner = new Spinner(context);
        LinearLayout.LayoutParams titleSpinnerParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleSpinnerParams.setMargins(15, 25, 15, 15);
        combo1Spinner.setLayoutParams(titleSpinnerParams);
        Set<String> titlesSet = dataMap.keySet();
        ArrayList<String> combo1Items = new ArrayList<>(titlesSet);
        combo1Items.add(0, "");
        ArrayAdapter<String> titleListAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, combo1Items);
        titleListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        combo1Spinner.setAdapter(titleListAdapter);
        combo1Spinner.setPopupBackgroundDrawable(Compat.getDrawable(context, R.drawable.thin_background_frame));
        combo1Spinner.setBackground(Compat.getDrawable(context, R.drawable.thin_background_frame));
        int minHeight = getMinComboHeight(context);
        combo1Spinner.setMinimumHeight(minHeight);

        combo2Spinner = new Spinner(context);
        LinearLayout.LayoutParams valueSpinnerParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        valueSpinnerParams.setMargins(15, 25, 15, 15);
        combo2Spinner.setLayoutParams(valueSpinnerParams);
        combo2Spinner.setPopupBackgroundDrawable(Compat.getDrawable(context, R.drawable.thin_background_frame));
        combo2Spinner.setBackground(Compat.getDrawable(context, R.drawable.thin_background_frame));
        combo2Spinner.setMinimumHeight(minHeight);

        List<String> combo2ItemsList = new ArrayList<>();
        if (_combo1Value.length() > 0) {
            combo2ItemsList = dataMap.get(_combo1Value);
            int indexOf = combo1Items.indexOf(_combo1Value.trim());
            if (indexOf != -1) {
                combo1Spinner.setSelection(indexOf, false);
            }
        }
        ArrayAdapter<String> combo2ListAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, combo2ItemsList);
        combo2ListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        combo2Spinner.setAdapter(combo2ListAdapter);

        combosLayout.addView(combo1Spinner);
        combosLayout.addView(combo2Spinner);

        if (_combo2Value.length() > 0) {
            int position = combo2ListAdapter.getPosition(_combo2Value);
            combo2Spinner.setSelection(position, false);
        }

        combo1Spinner.setOnItemSelectedListener(this);
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
        Object selected1Item = combo1Spinner.getSelectedItem();
        if (selected1Item == null) selected1Item = "";
        Object selected2Item = combo2Spinner.getSelectedItem();
        if (selected2Item == null) selected2Item = "";
        String result = selected1Item.toString() + SEP + selected2Item.toString();
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
        if (parent == combo1Spinner) {
            String combo1Item = combo1Spinner.getSelectedItem().toString();
            List<String> valuesList = new ArrayList<>();
            if (combo1Item.length() != 0) {
                valuesList = dataMap.get(combo1Item);
            }
            ArrayAdapter<String> valuesListAdapter = new ArrayAdapter<>(parent.getContext(), android.R.layout.simple_spinner_dropdown_item, valuesList);
            valuesListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            combo2Spinner.setAdapter(valuesListAdapter);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // ignore
    }

}