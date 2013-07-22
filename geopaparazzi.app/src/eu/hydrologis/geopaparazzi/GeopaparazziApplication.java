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
package eu.hydrologis.geopaparazzi;

import eu.geopaparazzi.library.database.GPLog;
import android.app.Application;
import android.util.Log;

/**
 * Application singleton.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopaparazziApplication extends Application {

    private static GeopaparazziApplication instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        if (GPLog.LOG_ANDROID) {
            Log.i(getClass().getSimpleName(), "GeopaparazziApplication singleton created.");
        }
    }

    public static GeopaparazziApplication getInstance() {
        return instance;
    }
}
