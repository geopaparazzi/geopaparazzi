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

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.mapsforge.android.maps.PausableThread;

import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;

class ScreenshotCapturer extends PausableThread {
 private static final String SCREENSHOT_DIRECTORY = "Pictures";
 private static final String SCREENSHOT_FILE_NAME = "Map screenshot";
 private static final int SCREENSHOT_QUALITY = 90;
 private static final String THREAD_NAME = "ScreenshotCapturer";

 private final MapsDirActivity mapsdir_Activity;
 private CompressFormat compressFormat;

 ScreenshotCapturer(MapsDirActivity mapsdir_Activity) {
  this.mapsdir_Activity = mapsdir_Activity;
 }

 private File assembleFilePath(File directory) {
  StringBuilder strinBuilder = new StringBuilder();
  strinBuilder.append(directory.getAbsolutePath());
  strinBuilder.append(File.separatorChar);
  strinBuilder.append(SCREENSHOT_FILE_NAME);
  strinBuilder.append('.');
  strinBuilder.append(this.compressFormat.name().toLowerCase(Locale.ENGLISH));
  return new File(strinBuilder.toString());
 }

 @Override
 protected void doWork() {
  try {
   File directory = new File(Environment.getExternalStorageDirectory(), SCREENSHOT_DIRECTORY);
   if (!directory.exists() && !directory.mkdirs()) {
    this.mapsdir_Activity.showToastOnUiThread("Could not create screenshot directory");
    return;
   }

   File outputFile = assembleFilePath(directory);
   if (this.mapsdir_Activity.mapView.takeScreenshot(this.compressFormat, SCREENSHOT_QUALITY, outputFile)) {
    this.mapsdir_Activity.showToastOnUiThread(outputFile.getAbsolutePath());
   } else {
    this.mapsdir_Activity.showToastOnUiThread("Screenshot could not be saved");
   }
  } catch (IOException e) {
   this.mapsdir_Activity.showToastOnUiThread(e.getMessage());
  }

  this.compressFormat = null;
 }

 @Override
 protected String getThreadName() {
  return THREAD_NAME;
 }

 @Override
 protected boolean hasWork() {
  return this.compressFormat != null;
 }

 void captureScreenShot(CompressFormat screenShotFormat) {
  this.compressFormat = screenShotFormat;
  synchronized (this) {
   notify();
  }
 }
}
