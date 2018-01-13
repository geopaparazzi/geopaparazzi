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
import java.util.List;
import java.util.Set;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.profiles.ProfilesActivity;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.profiles.Profile;
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
            Log.e("GEOS2GO", "", e);
        }

        mSelectedProfileIndex = mProfileList.indexOf(selectedProfile);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        int color = ColorUtilities.toColor(selectedProfile.color);
        mViewPager.setBackgroundColor(color);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mProfileList != null) {
            try {
                ProfilesHandler.INSTANCE.saveProfilesToPreferences(mPeferences, mProfileList);
            } catch (JSONException e) {
                Log.e("GEOS2GO", "Error saving profiles", e);
            }
        }
    }

    public void onProfileInfoChanged(String name, String description) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        profile.name = name;
        profile.description = description;
    }

    public void onFormPathChanged(String path) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        profile.tagsPath = path;
    }

    public void onProjectPathChanged(String path) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        profile.projectPath = path;
    }

    public void onBasemapAdded(String path) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        profile.basemapsList.add(path);
    }

    public void onBasemapRemoved(String path) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        profile.basemapsList.remove(path);
    }

    public void onSpatialitedbRemoved(String path) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        profile.spatialiteList.remove(path);
    }

    public void onSpatialitedbAdded(String path) {
        Profile profile = mProfileList.get(mSelectedProfileIndex);
        profile.spatialiteList.add(path);
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
                    if (crs.equals("CRS:84") || crs.equals("EPSG:4326")) {
                        srs = crs;

                        boolean doLonLat = false;
                        if (crs.equals("CRS:84")) {
                            doLonLat = true;
                        } else if (crs.equals("EPSG:4326") && !wmsversion.equals("1.3.0")) {
                            doLonLat = true;
                        }

                        String bboxStr;
                        if (doLonLat) {
                            bboxStr = "XXX,YYY,XXX,YYY";
                        } else {
                            bboxStr = "YYY,XXX,YYY,XXX";
                        }
                        sb.append("url=" + baseUrl.trim() + "?REQUEST=GetMap&SERVICE=WMS&VERSION=" + wmsversion //
                                + "&LAYERS=" + layerName + "&STYLES=&FORMAT=image/png&BGCOLOR=0xFFFFFF&TRANSPARENT=TRUE&SRS=" //
                                + srs + "&BBOX=" + bboxStr + "&WIDTH=256&HEIGHT=256\n");
                        sb.append("minzoom=1\n");
                        sb.append("maxzoom=22\n");
                        sb.append("defaultzoom=17\n");
                        sb.append("format=png\n");
                        sb.append("type=wms\n");
                        sb.append("description=").append(layerName).append("\n");


                        break;
                    }
                }

                if (srs == null) {
                    // TODO
                    return;
                }

                for (OGCBoundingBox bbox : layerCapability.getBoundingBoxes()) {
                    String crs = bbox.getCRS();
                    if (crs.equals("CRS:84") || crs.equals("EPSG:4326")) {
                        double centerX = bbox.getMinx() + (bbox.getMaxx() - bbox.getMinx()) / 2.0;
                        double centerY = bbox.getMiny() + (bbox.getMaxy() - bbox.getMiny()) / 2.0;
                        sb.append("center=");
                        sb.append(centerX).append(" ").append(centerY);
                        sb.append("\n");

                    }
                }

            }

            try {
                File applicationSupporterDir = ResourcesManager.getInstance(this).getApplicationSupporterDir();
                File newMapurl = new File(applicationSupporterDir, layerName + ".mapurl");

                sb.append("mbtiles=defaulttiles/_" + newMapurl.getName() + ".mbtiles\n");

                String mapurlText = sb.toString();
                FileUtilities.writefile(mapurlText, newMapurl);

                onBasemapAdded(newMapurl.getAbsolutePath());

                mSectionsPagerAdapter.notifyDataSetChanged();

                GPDialogs.quickInfo(mViewPager, "WMS mapurl file successfully added to the basemaps and saved in: " + newMapurl.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }

            break;
        }


    }

    public Profile getSelectedProfile() {
        return mProfileList.get(mSelectedProfileIndex);
    }

    public void onActiveProfileChanged(boolean isChecked) {
        for (int i = 0; i < mProfileList.size(); i++) {
            Profile profile = mProfileList.get(i);
            if (i == mSelectedProfileIndex && isChecked) {
                profile.active = true;
            } else {
                profile.active = false;
            }
        }
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
                    return "Profile info";
                case 1:
                    return "Basemaps";
                case 2:
                    return "Spatialite Databases";
                case 3:
                    return "Forms";
                case 4:
                    return "Project";
            }
            return null;
        }
    }
}
