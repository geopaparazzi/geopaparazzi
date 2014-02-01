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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.forms.FormTimePickerFragment;
import eu.geopaparazzi.library.util.TimeUtilities;

/**
 * A custom time view.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GTimeView extends View implements GView {

    private Button button;

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param defStyle def style.
     */
    public GTimeView( Context context, AttributeSet attrs, int defStyle ) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     */
    public GTimeView( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    /**
     * @param fragment the fragment.
     * @param attrs attributes.
     * @param parentView parent
     * @param key key
     * @param value value
     * @param constraintDescription constraints
     * @param readonly if <code>false</code>, the item is disabled for editing.
     */
    public GTimeView( final Fragment fragment, AttributeSet attrs, LinearLayout parentView, String key, String value,
            String constraintDescription, boolean readonly ) {
        super(fragment.getActivity(), attrs);

        Context context = fragment.getActivity();

        LinearLayout textLayout = new LinearLayout(context);
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

        button = new Button(context);
        button.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        button.setPadding(15, 5, 15, 5);

        final SimpleDateFormat timeFormatter = TimeUtilities.INSTANCE.TIMEONLY_FORMATTER;
        if (value == null || value.length() == 0) {
            String dateStr = timeFormatter.format(new Date());
            button.setText(dateStr);
        } else {
            button.setText(value);
        }
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                String dateStr = button.getText().toString();
                Date date = null;
                try {
                    date = timeFormatter.parse(dateStr);
                } catch (ParseException e) {
                    // fallback on current date
                    date = new Date();
                }
                final Calendar c = Calendar.getInstance();
                c.setTime(date);
                int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);

                FormTimePickerFragment newFragment = new FormTimePickerFragment();
                newFragment.setAttributes(hourOfDay, minute, true, button);
                newFragment.show(fragment.getFragmentManager(), "timePicker");
            }
        });
        button.setEnabled(!readonly);

        textLayout.addView(button);
    }

    public String getValue() {
        String text = button.getText().toString();
        if (text.trim().equals("...")) {
            text = "";
        }
        return text;
    }

    @Override
    public void setOnActivityResult( Intent data ) {
        // ignore
    }

    @Override
    public void refresh( Context context ) {
        // ignore
    }

}