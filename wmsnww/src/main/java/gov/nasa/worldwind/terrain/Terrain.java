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
package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;

/**
 * Provides operations on the best available terrain. Operations such as line/terrain intersection and surface point
 * computation use the highest resolution terrain data available from the globe's elevation model. Because the best
 * available data may not be available when the operations are performed, the operations block while they retrieve the
 * required data from either the local disk cache or a remote server. A timeout may be specified to limit the amount of
 * time allowed for retrieving data. Operations fail if the timeout is exceeded.
 *
 * @author tag
 * @version $Id: Terrain.java 847 2012-10-12 18:17:56Z dcollins $
 */
public interface Terrain
{
    /**
     * Returns this terrain's globe.
     *
     * @return the globe associated with this terrain.
     */
    Globe getGlobe();

    /**
     * Returns this terrain's vertical exaggeration.
     *
     * @return the vertical exaggeration associated with this terrain.
     */
    double getVerticalExaggeration();

    /**
     * Computes the elevation at a specified location.
     * <p/>
     * This operation fails with a {@link gov.nasa.worldwind.exception.WWTimeoutException} if a timeout has been
     * specified and it is exceeded during the operation.
     *
     * @param latitude  the location's latitude.
     * @param longitude the location's longitude.
     *
     * @return the elevation at the location, or <code>null</code> if the elevation could not be determined.
     *
     * @throws IllegalArgumentException if either the latitude or longitude are <code>null</code>.
     * @throws gov.nasa.worldwind.exception.WWTimeoutException
     *                                  if the current timeout is exceeded while retrieving terrain data.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if the operation is interrupted.
     */
    Double getElevation(Angle latitude, Angle longitude);

    /**
     * Computes the Cartesian, model-coordinate point of a position on the terrain.
     * <p/>
     * This operation fails with a {@link gov.nasa.worldwind.exception.WWTimeoutException} if a timeout has been
     * specified and it is exceeded during the operation.
     *
     * @param position the position.
     *
     * @return the Cartesian, model-coordinate point of the specified position, or <code>null</code> if the specified
     *         position does not exist within this instance's sector or if the operation is interrupted.
     *
     * @throws IllegalArgumentException if the position is <code>null</code>.
     * @throws gov.nasa.worldwind.exception.WWTimeoutException
     *                                  if the current timeout is exceeded while retrieving terrain data.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if the operation is interrupted.
     */
    Vec4 getSurfacePoint(Position position);

    /**
     * Computes the Cartesian, model-coordinate point of a location on the terrain.
     * <p/>
     * This operation fails with a {@link gov.nasa.worldwind.exception.WWTimeoutException} if a timeout has been
     * specified and it is exceeded during the operation.
     *
     * @param latitude     the location's latitude.
     * @param longitude    the location's longitude.
     * @param metersOffset the location's distance above the terrain.
     *
     * @return the Cartesian, model-coordinate point of the specified location, or <code>null</code> if the specified
     *         location does not exist within this instance's sector or if the operation is interrupted.
     *
     * @throws IllegalArgumentException if either the latitude or longitude are <code>null</code>.
     * @throws gov.nasa.worldwind.exception.WWTimeoutException
     *                                  if the current timeout is exceeded while retrieving terrain data.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if the operation is interrupted.
     */
    Vec4 getSurfacePoint(Angle latitude, Angle longitude, double metersOffset);

    void getSurfacePoint(Position position, Vec4 result);

    void getSurfacePoint(Angle latitude, Angle longitude, double metersOffset, Vec4 result);

    void getPoint(Position position, String altitudeMode, Vec4 result);

    void getPoint(Angle latitude, Angle longitude, double metersOffset, String altitudeMode, Vec4 result);
}
