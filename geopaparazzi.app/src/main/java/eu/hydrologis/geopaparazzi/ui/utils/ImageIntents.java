/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.hydrologis.geopaparazzi.ui.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.hydrologis.geopaparazzi.database.DaoImages;

/**
 * Utility class to show images
 *
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ImageIntents {
    public static void showImage(byte[] imageData, String imageName, Context context) throws Exception {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        File tempDir = ResourcesManager.getInstance(context).getTempDir();
        String ext = ".jpg";
        if (imageName.endsWith(".png"))
            ext = ".png";
        File imageFile = new File(tempDir, ImageUtilities.getTempImageName(ext));
        ImageUtilities.writeImageDataToFile(imageData, imageFile.getAbsolutePath());

        intent.setDataAndType(Uri.fromFile(imageFile), "image/*"); //$NON-NLS-1$
        context.startActivity(intent);
    }

    public static void showImage(File imageFile, Context context) throws Exception {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(imageFile), "image/*"); //$NON-NLS-1$
        context.startActivity(intent);
    }

}
