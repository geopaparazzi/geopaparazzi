package eu.geopaparazzi.core.profiles;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.profiles.gui.FormTagsFragment;
import eu.geopaparazzi.core.profiles.gui.NewProfileDialogFragment;
import eu.geopaparazzi.core.profiles.gui.ProfileSettingsActivity;
import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.dialogs.ColorStrokeDialogFragment;
import eu.geopaparazzi.library.database.DatabaseUtilities;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.style.ColorStrokeObject;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.FileTypes;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.TimeUtilities;

public class ProfilesActivity extends AppCompatActivity implements NewProfileDialogFragment.INewProfileCreatedListener, ColorStrokeDialogFragment.IColorStrokePropertiesChangeListener {

    public static final String PROFILES_CONFIG_JSON = "profiles_config.json"; //NON-NLS
    public static final String KEY_SELECTED_PROFILE = "KEY_SELECTED_PROFILE"; //NON-NLS
    private LinearLayout profilesContainer;
    private LinearLayout emptyFiller;
    private SharedPreferences mPeferences;

    private List<Profile> profileList = new ArrayList<>();
    private CardView currentColorCardView;
    private Profile currentProfile;
    private Profile previousActiveProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geoss2_go);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        profilesContainer = findViewById(R.id.profiles_container);
        emptyFiller = findViewById(R.id.empty_fillers);

        mPeferences = PreferenceManager.getDefaultSharedPreferences(this);

        FloatingActionButton fab = findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NewProfileDialogFragment newProfileDialogFragment = NewProfileDialogFragment.newInstance(null, null);
                newProfileDialogFragment.show(getSupportFragmentManager(), "New Profile Dialog");//NON-NLS
            }
        });

        previousActiveProfile = ProfilesHandler.INSTANCE.getActiveProfile();
    }


    @Override
    protected void onResume() {
        super.onResume();

        try {
            profileList = ProfilesHandler.INSTANCE.getProfilesFromPreferences(mPeferences);
        } catch (JSONException e) {
            Log.e("GEOS2GO", "", e);//NON-NLS
        }

        loadProfiles();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (profileList != null) {
            saveProfiles();
            try {
                ProfilesHandler.INSTANCE.checkActiveProfile(getContentResolver());
                Profile activeProfile = ProfilesHandler.INSTANCE.getActiveProfile();
                if (activeProfile != null) {
                    if (previousActiveProfile == null || !previousActiveProfile.name.equals(activeProfile.name)) {  //this is a "new" active profile
                        // --- for Map View ---
                        ProfilesHandler.INSTANCE.ProfileChanged = true;  // used to set map layers to profile's layers
                        String mapViewJson = activeProfile.mapView;
                        String[] coordinates = mapViewJson.split(",");
                        if (coordinates.length == 3) {
                            double lat = Double.parseDouble(coordinates[0]);
                            double lon = Double.parseDouble(coordinates[1]);
                            float zoom = Float.parseFloat(coordinates[2]);
                            PositionUtilities.putMapCenterInPreferences(mPeferences, lon, lat, zoom);
                        }

                        // --- for Project View ---
                        try {
                            GPApplication gpApplication = GPApplication.getInstance();
                            ResourcesManager resourcesManager = ResourcesManager.getInstance(gpApplication);
                            File sdcardDir = resourcesManager.getMainStorageDir();
                            String filePath = activeProfile.profileProject.getRelativePath(); //data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                            if (!filePath.endsWith(FileTypes.GPAP.getExtension())) {
                                GPDialogs.warningDialog(ProfilesActivity.this, ProfilesActivity.this.getString(R.string.selected_file_is_no_geopap_project), null);
                                return;
                            }
                            File file = new File(sdcardDir, filePath);
                            if (file.exists()) {
                                try {
                                    DatabaseUtilities.setNewDatabase(ProfilesActivity.this, GeopaparazziApplication.getInstance(), file.getAbsolutePath());
                                } catch (IOException e) {
                                    GPLog.error(this, null, e);
                                    GPDialogs.warningDialog(ProfilesActivity.this, ProfilesActivity.this.getString(R.string.error_while_setting_project), null);
                                }
                            }
                        } catch (Exception e) {
                            GPDialogs.errorDialog(ProfilesActivity.this, e, null);
                        }

                        // --- for Form/notes View ---
                        TagsManager.reset();

                    }
                }
            } catch (java.lang.Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadProfiles() {
        profilesContainer.removeAllViews();

        if (profileList.size() != 0) {
            emptyFiller.setVisibility(View.GONE);
        } else {
            emptyFiller.setVisibility(View.VISIBLE);
        }

        for (final Profile profile : profileList) {
            final CardView newProjectCardView = (CardView) getLayoutInflater().inflate(R.layout.profile_cardlayout, null);
            TextView profileNameText = newProjectCardView.findViewById(R.id.profileNameText);
            profileNameText.setText(profile.name);
            TextView profileDescriptionText = newProjectCardView.findViewById(R.id.profileDescriptionText);
            profileDescriptionText.setText(profile.description);

            final Switch activeSwitch = newProjectCardView.findViewById(R.id.activeSwitch);
            activeSwitch.setChecked(profile.active);
            activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                profile.active = isChecked;

                for (int i = 0; i < profileList.size(); i++) {
                    Profile tmpProfile = profileList.get(i);
                    if (profile != tmpProfile) {
                        tmpProfile.active = false;
                    }
                }
                if (profile.active) {
                    activeSwitch.setText(R.string.profiles_deactivate_profile);
                } else {
                    activeSwitch.setText(R.string.profiles_activate_profile);
                }
                // FIXME
//                    try {
//                        BaseMapSourcesManager.INSTANCE.setSelectedBaseMap(null);
//                    } catch (Exception e) {
//                        // can be ignored
//                    }
                loadProfiles();
            });
            if (profile.active) {
                activeSwitch.setText(R.string.profiles_deactivate_profile);
            } else {
                activeSwitch.setText(R.string.profiles_activate_profile);
            }

            TextView profilesummaryText = newProjectCardView.findViewById(R.id.profileSummaryText);
            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.basemaps_colon)).append(profile.basemapsList.size()).append("\n");
            sb.append(getString(R.string.spatialita_dbs_colon)).append(profile.spatialiteList.size()).append("\n");
            int formsCount = 0;
            if (profile.profileTags != null && profile.profileTags.getRelativePath().length() != 0) {
                File tagsFile = profile.getFile(profile.profileTags.getRelativePath());
                if (tagsFile.exists()) {
                    try {
                        List<String> sections = FormTagsFragment.getSectionsFromTagsFile(tagsFile);
                        formsCount = sections.size();
                    } catch (java.lang.Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            sb.append(getString(R.string.forms_colon)).append(formsCount).append("\n");
            sb.append(getString(R.string.has_project_colon)).append(profile.profileProject == null ? getString(R.string.no) : getString(R.string.yes)).append("\n");
            profilesummaryText.setText(sb.toString());

            ImageButton settingsButton = newProjectCardView.findViewById(R.id.settingsButton);
            settingsButton.setOnClickListener(v -> {
                Intent preferencesIntent = new Intent(ProfilesActivity.this, ProfileSettingsActivity.class);
                preferencesIntent.putExtra(KEY_SELECTED_PROFILE, profile);
                startActivity(preferencesIntent);
            });
            ImageButton deleteButton = newProjectCardView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(v -> {
                String msg = String.format(ProfilesActivity.this.getString(R.string.prompt_remove_profile), profile.name);
                GPDialogs.yesNoMessageDialog(ProfilesActivity.this, msg, () -> runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        profileList.remove(profile);
                        saveProfiles();
                        loadProfiles();
                    }
                }), null);

            });
            ImageButton colorButton = newProjectCardView.findViewById(R.id.colorButton);
            colorButton.setOnClickListener(v -> {

                currentColorCardView = newProjectCardView;
                currentProfile = profile;
                int color = ColorUtilities.toColor(profile.color);
                ColorStrokeObject colorStrokeObject = new ColorStrokeObject();
                colorStrokeObject.hasFill = true;
                colorStrokeObject.hasStroke = false;
                colorStrokeObject.fillColor = color;

                ColorStrokeDialogFragment colorStrokeDialogFragment = ColorStrokeDialogFragment.newInstance(colorStrokeObject);
                colorStrokeDialogFragment.show(getSupportFragmentManager(), "Color Dialog"); //NON-NLS
            });

            String color = profile.color;
            setCardviewColor(newProjectCardView, color);
