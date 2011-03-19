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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;

/**
 * Overlay to show the gps position.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsPositionOverlay extends Overlay {

    private boolean doDraw = true;
    private static Bitmap gpsIcon;
    private static int gpsIconWidth;
    private static int gpsIconHeight;

    private GpsLocation loc;

    public GpsPositionOverlay( final Context ctx, final ResourceProxy pResourceProxy ) {
        super(pResourceProxy);

        if (gpsIcon == null) {
            gpsIcon = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.current_position);
            gpsIconWidth = gpsIcon.getWidth();
            gpsIconHeight = gpsIcon.getHeight();
        }
    }

    public void setDoDraw( boolean doDraw ) {
        this.doDraw = doDraw;
    }

    public void setLoc( GpsLocation loc ) {
        this.loc = loc;
    }

    protected void draw( final Canvas canvas, final MapView mapsView, final boolean shadow ) {
        if (loc == null || shadow || !doDraw)
            return;

        Projection pj = mapsView.getProjection();
        float lat = (float) (loc.getLatitude());
        float lon = (float) (loc.getLongitude());
        GeoPoint g = new GeoPoint(lat, lon);
        Point mapPixels = pj.toMapPixels(g, null);
        canvas.drawBitmap(gpsIcon, mapPixels.x - gpsIconWidth / 2f, mapPixels.y - gpsIconHeight / 2f, null);
    }

}
