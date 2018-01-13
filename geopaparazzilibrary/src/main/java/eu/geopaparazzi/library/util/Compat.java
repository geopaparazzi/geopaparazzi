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

package eu.geopaparazzi.library.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.content.res.AppCompatResources;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import eu.geopaparazzi.library.R;

/**
 * Compatibility helper methods.
 *
 * @author Andrea Antonello
 */
public class Compat {

    public static Drawable getDrawable(Context context, int id) {
        Drawable drawable = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawable = context.getDrawable(id);
        } else {
            drawable = AppCompatResources.getDrawable(context, id);
//            drawable = context.getResources().getDrawable(id);
        }
        return drawable;
    }

    public static int getColor(Context context, int id) {
        int color = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            color = context.getColor(id);
        } else {
            color = context.getResources().getColor(id);
        }
        return color;
    }

    public static void setTextAppearance(Context context, TextView textView, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextAppearance(type);
        } else {
            textView.setTextAppearance(context, type);
        }
    }

    public static void setButtonTextAppearance(Context context, Button button, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            button.setTextAppearance(type);
        } else {
            button.setTextAppearance(context, type);
        }
    }
}
