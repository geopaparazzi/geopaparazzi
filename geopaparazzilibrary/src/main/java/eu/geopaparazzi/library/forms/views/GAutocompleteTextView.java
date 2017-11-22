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

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Compat;

import static eu.geopaparazzi.library.forms.FormUtilities.COLON;
import static eu.geopaparazzi.library.forms.FormUtilities.UNDERSCORE;

/**
 * A custom {@link Spinner} view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GAutocompleteTextView extends View implements GView {

    private AutoCompleteTextView autoCompleteTextView;
    private String selectedComboEntry;

    /**
     * @param context  the context to use.
     * @param attrs    attributes.
     * @param defStyle def style.
     */
    public GAutocompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context the context to use.
     * @param attrs   attributes.
     */
    public GAutocompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context               the context to use.
     * @param attrs                 attributes.
     * @param parentView            parent
     * @param label                 label
     * @param value                 value
     * @param itemsArray            the items.
     * @param constraintDescription constraints
     */
    public GAutocompleteTextView(Context context, AttributeSet attrs, LinearLayout parentView, String label, String value,
                                 String[] itemsArray, String constraintDescription) {
        super(context, attrs);

        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        parentView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(label.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(Compat.getColor(context, R.color.formcolor));
        textLayout.addView(textView);

        autoCompleteTextView = new AutoCompleteTextView(context);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_dropdown_item_1line, itemsArray);
        autoCompleteTextView.setAdapter(arrayAdapter);
        if (value != null) {
            autoCompleteTextView.setText(value);
            selectedComboEntry = value;
        }
        autoCompleteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View arg0) {
                autoCompleteTextView.showDropDown();
            }
        });
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedComboEntry = arrayAdapter.getItem(position);
            }
        });
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedComboEntry = null;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        autoCompleteTextView.setTextColor(Compat.getColor(context, R.color.formcolor));
//        autoCompleteTextView = new Spinner(context);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        spinnerParams.setMargins(15, 25, 15, 15);
        autoCompleteTextView.setLayoutParams(spinnerParams);

//        autoCompleteTextView.setPopupBackgroundDrawable(Compat.getDrawable(context, R.drawable.thin_background_frame));
//        autoCompleteTextView.setBackground(Compat.getDrawable(context, R.drawable.thin_background_frame));
//        int minHeight = 48;
//        if (context instanceof Activity) {
//            Activity activity = (Activity) context;
//            android.util.TypedValue tv = new android.util.TypedValue();
//            activity.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, tv, true);
//            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
//            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
//            float ret = tv.getDimension(metrics);
//
//            minHeight = (int) (ret - 1 * metrics.density);
//        }
//        autoCompleteTextView.setMinimumHeight(minHeight);
//
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, itemsArray);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        autoCompleteTextView.setAdapter(adapter);
//        if (value != null) {
//            for (int i = 0; i < itemsArray.length; i++) {
//                if (itemsArray[i].equals(value.trim())) {
//                    autoCompleteTextView.setSelection(i);
//                    break;
//                }
//            }
//        }

        textLayout.addView(autoCompleteTextView);
    }

    public String getValue() {
        if (selectedComboEntry == null) return "";
        return selectedComboEntry;
    }

    @Override
    public void setOnActivityResult(Intent data) {
        // ignore
    }

    @Override
    public void refresh(Context context) {
        // TODO Auto-generated method stub

    }

}