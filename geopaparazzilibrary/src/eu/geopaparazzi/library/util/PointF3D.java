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

import android.graphics.PointF;
import android.location.Location;

/**
 * Add a third dimension and description to {@link PointF}.
 * 
 * <p>Note that only functions added here implement the third dimension.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PointF3D extends PointF {
    private float z = Float.NaN;
    private boolean hasZ = false;

    private String description = ""; //$NON-NLS-1$

    /**
     * @param x x
     * @param y y
     */
    public PointF3D( float x, float y ) {
        super(x, y);
        hasZ = false;
    }

    /**
     * @param x x
     * @param y y
     * @param z z 
     */
    public PointF3D( float x, float y, float z ) {
        super(x, y);
        this.z = z;
        hasZ = true;
    }

    /**
     * @param x x
     * @param y y
     * @param z z 
     * @param description description.
     */
    public PointF3D( float x, float y, float z, String description ) {
        this(x, y, z);
        this.z = z;
        if (description != null)
            this.description = description;
        hasZ = true;
    }

    /**
     * @param z z to set.
     */
    public void setZ( float z ) {
        this.z = z;
        hasZ = true;
    }

    /**
     * @return z.
     */
    public float getZ() {
        return z;
    }

    /**
     * @return <code>true</code> if it has z.
     */
    public boolean hasZ() {
        return hasZ;
    }

    /**
     * @return description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description description to set.
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * Calculates the 2d distance between two points.
     * 
     * @param p the {@link PointF} from which to calculate from.
     * @return the 2d distance.
     */
    @SuppressWarnings("nls")
    public float distance2d( PointF p ) {
        Location thisLoc = new Location("dummy");
        thisLoc.setLongitude(x);
        thisLoc.setLatitude(y);
        Location thatLoc = new Location("dummy");
        thatLoc.setLongitude(p.x);
        thatLoc.setLatitude(p.y);

        return thisLoc.distanceTo(thatLoc);
    }

    /**
     * Calculates the 3d distance between two points if z is available.
     * 
     * @param p the {@link PointF3D} from which to calculate from.
     * @return the 3d distance (or 2d if no elevation info is available).
     */
    public float distance3d( PointF3D p ) {
        float distance2d = distance2d(p);
        if (hasZ && p.hasZ()) {
            double distance3d = Utilities.pythagoras(distance2d, Math.abs(z - p.getZ()));
            return (float) distance3d;
        }
        return distance2d;
    }

}
