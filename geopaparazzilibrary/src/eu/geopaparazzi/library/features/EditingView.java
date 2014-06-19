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
package eu.geopaparazzi.library.features;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A slider view to draw on.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class EditingView extends View {

    /**
     * Constructor.
     * 
     * @param context  the context to use.
     * @param attrs the attributes.
     */
    public EditingView( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    @Override
    protected void onDraw( Canvas canvas ) {
        super.onDraw(canvas);
        ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
        if (activeToolGroup instanceof DrawingTool) {
            ((DrawingTool) activeToolGroup).onToolDraw(canvas);
        }
        Tool activeTool = EditManager.INSTANCE.getActiveTool();
        if (activeTool instanceof DrawingTool) {
            ((DrawingTool) activeTool).onToolDraw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        Tool activeTool = EditManager.INSTANCE.getActiveTool();
        if (activeTool instanceof DrawingTool) {
            return ((DrawingTool) activeTool).onToolTouchEvent(event);
        }
        ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
        if (activeToolGroup instanceof DrawingTool) {
            return ((DrawingTool) activeToolGroup).onToolTouchEvent(event);
        }
        return false;
    }
}
