package eu.geopaparazzi.core.profiles;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.profiles.gui.FormTagsFragment;
import eu.geopaparazzi.core.profiles.gui.NewProfileDialogFragment;
import eu.geopaparazzi.core.profiles.gui.ProfileSettingsActivity;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.dialogs.ColorStrokeDialogFragment;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.style.ColorStrokeObject;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TimeUtilities;

public class ProfilesActivity extends AppCompatActivity implements NewProfileDialogFragment.INewProfileCreatedListener, ColorStrokeDialogFragment.IColorStrokePropertiesChangeListener {

    public static final String PROFILES_CONFIG_JSON = "profiles_config.json";
    public static final String KEY_SELECTED_PROFILE = "KEY_SELECTED_PROFILE";
    private LinearLayout profilesContainer;
    private LinearLayout emptyFiller;
    private SharedPreferences mPeferences;

    private List<Profile> profileList = new ArrayList<>();
    private CardView currentColorCardView;
    private Profile currentProfile;

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
                newProfileDialogFragment.show(getSupportFragmentManager(), "New Profile Dialog");
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();

        try {
            profileList = ProfilesHandler.INSTANCE.getProfilesFromPreferences(mPeferences);
        } catch (JSONException e) {
            Log.e("GEOS2GO", "", e);
        }

        loadProfiles();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (profileList != null) {
            saveProfiles();
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

            TextView profilesummaryText = newProjectCardView.findViewById(R.id.profileSummaryText);
            StringBuilder sb = new StringBuilder();
            sb.append("Basemaps: ").append(profile.basemapsList.size()).append("\n");
            sb.append("Spatialite dbs: ").append(profile.spatialiteList.size()).append("\n");
            int formsCount = 0;
            if (profile.tagsPath != null && profile.tagsPath.length() != 0) {
                File tagsFile = new File(profile.tagsPath);
                if (tagsFile.exists()) {
                    try {
                        List<String> sections = FormTagsFragment.getSectionsFromTagsFile(tagsFile);
                        formsCount = sections.size();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            sb.append("Forms: ").append(formsCount).append("\n");
            sb.append("Has project: ").append(profile.projectPath.length() == 0 ? "no" : "yes").append("\n");
            profilesummaryText.setText(sb.toString());

            ImageButton settingsButton = newProjectCardView.findViewById(R.id.settingsButton);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent preferencesIntent = new Intent(ProfilesActivity.this, ProfileSettingsActivity.class);
                    preferencesIntent.putExtra(KEY_SELECTED_PROFILE, profile);
                    startActivity(preferencesIntent);
                }
            });
            ImageButton deleteButton = newProjectCardView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String msg = String.format("Are you sure you want to remove the profile: '%s'? This can't be undone.", profile.name);
                    GPDialogs.yesNoMessageDialog(ProfilesActivity.this, msg, new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    profileList.remove(profile);
                                    saveProfiles();
                                    loadProfiles();
                                }
                            });
                        }
                    }, null);

                }
            });
            ImageButton colorButton = newProjectCardView.findViewById(R.id.colorButton);
            colorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    currentColorCardView = newProjectCardView;
                    currentProfile = profile;
                    int color = ColorUtilities.toColor(profile.color);
                    ColorStrokeObject colorStrokeObject = new ColorStrokeObject();
                    colorStrokeObject.hasFill = true;
                    colorStrokeObject.hasStroke = false;
                    colorStrokeObject.fillColor = color;

                    ColorStrokeDialogFragment colorStrokeDialogFragment = ColorStrokeDialogFragment.newInstance(colorStrokeObject);
                    colorStrokeDialogFragment.show(getSupportFragmentManager(), "Color Dialog");
                }
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
            File sdcardDir = resourcesManager.getMainStorageDir();
            File applicationSupporterDir = resourcesManager.getApplicationSupporterDir();
            File inputFile = new File(applicationSupporterDir, PROFILES_CONFIG_JSON);
            if (inputFile.exists()) {
                String profilesJson = FileUtilities.readfile(inputFile);
                List<Profile> importedProfiles = ProfilesHandler.INSTANCE.getProfilesFromJson(profilesJson);

                // substitute sdcard. In case it was exported from another device
                for (Profile profile : importedProfiles) {
                    profile.correctPaths(sdcardDir.getAbsolutePath());
                }


                profileList.addAll(importedProfiles);
                saveProfiles();
                loadProfiles();

                GPDialogs.quickInfo(profilesContainer, "Profiles properly imported.");
            } else {
                GPDialogs.warningDialog(this, "No profiles file exist in the path: " + inputFile.getAbsolutePath(), null);
            }
        } catch (Exception e) {
            GPDialogs.warningDialog(this, "An error occurred: " + e.getLocalizedMessage(), null);
            Log.e("GEOS2GO", "", e);
        }
    }

    private void exportProfiles() {
        try {
            ResourcesManager resourcesManager = ResourcesManager.getInstance(this);
            File applicationSupporterDir = resourcesManager.getApplicationSupporterDir();
            File outFile = new File(applicationSupporterDir, PROFILES_CONFIG_JSON);
            String jsonFromProfiles = ProfilesHandler.INSTANCE.getJsonFromProfilesList(profileList, true);
            FileUtilities.writefile(jsonFromProfiles, outFile);
            GPDialogs.quickInfo(profilesContainer, "Profiles exported to: " + outFile);
        } catch (Exception e) {
            GPDialogs.warningDialog(this, "An error occurred: " + e.getLocalizedMessage(), null);
            Log.e("GEOS2GO", "", e);
        }
    }

    @Override
    public void onNewProfileCreated(String name, String description) {
        Profile p = new Profile();
        p.name = name;
        p.description = description;
        p.creationdate = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date());
        try {
            p.sdcardPath = ResourcesManager.getInstance(this).getMainStorageDir().getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
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
            Log.e("GEOS2GO", "Error saving profiles", e);
        }
    }
}
