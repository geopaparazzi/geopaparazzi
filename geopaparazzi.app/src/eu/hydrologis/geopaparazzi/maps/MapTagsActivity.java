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

import java.sql.Date;

import android.app.Activity;
import android.content.Intent;
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
import eu.hydrologis.geopaparazzi.maps.TagsManager.TagObject;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * Osm tags adding activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapTagsActivity extends Activity {
    private EditText additionalInfoText;
    private float latitude;
    private float longitude;
    private String[] tagNamesArray;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.tags);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            latitude = extras.getFloat(Constants.VIEW_CENTER_LAT);
            longitude = extras.getFloat(Constants.VIEW_CENTER_LON);

        }

        additionalInfoText = (EditText) findViewById(R.id.osm_additionalinfo_id);

        GridView buttonGridView = (GridView) findViewById(R.id.osmgridview);
        try {
            tagNamesArray = TagsManager.getInstance(this).getTagsArrays();
        } catch (Exception e1) {
            tagNamesArray = new String[]{"ERROR IN READING TAGS"};
            Logger.e(this, e1.getLocalizedMessage(), e1);
            e1.printStackTrace();
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.gpslog_row, tagNamesArray){
            public View getView( final int position, View cView, ViewGroup parent ) {

                Button osmButton = new Button(MapTagsActivity.this);
                osmButton.setText(tagNamesArray[position]);
                // osmButton.setImageResource(R.drawable.gps);
                osmButton.setOnClickListener(new Button.OnClickListener(){
                    public void onClick( View v ) {
                        try {
                            Date sqlDate = new Date(System.currentTimeMillis());
                            StringBuilder sB = new StringBuilder(additionalInfoText.getText());
                            String infoString = sB.toString();
                            String name = tagNamesArray[position];
                            TagObject tag = TagsManager.getInstance(MapTagsActivity.this).getTagFromName(name);
                            String finalLongName = tag.longName;
                            if (infoString.length() != 0) {
                                String sep = ":";
                                if (finalLongName.indexOf(":") != -1) {
                                    sep = " ";
                                }
                                finalLongName = finalLongName + sep + infoString;
                            }

                            if (tag.hasForm) {
                                // launch form activity
                                String jsonString = tag.jsonString;

                                Intent formIntent = new Intent(Constants.FORM);
                                formIntent.putExtra(Constants.FORMJSON_KEY, jsonString);
                                formIntent.putExtra(Constants.FORMSHORTNAME_KEY, tag.shortName);
                                formIntent.putExtra(Constants.FORMLONGNAME_KEY, finalLongName);
                                formIntent.putExtra(Constants.VIEW_CENTER_LAT, latitude);
                                formIntent.putExtra(Constants.VIEW_CENTER_LON, longitude);
                                startActivity(formIntent);
                            } else {
                                // insert as it is
                                DaoNotes.addNote(getContext(), longitude, latitude, -1.0, sqlDate, finalLongName, null);
                            }

                        } catch (Exception e) {
                            Logger.e(this, e.getLocalizedMessage(), e);
                            e.printStackTrace();
                            Toast.makeText(MapTagsActivity.this, R.string.notenonsaved, Toast.LENGTH_LONG).show();
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
