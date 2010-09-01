/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.geopaparazzi.util;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

/**
 * Represents a line (log or map).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Line {

    private final String fileName;
    private final List<Double> latList;
    private final List<Double> lonList;
    private final List<Double> altimList;
    private final List<String> dateList;

    public Line( String fileName, List<Double> lonList, List<Double> latList, List<Double> altimList, List<String> dateList ) {
        this.fileName = fileName;
        this.lonList = lonList;
        this.latList = latList;
        this.altimList = altimList;
        this.dateList = dateList;

    }

    public Line( String logid ) {
        this.fileName = logid;
        this.lonList = new ArrayList<Double>();
        this.latList = new ArrayList<Double>();
        this.altimList = new ArrayList<Double>();
        this.dateList = new ArrayList<String>();
    }

    public void addPoint( double lon, double lat, double altim, String date ) {
        this.lonList.add(lon);
        this.latList.add(lat);
        this.altimList.add(altim);
        this.dateList.add(date);
    }

    public String getfileName() {
        return fileName;
    }

    public List<Double> getLatList() {
        return latList;
    }

    public List<Double> getLonList() {
        return lonList;
    }

    public List<Double> getAltimList() {
        return altimList;
    }

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
            double lat2 = latList.get(i + 1);
            double lon2 = lonList.get(i + 1);
            Location.distanceBetween(lat1, lon1, lat2, lon2, dist);
            length = length + dist[0];
        }
        return length;
    }

    @SuppressWarnings("nls")
    public String toKmlString() {
        StringBuilder sB = new StringBuilder();
        sB.append("<Placemark>\n");
        sB.append("<name>" + fileName + "</name>\n");
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
        sB.append("</Placemark>\n");

        return sB.toString();
    }
}
