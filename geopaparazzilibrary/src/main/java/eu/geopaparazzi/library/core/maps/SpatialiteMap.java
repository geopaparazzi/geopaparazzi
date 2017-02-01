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

package eu.geopaparazzi.library.core.maps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class representing a spatialite vector map.
 *
 * @author Andrea Antonello
 */
public class SpatialiteMap {
    public static final String SPATIALITEMAPS_PREF_KEY = "SPATIALITEMAPS_PREF_KEY";

    public static final String DATABASE_PATH = "databasePath";
    public static final String TABLE_TYPE = "tableType";
    public static final String GEOMETRY_TYPE = "geometryType";
    public static final String ISVISIBLE = "isVisible";
    public static final String ORDER = "order";
    public static final String TABLENAME = "tableName";

    public String databasePath;
    public String tableType;
    public String geometryType;
    public String tableName;
    public boolean isVisible = false;
    public double order = 0;

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(DATABASE_PATH, databasePath);
        jsonObject.put(TABLE_TYPE, tableType);
        jsonObject.put(GEOMETRY_TYPE, geometryType);
        jsonObject.put(TABLENAME, tableName);
        jsonObject.put(ISVISIBLE, isVisible);
        jsonObject.put(ORDER, order);
        return jsonObject;
    }

    public static String toJsonString(List<SpatialiteMap> maps) throws JSONException {
        JSONArray array = new JSONArray();
        for (SpatialiteMap map : maps) {
            array.put(map.toJson());
        }
        return array.toString();
    }

    public static List<SpatialiteMap> fromJsonString(String json) throws JSONException {
        List<SpatialiteMap> maps = new ArrayList<>();
        if (json.length() == 0) return maps;
        JSONArray array = new JSONArray(json);

        for (int i = 0; i < array.length(); i++) {
            JSONObject jsonObject = array.getJSONObject(i);
            SpatialiteMap map = new SpatialiteMap();
            map.databasePath = jsonObject.getString(DATABASE_PATH);
            map.tableType = jsonObject.getString(TABLE_TYPE);
            map.geometryType = jsonObject.getString(GEOMETRY_TYPE);
            map.tableName = jsonObject.getString(TABLENAME);
            map.isVisible = jsonObject.getBoolean(ISVISIBLE);
            map.order = jsonObject.getDouble(ORDER);

            File databaseFile = new File(map.databasePath);
            if (databaseFile.exists()) {
                maps.add(map);
            }
        }
        return maps;
    }

    @Override
    public String toString() {
        return "SpatialiteMap{" +
                "tableName='" + tableName + '\'' +
                ", tableType='" + tableType + '\'' +
                ", geometryType='" + geometryType + '\'' +
                ", databasePath='" + databasePath + '\'' +
                ", isVisible='" + isVisible + '\'' +
                ", order='" + order + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpatialiteMap spatialiteMap = (SpatialiteMap) o;

        if (!databasePath.equals(spatialiteMap.databasePath)) return false;
        if (!tableType.equals(spatialiteMap.tableType)) return false;
        if (!geometryType.equals(spatialiteMap.geometryType)) return false;
        return tableName.equals(spatialiteMap.tableName);

    }

    @Override
    public int hashCode() {
        int result = databasePath.hashCode();
        result = 31 * result + tableType.hashCode();
        result = 31 * result + geometryType.hashCode();
        result = 31 * result + tableName.hashCode();
        return result;
    }
}
