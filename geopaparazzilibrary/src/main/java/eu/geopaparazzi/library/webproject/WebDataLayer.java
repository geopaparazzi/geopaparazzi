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
package eu.geopaparazzi.library.webproject;

/**
 * The class holding webdata layer info.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WebDataLayer {

    /**
     * The machine name for the layer.
     */
    public String name;

    /**
     * The human readable name for the layer.
     */
    public String title;

    /**
     * The description of the layer
     */
    public String abstractStr;

    /**
     * The geometry type of the layer.
     */
    public String geomtype;

    /**
     * The layer's epsg code.
     */
    public int srid;

    /**
     * A string defining permissions for the user.
     * <p>
     * <p>ex. read-only, read-write</p>
     */
    public String permissions;

    /**
     * Last UTC timestamp of editing.
     */
    public Long lastEdited; // Unix long

    public boolean isSelected;


    /**
     * Checks if the project info match the supplied string.
     *
     * @param pattern the pattern to match.
     * @return <code>true</code> if the pattern matches any info.
     */
    public boolean matches(String pattern) {
        pattern = pattern.toLowerCase();
        if (name.toLowerCase().contains(pattern)) {
            return true;
        }
        if (title.toLowerCase().contains(pattern)) {
            return true;
        }
        if (abstractStr.toLowerCase().contains(pattern)) {
            return true;
        }
        if (geomtype.toLowerCase().contains(pattern)) {
            return true;
        }
        if (String.valueOf(srid).contains(pattern)) {
            return true;
        }
        return false;
    }
}
