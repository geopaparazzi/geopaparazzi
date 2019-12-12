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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.ListFragment;

import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Compat;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FormListFragment extends ListFragment {

    private IFragmentListSupporter mFragmentListSupporter;

    /**
     * Interface to keep track of the selected view.
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

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_activated_1,
                fragmentTitles.toArray(new String[fragmentTitles.size()]));

        int color = Compat.getColor(getActivity(), R.color.formcolor);
        int[] colors = {0, color, 0};
        ListView listView = getListView();
        listView.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, colors));
        listView.setDividerHeight(2);

        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setFocusableInTouchMode(true);
        StateListDrawable selector = new StateListDrawable();
        selector.addState(new int[]{-android.R.attr.state_pressed}, new ColorDrawable(Compat.getColor(getContext(), R.color.main_selection)));
        listView.setSelector(selector);

        setListAdapter(adapter);

        if (fragmentTitles.size() > 0)
            listView.setItemChecked(0, true);
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
        String selectedItemName = (String) getListAdapter().getItem(position);
        mFragmentListSupporter.onListItemSelected(selectedItemName);
    }
}