package eu.geopaparazzi.core.profiles.gui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.profiles.ProfilesActivity;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.objects.ProfileBasemaps;
import eu.geopaparazzi.library.profiles.objects.ProfileProjects;
import eu.geopaparazzi.library.profiles.objects.ProfileSpatialitemaps;
import eu.geopaparazzi.library.profiles.objects.ProfileTags;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import gov.nasa.worldwind.AddWMSDialog;
import gov.nasa.worldwind.ogc.OGCBoundingBox;
import gov.nasa.worldwind.ogc.wms.WMSCapabilityInformation;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;

public class ProfileSettingsActivity extends AppCompatActivity implements AddWMSDialog.OnWMSLayersAddedListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
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

    @Override
    public void onWMSLayersAdded(String baseUrl, String forcedWmsVersion, List<AddWMSDialog.LayerInfo> layersToAdd) {
        for (AddWMSDialog.LayerInfo li : layersToAdd) {
            String layerName = li.getName();

            StringBuilder sb = new StringBuilder();
            String wmsversion = "1.1.1";

            if (forcedWmsVersion != null) {
                wmsversion = forcedWmsVersion;
            } else if (li.caps.getVersion() != null) {
                wmsversion = li.caps.getVersion();
            }
            WMSCapabilityInformation capabilityInformation = li.caps.getCapabilityInformation();

            List<WMSLayerCapabilities> layerCapabilities = capabilityInformation.getLayerCapabilities();


            for (WMSLayerCapabilities layerCapability : layerCapabilities) {
                String srs = null;

                Set<String> crsList = layerCapability.getCRS();
                if (crsList.size() == 0) {
                    crsList = layerCapability.getSRS();
                }
                for (String crs : crsList) {
                    if (crs.equals("CRS:84") || crs.equals("EPSG:4326")) {//NON-NLS
                        srs = crs;

                        boolean doLonLat = false;
                        if (crs.equals("CRS:84")) {//NON-NLS
                            doLonLat = true;
                        } else if (crs.equals("EPSG:4326") && !wmsversion.equals("1.3.0")) {//NON-NLS
                            doLonLat = true;
                        }

                        String bboxStr;
                        if (doLonLat) {
                            bboxStr = "XXX,YYY,XXX,YYY";//NON-NLS
                        } else {
                            bboxStr = "YYY,XXX,YYY,XXX";//NON-NLS
                        }
                        sb.append("url=" + baseUrl.trim() + "?REQUEST=GetMap&SERVICE=WMS&VERSION=" + wmsversion //NON-NLS
                                + "&LAYERS=" + layerName + "&STYLES=&FORMAT=image/png&BGCOLOR=0xFFFFFF&TRANSPARENT=TRUE&SRS=" //NON-NLS
                                + srs + "&BBOX=" + bboxStr + "&WIDTH=256&HEIGHT=256\n");//NON-NLS
                        sb.append("minzoom=1\n");//NON-NLS
                        sb.append("maxzoom=22\n");//NON-NLS
                        sb.append("defaultzoom=17\n");//NON-NLS
                        sb.append("format=png\n");//NON-NLS
                        sb.append("type=wms\n");//NON-NLS
                        sb.append("description=").append(layerName).append("\n");//NON-NLS


                        break;
                    }
                }

                if (srs == null) {
                    // TODO
                    return;
                }

                for (OGCBoundingBox bbox : layerCapability.getBoundingBoxes()) {
                    String crs = bbox.getCRS();
                    if (crs != null && (crs.equals("CRS:84") || crs.equals("EPSG:4326"))) {//NON-NLS
                        double centerX = bbox.getMinx() + (bbox.getMaxx() - bbox.getMinx()) / 2.0;
                        double centerY = bbox.getMiny() + (bbox.getMaxy() - bbox.getMiny()) / 2.0;
                        sb.append("center=");//NON-NLS
                        sb.append(centerX).append(" ").append(centerY);
                        sb.append("\n");

                    }
                }

            }

            try {
                File applicationSupporterDir = ResourcesManager.getInstance(this).getApplicationSupporterDir();
                File newMapurl = new File(applicationSupporterDir, layerName + ".mapurl");//NON-NLS

                sb.append("mbtiles=defaulttiles/_").append(newMapurl.getName()).append(".mbtiles\n");//NON-NLS

                String mapurlText = sb.toString();
                FileUtilities.writefile(mapurlText, newMapurl);

                onBasemapAdded(newMapurl.getAbsolutePath());

                mSectionsPagerAdapter.notifyDataSetChanged();

                GPDialogs.quickInfo(mViewPager, getString(R.string.wms_mapurl_added_to_basemaps) + newMapurl.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }

            break;
        }


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
