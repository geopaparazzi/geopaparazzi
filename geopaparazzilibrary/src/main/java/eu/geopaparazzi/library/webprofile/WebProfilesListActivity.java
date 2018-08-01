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
package eu.geopaparazzi.library.webprofile;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.core.dialogs.ProgressBarDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.profiles.objects.ProfileBasemaps;
import eu.geopaparazzi.library.profiles.objects.ProfileOtherfiles;
import eu.geopaparazzi.library.profiles.objects.ProfileSpatialitemaps;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_PROFILE_URL;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_PWD;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_USER;

/**
 * Web profiles listing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WebProfilesListActivity extends AppCompatActivity implements ProgressBarDialogFragment.IProgressChangeListener {
    private static final String ERROR = "error"; //$NON-NLS-1$

    private EditText filterText;
    private ListView mListView;

    private List<Profile> profileList = new ArrayList<>();
    private List<Profile> profileListToLoad = new ArrayList<>();

    private String user;
    private String pwd;
    private String url;


    private ProgressDialog downloadProfileListDialog;

    private SharedPreferences mPreferences;
    private HashMap<String, Profile> existingProfileMap = new HashMap<>();
    private ProgressBarDialogFragment progressBarDialogFragment;

    private Profile downloadProfile;


    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            List<Profile> existingProfileList = ProfilesHandler.INSTANCE.getProfilesFromPreferences(mPreferences);
            for (Profile p : existingProfileList) {
                existingProfileMap.put(p.name, p);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.webprofilelist);

        Bundle extras = getIntent().getExtras();
        user = extras.getString(PREFS_KEY_USER);
        pwd = extras.getString(PREFS_KEY_PWD);
        url = extras.getString(PREFS_KEY_PROFILE_URL);

        filterText = findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);

        mListView = findViewById(R.id.list);

        downloadProfileListDialog = ProgressDialog.show(this, getString(R.string.downloading),
                getString(R.string.downloading_profiles_list_from_server), true, false);
        new AsyncTask<String, Void, String>() {

            protected String doInBackground(String... params) {
                WebProfilesListActivity context = WebProfilesListActivity.this;
                try {
                    profileList = WebProfileManager.INSTANCE.downloadProfileList(context, url, user, pwd);
                    for (Profile wp : profileList) {
                        profileListToLoad.add(wp);
                    }
                    return ""; //$NON-NLS-1$
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                    return ERROR;
                }
            }

            protected void onPostExecute(String response) { // on UI thread!
                GPDialogs.dismissProgressDialog(downloadProfileListDialog);
                WebProfilesListActivity context = WebProfilesListActivity.this;
                if (response.equals(ERROR)) {
                    GPDialogs.warningDialog(context, getString(R.string.error_profiles_list), null);
                } else {
                    refreshList();
                }
            }

        }.execute((String) null);

    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public void onPause() {
        GPDialogs.dismissProgressDialog(downloadProfileListDialog);
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        filterText.removeTextChangedListener(filterTextWatcher);
    }

    private void filterList(String filterText) {
        if (GPLog.LOG)
            GPLog.addLogEntry(this, "filter profiles list"); //$NON-NLS-1$

        profileListToLoad.clear();
        for (Profile profile : profileList) {
            if (profile.matches(filterText)) {
                profileListToLoad.add(profile);
            }
        }

        refreshList();
    }


    private void refreshList() {
        if (GPLog.LOG)
            GPLog.addLogEntry(this, "refreshing profiles list"); //$NON-NLS-1$
        ArrayAdapter<Profile> arrayAdapter = new ArrayAdapter<Profile>(this, R.layout.webprofilesrow, profileListToLoad) {
            @Override
            public View getView(int position, View cView, ViewGroup parent) {

                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.webprofilesrow, null);
                final Profile selectedWebprofile = profileListToLoad.get(position);

                TextView nameText = rowView.findViewById(R.id.nametext);
                TextView descriptionText = rowView.findViewById(R.id.descriptiontext);
                TextView dateText = rowView.findViewById(R.id.datetext);
                TextView commentsText = rowView.findViewById(R.id.comments);

                String wpName = selectedWebprofile.name;
                nameText.setText(wpName);
                descriptionText.setText(selectedWebprofile.description);
                dateText.setText(selectedWebprofile.creationdate);

                Profile profile = existingProfileMap.get(wpName);
                boolean ignore = false;
                if (profile == null) {
                    // a new profile
                    commentsText.setText("");
                } else {
                    String pName = profile.name;
                    String pModifieddate = profile.modifieddate;
                    String wpModifieddate = selectedWebprofile.modifieddate;
                    if (pName.equals(wpName) && pModifieddate.equals(wpModifieddate)) {
                        // and existing profile to be ignored
                        commentsText.setText(R.string.profile_status_exists);
                        ignore = true;
                    } else if (pName.equals(wpName)) {
                        // a new profile is available
                        commentsText.setText(R.string.profile_status_update);
                        ignore = true;
                    }
                }

                if (!ignore) {
                    ImageView imageText = rowView.findViewById(R.id.downloadprofile_image);
                    imageText.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            downloadProfile(selectedWebprofile);
                        }
                    });
                    TextView titleText = rowView.findViewById(R.id.nametext);
                    titleText.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            downloadProfile(selectedWebprofile);
                        }
                    });
                }
                return rowView;
            }

        };

        mListView.setAdapter(arrayAdapter);
    }

    private TextWatcher filterTextWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
            // ignore
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // ignore
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // arrayAdapter.getFilter().filter(s);
            filterList(s.toString());
        }
    };

    private void downloadProfile(Profile selectedWebprofile) {
        List<Parcelable> downloadables = new ArrayList<>();

        if (selectedWebprofile.profileTags != null) {
            selectedWebprofile.profileTags.setDestinationPath(selectedWebprofile.getFile(selectedWebprofile.profileTags.getRelativePath()).getAbsolutePath());
            downloadables.add(selectedWebprofile.profileTags);
        }

        if (selectedWebprofile.profileProject != null) {
            selectedWebprofile.profileProject.setDestinationPath(selectedWebprofile.getFile(selectedWebprofile.profileProject.getRelativePath()).getAbsolutePath());
            downloadables.add(selectedWebprofile.profileProject);
        }

        if (selectedWebprofile.basemapsList != null) {
            for (ProfileBasemaps item : selectedWebprofile.basemapsList) {
                item.setDestinationPath(selectedWebprofile.getFile(item.getRelativePath()).getAbsolutePath());
            }
            downloadables.addAll(selectedWebprofile.basemapsList);
        }

        if (selectedWebprofile.otherFilesList != null) {
            for (ProfileOtherfiles item : selectedWebprofile.otherFilesList) {
                item.setDestinationPath(selectedWebprofile.getFile(item.getRelativePath()).getAbsolutePath());
            }
            downloadables.addAll(selectedWebprofile.otherFilesList);
        }

        if (selectedWebprofile.spatialiteList != null) {
            for (ProfileSpatialitemaps item : selectedWebprofile.spatialiteList) {
                item.setDestinationPath(selectedWebprofile.getFile(item.getRelativePath()).getAbsolutePath());
            }
            downloadables.addAll(selectedWebprofile.spatialiteList);
        }


        downloadProfile = selectedWebprofile;
        progressBarDialogFragment = ProgressBarDialogFragment.newInstance(downloadables.toArray(new Parcelable[downloadables.size()]));
        progressBarDialogFragment.setCancelable(true);
        progressBarDialogFragment.show(getSupportFragmentManager(), "Download files");

    }

    @Override
    public void onProgressError(String errorMsg) {
        if (progressBarDialogFragment != null)
            progressBarDialogFragment.dismiss();
        GPDialogs.warningDialog(this, errorMsg, null);
    }

    @Override
    public void onProgressDone(String msg) {
        if (progressBarDialogFragment != null)
            progressBarDialogFragment.dismiss();
        if (downloadProfile != null) {
            saveWebProfile(downloadProfile);
            downloadProfile = null;
        }
        if (msg != null && msg.trim().length() > 0)
            GPDialogs.infoDialog(this, msg, null);


    }

    private void saveWebProfile(Profile profile) {
        try {
            List<Profile> profileList = ProfilesHandler.INSTANCE.getProfilesFromPreferences(mPreferences);
            for (Profile p : profileList) {
                p.active = false;
            }
            profile.active = false; // TODO now we can't activate from here, since we are not able to restart
            profileList.add(profile);
            ProfilesHandler.INSTANCE.saveProfilesToPreferences(mPreferences, profileList);

            Intent intent = new Intent((String) null);
            intent.putExtra(LibraryConstants.PREFS_KEY_RESTART_APPLICATION, true);
            setResult(Activity.RESULT_OK, intent);
        } catch (JSONException e) {
            Log.e("GEOS2GO", "Error saving profiles", e);
        }
    }
}
