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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.FileUtils;

/**
 * Singleton that takes care of osm tags.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OsmTagsManager {

    private static final String TAG_LONGNAME = "longname";
    private static final String TAG_SHORTNAME = "shortname";
    private static final String TAG_FORM = "form";
    private static final String TAG_FORMITEMS = "formitems";

    public static String TAGSFILENAME = "tags.json";

    private static HashMap<String, TagObject> tagsMap = new HashMap<String, TagObject>();

    private static OsmTagsManager tagsManager;

    private static String[] tagsArrays;

    /**
     * Gets the manager singleton. 
     * 
     * @return the {@link OsmTagsManager} singleton.
     * @throws IOException 
     */
    public static OsmTagsManager getInstance( Context context ) throws Exception {
        if (tagsManager == null) {
            tagsManager = new OsmTagsManager();
            getFileTags(context);

            Set<String> tagsSet = tagsMap.keySet();
            tagsArrays = (String[]) tagsSet.toArray(new String[tagsSet.size()]);
            Arrays.sort(tagsArrays);

        }

        return tagsManager;
    }

    private static void getFileTags( Context context ) throws Exception {
        File geoPaparazziDir = ApplicationManager.getInstance(context).getGeoPaparazziDir();
        File osmTagsFile = new File(geoPaparazziDir, TAGSFILENAME);
        // if (!osmTagsFile.exists()) {
        if (true) {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("tags/tags.json");

            FileUtils.copyFile(inputStream, new FileOutputStream(osmTagsFile));
        }

        if (osmTagsFile.exists()) {

            tagsMap.clear();
            String tagsFileString = FileUtils.readfile(osmTagsFile);
            JSONArray tagArrayObj = new JSONArray(tagsFileString);
            int tagsNum = tagArrayObj.length();
            for( int i = 0; i < tagsNum; i++ ) {
                JSONObject jsonObject = tagArrayObj.getJSONObject(i);
                String shortname = jsonObject.getString(TAG_SHORTNAME);
                String longname = jsonObject.getString(TAG_LONGNAME);
                JSONArray formItemsArray = null;
                if (jsonObject.has(TAG_FORM)) {
                    JSONObject formObj = jsonObject.getJSONObject(TAG_FORM);
                    formItemsArray = formObj.getJSONArray(TAG_FORMITEMS);
                }
                TagObject tag = new TagObject();
                tag.shortName = shortname;
                tag.longName = longname;
                tag.formItems = formItemsArray;
                tagsMap.put(shortname, tag);
            }

        }
    }

    public String[] getTagsArrays() {
        return tagsArrays;
    }

    public TagObject getTagFromName( String name ) {
        return tagsMap.get(name);
    }

    public static class TagObject {
        public String shortName;
        public String longName;
        public JSONArray formItems;
    }
}
