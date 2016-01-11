package eu.hydrologis.geopaparazzi.providers;

import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import eu.hydrologis.geopaparazzi.R;

public class ProviderTestActivity extends AppCompatActivity implements
        SourceUrlsFragment.SourceUrlsFragmentListener,
        AddEditFragment.AddEditFragmentListener,
        DetailFragment.DetailFragmentListener {

    private SourceUrlsFragment sourceUrlsFragment;

    // key for storing a contact's Uri in a Bundle passed to a fragment
    public static final String SOURCEURL_URI = "sourceurl_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tmp_providertestactivity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        sourceUrlsFragment = new SourceUrlsFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragmentContainer, sourceUrlsFragment);
        transaction.commit();

    }

    @Override
    public void onContactSelected(Uri contactUri) {
        displayContact(contactUri, R.id.fragmentContainer);
    }

    // display a contact
    private void displayContact(Uri contactUri, int viewID) {
        DetailFragment detailFragment = new DetailFragment();

        // specify contact's Uri as an argument to the DetailFragment
        Bundle arguments = new Bundle();
        arguments.putParcelable(SOURCEURL_URI, contactUri);
        detailFragment.setArguments(arguments);

        // use a FragmentTransaction to display the DetailFragment
        FragmentTransaction transaction =
                getSupportFragmentManager().beginTransaction();
        transaction.replace(viewID, detailFragment);
        transaction.addToBackStack(null);
        transaction.commit(); // causes DetailFragment to display
    }

    @Override
    public void onAddContact() {
        displayAddEditFragment(R.id.fragmentContainer, null);
    }

    // display fragment for adding a new or editing an existing contact
    private void displayAddEditFragment(int viewID, Uri contactUri) {
        AddEditFragment addEditFragment = new AddEditFragment();

        // if editing existing contact, provide contactUri as an argument
        if (contactUri != null) {
            Bundle arguments = new Bundle();
            arguments.putParcelable(SOURCEURL_URI, contactUri);
            addEditFragment.setArguments(arguments);
        }

        // use a FragmentTransaction to display the AddEditFragment
        FragmentTransaction transaction =
                getSupportFragmentManager().beginTransaction();
        transaction.replace(viewID, addEditFragment);
        transaction.addToBackStack(null);
        transaction.commit(); // causes AddEditFragment to display
    }

    // update GUI after new contact or updated contact saved
    @Override
    public void onAddEditCompleted(Uri contactUri) {
        // removes top of back stack
        getSupportFragmentManager().popBackStack();
        sourceUrlsFragment.updateSourceUrlsList(); // refresh contacts
    }

    @Override
    public void onSourceUrlDeleted() {

    }

    @Override
    public void onEditSourceUrl(Uri contactUri) {

    }
}
