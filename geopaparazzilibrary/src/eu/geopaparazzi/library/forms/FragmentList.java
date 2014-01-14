package eu.geopaparazzi.library.forms;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class FragmentList extends android.support.v4.app.ListFragment {
    private List<String> fragmentTitles;
    private String selectedItemName;
    private FormActivity activity;
    private final int RETURNCODE_DETAILACTIVITY = 665;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        activity = (FormActivity) getActivity();
        fragmentTitles = activity.getFragmentTitles();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated(savedInstanceState);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.fragment_list_item,
                fragmentTitles.toArray(new String[0]));

        int color = getActivity().getResources().getColor(R.color.formcolor);
        int[] colors = {0, color, 0}; // red for the example
        ListView listView = getListView();
        listView.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
        listView.setDividerHeight(2);

        setListAdapter(adapter);
    }

    @Override
    public void onAttach( Activity activity ) {
        super.onAttach(activity);

    }

    @Override
    public void onListItemClick( ListView l, View v, int position, long id ) {
        selectedItemName = (String) getListAdapter().getItem(position);
        int childCount = l.getChildCount();
        for( int i = 0; i < childCount; i++ ) {
            View childAt = l.getChildAt(i);
            if (childAt instanceof TextView) {
                TextView textView = (TextView) childAt;
                textView.setTypeface(null, Typeface.NORMAL);
            }
        }
        if (v instanceof TextView) {
            TextView textView = (TextView) v;
            textView.setTypeface(null, Typeface.BOLD_ITALIC);
            // textView.setTextColor(v.getResources().getColor(R.color.formcolorselected));
        }

        JSONObject sectionObject = activity.getSectionObject();
        FragmentDetail oldFragment = (FragmentDetail) getFragmentManager().findFragmentById(R.id.detailFragment);
        if (oldFragment != null) {// && oldFragment.isInLayout()) {
            try {
                oldFragment.storeFormItems(false);
            } catch (Exception e) {
                e.printStackTrace();
                Utilities.messageDialog(activity, R.string.error_while_storing_form_data, null);
            }
            // FragmentActivity activity2 = oldFragment.getActivity();
            // int id2 = oldFragment.getId();

            FragmentDetail newFragment = new FragmentDetail();
            newFragment.setForm(selectedItemName, sectionObject);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.remove(oldFragment);
            transaction.add(R.id.detailFragment, newFragment);
            // transaction.replace(R.id.detailFragment, newFragment);
            // transaction.addToBackStack(null); // Ads FirstFragment to the back-stack
            transaction.commit();
        } else {
            // String sectionName = activity.getSectionName();
            Intent intent = new Intent(getActivity().getApplicationContext(), FragmentDetailActivity.class);
            intent.putExtra(FormUtilities.ATTR_FORMNAME, selectedItemName);
            intent.putExtra(FormUtilities.ATTR_SECTIONOBJECTSTR, sectionObject.toString());
            intent.putExtra(LibraryConstants.LONGITUDE, activity.getLongitude());
            intent.putExtra(LibraryConstants.LATITUDE, activity.getLatitude());
            startActivityForResult(intent, RETURNCODE_DETAILACTIVITY);
        }
    }

    /**
     * @return the selected name.
     */
    public String getSelectedItemName() {
        return selectedItemName;
    }

    /**
     * @return the json object.
     */
    public JSONObject getSectionObject() {
        return activity.getSectionObject();
    }

    /**
     * @return the latitude.
     */
    public double getLatitude() {
        return activity.getLatitude();
    }

    /**
     * @return the longitude.
     */
    public double getLongitude() {
        return activity.getLongitude();
    }

    public void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        switch( requestCode ) {
        case (RETURNCODE_DETAILACTIVITY): {
            if (resultCode == Activity.RESULT_OK) {
                String sectionStringObject = data.getStringExtra(FormUtilities.ATTR_SECTIONOBJECTSTR);
                try {
                    activity.setSectionObject(new JSONObject(sectionStringObject));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            break;
        }
        }
    }

}