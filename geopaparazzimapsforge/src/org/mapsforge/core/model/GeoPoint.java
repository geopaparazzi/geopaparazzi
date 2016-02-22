/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.core.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A GeoPoint represents an immutable pair of latitude and longitude coordinates.
 */
public class GeoPoint implements Comparable<GeoPoint>, Serializable {
	private static final double EQUATORIAL_RADIUS = 6378137.0;
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new GeoPoint from a comma-separated string of coordinates in the order latitude, longitude. All
	 * coordinate values must be in degrees.
	 * 
	 * @param geoPointString
	 *            the string that describes the GeoPoint.
	 * @return a new GeoPoint with the given coordinates.
	 * @throws IllegalArgumentException
	 *             if the string cannot be parsed or describes an invalid GeoPoint.
	 */
	public static GeoPoint fromString(String geoPointString) {
		double[] coordinates = Coordinates.parseCoordinateString(geoPointString, 2);
		return new GeoPoint(coordinates[0], coordinates[1]);
	}

	/**
	 * Calculates the amount of degrees of latitude for a given distance in meters.
	 * 
	 * @param meters
	 *            distance in meters
	 * @return latitude degrees
	 */
	public static double latitudeDistance(int meters) {
		return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS);
	}

	/**
	 * Calculates the amount of degrees of longitude for a given distance in meters.
	 * 
	 * @param meters
	 *            distance in meters
	 * @param latitude
	 *            the latitude at which the calculation should be performed
	 * @return longitude degrees
	 */
	public static double longitudeDistance(int meters, double latitude) {
		return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS * Math.cos(Math.toRadians(latitude)));
	}

	private static void validateCoordinates(int latitudeE6, int longitudeE6) {
		Coordinates.validateLatitude(Coordinates.microdegreesToDegrees(latitudeE6));
		Coordinates.validateLongitude(Coordinates.microdegreesToDegrees(longitudeE6));
	}

	/**
	 * The latitude coordinate of this GeoPoint in microdegrees (degrees * 10^6).
	 */
	public final int latitudeE6;

	/**
	 * The longitude coordinate of this GeoPoint in microdegrees (degrees * 10^6).
	 */
	public final int longitudeE6;

	/**
	 * The hash code of this object.
	 */
	private transient int hashCodeValue;

	/**
	 * @param latitude
	 *            the latitude in degrees.
	 * @param longitude
	 *            the longitude in degrees.
	 */
	public GeoPoint(double latitude, double longitude) {
		this(Coordinates.degreesToMicrodegrees(latitude), Coordinates.degreesToMicrodegrees(longitude));
	}

	/**
	 * @param latitudeE6
	 *            the latitude in microdegrees (degrees * 10^6).
	 * @param longitudeE6
	 *            the longitude in microdegrees (degrees * 10^6).
	 */
	public GeoPoint(int latitudeE6, int longitudeE6) {
		validateCoordinates(latitudeE6, longitudeE6);

		this.latitudeE6 = latitudeE6;
		this.longitudeE6 = longitudeE6;
		this.hashCodeValue = calculateHashCode();
	}

	@Override
	public int compareTo(GeoPoint geoPoint) {
		if (this.longitudeE6 > geoPoint.longitudeE6) {
			return 1;
		} else if (this.longitudeE6 < geoPoint.longitudeE6) {
			return -1;
		} else if (this.latitudeE6 > geoPoint.latitudeE6) {
			return 1;
		} else if (this.latitudeE6 < geoPoint.latitudeE6) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof GeoPoint)) {
			return false;
		}
		GeoPoint other = (GeoPoint) obj;
		if (this.latitudeE6 != other.latitudeE6) {
			return false;
		} else if (this.longitudeE6 != other.longitudeE6) {
			return false;
		}
		return true;
	}

	/**
	 * @return the latitude coordinate of this GeoPoint in degrees.
	 */
	public double getLatitude() {
		return Coordinates.microdegreesToDegrees(this.latitudeE6);
	}

	/**
	 * @return the longitude coordinate of this GeoPoint in degrees.
	 */
	public double getLongitude() {
		return Coordinates.microdegreesToDegrees(this.longitudeE6);
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("GeoPoint [latitudeE6=");
		stringBuilder.append(this.latitudeE6);
		stringBuilder.append(", longitudeE6=");
		stringBuilder.append(this.longitudeE6);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + this.latitudeE6;
		result = 31 * result + this.longitudeE6;
		return result;
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		this.hashCodeValue = calculateHashCode();
	}
}
