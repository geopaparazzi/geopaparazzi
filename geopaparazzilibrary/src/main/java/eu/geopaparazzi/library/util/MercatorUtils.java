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

/**
 * Created by hydrologis on 15/01/16.
 */
public class MercatorUtils {

    private static double originShift = 2 * Math.PI * 6378137 / 2.0;
    private static final double METER_TO_FEET_CONVERSION_FACTOR = 3.2808399;

    /**
     * Converts TMS tile coordinates to Google Tile coordinates.
     * <p/>
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param tx   the x tile number.
     * @param ty   the y tile number.
     * @param zoom the current zoom level.
     * @return the converted values.
     */
    public static int[] tmsTile2GoogleTile(int tx, int ty, int zoom) {
        return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
    }

    /**
     * Converts Google tile coordinates to TMS Tile coordinates.
     * <p/>
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param tx   the x tile number.
     * @param ty   the y tile number.
     * @param zoom the current zoom level.
     * @return the converted values.
     */
    public static int[] googleTile2TmsTile(int tx, int ty, int zoom) {
        return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
    }

    /**
     * Converts TMS tile coordinates to Microsoft QuadTree.
     * <p/>
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param tx   tile x.
     * @param ty   tile y.
     * @param zoom zoomlevel.
     * @return the quadtree key.
     */
    public static String quadTree(int tx, int ty, int zoom) {
        String quadKey = ""; //$NON-NLS-1$
        ty = (int) ((Math.pow(2, zoom) - 1) - ty);
        for (int i = zoom; i < 0; i--) {
            int digit = 0;
            int mask = 1 << (i - 1);
            if ((tx & mask) != 0) {
                digit += 1;
            }
            if ((ty & mask) != 0) {
                digit += 2;
            }
            quadKey += (digit + ""); //$NON-NLS-1$
        }
        return quadKey;
    }

    /**
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param tx       tile x.
     * @param ty       tile y.
     * @param zoom     zoomlevel.
     * @param tileSize tile size.
     * @return [minx, miny, maxx, maxy]
     */
    public static double[] tileLatLonBounds(int tx, int ty, int zoom, int tileSize) {
        double[] bounds = tileBounds(tx, ty, zoom, tileSize);
        double[] mins = metersToLatLon(bounds[0], bounds[1]);
        double[] maxs = metersToLatLon(bounds[2], bounds[3]);
        return new double[]{mins[1], maxs[0], maxs[1], mins[0]};
    }

    /**
     * Returns bounds of the given tile in EPSG:900913 coordinates
     * <p/>
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param tx       tile x.
     * @param ty       tile y.
     * @param zoom     zoomlevel.
     * @param tileSize tile size.
     * @return [minx, miny, maxx, maxy]
     */
    public static double[] tileBounds(int tx, int ty, int zoom, int tileSize) {
        double[] min = pixelsToMeters(tx * tileSize, ty * tileSize, zoom, tileSize);
        double minx = min[0], miny = min[1];
        double[] max = pixelsToMeters((tx + 1) * tileSize, (ty + 1) * tileSize, zoom, tileSize);
        double maxx = max[0], maxy = max[1];
        return new double[]{minx, miny, maxx, maxy};
    }

    /**
     * Converts XY point from Spherical Mercator EPSG:900913 to lat/lon in WGS84
     * Datum
     * <p/>
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param mx x
     * @param my y
     * @return lat long
     */
    public static double[] metersToLatLon(double mx, double my) {

        double lon = (mx / originShift) * 180.0;
        double lat = (my / originShift) * 180.0;

        lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return new double[]{-lat, lon};
    }

    /**
     * Equatorial radius of earth is required for distance computation.
     */
    public static final double EQUATORIALRADIUS = 6378137.0;

    /**
     * Convert a longitude coordinate (in degrees) to a horizontal distance in meters from the
     * zero meridian
     *
     * @param longitude in degrees
     * @return longitude in meters in spherical mercator projection
     */
    public static double longitudeToMetersX(double longitude) {
        return EQUATORIALRADIUS * Math.toRadians(longitude);
    }

    /**
     * Convert a meter measure to a longitude
     *
     * @param x in meters
     * @return longitude in degrees in spherical mercator projection
     */
    public static double metersXToLongitude(double x) {
        return Math.toDegrees(x / EQUATORIALRADIUS);
    }

