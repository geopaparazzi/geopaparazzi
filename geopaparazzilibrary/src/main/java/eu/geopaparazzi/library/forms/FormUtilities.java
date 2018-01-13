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
package eu.geopaparazzi.library.forms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import eu.geopaparazzi.library.forms.constraints.Constraints;
import eu.geopaparazzi.library.forms.constraints.MandatoryConstraint;
import eu.geopaparazzi.library.forms.constraints.RangeConstraint;
import eu.geopaparazzi.library.forms.views.GAutocompleteTextView;
import eu.geopaparazzi.library.forms.views.GBooleanView;
import eu.geopaparazzi.library.forms.views.GComboView;
import eu.geopaparazzi.library.forms.views.GDateView;
import eu.geopaparazzi.library.forms.views.GDynamicEditTextView;
import eu.geopaparazzi.library.forms.views.GEditTextView;
import eu.geopaparazzi.library.forms.views.GMapView;
import eu.geopaparazzi.library.forms.views.GMultiComboView;
import eu.geopaparazzi.library.forms.views.GNfcUidView;
import eu.geopaparazzi.library.forms.views.GOneToManyConnectedComboView;
import eu.geopaparazzi.library.forms.views.GPictureView;
import eu.geopaparazzi.library.forms.views.GSketchView;
import eu.geopaparazzi.library.forms.views.GTextView;
import eu.geopaparazzi.library.forms.views.GTimeView;
import eu.geopaparazzi.library.forms.views.GTwoAutoCompleteConnectedTextView;
import eu.geopaparazzi.library.forms.views.GTwoConnectedComboView;
import eu.geopaparazzi.library.forms.views.GView;
import eu.geopaparazzi.library.util.MultipleChoiceDialog;
import eu.geopaparazzi.library.util.NamedList;
import eu.geopaparazzi.library.util.Utilities;

/**
 * Utilities methods for form stuff.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 2.6
 */
@SuppressWarnings("nls")
public class FormUtilities {

    /**
     *
     */
    public static final String COLON = ":";
    /**
     *
     */
    public static final String UNDERSCORE = "_";

    /**
     * Type for a {@link TextView}.
     */
    public static final String TYPE_LABEL = "label";

    /**
     * Type for a {@link TextView} with line below.
     */
    public static final String TYPE_LABELWITHLINE = "labelwithline";

    /**
     * Type for a {@link EditText} containing generic text.
     */
    public static final String TYPE_STRING = "string";

    /**
     * Type for a dynamic (multiple) {@link EditText} containing generic text.
     */
    public static final String TYPE_DYNAMICSTRING = "dynamicstring";

    /**
     * Type for a {@link EditText} area containing generic text.
     */
    public static final String TYPE_STRINGAREA = "stringarea";

    /**
     * Type for a {@link EditText} containing double numbers.
     */
    public static final String TYPE_DOUBLE = "double";

    /**
     * Type for a {@link EditText} containing integer numbers.
     */
    public static final String TYPE_INTEGER = "integer";

    /**
     * Type for a {@link Button} containing date.
     */
    public static final String TYPE_DATE = "date";

    /**
     * Type for a {@link Button} containing time.
     */
    public static final String TYPE_TIME = "time";

    /**
     * Type for a {@link CheckBox}.
     */
    public static final String TYPE_BOOLEAN = "boolean";

    /**
     * Type for a {@link Spinner}.
     */
    public static final String TYPE_STRINGCOMBO = "stringcombo";

    /**
     * Type for an autocomplete combo.
     */
    public static final String TYPE_AUTOCOMPLETESTRINGCOMBO = "autocompletestringcombo";

    /**
     * Type for autocomplete connected combos.
     */
    public static final String TYPE_AUTOCOMPLETECONNECTEDSTRINGCOMBO = "autocompleteconnectedstringcombo";

    /**
     * Type for two connected {@link Spinner}.
     */
    public static final String TYPE_CONNECTEDSTRINGCOMBO = "connectedstringcombo";

    /**
     * Type for one to many connected {@link Spinner}.
     */
    public static final String TYPE_ONETOMANYSTRINGCOMBO = "onetomanystringcombo";

    /**
     * Type for a multi combo.
     */
    public static final String TYPE_STRINGMULTIPLECHOICE = "multistringcombo";

    /**
     * Type for a the NFC UID reader.
     */
    public static final String TYPE_NFCUID = "nfcuid";

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
     * <p/>
     * <p>This is needed in case of abstraction of forms.</p>
     */
    public static final String TYPE_PRIMARYKEY = "primary_key";

