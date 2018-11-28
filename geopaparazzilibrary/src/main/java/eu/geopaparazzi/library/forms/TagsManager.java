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
package eu.geopaparazzi.library.forms;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.NamedList;
import eu.geopaparazzi.library.util.debug.Debug;

import static eu.geopaparazzi.library.forms.FormUtilities.ATTR_FORMNAME;
import static eu.geopaparazzi.library.forms.FormUtilities.ATTR_FORMS;
import static eu.geopaparazzi.library.forms.FormUtilities.ATTR_SECTIONDESCRIPTION;
import static eu.geopaparazzi.library.forms.FormUtilities.ATTR_SECTIONNAME;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_FORMITEMS;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_FORMS;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_ITEM;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_ITEMNAME;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_ITEMS;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_LONGNAME;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_SHORTNAME;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_VALUES;

/**
 * Singleton that takes care of tags.
 * <p/>
 * <p>The tags are looked for in the following places:</p>
 * <ul>
 * <li>a file named <b>tags.json</b> inside the application folder (Which
 * is retrieved via {@link ResourcesManager#getApplicationSupporterDir()} </li>
 * <li>or, if the above is missing, a file named <b>tags/tags.json</b> in
 * the asset folder of the project. In that case the file is copied over
 * to the file in the first point.</li>
 * </ul>
 * <p>
 * <p>
 * The tags file is subdivided as follows:
 * <p>
 * [{
 * "sectionname": "scheda_sisma",
 * "sectiondescription": "this produces a button names scheda_sisma",
 * "forms": [
 * {
 * "formname": "Name of the section, used in the fragments list",
 * "formitems": [
 * ....
 * ....
 * ]
 * },{
 * "formname": "This name produces a second fragment",
 * "formitems": [
 * ....
 * ....
 * ]
 * }
 * ]
 * },{
 * "sectionname": "section 2",
 * "sectiondescription": "this produces a second button",
 * "forms": [
 * {
 * "formname": "this produces one fragment in the list",
 * "formitems": [
 * ....
 * ....
 * ]
 * },{
 * <p>
 * }
 * ]
 * }]
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TagsManager {

    /**
     * The tags file name end pattern. All files that end with this are ligible as tags.
     */
    public static String TAGSFILENAME_ENDPATTERN = "tags.json";

    private LinkedHashMap<String, JSONObject> sectionsMap = null;
    private HashMap<String, String> sectionsDescriptionMap = null;

    private static TagsManager tagsManager;

    /**
     * Gets the manager singleton.
     *
     * @param context the context to use.
     * @return the {@link TagsManager} singleton.
     * @throws Exception if something goes wrong.
     */
    public static synchronized TagsManager getInstance(Context context) throws Exception {
        if (tagsManager == null) {
            tagsManager = new TagsManager();
            tagsManager.getFileTags(context);
        }
        return tagsManager;
    }

    /**
     * Reset the tags manager, forcing a new reread of the tags.
     */
    public static void reset() {
        tagsManager = null;
    }

    /**
     * Performs the first data reading. Necessary for everything else.
     *
     * @param context the context to use.
     * @throws Exception
     */
    private void getFileTags(Context context) throws Exception {
        if (sectionsMap == null) {
            sectionsMap = new LinkedHashMap<>();
            sectionsDescriptionMap = new HashMap<>();
        }
        File[] tagsFileArray = null;
        Profile activeProfile = ProfilesHandler.INSTANCE.getActiveProfile();
        if (activeProfile != null) {
            String relativePath = activeProfile.profileTags.getRelativePath();
            if (relativePath != null) {
                File profileTagsFile = activeProfile.getFile(relativePath);
                if (profileTagsFile.exists()) {
                    tagsFileArray = new File[1];
                    tagsFileArray[0] = profileTagsFile;
                }

            }
        }

        if (tagsFileArray == null) {
            File applicationDir = ResourcesManager.getInstance(context).getApplicationSupporterDir();

            tagsFileArray = applicationDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(TAGSFILENAME_ENDPATTERN);
                }
            });

            if (tagsFileArray == null || tagsFileArray.length == 0 || Debug.doOverwriteTags) {
                AssetManager assetManager = context.getAssets();
                InputStream inputStream = assetManager.open("tags/" + TAGSFILENAME_ENDPATTERN);

                File examplesTagsFile = new File(applicationDir, TAGSFILENAME_ENDPATTERN);
                FileUtilities.copyFile(inputStream, new FileOutputStream(examplesTagsFile));
                tagsFileArray = new File[1];
                tagsFileArray[0] = examplesTagsFile;
            }
        }

        sectionsMap.clear();
        sectionsDescriptionMap.clear();
        for (File tagsFile : tagsFileArray) {
            if (!tagsFile.exists()) continue;
            String tagsFileString = FileUtilities.readfile(tagsFile);
            JSONArray sectionsArrayObj = new JSONArray(tagsFileString);
            int tagsNum = sectionsArrayObj.length();
            for (int i = 0; i < tagsNum; i++) {
                JSONObject jsonObject = sectionsArrayObj.getJSONObject(i);
                if (jsonObject.has(ATTR_SECTIONNAME)) {
                    String sectionName = jsonObject.getString(ATTR_SECTIONNAME);
                    sectionsMap.put(sectionName, jsonObject);
                    if (jsonObject.has(ATTR_SECTIONDESCRIPTION)){
                        String descr = jsonObject.getString(ATTR_SECTIONDESCRIPTION);
                        sectionsDescriptionMap.put(sectionName, descr);
                    }
                }
            }
        }
    }

    /**
     * @return the section names.
     */
    public Set<String> getSectionNames() {
        return sectionsMap.keySet();
    }

    /**
     * get a section obj by name.
     *
     * @param name thename.
     * @return the section object.
     */
    public JSONObject getSectionByName(String name) {
        return sectionsMap.get(name);
    }

    public String getSectionDescriptionByName(String sectionName) {
        return sectionsDescriptionMap.get(sectionName);
    }

    /**
     * get form name from a section obj.
     *
     * @param section the section.
     * @return the name.
     * @throws JSONException if something goes wrong.
     */
    public static List<String> getFormNames4Section(JSONObject section) throws JSONException {
        List<String> names = new ArrayList<>();
        JSONArray jsonArray = section.getJSONArray(ATTR_FORMS);
        if (jsonArray != null && jsonArray.length() > 0) {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.has(ATTR_FORMNAME)) {
                    String formName = jsonObject.getString(ATTR_FORMNAME);
                    names.add(formName);
                }
            }
        }
        return names;
    }

    /**
     * Get the form for a name.
     *
     * @param formName the name.
     * @param section  the section object containing the form.
     * @return the form object.
     * @throws JSONException if something goes wrong.
     */
    public static JSONObject getForm4Name(String formName, JSONObject section) throws JSONException {
        JSONArray jsonArray = section.getJSONArray(ATTR_FORMS);
        if (jsonArray != null && jsonArray.length() > 0) {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.has(ATTR_FORMNAME)) {
                    String tmpFormName = jsonObject.getString(ATTR_FORMNAME);
                    if (tmpFormName.equals(formName)) {
                        return jsonObject;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Convert a string to a {@link TagObject}.
     *
     * @param jsonString the string.
     * @return the object.
     * @throws JSONException if something goes wrong.
     */
    public static TagObject stringToTagObject(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        String shortname = jsonObject.getString(TAG_SHORTNAME);
        String longname = jsonObject.getString(TAG_LONGNAME);

        TagObject tag = new TagObject();
        tag.shortName = shortname;
        tag.longName = longname;
        if (jsonObject.has(TAG_FORMS)) {
            tag.hasForm = true;
        }
        tag.jsonString = jsonString;
        return tag;
    }

    /**
     * Utility method to get the formitems of a form object.
     * <p>
     * <p>Note that the entering json object has to be one
     * object of the main array, not THE main array itself,
     * i.e. a choice was already done.
     *
     * @param formObj the single object.
     * @return the array of items of the contained form or <code>null</code> if
     * no form is contained.
     * @throws JSONException if something goes wrong.
     */
    public static JSONArray getFormItems(JSONObject formObj) throws JSONException {
        if (formObj.has(TAG_FORMITEMS)) {
            JSONArray formItemsArray = formObj.getJSONArray(TAG_FORMITEMS);
            int emptyIndex = -1;
            while ((emptyIndex = hasEmpty(formItemsArray)) >= 0) {
                formItemsArray.remove(emptyIndex);
            }
            return formItemsArray;
        }
        return new JSONArray();
    }

    private static int hasEmpty(JSONArray formItemsArray) throws JSONException {
        for (int i = 0; i < formItemsArray.length(); i++) {
            JSONObject formItem = formItemsArray.getJSONObject(i);
            if (formItem.length() == 0)
                return i;
        }
        return -1;
    }

    /**
     * Utility method to get the combo items of a formitem object.
     *
     * @param formItem the json form <b>item</b>.
     * @return the array of items.
     * @throws JSONException if something goes wrong.
     */
    public static JSONArray getComboItems(JSONObject formItem) throws JSONException {
        if (formItem.has(TAG_VALUES)) {
            JSONObject valuesObj = formItem.getJSONObject(TAG_VALUES);
            if (valuesObj.has(TAG_ITEMS)) {
                return valuesObj.getJSONArray(TAG_ITEMS);
            }
        }
        return null;
    }

    /**
     * @param comboItems combo items object.
     * @return the string names.
     * @throws JSONException if something goes wrong.
     */
    public static String[] comboItems2StringArray(JSONArray comboItems) throws JSONException {
        int length = comboItems.length();
        String[] itemsArray = new String[length];
        for (int i = 0; i < length; i++) {
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
     * Extract the combo values map.
     *
     * @param formItem the json object.
     * @return the map of combo items.
     * @throws JSONException if something goes wrong.
     */
    public static LinkedHashMap<String, List<String>> extractComboValuesMap(JSONObject formItem) throws JSONException {
        LinkedHashMap<String, List<String>> valuesMap = new LinkedHashMap<>();
        if (formItem.has(TAG_VALUES)) {
            JSONObject valuesObj = formItem.getJSONObject(TAG_VALUES);

            JSONArray names = valuesObj.names();
            int length = names.length();
            for (int i = 0; i < length; i++) {
                String name = names.getString(i);

                List<String> valuesList = new ArrayList<String>();
                JSONArray itemsArray = valuesObj.getJSONArray(name);
                int length2 = itemsArray.length();
                for (int j = 0; j < length2; j++) {
                    JSONObject itemObj = itemsArray.getJSONObject(j);
                    if (itemObj.has(TAG_ITEM)) {
                        valuesList.add(itemObj.getString(TAG_ITEM).trim());
                    } else {
                        valuesList.add(" - ");
                    }
                }
                valuesMap.put(name, valuesList);
            }
        }
        return valuesMap;

    }

    /**
     * Extract the combo values map.
     *
     * @param formItem the json object.
     * @return the map of combo items.
     * @throws JSONException if something goes wrong.
     */
    public static LinkedHashMap<String, List<NamedList<String>>> extractOneToManyComboValuesMap(JSONObject formItem) throws JSONException {
        LinkedHashMap<String, List<NamedList<String>>> valuesMap = new LinkedHashMap<>();
        if (formItem.has(TAG_VALUES)) {
            JSONObject valuesObj = formItem.getJSONObject(TAG_VALUES);

            JSONArray names = valuesObj.names();
            int length = names.length();
            for (int i = 0; i < length; i++) {
                String name = names.getString(i);

                List<NamedList<String>> valuesList = new ArrayList<>();
                JSONArray itemsArray = valuesObj.getJSONArray(name);
                int length2 = itemsArray.length();
                for (int j = 0; j < length2; j++) {
                    JSONObject itemObj = itemsArray.getJSONObject(j);

                    String itemName = itemObj.getString(TAG_ITEMNAME);
                    JSONArray itemsSubArray = itemObj.getJSONArray(TAG_ITEMS);
                    NamedList<String> namedList = new NamedList<>();
                    namedList.name = itemName;
                    int length3 = itemsSubArray.length();
                    for (int k = 0; k < length3; k++) {
                        JSONObject subIemObj = itemsSubArray.getJSONObject(k);
                        if (subIemObj.has(TAG_ITEM)) {
                            namedList.items.add(subIemObj.getString(TAG_ITEM).trim());
                        } else {
                            namedList.items.add(" - ");
                        }
                    }
                    valuesList.add(namedList);
                }
                valuesMap.put(name, valuesList);
            }
        }
        return valuesMap;
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
