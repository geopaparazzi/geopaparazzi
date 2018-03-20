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

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_USER;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_PWD;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_PROFILE_URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * Web profiles listing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WebProfilesListActivity extends ListActivity {
    private static final String ERROR = "error"; //$NON-NLS-1$

    private ArrayAdapter<Profile> arrayAdapter;
    private EditText filterText;

    private List<Profile> profileList = new ArrayList<>();
    private List<Profile> profileListToLoad = new ArrayList<>();

    private String user;
    private String pwd;
    private String url;

    private ProgressDialog downloadProfileListDialog;
    private ProgressDialog cloudProgressDialog;

    private SharedPreferences mPreferences;
    private HashMap<String, Profile> existingProfileMap = new HashMap<>();


    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

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
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    protected void onPause() {
        GPDialogs.dismissProgressDialog(downloadProfileListDialog);
        GPDialogs.dismissProgressDialog(cloudProgressDialog);
        super.onPause();
    }

    protected void onDestroy() {
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

    private void saveWebProfile(Profile profile) {
        try {
            List<Profile> profileList = ProfilesHandler.INSTANCE.getProfilesFromPreferences(mPreferences);
            profileList.add(profile);
            ProfilesHandler.INSTANCE.saveProfilesToPreferences(mPreferences, profileList);

            Intent intent = new Intent((String) null);
            intent.putExtra(LibraryConstants.PREFS_KEY_RESTART_APPLICATION, true);
            setResult(Activity.RESULT_OK, intent);
        } catch (JSONException e) {
            Log.e("GEOS2GO", "Error saving profiles", e);
        }
    }

    private void refreshList() {
        if (GPLog.LOG)
            GPLog.addLogEntry(this, "refreshing profiles list"); //$NON-NLS-1$
        arrayAdapter = new ArrayAdapter<Profile>(this, R.layout.webprofilesrow, profileListToLoad) {
            @Override
            public View getView(int position, View cView, ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.webprofilesrow, null);
                final Profile webprofile = profileListToLoad.get(position);

                TextView nameText = rowView.findViewById(R.id.nametext);
                TextView descriptionText = rowView.findViewById(R.id.descriptiontext);
                TextView dateText = rowView.findViewById(R.id.datetext);
                TextView commentsText = rowView.findViewById(R.id.comments);

                String wpName = webprofile.name;
                nameText.setText(wpName);
                descriptionText.setText(webprofile.description);
                dateText.setText(webprofile.creationdate);

                Profile profile = existingProfileMap.get(wpName);
                boolean ignore = false;
                if (profile == null) {
                    // a new profile
                    commentsText.setText("");
                } else {
                    String pName = profile.name;
                    String pModifieddate = profile.modifieddate;
                    String wpModifieddate = webprofile.modifieddate;
                    if (pName.equals(wpName) && pModifieddate.equals(wpModifieddate)) {
                        // and existing profile to be ignored
                        commentsText.setText("IGNORED: the same profile exists already.");
                        ignore = true;
                    } else if (pName.equals(wpName)) {
                        // a new profile is available
                        commentsText.setText("UPDATES: will update existing.");
                        ignore = true;
                    }
                }

                if (!ignore) {
                    ImageView imageText = rowView.findViewById(R.id.downloadprofile_image);
                    imageText.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            downloadProfile(webprofile);
                        }
                    });
                    TextView titleText = rowView.findViewById(R.id.nametext);
                    titleText.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            downloadProfile(webprofile);
                        }
                    });
                }
                return rowView;
            }

        };

        setListAdapter(arrayAdapter);
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

    private void downloadProfile(final Profile webprofile) {
        cloudProgressDialog = ProgressDialog.show(this, getString(R.string.downloading),
                getString(R.string.downloading_profile), true, false);
        new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                try {
                    String returnCode = WebProfileManager.INSTANCE.downloadProfileContent(WebProfilesListActivity.this, url, user, pwd,
                            webprofile);

                    if (returnCode.equals(getString(R.string.profile_successfully_downloaded)))
                        saveWebProfile(webprofile);
                    return returnCode;
                } catch (Exception e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    return e.getMessage();
                }
            }

            protected void onPostExecute(String response) { // on UI thread!
                GPDialogs.dismissProgressDialog(cloudProgressDialog);
                String okMsg = getString(R.string.profile_successfully_downloaded);
                if (response.equals(okMsg)) {
                    GPDialogs.infoDialog(WebProfilesListActivity.this, okMsg, null);
                } else {
                    GPDialogs.warningDialog(WebProfilesListActivity.this, response, null);
                }

            }
        }.execute((String) null);
    }
}
