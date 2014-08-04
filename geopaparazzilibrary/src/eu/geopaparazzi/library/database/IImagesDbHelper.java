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
     * @param thumbnail a scaled image for quick extraction and preview.
     * @param noteId    the note id, to which it is connected or -1 if it is standalone.
     * @return the inserted image record id.
     * @throws IOException if something goes wrong.
     */
    public long addImage(double lon, double lat, double altim, double azim, long timestamp, String text, byte[] image, byte[] thumbnail, long noteId)
            throws Exception;

    /**
     * Get an image from the db by its id..
     *
     * @param imageId the id of the image to get.
     * @return the image or null.
     * @throws IOException if something goes wrong.
     */
    public Image getImage(long imageId) throws Exception;

    /**
     * Get image data by image id.
     *
     * @param imageId the image id.
     * @return the image data.
     * @throws IOException if something goes wrong.
     */
    public byte[] getImageData(long imageId) throws Exception;

    /**
     * Get an image from the db by its <b>data</b> id.
     *
     * @param imageDataId the image data id.
     * @param sqliteDatabase the optional db to use. If called from #getImageData, this should not be null.
     * @return the image data.
     * @throws IOException
     */
    public byte[] getImageDataById(long imageDataId, SQLiteDatabase sqliteDatabase) throws Exception;

    /**
     * Get image thumbnail by image id.
     *
     * @param imageId the image id.
     * @return the image thumbnail data.
     * @throws IOException if something goes wrong.
     */
    public byte[] getImageThumbnail(long imageId) throws Exception;

    /**
     * Get an image thumbnail from the db by its <b>data</b> id.
     *
     * @param imageDataId the image data id.
     * @param sqliteDatabase the optional db to use. If called from #getImageData, this should not be null.
     * @return the image data.
     * @throws IOException
     */
    public byte[] getImageThumbnailById(SQLiteDatabase sqliteDatabase, long imageDataId) throws Exception;
}
