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
package eu.geopaparazzi.library.database;

import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;

/**
 * Interface that helps handling images in the database.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IImagesDbHelper {

    /**
     * Add an image to the db.
     *
     * @param lon       lon
     * @param lat       lat
     * @param altim     elevation
     * @param azim      azimuth
     * @param timestamp the timestamp
     * @param text      a text
     * @param image     the image data.
     * @param noteId    an optional note id, to which it is connected.
     * @return the inserted image record id.
     * @throws IOException if something goes wrong.
     */
    public long addImage(double lon, double lat, double altim, double azim, long timestamp, String text, byte[] image, Integer noteId)
            throws IOException;

    /**
     * Get an image from the db.
     *
     * @param imageId the id of the image to get.
     * @return the image or null.
     * @throws IOException if something goes wrong.
     */
    public Image getImage(long imageId) throws IOException;

    /**
     * Get image data by image id.
     *
     * @param imageId the image id.
     * @return the image data.
     * @throws IOException if something goes wrong.
     */
    public byte[] getImageData(long imageId) throws IOException;
}
