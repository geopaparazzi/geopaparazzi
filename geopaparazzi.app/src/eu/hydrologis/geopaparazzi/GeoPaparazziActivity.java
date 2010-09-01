/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.geopaparazzi;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
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

    private ApplicationManager deviceManager;
    private ImageButton cameraButton;
    private CompassView compassView;
    private LinearLayout mainLayout;

    private ImageButton gpsLogButton;
    private ImageButton notesButton;
    private TextView cameraLabelText;
    private TextView notesLabelText;
    private TextView gpsLabelText;
    private TextView mapLabelText;

    private ProgressDialog kmlProgressDialog;

    private File kmlOutputFile = null;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        deviceManager = ApplicationManager.getInstance(this);
        deviceManager.activateManagers();

        deviceManager.checkGps();

        deviceManager.startListening();

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.WHITE);

        /*
         * the compass view
         */
        compassView = new CompassView(this);
        LayoutParams compassParams = new LayoutParams(Constants.COMPASS_CANVAS_WIDTH, Constants.COMPASS_CANVAS_HEIGHT);
        compassView.setLayoutParams(compassParams);
        mainLayout.addView(compassView);

        /*
         * the buttons
         */
        GridView buttonGridView = new GridView(this);
        buttonGridView.setId(R.id.maingridview);
        buttonGridView.setLayoutParams(new GridView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        buttonGridView.setNumColumns(2);
        buttonGridView.setVerticalSpacing(10);
        buttonGridView.setHorizontalSpacing(10);
        buttonGridView.setColumnWidth(90);
        buttonGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);

        buttonGridView.setAdapter(new ButtonAdapter(this));
        mainLayout.addView(buttonGridView);

        setContentView(mainLayout);

        try {
            DatabaseManager.getInstance().getDatabase();
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
        Toast.makeText(this, R.string.loggingoff, Toast.LENGTH_LONG).show();
        // stop all logging
        deviceManager.stopListening();
        deviceManager.doLogGps(false);
        DatabaseManager.getInstance().closeDatabase();
        super.finish();
    }

    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged(newConfig);
        int orientation = mainLayout.getOrientation();
        orientation = (orientation == LinearLayout.VERTICAL ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        mainLayout.setOrientation(orientation);
        setContentView(mainLayout);
    }

    public void updateFromDeviceManager() {
        // int accuracy = deviceManager.getAccuracy();
        // double pitch = deviceManager.getPitch();
        // double roll = deviceManager.getRoll();
        double azimuth = deviceManager.getAzimuth();
        GpsLocation loc = deviceManager.getLoc();

        compassView.setLocationInfo((float) azimuth, loc);
        compassView.invalidate();
    }

    public void onWindowFocusChanged( boolean hasFocus ) {
        if (hasFocus) {
            fixGpsButton();
        }
        super.onWindowFocusChanged(hasFocus);
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
                    HashMap<Long, Line> linesList = DaoGpsLog.getLinesMap();
                    /*
                     * get notes
                     */
                    List<Note> notesList = DaoNotes.getNotesList();
                    /*
                     * add pictures
                     */
                    List<Picture> picturesList = ApplicationManager.getInstance().getPictures();

                    File kmlExportDir = ApplicationManager.getInstance().getKmlExportDir();
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

    /**
     * workaround for gps log button getting resetted
     * whenever the camera is opened and closed.  
     */
    private void fixGpsButton() {
        Log.d(LOGTAG, "Is logging = " + deviceManager.isGpsLogging());
        if (deviceManager.isGpsLogging()) {
            if (gpsLabelText != null && gpsLogButton != null) {
                gpsLabelText.setText(Constants.TEXT_STOP_GPS_LOGGING);
                gpsLogButton.setImageResource(R.drawable.gps_on);
            }
        } else {
            if (gpsLabelText != null && gpsLogButton != null) {
                gpsLabelText.setText(Constants.TEXT_START_GPS_LOGGING);
                gpsLogButton.setImageResource(R.drawable.gps);
            }
        }
    }

    /**
     * Adapter to put buttons into a grid.
     * 
     * @author Andrea Antonello (www.hydrologis.com)
     */
    private class ButtonAdapter extends BaseAdapter {
        private Context mContext;

        public ButtonAdapter( Context c ) {
            mContext = c;
        }

        public int getCount() {
            return 4;
        }

        public Object getItem( int position ) {
            return null;
        }

        public long getItemId( int position ) {
            return 0;
        }

        public View getView( int position, View convertView, ViewGroup parent ) {

            if (position == 0) {
                LinearLayout cameraViewLayout = new LinearLayout(mContext);
                cameraViewLayout.setOrientation(LinearLayout.VERTICAL);

                cameraLabelText = new TextView(mContext);
                cameraLabelText.setText(Constants.TEXT_TAKE_PICTURE);
                cameraLabelText.setGravity(Gravity.CENTER_HORIZONTAL);

                cameraButton = new ImageButton(mContext);
                cameraButton.setImageResource(R.drawable.camera);
                LayoutParams cameraParams = new LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.FILL_PARENT);
                cameraButton.setLayoutParams(cameraParams);
                cameraButton.setOnClickListener(new Button.OnClickListener(){
                    public void onClick( View v ) {
                        // GpsLocation loc = deviceManager.getLoc();
                        // if (loc != null) {
                        Intent intent = new Intent(Constants.TAKE_PICTURE);
                        startActivity(intent);
                        // } else {
                        // ApplicationManager.openDialog(R.string.gpslogging_only, mContext);
                        // }
                    }
                });

                cameraViewLayout.addView(cameraButton);
                cameraViewLayout.addView(cameraLabelText);

                return cameraViewLayout;
            } else if (position == 1) {
                LinearLayout notesViewLayout = new LinearLayout(mContext);
                notesViewLayout.setOrientation(LinearLayout.VERTICAL);

                notesLabelText = new TextView(mContext);
                notesLabelText.setText(Constants.TEXT_TAKE_A_GPS_NOTE);
                notesLabelText.setGravity(Gravity.CENTER_HORIZONTAL);

                notesButton = new ImageButton(mContext);
                notesButton.setImageResource(R.drawable.notes);
                LayoutParams notesParams = new LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.FILL_PARENT);
                notesButton.setLayoutParams(notesParams);
                notesButton.setOnClickListener(new Button.OnClickListener(){
                    public void onClick( View v ) {
                        GpsLocation loc = deviceManager.getLoc();
                        if (loc != null) {
                            Intent intent = new Intent(Constants.TAKE_NOTE);
                            startActivity(intent);
                        } else {
                            ApplicationManager.openDialog(R.string.gpslogging_only, mContext);
                        }
                    }
                });

                notesViewLayout.addView(notesButton);
                notesViewLayout.addView(notesLabelText);

                return notesViewLayout;
            } else if (position == 2) {
                LinearLayout gpsViewLayout = new LinearLayout(mContext);
                gpsViewLayout.setOrientation(LinearLayout.VERTICAL);

                gpsLabelText = new TextView(mContext);
                gpsLabelText.setText(Constants.TEXT_START_GPS_LOGGING);
                gpsLabelText.setGravity(Gravity.CENTER_HORIZONTAL);

                gpsLogButton = new ImageButton(mContext);
                gpsLogButton.setImageResource(R.drawable.gps);
                LayoutParams gpsLogParams = new LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.FILL_PARENT);
                gpsLogButton.setLayoutParams(gpsLogParams);
                gpsLogButton.setOnClickListener(new Button.OnClickListener(){
                    public void onClick( View v ) {
                        if (gpsLabelText.getText().equals(Constants.TEXT_START_GPS_LOGGING)) {
                            GpsLocation loc = deviceManager.getLoc();
                            if (loc != null) {
                                deviceManager.doLogGps(true);
                                gpsLabelText.setText(Constants.TEXT_STOP_GPS_LOGGING);
                                gpsLogButton.setImageResource(R.drawable.gps_on);
                            } else {
                                ApplicationManager.openDialog(R.string.gpslogging_only, mContext);
                            }
                        } else {
                            deviceManager.doLogGps(false);
                            gpsLabelText.setText(Constants.TEXT_START_GPS_LOGGING);
                            gpsLogButton.setImageResource(R.drawable.gps);
                        }
                    }
                });

                gpsViewLayout.addView(gpsLogButton);
                gpsViewLayout.addView(gpsLabelText);
                fixGpsButton();

                return gpsViewLayout;
            } else if (position == 3) {
                LinearLayout mapViewLayout = new LinearLayout(mContext);
                mapViewLayout.setOrientation(LinearLayout.VERTICAL);

                mapLabelText = new TextView(mContext);
                mapLabelText.setText(Constants.TEXT_SHOW_POSITION_ON_MAP);
                mapLabelText.setGravity(Gravity.CENTER_HORIZONTAL);

                final ImageButton mapButton = new ImageButton(mContext);
                mapButton.setImageResource(R.drawable.gmap);
                LayoutParams mapParams = new LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.FILL_PARENT);
                mapButton.setLayoutParams(mapParams);
                mapButton.setOnClickListener(new Button.OnClickListener(){
                    public void onClick( View view ) {
                        Intent intent2 = new Intent(Constants.VIEW_IN_OSM);
                        startActivity(intent2);
                    }
                });

                mapViewLayout.addView(mapButton);
                mapViewLayout.addView(mapLabelText);

                return mapViewLayout;

            }

            return null;
        }

    }

    // public static SharedPreferences preferences;
    // public void getPreferences() {
    // if (preferences == null) {
    // preferences = getPreferences(MODE_PRIVATE);
    // }
    // }

    private void checkMapsAndLogsVisibility() throws IOException {
        List<MapItem> maps = DaoMaps.getMaps();
        boolean oneVisible = false;
        for( MapItem item : maps ) {
            if (!oneVisible && item.isVisible()) {
                oneVisible = true;
            }
        }
        DataManager.getInstance().setMapsVisible(oneVisible);

        maps = DaoGpsLog.getGpslogs();
        oneVisible = false;
        for( MapItem item : maps ) {
            if (!oneVisible && item.isVisible()) {
                oneVisible = true;
            }
        }
        DataManager.getInstance().setLogsVisible(oneVisible);
    }
}