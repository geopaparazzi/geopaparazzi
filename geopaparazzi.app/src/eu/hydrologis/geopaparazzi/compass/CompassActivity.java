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
package eu.hydrologis.geopaparazzi.compass;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;

/**
 * Compass activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CompassActivity extends Activity {
    private CompassView compassView;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.compass_view);

        /*
         * the compass view
         */
        ApplicationManager applicationManager = ApplicationManager.getInstance(this);
        LinearLayout uppercol1View = (LinearLayout) findViewById(R.id.uppercol1);
        TextView compassInfoView = (TextView) findViewById(R.id.compassInfoView);
        GpsLocation tmpLoc = applicationManager.getLoc();
        compassView = new CompassView(this, compassInfoView, applicationManager, tmpLoc);
        LinearLayout.LayoutParams tmpParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        uppercol1View.addView(compassView, tmpParams);
    }

    protected void onStart() {
        ApplicationManager applicationManager = ApplicationManager.getInstance(this);
        applicationManager.removeCompassListener();
        applicationManager.addListener(compassView);
        super.onStart();
    }

    protected void onStop() {
        ApplicationManager applicationManager = ApplicationManager.getInstance(this);
        applicationManager.removeCompassListener();
        super.onStop();
    }

}
