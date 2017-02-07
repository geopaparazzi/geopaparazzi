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
package eu.geopaparazzi.core.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.Collection;

import eu.geopaparazzi.library.core.activities.LogAnalysisActivity;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.GPLogPreferencesHandler;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.R;

/**
 * The advanced settings activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AdvancedSettingsActivity extends AppCompatActivity implements CheckBox.OnCheckedChangeListener {

    private CheckBox logCheckbox;
    private CheckBox logHeavyCheckbox;
    private CheckBox logAbsurdCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advancedsettings);

        checkMockSettings();

        Toolbar toolbar = (Toolbar) findViewById(eu.geopaparazzi.core.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final CheckBox demoCheckbox = (CheckBox) findViewById(R.id.demoCheckbox);
        final boolean isDemoMode = preferences.getBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
        demoCheckbox.setChecked(isDemoMode);
        demoCheckbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !GpsServiceUtilities.isMockSettingsON(AdvancedSettingsActivity.this)) {
                    GPDialogs.warningDialog(AdvancedSettingsActivity.this, getString(R.string.enable_mock_locations_for_demo), null);
                    demoCheckbox.setChecked(false);
                }

                Editor edit = preferences.edit();
                if (isChecked) {
                    edit.putBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, true);
                } else {
                    edit.putBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
                }
                edit.apply();
            }
        });

        initLogs(preferences);
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkMockSettings();
    }

    private void checkMockSettings() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (!GpsServiceUtilities.isMockSettingsON(AdvancedSettingsActivity.this)) {
            Editor edit = preferences.edit();
            edit.putBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
            edit.apply();
        }
    }

    private void initLogs(final SharedPreferences preferences) {
        logCheckbox = (CheckBox) findViewById(R.id.logCheckbox);
        logCheckbox.setOnCheckedChangeListener(this);
        logCheckbox.setChecked(GPLogPreferencesHandler.checkLog(preferences));

        logHeavyCheckbox = (CheckBox) findViewById(R.id.logHeavyCheckbox);
        logHeavyCheckbox.setOnCheckedChangeListener(this);
        logHeavyCheckbox.setChecked(GPLogPreferencesHandler.checkLogHeavy(preferences));

        logAbsurdCheckbox = (CheckBox) findViewById(R.id.logAbsCheckbox);
        logAbsurdCheckbox.setOnCheckedChangeListener(this);
        logAbsurdCheckbox.setChecked(GPLogPreferencesHandler.checkLogAbsurd(preferences));
    }

    public void onCheckedChanged(CompoundButton checkBox, boolean newState) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (logCheckbox != null && logCheckbox == checkBox)
            if (newState) {
                GPLogPreferencesHandler.setLog(true, preferences);
            } else {
                GPLogPreferencesHandler.setLog(false, preferences);
                GPLogPreferencesHandler.setLogHeavy(false, preferences);
                GPLogPreferencesHandler.setLogAbsurd(false, preferences);
                logHeavyCheckbox.setChecked(false);
                logAbsurdCheckbox.setChecked(false);
            }
        if (logHeavyCheckbox != null && logHeavyCheckbox == checkBox)
            if (newState) {
                GPLogPreferencesHandler.setLog(true, preferences);
                GPLogPreferencesHandler.setLogHeavy(true, preferences);
                logCheckbox.setChecked(true);
            } else {
                GPLogPreferencesHandler.setLogHeavy(false, preferences);
                GPLogPreferencesHandler.setLogAbsurd(false, preferences);
                logAbsurdCheckbox.setChecked(false);
            }
        if (logAbsurdCheckbox != null && logAbsurdCheckbox == checkBox)
            if (newState) {
                GPLogPreferencesHandler.setLog(true, preferences);
                GPLogPreferencesHandler.setLogHeavy(true, preferences);
                GPLogPreferencesHandler.setLogAbsurd(true, preferences);
                logCheckbox.setChecked(true);
                logHeavyCheckbox.setChecked(true);
            } else {
                GPLogPreferencesHandler.setLogAbsurd(false, preferences);
            }
    }

    /**
     * Start sql view.
     *
     * @param view parent.
     */
    public void startSqlView(View view) {
        Intent sqlViewIntent = new Intent(this, SqlViewActivity.class);
        startActivity(sqlViewIntent);
    }

    /**
     * Start Analyze log view.
     *
     * @param view parent.
     */
    public void analyzeLog(View view) {
        Intent analyzeLogViewIntent = new Intent(this, LogAnalysisActivity.class);
        startActivity(analyzeLogViewIntent);
    }

    /**
     * Clear log.
     *
     * @param view parent.
     */
    public void clearLog(View view) {
        try {
            SQLiteDatabase database = GeopaparazziApplication.getInstance().getDatabase();
            GPLog.clearLogTable(database);
            GPDialogs.infoDialog(this, getString(R.string.log_cleared), null);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(this, e, null);
        }
    }

    /**
     * Reset style tables.
     *
     * @param view parent.
     */
    public void resetStyleTables(View view) {
        try {
            Collection<SpatialiteDatabaseHandler> spatialDatabaseHandlers = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2DbHandlersMap().values();
            for (AbstractSpatialDatabaseHandler iSpatialDatabaseHandler : spatialDatabaseHandlers) {
                if (iSpatialDatabaseHandler instanceof SpatialiteDatabaseHandler) {
                    SpatialiteDatabaseHandler sdHandler = (SpatialiteDatabaseHandler) iSpatialDatabaseHandler;
                    sdHandler.resetStyleTable();
                }
            }
            GPDialogs.infoDialog(this, "Style reset performed.", null);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(this, e, null);
        }
    }

}
