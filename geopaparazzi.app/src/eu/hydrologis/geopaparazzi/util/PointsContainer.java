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

/**
 * A container for points.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PointsContainer {

    private final String fileName;
    private String[] namesArray;
    private float[] lonArray;
    private float[] latArray;
    private int index = 0;

    private int maxArraySize = 100;

    public PointsContainer( String logid, int initialCount ) {
        this.fileName = logid;
        maxArraySize = initialCount;
        lonArray = new float[maxArraySize];
        latArray = new float[maxArraySize];
        namesArray = new String[maxArraySize];
    }
    public PointsContainer( String logid ) {
        this(logid, 100);
    }

    public void addPoint( float lon, float lat, String name ) {
        if (index == maxArraySize) {
            // enlarge array
            float[] tmpLon = new float[maxArraySize + 100];
            float[] tmpLat = new float[maxArraySize + 100];
            String[] tmpNames = new String[maxArraySize + 100];
            System.arraycopy(lonArray, 0, tmpLon, 0, maxArraySize);
            System.arraycopy(latArray, 0, tmpLat, 0, maxArraySize);
            System.arraycopy(namesArray, 0, tmpNames, 0, maxArraySize);
            lonArray = tmpLon;
            latArray = tmpLat;
            namesArray = tmpNames;
            maxArraySize = maxArraySize + 100;
        }

        lonArray[index] = lon;
        latArray[index] = lat;
        if (name != null) {
            namesArray[index] = name;
        } else {
            namesArray[index] = ""; //$NON-NLS-1$
        }
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

    public String[] getNamesArray() {
        return namesArray;
    }

    public int getIndex() {
        return index;
    }

}
