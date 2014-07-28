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

import android.graphics.Canvas;
import android.view.MotionEvent;

/**
 * A tool that can draw.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface DrawingTool extends Tool {

    /**
     * Called when the tool should draw.
     * 
     * @param canvas the {@link Canvas} to draw on.
     */
    public void onToolDraw( Canvas canvas );

    /**
     * Called on a touch event.
     * 
     * @param event the current triggered event.
     * @return <code>true</code> if the event has been handled.
     */
    public boolean onToolTouchEvent( MotionEvent event );
}