    /**
     * Type for pictures element.
     */
    public static final String TYPE_PICTURES = "pictures";

    /**
     * Type for pictures element.
     */
    public static final String TYPE_SKETCH = "sketch";

    /**
     * Type for map element.
     */
    public static final String TYPE_MAP = "map";

    /**
     * Type for barcode element.
     * <p>
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

    /**
     *
     */
    public static final String ATTR_SECTIONNAME = "sectionname";
    /**
     *
     */
    public static final String ATTR_FORMS = "forms";
    /**
     *
     */
    public static final String ATTR_FORMNAME = "formname";

    /**
     *
     */
    public static final String TAG_LONGNAME = "longname";
    /**
     *
     */
    public static final String TAG_SHORTNAME = "shortname";
    /**
     *
     */
    public static final String TAG_FORMS = "forms";
    /**
     *
     */
    public static final String TAG_FORMITEMS = "formitems";
    /**
     *
     */
    public static final String TAG_KEY = "key";
    /**
     *
     */
    public static final String TAG_LABEL = "label";
    /**
     *
     */
    public static final String TAG_VALUE = "value";
    /**
     *
     */
    public static final String TAG_IS_RENDER_LABEL = "islabel";
    /**
     *
     */
    public static final String TAG_VALUES = "values";
    /**
     *
     */
    public static final String TAG_ITEMS = "items";
    public static final String TAG_ITEMNAME = "itemname";
    /**
     *
     */
    public static final String TAG_ITEM = "item";
    /**
     *
     */
    public static final String TAG_TYPE = "type";
    /**
     *
     */
    public static final String TAG_READONLY = "readonly";
    /**
     *
     */
    public static final String TAG_SIZE = "size";
    /**
     *
     */
    public static final String TAG_URL = "url";

    /**
     * Separator for multiple items in the form results.
     */
    public static String SEP = "#";

    /**
     * Checks if the type is a special one.
     *
     * @param type the type string from the form.
     * @return <code>true</code> if the type is special.
     */
    public static boolean isTypeSpecial(String type) {
        if (type.equals(TYPE_PRIMARYKEY)) {
            return true;
        } else if (type.equals(TYPE_HIDDEN)) {
            return true;
        }
        return false;
    }

    /**
     * Adds a {@link TextView} to the supplied mainView.
     *
     * @param context               the context.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label identifying the widget.
     * @param value                 the value to put in the widget.
     * @param type                  the text type:
     *                              <ul>
     *                              <li>0: text</li>
     *                              <li>1: double number</li>
     *                              <li>2: phone</li>
     *                              <li>3: date</li>
     *                              <li>4: integer number</li>
     *                              </ul>
     * @param lines                 lines or 0
     * @param constraintDescription constraint
     * @param readonly              if <code>true</code>, it is disabled for editing.
     * @return the added view.
     */
    public static GView addEditText(Context context, LinearLayout mainView, String label, String value, int type, int lines,
                                    String constraintDescription, boolean readonly) {
        return new GEditTextView(context, null, mainView, label, value, type, lines, constraintDescription,
                readonly);
    }

    /**
     * Adds a dynamic {@link TextView} to the supplied mainView.
     *
     * @param context               the context.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label identifying the widget.
     * @param value                 the value to put in the widget.
     * @param type                  the text type:
     *                              <ul>
     *                              <li>0: text</li>
     *                              <li>1: double number</li>
     *                              <li>2: phone</li>
     *                              <li>3: date</li>
     *                              <li>4: integer number</li>
     *                              </ul>
     * @param constraintDescription constraint
     * @param readonly              if <code>true</code>, it is disabled for editing.
     * @return the added view.
     */
    public static GView addDynamicEditText(Context context, LinearLayout mainView, String label, String value, int type,
                                           String constraintDescription, boolean readonly) {
        return new GDynamicEditTextView(context, null, mainView, label, value, type, constraintDescription,
                readonly);
    }

    /**
     * Adds a {@link GTextView} to the supplied mainView.
     *
     * @param context  the context.
     * @param mainView the main view to which to add the new widget to.
     * @param value    the value to put in the widget.
     * @param size     the size.
     * @param withLine if it should have a line.
     * @param url      the url.
     * @return the added view.
     */
    public static GView addTextView(final Context context, LinearLayout mainView, String value, String size, boolean withLine,
                                    final String url) {
        return new GTextView(context, null, mainView, value, size, withLine, url);
    }

