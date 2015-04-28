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

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;

/** Road Leg is the portion of the road between 2 waypoints (intermediate points requested) 
 * 
 * @author M.Kergall
 * 
 */
public class RoadLeg implements Parcelable {
	/** in km */
	public double mLength; 
	/** in sec */
	public double mDuration; 
	/** starting node of the leg, as index in nodes array */
	public int mStartNodeIndex;
	/** and ending node */
	public int mEndNodeIndex; 
	
	public RoadLeg(){
		mLength = mDuration = 0.0;
		mStartNodeIndex = mEndNodeIndex = 0;
	}
	
	public RoadLeg(int startNodeIndex, int endNodeIndex, 
			ArrayList<RoadNode> nodes){
		mStartNodeIndex = startNodeIndex;
		mEndNodeIndex = endNodeIndex;
		mLength = mDuration = 0.0;
		for (int i=startNodeIndex; i<=endNodeIndex; i++){ //TODO: <= or < ??? To check. 
			RoadNode node = nodes.get(i);
			mLength += node.mLength;
			mDuration += node.mDuration;
		}
		Log.d(BonusPackHelper.LOG_TAG, "Leg: " + startNodeIndex + "-" + endNodeIndex
				+ ", length=" + mLength + "km, duration="+mDuration+"s");
	}

	//--- Parcelable implementation
	
	@Override public int describeContents() {
		return 0;
	}

	@Override public void writeToParcel(Parcel out, int flags) {
		out.writeDouble(mLength);
		out.writeDouble(mDuration);
		out.writeInt(mStartNodeIndex);
		out.writeInt(mEndNodeIndex);
	}
	
	public static final Creator<RoadLeg> CREATOR = new Creator<RoadLeg>() {
		@Override public RoadLeg createFromParcel(Parcel source) {
			return new RoadLeg(source);
		}
		@Override public RoadLeg[] newArray(int size) {
			return new RoadLeg[size];
		}
	};
	
	private RoadLeg(Parcel in){
		mLength = in.readDouble();
		mDuration = in.readDouble();
		mStartNodeIndex = in.readInt();
		mEndNodeIndex = in.readInt();
	}
}
