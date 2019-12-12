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
package eu.geopaparazzi.library.style;

import android.graphics.DashPathEffect;

import java.util.Arrays;
import java.util.HashMap;

import eu.geopaparazzi.library.database.GPLog;

/**
 * Simple style for shapes.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class Style {
    /**
     *
     */
    public long id;
    /**
     *
     */
    public String name;
    /**
     *
     */
    public float size = 5;
    /**
     *
     */
    public String fillcolor = "red";
    /**
     *
     */
    public String strokecolor = "black";
    /**
     *
     */
    public float fillalpha = 0.3f;
    /**
     *
     */
    public float strokealpha = 1.0f;
    /**
     * WKT shape name.
     */
    public String shape = "square";
    /**
     * The stroke width.
     */
    public float width = 3f;
    /**
     * The text size.
     */
    public float labelsize = 5f;

    /**
     * Field to use for labeling.
     */
    public String labelfield = "";
    /**
     * Defines if the labeling is enabled.
     * <p/>
     * <ul>
     * <li>0 = false</li>
     * <li>1 = true</li>
     * </ul>
     */
    public int labelvisible = 0;

    /**
     * Defines if the layer is enabled.
     * <p/>
     * <ul>
     * <li>0 = false</li>
     * <li>1 = true</li>
     * </ul>
     */
    public int enabled = 0;
    /**
     * Vertical order of the layer.
     */
    public int order = 0;
    /**
     * The pattern to dash lines.
     * <p/>
     * <p>The format is an array of floats, the first number being the shift.
     */
    public String dashPattern = "";
    /**
     * Min possible zoom level.
     */
    public int minZoom = 0;
    /**
     * Max possible zoom level.
     */
    public int maxZoom = 22;
    /**
     * Decimation factor for geometries.
     */
    public float decimationFactor = 0.0f;

    /**
     * If a unique style is defined, the hashmap contains in key the unique value
     * and in value the style to apply.
     */
    public HashMap<String, Style> themeMap;

    public String themeField;

    /**
     * @return a string that can be used in a sql insert statement with
     * all the values placed.
     */
    public String insertValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'");
        sb.append(name);
        sb.append("', ");
        sb.append(size);
        sb.append(", '");
        sb.append(fillcolor);
        sb.append("', '");
        sb.append(strokecolor);
        sb.append("', ");
        sb.append(fillalpha);
        sb.append(", ");
        sb.append(strokealpha);
        sb.append(", '");
        sb.append(shape);
        sb.append("', ");
        sb.append(width);
        sb.append(", ");
        sb.append(labelsize);
        sb.append(", '");
        sb.append(labelfield);
        sb.append("', ");
        sb.append(labelvisible);
        sb.append(", ");
        sb.append(enabled);
        sb.append(", ");
        sb.append(order);
        sb.append(", '");
        sb.append(dashPattern);
        sb.append("', ");
        sb.append(minZoom);
        sb.append(", ");
        sb.append(maxZoom);
        sb.append(", ");
        sb.append(decimationFactor);
        return sb.toString();
    }

    /**
     * Convert string to dash.
     *
     * @param dashPattern the string to convert.
     * @return the dash array or null, if conversion failed.
     */
    public static float[] dashFromString(String dashPattern) {
        if (dashPattern.trim().length() > 0) {
            String[] split = dashPattern.split(",");
            if (split.length > 1) {
                float[] dash = new float[split.length];
                for (int i = 0; i < split.length; i++) {
                    try {
                        float tmpDash = Float.parseFloat(split[i].trim());
                        dash[i] = tmpDash;
                    } catch (NumberFormatException e) {
                        GPLog.error("Style", "Can't convert to dash pattern: " + dashPattern, e);
                        return null;
                    }
                }
                return dash;
            }
        }
        return null;
    }

    /**
     * Convert a dash array to string.
     *
     * @param dash  the dash to convert.
     * @param shift the shift.
     * @return the string representation.
     */
    public static String dashToString(float[] dash, Float shift) {
        StringBuilder sb = new StringBuilder();
        if (shift != null)
            sb.append(shift);
        for (int i = 0; i < dash.length; i++) {
            if (shift != null || i > 0) {
                sb.append(",");
            }
            sb.append((int) dash[i]);
        }
        return sb.toString();
    }

    public static float[] getDashOnly(float[] shiftAndDash) {
        return Arrays.copyOfRange(shiftAndDash, 1, shiftAndDash.length);
    }

    public static float getDashShift(float[] shiftAndDash) {
        return shiftAndDash[0];
    }
}
