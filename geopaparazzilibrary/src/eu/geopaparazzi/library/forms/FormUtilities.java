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
package eu.geopaparazzi.library.forms;

import java.io.File;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.camera.CameraActivity;
import eu.geopaparazzi.library.forms.constraints.Constraints;
import eu.geopaparazzi.library.forms.constraints.MandatoryConstraint;
import eu.geopaparazzi.library.forms.constraints.RangeConstraint;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.MultipleChoiceDialog;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;

/**
 * Utilities methods for form stuff.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 2.6
 */
@SuppressWarnings("nls")
public class FormUtilities {

    public static final String COLON = ":";
    public static final String UNDERSCORE = "_";

    /**
     * Type for a {@link TextView}.
     */
    public static final String TYPE_LABEL = "label";

    /**
     * Type for a {@link TextView} with line below.
     */
    public static final String TYPE_LABELWITHLINE = "lablewithline";

    /**
     * Type for a {@link EditText} containing generic text.
     */
    public static final String TYPE_STRING = "string";

    /**
     * Type for a {@link EditText} containing generic numbers.
     */
    public static final String TYPE_DOUBLE = "double";

    /**
     * Type for a {@link EditText} containing date.
     */
    public static final String TYPE_DATE = "date";

    /**
     * Type for a {@link CheckBox}.
     */
    public static final String TYPE_BOOLEAN = "boolean";

    /**
     * Type for a {@link Spinner}.
     */
    public static final String TYPE_STRINGCOMBO = "stringcombo";

    /**
     * Type for a {@link MuSpinner}.
     */
    public static final String TYPE_STRINGMULTIPLECHOICE = "multistringcombo";

    /**
     * Type for a hidden widget, which just needs to be kept as it is but not displayed.
     */
    public static final String TYPE_HIDDEN = "hidden";

    /**
     * Type for latitude, which can be substituted by the engine if necessary.
     */
    public static final String TYPE_LATITUDE = "LATITUDE";

    /**
     * Type for longitude, which can be substituted by the engine if necessary.
     */
    public static final String TYPE_LONGITUDE = "LONGITUDE";

    /**
     * Type for a hidden item, the value of which needs to get the name of the element.
     * 
     * <p>This is needed in case of abstraction of forms.</p>
     */
    public static final String TYPE_PRIMARYKEY = "primary_key";

    /**
     * Type for pictures element.
     */
    public static final String TYPE_PICTURES = "pictures";

    /**
     * Type for barcode element.
     * 
     * <b>Not in use yet.</b>
     */
    public static final String TYPE_BARCODE = "barcode";

    /**
     * A constraint that defines the item as mandatory.
     */
    public static final String CONSTRAINT_MANDATORY = "mandatory";

    /**
     * A constraint that defines a range for the value.
     */
    public static final String CONSTRAINT_RANGE = "range";

    public static final String ATTR_SECTIONNAME = "sectionname";
    public static final String ATTR_SECTIONOBJECTSTR = "sectionobjectstr";
    public static final String ATTR_FORMS = "forms";
    public static final String ATTR_FORMNAME = "formname";

    public static final String TAG_LONGNAME = "longname";
    public static final String TAG_SHORTNAME = "shortname";
    public static final String TAG_FORMS = "forms";
    public static final String TAG_FORMITEMS = "formitems";
    public static final String TAG_KEY = "key";
    public static final String TAG_VALUE = "value";
    public static final String TAG_VALUES = "values";
    public static final String TAG_ITEMS = "items";
    public static final String TAG_ITEM = "item";
    public static final String TAG_TYPE = "type";
    public static final String TAG_SIZE = "size";

