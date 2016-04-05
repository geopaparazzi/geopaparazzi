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
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.util.Logging;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author dcollins
 * @version $Id: LatLon.java 812 2012-09-26 22:03:40Z dcollins $
 */
public class LatLon {
	public final Angle latitude;
	public final Angle longitude;

	public LatLon() {
		this.latitude = new Angle();
		this.longitude = new Angle();
	}

	public LatLon(Angle latitude, Angle longitude) {
		if (latitude == null) {
			String msg = Logging.getMessage("nullValue.LatitudeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (longitude == null) {
			String msg = Logging.getMessage("nullValue.LongitudeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.latitude = latitude;
		this.longitude = longitude;
	}

	public static LatLon fromDegrees(double latitude, double longitude) {
		return new LatLon(Angle.fromDegrees(latitude), Angle.fromDegrees(longitude));
	}

	public static LatLon fromRadians(double latitude, double longitude) {
		return new LatLon(Angle.fromRadians(latitude), Angle.fromRadians(longitude));
	}

	/**
	 * Returns the an interpolated location along the great-arc between the specified locations. This does not retain
	 * any reference to the specified locations, or modify them in any way.
	 * <p/>
	 * The interpolation factor amount is a floating-point value in the range [0.0, 1.0] which defines the weight given to each location.
	 * 
	 * @param amount
	 *            the interpolation factor as a floating-point value in the range [0.0, 1.0].
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return an interpolated location along the great-arc between lhs and rhs.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static LatLon interpolateGreatCircle(double amount, LatLon lhs, LatLon rhs) {
		if (lhs == null) {
			String msg = Logging.getMessage("nullValue.LhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (rhs == null) {
			String msg = Logging.getMessage("nullValue.RhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (lhs.equals(rhs)) return lhs;

		double t = (amount < 0 ? 0 : (amount > 1 ? 1 : amount));

		Angle azimuth = LatLon.greatCircleAzimuth(lhs, rhs);
		Angle distance = LatLon.greatCircleDistance(lhs, rhs);
		Angle pathLength = Angle.fromDegrees(t * distance.degrees);

		return LatLon.greatCircleEndPosition(lhs, azimuth, pathLength);
	}

	/**
	 * Returns the an interpolated location along the rhumb line between the specified locations. This does not retain
	 * any reference to the specified locations, or modify them in any way.
	 * <p/>
	 * The interpolation factor amount is a floating-point value in the range [0.0, 1.0] which defines the weight given to each location.
	 * 
	 * @param amount
	 *            the interpolation factor as a floating-point value in the range [0.0, 1.0].
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return an interpolated location along the rhumb line between lhs and rhs.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static LatLon interpolateRhumb(double amount, LatLon lhs, LatLon rhs) {
		if (lhs == null) {
			String msg = Logging.getMessage("nullValue.LhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (rhs == null) {
			String msg = Logging.getMessage("nullValue.RhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (lhs.equals(rhs)) return lhs;

		double t = (amount < 0 ? 0 : (amount > 1 ? 1 : amount));

		Angle azimuth = LatLon.rhumbAzimuth(lhs, rhs);
		Angle distance = LatLon.rhumbDistance(lhs, rhs);
		Angle pathLength = Angle.fromDegrees(t * distance.degrees);

		return LatLon.rhumbEndPosition(lhs, azimuth, pathLength);
	}

	/**
	 * Computes the azimuth angle (clockwise from North) that points from the first location to the second location
	 * along a great circle arc. This angle can be used as the starting azimuth for a great circle arc that begins at
	 * the first location, and passes through the second location. Note that this angle is valid only at the first
	 * location; the azimuth along a great circle arc varies continuously at every point along the arc. This does not
	 * retain any reference to the specified locations, or modify them in any way.
	 * 
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return Angle that points from the first location to the second location.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static Angle greatCircleAzimuth(LatLon lhs, LatLon rhs) {
		if (lhs == null) {
			String msg = Logging.getMessage("nullValue.LhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (rhs == null) {
			String msg = Logging.getMessage("nullValue.RhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		double lat1 = lhs.latitude.radians;
		double lon1 = lhs.longitude.radians;
		double lat2 = rhs.latitude.radians;
		double lon2 = rhs.longitude.radians;

		if (lat1 == lat2 && lon1 == lon2) return Angle.fromRadians(0);

		if (lon1 == lon2) return lat1 > lat2 ? Angle.fromDegrees(180) : Angle.fromRadians(0);

		// Taken from "Map Projections - A Working Manual", page 30, equation 5-4b.
		// The atan2() function is used in place of the traditional atan(y/x) to simplify the case when x==0.
		double y = Math.cos(lat2) * Math.sin(lon2 - lon1);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
		double azimuthRadians = Math.atan2(y, x);

		return Double.isNaN(azimuthRadians) ? Angle.fromRadians(0) : Angle.fromRadians(azimuthRadians);
	}

	/**
	 * Computes the great circle angular distance between two locations. The return value gives the distance as the
	 * angle between the two positions on the pi radius circle. In radians, this angle is also the arc length of the
	 * segment between the two positions on that circle. To compute a distance in meters from this value, multiply it by
	 * the radius of the globe. This does not retain any reference to the specified locations, or modify them in any
	 * way.
	 * 
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return the angular distance between the two locations. In radians, this value is the arc length on the radius pi
	 *         circle.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static Angle greatCircleDistance(LatLon lhs, LatLon rhs) {
		if (lhs == null) {
			String msg = Logging.getMessage("nullValue.LhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (rhs == null) {
			String msg = Logging.getMessage("nullValue.RhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		double lat1 = lhs.latitude.radians;
		double lon1 = lhs.longitude.radians;
		double lat2 = rhs.latitude.radians;
		double lon2 = rhs.longitude.radians;

		if (lat1 == lat2 && lon1 == lon2) return Angle.fromRadians(0);

		// "Haversine formula," taken from http://en.wikipedia.org/wiki/Great-circle_distance#Formul.C3.A6
		double a = Math.sin((lat2 - lat1) / 2.0);
		double b = Math.sin((lon2 - lon1) / 2.0);
		double c = a * a + +Math.cos(lat1) * Math.cos(lat2) * b * b;
		double distanceRadians = 2.0 * Math.asin(Math.sqrt(c));

		return Double.isNaN(distanceRadians) ? Angle.fromRadians(0) : Angle.fromRadians(distanceRadians);
	}

	/**
	 * Computes the location on a great circle arc with the given starting location, azimuth, and arc distance. This
	 * does not retain any reference to the location or angles, or modify them in any way.
	 * 
	 * @param location
	 *            the starting location.
	 * @param greatCircleAzimuth
	 *            great circle azimuth angle (clockwise from North).
	 * @param pathLength
	 *            arc distance to travel.
	 * @return a location on the great circle arc.
	 * @throws IllegalArgumentException
	 *             if any of the location, azimuth, or pathLength are <code>null</code>.
	 */
	public static LatLon greatCircleEndPosition(LatLon location, Angle greatCircleAzimuth, Angle pathLength) {
		if (location == null) {
			String msg = Logging.getMessage("nullValue.LocationIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (greatCircleAzimuth == null) {
			String msg = Logging.getMessage("nullValue.AzimuthIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (pathLength == null) {
			String msg = Logging.getMessage("nullValue.PathLengthIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		double lat = location.latitude.radians;
		double lon = location.longitude.radians;
		double azimuth = greatCircleAzimuth.radians;
		double distance = pathLength.radians;

		if (distance == 0) return location;

		// Taken from "Map Projections - A Working Manual", page 31, equation 5-5 and 5-6.
		double endLatRadians = Math.asin(Math.sin(lat) * Math.cos(distance) + Math.cos(lat) * Math.sin(distance) * Math.cos(azimuth));
		double endLonRadians = lon
				+ Math.atan2(Math.sin(distance) * Math.sin(azimuth), Math.cos(lat) * Math.cos(distance) - Math.sin(lat) * Math.sin(distance) * Math.cos(azimuth));

		if (Double.isNaN(endLatRadians) || Double.isNaN(endLonRadians)) return location;

		return LatLon.fromDegrees(Angle.normalizedDegreesLatitude(Angle.fromRadians(endLatRadians).degrees),
				Angle.normalizedDegreesLongitude(Angle.fromRadians(endLonRadians).degrees));
	}

	public static boolean locationsCrossDateline(LatLon p1, LatLon p2) {
		if (p1 == null || p2 == null) {
			String msg = Logging.getMessage("nullValue.LocationIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// A segment cross the line if end pos have different longitude signs
		// and are more than 180 degrees longitude apart
		if (Math.signum(p1.longitude.degrees) != Math.signum(p2.longitude.degrees)) {
			double delta = Math.abs(p1.longitude.degrees - p2.longitude.degrees);
			if (delta > 180 && delta < 360) return true;
		}

		return false;
	}

	/**
	 * Computes the azimuth angle (clockwise from North) of a rhumb line (a line of constant heading) between two
	 * locations. This does not retain any reference to the specified locations, or modify them in any way.
	 * 
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return azimuth the angle of a rhumb line between the two locations.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static Angle rhumbAzimuth(LatLon lhs, LatLon rhs) {
		if (lhs == null) {
			String msg = Logging.getMessage("nullValue.LhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (rhs == null) {
			String msg = Logging.getMessage("nullValue.RhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		double lat1 = lhs.latitude.radians;
		double lon1 = lhs.longitude.radians;
		double lat2 = rhs.latitude.radians;
		double lon2 = rhs.longitude.radians;

		if (lat1 == lat2 && lon1 == lon2) return Angle.fromRadians(0);

		// Taken from http://www.movable-type.co.uk/scripts/latlong.html
		double dLon = lon2 - lon1;
		double dPhi = Math.log(Math.tan(lat2 / 2.0 + Math.PI / 4.0) / Math.tan(lat1 / 2.0 + Math.PI / 4.0));
		// If lonChange over 180 take shorter rhumb across 180 meridian.
		if (Math.abs(dLon) > Math.PI) {
			dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
		}
		double azimuthRadians = Math.atan2(dLon, dPhi);

		return Double.isNaN(azimuthRadians) ? Angle.fromRadians(0) : Angle.fromRadians(azimuthRadians);
	}

	/**
	 * Computes the length of the rhumb line between two locations. The return value gives the distance as the angular
	 * distance between the two positions on the pi radius circle. In radians, this angle is also the arc length of the
	 * segment between the two positions on that circle. To compute a distance in meters from this value, multiply it by
	 * the radius of the globe. This does not retain any reference to the specified locations, or modify them in any
	 * way.
	 * 
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return the arc length of the rhumb line between the two locations. In radians, this value is the arc length on
	 *         the radius pi circle.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static Angle rhumbDistance(LatLon lhs, LatLon rhs) {
		if (lhs == null) {
			String msg = Logging.getMessage("nullValue.LhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (rhs == null) {
			String msg = Logging.getMessage("nullValue.RhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		double lat1 = lhs.latitude.radians;
		double lon1 = lhs.longitude.radians;
		double lat2 = rhs.latitude.radians;
		double lon2 = rhs.longitude.radians;

		if (lat1 == lat2 && lon1 == lon2) return Angle.fromRadians(0);

		// Taken from http://www.movable-type.co.uk/scripts/latlong.html
		double dLat = lat2 - lat1;
		double dLon = lon2 - lon1;
		double dPhi = Math.log(Math.tan(lat2 / 2.0 + Math.PI / 4.0) / Math.tan(lat1 / 2.0 + Math.PI / 4.0));
		double q = dLat / dPhi;
		if (Double.isNaN(dPhi) || Double.isNaN(q)) {
			q = Math.cos(lat1);
		}
		// If lonChange over 180 take shorter rhumb across 180 meridian.
		if (Math.abs(dLon) > Math.PI) {
			dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
		}

		double distanceRadians = Math.sqrt(dLat * dLat + q * q * dLon * dLon);

		return Double.isNaN(distanceRadians) ? Angle.fromRadians(0) : Angle.fromRadians(distanceRadians);
	}

	/**
	 * Computes the location on a rhumb line with the given starting location, rhumb azimuth, and arc distance along the
	 * line. This does not retain any reference to the specified location or angles, or modify them in any way.
	 * 
	 * @param location
	 *            the starting location.
	 * @param rhumbAzimuth
	 *            rhumb azimuth angle (clockwise from North).
	 * @param pathLength
	 *            arc distance to travel.
	 * @return a location on the rhumb line.
	 * @throws IllegalArgumentException
	 *             if any of the location, azimuth, or pathLength are <code>null</code>.
	 */
	public static LatLon rhumbEndPosition(LatLon location, Angle rhumbAzimuth, Angle pathLength) {
		if (location == null) {
			String msg = Logging.getMessage("nullValue.LocationIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (rhumbAzimuth == null) {
			String msg = Logging.getMessage("nullValue.AzimuthIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (pathLength == null) {
			String msg = Logging.getMessage("nullValue.PathLengthIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		double lat1 = location.latitude.radians;
		double lon1 = location.longitude.radians;
		double azimuth = rhumbAzimuth.radians;
		double distance = pathLength.radians;

		if (distance == 0) return location;

		// Taken from http://www.movable-type.co.uk/scripts/latlong.html
		double lat2 = lat1 + distance * Math.cos(azimuth);
		double dPhi = Math.log(Math.tan(lat2 / 2.0 + Math.PI / 4.0) / Math.tan(lat1 / 2.0 + Math.PI / 4.0));
		double q = (lat2 - lat1) / dPhi;
		if (Double.isNaN(dPhi) || Double.isNaN(q) || Double.isInfinite(q)) {
			q = Math.cos(lat1);
		}
		double dLon = distance * Math.sin(azimuth) / q;
		// Handle latitude passing over either pole.
		if (Math.abs(lat2) > Math.PI / 2.0) {
			lat2 = lat2 > 0 ? Math.PI - lat2 : -Math.PI - lat2;
		}
		double lon2 = (lon1 + dLon + Math.PI) % (2 * Math.PI) - Math.PI;

		if (Double.isNaN(lat2) || Double.isNaN(lon2)) return location;

		return LatLon.fromDegrees(Angle.normalizedDegreesLatitude(Angle.fromRadians(lat2).degrees), Angle.normalizedDegreesLongitude(Angle.fromRadians(lon2).degrees));
	}

	public LatLon copy() {
		return new LatLon(this.latitude.copy(), this.longitude.copy());
	}

	public LatLon set(LatLon location) {
		if (location == null) {
			String msg = Logging.getMessage("nullValue.LocationIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.latitude.set(location.latitude);
		this.longitude.set(location.longitude);

		return this;
	}

	public LatLon set(Angle latitude, Angle longitude) {
		if (latitude == null) {
			String msg = Logging.getMessage("nullValue.LatitudeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (longitude == null) {
			String msg = Logging.getMessage("nullValue.LongitudeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.latitude.set(latitude);
		this.longitude.set(longitude);

		return this;
	}

	public LatLon setDegrees(double latitude, double longitude) {
		this.latitude.setDegrees(latitude);
		this.longitude.setDegrees(longitude);

		return this;
	}

	public LatLon setRadians(double latitude, double longitude) {
		this.latitude.setRadians(latitude);
		this.longitude.setRadians(longitude);

		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || this.getClass() != o.getClass()) return false;

		LatLon that = (LatLon) o;
		return this.latitude.equals(that.latitude) && this.longitude.equals(that.longitude);
	}

	@Override
	public int hashCode() {
		int result;
		result = this.latitude.hashCode();
		result = 29 * result + this.longitude.hashCode();
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(this.latitude.toString()).append(", ");
		sb.append(this.longitude.toString());
		sb.append(")");
		return sb.toString();
	}
}
