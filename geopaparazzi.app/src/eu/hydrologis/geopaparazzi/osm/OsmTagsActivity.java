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
package eu.hydrologis.geopaparazzi.osm;

import java.sql.Date;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * Osm tags adding activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OsmTagsActivity extends Activity {
    private EditText additionalInfoText;
    private float latitude;
    private float longitude;
    private String[] osmBaseTypes;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.osmtags);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            latitude = extras.getFloat(Constants.OSMVIEW_CENTER_LAT);
            longitude = extras.getFloat(Constants.OSMVIEW_CENTER_LON);

        }

        additionalInfoText = (EditText) findViewById(R.id.osm_additionalinfo_id);

        GridView buttonGridView = (GridView) findViewById(R.id.osmgridview);
        osmBaseTypes = OsmTagsManager.getInstance().getOsmTagsArrays();

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.gpslog_row, osmBaseTypes){
            public View getView( final int position, View cView, ViewGroup parent ) {

                Button osmButton = new Button(OsmTagsActivity.this);
                osmButton.setText(osmBaseTypes[position]);
                // osmButton.setImageResource(R.drawable.gps);
                osmButton.setOnClickListener(new Button.OnClickListener(){
                    public void onClick( View v ) {
                        try {
                            Date sqlDate = new Date(System.currentTimeMillis());
                            StringBuilder sB = new StringBuilder(additionalInfoText.getText());
                            String infoString = sB.toString();
                            String tag = osmBaseTypes[position];
                            String finalString = OsmTagsManager.getInstance().getDefinitionFromTag(tag);
                            if (infoString.length() != 0) {
                                String sep = ":";
                                if (finalString.indexOf(":") != -1) {
                                    sep = " ";
                                }
                                finalString = finalString + sep + infoString;
                            }

                            DaoNotes.addNote(longitude, latitude, -1.0, sqlDate, finalString);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(OsmTagsActivity.this, R.string.notenonsaved, Toast.LENGTH_LONG).show();
                        }
                        finish();
                    }
                });

                return osmButton;
            }
        };

        // setListAdapter(arrayAdapter);
        buttonGridView.setAdapter(arrayAdapter);
    }
}
