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
package eu.hydrologis.geopaparazzi.osm;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.debug.Debug;

import static eu.hydrologis.geopaparazzi.osm.FormUtilities.TAG_FORM;
import static eu.hydrologis.geopaparazzi.osm.FormUtilities.TAG_FORMITEMS;
import static eu.hydrologis.geopaparazzi.osm.FormUtilities.TAG_ITEM;
import static eu.hydrologis.geopaparazzi.osm.FormUtilities.TAG_ITEMS;
import static eu.hydrologis.geopaparazzi.osm.FormUtilities.TAG_LONGNAME;
import static eu.hydrologis.geopaparazzi.osm.FormUtilities.TAG_SHORTNAME;
import static eu.hydrologis.geopaparazzi.osm.FormUtilities.TAG_VALUES;

/**
 * Singleton that takes care of tags.
 * 
 * <p>The tags are looked for in the following places:</p>
 * <ul>
 *  <li>a file named <b>tags.json</b> inside the application folder (Which 
 *      is retrieved via {@link ResourcesManager#getApplicationDir()}</li>
 *  <li>or, if the above is missing, a file named <b>tags/tags.json</b> in
 *      the asset folder of the project. In that case the file is copied over 
 *      to the file in the first point.</li>
 * </ul>
 * 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TagsManager {

    /**
     * The name of the tags file.
     */
    public static String TAGSFILENAME = "tags.json";

    private static HashMap<String, TagObject> tagsMap = new HashMap<String, TagObject>();

    private static TagsManager tagsManager;

    private static String[] tagsArrays = new String[0];

    /**
     * Gets the manager singleton.
     *  
     * @param context  the context to use.
     * 
     * @return the {@link TagsManager} singleton.
     * @throws Exception  if something goes wrong. 
     */
    public static synchronized TagsManager getInstance( Context context ) throws Exception {
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
        File applicationDir = ResourcesManager.getInstance(context).getApplicationDir();
        File tagsFile = new File(applicationDir, TAGSFILENAME);
        if (!tagsFile.exists() || Debug.doOverwriteTags) {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("tags/tags.json");

            FileUtilities.copyFile(inputStream, new FileOutputStream(tagsFile));
        }

        if (tagsFile.exists()) {
            tagsMap.clear();
            String tagsFileString = FileUtilities.readfile(tagsFile);
            JSONArray tagArrayObj = new JSONArray(tagsFileString);
            int tagsNum = tagArrayObj.length();
            for( int i = 0; i < tagsNum; i++ ) {
                JSONObject jsonObject = tagArrayObj.getJSONObject(i);
                TagObject tag = stringToTagObject(jsonObject.toString());
                tagsMap.put(tag.shortName, tag);
            }

        }
    }

    /**
     * @return the array of tag names.
     */
    public String[] getTagsArrays() {
        return tagsArrays;
    }

    /**
     * get tag for its name.
     * 
     * @param name the name
     * @return the tag.
     */
    public TagObject getTagFromName( String name ) {
        return tagsMap.get(name);
    }

    /**
     * Convert string to tag object.
     * 
     * @param jsonString teh string.
     * @return the object.
     * @throws JSONException  if something goes wrong.
     */
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
     * @throws JSONException  if something goes wrong.
     */
    public static JSONArray getFormItems( JSONObject jsonObj ) throws JSONException {
        if (jsonObj.has(TAG_FORM)) {
            JSONObject formObj = jsonObj.getJSONObject(TAG_FORM);
            if (formObj.has(TAG_FORMITEMS)) {
                JSONArray formItemsArray = formObj.getJSONArray(TAG_FORMITEMS);
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
     * @throws JSONException  if something goes wrong.
     */
    public static JSONArray getComboItems( JSONObject formItem ) throws JSONException {
        if (formItem.has(TAG_VALUES)) {
            JSONObject valuesObj = formItem.getJSONObject(TAG_VALUES);
            if (valuesObj.has(TAG_ITEMS)) {
                JSONArray itemsArray = valuesObj.getJSONArray(TAG_ITEMS);
                return itemsArray;
            }
        }
        return null;
    }

    /**
     * Convert json combo array to string. 
     * 
     * @param comboItems the combo json.
     * @return the strings.
     * @throws JSONException  if something goes wrong.
     */
    public static String[] comboItems2StringArray( JSONArray comboItems ) throws JSONException {
        int length = comboItems.length();
        String[] itemsArray = new String[length];
        for( int i = 0; i < length; i++ ) {
            JSONObject itemObj = comboItems.getJSONObject(i);
            if (itemObj.has(TAG_ITEM)) {
                itemsArray[i] = itemObj.getString(TAG_ITEM).trim();
            } else {
                itemsArray[i] = " - ";
            }
        }
        return itemsArray;
    }

    /**
     * The tag object.
     */
    public static class TagObject {
        /**
         * 
         */
        public String shortName;
        /**
         * 
         */
        public String longName;
        /**
         * 
         */
        public boolean hasForm;
        /**
         * 
         */
        public String jsonString;
    }
}
