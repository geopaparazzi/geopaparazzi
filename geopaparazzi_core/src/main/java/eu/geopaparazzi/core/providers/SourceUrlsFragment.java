// ContactsFragment.java
// Fragment subclass that displays the alphabetical list of contact names
package eu.geopaparazzi.core.providers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import eu.geopaparazzi.core.R;

public class SourceUrlsFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // callback method implemented by MainActivity
    public interface SourceUrlsFragmentListener {
        // called when contact selected
        void onContactSelected(Uri contactUri);

        // called when add button is pressed
        void onAddContact();
    }

    private static final int SOURCEURLS_LOADER = 0; // identifies Loader

    // used to inform the MainActivity when a contact is selected
    private SourceUrlsFragmentListener listener;

    private SourceUrlsAdapter sourceUrlsAdapter; // adapter for recyclerView

    // configures this fragment's GUI
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true); // fragment has menu items to display

        // inflate GUI and get reference to the RecyclerView
        View view = inflater.inflate(
                R.layout.tmp_providertest_fragment_sourceurls, container, false);
        RecyclerView recyclerView =
                (RecyclerView) view.findViewById(R.id.recyclerView);

        // recyclerView should display items in a vertical list
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity().getBaseContext()));

        // create recyclerView's adapter and item click listener
        sourceUrlsAdapter = new SourceUrlsAdapter(
                new SourceUrlsAdapter.SourceUrlClickListener() {
                    @Override
                    public void onClick(Uri contactUri) {
                        listener.onContactSelected(contactUri);
                    }
                }
        );
        recyclerView.setAdapter(sourceUrlsAdapter); // set the adapter

        // attach a custom ItemDecorator to draw dividers between list items
        recyclerView.addItemDecoration(new ItemDivider(getContext()));

        // improves performance if RecyclerView's layout size never changes
        recyclerView.setHasFixedSize(true);

        // get the FloatingActionButton and configure its listener
        FloatingActionButton addButton =
                (FloatingActionButton) view.findViewById(R.id.addButton);
        addButton.setOnClickListener(
                new View.OnClickListener() {
                    // displays the AddEditFragment when FAB is touched
                    @Override
                    public void onClick(View view) {
                        listener.onAddContact();
                    }
                }
        );

        return view;
    }

    // set SourceUrlsFragmentListener when fragment attached
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (SourceUrlsFragmentListener) context;
    }

    // remove SourceUrlsFragmentListener when Fragment detached
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    // initialize a Loader when this fragment's activity is created
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(SOURCEURLS_LOADER, null, this);
    }

    // called from MainActivity when other Fragment's update database
    public void updateSourceUrlsList() {
        sourceUrlsAdapter.notifyDataSetChanged();
    }

    // called by LoaderManager to create a Loader
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // create an appropriate CursorLoader based on the id argument;
        // only one Loader in this fragment, so the switch is unnecessary
        switch (id) {
            case SOURCEURLS_LOADER:
                return new CursorLoader(getActivity(),
                        DatabaseDescription.SourceUrl.CONTENT_URI, // Uri of contacts table
                        null, // null projection returns all columns
                        null, // null selection returns all rows
                        null, // no selection arguments
                        DatabaseDescription.SourceUrl.COLUMN_NAME + " COLLATE NOCASE ASC"); // sort order
            default:
                return null;
        }
    }

    // called by LoaderManager when loading completes
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        sourceUrlsAdapter.swapCursor(data);
    }

    // called by LoaderManager when the Loader is being reset
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        sourceUrlsAdapter.swapCursor(null);
    }
}

