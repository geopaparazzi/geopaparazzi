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

package eu.geopaparazzi.library.util;

/**
 * Created by hydrologis on 31/01/16.
 */
@SuppressWarnings("ALL")
public enum FileTypes {
    GPAP("gpap", "Geopaparazzi Project", "gpap"),
    GPX("gpx", "GPX gps file", "gpx"),
    KML("kml", "KML file", "kml");


    private String label;
    private String description;
    private String extension;

    FileTypes(String label, String description, String extension) {
        this.label = label;
        this.description = description;
        this.extension = extension;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getExtension() {
        return extension;
    }
}
