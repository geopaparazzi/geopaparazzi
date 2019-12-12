package eu.geopaparazzi.core.profiles.gui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;

import java.util.List;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.profiles.ProfilesActivity;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.profiles.objects.ProfileBasemaps;
import eu.geopaparazzi.library.profiles.objects.ProfileProjects;
import eu.geopaparazzi.library.profiles.objects.ProfileSpatialitemaps;
import eu.geopaparazzi.library.profiles.objects.ProfileTags;
import eu.geopaparazzi.library.style.ColorUtilities;

public class ProfileSettingsActivity extends AppCompatActivity {

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private int mSelectedProfileIndex;
    private SharedPreferences mPeferences;
    private List<Profile> mProfileList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Bundle extras = getIntent().getExtras();
        Profile selectedProfile = extras.getParcelable(ProfilesActivity.KEY_SELECTED_PROFILE);

        mPeferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            mProfileList = ProfilesHandler.INSTANCE.getProfilesFromPreferences(mPeferences);
        } catch (JSONException e) {
            Log.e("GEOS2GO", "", e);//NON-NLS
        }

        mSelectedProfileIndex = mProfileList.indexOf(selectedProfile);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        int color = ColorUtilities.toColor(selectedProfile.color);
        mViewPager.setBackgroundColor(color);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mProfileList != null) {
            try {
                ProfilesHandler.INSTANCE.saveProfilesToPreferences(mPeferences, mProfileList);
            } catch (JSONException e) {
                Log.e("GEOS2GO", "Error saving profiles", e);//NON-NLS
            }
        }
    }

    public void onProfileInfoChanged(String name, String description) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        profile.name = name;
        profile.description = description;
    }

    public void onFormPathChanged(String relativePath) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        if (profile.profileTags == null) {
            profile.profileTags = new ProfileTags();
        }
        profile.profileTags.setRelativePath(relativePath);
        TagsManager.reset();
    }

    public void onProjectPathChanged(String relatvePath) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        if (relatvePath == null || relatvePath.length() == 0) {
            profile.profileProject = null;
        } else {
            if (profile.profileProject == null) {
                profile.profileProject = new ProfileProjects();
            }
            profile.profileProject.setRelativePath(relatvePath);
        }
    }

    public void onBasemapAdded(String relativePath) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        ProfileBasemaps basemap = new ProfileBasemaps();
        basemap.setRelativePath(relativePath);
        if (!profile.basemapsList.contains(basemap))
            profile.basemapsList.add(basemap);
    }

    public void onBasemapRemoved(String relativePath) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        ProfileBasemaps basemap = new ProfileBasemaps();
        basemap.setRelativePath(relativePath);
        profile.basemapsList.remove(basemap);
    }

    public void onSpatialitedbAdded(String relativePath) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        ProfileSpatialitemaps spatialitemap = new ProfileSpatialitemaps();
        spatialitemap.setRelativePath(relativePath);
        if (!profile.spatialiteList.contains(spatialitemap))
            profile.spatialiteList.add(spatialitemap);
    }

    public void onSpatialitedbRemoved(String relativePath) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        ProfileSpatialitemaps spatialitemap = new ProfileSpatialitemaps();
        spatialitemap.setRelativePath(relativePath);
        profile.spatialiteList.remove(spatialitemap);
    }


    public Profile getSelectedProfile() {
        return mProfileList.get(mSelectedProfileIndex);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Profile profile = mProfileList.get(mSelectedProfileIndex);
            switch (position) {
                case 0:
                    return ProfileInfoFragment.newInstance(profile);
                case 1:
                    return BasemapsFragment.newInstance(profile);
                case 2:
                    return SpatialiteDatabasesFragment.newInstance(profile);
                case 3:
                    return FormTagsFragment.newInstance(profile);
                case 4:
                    return ProjectFragment.newInstance(profile);
            }
            return null;
        }

        @Override
        public int getItemPosition(Object object) {

            // TODO check performance and in case implement with updateable interface
            //  MyFragment f = (MyFragment ) object;
//            if (f != null) {
//                f.update();
//            }
//            return super.getItemPosition(object);
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.profile_info_title);
                case 1:
                    return getString(R.string.profile_basemap_title);
                case 2:
                    return getString(R.string.profile_spatialite_title);
                case 3:
                    return getString(R.string.profile_forms_title);
                case 4:
                    return getString(R.string.profile_project_title);
            }
            return null;
        }
    }
}
