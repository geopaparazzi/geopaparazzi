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
package com.vividsolutions.jts.android;

import java.util.Collection;
import java.util.Iterator;

import org.afree.graphics.geom.PathShape;
import org.afree.graphics.geom.RectShape;
import org.afree.graphics.geom.Shape;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A {@link Shape} which represents a polygon which may contain holes.
 * Provided because the standard AWT Polygon class does not support holes.
 * 
 * @author Martin Davis
 *
 */
public class PolygonShape implements Shape {
    // use a GeneralPath with a winding rule, since it supports floating point coordinates
    private Path polygonPath;
    private Path ringPath;

    /**
     * Creates a new polygon {@link Shape}.
     * 
     * @param shellVertices the vertices of the shell 
     * @param holeVerticesCollection a collection of Coordinate[] for each hole
     */
    public PolygonShape( Coordinate[] shellVertices, Collection holeVerticesCollection ) {
        polygonPath = toPath(shellVertices);

        for( Iterator i = holeVerticesCollection.iterator(); i.hasNext(); ) {
            Coordinate[] holeVertices = (Coordinate[]) i.next();
            polygonPath.addPath(toPath(holeVertices));
        }
    }

    public PolygonShape() {
    }
    
    public void initPath(){
        polygonPath = new Path();
    }
    
    
    void addToRing( PointF p ) {
        if (ringPath == null) {
            ringPath = new Path();
            ringPath.setFillType(Path.FillType.EVEN_ODD);
            ringPath.moveTo((float) p.x, (float) p.y);
        } else {
            ringPath.lineTo((float) p.x, (float) p.y);
        }
    }

    void endRing() {
        ringPath.close();
        if (polygonPath == null) {
            polygonPath = ringPath;
        } else {
            polygonPath.addPath(ringPath);
        }
        ringPath = null;
    }

    /**
     * Creates a GeneralPath representing a polygon ring 
     * having the given coordinate sequence.
     * Uses the GeneralPath.WIND_EVEN_ODD winding rule.
     * 
     * @param coordinates a coordinate sequence
     * @return the path for the coordinate sequence
     */
    private Path toPath( Coordinate[] coordinates ) {
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);

        if (coordinates.length > 0) {
            path.moveTo((float) coordinates[0].x, (float) coordinates[0].y);
            for( int i = 0; i < coordinates.length; i++ ) {
                path.lineTo((float) coordinates[i].x, (float) coordinates[i].y);
            }
        }
        return path;
    }

    public RectShape getBounds() {
        RectF bounds = new RectF();
        polygonPath.computeBounds(bounds, false);
        RectShape r = new RectShape(bounds);
        return r;
    }

    // public Rectangle2D getBounds2D() {
    // RectF bounds = new RectF();
    // polygonPath.computeBounds(bounds, false);
    // RectShape r = new RectShape(bounds);
    // return r;
    // }

    public boolean contains( double x, double y ) {
        PathShape pShape = new PathShape(polygonPath);
        return pShape.contains((float) x, (float) y);
    }

    public boolean contains( PointF p ) {
        PathShape pShape = new PathShape(polygonPath);
        return pShape.contains(p);
    }

    public boolean intersects( float x, float y, float w, float h ) {
        PathShape pShape = new PathShape(polygonPath);
        return pShape.intersects(x, y, w, h);
    }

    public boolean intersects( RectF r ) {
        PathShape pShape = new PathShape(polygonPath);
        RectShape rect = new RectShape(r);
        return pShape.intersects(rect);
    }

    public boolean contains( float x, float y, float w, float h ) {
        PathShape pShape = new PathShape(polygonPath);
        return pShape.contains(x, y, w, h);
    }

    public boolean contains( RectF r ) {
        PathShape pShape = new PathShape(polygonPath);
        RectShape rect = new RectShape(r);
        return pShape.contains(rect);
    }

//    public PathIterator getPathIterator( AffineTransform at ) {
//        return polygonPath.getPathIterator(at);
//    }
//
//    public PathIterator getPathIterator( AffineTransform at, double flatness ) {
//        return getPathIterator(at, flatness);
//    }

    @Override
    public Shape clone()  {
        try {
            return (Shape) super.clone();
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void clip( Canvas arg0 ) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public boolean contains( RectShape arg0 ) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public boolean contains( float arg0, float arg1 ) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public void draw( Canvas canvas, Paint paint) {
        canvas.drawPath(polygonPath, paint);
    }

    @Override
    public void fill( Canvas canvas, Paint paint) {
        canvas.drawPath(polygonPath, paint);
    }

    @Override
    public void fillAndStroke( Canvas canvas, Paint paint) {
        canvas.drawPath(polygonPath, paint);
    }

    @Override
    public void getBounds( RectShape arg0 ) {
        throw new RuntimeException("not implemented yet");
        
    }

    @Override
    public Path getPath() {
        return polygonPath;
    }

    @Override
    public boolean intersects( Rect arg0 ) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public boolean intersects( RectShape arg0 ) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public void translate( float arg0, float arg1 ) {
        throw new RuntimeException("not implemented yet");
        
    }

}
