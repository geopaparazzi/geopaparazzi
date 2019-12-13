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

import android.graphics.Color;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a label of a feature.
 *
 * @author Andrea Antonello
 */
public class LabelObject implements Serializable {

    public String tableName;
    public String dbPath;
    public boolean hasLabel = false;
    public int labelColor = Color.BLACK;
    public int labelSize = 30;
    public String label;
    public List<String> labelFieldsList = new ArrayList<>();

    public LabelObject duplicate() {
        LabelObject dup = new LabelObject();
        dup.tableName = tableName;
        dup.dbPath = dbPath;
        dup.hasLabel = hasLabel;
        dup.labelColor = labelColor;
        dup.labelSize = labelSize;
        dup.label = label;
        dup.labelFieldsList = new ArrayList<>(labelFieldsList);
        return dup;
    }

}
