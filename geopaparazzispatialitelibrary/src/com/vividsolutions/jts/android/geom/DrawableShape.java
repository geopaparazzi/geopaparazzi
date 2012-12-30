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
package com.vividsolutions.jts.android.geom;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Interface to help drawing JTS shapes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface DrawableShape {

    /**
     * Draw the current shape in stroke mode.
     * 
     * @param canvas the {@link Canvas} on which to draw to.
     * @param paint the {@link Paint} to use.
     */
    public void draw( Canvas canvas, Paint paint );

    /**
     * Draw the current shape in fill mode.
     * 
     * @param canvas the {@link Canvas} on which to draw to.
     * @param paint the {@link Paint} to use.
     */
    public void fill( Canvas canvas, Paint paint );

    /**
     * Draw the current shape in fill and stroke mode.
     * 
     * @param canvas the {@link Canvas} on which to draw to.
     * @param paint the {@link Paint} to use.
     */
    public void fillAndStroke( Canvas canvas, Paint paint );
}
