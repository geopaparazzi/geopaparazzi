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
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.forms.FormUtilities;
import eu.geopaparazzi.library.util.Compat;

import static eu.geopaparazzi.library.forms.FormUtilities.COLON;
import static eu.geopaparazzi.library.forms.FormUtilities.UNDERSCORE;

/**
 * A custom view with possibility to add {@link EditText}s dynamically.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GDynamicEditTextView extends View implements GView {

    private Button addTextButton;
    private List<EditText> editViewList;
    private LinearLayout mainLayout;

    /**
     * @param context  the context to use.
     * @param attrs    attributes.
     * @param defStyle def style.
     */
    public GDynamicEditTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context the context to use.
     * @param attrs   attributes.
     */
    public GDynamicEditTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context               the context to use.
     * @param attrs                 attributes.
     * @param parentView            parent
     * @param label                 label
     * @param value                 value
     * @param type                  the text type.
     * @param constraintDescription constraints
     * @param readonly              if <code>false</code>, the item is disabled for editing.
     */
    public GDynamicEditTextView(final Context context, AttributeSet attrs, LinearLayout parentView, String label, String value, final int type,
                                String constraintDescription, final boolean readonly) {
        super(context, attrs);

        editViewList = new ArrayList<>();

        mainLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        mainLayout.setLayoutParams(layoutParams);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        parentView.addView(mainLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(label.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(Compat.getColor(context, R.color.formcolor));

        mainLayout.addView(textView);

        String[] valuesSplit = value.trim().split(";");
        if (valuesSplit.length == 0) {
            valuesSplit = new String[]{" "};
        }

        for (String singleValue : valuesSplit) {
            addSingleEditText(context, mainLayout, singleValue.trim(), readonly, type);
        }

        float minTouch = context.getResources().getDimension(R.dimen.min_touch_size);
        addTextButton = new Button(context);
        addTextButton.setLayoutParams(new LinearLayout.LayoutParams((int) minTouch, (int) minTouch));
        addTextButton.setPadding(5, 5, 5, 5);
//        addTextButton.setText("+");
        addTextButton.setBackground(Compat.getDrawable(context, R.drawable.ic_add_primary_24dp));
        mainLayout.addView(addTextButton);

        addTextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mainLayout.removeView(addTextButton);

                addSingleEditText(context, mainLayout, "", readonly, type);

                // add the button back
                mainLayout.addView(addTextButton);
            }

        });


    }

    private void addSingleEditText(Context context, LinearLayout mainLayout, String singleValue, boolean readonly, int type) {
        EditText editView = new EditText(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(15, 25, 15, 15);
        editView.setLayoutParams(params);
        editView.setText(singleValue);
        editView.setEnabled(!readonly);

        switch (type) {
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

        mainLayout.addView(editView);
        editViewList.add(editView);
    }

    public String getValue() {
        if (editViewList != null && editViewList.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (EditText editText : editViewList) {
                String text = editText.getText().toString();
                if (text.trim().length() == 0) continue;
                text = FormUtilities.makeTextJsonSafe(text);
                sb.append(";").append(text);
            }
            if (sb.length() == 0)
                return "";
            return sb.substring(1);
        } else {
            return null;
        }
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