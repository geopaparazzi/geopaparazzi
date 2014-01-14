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
package eu.geopaparazzi.library.forms.views;

import android.content.Context;
import android.content.Intent;

/**
 * Interface for geopaparazzi custom views.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface GView {
    /**
     * Get a representative value of the view.
     * 
     * @return a value the view specialises on.
     */
    public String getValue();

    /**
     * A callback in case the view was registered to react on an activity result.
     * 
     * @param data the data returned.
     */
    public void setOnActivityResult( Intent data );

    /**
     * A method to refresh content if necessary.
     * 
     * @param context  the context to use.
     */
    public void refresh( Context context );
}
