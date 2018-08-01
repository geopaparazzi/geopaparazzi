/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2018  HydroloGIS (www.hydrologis.com)
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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.core.dialogs.ProgressBarUploadDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.profiles.objects.ProfileSpatialitemaps;
import eu.geopaparazzi.library.util.GPDialogs;

/**
 * Web profiles listing activity.
 *
 * @author Brent Fraser (www.geoanalytic.com)
 */
public class WebProfilesUploadListActivity extends AppCompatActivity implements ProgressBarUploadDialogFragment.IProgressChangeListener {

    private EditText filterText;
    private ListView mListView;
    private List<Profile> existingProfileList = new ArrayList<>();
    private ProgressDialog uploadProfileListDialog;
    private SharedPreferences mPreferences;
    private ProgressBarUploadDialogFragment progressBarUploadDialogFragment;
    private Profile uploadProfile;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            existingProfileList = ProfilesHandler.INSTANCE.getProfilesFromPreferences(mPreferences, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.webprofilelist);
        filterText = findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);

        mListView = findViewById(R.id.list);

        refreshList();

    }
    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public void onPause() {
        GPDialogs.dismissProgressDialog(uploadProfileListDialog);
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        filterText.removeTextChangedListener(filterTextWatcher);
    }

    private void filterList(String filterText) {
        if (GPLog.LOG)
            GPLog.addLogEntry(this, "filter profiles list"); //$NON-NLS-1$
/*
        profileListToLoad.clear();
        for (Profile profile : profileList) {
            if (profile.matches(filterText)) {
                profileListToLoad.add(profile);
            }
        }
*/
        refreshList();
    }

    private void refreshList() {
        if (GPLog.LOG)
            GPLog.addLogEntry(this, "refreshing profiles list"); //$NON-NLS-1$
        ArrayAdapter<Profile> arrayAdapter = new ArrayAdapter<Profile>(this, R.layout.webprofilesrow, existingProfileList) {
            @Override
            public View getView(int position, View cView, ViewGroup parent) {

                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.webprofilesuploadrow, null);
                final Profile selectedWebprofile = existingProfileList.get(position);

                TextView nameText = rowView.findViewById(R.id.nametext);
                TextView descriptionText = rowView.findViewById(R.id.descriptiontext);
                TextView dateText = rowView.findViewById(R.id.datetext);
                TextView commentsText = rowView.findViewById(R.id.comments);

                String wpName = selectedWebprofile.name;
                nameText.setText(wpName);
                descriptionText.setText(selectedWebprofile.description);
                dateText.setText(selectedWebprofile.creationdate);

                Profile profile = selectedWebprofile;
                boolean ignore = false;
                if (profile == null) {
                    // a new profile
                    commentsText.setText("");
                } else {
                    String pName = profile.name;
                    String pModifieddate = profile.modifieddate;
                    String wpModifieddate = selectedWebprofile.modifieddate;
                    commentsText.setText("");

                    if (pName.equals(wpName) && pModifieddate.equals(wpModifieddate)) {
                        // and existing profile to be ignored
//                        commentsText.setText(R.string.profile_status_exists);
                        ignore = true;
                    } else if (pName.equals(wpName)) {
                        // a new profile is available
//                        commentsText.setText(R.string.profile_status_update);
                        ignore = true;
                    }

                }
                ignore = false; //bwf ToDo
                if (!ignore) {
                    ImageView imageText = rowView.findViewById(R.id.cloud_image);
                    imageText.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            uploadProfile(selectedWebprofile);
                        }
                    });
                    TextView titleText = rowView.findViewById(R.id.nametext);
                    titleText.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            uploadProfile(selectedWebprofile);
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
//            filterList(s.toString());
        }
    };

    private void uploadProfile(Profile selectedLocalprofile) {
        List<Parcelable> uploadables = new ArrayList<>();

        if (selectedLocalprofile.profileProject != null) {
            if (selectedLocalprofile.profileProject.getUploadUrl() != null) {
                selectedLocalprofile.profileProject.setDestinationPath(selectedLocalprofile.getFile(selectedLocalprofile.profileProject.getRelativePath()).getAbsolutePath());
                uploadables.add(selectedLocalprofile.profileProject);
            }
        }

        if (selectedLocalprofile.spatialiteList != null) {
            for (ProfileSpatialitemaps item : selectedLocalprofile.spatialiteList) {
                item.setDestinationPath(selectedLocalprofile.getFile(item.getRelativePath()).getAbsolutePath());
            }
        }
        uploadables.addAll(selectedLocalprofile.spatialiteList);
        uploadProfile = selectedLocalprofile;

        progressBarUploadDialogFragment = ProgressBarUploadDialogFragment.newInstance(uploadables.toArray(new Parcelable[uploadables.size()]));
        progressBarUploadDialogFragment.setCancelable(true);
        progressBarUploadDialogFragment.show(getSupportFragmentManager(), "Upload Profile");
    }

    @Override
    public void onProgressError(String errorMsg) {
        if (progressBarUploadDialogFragment != null)
            progressBarUploadDialogFragment.dismiss();
        GPDialogs.warningDialog(this, errorMsg, null);
    }
    @Override
    public void onProgressDone(String msg) {
        if (progressBarUploadDialogFragment != null)
            progressBarUploadDialogFragment.dismiss();
        if (uploadProfile != null) {
//            saveWebProfile(uploadProfile);
            uploadProfile = null;
        }
        if (msg != null && msg.trim().length() > 0)
            GPDialogs.infoDialog(this, msg, null);


    }


}