    /**
     * Adds a {@link CheckBox} to the supplied mainView.
     *
     * @param context               the context.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param constraintDescription constraint
     * @param readonly              if <code>true</code>, it is disabled for editing.
     * @return the added view.
     */
    public static GView addBooleanView(Context context, LinearLayout mainView, String label, String value,
                                       String constraintDescription, boolean readonly) {
        return new GBooleanView(context, null, mainView, label, value, constraintDescription, readonly);
    }

    /**
     * Adds a {@link Spinner} to the supplied mainView.
     *
     * @param context               the context.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param itemsArray            the items to put in the spinner.
     * @param constraintDescription constraint
     * @return the added view.
     */
    public static GView addComboView(Context context, LinearLayout mainView, String label, String value, String[] itemsArray,
                                     String constraintDescription) {
        return new GComboView(context, null, mainView, label, value, itemsArray, constraintDescription);
    }

    /**
     * Adds an autocomplete combo to the supplied mainView.
     *
     * @param context               the context.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param itemsArray            the items to put in the spinner.
     * @param constraintDescription constraint
     * @return the added view.
     */
    public static GView addAutocompleteComboView(Context context, LinearLayout mainView, String label, String value, String[] itemsArray,
                                     String constraintDescription) {
        return new GAutocompleteTextView(context, null, mainView, label, value, itemsArray, constraintDescription);
    }

    /**
     * Adds two connected {@link Spinner} to the supplied mainView.
     *
     * @param context               the context.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param valuesMap             the map of connected strings to put in the spinners.
     * @param constraintDescription constraint
     * @return the added view.
     */
    public static GView addConnectedComboView(Context context, LinearLayout mainView, String label, String value,
                                              LinkedHashMap<String, List<String>> valuesMap, String constraintDescription) {
        return new GTwoConnectedComboView(context, null, mainView, label, value, valuesMap,
                constraintDescription);
    }

    /**
     * Adds two connected autocomplete views to the supplied mainView.
     *
     * @param context               the context.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param valuesMap             the map of connected strings to put in the spinners.
     * @param constraintDescription constraint
     * @return the added view.
     */
    public static GView addAutoCompleteConnectedComboView(Context context, LinearLayout mainView, String label, String value,
                                              LinkedHashMap<String, List<String>> valuesMap, String constraintDescription) {
        return new GTwoAutoCompleteConnectedTextView(context, null, mainView, label, value, valuesMap,
                constraintDescription);
    }

    /**
     * Adds one to many connected {@link Spinner} to the supplied mainView.
     *
     * @param context               the context.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param valuesMap             the map of connected strings to put in the spinners.
     * @param constraintDescription constraint
     * @return the added view.
     */
    public static GView addOneToManyConnectedComboView(Context context, LinearLayout mainView, String label, String value,
                                                       LinkedHashMap<String, List<NamedList<String>>> valuesMap, String constraintDescription) {
        return new GOneToManyConnectedComboView(context, null, mainView, label, value, valuesMap,
                constraintDescription);
    }

    /**
     * Adds a {@link MultipleChoiceDialog} to the supplied mainView.
     *
     * @param context               the context.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param itemsArray            the items to put in the spinner.
     * @param constraintDescription constraint
     * @return the added view.
     */
    public static GView addMultiSelectionView(final Context context, LinearLayout mainView, String label, String value,
                                              final String[] itemsArray, String constraintDescription) {
        return new GMultiComboView(context, null, mainView, label, value, itemsArray, constraintDescription);
    }

    /**
     * Adds a {@link GPictureView} to the supplied mainView.
     *
     * @param noteId                the note id this form belogs to.
     * @param fragmentDetail        the fragmentDetail.
     * @param requestCode           the code to use for activity return.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param constraintDescription constraint
     * @return the added view.
     */
    public static GView addPictureView(long noteId, FormDetailFragment fragmentDetail, int requestCode, LinearLayout mainView, String label, String value,
                                       String constraintDescription) {
        return new GPictureView(noteId, fragmentDetail, null, requestCode, mainView, label, value, constraintDescription);
    }

    /**
     * Adds a {@link GSketchView} to the supplied mainView.
     *
     * @param noteId                the note id this form belogs to.
     * @param fragmentDetail        the fragmentDetail.
     * @param requestCode           the code to use for activity return.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param constraintDescription constraint
     * @return the added view.
     */
    public static GView addSketchView(long noteId, FormDetailFragment fragmentDetail, int requestCode, LinearLayout mainView, String label, String value,
                                      String constraintDescription) {
        return new GSketchView(noteId, fragmentDetail, null, requestCode, mainView, label, value, constraintDescription);
    }

