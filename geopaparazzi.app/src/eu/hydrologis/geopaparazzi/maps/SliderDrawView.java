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
import eu.hydrologis.geopaparazzi.maptools.core.MapTool;

/**
 * A slider view to draw on.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SliderDrawView extends View {
    private MapTool mapTool;

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
        if (mapTool != null)
            mapTool.onToolDraw(canvas);
    }

    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        if (mapTool != null)
            return mapTool.onToolTouchEvent(event);
        return true;
    }

    /**
     * Disable tool. 
     */
    public void disableTool() {
        mapTool.disable();
        mapTool = null;
    }

    /**
     * Enable tool.
     *  
     * @param mapTool the tool to use.
     */
    public void enableTool( MapTool mapTool ) {
        this.mapTool = mapTool;
    }
}
