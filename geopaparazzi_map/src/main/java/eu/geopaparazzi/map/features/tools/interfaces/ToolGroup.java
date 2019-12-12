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

package eu.geopaparazzi.map.features.tools.interfaces;

/**
 * A group of tools.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface ToolGroup extends DrawingTool {

    /**
     * Create a custom UI for the tool if necessary.
     */
    void initUI();

    /**
     * Disables the toolgroup.
     */
    void disable();

    /**
     * Callback when a tool finishes.
     *
     * @param tool the tool that finished.
     */
    void onToolFinished(Tool tool);

    /**
     * Callback for position updates.
     *
     * @param lon longitude.
     * @param lat latitude.
     */
    void onGpsUpdate(double lon, double lat);
}
