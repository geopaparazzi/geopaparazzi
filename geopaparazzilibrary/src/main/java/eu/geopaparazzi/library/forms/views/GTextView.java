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
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Compat;

/**
 * A custom {@link TextView} view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GTextView extends View implements GView {

    private TextView textView;

    /**
     * @param context  the context to use.
     * @param attrs    attributes.
     * @param defStyle def style.
     */
    public GTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context the context to use.
     * @param attrs   attributes.
     */
    public GTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context  the context to use.
     * @param attrs    attributes.
     * @param mainView parent
     * @param value    value
     * @param size     size
     * @param withLine with line.
     * @param url      url support.
     */
    public GTextView(final Context context, AttributeSet attrs, LinearLayout mainView, String value, String size,
                     boolean withLine, final String url) {
        super(context, attrs);

        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        mainView.addView(textLayout);

        textView = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(15, 25, 15, 15);
        textView.setLayoutParams(params);
        textView.setPadding(2, 2, 2, 2);
        textView.setText(value);

        size = size.trim();
        if (size.equals("large")) {
            Compat.setTextAppearance(context, textView, android.R.attr.textAppearanceLarge);
        } else if (size.equals("medium")) {
            Compat.setTextAppearance(context, textView, android.R.attr.textAppearanceMedium);
        } else if (size.equals("small")) {
            Compat.setTextAppearance(context, textView, android.R.attr.textAppearanceSmall);
        } else {
            int sizeInt = Integer.parseInt(size);
            textView.setTextSize(sizeInt);
        }
        textView.setTextColor(Compat.getColor(context, R.color.formcolor));
        if (url != null && url.length() > 0) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    context.startActivity(intent);
                }
            });
        }
        textLayout.addView(textView);

        if (withLine) {
            View view = new View(context);
            view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 2));
            view.setBackgroundColor(Compat.getColor(context, R.color.formcolor));

            textLayout.addView(view);
        }

    }

    public String getValue() {
        return textView.getText().toString();
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