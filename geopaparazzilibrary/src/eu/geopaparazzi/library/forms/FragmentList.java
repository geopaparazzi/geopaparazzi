package eu.geopaparazzi.library.forms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.LibraryConstants;

public class FragmentList extends android.support.v4.app.ListFragment {
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        FormActivity activity = (FormActivity) getActivity();
        String theString = activity.getTheString();
        System.out.println();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated(savedInstanceState);
        String[] values = new String[]{"Android", "iPhone", "WindowsMobile", "Blackberry", "WebOS", "Ubuntu", "Windows7",
                "Max OS X", "Linux", "OS/2"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, values);
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick( ListView l, View v, int position, long id ) {
        String item = (String) getListAdapter().getItem(position);
        FragmentDetail fragment = (FragmentDetail) getFragmentManager().findFragmentById(R.id.detailFragment);
        if (fragment != null && fragment.isInLayout()) {
            fragment.setText(item);
        } else {
            Intent intent = new Intent(getActivity().getApplicationContext(), FragmentDetailActivity.class);
            intent.putExtra("value", item);
            startActivity(intent);

        }

    }
}