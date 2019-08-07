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

import java.io.Serializable;

/**
 * Form info to pass around.
 *
 * @author Andrea Antonello
 */
@SuppressWarnings("ALL")
public class FormInfoHolder implements Serializable {
    public static final String BUNDLE_KEY_INFOHOLDER = "BUNDLE_KEY_INFOHOLDER";

    public String sectionName;
    public String formName;
    public String sectionObjectString;
    public long noteId;
    public double longitude;
    public double latitude;
    public boolean objectExists;

    // ADDED INFO FOR RENDERING AND UPDATE
    public String renderingLabel;
    public long lastTimestamp;
    public String category = "POI";
}
