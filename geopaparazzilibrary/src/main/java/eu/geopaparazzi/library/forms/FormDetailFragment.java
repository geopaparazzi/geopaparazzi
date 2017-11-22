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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IImagesDbHelper;
import eu.geopaparazzi.library.forms.constraints.Constraints;
import eu.geopaparazzi.library.forms.views.GMapView;
import eu.geopaparazzi.library.forms.views.GNfcUidView;
import eu.geopaparazzi.library.forms.views.GView;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.NamedList;

import static eu.geopaparazzi.library.forms.FormUtilities.TAG_KEY;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_LABEL;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_READONLY;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_SIZE;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_TYPE;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_URL;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_VALUE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_AUTOCOMPLETECONNECTEDSTRINGCOMBO;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_AUTOCOMPLETESTRINGCOMBO;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_BOOLEAN;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_CONNECTEDSTRINGCOMBO;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_DATE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_DOUBLE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_DYNAMICSTRING;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_HIDDEN;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_INTEGER;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_LABEL;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_LABELWITHLINE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_MAP;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_NFCUID;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_ONETOMANYSTRINGCOMBO;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_PICTURES;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_SKETCH;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_STRING;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_STRINGAREA;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_STRINGCOMBO;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_STRINGMULTIPLECHOICE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_TIME;

