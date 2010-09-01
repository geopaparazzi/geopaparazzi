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

import android.graphics.PointF;
import android.location.Location;

/**
 * Add a third dimension to {@link PointF}.
 * 
 * <p>Note that only functions added here implement the third dimension.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PointF3D extends PointF {
    private float z = Float.NaN;
    private boolean hasZ = false;

    public PointF3D( float x, float y ) {
        super(x, y);
        hasZ = false;
    }

    public PointF3D( float x, float y, float z ) {
        super(x, y);
        this.z = z;
        hasZ = true;
    }

    public void setZ( float z ) {
        this.z = z;
        hasZ = true;
    }

    public float getZ() {
        return z;
    }

    public boolean isHasZ() {
        return hasZ;
    }

    @SuppressWarnings("nls")
    public float distance( PointF p ) {
        Location thisLoc = new Location("dummy");
        thisLoc.setLongitude(x);
        thisLoc.setLatitude(y);
        Location thatLoc = new Location("dummy");
        thatLoc.setLongitude(p.x);
        thatLoc.setLatitude(p.y);
        
        return thisLoc.distanceTo(thatLoc);
    }

}
