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
package eu.geopaparazzi.library;

import android.content.Context;

/**
 * Class to hold the application context.
 * 
 * <p>This should be called by the Application singleton of the main application.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum GeopaparazziLibraryContextHolder {
    INSTANCE;

    private Context context;

    public void setContext( Context context ) {
        this.context = context;
    }

    public Context getContext() {
        if (context == null) {
            throw new IllegalArgumentException("Application context is null. Did you set it from your main Application?");
        }

        return context;
    }
}
