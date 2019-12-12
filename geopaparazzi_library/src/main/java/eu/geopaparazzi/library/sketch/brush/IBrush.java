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

package eu.geopaparazzi.library.sketch.brush;

import android.graphics.Path;

/**
 * Created by IntelliJ IDEA.
 * User: almondmendoza
 * Date: 01/12/2010
 * Time: 10:48 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IBrush {
    /**
     * @param path path
     * @param x    x
     * @param y    y
     */
    void mouseDown(Path path, float x, float y);

    /**
     * @param path path
     * @param x    x
     * @param y    y
     */
    void mouseMove(Path path, float x, float y);

    /**
     * @param path path
     * @param x    x
     * @param y    y
     */
    void mouseUp(Path path, float x, float y);
}
