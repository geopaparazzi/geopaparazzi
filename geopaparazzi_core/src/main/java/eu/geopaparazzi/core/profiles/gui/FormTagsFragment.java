package eu.geopaparazzi.core.profiles.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.activities.DirectoryBrowserActivity;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;

import static eu.geopaparazzi.library.forms.FormUtilities.ATTR_SECTIONNAME;

public class FormTagsFragment extends Fragment {
    private static final String ARG_PROFILE = "profile";
    public static final int RETURNCODE_BROWSE = 666;

    private EditText nameEdittext;
    private EditText pathEdittext;
    private EditText formsEdittext;
    private Profile profile;

    public FormTagsFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static FormTagsFragment newInstance(Profile profile) {
        FormTagsFragment fragment = new FormTagsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PROFILE, profile);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profilesettings_forms, container, false);

        profile = getArguments().getParcelable(ARG_PROFILE);

        nameEdittext = rootView.findViewById(R.id.formNameEditText);
        pathEdittext = rootView.findViewById(R.id.formPathEditText);
        formsEdittext = rootView.findViewById(R.id.sectionsEditText);
        if (profile != null && profile.profileTags != null && profile.getFile(profile.profileTags.getRelativePath()).exists()) {
            setFormData(profile.getFile(profile.profileTags.getRelativePath()));
        }
        FloatingActionButton addFormButton = rootView.findViewById(R.id.addFormsjsonButton);
        addFormButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File sdcardDir = ResourcesManager.getInstance(getContext()).getMainStorageDir();
                    Intent browseIntent = new Intent(getContext(), DirectoryBrowserActivity.class);
                    browseIntent.putExtra(DirectoryBrowserActivity.EXTENSIONS, new String[]{"json"});
                    browseIntent.putExtra(DirectoryBrowserActivity.STARTFOLDERPATH, sdcardDir.getAbsolutePath());
                    startActivityForResult(browseIntent, RETURNCODE_BROWSE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return rootView;
    }

    private void setFormData(File tagsFile) {
        try {
            List<String> sectionsMap = getSectionsFromTagsFile(tagsFile);
            nameEdittext.setText(tagsFile.getName());
            pathEdittext.setText(tagsFile.getAbsolutePath());
            formsEdittext.setText(Arrays.toString(sectionsMap.toArray()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    public static List<String> getSectionsFromTagsFile(File file) throws IOException, JSONException {
        List<String> sectionsMap = new ArrayList<>();
        String tagsFileString = FileUtilities.readfile(file);
        JSONArray sectionsArrayObj = new JSONArray(tagsFileString);
        int tagsNum = sectionsArrayObj.length();
        for (int i = 0; i < tagsNum; i++) {
            JSONObject jsonObject = sectionsArrayObj.getJSONObject(i);
            if (jsonObject.has(ATTR_SECTIONNAME)) {
                String sectionName = jsonObject.get(ATTR_SECTIONNAME).toString();
                sectionsMap.add(sectionName);
            }
        }
        return sectionsMap;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (RETURNCODE_BROWSE): {
                if (resultCode == Activity.RESULT_OK) {
                    String path = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                    if (path != null && new File(path).exists()) {
                        String sdcardPath = profile.getSdcardPath();

                        if (!path.contains(sdcardPath)) {
                            GPDialogs.warningDialog(getActivity(), "All data of the same profile have to reside in the same root path.", null);
                            return;
                        }

                        String relativePath = path.replaceFirst(sdcardPath, "");

                        setFormData(new File(path));
                        ProfileSettingsActivity activity = (ProfileSettingsActivity) getActivity();
                        activity.onFormPathChanged(relativePath);
                    }
                }
            }
        }
    }
}