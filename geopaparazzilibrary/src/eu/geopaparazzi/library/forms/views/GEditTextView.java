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
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.forms.FormUtilities;

/**
 * A custom {@link EditText} view.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GEditTextView extends View implements GView {

    private EditText editView;
    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param defStyle def style.
     */
    public GEditTextView( Context context, AttributeSet attrs, int defStyle ) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     */
    public GEditTextView( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param parentView parent
     * @param key key
     * @param value value
     * @param type the text type.
     * @param lines the lines num.
     * @param constraintDescription constraints
     * @param readonly if <code>false</code>, the item is disabled for editing.
     */
    public GEditTextView( Context context, AttributeSet attrs, LinearLayout parentView, String key, String value, int type,
            int lines, String constraintDescription, boolean readonly ) {
        super(context, attrs);

        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        // textLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.formitem_background));
        parentView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(context.getResources().getColor(R.color.formcolor));

        textLayout.addView(textView);

        editView = new EditText(context);
        editView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        editView.setPadding(15, 5, 15, 5);
        editView.setText(value);
        editView.setEnabled(!readonly);

        switch( type ) {
        case 1:
            editView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            break;
        case 2:
            editView.setInputType(InputType.TYPE_CLASS_PHONE);
            break;
        case 3:
            editView.setInputType(InputType.TYPE_CLASS_DATETIME);
            break;
        case 4:
            editView.setInputType(InputType.TYPE_CLASS_NUMBER);
            break;
        default:
            break;
        }

        if (lines > 0) {
            editView.setLines(lines);
            editView.setGravity(Gravity.TOP);
        }

        textLayout.addView(editView);
    }

    public String getValue() {
        if (editView != null) {
            String text = editView.getText().toString();
            text = FormUtilities.makeTextJsonSafe(text);
            return text;
        } else {
            return null;
        }
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