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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * The measure info view.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MeasureInfoActivity extends Activity {
    @SuppressWarnings("nls")
    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.measureinfo);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            float[] xArray = extras.getFloatArray(Constants.MEASURECOORDSX);
            float[] yArray = extras.getFloatArray(Constants.MEASURECOORDSY);
            float distance = extras.getFloat(Constants.MEASUREDIST);

            EditText distanceView = (EditText) findViewById(R.id.measuredistancetext);
            distanceView.setText(String.valueOf(distance));
            
            EditText firstLonView = (EditText) findViewById(R.id.longitudetext_first);
            firstLonView.setText(String.valueOf(xArray[0]));
            EditText firstLatView = (EditText) findViewById(R.id.latitudetext_first);
            firstLatView.setText(String.valueOf(yArray[0]));
            
            EditText lastLonView = (EditText) findViewById(R.id.longitudetext_last);
            lastLonView.setText(String.valueOf(xArray[xArray.length - 1]));
            EditText lastLatView = (EditText) findViewById(R.id.latitudetext_last);
            lastLatView.setText(String.valueOf(yArray[yArray.length - 1]));

            Button okButton = (Button) findViewById(R.id.measureOkButton);
            okButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    finish();
                }
            });
        } else {
            Toast.makeText(this, "An error occurred while retrieving the measure info.", Toast.LENGTH_LONG).show();
        }

    }

}
