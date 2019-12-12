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

package eu.geopaparazzi.library.gpx.parser;

/**
 * A GPS/KML way point.
 * <p/>A waypoint is a user specified location, with a name and an optional description.
 */
public final class WayPoint extends LocationPoint {
    private String mName;
    private String mDescription;

    void setName(String name) {
        mName = name;
    }

    /**
     * @return name.
     */
    public String getName() {
        return mName;
    }

    void setDescription(String description) {
        mDescription = description;
    }

    /**
     * @return description.
     */
    public String getDescription() {
        return mDescription;
    }
}
