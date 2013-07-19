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
package eu.geopaparazzi.library.forms.views;

import static eu.geopaparazzi.library.forms.FormUtilities.COLON;
import static eu.geopaparazzi.library.forms.FormUtilities.UNDERSCORE;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;

/**
 * A custom map view.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GMapView extends View implements GView {

    private File image;

    public GMapView( Context context, AttributeSet attrs, int defStyle ) {
        super(context, attrs, defStyle);
    }

    public GMapView( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    public GMapView( final Context context, AttributeSet attrs, LinearLayout parentView, String key, String value,
            String constraintDescription ) {
        super(context, attrs);

        LinearLayout mainLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        mainLayout.setLayoutParams(layoutParams);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        parentView.addView(mainLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(context.getResources().getColor(R.color.formcolor));
        mainLayout.addView(textView);

        image = new File(value);
        if (!image.exists()) {
            // look also in media folder for relative path name
            File mediaDir = null;
            try {
                mediaDir = ResourcesManager.getInstance(context).getMediaDir();
            } catch (Exception e) {
                e.printStackTrace();
            }
            File parentFolder = mediaDir.getParentFile();
            image = new File(parentFolder, value);
        }

        if (image.exists()) {
            Bitmap thumbnail = FileUtilities.readScaledBitmap(image, 200);
            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            imageView.setPadding(5, 5, 5, 5);
            imageView.setImageBitmap(thumbnail);
            imageView.setOnClickListener(new View.OnClickListener(){
                public void onClick( View v ) {
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(image), "image/*"); //$NON-NLS-1$
                    context.startActivity(intent);
                }
            });
            mainLayout.addView(imageView);
        }
    }

    public String getValue() {
        return image.getAbsolutePath();
    }

    @Override
    public void setOnActivityResult( Intent data ) {
    }

}