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
package eu.hydrologis.geopaparazzi.maps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.FileUtils;
import eu.hydrologis.geopaparazzi.util.debug.Debug;

/**
 * Singleton that takes care of tags.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TagsManager {

    public static final String TAG_LONGNAME = "longname";
    public static final String TAG_SHORTNAME = "shortname";
    public static final String TAG_FORM = "form";
    public static final String TAG_FORMITEMS = "formitems";
    public static final String TAG_KEY = "key";
    public static final String TAG_VALUE = "value";
    public static final String TAG_VALUES = "values";
    public static final String TAG_ITEMS = "items";
    public static final String TAG_ITEM = "item";
    public static final String TAG_TYPE = "type";

    public static final String TYPE_STRING = "string";
    public static final String TYPE_DOUBLE = "double";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_DOUBLECOMBO = "doublecombo";
    public static final String TYPE_STRINGCOMBO = "stringcombo";

    public static String TAGSFILENAME = "tags.json";

    private static HashMap<String, TagObject> tagsMap = new HashMap<String, TagObject>();

    private static TagsManager tagsManager;

    private static String[] tagsArrays = new String[0];

    /**
     * Gets the manager singleton. 
     * 
     * @return the {@link TagsManager} singleton.
     * @throws IOException 
     */
    public static TagsManager getInstance( Context context ) throws Exception {
        if (tagsManager == null) {
            tagsManager = new TagsManager();
            getFileTags(context);

            Set<String> tagsSet = tagsMap.keySet();
            tagsArrays = (String[]) tagsSet.toArray(new String[tagsSet.size()]);
            Arrays.sort(tagsArrays);

        }

        return tagsManager;
    }

    private static void getFileTags( Context context ) throws Exception {
        File geoPaparazziDir = ApplicationManager.getInstance(context).getGeoPaparazziDir();
        File tagsFile = new File(geoPaparazziDir, TAGSFILENAME);
        if (!tagsFile.exists() || Debug.doOverwriteTags) {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("tags/tags.json");

            FileUtils.copyFile(inputStream, new FileOutputStream(tagsFile));
        }

        if (tagsFile.exists()) {
            tagsMap.clear();
            String tagsFileString = FileUtils.readfile(tagsFile);
            JSONArray tagArrayObj = new JSONArray(tagsFileString);
            int tagsNum = tagArrayObj.length();
            for( int i = 0; i < tagsNum; i++ ) {
                JSONObject jsonObject = tagArrayObj.getJSONObject(i);
                TagObject tag = stringToTagObject(jsonObject.toString());
                tagsMap.put(tag.shortName, tag);
            }

        }
    }

    public String[] getTagsArrays() {
        return tagsArrays;
    }

    public TagObject getTagFromName( String name ) {
        return tagsMap.get(name);
    }

    public static TagObject stringToTagObject( String jsonString ) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        String shortname = jsonObject.getString(TAG_SHORTNAME);
        String longname = jsonObject.getString(TAG_LONGNAME);

        TagObject tag = new TagObject();
        tag.shortName = shortname;
        tag.longName = longname;
        if (jsonObject.has(TAG_FORM)) {
            tag.hasForm = true;
        }
        tag.jsonString = jsonString;
        return tag;
    }

    /**
     * Utility method to get the formitems of a form object.
     * 
     * <p>Note that the entering json object has to be one 
     * object of the main array, not THE main array itself, 
     * i.e. a choice was already done.
     * 
     * @param jsonObj the single object.
     * @return the array of items of the contained form or <code>null</code> if 
     *          no form is contained.
     * @throws JSONException
     */
    public static JSONArray getFormItems( JSONObject jsonObj ) throws JSONException {
        if (jsonObj.has(TagsManager.TAG_FORM)) {
            JSONObject formObj = jsonObj.getJSONObject(TagsManager.TAG_FORM);
            if (formObj.has(TagsManager.TAG_FORMITEMS)) {
                JSONArray formItemsArray = formObj.getJSONArray(TagsManager.TAG_FORMITEMS);
                return formItemsArray;
            }
        }
        return null;
    }

    /**
     * Utility method to get the combo items of a formitem object.
     * 
     * @param formItem the json form <b>item</b>.
     * @return the array of items.
     * @throws JSONException
     */
    public static JSONArray getComboItems( JSONObject formItem ) throws JSONException {
        if (formItem.has(TagsManager.TAG_VALUES)) {
            JSONObject valuesObj = formItem.getJSONObject(TagsManager.TAG_VALUES);
            if (valuesObj.has(TagsManager.TAG_ITEMS)) {
                JSONArray itemsArray = valuesObj.getJSONArray(TagsManager.TAG_ITEMS);
                return itemsArray;
            }
        }
        return null;
    }

    public static String[] comboItems2StringArray( JSONArray comboItems ) throws JSONException {
        int length = comboItems.length();
        String[] itemsArray = new String[length];
        for( int i = 0; i < length; i++ ) {
            JSONObject itemObj = comboItems.getJSONObject(i);
            if (itemObj.has(TagsManager.TAG_ITEM)) {
                itemsArray[i] = itemObj.getString(TagsManager.TAG_ITEM).trim();
            } else {
                itemsArray[i] = " - ";
            }
        }
        return itemsArray;
    }

    public static class TagObject {
        public String shortName;
        public String longName;
        public boolean hasForm;
        public String jsonString;
    }
}
