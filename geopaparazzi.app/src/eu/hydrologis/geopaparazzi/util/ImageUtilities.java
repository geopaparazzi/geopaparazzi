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

import java.io.FileOutputStream;
import java.io.IOException;

import eu.hydrologis.geopaparazzi.database.DaoImages;

/**
 * Created by hydrologis on 30/07/14.
 */
public class ImageUtilities {

    /**
     * Write an image from the databases to file.
     *
     * @param imageId the image id.
     * @param path the path to write to.
     * @throws IOException
     */
    public static void imageIdToFile(long imageId, String path) throws IOException {
        byte[] imageData = DaoImages.getImageData(imageId);
        FileOutputStream stream = new FileOutputStream(path);
        stream.write(imageData);
        stream.close();
    }
}
