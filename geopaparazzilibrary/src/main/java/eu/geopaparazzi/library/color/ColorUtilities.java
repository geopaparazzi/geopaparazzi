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
package eu.geopaparazzi.library.color;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.TypedValue;

import java.util.HashMap;

import eu.geopaparazzi.library.R;

/**
 * Color utils.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum ColorUtilities {
    // MATERIAL DESIGN COLORS
    red("#D32F2F"), //
    pink("#C2185B"), //
    purple("#7B1FA2"), //
    deep_purple("#512DA8"), //
    indigo("#303F9F"), //
    blue("#1976D2"), //
    light_blue("#0288D1"), //
    cyan("#0097A7"), //
    teal("#00796B"), //
    green("#00796B"), //
    light_green("#689F38"), //
    lime("#AFB42B"), //
    yellow("#FBC02D"), //
    amber("#FFA000"), //
    orange("#F57C00"), //
    deep_orange("#E64A19"), //
    brown("#5D4037"), //
    grey("#616161"), //
    blue_grey("#455A64"), //
    white("#FFFFFF"), //
    almost_black("#212121"), //

    // PREVIEWCOLOR
    selection_stroke("#ffff00"), //
    selection_fill("#ff0000"), //
    preview_stroke("#00bdbd"), //
    preview_fill("#00ffff"), //
    infoselection_stroke("#0000ff"), //
    infoselection_fill("#0000ff") //
    ; //

    private static HashMap<String, Integer> colorMap = new HashMap<String, Integer>();
    private String hex;

    ColorUtilities(String hex) {
        this.hex = hex;
    }

    public String getHex() {
        return hex;
    }

    public static String getHex(int color) {
        String hexColor = String.format("#%06X", (0xFFFFFF & color));
        return hexColor;
    }

    /**
     * Returns the corresponding color int.
     *
     * @param nameOrHex the name of the color as supported in this class, or the hex value.
     * @return the int color.
     */
    public static int toColor(String nameOrHex) {
        nameOrHex = nameOrHex.trim();
        if (nameOrHex.startsWith("#")) {
            return Color.parseColor(nameOrHex);
        }
        Integer color = colorMap.get(nameOrHex);
        if (color == null) {
            ColorUtilities[] values = values();
            for (ColorUtilities colorUtil : values) {
                if (colorUtil.name().equalsIgnoreCase(nameOrHex)) {
                    color = Color.parseColor(colorUtil.hex);
                    colorMap.put(nameOrHex, color);
                    return color;
                }
            }
        }
        if (color == null) {
            String hex = ColorUtilitiesCompat.getHex(nameOrHex);
            if (hex != null) {
                return toColor(hex);
            }
        }
        color = Color.parseColor(blue_grey.hex);
        return color;
    }


    public static int getColor(ColorUtilities colorEnum) {
        String name = colorEnum.name();
        Integer color = colorMap.get(name);
        if (color == null) {
            color = Color.parseColor(colorEnum.hex);
            colorMap.put(name, color);
        }
        return color;
    }

    /**
     * Get the current style accent color.
     *
     * @param context the context to use.
     * @return the color.
     */
    public static int getAccentColor(Context context) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorAccent});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    /**
     * Get the current style primary color.
     *
     * @param context the context to use.
     * @return the color.
     */
    public static int getPrimaryColor(Context context) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorPrimary});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }
}
