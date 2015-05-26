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

// Created by plusminus on 19:06:38 - 25.09.2008
package eu.geopaparazzi.library.routing.osmbonuspack;


import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author Nicolas Gramlich
 *
 */
public class BoundingBoxE6 implements Parcelable, Serializable {

	// ===========================================================
	// Constants
	// ===========================================================

	static final long serialVersionUID = 2L;

	// ===========================================================
	// Fields
	// ===========================================================

	protected final int mLatNorthE6;
	protected final int mLatSouthE6;
	protected final int mLonEastE6;
	protected final int mLonWestE6;

	// ===========================================================
	// Constructors
	// ===========================================================

	public BoundingBoxE6(final int northE6, final int eastE6, final int southE6, final int westE6) {
		this.mLatNorthE6 = northE6;
		this.mLonEastE6 = eastE6;
		this.mLatSouthE6 = southE6;
		this.mLonWestE6 = westE6;
	}

	public BoundingBoxE6(final double north, final double east, final double south,
			final double west) {
		this.mLatNorthE6 = (int) (north * 1E6);
		this.mLonEastE6 = (int) (east * 1E6);
		this.mLatSouthE6 = (int) (south * 1E6);
		this.mLonWestE6 = (int) (west * 1E6);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	/**
	 * @return GeoPoint center of this BoundingBox
	 */
	public GeoPoint getCenter() {
		return new GeoPoint((this.mLatNorthE6 + this.mLatSouthE6) / 2,
				(this.mLonEastE6 + this.mLonWestE6) / 2);
	}

	public int getDiagonalLengthInMeters() {
		return new GeoPoint(this.mLatNorthE6, this.mLonWestE6).distanceTo(new GeoPoint(
				this.mLatSouthE6, this.mLonEastE6));
	}

	public int getLatNorthE6() {
		return this.mLatNorthE6;
	}

	public int getLatSouthE6() {
		return this.mLatSouthE6;
	}

	public int getLonEastE6() {
		return this.mLonEastE6;
	}

	public int getLonWestE6() {
		return this.mLonWestE6;
	}

	public int getLatitudeSpanE6() {
		return Math.abs(this.mLatNorthE6 - this.mLatSouthE6);
	}

	public int getLongitudeSpanE6() {
		return Math.abs(this.mLonEastE6 - this.mLonWestE6);
	}

	/**
	 *
	 * @param aLatitude
	 * @param aLongitude
	 * @param reuse
	 * @return relative position determined from the upper left corner.<br>
	 *         {0,0} would be the upper left corner. {1,1} would be the lower right corner. {1,0}
	 *         would be the lower left corner. {0,1} would be the upper right corner.
	 */
	public PointF getRelativePositionOfGeoPointInBoundingBoxWithLinearInterpolation(
			final int aLatitude, final int aLongitude, final PointF reuse) {
		final PointF out = (reuse != null) ? reuse : new PointF();
		final float y = ((float) (this.mLatNorthE6 - aLatitude) / getLatitudeSpanE6());
		final float x = 1 - ((float) (this.mLonEastE6 - aLongitude) / getLongitudeSpanE6());
		out.set(x, y);
		return out;
	}


	public GeoPoint getGeoPointOfRelativePositionWithLinearInterpolation(final float relX,
			final float relY) {

		int lat = (int) (this.mLatNorthE6 - (this.getLatitudeSpanE6() * relY));

		int lon = (int) (this.mLonWestE6 + (this.getLongitudeSpanE6() * relX));

		/* Bring into bounds. */
		while (lat > 90500000)
			lat -= 90500000;
		while (lat < -90500000)
			lat += 90500000;

		/* Bring into bounds. */
		while (lon > 180000000)
			lon -= 180000000;
		while (lon < -180000000)
			lon += 180000000;

		return new GeoPoint(lat, lon);
	}


	public BoundingBoxE6 increaseByScale(final float pBoundingboxPaddingRelativeScale) {
		final GeoPoint pCenter = this.getCenter();
		final int mLatSpanE6Padded_2 = (int) ((this.getLatitudeSpanE6() * pBoundingboxPaddingRelativeScale) / 2);
		final int mLonSpanE6Padded_2 = (int) ((this.getLongitudeSpanE6() * pBoundingboxPaddingRelativeScale) / 2);

		return new BoundingBoxE6(pCenter.getLatitudeE6() + mLatSpanE6Padded_2,
				pCenter.getLongitudeE6() + mLonSpanE6Padded_2, pCenter.getLatitudeE6()
						- mLatSpanE6Padded_2, pCenter.getLongitudeE6() - mLonSpanE6Padded_2);
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	public String toString() {
		return new StringBuffer().append("N:").append(this.mLatNorthE6).append("; E:")
				.append(this.mLonEastE6).append("; S:").append(this.mLatSouthE6).append("; W:")
				.append(this.mLonWestE6).toString();
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public GeoPoint bringToBoundingBox(final int aLatitudeE6, final int aLongitudeE6) {
		return new GeoPoint(Math.max(this.mLatSouthE6, Math.min(this.mLatNorthE6, aLatitudeE6)),
				Math.max(this.mLonWestE6, Math.min(this.mLonEastE6, aLongitudeE6)));
	}

	public static BoundingBoxE6 fromGeoPoints(final ArrayList<? extends GeoPoint> partialPolyLine) {
		int minLat = Integer.MAX_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int maxLon = Integer.MIN_VALUE;
		for (final GeoPoint gp : partialPolyLine) {
			final int latitudeE6 = gp.getLatitudeE6();
			final int longitudeE6 = gp.getLongitudeE6();

			minLat = Math.min(minLat, latitudeE6);
			minLon = Math.min(minLon, longitudeE6);
			maxLat = Math.max(maxLat, latitudeE6);
			maxLon = Math.max(maxLon, longitudeE6);
		}

		return new BoundingBoxE6(maxLat, maxLon, minLat, minLon);
	}

	public boolean contains(final GeoPoint pGeoPoint) {
		return contains(pGeoPoint.getLatitudeE6(), pGeoPoint.getLongitudeE6());
	}

	public boolean contains(final int aLatitudeE6, final int aLongitudeE6) {
		return ((aLatitudeE6 < this.mLatNorthE6) && (aLatitudeE6 > this.mLatSouthE6))
				&& ((aLongitudeE6 < this.mLonEastE6) && (aLongitudeE6 > this.mLonWestE6));
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	// ===========================================================
	// Parcelable
	// ===========================================================

	public static final Parcelable.Creator<BoundingBoxE6> CREATOR = new Parcelable.Creator<BoundingBoxE6>() {
		@Override
		public BoundingBoxE6 createFromParcel(final Parcel in) {
			return readFromParcel(in);
		}

		@Override
		public BoundingBoxE6[] newArray(final int size) {
			return new BoundingBoxE6[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel out, final int arg1) {
		out.writeInt(this.mLatNorthE6);
		out.writeInt(this.mLonEastE6);
		out.writeInt(this.mLatSouthE6);
		out.writeInt(this.mLonWestE6);
	}

	private static BoundingBoxE6 readFromParcel(final Parcel in) {
		final int latNorthE6 = in.readInt();
		final int lonEastE6 = in.readInt();
		final int latSouthE6 = in.readInt();
		final int lonWestE6 = in.readInt();
		return new BoundingBoxE6(latNorthE6, lonEastE6, latSouthE6, lonWestE6);
	}
}