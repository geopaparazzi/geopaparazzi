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

package eu.geopaparazzi.map.layers.utils;

import android.graphics.Color;

import java.io.Serializable;

/**
 * Class representing color and stroke and shape.
 *
 * @author Andrea Antonello
 */
public class ColorStrokeObject implements Serializable {

    public String tableName;
    public String dbPath;

    public boolean hasFill = false;
    public int fillColor = Color.WHITE;
    public int fillAlpha = 255;

    public boolean hasStroke = false;
    public int strokeColor = Color.BLACK;
    public int strokeAlpha = 255;

    public boolean hasStrokeWidth = false;
    public int strokeWidth = 8;

    public boolean hasShape = false;
    public int shapeSize = 50;
    public String shapeWKT = "circle"; //NON-NLS

    public ColorStrokeObject duplicate() {
        ColorStrokeObject dup = new ColorStrokeObject();
        dup.tableName = tableName;
        dup.dbPath = dbPath;

        dup.hasFill = hasFill;
        dup.fillColor = fillColor;
        dup.fillAlpha = fillAlpha;

        dup.hasStroke = hasStroke;
        dup.strokeColor = strokeColor;
        dup.strokeAlpha = strokeAlpha;

        dup.hasStrokeWidth = hasStrokeWidth;
        dup.strokeWidth = strokeWidth;

        dup.hasShape = hasShape;
        dup.shapeSize = shapeSize;
        dup.shapeWKT = shapeWKT;

        return dup;
    }

}
