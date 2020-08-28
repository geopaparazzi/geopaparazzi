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
package eu.geopaparazzi.map.features.tools;


import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.features.tools.interfaces.DrawingTool;

/**
 * A tool that works on a {@link GPMapView}.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class MapTool implements DrawingTool {

    protected GPMapView mapView;

    /**
     * Constructor.
     *
     * @param mapView the mapview to work on
     */
    public MapTool(GPMapView mapView) {
        this.mapView = mapView;
    }

    /*
     * Called when something in the view changes (ex. zoom, pan).
     */
    public abstract void onViewChanged();
}
