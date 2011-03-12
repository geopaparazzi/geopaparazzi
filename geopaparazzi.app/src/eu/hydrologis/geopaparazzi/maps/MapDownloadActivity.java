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
package eu.hydrologis.geopaparazzi.maps;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * The map download activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapDownloadActivity extends Activity {
    private ProgressDialog mapTiesCountProgressDialog;
    private int selectedZoom;

    private Handler maptilesCountHandler = new Handler(){
        public void handleMessage( android.os.Message msg ) {
            mapTiesCountProgressDialog.dismiss();
        };
    };

    @SuppressWarnings("nls")
    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.mapsdownload);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final float[] nsewArray = extras.getFloatArray(Constants.NSEW_COORDS);

            final TextView tileNumView = (TextView) findViewById(R.id.tiles_number);

            final Spinner zoomLevelView = (Spinner) findViewById(R.id.zoom_level_download_spinner);

            ArrayAdapter< ? > zoomLevelSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.array_zoomlevels,
                    android.R.layout.simple_spinner_item);
            zoomLevelSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            zoomLevelView.setAdapter(zoomLevelSpinnerAdapter);
            zoomLevelView.setOnItemSelectedListener(new OnItemSelectedListener(){
                public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {

                    Object selectedItem = zoomLevelView.getSelectedItem();
                    String item = selectedItem.toString();
                    if (item.equals("-")) {
                        tileNumView.setText("---");
                        return;
                    }
                    selectedZoom = Integer.parseInt(item);

                    countMapTiles(nsewArray, tileNumView);

                }

                public void onNothingSelected( AdapterView< ? > arg0 ) {
                }
            });

            // Button okButton = (Button) findViewById(R.id.measureOkButton);
            // okButton.setOnClickListener(new Button.OnClickListener(){
            // public void onClick( View v ) {
            // finish();
            // }
            // });
        } else {
            Toast.makeText(this, "An error occurred while retrieving the boundary info.", Toast.LENGTH_LONG).show();
        }

        // File osmCacheDir = applicationManager.getOsmCacheDir();
        // boolean internetIsOn = applicationManager.isInternetOn();
    }

    private void countMapTiles( final float[] nsewArray, final TextView tileNumView ) {
        mapTiesCountProgressDialog = ProgressDialog.show(MapDownloadActivity.this, "Counting tiles...", "", true, true);
        new Thread(){
            public void run() {
                final int tilesNumber = TileCache.fetchTilesNumber(nsewArray[3], nsewArray[1], nsewArray[2], nsewArray[0],
                        selectedZoom);
                MapDownloadActivity.this.runOnUiThread(new Runnable(){
                    public void run() {
                        tileNumView.setText(String.valueOf(tilesNumber));
                        maptilesCountHandler.sendEmptyMessage(0);
                    }
                });
            }
        }.start();
    }

}
