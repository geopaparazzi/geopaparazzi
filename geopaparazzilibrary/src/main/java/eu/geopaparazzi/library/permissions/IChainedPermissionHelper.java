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
package eu.geopaparazzi.library.permissions;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

/**
 * An interface to handle chained permissions.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IChainedPermissionHelper {
    boolean canAskPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    /**
     * Get a description of this permission helper.
     *
     * @return a string that can be used in dialogs.
     */
    String getDescription();

    /**
     * Checks if the permission is granted.
     *
     * @param context the context to use.
     * @return true, if permission is granted.
     */
    boolean hasPermission(Context context);

    /**
     * Request the permission.
     *
     * @param activity the asking activity.
     */
    void requestPermission(Activity activity);

    /**
     * Checks if the permission has finally been granted.
     * <p>
     * <p>To be checked in the <code>onRequestPermissionsResult</code> method once the request comes back.</p>
     *
     * @param requestCode  the requestcode used.
     * @param grantResults the results array.
     * @return true if permission has been granted.
     */
    boolean hasGainedPermission(int requestCode, int[] grantResults);

    /**
     * Get the next non granted permission in the chain.
     *
     * @param context the context to use.
     * @return the next non granted permission or null if all are granted.
     */
    IChainedPermissionHelper getNextWithoutPermission(Context context);
}
