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

package eu.hydrologis.geopaparazzi.maps.overlays;

import android.graphics.drawable.Drawable;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.GeoPoint;

/**
 * Wrapper shell.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NoteOverlayItem extends OverlayItem {

    /**
     * Constructor.
     * 
     * @param geoPoint the point.
     * @param text a text.
     * @param string a string.
     * @param marker teh marker to use.
     */
    public NoteOverlayItem( GeoPoint geoPoint, String text, String string, Drawable marker ) {
        super(geoPoint, text, string, marker);
    }

}
