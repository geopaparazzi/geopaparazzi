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
package eu.geopaparazzi.library.util.debug;

/**
 * Small interface to get hold of all debug possibilities in one place. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class Debug {
    /**
     * Flag to define if we are in debug mode.
     * 
     * <p>For release = <code>false</code>.
     */
    public final static boolean D = false;

    /**
     * Flag to define if the tags file should be overwritten. 
     * 
     * <p>For release = <code>false</code>.
     */
    public final static boolean doOverwriteTags = false;

}
