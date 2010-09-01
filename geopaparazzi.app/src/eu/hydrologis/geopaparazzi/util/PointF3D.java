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
