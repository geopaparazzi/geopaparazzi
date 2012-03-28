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

import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * Overlay to show gps notes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NotesItemizedOverlay extends ArrayItemizedOverlay {
    private final Context context;

    /**
     * Constructs a new NotesItemizedOverlay.
     * 
     * @param defaultMarker
     *            the default marker (may be null).
     * @param context
     *            the reference to the application context.
     */
    public NotesItemizedOverlay( Drawable defaultMarker, Context context ) {
        super(defaultMarker);
        this.context = context;
    }

    /**
     * Handles a tap event on the given item.
     */
    @Override
    protected boolean onTap( int index ) {
        OverlayItem item = createItem(index);
        if (item != null) {
            Builder builder = new AlertDialog.Builder(this.context);
            builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setTitle(item.getTitle());
            builder.setMessage(item.getSnippet());
            builder.setPositiveButton("OK", null);
            builder.show();
        }
        return true;
    }
}