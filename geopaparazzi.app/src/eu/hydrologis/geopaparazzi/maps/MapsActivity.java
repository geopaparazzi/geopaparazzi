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

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.dashboard.ActionBar;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsActivity extends Activity {
    private static final int GO_TO = 1;
    private static final int MENU_GPSDATA = 2;
    private static final int MENU_MAPDATA = 3;
    private static final int MENU_TOGGLE_MEASURE = 4;
    private static final int MENU_ADDTAGS = 5;
    private MapView mapsView;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.mapsview);

        ApplicationManager applicationManager = ApplicationManager.getInstance(this);

        ActionBar actionBar = ActionBar.getActionBar(this, R.id.maps_action_bar, applicationManager);
        actionBar.setTitle(R.string.app_name, R.id.action_bar_title);
        actionBar.checkLogging();

        // requestWindowFeature(Window.FEATURE_PROGRESS);
        mapsView = (MapView) findViewById(R.id.osmviewid);
        applicationManager.addListener(mapsView);

        // button view
        ImageButton zoomIn = (ImageButton) findViewById(R.id.zoom_in_btn);
        ImageButton centerOnGps = (ImageButton) findViewById(R.id.center_on_gps_btn);
        ImageButton zoomOut = (ImageButton) findViewById(R.id.zoom_out_btn);

        zoomIn.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                mapsView.zoomIn();
            }
        });

        centerOnGps.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                mapsView.centerOnGps();
            }
        });

        zoomOut.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                mapsView.zoomOut();
            }
        });

        mapsView.invalidate();
    }

    // @Override
    // protected void onPause() {
    // osmView.clearCache();
    //
    // super.onPause();
    // }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_ADDTAGS, 1, R.string.mainmenu_addtags).setIcon(android.R.drawable.ic_menu_add);
        menu.add(Menu.NONE, MENU_GPSDATA, 2, R.string.mainmenu_gpsdataselect).setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_MAPDATA, 3, R.string.mainmenu_mapdataselect).setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_TOGGLE_MEASURE, 4, R.string.mainmenu_togglemeasure).setIcon(
                android.R.drawable.ic_menu_sort_by_size);
        menu.add(Menu.NONE, GO_TO, 5, R.string.goto_coordinate).setIcon(android.R.drawable.ic_menu_myplaces);
        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MENU_GPSDATA:
            Intent gpsDatalistIntent = new Intent(Constants.GPSLOG_DATALIST);
            startActivity(gpsDatalistIntent);
            return true;

        case MENU_MAPDATA:
            try {
                List<MapItem> mapsList = DaoMaps.getMaps(this);
                int mapsNum = mapsList.size();
                if (mapsNum < 1) {
                    ApplicationManager.openDialog(R.string.no_maps_in_list, this);
                    return true;
                } else {
                    Intent mapDatalistIntent = new Intent(Constants.MAPSDATALIST);
                    startActivity(mapDatalistIntent);
                    return true;
                }
            } catch (IOException e1) {
                Logger.e(this, e1.getLocalizedMessage(), e1);
                e1.printStackTrace();
                return false;
            }

        case MENU_ADDTAGS:
            Intent osmTagsIntent = new Intent(Constants.TAGS);
            osmTagsIntent.putExtra(Constants.VIEW_CENTER_LAT, mapsView.getCenterLat());
            osmTagsIntent.putExtra(Constants.VIEW_CENTER_LON, mapsView.getCenterLon());
            startActivity(osmTagsIntent);
            return true;

        case GO_TO:
            Intent intent = new Intent(Constants.INSERT_COORD);
            startActivity(intent);
            return true;
        case MENU_TOGGLE_MEASURE:
            mapsView.setMeasureMode(!mapsView.isMeasureMode());

            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

}
