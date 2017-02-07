/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.core.utilities;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Applies a sliding average on linear geometries for smoothing.
 * <p>
 * <p>
 * See: http://grass.osgeo.org/wiki/V.generalize_tutorial
 * </p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureSlidingAverage {

    private static final double DELTA = 0.00001;
    private final List<Coordinate> data;

    public FeatureSlidingAverage(List<Coordinate> data) {
        this.data = data;
    }

    public List<Coordinate> smooth(int lookAhead, boolean considerZ, double slide) {

        double sc;
        List<Coordinate> res = new ArrayList<Coordinate>();

        Coordinate[] coordinates = data.toArray(new Coordinate[data.size()]);
        int n = coordinates.length;

        if (n < 4 * lookAhead) {
            /*
             * if lookahead is too large, lets put it as the
             * 20% of the number of coordinates
             */
            lookAhead = (int) Math.floor(n * 0.2d);
        }

        if (lookAhead % 2 == 0) {
            lookAhead++;
        }
        if (lookAhead < 3)
            return null;

        int halfLookAhead = lookAhead / 2;
        if (halfLookAhead > coordinates.length) {
            throw new RuntimeException();
        }

        int padding = 0;
        if (coordinates[0].distance(coordinates[n - 1]) < DELTA) {
            // we have a ring, extend it for smoothing
            int tmpN = lookAhead / 2;
            if (tmpN > n / 2) {
                tmpN = n / 2;
            }
            padding = tmpN;
            Coordinate[] ringCoordinates = new Coordinate[n + 2 * tmpN];
            for (int i = 0; i < tmpN; i++) {
                ringCoordinates[i] = coordinates[n - (tmpN - i) - 1];
            }
            System.arraycopy(coordinates, 0, ringCoordinates, tmpN, n);
            int index = 1;
            for (int i = ringCoordinates.length - padding; i < ringCoordinates.length; i++) {
                ringCoordinates[i] = coordinates[index++];
            }
            coordinates = ringCoordinates;
        }
        n = n + 2 * padding;

        for (int j = 0; j < n; j++) {
            Coordinate tmp = new Coordinate();
            res.add(tmp);
        }

        sc = 1.0 / (double) lookAhead;

        Coordinate pCoord = new Coordinate();
        Coordinate sCoord = new Coordinate();
        pointAssign(coordinates, 0, considerZ, pCoord);
        for (int i = 1; i < lookAhead; i++) {
            Coordinate tmpCoord = new Coordinate();
            pointAssign(coordinates, i, considerZ, tmpCoord);
            pointAdd(pCoord, tmpCoord, pCoord);
        }

        /* and calculate the average of remaining points */
        for (int i = halfLookAhead; i + halfLookAhead < n; i++) {
            Coordinate tmpCoord = new Coordinate();
            pointAssign(coordinates, i, considerZ, sCoord);
            pointScalar(sCoord, 1.0 - slide, sCoord);
            pointScalar(pCoord, sc * slide, tmpCoord);
            pointAdd(tmpCoord, sCoord, res.get(i));
            if (i + halfLookAhead + 1 < n) {
                pointAssign(coordinates, i - halfLookAhead, considerZ, tmpCoord);
                pointSubtract(pCoord, tmpCoord, pCoord);
                pointAssign(coordinates, i + halfLookAhead + 1, considerZ, tmpCoord);
                pointAdd(pCoord, tmpCoord, pCoord);
            }
        }

        for (int i = 0; i < halfLookAhead; i++) {
            Coordinate coordinate = res.get(i);
            coordinate.x = coordinates[i].x;
            coordinate.y = coordinates[i].y;
            coordinate.z = coordinates[i].z;
        }
        for (int i = n - halfLookAhead - 1; i < n; i++) {
            Coordinate coordinate = res.get(i);
            coordinate.x = coordinates[i].x;
            coordinate.y = coordinates[i].y;
            coordinate.z = coordinates[i].z;
        }

        if (padding != 0) {
            res = res.subList(padding, n - padding - 1);
            res.add(res.get(0));
        }

        return res;
    }

    private void pointAssign(Coordinate[] coordinates, int index, boolean considerZ,
                             Coordinate newAssignedCoordinate) {
        Coordinate coordinate = coordinates[index];
        newAssignedCoordinate.x = coordinate.x;
        newAssignedCoordinate.y = coordinate.y;
        if (considerZ) {
            newAssignedCoordinate.z = coordinate.z;
        } else {
            newAssignedCoordinate.z = 0;
        }
        return;
    }

    private void pointAdd(Coordinate a, Coordinate b, Coordinate res) {
        res.x = a.x + b.x;
        res.y = a.y + b.y;
        res.z = a.z + b.z;
    }

    private void pointSubtract(Coordinate a, Coordinate b, Coordinate res) {
        res.x = a.x - b.x;
        res.y = a.y - b.y;
        res.z = a.z - b.z;
    }

    private void pointScalar(Coordinate a, double k, Coordinate res) {
        res.x = a.x * k;
        res.y = a.y * k;
        res.z = a.z * k;
    }

}
