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
 * A class representing a basemap (tiles or rasterlite for example).
 *
 * @author Andrea Antonello
 */
public class BaseMap {
    public static final String BASEMAPS_PREF_KEY = "BASEMAPS_PREF_KEY";//NON-NLS

    public static final String PARENT_FOLDER = "parentFolder";//NON-NLS
    public static final String DATABASE_PATH = "databasePath";//NON-NLS
    public static final String MAP_TYPE = "mapType";//NON-NLS
    public static final String TITLE = "title";//NON-NLS

    public String parentFolder;
    public String databasePath;
    public String mapType;
    public String title;

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(PARENT_FOLDER, parentFolder);
        jsonObject.put(DATABASE_PATH, databasePath);
        jsonObject.put(MAP_TYPE, mapType);
        jsonObject.put(TITLE, title);
        return jsonObject;
    }

    public static String toJsonString(List<BaseMap> maps) throws JSONException {
        JSONArray array = new JSONArray();
        for (BaseMap map : maps) {
            array.put(map.toJson());
        }
        return array.toString();
    }

    public static List<BaseMap> fromJsonString(String json) throws JSONException {
        List<BaseMap> maps = new ArrayList<>();
        if (json.length() == 0) return maps;
        JSONArray array = new JSONArray(json);

        for (int i = 0; i < array.length(); i++) {
            JSONObject jsonObject = array.getJSONObject(i);
            BaseMap map = new BaseMap();
            map.parentFolder = jsonObject.getString(PARENT_FOLDER);
            map.databasePath = jsonObject.getString(DATABASE_PATH);
            map.mapType = jsonObject.getString(MAP_TYPE);
            map.title = jsonObject.getString(TITLE);

            File databaseFile = new File(map.databasePath);
            if (databaseFile.exists()) {
                maps.add(map);
            }
        }
        return maps;
    }

    @Override
    public String toString() {
        return "BaseMap{" +//NON-NLS
                "title='" + title + '\'' +//NON-NLS
                ", mapType='" + mapType + '\'' +//NON-NLS
                ", databasePath='" + databasePath + '\'' +//NON-NLS
                ", parentFolder='" + parentFolder + '\'' +//NON-NLS
                '}';//NON-NLS
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseMap baseMap = (BaseMap) o;

        if (!parentFolder.equals(baseMap.parentFolder)) return false;
        if (!databasePath.equals(baseMap.databasePath)) return false;
        if (!mapType.equals(baseMap.mapType)) return false;
        return title.equals(baseMap.title);

    }

    @Override
    public int hashCode() {
        int result = parentFolder.hashCode();
        result = 31 * result + databasePath.hashCode();
        result = 31 * result + mapType.hashCode();
        result = 31 * result + title.hashCode();
        return result;
    }
}
