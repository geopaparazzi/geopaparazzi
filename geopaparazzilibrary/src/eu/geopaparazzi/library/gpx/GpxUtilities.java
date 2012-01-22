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
package eu.geopaparazzi.library.gpx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import eu.geopaparazzi.library.gpx.parser.GpxParser;
import eu.geopaparazzi.library.gpx.parser.GpxParser.Route;
import eu.geopaparazzi.library.gpx.parser.GpxParser.TrackSegment;
import eu.geopaparazzi.library.gpx.parser.WayPoint;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * Utilities to handle gpx stuff.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpxUtilities {

    public static List<GpxItem> readGpxData( Context context, String path, boolean asLines ) {
        List<GpxItem> gpxItems = new ArrayList<GpxItem>();

        File file = new File(path);
        GpxParser parser = new GpxParser(path);
        if (parser.parse()) {
            List<WayPoint> wayPoints = parser.getWayPoints();
            if (wayPoints.size() > 0) {
                String name = file.getName();
                GpxItem item = new GpxItem();
                item.setName(name);
                item.setWidth("2"); //$NON-NLS-1$
                item.setVisible(false);
                item.setColor("blue"); //$NON-NLS-1$
                item.setData(wayPoints);
                gpxItems.add(item);
            }
            List<TrackSegment> tracks = parser.getTracks();
            if (tracks.size() > 0) {
                for( TrackSegment trackSegment : tracks ) {
                    String name = trackSegment.getName();
                    GpxItem item = new GpxItem();
                    item.setName(name);
                    item.setWidth("2"); //$NON-NLS-1$
                    item.setVisible(false);
                    item.setColor("red"); //$NON-NLS-1$
                    item.setData(trackSegment);
                    gpxItems.add(item);
                }
            }
            List<Route> routes = parser.getRoutes();
            if (routes.size() > 0) {
                for( Route route : routes ) {
                    String name = route.getName();
                    GpxItem item = new GpxItem();
                    item.setName(name);
                    item.setWidth("2"); //$NON-NLS-1$
                    item.setVisible(false);
                    item.setColor("green"); //$NON-NLS-1$
                    item.setData(route);
                    gpxItems.add(item);
                }
            }
        } else {
            if (Debug.D)
                Logger.d("GPXUTILITIES", "ERROR"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return gpxItems;

    }

}
