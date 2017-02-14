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

// Created by plusminus on 19:06:38 - 25.09.2008
package eu.geopaparazzi.library.routing.osmbonuspack;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class BoundingBox implements Parcelable, Serializable {

	// ===========================================================
	// Constants
	// ===========================================================

	static final long serialVersionUID = 2L;

	// ===========================================================
	// Fields
	// ===========================================================

	protected final double mLatNorth;
	protected final double mLatSouth;
	protected final double mLonEast;
	protected final double mLonWest;

	// ===========================================================
	// Constructors
	// ===========================================================

	public BoundingBox(final double north, final double east, final double south, final double west) {
		this.mLatNorth = north;
		this.mLonEast = east;
		this.mLatSouth = south;
		this.mLonWest = west;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	/**
	 * @return GeoPoint center of this BoundingBox
	 */
	public GeoPoint getCenter() {
		return new GeoPoint((this.mLatNorth + this.mLatSouth) / 2.0,
				(this.mLonEast + this.mLonWest) / 2.0);
	}

	public double getDiagonalLengthInMeters() {
		return new GeoPoint(this.mLatNorth, this.mLonWest).distanceTo(new GeoPoint(
				this.mLatSouth, this.mLonEast));
	}

	public double getLatNorth() {
		return this.mLatNorth;
	}

	public double getLatSouth() {
		return this.mLatSouth;
	}

	public double getLonEast() {
		return this.mLonEast;
	}

	public double getLonWest() {
		return this.mLonWest;
	}

	public double getLatitudeSpan() {
		return Math.abs(this.mLatNorth - this.mLatSouth);
	}

	public double getLongitudeSpan() {
		return Math.abs(this.mLonEast - this.mLonWest);
	}


	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	public String toString() {
		return new StringBuffer().append("N:").append(this.mLatNorth).append("; E:")
				.append(this.mLonEast).append("; S:").append(this.mLatSouth).append("; W:")
				.append(this.mLonWest).toString();
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public GeoPoint bringToBoundingBox(final double aLatitude, final double aLongitude) {
		return new GeoPoint(Math.max(this.mLatSouth, Math.min(this.mLatNorth, aLatitude)),
				Math.max(this.mLonWest, Math.min(this.mLonEast, aLongitude)));
	}

	public static BoundingBox fromGeoPoints(final List<? extends IGeoPoint> partialPolyLine) {
		double minLat = Double.MAX_VALUE;
		double minLon = Double.MAX_VALUE;
		double maxLat = -Double.MAX_VALUE;
		double maxLon = -Double.MAX_VALUE;
		for (final IGeoPoint gp : partialPolyLine) {
			final double latitude = gp.getLatitude();
			final double longitude = gp.getLongitude();

			minLat = Math.min(minLat, latitude);
			minLon = Math.min(minLon, longitude);
			maxLat = Math.max(maxLat, latitude);
			maxLon = Math.max(maxLon, longitude);
		}

		return new BoundingBox(maxLat, maxLon, minLat, minLon);
	}

	public boolean contains(final IGeoPoint pGeoPoint) {
		return contains(pGeoPoint.getLatitude(), pGeoPoint.getLongitude());
	}

	public boolean contains(final double aLatitude, final double aLongitude) {
		return ((aLatitude < this.mLatNorth) && (aLatitude > this.mLatSouth))
				&& ((aLongitude < this.mLonEast) && (aLongitude > this.mLonWest));
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	// ===========================================================
	// Parcelable
	// ===========================================================

	public static final Parcelable.Creator<BoundingBox> CREATOR = new Parcelable.Creator<BoundingBox>() {
		@Override
		public BoundingBox createFromParcel(final Parcel in) {
			return readFromParcel(in);
		}

		@Override
		public BoundingBox[] newArray(final int size) {
			return new BoundingBox[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel out, final int arg1) {
		out.writeDouble(this.mLatNorth);
		out.writeDouble(this.mLonEast);
		out.writeDouble(this.mLatSouth);
		out.writeDouble(this.mLonWest);
	}

	private static BoundingBox readFromParcel(final Parcel in) {
		final double latNorth = in.readDouble();
		final double lonEast = in.readDouble();
		final double latSouth = in.readDouble();
		final double lonWest = in.readDouble();
		return new BoundingBox(latNorth, lonEast, latSouth, lonWest);
	}

	@Deprecated
	public int getLatitudeSpanE6() {
		return (int)(getLatitudeSpan() * 1E6);
	}

	@Deprecated
	public int getLongitudeSpanE6() {
		return (int)(getLongitudeSpan() * 1E6);
	}
}