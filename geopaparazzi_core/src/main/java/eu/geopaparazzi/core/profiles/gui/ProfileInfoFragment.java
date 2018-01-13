package eu.geopaparazzi.core.profiles.gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.library.profiles.Profile;


public class ProfileInfoFragment extends Fragment implements TextWatcher, CompoundButton.OnCheckedChangeListener {
    private static final String ARG_PROFILE = "profile";
    private EditText nameEdittext;
    private EditText descriptionEdittext;
    private EditText creationdateEdittext;

    public ProfileInfoFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ProfileInfoFragment newInstance(Profile profile) {
        ProfileInfoFragment fragment = new ProfileInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PROFILE, profile);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profilesettings_info, container, false);

        Profile profile = getArguments().getParcelable(ARG_PROFILE);

        nameEdittext = (EditText) rootView.findViewById(R.id.profileNameEditText);
        nameEdittext.setText(profile.name);
        nameEdittext.addTextChangedListener(this);

        descriptionEdittext = (EditText) rootView.findViewById(R.id.profileDescriptionEditText);
        descriptionEdittext.setText(profile.description);
        descriptionEdittext.addTextChangedListener(this);

        creationdateEdittext = (EditText) rootView.findViewById(R.id.profileCreationdateEditText);
        creationdateEdittext.setText(profile.creationdate);
        creationdateEdittext.addTextChangedListener(this);

        final Switch activeSwitch = (Switch) rootView.findViewById(R.id.activeSwitch);
        activeSwitch.setChecked(profile.active);
        activeSwitch.setOnCheckedChangeListener(this);
        if (profile.active) {
            activeSwitch.setText(R.string.profiles_deactivate_profile);
        } else {
            activeSwitch.setText(R.string.profiles_activate_profile);
        }


        return rootView;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        ProfileSettingsActivity activity = (ProfileSettingsActivity) getActivity();
        activity.onProfileInfoChanged(nameEdittext.getText().toString(), descriptionEdittext.getText().toString());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ProfileSettingsActivity activity = (ProfileSettingsActivity) getActivity();
        activity.onActiveProfileChanged(isChecked);
    }
}