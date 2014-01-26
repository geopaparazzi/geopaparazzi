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
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.MultipleChoiceDialog;

/**
 * A custom {@link Spinner} view.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GMultiComboView extends View implements GView {

    private Button button;

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param defStyle def style.
     */
    public GMultiComboView( Context context, AttributeSet attrs, int defStyle ) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     */
    public GMultiComboView( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param parentView parent
     * @param key key
     * @param value value
     * @param itemsArray the items.
     * @param constraintDescription constraints
     */
    public GMultiComboView( final Context context, AttributeSet attrs, LinearLayout parentView, String key, String value,
            final String[] itemsArray, String constraintDescription ) {
        super(context, attrs);

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

        if (value == null || value.length() == 0) {
            button.setText("...");
        } else {
            button.setText(value);
        }
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                MultipleChoiceDialog dialog = new MultipleChoiceDialog();
                dialog.open(context, button, itemsArray);
            }
        });

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
        // TODO Auto-generated method stub

    }

}