///*
// * Geopaparazzi - Digital field mapping on Android based devices
// * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package eu.hydrologis.geopaparazzi.util;
//
//
///**
// * Represents a line in 2d based on screen coords.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class LineScreenArray {
//
//    private final String fileName;
//
//    private int[] xArray;
//    private int[] yArray;
//    private int index = 0;
//
//    private int maxArraySize = 100;
//
//    public LineScreenArray( String logid, int initialCount ) {
//        this.fileName = logid;
//        maxArraySize = initialCount;
//        xArray = new int[maxArraySize];
//        yArray = new int[maxArraySize];
//    }
//
//    public LineScreenArray( String logid ) {
//        this(logid, 100);
//    }
//
//    public void addPoint( int x, int y ) {
//        if (index == maxArraySize) {
//            // enlarge array
//            int[] tmpLon = new int[maxArraySize + 100];
//            int[] tmpLat = new int[maxArraySize + 100];
//            System.arraycopy(xArray, 0, tmpLon, 0, maxArraySize);
//            System.arraycopy(yArray, 0, tmpLat, 0, maxArraySize);
//            xArray = tmpLon;
//            yArray = tmpLat;
//            maxArraySize = maxArraySize + 100;
//        }
//
//        xArray[index] = x;
//        yArray[index] = y;
//        index++;
//
//    }
//
//    public String getfileName() {
//        return fileName;
//    }
//
//    public int[] getLatArray() {
//        return yArray;
//    }
//
//    public int[] getLonArray() {
//        return xArray;
//    }
//
//    public int getIndex() {
//        return index;
//    }
//
// }
