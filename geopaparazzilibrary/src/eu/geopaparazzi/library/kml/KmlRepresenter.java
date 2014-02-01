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
package eu.geopaparazzi.library.kml;

import java.util.List;

/**
 * Interface for objects that are able to represent theirself as kml.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface KmlRepresenter {
    /**
     * Transforms the object in its kml representation.
     *  
     * @return the kml representation.
     * @throws Exception  if something goes wrong.
     */
    public String toKmlString() throws Exception;

    /**
     * Getter for image flag.
     * 
     * @return <code>true</code> if the object has also an image that needs to be embedded in the kmz.
     */
    public boolean hasImages();

    /**
     * Getter for the relative image path against the application folder. 
     * 
     * @return the image path.
     */
    public List<String> getImagePaths();
}
