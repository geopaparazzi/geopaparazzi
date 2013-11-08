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
package eu.geopaparazzi.mapsdirmanager;

import org.mapsforge.core.model.GeoPoint;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

class MyLocationListener implements LocationListener {
 private final MapsDirActivity mapsdir_Activity;
 private boolean centerAtFirstFix;

 MyLocationListener(MapsDirActivity mapsdir_Activity) {
  this.mapsdir_Activity = mapsdir_Activity;
 }

 @Override
 public void onLocationChanged(Location location) {
  if (!this.mapsdir_Activity.isShowMyLocationEnabled()) {
   return;
  }

  GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
  this.mapsdir_Activity.overlayCircle.setCircleData(point, location.getAccuracy());
  this.mapsdir_Activity.overlayItem.setPoint(point);
  this.mapsdir_Activity.circleOverlay.requestRedraw();
  this.mapsdir_Activity.itemizedOverlay.requestRedraw();
  if (this.centerAtFirstFix || this.mapsdir_Activity.isSnapToLocationEnabled()) {
   this.centerAtFirstFix = false;
   this.mapsdir_Activity.mapController.setCenter(point);
  }
 }

 @Override
 public void onProviderDisabled(String provider) {
  // do nothing
 }

 @Override
 public void onProviderEnabled(String provider) {
  // do nothing
 }

 @Override
 public void onStatusChanged(String provider, int status, Bundle extras) {
  // do nothing
 }

 boolean isCenterAtFirstFix() {
  return this.centerAtFirstFix;
 }

 void setCenterAtFirstFix(boolean centerAtFirstFix) {
  this.centerAtFirstFix = centerAtFirstFix;
 }
}
