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
package eu.hydrologis.geopaparazzi.gpx;

import java.util.List;

import eu.hydrologis.geopaparazzi.util.PointF3D;

/**
 * Item representing a gpx file.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GpxItem implements Comparable<GpxItem> {
    private String filepath;
    private String filename;
    private String width;
    private String color;
    private boolean isLine;
    private boolean isVisible;

    private List<PointF3D> plots;
    private List<String> names;
    private float e;
    private float w;
    private float n;
    private float s;
    private float h;
    private float l;
    private float length;

    /**
     * Reads data from gpx file.
     *
     * @return the list of points, same as {@link GpxItem#getPlots()}.
     */
    public List<PointF3D> read() {
        if (plots == null) {
            IGpxParser gp = null;
            if (isLine) {
                gp = new GpxTrackParser();
            } else {
                gp = new GpxWaypointsParser();
            }
            gp.read(filepath);
            plots = gp.getPoints();
            names = gp.getNames();
            e = gp.getEastBound();
            w = gp.getWestBound();
            n = gp.getNorthBound();
            s = gp.getSouthBound();
            h = gp.getMaxElev();
            l = gp.getMinElev();
            length = gp.getLength();
        }
        return plots;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath( String filepath ) {
        this.filepath = filepath;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename( String filename ) {
        this.filename = filename;
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

    public void setLine( boolean isLine ) {
        this.isLine = isLine;
    }

    public List<PointF3D> getPlots() {
        return plots;
    }

    public List<String> getNames() {
        return names;
    }

    /**
     * Clears the data, if they were read. 
     * 
     * <p>Usefull for freeing memory.</p>
     */
    public void clear() {
        if (plots != null) {
            plots.clear();
            plots = null;
        }
        if (names != null) {
            names.clear();
            names = null;
        }
    }

    public void setVisible( boolean isVisible ) {
        this.isVisible = isVisible;
        if (!isVisible) {
            clear();
        }
    }

    public boolean isVisible() {
        return isVisible;
    }

    public int compareTo( GpxItem another ) {
        if (filepath.equals(another.filepath)) {
            return 0;
        } else {
            return filepath.compareTo(another.filepath);
        }
    }

    public float getE() {
        return e;
    }

    public float getW() {
        return w;
    }

    public float getN() {
        return n;
    }

    public float getS() {
        return s;
    }

    public float getH() {
        return h;
    }

    public float getL() {
        return l;
    }

    public float getLength() {
        return length;
    }
}
