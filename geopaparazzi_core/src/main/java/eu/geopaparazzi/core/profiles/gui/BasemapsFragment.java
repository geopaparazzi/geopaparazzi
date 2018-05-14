package eu.geopaparazzi.core.profiles.gui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.activities.DirectoryBrowserActivity;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.objects.ProfileBasemaps;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import gov.nasa.worldwind.AddWMSDialog;

public class BasemapsFragment extends Fragment {
    private static final String ARG_PROFILE = "profile";
    public static final int RETURNCODE_BROWSE = 666;


    private List<ProfileBasemaps> mBasemapsList = new ArrayList<>();
    private ListView listView;

    private String[] supportedExtensions = {"mapurl", "map", "sqlite", "mbtiles"};

    public BasemapsFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static BasemapsFragment newInstance(Profile profile) {
        BasemapsFragment fragment = new BasemapsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PROFILE, profile);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_profile_settings_basemaps, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_addwms) {
            Context context = getActivity();
            if (!NetworkUtilities.isNetworkAvailable(context)) {
                GPDialogs.infoDialog(context, context.getString(R.string.available_only_with_network), null);
            } else {
                AddWMSDialog addWMSDialog = AddWMSDialog.newInstance(null);
                addWMSDialog.show(getFragmentManager(), "wms import");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profilesettings_basemaps, container, false);

        listView = rootView.findViewById(R.id.basemapsList);

        FloatingActionButton addFormButton = rootView.findViewById(R.id.addFormsjsonButton);
        addFormButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent browseIntent = new Intent(getContext(), DirectoryBrowserActivity.class);
                    browseIntent.putExtra(DirectoryBrowserActivity.EXTENSIONS, supportedExtensions);

                    String lastPath = Utilities.getLastFilePath(getContext());
                    browseIntent.putExtra(DirectoryBrowserActivity.STARTFOLDERPATH, lastPath);
                    startActivityForResult(browseIntent, RETURNCODE_BROWSE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        ProfileSettingsActivity activity = (ProfileSettingsActivity) getActivity();
        Profile profile = activity.getSelectedProfile();
        mBasemapsList.clear();
        mBasemapsList.addAll(profile.basemapsList);
        refreshList();
    }

    private void refreshList() {
        final ProfileSettingsActivity activity = (ProfileSettingsActivity) getActivity();
        final Profile profile = activity.getSelectedProfile();
        ArrayAdapter<ProfileBasemaps> mArrayAdapter = new ArrayAdapter<ProfileBasemaps>(activity, R.layout.fragment_profilesettings_basemaps_row, mBasemapsList) {
            class ViewHolder {
                TextView nameView;
                TextView pathView;
            }

            @Override
            public View getView(int position, View rowView, ViewGroup parent) {

                ViewHolder holder;
                // Recycle existing view if passed as parameter
                if (rowView == null) {
                    LayoutInflater inflater = activity.getLayoutInflater();
                    rowView = inflater.inflate(R.layout.fragment_profilesettings_basemaps_row, parent, false);
                    holder = new ViewHolder();
                    holder.nameView = rowView.findViewById(R.id.basemapName);
                    holder.pathView = rowView.findViewById(R.id.basemapPath);

                    rowView.setTag(holder);
                } else {
                    holder = (ViewHolder) rowView.getTag();
                }

                final ProfileBasemaps currentBasemap = mBasemapsList.get(position);
                File basemapFile = profile.getFile(currentBasemap.getRelativePath());

                holder.nameView.setText(basemapFile.getName());
                holder.pathView.setText(basemapFile.getAbsolutePath());

                return rowView;
            }

        };


        listView.setAdapter(mArrayAdapter);
        listView.setClickable(true);
        listView.setLongClickable(true);
        listView.setFocusable(true);
        listView.setFocusableInTouchMode(true);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final ProfileBasemaps baseMap = mBasemapsList.get(position);
                File file = profile.getFile(baseMap.getRelativePath());
                GPDialogs.yesNoMessageDialog(getActivity(), "Do you want to remove: " + file.getName() + "?", new Runnable() {
                    @Override
                    public void run() {
                        mBasemapsList.remove(position);
                        ProfileSettingsActivity activity = (ProfileSettingsActivity) getActivity();
                        if (activity != null) {
                            activity.onBasemapRemoved(baseMap.getRelativePath());
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshList();
                                }
                            });
                        }

                    }
                }, null);

                return true;
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (RETURNCODE_BROWSE): {
                if (resultCode == Activity.RESULT_OK) {
                    String path = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                    if (path != null && new File(path).exists()) {
                        try {
                            Utilities.setLastFilePath(getContext(), path);
                        } catch (Exception e) {
                            GPLog.error(this, null, e);
                        }
                        ProfileSettingsActivity activity = (ProfileSettingsActivity) getActivity();
                        if (activity != null) {
                            final Profile profile = activity.getSelectedProfile();
                            String sdcardPath = profile.getSdcardPath();

                            if (!path.contains(sdcardPath)) {
                                GPDialogs.warningDialog(getActivity(), "All data of the same profile have to reside in the same root path.", null);
                                return;
                            }

                            String relativePath = path.replaceFirst(sdcardPath, "");
                            boolean hasIt = false;
                            for (ProfileBasemaps bm : mBasemapsList) {
                                if (bm.getRelativePath().equals(relativePath)) {
                                    hasIt = true;
                                    break;
                                }
                            }

                            if (!hasIt) {
                                ProfileBasemaps basemaps = new ProfileBasemaps();
                                basemaps.setRelativePath(relativePath);
                                mBasemapsList.add(basemaps);
                                activity.onBasemapAdded(relativePath);
                                refreshList();
                            }
                        }
                    }
                }
            }
        }
    }
}