    /**
     * Adds a {@link TextView} to the supplied mainView.
     * 
     * @param context the context.
     * @param mainView the main view to which to add the new widget to.
     * @param key the key identifying the widget.
     * @param value the value to put in the widget.
     * @param type the text type:
     *          <ul>
     *             <li>0: text</li>
     *             <li>1: numeric</li>
     *             <li>2: phone</li>
     *             <li>3: date</li>
     *          </ul>
     * @param constraintDescription 
     * @return the added view.
     */
    public static View addEditText( Context context, LinearLayout mainView, String key, String value, int type,
            String constraintDescription ) {
        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        // textLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.formitem_background));
        mainView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(context.getResources().getColor(R.color.formcolor));

        textLayout.addView(textView);

        EditText editView = new EditText(context);
        editView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        editView.setPadding(15, 5, 15, 5);
        editView.setText(value);

        switch( type ) {
        case 1:
            editView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            break;
        case 2:
            editView.setInputType(InputType.TYPE_CLASS_PHONE);
            break;
        case 3:
            editView.setInputType(InputType.TYPE_CLASS_DATETIME);
            break;
        default:
            break;
        }

        textLayout.addView(editView);
        return editView;
    }

    /**
     * Adds a {@link CheckBox} to the supplied mainView.
     * 
     * @param context the context.
     * @param mainView the main view to which to add the new widget to.
     * @param key the key identifying the widget.
     * @param value the value to put in the widget.
     * @param constraintDescription 
     * @return the added view.
     */
    public static View addBooleanView( Context context, LinearLayout mainView, String key, String value,
            String constraintDescription ) {
        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        mainView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(context.getResources().getColor(R.color.formcolor));

        textLayout.addView(textView);

        CheckBox checkbox = new CheckBox(context);
        checkbox.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        checkbox.setPadding(15, 5, 15, 5);

        if (value != null) {
            if (value.trim().toLowerCase().equals("true")) { //$NON-NLS-1$
                checkbox.setSelected(true);
            } else {
                checkbox.setSelected(false);
            }
        }

        textLayout.addView(checkbox);

        return checkbox;
    }

