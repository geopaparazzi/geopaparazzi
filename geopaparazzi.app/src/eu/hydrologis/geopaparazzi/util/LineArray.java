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

import eu.hydrologis.geopaparazzi.util.debug.Logger;
import android.location.Location;

/**
 * Represents a line in 2d based on float arrays.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LineArray {

    private final String fileName;

    private float[] lonArray;
    private float[] latArray;
    private int index = 0;

    private int maxArraySize = 100;

    public LineArray( String logid, int initialCount ) {
        this.fileName = logid;
        maxArraySize = initialCount;
        lonArray = new float[maxArraySize];
        latArray = new float[maxArraySize];
    }

    public LineArray( String logid ) {
        this(logid, 100);
    }

    public void addPoint( float lon, float lat ) {
        if (index == maxArraySize) {
            // enlarge array
            float[] tmpLon = new float[maxArraySize + 100];
            float[] tmpLat = new float[maxArraySize + 100];
            System.arraycopy(lonArray, 0, tmpLon, 0, maxArraySize);
            System.arraycopy(latArray, 0, tmpLat, 0, maxArraySize);
            lonArray = tmpLon;
            latArray = tmpLat;
            maxArraySize = maxArraySize + 100;
            Logger.d(this, "New line size: " + lonArray.length);
        }

        lonArray[index] = lon;
        latArray[index] = lat;
        index++;

    }

    public String getfileName() {
        return fileName;
    }

    public float[] getLatArray() {
        return latArray;
    }

    public float[] getLonArray() {
        return lonArray;
    }

    public int getIndex() {
        return index;
    }

    /**
     * Calculates the length of a line.
     * 
     * @return the length of the line in meters.
     */
    public float getLength() {
        final float[] dist = new float[3];
        float length = 0;
        for( int i = 0; i < lonArray.length - 1; i++ ) {
            float lon1 = lonArray[i];
            float lat1 = latArray[i];
            float lon2 = lonArray[i + 1];
            float lat2 = latArray[i + 1];
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
        for( int i = 0; i < lonArray.length; i++ ) {
            float lon = lonArray[i];
            float lat = latArray[i];
            sB.append(lon).append(",").append(lat).append(",1 \n");
        }
        sB.append("</coordinates>\n");
        sB.append("</LineString>\n");
        sB.append("</Placemark>\n");

        return sB.toString();
    }
}