/**
 * The fragment detail view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FormDetailFragment extends android.support.v4.app.Fragment {
    public static final String ARGS_FORMINFO = "args_forminfo";

    private HashMap<String, GView> key2WidgetMap = new HashMap<>();
    private SparseArray<GView> requestCodes2WidgetMap = new SparseArray<>();
    private HashMap<String, Constraints> key2ConstraintsMap = new HashMap<>();
    private List<String> keyList = new ArrayList<>();
    private String selectedFormName;
    private JSONObject sectionObject;
    private FormInfoHolder mFormInfoHolder;
    private LinearLayout mainView;

    public static FormDetailFragment newInstance(FormInfoHolder formInfoHolder) {
        FormDetailFragment f = new FormDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARGS_FORMINFO, formInfoHolder);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null)
            mFormInfoHolder = (FormInfoHolder) arguments.getSerializable(ARGS_FORMINFO);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        try {

            view = inflater.inflate(R.layout.fragment_form_detail, container, false);
            mainView = (LinearLayout) view.findViewById(R.id.form_linear);

            refreshView(mFormInfoHolder);
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
        return view;
    }

    public void refreshView(FormInfoHolder formInfoHolder) throws Exception {
        if (mainView == null) return;

        mFormInfoHolder = formInfoHolder;
        long noteId = -1;
        double longitude;
        double latitude;
        if (mFormInfoHolder != null) {
            selectedFormName = mFormInfoHolder.formName;
            sectionObject = new JSONObject(mFormInfoHolder.sectionObjectString);
            noteId = mFormInfoHolder.noteId;
            longitude = mFormInfoHolder.longitude;
            latitude = mFormInfoHolder.latitude;
        } else {
            return;
        }

        mainView.removeAllViews();

        if (selectedFormName != null) {
            Activity activity = getActivity();
            JSONObject formObject = TagsManager.getForm4Name(selectedFormName, sectionObject);

            key2WidgetMap.clear();
            requestCodes2WidgetMap.clear();
            int requestCode = 666;
            keyList.clear();
            key2ConstraintsMap.clear();

            JSONArray formItemsArray = TagsManager.getFormItems(formObject);

            int length = formItemsArray.length();
            for (int i = 0; i < length; i++) {
                JSONObject jsonObject = formItemsArray.getJSONObject(i);

                String key = "-"; //$NON-NLS-1$
                if (jsonObject.has(TAG_KEY))
                    key = jsonObject.getString(TAG_KEY).trim();

                String label = key;
                if (jsonObject.has(TAG_LABEL))
                    label = jsonObject.getString(TAG_LABEL).trim();

                String value = ""; //$NON-NLS-1$
                if (jsonObject.has(TAG_VALUE)) {
                    value = jsonObject.getString(TAG_VALUE).trim();
                }
                String type = FormUtilities.TYPE_STRING;
                if (jsonObject.has(TAG_TYPE)) {
                    type = jsonObject.getString(TAG_TYPE).trim();
                }

                boolean readonly = false;
                if (jsonObject.has(TAG_READONLY)) {
                    String readonlyStr = jsonObject.getString(TAG_READONLY).trim();
                    readonly = Boolean.parseBoolean(readonlyStr);
                }

                Constraints constraints = new Constraints();
                FormUtilities.handleConstraints(jsonObject, constraints);
                key2ConstraintsMap.put(key, constraints);
                String constraintDescription = constraints.getDescription();

                GView addedView = null;
                switch (type) {
                    case TYPE_STRING:
                        addedView = FormUtilities.addEditText(activity, mainView, label, value, 0, 0, constraintDescription,
                                readonly);
                        break;
                    case TYPE_DYNAMICSTRING:
                        addedView = FormUtilities.addDynamicEditText(activity, mainView, label, value, 0, constraintDescription,
                                readonly);
                        break;
                    case TYPE_STRINGAREA:
                        addedView = FormUtilities.addEditText(activity, mainView, label, value, 0, 7, constraintDescription,
                                readonly);
                        break;
                    case TYPE_DOUBLE:
                        addedView = FormUtilities.addEditText(activity, mainView, label, value, 1, 0, constraintDescription,
                                readonly);
                        break;
                    case TYPE_INTEGER:
                        addedView = FormUtilities.addEditText(activity, mainView, label, value, 4, 0, constraintDescription,
                                readonly);
                        break;
                    case TYPE_DATE:
                        addedView = FormUtilities.addDateView(FormDetailFragment.this, mainView, label, value, constraintDescription,
                                readonly);
                        break;
                    case TYPE_TIME:
                        addedView = FormUtilities.addTimeView(FormDetailFragment.this, mainView, label, value, constraintDescription,
                                readonly);
                        break;
                    case TYPE_LABEL: {
                        String size = "20"; //$NON-NLS-1$

                        if (jsonObject.has(TAG_SIZE))
                            size = jsonObject.getString(TAG_SIZE);
                        String url = null;
                        if (jsonObject.has(TAG_URL))
                            url = jsonObject.getString(TAG_URL);
                        addedView = FormUtilities.addTextView(activity, mainView, value, size, false, url);
                        break;
                    }
                    case TYPE_LABELWITHLINE: {
                        String size = "20"; //$NON-NLS-1$

                        if (jsonObject.has(TAG_SIZE))
                            size = jsonObject.getString(TAG_SIZE);
                        String url = null;
                        if (jsonObject.has(TAG_URL))
                            url = jsonObject.getString(TAG_URL);
                        addedView = FormUtilities.addTextView(activity, mainView, value, size, true, url);
                        break;
                    }
                    case TYPE_BOOLEAN:
                        addedView = FormUtilities.addBooleanView(activity, mainView, label, value, constraintDescription, readonly);
                        break;
                    case TYPE_STRINGCOMBO: {
                        JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                        String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                        addedView = FormUtilities.addComboView(activity, mainView, label, value, itemsArray, constraintDescription);
                        break;
                    }
                    case TYPE_AUTOCOMPLETESTRINGCOMBO: {
                        JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                        String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                        addedView = FormUtilities.addAutocompleteComboView(activity, mainView, label, value, itemsArray, constraintDescription);
                        break;
                    }
                    case TYPE_CONNECTEDSTRINGCOMBO: {
                        LinkedHashMap<String, List<String>> valuesMap = TagsManager.extractComboValuesMap(jsonObject);
                        addedView = FormUtilities.addConnectedComboView(activity, mainView, label, value, valuesMap,
                                constraintDescription);
                        break;
                    }
                    case TYPE_AUTOCOMPLETECONNECTEDSTRINGCOMBO: {
                        LinkedHashMap<String, List<String>> valuesMap = TagsManager.extractComboValuesMap(jsonObject);
                        addedView = FormUtilities.addAutoCompleteConnectedComboView(activity, mainView, label, value, valuesMap,
                                constraintDescription);
                        break;
                    }
                    case TYPE_ONETOMANYSTRINGCOMBO:
                        LinkedHashMap<String, List<NamedList<String>>> oneToManyValuesMap = TagsManager.extractOneToManyComboValuesMap(jsonObject);
                        addedView = FormUtilities.addOneToManyConnectedComboView(activity, mainView, label, value, oneToManyValuesMap,
                                constraintDescription);
                        break;
                    case TYPE_STRINGMULTIPLECHOICE: {
                        JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                        String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                        addedView = FormUtilities.addMultiSelectionView(activity, mainView, label, value, itemsArray,
                                constraintDescription);
                        break;
                    }
                    case TYPE_PICTURES:
                        addedView = FormUtilities.addPictureView(noteId, this, requestCode, mainView, label, value, constraintDescription);
                        break;
                    case TYPE_SKETCH:
                        addedView = FormUtilities.addSketchView(noteId, this, requestCode, mainView, label, value, constraintDescription);
                        break;
                    case TYPE_MAP:
                        if (value.length() <= 0) {
                            // need to read image
                            File tempDir = ResourcesManager.getInstance(activity).getTempDir();
                            File tmpImage = new File(tempDir, LibraryConstants.TMPPNGIMAGENAME);
                            if (tmpImage.exists()) {
                                byte[][] imageAndThumbnailFromPath = ImageUtilities.getImageAndThumbnailFromPath(tmpImage.getAbsolutePath(), 1);
                                Date date = new Date();
                                String mapImageName = ImageUtilities.getMapImageName(date);

                                IImagesDbHelper imageHelper = DefaultHelperClasses.getDefaulfImageHelper();
                                long imageId = imageHelper.addImage(longitude, latitude, -1.0, -1.0, date.getTime(), mapImageName, imageAndThumbnailFromPath[0], imageAndThumbnailFromPath[1], noteId);
                                value = "" + imageId;
                            }
                        }
                        addedView = FormUtilities.addMapView(activity, mainView, label, value, constraintDescription);
                        break;
                    case TYPE_NFCUID:
                        addedView = new GNfcUidView(this, null, requestCode, mainView, label, value, constraintDescription);
                        break;
                    case TYPE_HIDDEN:
                        break;
                    default:
                        GPLog.addLogEntry(this, null, null, "Type non implemented yet: " + type);
                        break;
                }
                key2WidgetMap.put(key, addedView);
                keyList.add(key);
                requestCodes2WidgetMap.put(requestCode, addedView);
                requestCode++;
            }

        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            GView gView = requestCodes2WidgetMap.get(requestCode);
            if (gView != null) {
                gView.setOnActivityResult(data);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        for (int i = 0; i < requestCodes2WidgetMap.size(); i++) {
            int key = requestCodes2WidgetMap.keyAt(i);
            // get the object by the key.
            GView view = requestCodes2WidgetMap.get(key);
            if (view instanceof GMapView) {
                try {
                    view.refresh(((GMapView) view).getContext());
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                }
            }
        }
    }

    /**
     * @return the updated form info.
     */
    public FormInfoHolder getFormInfoHolder() {
        mFormInfoHolder.sectionObjectString = sectionObject.toString();
        return mFormInfoHolder;
    }


    /**
     * Store the form items the widgets.
     *
     * @param doConstraintsCheck if <code>true</code>, a check on all constraints is performed.
     * @return <code>null</code>, if everything was saved properly, the key of the items
     * that didn't pass the constraint check.
     * @throws Exception if something goes wrong.
     */
    public String storeFormItems(boolean doConstraintsCheck) throws Exception {
        if (selectedFormName == null) {
            return null;
        }
        JSONObject form4Name = TagsManager.getForm4Name(selectedFormName, sectionObject);
        JSONArray formItems = TagsManager.getFormItems(form4Name);

        // update the items
        for (String key : keyList) {
            Constraints constraints = key2ConstraintsMap.get(key);

            GView view = key2WidgetMap.get(key);
            if (view != null) {
                String text = view.getValue();
                if (doConstraintsCheck && !constraints.isValid(text)) {
                    return key;
                }

                try {
                    if (text != null)
                        FormUtilities.update(formItems, key, text);
                } catch (JSONException e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                }
            }
        }

        FormUtilities.updateExtras(formItems, mFormInfoHolder.latitude, mFormInfoHolder.longitude, null);
        return null;
    }

}