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
package eu.geopaparazzi.library.sketch;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.FormDetailFragment;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * Utilities for interaction with the sketch application.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SketchUtilities {
    private static final boolean LOG_HOW = GPLog.LOG_ABSURD;
    public static final String APP_MAIN_ACTIVITY = "anupam.acrylic.EasyPaint";
    /**
     *
     */
    public static final String EXTRA_KEY = MediaStore.EXTRA_OUTPUT;

    /**
     * Opens Marker in new sketch mode, supplying a file to save to.
     * <p/>
     * <p>If position data are supplied, they should be used to create a properties file.
     *
     * @param context     the context to use.
     * @param image       the image file to save to.
     * @param gpsLocation the position of the sketch or <code>null</code>.
     * @param requestCode if > 0, then the activity is started for a result (in which case the
     *                    Context needs to be an activity..
     */
    public static void launch(Context context, File image, double[] gpsLocation, int requestCode) {
        Intent sketchIntent = prepareIntent(context, image, gpsLocation);
        if (requestCode < 0) {
            context.startActivity(sketchIntent);
        } else {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                activity.startActivityForResult(sketchIntent, requestCode);
            }
        }
    }

    /**
     * Opens Marker in new sketch mode, supplying a file to save to.
     * <p/>
     * <p>If position data are supplied, they should be used to create a properties file.
     *
     * @param fragmentDetail the fragmentDetail to use.
     * @param image          the image file to save to.
     * @param gpsLocation    the position of the sketch or <code>null</code>.
     * @param requestCode    if > 0, then the activity is started for a result (in which case the
     *                       Context needs to be an activity..
     */
    public static void launch(FormDetailFragment fragmentDetail, File image, double[] gpsLocation, int requestCode) {
        FragmentActivity activity = fragmentDetail.getActivity();
        Intent sketchIntent = prepareIntent(activity, image, gpsLocation);
        if (requestCode < 0) {
            fragmentDetail.startActivity(sketchIntent);
        } else {
            fragmentDetail.startActivityForResult(sketchIntent, requestCode);
        }
    }

    private static Intent prepareIntent(Context context, File image, double[] gpsLocation) {
        Intent sketchIntent = null;
        try {
            sketchIntent = new Intent(context, Class.forName(APP_MAIN_ACTIVITY));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        sketchIntent.putExtra(SketchUtilities.EXTRA_KEY, image.getAbsolutePath());
        if (gpsLocation != null) {
            sketchIntent.putExtra(LibraryConstants.LATITUDE, gpsLocation[1]);
            sketchIntent.putExtra(LibraryConstants.LONGITUDE, gpsLocation[0]);
            sketchIntent.putExtra(LibraryConstants.ELEVATION, gpsLocation[2]);
        }
        return sketchIntent;
    }

    /**
     * Launch intend for result.
     *
     * @param activity    the parent activiuty.
     * @param image       the image to use.
     * @param gpsLocation the position.
     * @param requestCode the return code.
     */
    public static void launchForResult(Activity activity, File image, double[] gpsLocation, int requestCode) {
        Intent sketchIntent = prepareIntent(activity, image, gpsLocation);
        activity.startActivityForResult(sketchIntent, requestCode);
    }
}