//            newProjectCardView.setBackgroundColor(backgroundColor);

            LinearLayout activeLayoutTop = newProjectCardView.findViewById(R.id.activeColorLayoutTop);
            LinearLayout activeLayoutBottom = newProjectCardView.findViewById(R.id.activeColorLayoutBottom);
            if (profile.active) {
                activeLayoutTop.setVisibility(View.VISIBLE);
                activeLayoutTop.setBackgroundColor(Color.RED);
                activeLayoutBottom.setVisibility(View.VISIBLE);
                activeLayoutBottom.setBackgroundColor(Color.RED);
            } else {
                activeLayoutTop.setVisibility(View.INVISIBLE);
                activeLayoutTop.setBackgroundColor(Color.WHITE);
                activeLayoutBottom.setVisibility(View.INVISIBLE);
                activeLayoutBottom.setBackgroundColor(Color.WHITE);
            }

            profilesContainer.addView(newProjectCardView);
        }

    }

    private void setCardviewColor(CardView newProjectCardView, String color) {
        int backgroundColor = ColorUtilities.toColor(color);
        newProjectCardView.setCardBackgroundColor(backgroundColor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_geoss2_go, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_import) {
            importProfiles();
            return true;
        } else if (id == R.id.action_export) {
            exportProfiles();
            return true;
        } else if (id == R.id.action_delete_all) {
            profileList.clear();
            saveProfiles();
            loadProfiles();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void importProfiles() {
        try {
            ResourcesManager resourcesManager = ResourcesManager.getInstance(this);
            File applicationSupporterDir = resourcesManager.getApplicationSupporterDir();
            File inputFile = new File(applicationSupporterDir, PROFILES_CONFIG_JSON);
            if (inputFile.exists()) {
                String profilesJson = FileUtilities.readfile(inputFile);
                List<Profile> importedProfiles = ProfilesHandler.INSTANCE.getProfilesFromJson(profilesJson, false);

                for (Profile profile : importedProfiles) {
                    if (!profileList.contains(profile)) {
                        profileList.add(profile);
                    }
                }

                saveProfiles();
                loadProfiles();

                GPDialogs.quickInfo(profilesContainer, getString(R.string.profiles_imported));
            } else {
                GPDialogs.warningDialog(this, getString(R.string.no_profiles_in_path) + inputFile.getAbsolutePath(), null);
            }
        } catch (java.lang.Exception e) {
            GPDialogs.warningDialog(this, getString(R.string.an_error_occurred) + e.getLocalizedMessage(), null);
            Log.e("GEOS2GO", "", e); //NON-NLS
        }
    }

    private void exportProfiles() {
        try {
            ResourcesManager resourcesManager = ResourcesManager.getInstance(this);
            File applicationSupporterDir = resourcesManager.getApplicationSupporterDir();
            File outFile = new File(applicationSupporterDir, PROFILES_CONFIG_JSON);
            String jsonFromProfiles = ProfilesHandler.INSTANCE.getJsonFromProfilesList(profileList, true);
            FileUtilities.writefile(jsonFromProfiles, outFile);
            GPDialogs.quickInfo(profilesContainer, getString(R.string.profiles_exported_to) + outFile);
        } catch (java.lang.Exception e) {
            GPDialogs.warningDialog(this, getString(R.string.an_error_occurred) + e.getLocalizedMessage(), null);
            GPLog.error(this, null, e);
        }
    }

    @Override
    public void onNewProfileCreated(String name, String description) {
        Profile p = new Profile();
        p.name = name;
        p.description = description;
        p.creationdate = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date());
        try {
            p.setSdcardPath(ResourcesManager.getInstance(this).getMainStorageDir().getAbsolutePath());
        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }
        profileList.add(p);
        loadProfiles();
    }

    @Override
    public void onPropertiesChanged(ColorStrokeObject newColorStrokeObject) {
        if (newColorStrokeObject != null && newColorStrokeObject.hasFill && currentColorCardView != null && currentProfile != null) {
            String newColor = ColorUtilities.getHex(newColorStrokeObject.fillColor);
            currentProfile.color = newColor;
            setCardviewColor(currentColorCardView, newColor);

            // and save it to disk
            saveProfiles();
        }
    }

    private void saveProfiles() {
        try {
            ProfilesHandler.INSTANCE.saveProfilesToPreferences(mPeferences, profileList);

            Intent intent = new Intent((String) null);
            intent.putExtra(LibraryConstants.PREFS_KEY_RESTART_APPLICATION, true);
            setResult(Activity.RESULT_OK, intent);
        } catch (JSONException e) {
            Log.e("GEOS2GO", "Error saving profiles", e); //NON-NLS
        }
    }

}