    /**
     * Adds a {@link GMapView} to the supplied mainView.
     *
     * @param context               the context.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param constraintDescription constraint
     * @return the added view.
     */
    public static GView addMapView(final Context context, LinearLayout mainView, String label, String value,
                                   String constraintDescription) {
        return new GMapView(context, null, mainView, label, value, constraintDescription);
    }

    /**
     * Adds a {@link DatePicker} to the supplied mainView.
     *
     * @param fragment              the parent {@link Fragment}.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param constraintDescription constraint
     * @param readonly              if <code>true</code>, it is disabled for editing.
     * @return the added view.
     */
    public static GView addDateView(final Fragment fragment, LinearLayout mainView, String label, String value,
                                    String constraintDescription, boolean readonly) {
        return new GDateView(fragment, null, mainView, label, value, constraintDescription, readonly);
    }

    /**
     * Adds a {@link TimePickerDialog} to the supplied mainView.
     *
     * @param fragment              the parent {@link Fragment}.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param constraintDescription constraint
     * @param readonly              if <code>true</code>, it is disabled for editing.
     * @return the added view.
     */
    public static GView addTimeView(final Fragment fragment, LinearLayout mainView, String label, String value,
                                    String constraintDescription, boolean readonly) {
        return new GTimeView(fragment, null, mainView, label, value, constraintDescription, readonly);
    }

    /**
     * Adds a {@link GNfcUidView} to the supplied mainView.
     *
     * @param activity              the activity
     * @param requestCode           teh requestcode for activity return.
     * @param mainView              the main view to which to add the new widget to.
     * @param label                 the label of the widget.
     * @param value                 the value to put in the widget.
     * @param constraintDescription constraint
     * @return the added view.
     */
    public static GView addNfcUIDView(Activity activity, int requestCode, LinearLayout mainView, String label, String value,
                                      String constraintDescription) {
        return new GNfcUidView(activity, null, requestCode, mainView, label, value, constraintDescription);
    }

