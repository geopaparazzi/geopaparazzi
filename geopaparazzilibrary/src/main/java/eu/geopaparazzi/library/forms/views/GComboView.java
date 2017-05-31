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

import static eu.geopaparazzi.library.forms.FormUtilities.COLON;
import static eu.geopaparazzi.library.forms.FormUtilities.UNDERSCORE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Compat;

/**
 * A custom {@link Spinner} view.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GComboView extends View implements GView {

    private Spinner spinner;

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param defStyle def style.
     */
    public GComboView( Context context, AttributeSet attrs, int defStyle ) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     */
    public GComboView( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param parentView parent
     * @param label label
     * @param value value
     * @param itemsArray the items.
     * @param constraintDescription constraints
     */
    public GComboView( Context context, AttributeSet attrs, LinearLayout parentView, String label, String value,
            String[] itemsArray, String constraintDescription ) {
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

        spinner = new Spinner(context);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        spinnerParams.setMargins(15,25, 15, 15);
        spinner.setLayoutParams(spinnerParams);
        spinner.setPopupBackgroundDrawable(Compat.getDrawable(context, R.drawable.thin_background_frame));
        spinner.setBackground(Compat.getDrawable(context, R.drawable.thin_background_frame));
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
        spinner.setMinimumHeight(minHeight);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, itemsArray);
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
    }

    public String getValue() {
        return spinner.getSelectedItem().toString();
    }

    @Override
    public void setOnActivityResult( Intent data ) {
        // ignore
    }

    @Override
    public void refresh( Context context ) {
        // TODO Auto-generated method stub

    }

}