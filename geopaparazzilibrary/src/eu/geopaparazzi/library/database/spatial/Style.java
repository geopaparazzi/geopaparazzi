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
package eu.geopaparazzi.library.database.spatial;

/**
 * Simple style for shapes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Style {
    public String name;
    public float size = 5;
    public String fillcolor = "red";
    public String strokecolor = "black";
    public float fillalpha = 0.3f;
    public float strokealpha = 1.0f;
    public String shape = "square";
    public float width = 3f;
    public float textsize = 5f;
    public String textfield = "";
    public int enabled = 0;
    public int order = 0;

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
        sb.append(textsize);
        sb.append(", '");
        sb.append(textfield);
        sb.append("', ");
        sb.append(enabled);
        sb.append(", ");
        sb.append(order);
        return sb.toString();
    }

}
