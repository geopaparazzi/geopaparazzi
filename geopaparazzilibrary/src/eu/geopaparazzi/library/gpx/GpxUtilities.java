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
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gpx.parser.GpxParser;
import eu.geopaparazzi.library.gpx.parser.GpxParser.Route;
import eu.geopaparazzi.library.gpx.parser.GpxParser.TrackSegment;
import eu.geopaparazzi.library.gpx.parser.WayPoint;

/**
 * Utilities to handle gpx stuff.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class GpxUtilities {

    /**
     * 
     */
    public static final String GPX_TRACK_START = "<trk>";
    /**
     * 
     */
    public static final String GPX_TRACK_END = "</trk>";
    /**
     * 
     */
    public static final String GPX_TRACKSEGMENT_START = "<trkseg>";
    /**
     * 
     */
    public static final String GPX_TRACKSEGMENT_END = "</trkseg>";

    /**
     * Creates a Waypoint string from the point values.
     * 
     * @param lat latitude of the point.
     * @param lon longitude of the point.
     * @param elev elevation of the point.
     * @param name the name of the point.
     * @param desc a description of the point.
     * @return the waypoint string.
     */
    public static String getWayPointString( double lat, double lon, double elev, String name, String desc ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<wpt lat=\"" + lat + "\" lon=\"" + lon + "\">").append("\n");
        sb.append("  <ele>" + elev + "</ele>").append("\n");
        sb.append("  <name>" + name + "</name>").append("\n");
        sb.append("  <cmt>" + desc + "</cmt>").append("\n");
        sb.append("  <desc>" + desc + "</desc>").append("\n");
        sb.append("</wpt>").append("\n");
        return sb.toString();
    }

    /**
     * Creates a Trackpoint string from the point values.
     * 
     * @param lat latitude of the point.
     * @param lon longitude of the point.
     * @param elev elevation of the point.
     * @param time the time at which the point was taken.
     * @return the trackpoint string.
     */
    public static String getTrackPointString( double lat, double lon, double elev, String time ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<trkpt lat=\"" + lat + "\" lon=\"" + lon + "\">").append("\n");
        sb.append("  <ele>" + elev + "</ele>").append("\n");
        sb.append("  <time>" + time + "</time>").append("\n");
        sb.append("</trkpt>").append("\n");
        return sb.toString();
    }

    /**
     * Creates a Track name string from the name.
     * 
     * @param name the name of the track.
     * @return the gpx string.
     */
    public static String getTrackNameString( String name ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<name>" + name + "</name>").append("\n");
        return sb.toString();
    }

    /**
     * Read gpx data.
     * 
     * @param context  the context to use.
     * @param path the string data.
     * @param asLines if <code>true</code>, the data are read as lines.
     * @return list of {@link GpxItem}s.
     */
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
            GPLog.error("GPXUTILITIES", "ERROR", new RuntimeException()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return gpxItems;

    }

}
