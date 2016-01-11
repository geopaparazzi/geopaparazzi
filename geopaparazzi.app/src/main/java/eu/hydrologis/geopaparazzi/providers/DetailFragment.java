// DetailFragment.java
// Fragment subclass that displays one contact's details
package eu.hydrologis.geopaparazzi.providers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import eu.hydrologis.geopaparazzi.R;

public class DetailFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // callback methods implemented by MainActivity
    public interface DetailFragmentListener {
        void onSourceUrlDeleted(); // called when a contact is deleted

        // pass Uri of contact to edit to the DetailFragmentListener
        void onEditSourceUrl(Uri contactUri);
    }

    private static final int SOURCEURLS_LOADER = 0; // identifies the Loader

    private DetailFragmentListener listener; // MainActivity
    private Uri sourceurlUri; // Uri of selected contact

    private TextView nameTextView; // displays contact's name
    private TextView urlTextView; // displays contact's phone

    // set DetailFragmentListener when fragment attached
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (DetailFragmentListener) context;
    }

    // remove DetailFragmentListener when fragment detached
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    // called when DetailFragmentListener's view needs to be created
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
//      setHasOptionsMenu(true); // this fragment has menu items to display

        // get Bundle of arguments then extract the contact's Uri
        Bundle arguments = getArguments();

        if (arguments != null)
            sourceurlUri = arguments.getParcelable(ProviderTestActivity.SOURCEURL_URI);

        // inflate DetailFragment's layout
        View view =
                inflater.inflate(R.layout.tmp_providertest_fragment_detail, container, false);

        // get the EditTexts
        nameTextView = (TextView) view.findViewById(R.id.nameTextView);
        urlTextView = (TextView) view.findViewById(R.id.urlTextView);

        // load the contact
        getLoaderManager().initLoader(SOURCEURLS_LOADER, null, this);
        return view;
    }


    // called by LoaderManager to create a Loader
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // create an appropriate CursorLoader based on the id argument;
        // only one Loader in this fragment, so the switch is unnecessary
        CursorLoader cursorLoader;

        switch (id) {
            case SOURCEURLS_LOADER:
                cursorLoader = new CursorLoader(getActivity(),
                        sourceurlUri, // Uri of contact to display
                        null, // null projection returns all columns
                        null, // null selection returns all rows
                        null, // no selection arguments
                        null); // sort order
                break;
            default:
                cursorLoader = null;
                break;
        }

        return cursorLoader;
    }

    // called by LoaderManager when loading completes
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // if the contact exists in the database, display its data
        if (data != null && data.moveToFirst()) {
            // get the column index for each data item
            int nameIndex = data.getColumnIndex(DatabaseDescription.SourceUrl.COLUMN_NAME);
            int urlIndex = data.getColumnIndex(DatabaseDescription.SourceUrl.COLUMN_URL);

            // fill TextViews with the retrieved data
            nameTextView.setText(data.getString(nameIndex));
            urlTextView.setText(data.getString(urlIndex));
        }
    }

    // called by LoaderManager when the Loader is being reset
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}


