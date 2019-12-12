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

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.HashMap;

/**
 * Class to support geo-url parsing and creating.
 *
 * @author Andrea Antonello
 */
@SuppressWarnings("ALL")
public class UrlUtilities {

    public static final String OSM_MAPS_URL = "http://www.openstreetmap.org";
    public static final String SHORT_OSM_MAPS_URL = "http://www.osm.org";

    /**
     * Create an OSM url from coordinates.
     *
     * @param lat             lat
     * @param lon             lon
     * @param withMarker      if <code>true</code>, marker is added.
     * @param withGeosmsParam if <code>true</code>, geosms params are added.
     * @return url string.
     */
    public static String osmUrlFromLatLong(float lat, float lon, boolean withMarker, boolean withGeosmsParam) {
        // http://www.osm.org/?mlat=45.79668&mlon=9.12342#map=18/45.79668/9.12342 -> with marker
        // http://www.osm.org/#map=18/45.79668/9.12275

        StringBuilder sB = new StringBuilder();
        sB.append("http://www.osm.org/");
        if (withMarker) {
            sB.append("?mlat=");
            sB.append(lat);
            sB.append("&mlon=");
            sB.append(lon);
        }
        sB.append("#map=");
        sB.append(18);
        sB.append("/");
        sB.append(lat);
        sB.append("/");
        sB.append(lon);

        if (withGeosmsParam) {
            sB.append("&GeoSMS");
        }
        return sB.toString();
    }

    /**
     * Gets the data from a osm url.
     *
     * @param urlString the url to parse.
     * @return a SimplePosition. It needs to be checked for internal nulls (in case this failed)
     */
    @NonNull
    public static SimplePosition getLatLonTextFromOsmUrl(String urlString) {
        // http://www.openstreetmap.org/?mlat=42.082&mlon=9.822#map=6/42.082/9.822&layers=N
        // http://www.osm.org/?mlat=45.79668&mlon=9.12342#map=18/45.79668/9.12342 -> with marker
        // http://www.osm.org/#map=18/45.79668/9.12275

        SimplePosition simplePosition = new SimplePosition();
        if (urlString == null) return simplePosition;

        if (urlString.startsWith(OSM_MAPS_URL) || urlString.startsWith(SHORT_OSM_MAPS_URL)) {
            String[] urlSplit = urlString.split("#|&|\\?");
            HashMap<String, String> paramsMap = new HashMap<String, String>();
            for (String string : urlSplit) {
                if (string.indexOf('=') != -1) {
                    String[] keyValue = string.split("=");
                    if (keyValue.length == 2) {
                        paramsMap.put(keyValue[0].toLowerCase(), keyValue[1]);
                    }
                }
            }

            // check if there is a dash for adding text
            String textStr = new Date().toString();
            int lastDashIndex = urlString.lastIndexOf('#');
            if (lastDashIndex != -1) {
                // everything after a dash is taken as text
                String tmpTextStr = urlString.substring(lastDashIndex + 1);
                if (!tmpTextStr.startsWith("map=")) {
                    textStr = tmpTextStr;
                }
            }

            String coordsStr = paramsMap.get("map");
            if (coordsStr != null) {
                String[] split = coordsStr.split("/");
                if (split.length == 3) {
                    try {
                        double lat = Double.parseDouble(split[1]);
                        double lon = Double.parseDouble(split[2]);
                        int zoom = (int) Double.parseDouble(split[0]);
                        simplePosition.latitude = lat;
                        simplePosition.longitude = lon;
                        simplePosition.text = textStr;
                        simplePosition.zoomLevel = zoom;
                    } catch (NumberFormatException e) {
                        // ingore this
                    }
                }
            }
        }
        return simplePosition;
    }


}
