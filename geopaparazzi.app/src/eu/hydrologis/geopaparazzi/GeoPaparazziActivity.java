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

import static eu.hydrologis.geopaparazzi.util.Constants.GPSLAST_LATITUDE;
import static eu.hydrologis.geopaparazzi.util.Constants.GPSLAST_LONGITUDE;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.compass.CompassView;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.DatabaseManager;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.kml.KmlExport;
import eu.hydrologis.geopaparazzi.osm.DataManager;
import eu.hydrologis.geopaparazzi.osm.MapItem;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Line;
import eu.hydrologis.geopaparazzi.util.LogToggleButton;
import eu.hydrologis.geopaparazzi.util.Note;
import eu.hydrologis.geopaparazzi.util.Picture;

/**
 * The main {@link Activity activity} of GeoPaparazzi.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoPaparazziActivity extends Activity {

    private static final String LOGTAG = "GEOPAPARAZZIACTIVITY";

    private static final int MENU_ABOUT = Menu.FIRST;
    private static final int MENU_OSM = 2;
    private static final int MENU_EXIT = 3;
    private static final int MENU_KMLEXPORT = 4;
    private static final int MENU_SETTINGS = 5;

    private ApplicationManager applicationManager;
    private CompassView compassView;

    private ProgressDialog kmlProgressDialog;

    private LogToggleButton logButton;

    private File kmlOutputFile = null;

    private boolean isChecked = false;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        setContentView(R.layout.main);

        Object stateObj = getLastNonConfigurationInstance();
        if (stateObj instanceof ApplicationManager) {
            applicationManager = (ApplicationManager) stateObj;
        } else {
            ApplicationManager.resetManager();
            applicationManager = ApplicationManager.getInstance(this);
        }

        /*
         * the compass view
         */
        LinearLayout uppercol1View = (LinearLayout) findViewById(R.id.uppercol1);
        TextView compassInfoView = (TextView) findViewById(R.id.compassInfoView);
        GpsLocation tmpLoc = applicationManager.getLoc();
        compassView = new CompassView(this, compassInfoView, applicationManager, tmpLoc);
        LinearLayout.LayoutParams tmpParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        uppercol1View.addView(compassView, tmpParams);
        applicationManager.removeCompassListener();
        applicationManager.addListener(compassView);

        /*
         * the buttons
         */
        Button photoButton = (Button) findViewById(R.id.photoButton);
        photoButton.setText(R.string.text_take_picture);
        photoButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                GpsLocation loc = applicationManager.getLoc();
                if (loc != null) {
                    Intent intent = new Intent(Constants.TAKE_PICTURE);
                    startActivity(intent);
                } else {
                    ApplicationManager.openDialog(R.string.gpslogging_only, GeoPaparazziActivity.this);
                }
            }
        });

        logButton = (LogToggleButton) findViewById(R.id.logButton);
        isChecked = applicationManager.isGpsLogging();
        logButton.setChecked(isChecked);
        logButton.setIsLandscape(isLandscape());
        logButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                isChecked = logButton.isChecked();
                if (isChecked) {
                    GpsLocation loc = applicationManager.getLoc();
                    if (loc != null) {
                        applicationManager.doLogGps(true);
                    } else {
                        ApplicationManager.openDialog(R.string.gpslogging_only, GeoPaparazziActivity.this);
                        isChecked = !isChecked;
                        logButton.setChecked(isChecked);
                    }
                } else {
                    applicationManager.doLogGps(false);
                }
            }
        });

        Button noteButton = (Button) findViewById(R.id.noteButton);
        noteButton.setText(R.string.text_take_a_gps_note);
        noteButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                GpsLocation loc = applicationManager.getLoc();
                if (loc != null) {
                    Intent intent = new Intent(Constants.TAKE_NOTE);
                    startActivity(intent);
                } else {
                    ApplicationManager.openDialog(R.string.gpslogging_only, GeoPaparazziActivity.this);
                }
            }
        });

        Button mapButton = (Button) findViewById(R.id.mapButton);
        mapButton.setText(R.string.text_show_position_on_map);
        mapButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View view ) {
                Intent intent2 = new Intent(Constants.VIEW_IN_OSM);
                startActivity(intent2);
            }
        });

        try {
            DatabaseManager.getInstance().getDatabase(this);
            checkMapsAndLogsVisibility();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.databaseError, Toast.LENGTH_LONG).show();
        }
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_SETTINGS, 0, R.string.mainmenu_preferences).setIcon(R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, MENU_OSM, 1, R.string.osmview).setIcon(R.drawable.menu_mapmode);
        menu.add(Menu.NONE, MENU_KMLEXPORT, 2, R.string.mainmenu_kmlexport).setIcon(R.drawable.kmlexport);
        menu.add(Menu.NONE, MENU_EXIT, 3, R.string.exit).setIcon(R.drawable.exit);
        menu.add(Menu.NONE, MENU_ABOUT, 4, R.string.about).setIcon(R.drawable.about);

        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MENU_ABOUT:
            Intent intent = new Intent(Constants.ABOUT);
            startActivity(intent);

            return true;
        case MENU_OSM:
            Intent intent2 = new Intent(Constants.VIEW_IN_OSM);
            startActivity(intent2);

            return true;
        case MENU_KMLEXPORT:
            exportToKml();
            return true;

        case MENU_SETTINGS:
            Intent preferencesIntent = new Intent(Constants.PREFERENCES);
            startActivity(preferencesIntent);
            return true;
        case MENU_EXIT:
            finish();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public void finish() {
        Log.d(LOGTAG, "Finish called!");
        // save last location just in case
        GpsLocation loc = applicationManager.getLoc();
        if (loc != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Editor editor = preferences.edit();
            editor.putFloat(GPSLAST_LONGITUDE, (float) loc.getLongitude());
            editor.putFloat(GPSLAST_LATITUDE, (float) loc.getLatitude());
            editor.commit();
        }

        Toast.makeText(this, R.string.loggingoff, Toast.LENGTH_LONG).show();
        // stop all logging
        applicationManager.stopListening();
        applicationManager.doLogGps(false);
        DatabaseManager.getInstance().closeDatabase();
        super.finish();
    }

    private boolean isLandscape() {
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        return width > height ? true : false;
    }

    private Handler kmlHandler = new Handler(){
        public void handleMessage( android.os.Message msg ) {
            kmlProgressDialog.dismiss();
            if (kmlOutputFile.exists()) {
                Toast.makeText(GeoPaparazziActivity.this, R.string.kmlsaved + kmlOutputFile.getAbsolutePath(), Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(GeoPaparazziActivity.this, R.string.kmlnonsaved, Toast.LENGTH_LONG).show();
            }
        };
    };

    private void exportToKml() {

        kmlProgressDialog = ProgressDialog.show(this, "Exporting to kml...", "", true, true);
        new Thread(){

            public void run() {
                try {
                    /*
                     * add gps logs
                     */
                    HashMap<Long, Line> linesList = DaoGpsLog.getLinesMap(GeoPaparazziActivity.this);
                    /*
                     * get notes
                     */
                    List<Note> notesList = DaoNotes.getNotesList(GeoPaparazziActivity.this);
                    /*
                     * add pictures
                     */
                    List<Picture> picturesList = applicationManager.getPictures();

                    File kmlExportDir = applicationManager.getKmlExportDir();
                    String filename = "geopaparazzi_" + Constants.TIMESTAMPFORMATTER.format(new Date()) + ".kmz";
                    kmlOutputFile = new File(kmlExportDir, filename);
                    KmlExport export = new KmlExport(null, kmlOutputFile);
                    export.export(notesList, linesList, picturesList);

                    kmlHandler.sendEmptyMessage(0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(GeoPaparazziActivity.this, R.string.kmlnonsaved, Toast.LENGTH_LONG).show();
                }
            }
        }.start();
    }

    private void checkMapsAndLogsVisibility() throws IOException {
        List<MapItem> maps = DaoMaps.getMaps(this);
        boolean oneVisible = false;
        for( MapItem item : maps ) {
            if (!oneVisible && item.isVisible()) {
                oneVisible = true;
            }
        }
        DataManager.getInstance().setMapsVisible(oneVisible);

        maps = DaoGpsLog.getGpslogs(this);
        oneVisible = false;
        for( MapItem item : maps ) {
            if (!oneVisible && item.isVisible()) {
                oneVisible = true;
            }
        }
        DataManager.getInstance().setLogsVisible(oneVisible);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return applicationManager;
    }

}