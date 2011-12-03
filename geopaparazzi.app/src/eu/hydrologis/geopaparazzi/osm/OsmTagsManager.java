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
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;

/**
 * Singleton that takes care of tags.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class OsmTagsManager {

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

    public static String TAGSFOLDERNAME = "osmtags";

    private static OsmTagsManager osmTagsManager;

    private File tagsFolderFile;
    private String[] categoriesNames;

    /**
     * Gets the manager singleton. 
     * 
     * @return the {@link OsmTagsManager} singleton.
     * @throws IOException 
     */
    public static OsmTagsManager getInstance() {
        if (osmTagsManager == null) {
            osmTagsManager = new OsmTagsManager();
        }
        return osmTagsManager;
    }

    public synchronized File getTagsFolderFile( Context context ) {
        if (tagsFolderFile == null) {
            File geoPaparazziDir = ApplicationManager.getInstance(context).getGeoPaparazziDir();
            tagsFolderFile = new File(geoPaparazziDir.getParentFile(), TAGSFOLDERNAME);
        }
        return tagsFolderFile;
    }

    public synchronized String[] getTagCategories( Context context ) throws Exception {
        if (categoriesNames == null) {
            File tagsFolderFile = getTagsFolderFile(context);
            if (tagsFolderFile.exists()) {
                File[] foldersList = tagsFolderFile.listFiles(new FileFilter(){
                    public boolean accept( File pathname ) {
                        return pathname.isDirectory();
                    }
                });
                categoriesNames = new String[foldersList.length];
                for( int i = 0; i < foldersList.length; i++ ) {
                    categoriesNames[i] = foldersList[i].getName();
                }
                Arrays.sort(categoriesNames);
                return categoriesNames;
            }
        }
        return categoriesNames;
    }

    public String[] getItemsForCategory( Context context, String category ) {
        File tagsFolderFile = getTagsFolderFile(context);
        File categoryFolderFile = new File(tagsFolderFile, category);
        File[] iconFiles = categoryFolderFile.listFiles(new FileFilter(){
            public boolean accept( File pathname ) {
                return pathname.getName().endsWith("png");
            }
        });
        String[] iconFileNames = new String[iconFiles.length];
        for( int i = 0; i < iconFiles.length; i++ ) {
            iconFileNames[i] = iconFiles[i].getName().replace("\\.png", "");
        }
        return iconFileNames;
    }

}
