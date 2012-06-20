package eu.geopaparazzi.library.forms;

import static eu.geopaparazzi.library.forms.FormUtilities.CONSTRAINT_MANDATORY;
import static eu.geopaparazzi.library.forms.FormUtilities.CONSTRAINT_RANGE;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_KEY;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_TYPE;
import static eu.geopaparazzi.library.forms.FormUtilities.TAG_VALUE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_BOOLEAN;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_DATE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_DOUBLE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_LABEL;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_LABELWITHLINE;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_STRING;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_STRINGCOMBO;
import static eu.geopaparazzi.library.forms.FormUtilities.TYPE_STRINGMULTIPLECHOICE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.forms.constraints.Constraints;
import eu.geopaparazzi.library.forms.constraints.MandatoryConstraint;
import eu.geopaparazzi.library.forms.constraints.RangeConstraint;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Logger;

public class FragmentDetail extends Fragment {

    private HashMap<String, View> key2WidgetMap = new HashMap<String, View>();
    private HashMap<String, Constraints> key2ConstraintsMap = new HashMap<String, Constraints>();
    private List<String> keyList = new ArrayList<String>();
    private LayoutInflater inflater;
    private ViewGroup container;
    private String selectedFormName;
    private JSONObject sectionObject;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        this.inflater = inflater;
        this.container = container;
        View view = inflater.inflate(R.layout.details, container, false);
        LinearLayout mainView = (LinearLayout) view.findViewById(R.id.form_linear);
        try {
            FragmentActivity activity = getActivity();
            if (selectedFormName == null || sectionObject == null) {
                FragmentList listFragment = (FragmentList) getFragmentManager().findFragmentById(R.id.listFragment);
                if (listFragment != null) {
                    selectedFormName = listFragment.getSelectedItemName();
                    sectionObject = listFragment.getSectionObject();
                } else {
                    if (activity instanceof FragmentDetailActivity) {
                        FragmentDetailActivity fragmentDetailActivity = (FragmentDetailActivity) activity;
                        selectedFormName = fragmentDetailActivity.getFormName();
                        String sectionName = fragmentDetailActivity.getSectionName();
                        sectionObject = TagsManager.getInstance(activity).getSectionByName(sectionName);
                    }
                }
            }
            if (selectedFormName != null) {
                JSONObject formObject = TagsManager.getInstance(activity).getForm4Name(selectedFormName, sectionObject);

                key2WidgetMap.clear();
                keyList.clear();
                key2ConstraintsMap.clear();

                JSONArray formItemsArray = TagsManager.getFormItems(formObject);

                int length = formItemsArray.length();
                for( int i = 0; i < length; i++ ) {
                    JSONObject jsonObject = formItemsArray.getJSONObject(i);

                    String key = "-";
                    if (jsonObject.has(TAG_KEY))
                        key = jsonObject.getString(TAG_KEY).trim();

                    String value = "";
                    if (jsonObject.has(TAG_VALUE)) {
                        value = jsonObject.getString(TAG_VALUE).trim();
                    }
                    String type = FormUtilities.TYPE_STRING;
                    if (jsonObject.has(TAG_TYPE)) {
                        type = jsonObject.getString(TAG_TYPE).trim();
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
                            boolean lowIncluded = rangeSplit[0].startsWith("[") ? true : false;
                            String lowStr = rangeSplit[0].substring(1);
                            Double low = Utilities.adapt(lowStr, Double.class);
                            boolean highIncluded = rangeSplit[1].endsWith("]") ? true : false;
                            String highStr = rangeSplit[1].substring(0, rangeSplit[1].length() - 1);
                            Double high = Utilities.adapt(highStr, Double.class);
                            constraints.addConstraint(new RangeConstraint(low, lowIncluded, high, highIncluded));
                        }
                    }
                    key2ConstraintsMap.put(key, constraints);
                    String constraintDescription = constraints.getDescription();

                    View addedView = null;
                    if (type.equals(TYPE_STRING)) {
                        addedView = FormUtilities.addEditText(activity, mainView, key, value, 0, constraintDescription);
                    } else if (type.equals(TYPE_DOUBLE)) {
                        addedView = FormUtilities.addEditText(activity, mainView, key, value, 1, constraintDescription);
                    } else if (type.equals(TYPE_DATE)) {
                        addedView = FormUtilities.addEditText(activity, mainView, key, value, 3, constraintDescription);
                    } else if (type.equals(TYPE_LABEL)) {
                        String size = jsonObject.getString("size");
                        addedView = FormUtilities.addTextView(activity, mainView, value, size, false);
                    } else if (type.equals(TYPE_LABELWITHLINE)) {
                        String size = jsonObject.getString("size");
                        addedView = FormUtilities.addTextView(activity, mainView, value, size, true);
                    } else if (type.equals(TYPE_BOOLEAN)) {
                        addedView = FormUtilities.addBooleanView(activity, mainView, key, value, constraintDescription);
                    } else if (type.equals(TYPE_STRINGCOMBO)) {
                        JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                        String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                        addedView = FormUtilities.addComboView(activity, mainView, key, value, itemsArray, constraintDescription);
                    } else if (type.equals(TYPE_STRINGMULTIPLECHOICE)) {
                        JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                        String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                        addedView = FormUtilities.addMultiSelectionView(activity, mainView, key, value, itemsArray,
                                constraintDescription);
                    } else {
                        System.out.println("Type non implemented yet: " + type);
                    }
                    key2WidgetMap.put(key, addedView);
                    keyList.add(key);

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }

    public void setForm( String selectedItemName, JSONObject sectionObject ) {
        this.selectedFormName = selectedItemName;
        this.sectionObject = sectionObject;
    }

    /**
     * Store the form items the widgets.
     * 
     * @param doConstraintsCheck if <code>true</code>, a check on all constraints is performed.
     * @return <code>null</code>, if everything was saved properly, the key of the items
     *              that didn't pass the constraint check.
     * @throws Exception
     */
    public String storeFormItems( boolean doConstraintsCheck ) throws Exception {
        if (selectedFormName == null) {
            return null;
        }
        JSONObject form4Name = TagsManager.getInstance(getActivity()).getForm4Name(selectedFormName, sectionObject);
        JSONArray formItems = TagsManager.getFormItems(form4Name);

        // update the items
        for( String key : keyList ) {
            Constraints constraints = key2ConstraintsMap.get(key);

            View view = key2WidgetMap.get(key);
            String text = null;
            if (view instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) view;
                boolean checked = checkBox.isChecked();
                text = checked ? "true" : "false";
            } else if (view instanceof Button) {
                Button button = (Button) view;
                text = button.getText().toString();
                if (text.trim().equals("...")) {
                    text = "";
                }
            } else if (view instanceof TextView) {
                TextView textView = (TextView) view;
                text = textView.getText().toString();
            } else if (view instanceof Spinner) {
                Spinner spinner = (Spinner) view;
                text = spinner.getSelectedItem().toString();
            }

            if (doConstraintsCheck && !constraints.isValid(text)) {
                return key;
            }

            try {
                if (text != null)
                    FormUtilities.update(formItems, key, text);
            } catch (JSONException e) {
                Logger.e(this, e.getLocalizedMessage(), e);
                e.printStackTrace();
            }
        }

        return null;
    }

}