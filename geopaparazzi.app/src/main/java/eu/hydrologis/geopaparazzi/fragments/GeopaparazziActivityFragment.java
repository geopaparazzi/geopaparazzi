// MainActivityFragment.java
// Contains the Flag Quiz logic
package eu.hydrologis.geopaparazzi.fragments;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.SettingsActivity;
import eu.hydrologis.geopaparazzi.providers.ProviderTestActivity;

public class GeopaparazziActivityFragment extends Fragment implements View.OnLongClickListener, View.OnClickListener {

    private ImageButton notesButton;
    private ImageButton metadataButton;
    private ImageButton mapviewButton;
    private MenuItem gpsMenuItem;
    private ImageButton gpslogButton;
    private ImageButton importButton;

    // configures the GeopaparazziActivityFragment when its View is created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_geopaparazzi, container, false);

        setHasOptionsMenu(true);

        return v; // return the fragment's view for display
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notesButton = (ImageButton) view.findViewById(R.id.dashboardButtonNotes);
        notesButton.setOnLongClickListener(this);

        metadataButton = (ImageButton) view.findViewById(R.id.dashboardButtonMetadata);
        metadataButton.setOnClickListener(this);

        mapviewButton = (ImageButton) view.findViewById(R.id.dashboardButtonMapview);
        mapviewButton.setOnClickListener(this);

        gpslogButton = (ImageButton) view.findViewById(R.id.dashboardButtonGpslog);
        gpslogButton.setOnClickListener(this);

        importButton = (ImageButton) view.findViewById(R.id.dashboardButtonImport);
        importButton.setOnClickListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);

        gpsMenuItem = menu.getItem(3);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this.getActivity(), SettingsActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onLongClick(View v) {
        String tooltip = "blah";
        if (v == notesButton) {
            tooltip = "Available providers:";
            for (PackageInfo pack : getActivity().getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        Log.d("Example", "provider: " + provider.authority);
                        tooltip = tooltip + "\n" + provider.authority;
                    }
                }
            }


        }

        Snackbar.make(v, tooltip, Snackbar.LENGTH_LONG).show();

        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == metadataButton) {
            LineWidthDialogFragment widthDialog =
                    new LineWidthDialogFragment();
            widthDialog.show(getFragmentManager(), "line width dialog");
        } else if (v == mapviewButton) {
            ColorDialogFragment colorDialog = new ColorDialogFragment();
            colorDialog.show(getFragmentManager(), "color dialog");
        } else if (v == gpslogButton) {
            gpsMenuItem.setIcon(R.drawable.actionbar_gps_nofix);
        } else if (v == importButton) {
            Intent providerIntent= new Intent(getActivity(), ProviderTestActivity.class);
            startActivity(providerIntent);
        }

    }
}
