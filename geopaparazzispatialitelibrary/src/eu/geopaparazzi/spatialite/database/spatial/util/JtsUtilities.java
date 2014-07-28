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
package eu.geopaparazzi.spatialite.database.spatial.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities class for JTS.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class JtsUtilities {

    private static final double DELTA = 0.0000001;
    private static GeometryFactory gf = new GeometryFactory();

    /**
     * Create a {@link Polygon} from a list of coordinates.
     * 
     * @param coordinatesList the list of coordinates.
     * @return the created polygon.
     */
    public static Polygon createPolygon( List<Coordinate> coordinatesList ) {
        coordinatesList = new ArrayList<Coordinate>(coordinatesList);
        Coordinate firstCoord = coordinatesList.get(0);
        Coordinate lastCoord = coordinatesList.get(coordinatesList.size() - 1);
        if (firstCoord.distance(lastCoord) > DELTA) {
            coordinatesList.add(firstCoord);
        }
        Polygon polygon = gf.createPolygon(coordinatesList.toArray(new Coordinate[0]));
        return polygon;
    }

    /**
     * Create {@link MultiPoint}s from a list of coordinates.
     * 
     * @param coordinatesList the list of coordinates.
     * @return the created points.
     */
    public static MultiPoint createPoints( List<Coordinate> coordinatesList ) {
        MultiPoint multiPoints = gf.createMultiPoint(coordinatesList.toArray(new Coordinate[0]));
        return multiPoints;
    }

    /**
     * Create vertex points as polygons from a list of coordinates.
     * 
     * @param coordinatesList the list of coordinates.
     * @return the created points.
     */
    public static Geometry createVertexBuffers( List<Coordinate> coordinatesList ) {
        MultiPoint multiPoints = gf.createMultiPoint(coordinatesList.toArray(new Coordinate[0]));
        Geometry buffer = multiPoints.buffer(0.0001);
        return buffer;
    }

}
