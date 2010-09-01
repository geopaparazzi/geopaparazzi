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
package eu.hydrologis.geopaparazzi.gpx;

import java.util.List;

import eu.hydrologis.geopaparazzi.util.PointF3D;

public interface IGpxParser {

    /*
     * READ GPX DATA FILE
     */
    public abstract int read( String filename );

    public abstract List<PointF3D> getPoints();

    public abstract List<String> getNames();

    public abstract float getNorthBound();
    public abstract float getSouthBound();
    public abstract float getEastBound();
    public abstract float getWestBound();
    public abstract float getMaxElev();
    public abstract float getMinElev();
    public abstract float getLength();

}