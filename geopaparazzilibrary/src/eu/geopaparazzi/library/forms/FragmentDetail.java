package eu.geopaparazzi.library.forms;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import eu.geopaparazzi.library.R;

public class FragmentDetail extends Fragment {
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
        View view = inflater.inflate(R.layout.details, container, false);
        return view;
    }

    public void setText( String item ) {
        // TextView view = (TextView) getView().findViewById(R.id.detailsText);
        // view.setText(item);
    }
}