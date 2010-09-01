package eu.hydrologis.geopaparazzi.gpx;

import java.util.List;

import eu.hydrologis.geopaparazzi.util.PointF3D;

public interface IGpxParser {

    /*
     * READ GPX DATA FILE
     */
    public abstract int read( String filename );

    public abstract List<PointF3D> getPoints();

    public abstract List<String> getNames();

    public abstract float getNorthBound();
    public abstract float getSouthBound();
    public abstract float getEastBound();
    public abstract float getWestBound();
    public abstract float getMaxElev();
    public abstract float getMinElev();
    public abstract float getLength();

}