    /**
     * Convert a meter measure to a latitude
     *
     * @param y in meters
     * @return latitude in degrees in spherical mercator projection
     */
    public static double metersYToLatitude(double y) {
        return Math.toDegrees(Math.atan(Math.sinh(y / EQUATORIALRADIUS)));
    }

    /**
     * Convert a latitude coordinate (in degrees) to a vertical distance in meters from the
     * equator
     *
     * @param latitude in degrees
     * @return latitude in meters in spherical mercator projection
     */
    public static double latitudeToMetersY(double latitude) {
        return EQUATORIALRADIUS
                * Math.log(Math.tan(Math.PI / 4 + 0.5 * Math.toRadians(latitude)));
    }

    /**
     * Convert a east-longitude,west-longitude coordinate (in degrees) to distance in meters
     *
     * @param east_longitude longitude in degrees
     * @param west_longitude longitude in degrees
     * @return meters in spherical mercator projection
     */
    public static double longitudeToMeters(double east_longitude, double west_longitude) {
        return longitudeToMetersX(east_longitude) - longitudeToMetersX(west_longitude);
    }

    /**
     * Convert a north-latitude,south-latitude coordinate (in degrees) to distance in meters
     *
     * @param north_latitude latitude in degrees
     * @param south_latitude latitude in degrees
     * @return meters in spherical mercator projection
     */
    public static double latitudeToMeters(double north_latitude, double south_latitude) {
        return latitudeToMetersY(north_latitude) - latitudeToMetersY(south_latitude);
    }

    /**
     * Converts given lat/lon in WGS84 Datum to XY in Spherical Mercator
     * EPSG:900913
     *
     * @param lat
     * @param lon
     * @return
     */
    public static double[] latLonToMeters(double lat, double lon) {
        double mx = lon * originShift / 180.0;
        double my = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        my = my * originShift / 180.0;
        return new double[]{mx, my};
    }


    /**
     * Converts pixel coordinates in given zoom level of pyramid to EPSG:900913
     * <p/>
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param px       pixel x.
     * @param py       pixel y.
     * @param zoom     zoomlevel.
     * @param tileSize tile size.
     * @return converted coordinate.
     */
    public static double[] pixelsToMeters(double px, double py, int zoom, int tileSize) {
        double res = getResolution(zoom, tileSize);
        double mx = px * res - originShift;
        double my = py * res - originShift;
        return new double[]{mx, my};
    }


    /**
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param px
     * @param py
     * @return
     */
    public static int[] pixelsToTile(int px, int py, int tileSize) {
        int tx = (int) Math.ceil(px / ((double) tileSize) - 1);
        int ty = (int) Math.ceil(py / ((double) tileSize) - 1);
        return new int[]{tx, ty};
    }


    /**
     * Converts EPSG:900913 to pyramid pixel coordinates in given zoom level
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param mx
     * @param my
     * @param zoom
     * @return
     */
    public static int[] metersToPixels(double mx, double my, int zoom, int tileSize) {
        double res = getResolution(zoom, tileSize);
        int px = (int) Math.round((mx + originShift) / res);
        int py = (int) Math.round((my + originShift) / res);
        return new int[]{px, py};
    }

    /**
     * Returns tile for given mercator coordinates
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @return
     */
    public static int[] metersToTile(double mx, double my, int zoom, int tileSize) {
        int[] p = metersToPixels(mx, my, zoom, tileSize);
        return pixelsToTile(p[0], p[1], tileSize);
    }

    /**
     * Resolution (meters/pixel) for given zoom level (measured at Equator)
     * <p/>
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param zoom     zoomlevel.
     * @param tileSize tile size.
     * @return resolution.
     */
    public static double getResolution(int zoom, int tileSize) {
        // return (2 * Math.PI * 6378137) / (this.tileSize * 2**zoom)
        double initialResolution = 2 * Math.PI * 6378137 / tileSize;
        return initialResolution / Math.pow(2, zoom);
    }

    /**
     * Convert meters to feet.
     *
     * @param meters the value in meters to convert to feet.
     * @return meters converted to feet.
     */
    public static double toFeet(final double meters) {
        return meters * METER_TO_FEET_CONVERSION_FACTOR;
    }

}
