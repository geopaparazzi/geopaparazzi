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
package eu.hydrologis.geopaparazzi.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import eu.geopaparazzi.library.features.DrawingTool;

/**
 * A slider view to draw on.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SliderDrawView extends View {
    private DrawingTool drawingTool;

    /**
     * Constructor.
     * 
     * @param context  the context to use.
     * @param attrs the attributes.
     */
    public SliderDrawView( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    @Override
    protected void onDraw( Canvas canvas ) {
        super.onDraw(canvas);
        if (drawingTool != null)
            drawingTool.onToolDraw(canvas);
    }

    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        if (drawingTool != null)
            return drawingTool.onToolTouchEvent(event);
        return false;
    }

    /**
     * Disable tool. 
     */
    public void disableTool() {
        if (drawingTool != null)
            drawingTool.disable();
        drawingTool = null;
    }

    /**
     * Enable tool.
     * 
     * <p>If a tool is already enabled, that one is disabled first.
     * Then invalidate is called on the view.
     *  
     * @param drawingTool the tool to use.
     */
    public void enableTool( DrawingTool drawingTool ) {
        if (this.drawingTool != null) {
            // first disable the current tool
            disableTool();
        }
        this.drawingTool = drawingTool;
        invalidate();
    }

    /**
     * @return the current {@link DrawingTool} or <code>null</code>.
     */
    public DrawingTool getDrawingTool() {
        return drawingTool;
    }
}
