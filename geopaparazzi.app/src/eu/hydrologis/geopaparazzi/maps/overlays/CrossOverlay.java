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
package eu.hydrologis.geopaparazzi.maps.overlays;

import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.Overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

/**
 * Overlay to show the cross.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CrossOverlay extends Overlay {

    private boolean doDraw = true;
    private Paint crossPaint = new Paint();

    public CrossOverlay( final Context ctx, final ResourceProxy pResourceProxy ) {
        super(pResourceProxy);

        crossPaint.setAntiAlias(true);
        crossPaint.setColor(Color.GRAY);
        crossPaint.setStrokeWidth(1f);
        crossPaint.setStyle(Paint.Style.STROKE);
    }

    public void setDoDraw( boolean doDraw ) {
        this.doDraw = doDraw;
    }

    protected void draw( final Canvas canvas, final MapView mapsView, final boolean shadow ) {
        if (shadow || !doDraw)
            return;

        Projection pj = mapsView.getProjection();
        GeoPoint mapCenter = mapsView.getMapCenter();
        Point center = pj.toMapPixels(mapCenter, null);
        canvas.drawLine(center.x, center.y - 20, center.x, center.y + 20, crossPaint);
        canvas.drawLine(center.x - 20, center.y, center.x + 20, center.y, crossPaint);
    }
}
