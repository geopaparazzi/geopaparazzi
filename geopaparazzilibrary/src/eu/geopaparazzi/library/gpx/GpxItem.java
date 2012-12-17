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

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.gpx.parser.GpxParser.Route;
import eu.geopaparazzi.library.gpx.parser.GpxParser.TrackSegment;
import eu.geopaparazzi.library.gpx.parser.WayPoint;

/**
 * Item representing a gpx file.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpxItem implements Comparable<GpxItem> {
    private String name;
    private String width;
    private String color;
    private boolean isLine;
    private boolean isVisible;

    private List<WayPoint> wayPoints = new ArrayList<WayPoint>();
    private List<TrackSegment> trackSegments = new ArrayList<TrackSegment>();
    private List<Route> routes = new ArrayList<Route>();

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth( String width ) {
        this.width = width;
    }

    public String getColor() {
        return color;
    }

    public void setColor( String color ) {
        this.color = color;
    }

    public boolean isLine() {
        return isLine;
    }

    public void setVisible( boolean isVisible ) {
        this.isVisible = isVisible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public int compareTo( GpxItem another ) {
        return 0;
    }

    @SuppressWarnings("unchecked")
    public void setData( Object data ) {
        if (data instanceof List< ? >) {
            wayPoints = (List<WayPoint>) data;
        }
        if (data instanceof TrackSegment) {
            TrackSegment segment = (TrackSegment) data;
            trackSegments.add(segment);
        }
        if (data instanceof Route) {
            Route route = (Route) data;
            routes.add(route);
        }
    }

    public List<WayPoint> getWayPoints() {
        return wayPoints;
    }

    public List<TrackSegment> getTrackSegments() {
        return trackSegments;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    // public int compareTo( GpxItem another ) {
    // if (filepath.equals(another.filepath)) {
    // return 0;
    // } else {
    // return filepath.compareTo(another.filepath);
    // }
    // }
    //
    // @Override
    // public boolean equals( Object o ) {
    // if (o instanceof GpxItem) {
    // GpxItem other = (GpxItem) o;
    // return filepath.equals(other.filepath);
    // }
    // return false;
    // }
}
