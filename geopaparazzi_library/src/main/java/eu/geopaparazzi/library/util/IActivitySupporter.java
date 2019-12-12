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

package eu.geopaparazzi.library.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;

/**
 * Class able to start activities.
 *
 * <p>This is done to handle Fragments and Activities the same way.</p>
 *
 * Created by hydrologis on 04/02/16.
 */
public interface IActivitySupporter {

    /**
     * Start an activity through and intent.
     *
     * @param intent the intent.
     */
    void startActivity(Intent intent);

    /**
     * Start an activity for result.
     *
     * @param intent the intent to use.
     * @param requestCode the request code.
     */
    void startActivityForResult(Intent intent, int requestCode);

    /**
     * Getter for the context.
     *
     * @return the context.
     */
    Context getContext();


    /**
     * Getter for a support fragment handler.
     *
     * @return the fragment manager, if available.
     */
    FragmentManager getSupportFragmentManager();

}
