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
package eu.geopaparazzi.library.webprofile;

import org.json.JSONObject;

/**
 * The class holding webprofile info.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Webprofile {

    /**
     * Name of the profile.
     * 
     * <p>This is also the folder that will be created.</p> 
     */
    public String name;
    /**
     * A user title for the profile when uploaded. Can be a description.
     */
    public String description;

    /**
     * The upload date of the profile.
     */
    public String date;

    /**
     * The json of the profile.
     */
    public JSONObject oJson;
    /**
     * Checks if the profile info match the supplied string.
     * 
     * @param pattern the pattern to match.
     * @return <code>true</code> if the pattern matches any info.
     */
    public boolean matches( String pattern ) {
        pattern = pattern.toLowerCase();
        if (name.toLowerCase().contains(pattern)) {
            return true;
        }
        if (description.toLowerCase().contains(pattern)) {
            return true;
        }
        if (date.toLowerCase().contains(pattern)) {
            return true;
        }
        return false;
    }
}
