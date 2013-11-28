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
package eu.hydrologis.geopaparazzi;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.util.Log;
import eu.geopaparazzi.library.GeopaparazziLibraryContextHolder;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteContextHolder;

/**
 * Application singleton.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@ReportsCrashes(//
formKey = "", //
mailTo = "feedback@geopaparazzi.eu", //
customReportContent = {//
/*    */ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, //
        ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, //
        ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT}, //
mode = ReportingInteractionMode.TOAST, //
resToastText = R.string.crash_toast_text, //
logcatArguments = {"-t", "400", "-v", "time", "GPLOG:I", "*:S"})
public class GeopaparazziApplication extends Application {

    private static GeopaparazziApplication instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        GeopaparazziLibraryContextHolder.INSTANCE.setContext(instance);
        SpatialiteContextHolder.INSTANCE.setContext(instance);

        ACRA.init(instance);
        Log.i("TRACKOIDAPPLICATION", "ACRA Initialized.");

        if (GPLog.LOG_ANDROID) {
            Log.i(getClass().getSimpleName(), "GeopaparazziApplication singleton created.");
        }
    }

    public static GeopaparazziApplication getInstance() {
        return instance;
    }
}
