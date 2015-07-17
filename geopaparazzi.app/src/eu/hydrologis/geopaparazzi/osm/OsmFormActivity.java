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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;

import static eu.geopaparazzi.library.forms.FormUtilities.*;

import eu.geopaparazzi.library.forms.FormUtilities;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.forms.constraints.Constraints;
import eu.geopaparazzi.library.forms.constraints.MandatoryConstraint;
import eu.geopaparazzi.library.forms.constraints.RangeConstraint;
import eu.geopaparazzi.library.forms.views.GBooleanView;
import eu.geopaparazzi.library.forms.views.GComboView;
import eu.geopaparazzi.library.forms.views.GEditTextView;
import eu.geopaparazzi.library.forms.views.GMultiComboView;
import eu.geopaparazzi.library.forms.views.GTextView;
import eu.geopaparazzi.library.forms.views.GView;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * The osm form activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class OsmFormActivity extends Activity {

    private String formJsonString;
    private String formLongnameDefinition;

    private HashMap<String, GView> key2WidgetMap = new HashMap<String, GView>();
    private HashMap<String, Constraints> key2ConstraintsMap = new HashMap<String, Constraints>();
    private List<String> keyList = new ArrayList<String>();
    private JSONArray formItemsArray;
    private JSONObject jsonFormObject;
    private double latitude;
    private double longitude;
    private String tagName;

    private boolean isExisting = false;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.osm_form);

        key2WidgetMap.clear();
        keyList.clear();
        key2ConstraintsMap.clear();

        Bundle extras = getIntent().getExtras();
        String category = extras.getString(Constants.OSM_CATEGORY_KEY);
        String form = extras.getString(Constants.OSM_FORM);
        final long id = extras.getLong(LibraryConstants.DATABASE_ID);
        tagName = extras.getString(Constants.OSM_TAG_KEY);
        formLongnameDefinition = tagName;

        TextView textView = (TextView) findViewById(R.id.osmform_textview);
        StringBuilder sb = new StringBuilder();
        sb.append(tagName.replace(FormUtilities.UNDERSCORE, " ").replace(FormUtilities.COLON, " "));
        if (category != null) {
            sb.append(" (");
            sb.append(category);
            sb.append(")");
        }
        textView.setText(sb.toString());

        if (form != null) {
            formJsonString = form;
            isExisting = true;
        } else {
            try {
                File tagsFolderFile = OsmTagsManager.getInstance().getTagsFolderFile(this);

                sb = new StringBuilder();
                sb.append(category);
                sb.append("/");
                sb.append(tagName);
                sb.append(".json");
                File tagsJsonFile = new File(tagsFolderFile, sb.toString());
                if (!tagsJsonFile.exists()) {
                    sb = new StringBuilder();
                    sb.append(category);
                    sb.append("/");
                    sb.append(category);
                    sb.append(".json");
                    tagsJsonFile = new File(tagsFolderFile, sb.toString());
                }
                formJsonString = FileUtilities.readfile(tagsJsonFile);
            } catch (Exception e1) {
                GPLog.error(this, null, e1); //$NON-NLS-1$
            }
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        double[] mapCenter = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);

        latitude = mapCenter[1];
        longitude = mapCenter[0];

        LinearLayout mainView = (LinearLayout) findViewById(R.id.osmform_linear);
        Button okButton = (Button) findViewById(R.id.osmform_ok);
        okButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String endString = "";
                try {
                    String result = storeNote();
                    if (result == null) {
                        endString = jsonFormObject.toString();

                        if (isExisting) {
                            DaoNotes.updateForm(id, tagName, endString);
                        } else {
                            long timestamp = System.currentTimeMillis();
                            DaoNotes.addNote(longitude, latitude, -1.0, timestamp, formLongnameDefinition, LibraryConstants.OSM,
                                    endString, null);
                        }
                        finish();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(OsmFormActivity.this);
                        String msg = MessageFormat.format(getString(R.string.check_valid_field), result);
                        builder.setMessage(msg).setCancelable(false)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // ignore
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                } catch (Exception e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    AlertDialog.Builder builder = new AlertDialog.Builder(OsmFormActivity.this);
                    builder.setMessage("An error occurred while saving:\n" + endString).setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // ignore
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }

        });
        Button cancelButton = (Button) findViewById(R.id.osmform_cancel);
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        try {
            JSONArray tagArrayObj = new JSONArray(formJsonString);
            jsonFormObject = tagArrayObj.getJSONObject(0);
            List<String> formNames = TagsManager.getFormNames4Section(jsonFormObject);
            int length = formNames.size();
            for (int i = 0; i < length; i++) {
                String name = formNames.get(i);
                JSONObject form4Name = TagsManager.getForm4Name(name, jsonFormObject);
                formItemsArray = TagsManager.getFormItems(form4Name);
                for (int j = 0; j < formItemsArray.length(); j++) {
                    JSONObject jsonObject = formItemsArray.getJSONObject(j);

                    String key = jsonObject.getString(TAG_KEY).trim();
                    String value = "";
                    if (jsonObject.has(TAG_VALUE)) {
                        value = jsonObject.getString(TAG_VALUE).trim();
                    }
                    String type = TYPE_STRING;
                    if (jsonObject.has(TAG_TYPE)) {
                        type = jsonObject.getString(TAG_TYPE).trim();
                    }
                    if (FormUtilities.isTypeSpecial(type)) {
                        continue;
                    }

                    Constraints constraints = new Constraints();
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
                            String highStr = rangeSplit[1].substring(1);
                            Double high = Utilities.adapt(highStr, Double.class);
                            constraints.addConstraint(new RangeConstraint(low, lowIncluded, high, highIncluded));
                        }
                    }
                    key2ConstraintsMap.put(key, constraints);
                    String constraintDescription = constraints.getDescription();

                    boolean readOnly = false;
                    GView addedView = null;
                    if (type.equals(TYPE_STRING)) {
                        addedView = FormUtilities.addEditText(this, mainView, key, value, 0, 0, constraintDescription, readOnly);
                    } else if (type.equals(TYPE_DOUBLE)) {
                        addedView = FormUtilities.addEditText(this, mainView, key, value, 1, 0, constraintDescription, readOnly);
                    } else if (type.equals(TYPE_BOOLEAN)) {
                        addedView = FormUtilities.addBooleanView(this, mainView, key, value, constraintDescription, readOnly);
                    } else if (type.equals(TYPE_STRINGCOMBO)) {
                        JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                        String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                        addedView = FormUtilities.addComboView(this, mainView, key, value, itemsArray, constraintDescription);
                    } else if (type.equals(TYPE_STRINGMULTIPLECHOICE)) {
                        JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                        String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                        addedView = FormUtilities
                                .addMultiSelectionView(this, mainView, key, value, itemsArray, constraintDescription);
                    } else {
                        GPLog.addLogEntry("Type non implemented yet: " + type);
                    }
                    key2WidgetMap.put(key, addedView);
                    keyList.add(key);
                }
            }
        } catch (JSONException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

    }

    private String storeNote() throws JSONException {

        // update the name with info
        jsonFormObject.put(TAG_LONGNAME, formLongnameDefinition);

        // update the items
        for (String key : keyList) {
            Constraints constraints = key2ConstraintsMap.get(key);

            GView view = key2WidgetMap.get(key);
            String text = null;
            if (view instanceof GBooleanView) {
                GBooleanView checkBox = (GBooleanView) view;
                text = checkBox.getValue();
            } else if (view instanceof Button) {
                Button button = (Button) view;
                text = button.getText().toString();
                if (text.trim().equals("...")) {
                    text = "";
                }
            } else if (view instanceof GEditTextView) {
                GEditTextView textView = (GEditTextView) view;
                text = textView.getValue();
            } else if (view instanceof GTextView) {
                GTextView textView = (GTextView) view;
                text = textView.getValue();
            } else if (view instanceof GComboView) {
                GComboView spinner = (GComboView) view;
                text = spinner.getValue();
            } else if (view instanceof GMultiComboView) {
                GMultiComboView spinner = (GMultiComboView) view;
                text = spinner.getValue();
            }

            if (!constraints.isValid(text)) {
                return key;
            }

            try {
                if (text != null)
                    FormUtilities.update(formItemsArray, key, text);

                FormUtilities.updateExtras(formItemsArray, latitude, longitude, tagName);

            } catch (JSONException e) {
                GPLog.error(this, e.getLocalizedMessage(), e);
                e.printStackTrace();
            }

        }

        return null;

    }

}
