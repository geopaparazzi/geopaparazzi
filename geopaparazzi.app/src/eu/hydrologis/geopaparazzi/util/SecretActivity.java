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
package eu.hydrologis.geopaparazzi.util;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.GPLogPreferencesHandler;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DatabaseManager;
import eu.hydrologis.geopaparazzi.database.SqlViewActivity;

/**
 * The hidden activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SecretActivity extends Activity {
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hiddenpanel);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        CheckBox demoCheckbox = (CheckBox) findViewById(R.id.demoCheckbox);
        demoCheckbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                Editor edit = preferences.edit();
                if (isChecked) {
                    edit.putBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, true);
                } else {
                    edit.putBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
                }
                edit.commit();
            }
        });
        boolean isDemoMode = preferences.getBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
        demoCheckbox.setChecked(isDemoMode);

        checkLogs(preferences);
    }

    private void checkLogs( final SharedPreferences preferences ) {
        CheckBox logCheckbox = (CheckBox) findViewById(R.id.logCheckbox);
        logCheckbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                GPLogPreferencesHandler.setLog(isChecked, preferences);
            }
        });
        logCheckbox.setChecked(GPLogPreferencesHandler.checkLog(preferences));

        CheckBox logHeavyCheckbox = (CheckBox) findViewById(R.id.logHeavyCheckbox);
        logHeavyCheckbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                GPLogPreferencesHandler.setLogHeavy(isChecked, preferences);
            }
        });
        logHeavyCheckbox.setChecked(GPLogPreferencesHandler.checkLogHeavy(preferences));

        CheckBox logAbsurdCheckbox = (CheckBox) findViewById(R.id.logAbsCheckbox);
        logAbsurdCheckbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                GPLogPreferencesHandler.setLogAbsurd(isChecked, preferences);
            }
        });
        logAbsurdCheckbox.setChecked(GPLogPreferencesHandler.checkLogAbsurd(preferences));

    }

    public void startSqlView( View view ) {
        Intent sqlViewIntent = new Intent(this, SqlViewActivity.class);
        startActivity(sqlViewIntent);
    }

    public void clearLog( View view ) {
        try {
            SQLiteDatabase database = DatabaseManager.getInstance().getDatabase(this);
            GPLog.clearLogTable(database);
            Utilities.messageDialog(this, "Log cleared.", null);
        } catch (Exception e) {
            e.printStackTrace();
            Utilities.messageDialog(this, "An error occurred: " + e.getLocalizedMessage(), null);
        }
    }

    private int backCount = 0;
    public boolean onKeyDown( int keyCode, KeyEvent event ) {
        // force to exit through the exit button
        // System.out.println(keyCode + "/" + KeyEvent.KEYCODE_BACK);
        switch( keyCode ) {
        case KeyEvent.KEYCODE_BACK:
            backCount++;
            if (backCount > 3) {
                backCount = 0;
            } else {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
