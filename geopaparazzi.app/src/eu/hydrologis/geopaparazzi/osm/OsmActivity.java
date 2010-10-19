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
package eu.hydrologis.geopaparazzi.osm;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OsmActivity extends Activity {
    private static final int CENTER_ON_GPS = 1;
    private static final int GO_TO = 2;
    private static final int MENU_GPSDATA = 3;
    private static final int MENU_MAPDATA = 4;
    private static final int MENU_TOGGLE_MEASURE = 5;
    private static final int MENU_GPXCONVERT = 6;
    private static final int MENU_ADDTAGS = 7;
    private static final int MENU_REMOVE_LAST_NOTE = 8;

    private OsmView osmView;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        // main frame
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(-9999);
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        // map view
        osmView = new OsmView(this);
        ApplicationManager applicationManager = ApplicationManager.getInstance(this);
        applicationManager.addListener(osmView);
        applicationManager.setOsmView(osmView);

        frameLayout.addView(osmView);

        // button view
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        buttonsLayout.setPadding(5, 5, 5, 0);
        buttonsLayout.setOrientation(LinearLayout.VERTICAL);
        buttonsLayout.setGravity(Gravity.BOTTOM);

        TransparentPanel transparentPanel = new TransparentPanel(this);
        transparentPanel.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        transparentPanel.setPadding(0, 0, 0, 0);
        buttonsLayout.addView(transparentPanel);

        Paint buttonBack = new Paint();
        buttonBack.setARGB(0, 255, 255, 255);
        ImageButton zoomoutButton = new ImageButton(this);
        zoomoutButton.setImageResource(R.drawable.zoomout);
        zoomoutButton.setBackgroundColor(buttonBack.getColor());
        zoomoutButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                osmView.zoomOut();
            }
        });
        android.widget.RelativeLayout.LayoutParams paramsOut = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        paramsOut.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        zoomoutButton.setLayoutParams(paramsOut);

        transparentPanel.addView(zoomoutButton);

        ImageButton zoominButton = new ImageButton(this);
        zoominButton.setImageResource(R.drawable.zoomin);
        zoominButton.setBackgroundColor(buttonBack.getColor());
        zoominButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                osmView.zoomIn();
            }
        });
        android.widget.RelativeLayout.LayoutParams paramsIn = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        paramsIn.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        zoominButton.setLayoutParams(paramsIn);

        transparentPanel.addView(zoominButton);

        frameLayout.addView(buttonsLayout);

        setContentView(frameLayout);

        osmView.invalidate();
    }

    // @Override
    // protected void onPause() {
    // osmView.clearCache();
    //
    // super.onPause();
    // }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_GPSDATA, 1, R.string.mainmenu_gpsdataselect).setIcon(R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_MAPDATA, 2, R.string.mainmenu_mapdataselect).setIcon(R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_ADDTAGS, 3, R.string.mainmenu_addtags).setIcon(R.drawable.ic_menu_add);
        menu.add(Menu.NONE, CENTER_ON_GPS, 4, R.string.centerongps).setIcon(R.drawable.menu_mylocation);
        menu.add(Menu.NONE, MENU_TOGGLE_MEASURE, 5, R.string.mainmenu_togglemeasure).setIcon(R.drawable.ic_menu_measure);
        menu.add(Menu.NONE, MENU_REMOVE_LAST_NOTE, 6, R.string.mainmenu_removelastnote).setIcon(R.drawable.ic_menu_remove);
        menu.add(Menu.NONE, MENU_GPXCONVERT, 7, R.string.mainmenu_gpxconvert).setIcon(R.drawable.gpxconvert);
        menu.add(Menu.NONE, GO_TO, 8, R.string.goto_coordinate).setIcon(R.drawable.menu_goto);
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
                e1.printStackTrace();
                return false;
            }

        case MENU_ADDTAGS:
            Intent osmTagsIntent = new Intent(Constants.OSMTAGS);
            osmTagsIntent.putExtra(Constants.OSMVIEW_CENTER_LAT, osmView.getCenterLat());
            osmTagsIntent.putExtra(Constants.OSMVIEW_CENTER_LON, osmView.getCenterLon());
            startActivity(osmTagsIntent);
            return true;

        case MENU_REMOVE_LAST_NOTE:
            try {
                DaoNotes.deleteLastInsertedNote(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            osmView.invalidate();
            return true;

        case CENTER_ON_GPS:
            osmView.centerOnGps();

            return true;
        case GO_TO:
            Intent intent = new Intent(Constants.INSERT_COORD);
            startActivity(intent);
            return true;
        case MENU_TOGGLE_MEASURE:
            osmView.setMeasureMode(!osmView.isMeasureMode());

            return true;
        case MENU_GPXCONVERT:
            Intent browseIntent = new Intent(Constants.DIRECTORYBROWSER);
            browseIntent.putExtra(Constants.INTENT_ID, Constants.GPXIMPORT);
            browseIntent.putExtra(Constants.EXTENTION, ".gpx"); //$NON-NLS-1$
            startActivity(browseIntent);
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

}
