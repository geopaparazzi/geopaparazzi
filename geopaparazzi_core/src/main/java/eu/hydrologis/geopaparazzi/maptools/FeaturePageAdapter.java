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
package eu.hydrologis.geopaparazzi.maptools;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.core.dialogs.DatePickerDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.types.EDataType;

/**
 * Page adapter for features.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeaturePageAdapter extends PagerAdapter {
    private Context context;
    private List<Feature> featuresList;
    private boolean isReadOnly;
    private FragmentManager fragmentManager;

    private DecimalFormat areaLengthFormatter = new DecimalFormat("0.00");

    /**
     * Constructor.
     *
     * @param context      the {@link Context} to use.
     * @param featuresList the list of features to show.
     * @param isReadOnly   if <code>true</code>, the adapter will be initially readonly.
     */
    public FeaturePageAdapter(Context context, List<Feature> featuresList, boolean isReadOnly, FragmentManager manager) {
        super();
        this.context = context;
        this.featuresList = featuresList;
        this.isReadOnly = isReadOnly;
        this.fragmentManager = manager;
    }

    /**
     * Mark the adapter as readonly.
     *
     * @param isReadOnly if <code>true</code>, then editing will be disabled.
     */
    public void setReadOnly(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    @Override
    public int getCount() {
        return featuresList.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final Feature feature = featuresList.get(position);

        int bgColor = Compat.getColor(context, eu.geopaparazzi.library.R.color.formbgcolor);
        int textColor = Compat.getColor(context, eu.geopaparazzi.library.R.color.formcolor);

        ScrollView scrollView = new ScrollView(context);
        ScrollView.LayoutParams scrollLayoutParams = new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        scrollLayoutParams.setMargins(10, 10, 10, 10);
        scrollView.setLayoutParams(scrollLayoutParams);
        scrollView.setBackgroundColor(bgColor);

        LinearLayout linearLayoutView = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        int margin = 10;
        layoutParams.setMargins(margin, margin, margin, margin);
        linearLayoutView.setLayoutParams(layoutParams);
        linearLayoutView.setOrientation(LinearLayout.VERTICAL);
        int padding = 10;
        linearLayoutView.setPadding(padding, padding, padding, padding);
        scrollView.addView(linearLayoutView);

        List<String> attributeNames = feature.getAttributeNames();
        List<String> attributeValues = feature.getAttributeValuesStrings();
        List<String> attributeTypes = feature.getAttributeTypes();
        for (int i = 0; i < attributeNames.size(); i++) {
            final String name = attributeNames.get(i);
            String value = attributeValues.get(i);
            String typeString = attributeTypes.get(i);
            EDataType type = EDataType.getType4Name(typeString);

            TextView textView = new TextView(context);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            textView.setPadding(padding, padding, padding, padding);
            textView.setText(name);
            textView.setTextColor(textColor);
            // textView.setTextAppearance(context, android.R.style.TextAppearance_Medium);

            linearLayoutView.addView(textView);

            final TextView editView = getEditView(feature, name, type, value);
            LinearLayout.LayoutParams editViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            editViewParams.setMargins(margin, 0, margin, 0);
            editView.setLayoutParams(editViewParams);
            editView.setPadding(padding * 2, padding, padding * 2, padding);
            editView.setFocusable(!isReadOnly);

            if (isReadOnly) {
                editView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        String text = editView.getText().toString();
                        FeatureUtilities.viewIfApplicable(v.getContext(), text);
                        return false;
                    }
                });
            }
            linearLayoutView.addView(editView);
        }

        /*
         * add also area and length
         */
        if (feature.getOriginalArea() > -1) {
            TextView areaTextView = new TextView(context);
            areaTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            areaTextView.setPadding(padding, padding, padding, padding);
            areaTextView.setText(context.getString(eu.hydrologis.geopaparazzi.R.string.area_colon) + areaLengthFormatter.format(feature.getOriginalArea()));
            areaTextView.setTextColor(textColor);
            TextView lengthTextView = new TextView(context);
            lengthTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            lengthTextView.setPadding(padding, padding, padding, padding);
            lengthTextView.setText(context.getString(eu.hydrologis.geopaparazzi.R.string.length_colon) + areaLengthFormatter.format(feature.getOriginalLength()));
            lengthTextView.setTextColor(textColor);
            linearLayoutView.addView(areaTextView);
            linearLayoutView.addView(lengthTextView);
        }
        container.addView(scrollView);

        return scrollView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    private TextView getEditView(final Feature feature, final String fieldName, EDataType type, String value) {
        final TextView editView;
        switch (type) {
            case DATE:
                editView = new TextView(context);
                editView.setInputType(InputType.TYPE_CLASS_DATETIME);
                editView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FeaturePageAdapter.this.openDatePicker((EditText) view);
                    }
                });
                if (value == null || value.equals("")) {
                    value = "____-__-__";
                }
                break;
            default:
                editView = new EditText(context);
                break;
        }
        editView.setText(value);

        switch (type) {
            case DOUBLE:
            case FLOAT:
                editView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
            case PHONE:
                editView.setInputType(InputType.TYPE_CLASS_PHONE);
                break;
            case DATE:
                editView.setInputType(InputType.TYPE_CLASS_DATETIME);
                editView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FeaturePageAdapter.this.openDatePicker((TextView) view);
                    }
                });
                break;
            case INTEGER:
                editView.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            default:
                break;
        }
        editView.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            public void afterTextChanged(Editable s) {
                String text = editView.getText().toString();
                feature.setAttribute(fieldName, text);
            }
        });

        return editView;
    }

    private void openDatePicker(TextView editView) {
        String dateStr = editView.getText().toString();
        Date date = null;
        try {
            final SimpleDateFormat dateFormatter = TimeUtilities.INSTANCE.DATEONLY_FORMATTER;
            if (dateStr!=null && dateStr.equals("")) {
                date = dateFormatter.parse(dateStr);
            }
            else {
                date = new Date();
            }
        } catch (ParseException e) {
            GPLog.error(this, null, e);
            date = new Date();
        }
        final Calendar c = Calendar.getInstance();
        c.setTime(date);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialogFragment newFragment = new DatePickerDialogFragment(year, month, day, editView);
        newFragment.show(this.fragmentManager, "datePicker");
    }
}
