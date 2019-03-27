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
package org.locationtech.jts.android.geom;

import android.graphics.RectF;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;

/**
 * An oval {@link DrawableShape shape}.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OvalShape implements DrawableShape {

    protected int cx;
    protected int cy;
    protected int radius;

    public OvalShape() {
    }

    public OvalShape(RectF rectF) {
        cx = (int) (rectF.left + (rectF.right - rectF.left) / 2);
        cy = (int) (rectF.bottom + (rectF.top - rectF.bottom) / 2);
        radius = (int) ((rectF.top - rectF.bottom) / 2);
    }

    public OvalShape(float x, float y, float width, float height) {
        cx = (int) (x + width / 2);
        cy = (int) (y + height / 2);
        radius = (int) (height / 2);
    }

    public void draw(Canvas canvas, Paint paint) {
        paint.setStyle(Style.STROKE);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    public void fill(Canvas canvas, Paint paint) {
        paint.setStyle(Style.FILL);
        canvas.drawCircle(cx, cy, radius, paint);
    }

}
