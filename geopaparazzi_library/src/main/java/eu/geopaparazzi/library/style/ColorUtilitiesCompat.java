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

package eu.geopaparazzi.library.style;

/**
 * Th old colors for backwards compat.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("ALL")
public enum ColorUtilitiesCompat {
    black("#000000"), //
    blue("#0000ff"), //
    cyan("#00ffff"), //
    darkgray("#444444"), //
    gray("#888888"), //
    green("#00ff00"), //
    lightgray("#cccccc"), //
    magenta("#ff00ff"), //
    red("#ff0000"), //
    white("#ffffff"), //
    yellow("#ffff00"), //
    purple("#800080"), //
    violet("#ee82ee"), //
    turquoise("#40e0d0"), //
    plum("#dda0dd"), //
    tomato("#ff6347"), //
    salmon("#fa8072"), //
    ; //

    private String hex;

    ColorUtilitiesCompat(String hex) {
        this.hex = hex;
    }

    public static String getHex(String name) {
        for (ColorUtilitiesCompat color : values()) {
            if (color.name().equalsIgnoreCase(name)) {
                return color.hex;
            }
        }
        return null;
    }

}