    /**
     * Adds a {@link Spinner} to the supplied mainView.
     * 
     * @param context the context.
     * @param mainView the main view to which to add the new widget to.
     * @param key the key identifying the widget.
     * @param value the value to put in the widget.
     * @param itemsArray the items to put in the spinner.
     * @param constraintDescription 
     * @return the added view.
     */
    public static View addComboView( Context context, LinearLayout mainView, String key, String value, String[] itemsArray,
            String constraintDescription ) {
        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        mainView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(context.getResources().getColor(R.color.formcolor));
        textLayout.addView(textView);

        Spinner spinner = new Spinner(context);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        spinner.setPadding(15, 5, 15, 5);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, itemsArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (value != null) {
            for( int i = 0; i < itemsArray.length; i++ ) {
                if (itemsArray[i].equals(value.trim())) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }

        textLayout.addView(spinner);
        return spinner;
    }

    /**
     * Adds a {@link MultipleChoiceDialog} to the supplied mainView.
     * 
     * @param context the context.
     * @param mainView the main view to which to add the new widget to.
     * @param key the key identifying the widget.
     * @param value the value to put in the widget.
     * @param itemsArray the items to put in the spinner.
     * @param constraintDescription 
     * @return the added view.
     */
    public static View addMultiSelectionView( final Context context, LinearLayout mainView, String key, String value,
            final String[] itemsArray, String constraintDescription ) {
        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        mainView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(context.getResources().getColor(R.color.formcolor));
        textLayout.addView(textView);

        final Button button = new Button(context);
        button.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        button.setPadding(15, 5, 15, 5);

        if (value == null || value.length() == 0) {
            button.setText("...");
        } else {
            button.setText(value);
        }
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                MultipleChoiceDialog dialog = new MultipleChoiceDialog();
                dialog.open(context, button, itemsArray);
            }
        });

        textLayout.addView(button);
        return button;
    }

    public static View addPictureView( final Context context, LinearLayout mainView, String key, String value,
            String constraintDescription ) {
        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        mainView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(key.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
        textView.setTextColor(context.getResources().getColor(R.color.formcolor));
        textLayout.addView(textView);

        final Button button = new Button(context);
        button.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        button.setPadding(15, 5, 15, 5);
        button.setText("Take picture");
        textLayout.addView(button);

        final EditText imagesText = new EditText(context);
        imagesText.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        imagesText.setPadding(2, 2, 2, 2);
        imagesText.setText(value);
        imagesText.setTextColor(context.getResources().getColor(R.color.black));
        imagesText.setKeyListener(null);
        textLayout.addView(imagesText);

        button.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                double[] gpsLocation = PositionUtilities.getGpsLocationFromPreferences(preferences);

                Date currentDate = new Date();
                String currentDatestring = LibraryConstants.TIMESTAMPFORMATTER.format(currentDate);
                File mediaDir = ResourcesManager.getInstance(context).getMediaDir();
                File imageFile = new File(mediaDir, "IMG_" + currentDatestring + ".jpg");
                String relativeImagePath = mediaDir.getName() + File.separator + imageFile.getName();
                String text = imagesText.getText().toString();
                StringBuilder sb = new StringBuilder();
                sb.append(text);
                if (text.length() != 0) {
                    sb.append(";");
                }
                sb.append(relativeImagePath);
                imagesText.setText(sb.toString());

                Intent cameraIntent = new Intent(context, CameraActivity.class);
                cameraIntent.putExtra(LibraryConstants.PREFS_KEY_CAMERA_IMAGENAME, imageFile.getName());
                if (gpsLocation != null) {
                    cameraIntent.putExtra(LibraryConstants.LATITUDE, gpsLocation[1]);
                    cameraIntent.putExtra(LibraryConstants.LONGITUDE, gpsLocation[0]);
                    cameraIntent.putExtra(LibraryConstants.ELEVATION, gpsLocation[2]);
                }
                context.startActivity(cameraIntent);
            }
        });

        if (imagesText.getText().toString().length() > 0) {
            ScrollView scrollView = new ScrollView(context);
            ScrollView.LayoutParams scrollLayoutParams = new ScrollView.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT);
            scrollView.setLayoutParams(scrollLayoutParams);
            mainView.addView(scrollView);

            LinearLayout imageLayout = new LinearLayout(context);
            LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT);
            imageLayout.setLayoutParams(imageLayoutParams);
            imageLayout.setOrientation(LinearLayout.HORIZONTAL);
            scrollView.addView(imageLayout);

            String text = imagesText.getText().toString();
            String[] imageSplit = text.split(";");
            File mediaDir = ResourcesManager.getInstance(context).getMediaDir();
            File parentFolder = mediaDir.getParentFile();
            for( String imageRelativePath : imageSplit ) {
                File image = new File(parentFolder, imageRelativePath);

                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(image.getAbsolutePath(), bounds);
                int width = bounds.outWidth;
                // int height = bounds.outHeight;
                int newWidth = 100;
                // int newHeight = (int) ((double) newWidth * height / width);

                float sampleSizeF = (float) width / (float) newWidth;
                int sampleSize = Math.round(sampleSizeF);
                BitmapFactory.Options resample = new BitmapFactory.Options();
                resample.inSampleSize = sampleSize;

                Bitmap thumbnail = BitmapFactory.decodeFile(image.getAbsolutePath(), resample);
                // Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, newWidth, newHeight);
                ImageView imageView = new ImageView(context);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                imageView.setPadding(5, 5, 5, 5);
                imageView.setImageBitmap(thumbnail);
                imageLayout.addView(imageView);
                // thumbnail.recycle();
            }

        }

        return imagesText;
    }
    /**
     * Check an {@link JSONObject object} for constraints and collect them.
     * 
     * @param jsonObject the object to check.
     * @param constraints the {@link Constraints} object to use or <code>null</code>.
     * @return the original {@link Constraints} object or a new created.
     * @throws Exception
     */
    public static Constraints handleConstraints( JSONObject jsonObject, Constraints constraints ) throws Exception {
        if (constraints == null)
            constraints = new Constraints();
        if (jsonObject.has(CONSTRAINT_MANDATORY)) {
            String mandatory = jsonObject.getString(CONSTRAINT_MANDATORY).trim();
            if (mandatory.trim().equals("yes")) {
                constraints.addConstraint(new MandatoryConstraint());
            }
        }
        if (jsonObject.has(CONSTRAINT_RANGE)) {
            String range = jsonObject.getString(CONSTRAINT_RANGE).trim();
            String[] rangeSplit = range.split(",");
            if (rangeSplit.length == 2) {
                boolean lowIncluded = rangeSplit[0].startsWith("[") ? true : false;
                String lowStr = rangeSplit[0].substring(1);
                Double low = Utilities.adapt(lowStr, Double.class);
                boolean highIncluded = rangeSplit[1].endsWith("]") ? true : false;
                String highStr = rangeSplit[1].substring(0, rangeSplit[1].length() - 1);
                Double high = Utilities.adapt(highStr, Double.class);
                constraints.addConstraint(new RangeConstraint(low, lowIncluded, high, highIncluded));
            }
        }
        return constraints;
    }

    /**
     * Updates a form items array with the given kay/value pair.
     * 
     * @param formItemsArray the array to update.
     * @param key the key of the item to update.
     * @param value the new value to use.
     * @throws JSONException
     */
    public static void update( JSONArray formItemsArray, String key, String value ) throws JSONException {
        int length = formItemsArray.length();
        for( int i = 0; i < length; i++ ) {
            JSONObject itemObject = formItemsArray.getJSONObject(i);
            String objKey = itemObject.getString(TAG_KEY).trim();
            if (objKey.equals(key)) {
                itemObject.put(TAG_VALUE, value);
            }
        }
    }

    /**
     * Update those fields that do not generate widgets.
     * 
     * @param formItemsArray the items array.
     * @param latitude the lat value.
     * @param longitude the long value.
     * @param category the category of the tag.
     * @param tagName the tag name.
     * @throws JSONException 
     */
    public static void updateExtras( JSONArray formItemsArray, double latitude, double longitude, String category, String tagName )
            throws JSONException {
        int length = formItemsArray.length();
        for( int i = 0; i < length; i++ ) {
            JSONObject itemObject = formItemsArray.getJSONObject(i);
            String objKey = itemObject.getString(TAG_KEY).trim();
            String objType = itemObject.getString(TAG_TYPE).trim();
            if (objKey.equals(TYPE_LATITUDE)) {
                itemObject.put(TAG_VALUE, latitude);
            } else if (objKey.equals(TYPE_LONGITUDE)) {
                itemObject.put(TAG_VALUE, longitude);
            }
            if (objType.equals(TYPE_PRIMARYKEY)) {
                itemObject.put(TAG_VALUE, tagName);
            }
        }

    }

    public static View addTextView( Context context, LinearLayout mainView, String value, String size, boolean withLine ) {
        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        textLayout.setLayoutParams(layoutParams);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        // textLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.formitem_background));
        mainView.addView(textLayout);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(value);

        size = size.trim();
        if (size.equals("large")) {
            textView.setTextAppearance(context, android.R.attr.textAppearanceLarge);
        } else if (size.equals("medium")) {
            textView.setTextAppearance(context, android.R.attr.textAppearanceMedium);
        } else if (size.equals("small")) {
            textView.setTextAppearance(context, android.R.attr.textAppearanceSmall);
        } else {
            int sizeInt = Integer.parseInt(size);
            textView.setTextSize(sizeInt);
        }
        textView.setTextColor(context.getResources().getColor(R.color.formcolor));
        textLayout.addView(textView);

        if (withLine) {
            View view = new View(context);
            view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 2));
            view.setBackgroundColor(context.getResources().getColor(R.color.formcolor));

            textLayout.addView(view);
        }
        return textView;
    }
}
