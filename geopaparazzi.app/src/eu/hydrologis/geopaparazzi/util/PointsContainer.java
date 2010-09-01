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

import java.util.ArrayList;
import java.util.List;

/**
 * A container for points.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class PointsContainer {

    private final String fileName;
    private final List<Double> latList;
    private final List<Double> lonList;
    private final List<Double> altimList;
    private final List<String> dateList;
    private final List<String> namesList;

    public PointsContainer( String fileName, List<Double> lonList, List<Double> latList, List<Double> altimList,
            List<String> dateList, List<String> namesList ) {
        this.fileName = fileName;
        this.lonList = lonList;
        this.latList = latList;
        this.altimList = altimList;
        this.dateList = dateList;
        this.namesList = namesList;

    }

    public PointsContainer( String logid ) {
        this.fileName = logid;
        this.lonList = new ArrayList<Double>();
        this.latList = new ArrayList<Double>();
        this.altimList = new ArrayList<Double>();
        this.dateList = new ArrayList<String>();
        this.namesList = new ArrayList<String>();
    }

    public void addPoint( double lon, double lat, double altim, String date, String name ) {
        this.lonList.add(lon);
        this.latList.add(lat);
        this.altimList.add(altim);
        this.dateList.add(date);
        if (name != null) {
            this.namesList.add(name);
        }
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

    public List<String> getNamesList() {
        return namesList;
    }

}
