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

package eu.geopaparazzi.core.ui.activities.mapsforgeextractor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A helper class for mapsforge extracted notes.
 *
 * @author Andrea Antonello
 */
public class MapsforgeExtractedFormHelper {

    private String pre = "{\n" +//NON-NLS
            "    \"sectionname\": \"mapsforge note\",\n" +//NON-NLS
            "    \"sectiondescription\": \"a note generated from mapsforge extraction\",\n" +//NON-NLS
            "    \"forms\": [\n" +//NON-NLS
            "        {\n" +//NON-NLS
            "            \"formname\": \"osm poi\",\n" +//NON-NLS
            "            \"formitems\": [\n";//NON-NLS

    private String post = "            ]\n" +//NON-NLS
            "        }\n" +//NON-NLS
            "    ]\n" +//NON-NLS
            "}\n";//NON-NLS

    private String firstKey = null;
    private String labelKey = null;
    private String firstValue = null;
    private String labelValue = null;

    private LinkedHashMap<String, String> valuesMap = new LinkedHashMap<>();

    public void addTag(String key, String value) {
        if (firstKey == null) {
            firstKey = key;
            firstValue = value;
        }
        if (key.equals("name")) {//NON-NLS
            labelKey = key;
            labelValue = value;
        }

        valuesMap.put(key, value);
    }

    public String toForm() {
        StringBuilder sb = new StringBuilder();

        sb.append(pre);

        checkLabelValue();

        int size = valuesMap.size();

        int count = 0;
        for (Map.Entry<String, String> entry : valuesMap.entrySet()) {
            String key = entry.getKey();
            String item = "{\n" +
                    "                    \"key\": \"" + key + "\",\n" +//NON-NLS
                    "                    \"value\": \"" + entry.getValue() + "\",\n";//NON-NLS
            if (key.equals(labelKey)) {
                item = item + "                    \"islabel\": \"true\",\n";//NON-NLS
            }
            item = item +
                    "                    \"type\": \"string\",\n" +//NON-NLS
                    "                    \"mandatory\": \"no\"\n" +//NON-NLS
                    "                }\n";
            if (count < size - 1) {
                item = item + "                ,\n";
            }
            count++;
            sb.append(item);
        }

        sb.append(post);
        return sb.toString();
    }

    private void checkLabelValue() {
        if (labelKey == null) {
            labelKey = firstKey;
            labelValue = firstValue;
        }
    }

    public String getLabelValue() {
        checkLabelValue();
        return labelValue + " (MF)";//NON-NLS
    }
}
