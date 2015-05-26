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

package eu.geopaparazzi.library.routing.osmbonuspack;

import android.content.Context;

import java.util.ArrayList;

/**
 * Generic class to get a route between a start and a destination point,
 * going through a list of waypoints.
 *
 * @author M.Kergall
 * @see MapQuestRoadManager
 * @see GoogleRoadManager
 * @see OSRMRoadManager
 */
public abstract class RoadManager {

    protected String mOptions;

    public abstract Road getRoad(ArrayList<GeoPoint> waypoints);

    public RoadManager() {
        mOptions = "";
    }

    /**
     * Add an option that will be used in the route request.
     * Note that some options are set in the request in all cases.
     *
     * @param requestOption see provider documentation.
     *                      Just one example: "routeType=bicycle" for MapQuest; "mode=bicycling" for Google.
     */
    public void addRequestOption(String requestOption) {
        mOptions += "&" + requestOption;
    }

    /**
     * @return the GeoPoint as a string, properly formatted: lat,lon
     */
    protected String geoPointAsString(GeoPoint p) {
        StringBuffer result = new StringBuffer();
        double d = p.getLatitude();
        result.append(Double.toString(d));
        d = p.getLongitude();
        result.append("," + Double.toString(d));
        return result.toString();
    }

//	/**
//	 * Using the road high definition shape, builds and returns a Polyline.
//	 * @param road
//	 * @param color
//	 * @param width
//	 * @param context
//	 */
//	public static Polyline buildRoadOverlay(Road road, int color, float width, Context context){
//		Polyline roadOverlay = new Polyline(context);
//		roadOverlay.setColor(color);
//		roadOverlay.setWidth(width);
//		if (road != null) {
//			ArrayList<GeoPoint> polyline = road.mRouteHigh;
//			roadOverlay.setPoints(polyline);
//		}
//		return roadOverlay;
//	}
//
//	/**
//	 * Builds an overlay for the road shape with a default (and nice!) style.
//	 * @return route shape overlay
//	 */
//	public static Polyline buildRoadOverlay(Road road, Context context){
//		return buildRoadOverlay(road, 0x800000FF, 5.0f, context);
//	}

}
