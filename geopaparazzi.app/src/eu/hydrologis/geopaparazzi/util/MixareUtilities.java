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
package eu.hydrologis.geopaparazzi.util;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.mixare.MixareHandler;
import eu.geopaparazzi.library.util.PointF3D;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.DaoNotes;

/**
 * Utilities for interaction with the Mixare project. 
 * 
 * <p>
 * A mixare json object looks like:
 * <pre>
 * {
 *     "id": "2827",
 *     "lat": "46.43893",
 *     "lng": "11.21706",
 *     "elevation": "1737",
 *     "title": "Penegal",
 *     "distance": "9.756",
 *     "has_detail_page": "1",
 *     "webpage": "http%3A%2F%2Fwww.suedtirolerland.it%2Fapi%2Fmap%2FgetMarkerTplM%2F%3Fmarker_id%3D2827%26project_id%3D15%26lang_id%3D9"
 * }
 * </pre>
 * 
 * <p>With everything optional but lat, lng, elevation and title.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MixareUtilities {

    /**
     * Show the points in the supplied region in Mixare.
     * 
     * @param context  the context to use.
     * @param n north bound.
     * @param s south bound.
     * @param w west bound.
     * @param e east bound.
     * @throws Exception  if something goes wrong.
     */
    public static void runRegionOnMixare( Context context, float n, float s, float w, float e ) throws Exception {
        List<PointF3D> points = new ArrayList<PointF3D>();
        List<Bookmark> bookmarksList = DaoBookmarks.getBookmarksInWorldBounds(n, s, w, e);
        for( Bookmark bookmark : bookmarksList ) {
            double lat = bookmark.getLat();
            double lon = bookmark.getLon();
            String title = bookmark.getName();

            PointF3D p = new PointF3D((float) lon, (float) lat, 0f, title);
            points.add(p);
        }

        List<Note> notesList = DaoNotes.getNotesInWorldBounds(n, s, w, e);
        for( Note note : notesList ) {
            double lat = note.getLat();
            double lon = note.getLon();
            double elevation = note.getAltim();
            String title = note.getName(); // note.getName() + " (" + note.getDescription() +

            PointF3D p = new PointF3D((float) lon, (float) lat, (float) elevation, title);
            points.add(p);
        }

        MixareHandler handler = new MixareHandler();
        handler.runRegionOnMixare(context, points);
    }

}
