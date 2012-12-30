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

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;

import com.vividsolutions.jts.android.geom.DrawableShape;
import com.vividsolutions.jts.android.geom.RectShape;
import com.vividsolutions.jts.geom.Geometry;

/**
 * A {@link Shape} which contains a heterogeneous collection of other shapes
 * representing JTS {@link Geometry}s.
 * 
 * <p>Modified for Android use.</p>
 * 
 * @author Martin Davis
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GeometryCollectionShape implements DrawableShape {
    private ArrayList<DrawableShape> shapes = new ArrayList<DrawableShape>();

    public GeometryCollectionShape() {
    }

    public void add( DrawableShape shape ) {
        shapes.add(shape);
    }

    public RectShape getBounds() {
        /**@todo Implement this java.awt.Shape method*/
        throw new java.lang.UnsupportedOperationException("Method getBounds() not yet implemented.");
    }

    // public Rectangle2D getBounds2D() {
    // Rectangle2D rectangle = null;
    //
    // for (Iterator i = shapes.iterator(); i.hasNext();) {
    // Shape shape = (Shape) i.next();
    //
    // if (rectangle == null) {
    // rectangle = shape.getBounds2D();
    // } else {
    // rectangle.add(shape.getBounds2D());
    // }
    // }
    //
    // return rectangle;
    // }

    public boolean contains( double x, double y ) {
        /**@todo Implement this java.awt.Shape method*/
        throw new java.lang.UnsupportedOperationException("Method contains() not yet implemented.");
    }

    public boolean contains( PointF p ) {
        /**@todo Implement this java.awt.Shape method*/
        throw new java.lang.UnsupportedOperationException("Method contains() not yet implemented.");
    }

    public boolean intersects( float x, float y, float w, float h ) {
        /**@todo Implement this java.awt.Shape method*/
        throw new java.lang.UnsupportedOperationException("Method intersects() not yet implemented.");
    }

    public boolean intersects( RectF r ) {
        /**@todo Implement this java.awt.Shape method*/
        throw new java.lang.UnsupportedOperationException("Method intersects() not yet implemented.");
    }

    public boolean contains( float x, float y, float w, float h ) {
        /**@todo Implement this java.awt.Shape method*/
        throw new java.lang.UnsupportedOperationException("Method contains() not yet implemented.");
    }

    public boolean contains( RectF r ) {
        /**@todo Implement this java.awt.Shape method*/
        throw new java.lang.UnsupportedOperationException("Method contains() not yet implemented.");
    }

    public void clip( Canvas arg0 ) {
        throw new RuntimeException("not implemented yet");

    }

    public boolean contains( RectShape arg0 ) {
        throw new RuntimeException("not implemented yet");
    }

    public boolean contains( float arg0, float arg1 ) {
        throw new RuntimeException("not implemented yet");
    }

    public void draw( Canvas canvas, Paint paint ) {
        for( DrawableShape shape : shapes ) {
            shape.draw(canvas, paint);
        }
    }

    public void fill( Canvas canvas, Paint paint ) {
        for( DrawableShape shape : shapes ) {
            shape.fill(canvas, paint);
        }
    }

    public void fillAndStroke( Canvas canvas, Paint paint ) {
        for( DrawableShape shape : shapes ) {
            shape.fillAndStroke(canvas, paint);
        }
    }

    public void getBounds( RectShape arg0 ) {
        throw new RuntimeException("not implemented yet");

    }

    public Path getPath() {
        throw new RuntimeException("not implemented yet");
    }

    public boolean intersects( Rect arg0 ) {
        throw new RuntimeException("not implemented yet");
    }

    public boolean intersects( RectShape arg0 ) {
        throw new RuntimeException("not implemented yet");
    }

    public void translate( float arg0, float arg1 ) {
        throw new RuntimeException("not implemented yet");

    }

    public Shape clone() {
        try {
            return (Shape) super.clone();
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    // public PathIterator getPathIterator(AffineTransform at) {
    // return new ShapeCollectionPathIterator(shapes, at);
    // }
    //
    // public PathIterator getPathIterator(AffineTransform at, double flatness) {
    // // since Geometry is linear, can simply delegate to the simple method
    // return getPathIterator(at);
    // }
}
