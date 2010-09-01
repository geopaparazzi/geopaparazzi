/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.geopaparazzi.osm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import eu.hydrologis.geopaparazzi.util.ApplicationManager;

/**
 * Singleton that takes care of osm tags.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OsmTagsManager {
    public static String OSMTAGSFILENAME = "osmtags.properties";

    private static HashMap<String, String> osmTagsMap = new HashMap<String, String>();
    static {
        osmTagsMap.put("ATM", "ATM");
        osmTagsMap.put("Beach", "Beach");
        osmTagsMap.put("Church", "Place of Worship: Church");
        osmTagsMap.put("Ecocenter", "Ecocenter");
        osmTagsMap.put("Fountain", "Fountain");
        osmTagsMap.put("Market", "Market");
        osmTagsMap.put("Public Phone", "Public Phone");
        osmTagsMap.put("Restaurant", "Restaurant");
        osmTagsMap.put("Village", "Village");
        osmTagsMap.put("POI", "POI");
    }

    private static OsmTagsManager osmTagsManager;

    private static String[] osmTagsArrays;

    /**
     * Gets the manager singleton. 
     * 
     * @return the {@link OsmTagsManager} singleton.
     */
    public static OsmTagsManager getInstance() {
        if (osmTagsManager == null) {
            osmTagsManager = new OsmTagsManager();
            getFileTags();

            Set<String> tagsSet = osmTagsMap.keySet();
            osmTagsArrays = (String[]) tagsSet.toArray(new String[tagsSet.size()]);
            Arrays.sort(osmTagsArrays);
        }

        return osmTagsManager;
    }

    private static void getFileTags() {
        File geoPaparazziDir = ApplicationManager.getInstance().getGeoPaparazziDir();
        File osmTagsFile = new File(geoPaparazziDir, OSMTAGSFILENAME);
        if (osmTagsFile.exists()) {
            BufferedReader br = null;
            osmTagsMap.clear();
            try {
                br = new BufferedReader(new FileReader(osmTagsFile));
                String line = null;
                while( (line = br.readLine()) != null ) {
                    if (line.indexOf("=") == -1 || line.startsWith("#")) {
                        continue;
                    }
                    String[] lineSplit = line.split("=");
                    osmTagsMap.put(lineSplit[0].trim(), lineSplit[1].trim());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String[] getOsmTagsArrays() {
        return osmTagsArrays;
    }

    public String getDefinitionFromTag( String tag ) {
        return osmTagsMap.get(tag);
    }

}
