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
    RED("#D32F2F"), //
    PINK("#C2185B"), //
    PURPLE("#7B1FA2"), //
    deep_purple("#512da8"), //
    indigo("#303f9f"), //
    blue("#1976d2"), //
    light_blue("#0288d1"), //
    cyan("#0097a7"), //
    teal("#00796b"), //
    green("#00796b"), //
    light_green("#689f38"), //
    lime("#afb42b"), //
    yellow("#fbc02d"), //
    amber("#ffa000"), //
    orange("#f57c00"), //
    deep_orange("#e64a19"), //
    brown("#5d4037"), //
    grey("#616161"), //
    blue_grey("#455a64"), //
    white("#ffffff"), //
    almost_black("#212121")//
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
