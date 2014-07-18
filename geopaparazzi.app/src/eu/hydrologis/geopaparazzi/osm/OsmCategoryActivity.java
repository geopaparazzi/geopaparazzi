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

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * Osm category activity showing the available tags.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OsmCategoryActivity extends Activity {

    private String category;

    private String[] itemsForCategory = new String[0];
    
    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.osmcategorytags);

        Bundle extras = getIntent().getExtras();
        category = extras.getString(Constants.OSM_CATEGORY_KEY);

        try {
            itemsForCategory = OsmTagsManager.getInstance().getItemsForCategory(this, category);
        } catch (Exception e) {
            e.printStackTrace();
        }

        GridView buttonGridView = (GridView) findViewById(R.id.osmtagsgridview);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.osm_button_layout, itemsForCategory){
            public View getView( final int position, View cView, ViewGroup parent ) {

                View inflate = LayoutInflater.from(OsmCategoryActivity.this).inflate(R.layout.osm_button_layout, parent, false);

                final TextView text = (TextView) inflate.findViewById(R.id.osm_item_text);
                final String tagName = itemsForCategory[position];
                text.setText(tagName.replaceAll("_", " ")); //$NON-NLS-1$ //$NON-NLS-2$

                Drawable icon = OsmImageCache.getInstance(OsmCategoryActivity.this).getImageForTag(tagName, category);
                final ImageButton osmButton = (ImageButton) inflate.findViewById(R.id.osm_item_image);
                osmButton.setImageDrawable(icon);
                osmButton.setOnClickListener(new Button.OnClickListener(){
                    public void onClick( View v ) {
                        Intent osmCategoryIntent = new Intent(OsmCategoryActivity.this, OsmFormActivity.class);
                        osmCategoryIntent.putExtra(Constants.OSM_CATEGORY_KEY, category);
                        osmCategoryIntent.putExtra(Constants.OSM_TAG_KEY, tagName);
                        startActivity(osmCategoryIntent);
                        // finish();
                    }
                });

                return inflate;
            }
        };

        // setListAdapter(arrayAdapter);
        buttonGridView.setAdapter(arrayAdapter);
    }
}
