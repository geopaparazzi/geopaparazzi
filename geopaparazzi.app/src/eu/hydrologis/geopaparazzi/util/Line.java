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

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.gpx.GpxRepresenter;
import eu.geopaparazzi.library.gpx.GpxUtilities;
import eu.geopaparazzi.library.kml.KmlRepresenter;
import eu.geopaparazzi.library.util.ColorUtilities;
import eu.geopaparazzi.library.util.DynamicDoubleArray;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Represents a line (log or map).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Line implements KmlRepresenter, GpxRepresenter {

    private String name;
    private DynamicDoubleArray latList;
    private DynamicDoubleArray lonList;
    private DynamicDoubleArray altimList;
    private List<String> dateList;
    private boolean boundsAreDirty = true;
    private double minLat = 0.0;
    private double minLon = 0.0;
    private double maxLat = 0.0;
    private double maxLon = 0.0;

    private float width = 1f;
    private String color = "#ff0000ff"; //$NON-NLS-1$

    /**
     * @param name line name.
     * @param lonList lon coords.
     * @param latList lat coords.
     * @param altimList elevation list.
     * @param dateList date list.
     */
    public Line( String name, DynamicDoubleArray lonList, DynamicDoubleArray latList, DynamicDoubleArray altimList,
            List<String> dateList ) {
        this.name = name;
        this.lonList = lonList;
        this.latList = latList;
        this.altimList = altimList;
        this.dateList = dateList;
    }

    /**
     * Empty line constructor.
     * 
     * @param logid log id.
     */
    public Line( String logid ) {
        this.name = logid;
        this.lonList = new DynamicDoubleArray();
        this.latList = new DynamicDoubleArray();
        this.altimList = new DynamicDoubleArray();
        this.dateList = new ArrayList<String>();
    }

    /**
     * @param lon lon
     * @param lat lat
     * @param altim elevation.
     * @param date date.
     */
    public void addPoint( double lon, double lat, double altim, String date ) {
        if (lat < 0.0001 && lon < 0.0001) {
            // don't add points in 0,0
            return;
        }
        boundsAreDirty = true;
        this.lonList.add(lon);
        this.latList.add(lat);
        this.altimList.add(altim);
        this.dateList.add(date);
    }

    /**
     * Set the style.
     * 
     * @param width width.
     * @param color color.
     */
    public void setStyle( float width, String color ) {
        if (width > 0)
            this.width = width;
        if (color != null)
            this.color = color;
    }

    /**
     * Set the name.
     * 
     * @param name the name.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return line name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return lat list
     */
    public DynamicDoubleArray getLatList() {
        return latList;
    }

    /**
     * @return lon list
     */
    public DynamicDoubleArray getLonList() {
        return lonList;
    }

    /**
     * @return elevations list.
     */
    public DynamicDoubleArray getAltimList() {
        return altimList;
    }

    /**
     * @return dates list.
     */
    public List<String> getDateList() {
        return dateList;
    }

    /**
     * Calculates the length of a line.
     * 
     * @return the length of the line in meters.
     */
    public double getLength() {
        final float[] dist = new float[3];
        double length = 0;
        for( int i = 0; i < latList.size() - 1; i++ ) {
            double lat1 = latList.get(i);
            double lon1 = lonList.get(i);
            double altim1 = altimList.get(i);
            double lat2 = latList.get(i + 1);
            double lon2 = lonList.get(i + 1);
            double altim2 = altimList.get(i + 1);
            Location.distanceBetween(lat1, lon1, lat2, lon2, dist);

            double deltaAltim = abs(altim2 - altim1);
            double deltaLength = sqrt(pow(deltaAltim, 2.0) + pow(dist[0], 2.0));
            length = length + deltaLength;

        }
        return length;
    }

    @SuppressWarnings("nls")
    public String toKmlString() {
        String name = Utilities.makeXmlSafe(this.name);
        StringBuilder sB = new StringBuilder();
        sB.append("<Placemark>\n");
        sB.append("<name>" + name + "</name>\n");
        sB.append("<visibility>1</visibility>\n");
        sB.append("<LineString>\n");
        sB.append("<tessellate>1</tessellate>\n");
        sB.append("<coordinates>\n");
        for( int i = 0; i < lonList.size(); i++ ) {
            double lon = lonList.get(i);
            double lat = latList.get(i);
            sB.append(lon).append(",").append(lat).append(",1 \n");
        }
        sB.append("</coordinates>\n");
        sB.append("</LineString>\n");
        sB.append("<Style>\n");
        sB.append("<LineStyle>\n");
        int parsedColor = ColorUtilities.toColor(color);
        String hexColor = "#" + Integer.toHexString(parsedColor);
        sB.append("<color>").append(hexColor).append("</color>\n");
        sB.append("<width>").append(width).append("</width>\n");
        sB.append("</LineStyle>\n");
        sB.append("</Style>\n");
        sB.append("</Placemark>\n");

        return sB.toString();
    }

    public boolean hasImages() {
        return false;
    }

    public List<String> getImagePaths() {
        return null;
    }

    private void calculateBounds() {
        if (boundsAreDirty) {
            double[] latArray = latList.getInternalArray();
            int size = latList.size();
            minLat = Double.POSITIVE_INFINITY;
            maxLat = Double.NEGATIVE_INFINITY;
            for( int i = 0; i < size; i++ ) {
                double d = latArray[i];
                minLat = Math.min(d, minLat);
                maxLat = Math.max(d, maxLat);
            }
            double[] lonArray = lonList.getInternalArray();
            minLon = Double.POSITIVE_INFINITY;
            maxLon = Double.NEGATIVE_INFINITY;
            for( int i = 0; i < size; i++ ) {
                double d = lonArray[i];
                minLon = Math.min(d, minLon);
                maxLon = Math.max(d, maxLon);
            }
            boundsAreDirty = false;
        }
    }

    public double getMinLat() {
        calculateBounds();
        return minLat;
    }

    public double getMinLon() {
        calculateBounds();
        return minLon;
    }

    public double getMaxLat() {
        calculateBounds();
        return maxLat;
    }

    public double getMaxLon() {
        calculateBounds();
        return maxLon;
    }

    @SuppressWarnings("nls")
    public String toGpxString() throws Exception {

        String name = Utilities.makeXmlSafe(this.name);
        StringBuilder sb = new StringBuilder();
        sb.append(GpxUtilities.GPX_TRACK_START).append("\n");
        sb.append(GpxUtilities.getTrackNameString(name)).append("\n");
        sb.append(GpxUtilities.GPX_TRACKSEGMENT_START).append("\n");
        int size = latList.size();
        double[] latArray = latList.getInternalArray();
        double[] lonArray = lonList.getInternalArray();
        double[] altimArray = altimList.getInternalArray();
        for( int i = 0; i < size; i++ ) {
            String dateString = dateList.get(i);
            // TODO change this sooner or later - needs ts to be hold differently in db
            dateString = TimeUtilities.INSTANCE.TIME_FORMATTER_GPX_UTC.format(TimeUtilities.INSTANCE.TIME_FORMATTER_SQLITE_UTC
                    .parse(dateString));
            String trackPointString = GpxUtilities.getTrackPointString(latArray[i], lonArray[i], altimArray[i], dateString);
            sb.append(trackPointString);
        }
        sb.append(GpxUtilities.GPX_TRACKSEGMENT_END).append("\n");
        sb.append(GpxUtilities.GPX_TRACK_END).append("\n");
        return sb.toString();
    }
}
