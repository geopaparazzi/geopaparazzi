/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package org.locationtech.jts.android.geom;

import org.locationtech.jts.geom.Coordinate;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.FillRule;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.util.Collection;
import java.util.Iterator;

/**
 * A Shape which represents a polygon which may contain holes.
 * Provided because the standard AWT Polygon class does not support holes.
 *
 * <p>Adapted for android.</p>
 *
 * @author Martin Davis
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PolygonShape implements DrawableShape {
    // use a GeneralPath with a winding rule, since it supports floating point coordinates
    private Path polygonPath;
    private Path ringPath;

    /**
     * Creates a new polygon Shape.
     *
     * @param shellVertices          the vertices of the shell
     * @param holeVerticesCollection a collection of Coordinate[] for each hole
     */
    @SuppressWarnings("rawtypes")
    public PolygonShape(Coordinate[] shellVertices, Collection holeVerticesCollection) {
        polygonPath = toPath(shellVertices);

        for (Iterator i = holeVerticesCollection.iterator(); i.hasNext(); ) {
            Coordinate[] holeVertices = (Coordinate[]) i.next();
            if (holeVertices.length > 0) {
                polygonPath.moveTo((float) holeVertices[0].x, (float) holeVertices[0].y);
                for (int j = 1; j < holeVertices.length; j++) {
                    polygonPath.lineTo((float) holeVertices[j].x, (float) holeVertices[j].y);
                }
            }
        }
        polygonPath.close();
    }

    public PolygonShape() {
    }

    public void initPath() {
        polygonPath = AndroidGraphicFactory.INSTANCE.createPath();
    }

//    void addToRing(PointF p) {
//        if (ringPath == null) {
//            ringPath = new Path();
//            ringPath.setFillType(Path.FillType.EVEN_ODD);
//            ringPath.moveTo((float) p.x, (float) p.y);
//        } else {
//            ringPath.lineTo((float) p.x, (float) p.y);
//        }
//    }
//
//    void endRing() {
//        ringPath.close();
//        if (polygonPath == null) {
//            polygonPath = ringPath;
//        } else {
//            polygonPath.addPath(ringPath);
//        }
//        ringPath = null;
//    }

    /**
     * Creates a GeneralPath representing a polygon ring
     * having the given coordinate sequence.
     * Uses the GeneralPath.WIND_EVEN_ODD winding rule.
     *
     * @param coordinates a coordinate sequence
     * @return the path for the coordinate sequence
     */
    private Path toPath(Coordinate[] coordinates) {
        Path path = AndroidGraphicFactory.INSTANCE.createPath();
        path.setFillRule(FillRule.EVEN_ODD);

        if (coordinates.length > 0) {
            path.moveTo((float) coordinates[0].x, (float) coordinates[0].y);
            for (Coordinate coordinate : coordinates) {
                path.lineTo((float) coordinate.x, (float) coordinate.y);
            }
        }
        return path;
    }

    public Path getPath() {
        return polygonPath;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        paint.setStyle(Style.STROKE);
        canvas.drawPath(polygonPath, paint);
    }

    @Override
    public void fill(Canvas canvas, Paint paint) {
        paint.setStyle(Style.FILL);
        canvas.drawPath(polygonPath, paint);
    }

}
