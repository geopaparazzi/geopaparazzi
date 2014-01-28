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
package eu.geopaparazzi.library.util;
/**
 * @author Andrea Antonello (www.hydrologis.com)
 * 
 * A bounding box that keeps both world and screen space.
 */
public class BoundingBox {
    /**
     * 
     */
    public float north;
    /**
     * 
     */
    public float south;
    /**
     * 
     */
    public float east;
    /**
     * 
     */
    public float west;
    /**
     * 
     */
    public int left;
    /**
     * 
     */
    public int bottom;
    /**
     * 
     */
    public int right;
    /**
     * 
     */
    public int top;

    @SuppressWarnings("nls")
    public String toString() {
        StringBuilder sB = new StringBuilder();
        sB.append("left=").append(left).append("/");
        sB.append("right=").append(right).append("/");
        sB.append("top=").append(top).append("/");
        sB.append("bottom=").append(bottom);
        return sB.toString();
    }
}