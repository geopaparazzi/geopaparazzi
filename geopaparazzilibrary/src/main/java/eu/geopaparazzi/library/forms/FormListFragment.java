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

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import eu.geopaparazzi.library.R;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FormListFragment extends android.support.v4.app.ListFragment {
    private String selectedItemName;

    private IFragmentListSupporter mFragmentListSupporter;

    /**
     * Intereface to keep track of the selected view.
     */
    public interface IFragmentListSupporter {
        void onListItemSelected(String selectedItemName);

        List<String> getListTitles();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        List<String> fragmentTitles = mFragmentListSupporter.getListTitles();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.fragment_list_item,
                fragmentTitles.toArray(new String[fragmentTitles.size()]));

        int color = getActivity().getColor(R.color.formcolor);
        int[] colors = {0, color, 0}; // red for the example
        ListView listView = getListView();
        listView.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
        listView.setDividerHeight(2);

        setListAdapter(adapter);
    }

    @Override
    public void onAttach(Context context) {
        if (context instanceof IFragmentListSupporter) {
            mFragmentListSupporter = (IFragmentListSupporter) context;
        }
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        mFragmentListSupporter = null;
        super.onDetach();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        selectedItemName = (String) getListAdapter().getItem(position);
        int childCount = l.getChildCount();
        for (int i = 0; i < childCount; i++) {
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

        mFragmentListSupporter.onListItemSelected(selectedItemName);

    }
}