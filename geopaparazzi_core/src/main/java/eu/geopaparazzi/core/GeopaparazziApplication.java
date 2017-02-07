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
package eu.geopaparazzi.core;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;

import java.io.IOException;

import eu.geopaparazzi.core.database.DatabaseManager;
import eu.geopaparazzi.library.GPApplication;

/**
 * Application singleton.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopaparazziApplication extends GPApplication {

    private static SQLiteDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();

        ACRAConfiguration config = ACRA.getConfig();
        config.setMailTo("feedback@geopaparazzi.eu");
        config.setCustomReportContent(new ReportField[]{//
/*    */ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, //
                ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, //
                ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT});
        config.setResToastText(R.string.crash_toast_text);
        config.setLogcatArguments(new String[]{"-t", "400", "-v", "time", "GPLOG:I", "*:S"});

        try
        {
            config.setMode(ReportingInteractionMode.TOAST);
        }
        catch (ACRAConfigurationException e)
        {
            e.printStackTrace();
            return;
        }

        ACRA.init(getInstance(), config);

        Log.i("GEOPAPARAZZIAPPLICATION", "ACRA Initialized."); //$NON-NLS-1$//$NON-NLS-2$
    }

    @Override
    public synchronized SQLiteDatabase getDatabase() throws IOException {
        if (database == null) {
            DatabaseManager databaseManager = new DatabaseManager();
            database = databaseManager.getDatabase(getInstance());
        }
        return database;
    }

    @Override
    public void closeDatabase() {
        if (database != null && database.isOpen()) {
            database.close();
        }
        database = null;
    }

    public static void reset(){
        database = null;
    }
}
