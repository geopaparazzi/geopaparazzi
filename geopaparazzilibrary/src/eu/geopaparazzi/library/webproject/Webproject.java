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
package eu.geopaparazzi.library.webproject;

/**
 * The class holding webproject info.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Webproject {

    /**
     * The server unique id for the project.
     */
    public long id;
    /**
     * The size of the project in bytes.
     */
    public long size;
    /**
     * Name of the project.
     * 
     * <p>This is also the folder that will be created.</p> 
     */
    public String name;
    /**
     * A user title for the project when uploaded. Can be a description.
     */
    public String title;
    /**
     * The author of the project.
     */
    public String author;
    /**
     * The upload date of the project.
     */
    public String date;

    /**
     * Checks if the project info match the supplied string.
     * 
     * @param pattern the pattern to match.
     * @return <code>true</code> if the pattern matches any info.
     */
    public boolean matches( String pattern ) {
        pattern = pattern.toLowerCase();
        if (name.toLowerCase().contains(pattern)) {
            return true;
        }
        if (title.toLowerCase().contains(pattern)) {
            return true;
        }
        if (author.toLowerCase().contains(pattern)) {
            return true;
        }
        if (date.toLowerCase().contains(pattern)) {
            return true;
        }
        if (String.valueOf(id).toLowerCase().contains(pattern)) {
            return true;
        }
        return false;
    }
}
