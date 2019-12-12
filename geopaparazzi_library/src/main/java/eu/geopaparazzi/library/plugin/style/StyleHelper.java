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
package eu.geopaparazzi.library.plugin.style;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Compat;

/**
 * Created by hydrologis on 10/02/17.
 */
public class StyleHelper {


    public static LinearLayout.LayoutParams styleButton(Context context, Button button) {
        button.setTextColor(Compat.getColor(context, R.color.main_text_color));
        Compat.setButtonTextAppearance(context, button, android.R.attr.textAppearanceMedium);
        button.setBackground(Compat.getDrawable(context, R.drawable.button_background_drawable));
        int pad = (int) context.getResources().getDimension(R.dimen.button_indent);
        button.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(15, 15, 15, 15);
        return lp;
    }
}
