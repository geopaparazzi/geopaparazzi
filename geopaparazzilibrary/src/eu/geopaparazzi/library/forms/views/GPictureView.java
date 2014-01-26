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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.camera.CameraActivity;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.markers.MarkersUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.TimeUtilities;

/**
 * A custom pictures view.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GPictureView extends View implements GView {

    private String _value;

    private List<String> addedImages = new ArrayList<String>();

    private LinearLayout imageLayout;
    private File lastImageFile;

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param defStyle def style.
     */
    public GPictureView( Context context, AttributeSet attrs, int defStyle ) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     */
    public GPictureView( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    /**
     * @param context   the context to use.
     * @param attrs attributes.
     * @param parentView parent
     * @param key key
     * @param value value
     * @param constraintDescription constraints
     */
    public GPictureView( final Context context, AttributeSet attrs, LinearLayout parentView, String key, String value,
            String constraintDescription ) {
        super(context, attrs);

        _value = value;

        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        parentView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(context.getResources().getColor(R.color.formcolor));
        textLayout.addView(textView);

        final Button button = new Button(context);
        button.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        button.setPadding(15, 5, 15, 5);
        button.setText(R.string.take_picture);
        textLayout.addView(button);

        button.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                double[] gpsLocation = PositionUtilities.getGpsLocationFromPreferences(preferences);

                Date currentDate = new Date();
                String currentDatestring = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(currentDate);
                File mediaDir = null;
                try {
                    mediaDir = ResourcesManager.getInstance(context).getMediaDir();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                lastImageFile = new File(mediaDir, "IMG_" + currentDatestring + ".jpg");

                Intent cameraIntent = new Intent(context, CameraActivity.class);
                cameraIntent.putExtra(LibraryConstants.PREFS_KEY_CAMERA_IMAGENAME, lastImageFile.getName());
                if (gpsLocation != null) {
                    cameraIntent.putExtra(LibraryConstants.LATITUDE, gpsLocation[1]);
                    cameraIntent.putExtra(LibraryConstants.LONGITUDE, gpsLocation[0]);
                    cameraIntent.putExtra(LibraryConstants.ELEVATION, gpsLocation[2]);
                }
                context.startActivity(cameraIntent);
            }
        });

        ScrollView scrollView = new ScrollView(context);
        ScrollView.LayoutParams scrollLayoutParams = new ScrollView.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        scrollView.setLayoutParams(scrollLayoutParams);
        parentView.addView(scrollView);

        imageLayout = new LinearLayout(context);
        LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        imageLayout.setLayoutParams(imageLayoutParams);
        imageLayout.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(imageLayout);

        ViewTreeObserver observer = imageLayout.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
            public boolean onPreDraw() {
                if (lastImageFile != null && lastImageFile.exists()) {
                    String imagePath = lastImageFile.getAbsolutePath();
                    _value = _value + ";" + imagePath;

                    try {
                        // THIS IS PLAIN UGLY
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    refresh(context);
                    lastImageFile = null;
                }
                return true;
            }
        });

        refresh(context);
    }

    public void refresh( final Context context ) {
        log("Entering refresh....");

        if (_value != null && _value.length() > 0) {
            String[] imageSplit = _value.split(";");
            log("Handling images: " + _value);

            for( String imageAbsolutePath : imageSplit ) {
                log("img: " + imageAbsolutePath);

                if (imageAbsolutePath.length() == 0) {
                    continue;
                }
                if (addedImages.contains(imageAbsolutePath.trim())) {
                    continue;
                }

                final File image = new File(imageAbsolutePath);
                if (!image.exists()) {
                    log("Img doesn't exist on disk....");
                    continue;
                }

                Bitmap thumbnail = FileUtilities.readScaledBitmap(image, 100);
                ImageView imageView = new ImageView(context);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(102, 102));
                imageView.setPadding(5, 5, 5, 5);
                imageView.setImageBitmap(thumbnail);
                imageView.setBackgroundDrawable(getResources().getDrawable(R.drawable.border_black_1px));
                imageView.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        /*
                         * open in markers to edit it
                         */
                        MarkersUtilities.launchOnImage(context, image);
                        // Intent intent = new Intent();
                        // intent.setAction(android.content.Intent.ACTION_VIEW);
                        //                        intent.setDataAndType(Uri.fromFile(image), "image/*"); //$NON-NLS-1$
                        // context.startActivity(intent);
                    }
                });
                log("Creating thumb and adding it: " + imageAbsolutePath);
                imageLayout.addView(imageView);
                imageLayout.invalidate();
                addedImages.add(imageAbsolutePath);
            }

            if (addedImages.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for( String imagePath : addedImages ) {
                    sb.append(";").append(imagePath);
                }
                _value = sb.substring(1);

                log("New img paths: " + _value);

            }
            log("Exiting refresh....");

        }
    }

    private void log( String msg ) {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, null, null, msg);
    }

    public String getValue() {
        return _value;
    }

    @Override
    public void setOnActivityResult( Intent data ) {
        // ignore
    }

}