// AddEditFragment.java
// Fragment for adding a new contact or editing an existing one
package eu.geopaparazzi.core.providers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import eu.geopaparazzi.core.R;

public class AddEditFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // defines callback method implemented by MainActivity
    public interface AddEditFragmentListener {
        // called when contact is saved
        void onAddEditCompleted(Uri contactUri);
    }

    // constant used to identify the Loader
    private static final int SOURCEURLS_LOADER = 0;

    private AddEditFragmentListener listener; // MainActivity
    private Uri sourceUrlUri; // Uri of selected source url
    private boolean addingNewSourceurl = true; // adding (true) or editing

    // EditTexts for contact information
    private TextInputLayout nameTextInputLayout;
    private TextInputLayout sourceurlTextInputLayout;
    private FloatingActionButton saveSourceurlFAB;

    private CoordinatorLayout coordinatorLayout; // used with SnackBars

    // set AddEditFragmentListener when Fragment attached
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (AddEditFragmentListener) context;
    }

    // remove AddEditFragmentListener when Fragment detached
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    // called when Fragment's view needs to be created
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true); // fragment has menu items to display

        // inflate GUI and get references to EditTexts
        View view =
                inflater.inflate(R.layout.tmp_providertest_fragment_addedit, container, false);
        nameTextInputLayout =
                (TextInputLayout) view.findViewById(R.id.nameTextInputLayout);
        nameTextInputLayout.getEditText().addTextChangedListener(
                nameChangedListener);
        sourceurlTextInputLayout =
                (TextInputLayout) view.findViewById(R.id.sourceurlTextInputLayout);

        // set FloatingActionButton's event listener
        saveSourceurlFAB = (FloatingActionButton) view.findViewById(
                R.id.saveFloatingActionButton);
        saveSourceurlFAB.setOnClickListener(saveContactButtonClicked);
        updateSaveButtonFAB();

        // used to display SnackBars with brief messages
        coordinatorLayout = (CoordinatorLayout) getActivity().findViewById(
                R.id.coordinatorLayout);

        Bundle arguments = getArguments(); // null if creating new contact

        if (arguments != null) {
            addingNewSourceurl = false;
            sourceUrlUri = arguments.getParcelable(ProviderTestActivity.SOURCEURL_URI);
        }

        // if editing an existing contact, create Loader to get the contact
        if (sourceUrlUri != null)
            getLoaderManager().initLoader(SOURCEURLS_LOADER, null, this);

        return view;
    }

    // detects when the text in the nameTextInputLayout's EditText changes
    // to hide or show saveButtonFAB
    private final TextWatcher nameChangedListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        // called when the text in nameTextInputLayout changes
        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            updateSaveButtonFAB();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    // shows saveButtonFAB only if the name is not empty
    private void updateSaveButtonFAB() {
        String input =
                nameTextInputLayout.getEditText().getText().toString();

        // if there is a name for the contact, show the FloatingActionButton
        if (input.trim().length() != 0)
            saveSourceurlFAB.show();
        else
            saveSourceurlFAB.hide();
    }

    // responds to event generated when user saves a contact
    private final View.OnClickListener saveContactButtonClicked =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // hide the virtual keyboard
                    ((InputMethodManager) getActivity().getSystemService(
                            Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                            getView().getWindowToken(), 0);
                    saveContact(); // save contact to the database
                }
            };

    // saves contact information to the database
    private void saveContact() {
        // create ContentValues object containing contact's key-value pairs
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseDescription.SourceUrl.COLUMN_NAME,
                nameTextInputLayout.getEditText().getText().toString());
        contentValues.put(DatabaseDescription.SourceUrl.COLUMN_URL,
                sourceurlTextInputLayout.getEditText().getText().toString());

        if (addingNewSourceurl) {
            // use Activity's ContentResolver to invoke
            // insert on the AddressBookContentProvider
            Uri newContactUri = getActivity().getContentResolver().insert(
                    DatabaseDescription.SourceUrl.CONTENT_URI, contentValues);

            if (newContactUri != null) {
                Snackbar.make(coordinatorLayout,
                        "Source url added", Snackbar.LENGTH_LONG).show();
                listener.onAddEditCompleted(newContactUri);
            } else {
                Snackbar.make(coordinatorLayout,
                        "Source url not added", Snackbar.LENGTH_LONG).show();
            }
        } else {
            // use Activity's ContentResolver to invoke
            // insert on the AddressBookContentProvider
            int updatedRows = getActivity().getContentResolver().update(
                    sourceUrlUri, contentValues, null, null);

            if (updatedRows > 0) {
                listener.onAddEditCompleted(sourceUrlUri);
                Snackbar.make(coordinatorLayout,
                        "Source url updated", Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(coordinatorLayout,
                        "Source url not updated", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    // called by LoaderManager to create a Loader
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // create an appropriate CursorLoader based on the id argument;
        // only one Loader in this fragment, so the switch is unnecessary
        switch (id) {
            case SOURCEURLS_LOADER:
                return new CursorLoader(getActivity(),
                        sourceUrlUri, // Uri of contact to display
                        null, // null projection returns all columns
                        null, // null selection returns all rows
                        null, // no selection arguments
                        null); // sort order
            default:
                return null;
        }
    }

    // called by LoaderManager when loading completes
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // if the contact exists in the database, display its data
        if (data != null && data.moveToFirst()) {
            // get the column index for each data item
            int nameIndex = data.getColumnIndex(DatabaseDescription.SourceUrl.COLUMN_NAME);
            int sourceurlIndex = data.getColumnIndex(DatabaseDescription.SourceUrl.COLUMN_URL);

            // fill EditTexts with the retrieved data
            nameTextInputLayout.getEditText().setText(
                    data.getString(nameIndex));
            sourceurlTextInputLayout.getEditText().setText(
                    data.getString(sourceurlIndex));

            updateSaveButtonFAB();
        }
    }

    // called by LoaderManager when the Loader is being reset
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
