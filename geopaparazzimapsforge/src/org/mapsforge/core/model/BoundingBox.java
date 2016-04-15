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
 * A BoundingBox represents an immutable set of two latitude and two longitude coordinates.
 */
public class BoundingBox implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new BoundingBox from a comma-separated string of coordinates in the order minLat, minLon, maxLat,
	 * maxLon. All coordinate values must be in degrees.
	 * 
	 * @param boundingBoxString
	 *            the string that describes the BoundingBox.
	 * @return a new BoundingBox with the given coordinates.
	 * @throws IllegalArgumentException
	 *             if the string cannot be parsed or describes an invalid BoundingBox.
	 */
	public static BoundingBox fromString(String boundingBoxString) {
		double[] coordinates = Coordinates.parseCoordinateString(boundingBoxString, 4);
		int minLat = Coordinates.degreesToMicrodegrees(coordinates[0]);
		int minLon = Coordinates.degreesToMicrodegrees(coordinates[1]);
		int maxLat = Coordinates.degreesToMicrodegrees(coordinates[2]);
		int maxLon = Coordinates.degreesToMicrodegrees(coordinates[3]);
		return new BoundingBox(minLat, minLon, maxLat, maxLon);
	}

	private static boolean isBetween(int number, int min, int max) {
		return min <= number && number <= max;
	}

	private static void validateCoordinates(int minLatitudeE6, int minLongitudeE6, int maxLatitudeE6, int maxLongitudeE6) {
		Coordinates.validateLatitude(Coordinates.microdegreesToDegrees(minLatitudeE6));
		Coordinates.validateLongitude(Coordinates.microdegreesToDegrees(minLongitudeE6));
		Coordinates.validateLatitude(Coordinates.microdegreesToDegrees(maxLatitudeE6));
		Coordinates.validateLongitude(Coordinates.microdegreesToDegrees(maxLongitudeE6));

		if (minLatitudeE6 > maxLatitudeE6) {
			throw new IllegalArgumentException("invalid latitude range: " + minLatitudeE6 + ' ' + maxLatitudeE6);
		}

		if (minLongitudeE6 > maxLongitudeE6) {
			throw new IllegalArgumentException("invalid longitude range: " + minLongitudeE6 + ' ' + maxLongitudeE6);
		}
	}

	/**
	 * The maximum latitude coordinate of this BoundingBox in microdegrees (degrees * 10^6).
	 */
	public final int maxLatitudeE6;

	/**
	 * The maximum longitude coordinate of this BoundingBox in microdegrees (degrees * 10^6).
	 */
	public final int maxLongitudeE6;

	/**
	 * The minimum latitude coordinate of this BoundingBox in microdegrees (degrees * 10^6).
	 */
	public final int minLatitudeE6;

	/**
	 * The minimum longitude coordinate of this BoundingBox in microdegrees (degrees * 10^6).
	 */
	public final int minLongitudeE6;

	/**
	 * The hash code of this object.
	 */
	private transient int hashCodeValue;

	/**
	 * @param minLatitudeE6
	 *            the minimum latitude in microdegrees (degrees * 10^6).
	 * @param minLongitudeE6
	 *            the minimum longitude in microdegrees (degrees * 10^6).
	 * @param maxLatitudeE6
	 *            the maximum latitude in microdegrees (degrees * 10^6).
	 * @param maxLongitudeE6
	 *            the maximum longitude in microdegrees (degrees * 10^6).
	 * @throws IllegalArgumentException
	 *             if a coordinate is invalid.
	 */
	public BoundingBox(int minLatitudeE6, int minLongitudeE6, int maxLatitudeE6, int maxLongitudeE6) {
		validateCoordinates(minLatitudeE6, minLongitudeE6, maxLatitudeE6, maxLongitudeE6);

		this.minLatitudeE6 = minLatitudeE6;
		this.minLongitudeE6 = minLongitudeE6;
		this.maxLatitudeE6 = maxLatitudeE6;
		this.maxLongitudeE6 = maxLongitudeE6;
		this.hashCodeValue = calculateHashCode();
	}

	/**
	 * @param geoPoint
	 *            the point whose coordinates should be checked.
	 * @return true if this BoundingBox contains the given GeoPoint, false otherwise.
	 */
	public boolean contains(GeoPoint geoPoint) {
		return isBetween(geoPoint.latitudeE6, this.minLatitudeE6, this.maxLatitudeE6)
				&& isBetween(geoPoint.longitudeE6, this.minLongitudeE6, this.maxLongitudeE6);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof BoundingBox)) {
			return false;
		}
		BoundingBox other = (BoundingBox) obj;
		if (this.maxLatitudeE6 != other.maxLatitudeE6) {
			return false;
		} else if (this.maxLongitudeE6 != other.maxLongitudeE6) {
			return false;
		} else if (this.minLatitudeE6 != other.minLatitudeE6) {
			return false;
		} else if (this.minLongitudeE6 != other.minLongitudeE6) {
			return false;
		}
		return true;
	}

	/**
	 * @return a new GeoPoint at the horizontal and vertical center of this BoundingBox.
	 */
	public GeoPoint getCenterPoint() {
		int latitudeOffset = (this.maxLatitudeE6 - this.minLatitudeE6) / 2;
		int longitudeOffset = (this.maxLongitudeE6 - this.minLongitudeE6) / 2;
		return new GeoPoint(this.minLatitudeE6 + latitudeOffset, this.minLongitudeE6 + longitudeOffset);
	}

	/**
	 * @return the maximum latitude coordinate of this BoundingBox in degrees.
	 */
	public double getMaxLatitude() {
		return Coordinates.microdegreesToDegrees(this.maxLatitudeE6);
	}

	/**
	 * @return the maximum longitude coordinate of this BoundingBox in degrees.
	 */
	public double getMaxLongitude() {
		return Coordinates.microdegreesToDegrees(this.maxLongitudeE6);
	}

	/**
	 * @return the minimum latitude coordinate of this BoundingBox in degrees.
	 */
	public double getMinLatitude() {
		return Coordinates.microdegreesToDegrees(this.minLatitudeE6);
	}

	/**
	 * @return the minimum longitude coordinate of this BoundingBox in degrees.
	 */
	public double getMinLongitude() {
		return Coordinates.microdegreesToDegrees(this.minLongitudeE6);
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("BoundingBox [minLatitudeE6=");
		stringBuilder.append(this.minLatitudeE6);
		stringBuilder.append(", minLongitudeE6=");
		stringBuilder.append(this.minLongitudeE6);
		stringBuilder.append(", maxLatitudeE6=");
		stringBuilder.append(this.maxLatitudeE6);
		stringBuilder.append(", maxLongitudeE6=");
		stringBuilder.append(this.maxLongitudeE6);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + this.maxLatitudeE6;
		result = 31 * result + this.maxLongitudeE6;
		result = 31 * result + this.minLatitudeE6;
		result = 31 * result + this.minLongitudeE6;
		return result;
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		this.hashCodeValue = calculateHashCode();
	}
}
