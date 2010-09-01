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
package eu.hydrologis.geopaparazzi.util;
/**
 * @author Andrea Antonello (www.hydrologis.com)
 * 
 * A bounding box that keeps both world and screen space.
 */
public class BoundingBox {
    public float north;
    public float south;
    public float east;
    public float west;
    public int left;
    public int bottom;
    public int right;
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