    /**
     * Check an {@link JSONObject object} for constraints and collect them.
     *
     * @param jsonObject  the object to check.
     * @param constraints the {@link Constraints} object to use or <code>null</code>.
     * @return the original {@link Constraints} object or a new created.
     * @throws Exception if something goes wrong.
     */
    public static Constraints handleConstraints(JSONObject jsonObject, Constraints constraints) throws Exception {
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
                boolean lowIncluded = rangeSplit[0].startsWith("[");
                String lowStr = rangeSplit[0].substring(1);
                Double low = Utilities.adapt(lowStr, Double.class);
                boolean highIncluded = rangeSplit[1].endsWith("]");
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
     * @param key            the key of the item to update.
     * @param value          the new value to use.
     * @throws JSONException if something goes wrong.
     */
    public static void update(JSONArray formItemsArray, String key, String value) throws JSONException {
        int length = formItemsArray.length();
        for (int i = 0; i < length; i++) {
            JSONObject itemObject = formItemsArray.getJSONObject(i);
            if (itemObject.has(TAG_KEY)) {
                String objKey = itemObject.getString(TAG_KEY).trim();
                if (objKey.equals(key)) {
                    itemObject.put(TAG_VALUE, value);
                }
            }
        }
    }

    /**
     * Update those fields that do not generate widgets.
     *
     * @param formItemsArray the items array.
     * @param latitude       the lat value.
     * @param longitude      the long value.
     * @param pkValue        an optional value to set the PRIMARYKEY to.
     * @throws JSONException if something goes wrong.
     */
    public static void updateExtras(JSONArray formItemsArray, double latitude, double longitude, String pkValue) throws JSONException {
        int length = formItemsArray.length();
        // TODO check back if it would be good to check also on labels
        for (int i = 0; i < length; i++) {
            JSONObject itemObject = formItemsArray.getJSONObject(i);
            if (itemObject.has(TAG_KEY)) {
                String objKey = itemObject.getString(TAG_KEY).trim();
                if (objKey.contains(TYPE_LATITUDE)) {
                    itemObject.put(TAG_VALUE, latitude);
                } else if (objKey.contains(TYPE_LONGITUDE)) {
                    itemObject.put(TAG_VALUE, longitude);
                }
                if (pkValue != null && itemObject.has(TAG_TYPE))
                    if (itemObject.getString(TAG_TYPE).trim().equals(TYPE_PRIMARYKEY)) {
                        itemObject.put(TAG_VALUE, pkValue);
                    }
            }
        }
    }

    /**
     * Transforms a form content to its plain text representation.
     * <p/>
     * <p>Media are inserted as the file name.</p>
     *
     * @param section    the json form.
     * @param withTitles if <code>true</code>, all the section titles are added.
     * @return the plain text representation of the form.
     * @throws Exception if something goes wrong.
     */
    public static String formToPlainText(String section, boolean withTitles) throws Exception {

        StringBuilder sB = new StringBuilder();
        JSONObject sectionObject = new JSONObject(section);
        if (withTitles) {
            if (sectionObject.has(FormUtilities.ATTR_SECTIONNAME)) {
                String sectionName = sectionObject.getString(FormUtilities.ATTR_SECTIONNAME);
                sB.append(sectionName).append("\n");
                for (int i = 0; i < sectionName.length(); i++) {
                    sB.append("=");
                }
                sB.append("\n");
            }
        }

        List<String> formsNames = TagsManager.getFormNames4Section(sectionObject);
        for (String formName : formsNames) {
            if (withTitles) {
                sB.append(formName).append("\n");
                for (int i = 0; i < formName.length(); i++) {
                    sB.append("-").append("-");
                }
                sB.append("\n");
            }
            JSONObject form4Name = TagsManager.getForm4Name(formName, sectionObject);
            JSONArray formItems = TagsManager.getFormItems(form4Name);
            for (int i = 0; i < formItems.length(); i++) {
                JSONObject formItem = formItems.getJSONObject(i);
                if (!formItem.has(FormUtilities.TAG_KEY)) {
                    continue;
                }

                String type = formItem.getString(FormUtilities.TAG_TYPE);
                String key = formItem.getString(FormUtilities.TAG_KEY);
                String value = formItem.getString(FormUtilities.TAG_VALUE);
                String label = key;
                if (formItem.has(FormUtilities.TAG_LABEL)) {
                    label = formItem.getString(FormUtilities.TAG_LABEL);
                }

                if (type.equals(FormUtilities.TYPE_PICTURES) || type.equals(FormUtilities.TYPE_MAP)
                        || type.equals(FormUtilities.TYPE_SKETCH)) {
                    if (value.trim().length() == 0) {
                        continue;
                    }
                    String[] imageSplit = value.split(";");
                    for (String image : imageSplit) {
                        File imgFile = new File(image);
                        String imgName = imgFile.getName();
                        sB.append(label).append(": ");
                        sB.append(imgName);
                        sB.append("\n");
                    }
                } else {
                    sB.append(label).append(": ");
                    sB.append(value);
                    sB.append("\n");
                }
            }
        }
        return sB.toString();
    }

    /**
     * Get the images paths out of a form string.
     *
     * @param formString the form.
     * @return the list of images paths.
     * @throws Exception if something goes wrong.
     */
    public static List<String> getImageIds(String formString) throws Exception {
        List<String> imageIds = new ArrayList<String>();
        if (formString != null && formString.length() > 0) {
            JSONObject sectionObject = new JSONObject(formString);
            List<String> formsNames = TagsManager.getFormNames4Section(sectionObject);
            for (String formName : formsNames) {
                JSONObject form4Name = TagsManager.getForm4Name(formName, sectionObject);
                JSONArray formItems = TagsManager.getFormItems(form4Name);
                for (int i = 0; i < formItems.length(); i++) {
                    JSONObject formItem = formItems.getJSONObject(i);
                    if (!formItem.has(FormUtilities.TAG_KEY)) {
                        continue;
                    }

                    String type = formItem.getString(FormUtilities.TAG_TYPE);
                    String value = formItem.getString(FormUtilities.TAG_VALUE);

                    if (type.equals(FormUtilities.TYPE_PICTURES)) {
                        if (value.trim().length() == 0) {
                            continue;
                        }
                        String[] imageSplit = value.split(";");
                        Collections.addAll(imageIds, imageSplit);
                    } else if (type.equals(FormUtilities.TYPE_MAP)) {
                        if (value.trim().length() == 0) {
                            continue;
                        }
                        String image = value.trim();
                        imageIds.add(image);
                    } else if (type.equals(FormUtilities.TYPE_SKETCH)) {
                        if (value.trim().length() == 0) {
                            continue;
                        }
                        String[] imageSplit = value.split(";");
                        Collections.addAll(imageIds, imageSplit);
                    }
                }
            }
        }
        return imageIds;
    }

    /**
     * Make the given string json safe.
     *
     * @param text the srting to check.
     * @return the modified string.
     */
    public static String makeTextJsonSafe(String text) {
        text = text.replaceAll("\"", "'");
        return text;
    }

}
