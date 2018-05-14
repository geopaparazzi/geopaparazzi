/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.camera.CameraNoteActivity;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IImagesDbHelper;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.forms.FormDetailFragment;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.plugin.style.StyleHelper;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.core.ResourcesManager;

/**
 * A custom pictures view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GPictureView extends View implements GView {

    public static final String IMAGE_ID_SEPARATOR = ";";
    /**
     * The ids of the pictures.
     */
    private String _value;

    private List<String> addedImages = new ArrayList<String>();

    private LinearLayout imageLayout;

    /**
     * @param context  the context to use.
     * @param attrs    attributes.
     * @param defStyle def style.
     */
    public GPictureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context the context to use.
     * @param attrs   attributes.
     */
    public GPictureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param noteId                the id of the note this image belows to.
     * @param fragmentDetail        the fragment detail  to use.
     * @param attrs                 attributes.
     * @param requestCode           the code for starting the activity with result.
     * @param parentView            parent
     * @param label                 label
     * @param value                 in case of pictures, the value are the ids of the image, semicolonseparated.
     * @param constraintDescription constraints
     */
    public GPictureView(final long noteId, final FormDetailFragment fragmentDetail, AttributeSet attrs, final int requestCode, LinearLayout parentView, String label, String value,
                        String constraintDescription) {
        super(fragmentDetail.getActivity(), attrs);

        _value = value;

        final FragmentActivity activity = fragmentDetail.getActivity();
        LinearLayout textLayout = new LinearLayout(activity);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        parentView.addView(textLayout);

        TextView textView = new TextView(activity);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(label.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(Compat.getColor(activity, R.color.formcolor));
        textLayout.addView(textView);

        final Button button = new Button(activity);
        StyleHelper.styleButton(activity, button);
        button.setText(R.string.take_picture);
        textLayout.addView(button);

        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                double[] gpsLocation = PositionUtilities.getGpsLocationFromPreferences(preferences);

                String imageName = ImageUtilities.getCameraImageName(null);
                Intent cameraIntent = new Intent(activity, CameraNoteActivity.class);
                cameraIntent.putExtra(LibraryConstants.PREFS_KEY_CAMERA_IMAGENAME, imageName);
                cameraIntent.putExtra(LibraryConstants.DATABASE_ID, noteId);
                if (gpsLocation != null) {
                    cameraIntent.putExtra(LibraryConstants.LATITUDE, gpsLocation[1]);
                    cameraIntent.putExtra(LibraryConstants.LONGITUDE, gpsLocation[0]);
                    cameraIntent.putExtra(LibraryConstants.ELEVATION, gpsLocation[2]);
                }
                fragmentDetail.startActivityForResult(cameraIntent, requestCode);
            }
        });

        ScrollView scrollView = new ScrollView(activity);
        ScrollView.LayoutParams scrollLayoutParams = new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        scrollView.setLayoutParams(scrollLayoutParams);
        parentView.addView(scrollView);

        imageLayout = new LinearLayout(activity);
        LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        imageLayoutParams.setMargins(10, 10, 10, 10);
        imageLayout.setLayoutParams(imageLayoutParams);
        imageLayout.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(imageLayout);

        try {
            refresh(activity);
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    public void refresh(final Context context) throws Exception {
        log("Entering refresh....");

        if (_value != null && _value.length() > 0) {
            String[] imageSplit = _value.split(IMAGE_ID_SEPARATOR);
            log("Handling images: " + _value);

            final IImagesDbHelper imagesDbHelper = DefaultHelperClasses.getDefaulfImageHelper();

            for (String imageId : imageSplit) {
                log("img: " + imageId);

                if (imageId.length() == 0) {
                    continue;
                }
                try {
                    Long.parseLong(imageId);
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                    continue;
                }
                if (addedImages.contains(imageId.trim())) {
                    continue;
                }
                final long imageIdLong = Long.parseLong(imageId);

                byte[] imageThumbnail = imagesDbHelper.getImageThumbnail(imageIdLong);
                Bitmap thumbnail = ImageUtilities.getImageFromImageData(imageThumbnail);

                ImageView imageView = new ImageView(context);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(150, 150);
                lp.setMargins(10, 10, 10, 10);
                imageView.setLayoutParams(lp);
                imageView.setPadding(5, 5, 5, 5);
                imageView.setImageBitmap(thumbnail);
                imageView.setBackground(Compat.getDrawable(context, R.drawable.border_black_1px));
                imageView.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        /*
                         * open in markers to edit it
                         */
                        // SketchUtilities.launchOnImage(context, image);
                        try {
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            Image image = imagesDbHelper.getImage(imageIdLong);
                            File tempDir = ResourcesManager.getInstance(context).getTempDir();
                            String ext = ".jpg";
                            if (image.getName().endsWith(".png"))
                                ext = ".png";
                            File imageFile = new File(tempDir, ImageUtilities.getTempImageName(ext));
                            byte[] imageData = imagesDbHelper.getImageData(image.getId());
                            ImageUtilities.writeImageDataToFile(imageData, imageFile.getAbsolutePath());

                            Uri imageUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".library.fileprovider", imageFile);
                            intent.setDataAndType(imageUri, "image/*"); //$NON-NLS-1$
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            context.startActivity(intent);
                        } catch (Exception e) {
                            GPLog.error(this, null, e);
                        }
                    }
                });
                log("Creating thumb and adding it: " + imageId);
                imageLayout.addView(imageView);
                imageLayout.invalidate();
                addedImages.add(imageId);
            }

            if (addedImages.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (String imagePath : addedImages) {
                    sb.append(IMAGE_ID_SEPARATOR).append(imagePath);
                }
                _value = sb.substring(1);

                log("New img ids: " + _value);

            }
            log("Exiting refresh....");

        }
    }

    private void log(String msg) {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, null, null, msg);
    }

    public String getValue() {
        return _value;
    }

    @Override
    public void setOnActivityResult(Intent data) {

        boolean imageTaken = data.getBooleanExtra(LibraryConstants.OBJECT_EXISTS, false);
        if (imageTaken) {
            long imageId = data.getLongExtra(LibraryConstants.DATABASE_ID, -1);
            if (imageId == -1) {
                return;
            }
            _value = _value + IMAGE_ID_SEPARATOR + imageId;
            try {
                refresh(getContext());
            } catch (Exception e) {
                GPLog.error(this, null, e);
            }
        }
    }
}

