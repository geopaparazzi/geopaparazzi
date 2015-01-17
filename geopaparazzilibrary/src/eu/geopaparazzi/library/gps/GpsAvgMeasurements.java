/*
   Copyright 2010 Libor Tvrdik, David "Destil" Vavra

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Modified and added to Geopaparazzi by Tim Howard, 2015
 */
package eu.geopaparazzi.library.gps;

import android.location.Location;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Group all the measurements. It allows the calculation of the weighted average
 * of the measured values and record times of measurement.
 * 
 * @author Libor Tvrdik (libor.tvrdik@gmail.com), Destil
 */
public class GpsAvgMeasurements {

	private static volatile GpsAvgMeasurements instance = null;
	private final List<Location> locations;
	private double averageLat;
	private double averageLon;
	private double averageAlt;
	private float averageAccuracy;
	private double weightedLatSum;
	private double weightedLonSum;
	private double altSum;
	private double invertedAccuracySum;
	private float distanceFromAverageCoordsSum;

	/** Creates a new list. */
	private GpsAvgMeasurements() {
		this.locations = new ArrayList<Location>();
		clean();
	}

	/**
	 * Singleton
	 */
	public static GpsAvgMeasurements getInstance() {
		if (instance == null) {
			instance = new GpsAvgMeasurements();
		}
		return instance;
	}

	/**
	 * @return size of list (count of items)
	 * @see #add(android.location.Location)
	 */
	public synchronized int size() {
		return locations.size();
	}

	/** Delete all items from list. Can be reused for next measurements. */
	public synchronized void clean() {
		locations.clear();
		averageLat = 0;
		averageLon = 0;
		averageAlt = 0;
		averageAccuracy = 0;
		weightedLatSum = 0;
		weightedLonSum = 0;
		altSum = 0;
		invertedAccuracySum = 0;
		distanceFromAverageCoordsSum = 0;
	}

	/** Adds a new value into list. */
	public synchronized void add(Location location) {

		locations.add(location);

		final double invertedAccuracy = 1 / (location.getAccuracy() == 0 ? 1 : location.getAccuracy());
		weightedLatSum += location.getLatitude() * invertedAccuracy;
		weightedLonSum += location.getLongitude() * invertedAccuracy;
		invertedAccuracySum += invertedAccuracy;
		altSum += location.getAltitude();

		// calculating average coordinates (weighted by accuracy) and altitude
		averageLat = weightedLatSum / invertedAccuracySum;
		averageLon = weightedLonSum / invertedAccuracySum;
		averageAlt = altSum / size();

		// calculating accuracy improved by averaging
		double distance = distance(location.getLatitude(), location.getLongitude(), averageLat, averageLon);
		if (distance == 0) {
			distance = (location.getAccuracy() == 0 ? 2 : location.getAccuracy());
		}
		distanceFromAverageCoordsSum += distance;
		averageAccuracy = distanceFromAverageCoordsSum / size();
	}

	/**
	 * @return weighted mean of all location in list, or location with all
	 *         property set on 0 if list is empty.
	 */
	public synchronized Location getAveragedLocation() {

		final Location location = new Location("average");
		location.setLatitude(getLatitude());
		location.setLongitude(getLongitude());
		location.setAccuracy(getAccuracy());
		location.setAltitude(getAltitude());
		location.setTime(System.currentTimeMillis());
		return location;
	}

	/**
	 * @return weighted mean of all location in list, or location with all
	 *         property set on 0 if list is empty.
	 */
	public synchronized Location getLocation(int index) {
		return locations.get(index);
	}

	/** @return weighted mean of all latitudes in list, or 0 if list is empty. */
	public synchronized double getLatitude() {
		return averageLat;
	}

	/** @return weighted mean of all longitudes in list, or 0 if list is empty. */
	public synchronized double getLongitude() {
		return averageLon;
	}

	/** @return arithmetic mean of all errors in list, or 0 if list is empty. */
	public synchronized float getAccuracy() {
		return averageAccuracy;
	}

	/** @return weighted mean of all altitudes in list, or 0 if list is empty. */
	public synchronized double getAltitude() {
		return averageAlt;
	}

	/**
	 * @param index
	 *            in list, 0 for first item, size()-1 for last item
	 * @return stored latitude from list.
	 */
	public synchronized double getLatitude(int index) {
		return locations.get(index).getLatitude();
	}

	/**
	 * @param index
	 *            in list, 0 for first item, size()-1 for last item
	 * @return stored longitude from list.
	 */
	public synchronized double getLongitude(int index) {
		return locations.get(index).getLongitude();
	}

	/**
	 * @param index
	 *            in list, 0 for first item, size()-1 for last item
	 * @return stored error from list.
	 */
	public synchronized float getAccuracy(int index) {
		return locations.get(index).getAccuracy();
	}

	/**
	 * @param index
	 *            in list, 0 for first item, size()-1 for last item
	 * @return stored altitude from list.
	 */
	public synchronized double getAltitude(int index) {
		return locations.get(index).getAltitude();
	}

	/**
	 * @param index
	 *            in list, 0 for first item, size()-1 for last item
	 * @return date of store index in list.
	 */
	public synchronized Date getTime(int index) {
		return new Date(locations.get(index).getTime());
	}

	@Override
	public synchronized String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append(getClass().getSimpleName()).append(": (");
		sb.append("avg lat=").append(getLatitude());
		sb.append(" lon=").append(getLongitude());
		sb.append(" alt=").append(getAltitude());
		sb.append(" acc=").append(getAccuracy()).append(" ");
		for (int i = 0; i < size(); i++) {
			sb.append("[").append(getLongitude(i)).append(",");
			sb.append(getLatitude(i)).append(",");
			sb.append(getAltitude(i)).append(",");
			sb.append(getAccuracy(i)).append("]");
		}
		sb.append(")");

		return sb.toString();
	}

	/** Calculates distance between two gps lat/lon pairs */
	private double distance(double lat1, double lng1, double lat2, double lng2) {
		double earthRadius = 3958.75;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = earthRadius * c;

		int meterConversion = 1609;
		return (dist * meterConversion);
	}

}
