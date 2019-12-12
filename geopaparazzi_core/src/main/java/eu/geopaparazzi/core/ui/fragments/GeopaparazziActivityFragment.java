// MainActivityFragment.java
// Contains the Flag Quiz logic
package eu.geopaparazzi.core.ui.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.system.Os;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.GeopaparazziCoreActivity;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoGpsLog;
import eu.geopaparazzi.core.database.DaoMetadata;
import eu.geopaparazzi.core.database.DaoNotes;
import eu.geopaparazzi.core.database.objects.Metadata;
import eu.geopaparazzi.core.mapview.MapviewActivity;
import eu.geopaparazzi.core.profiles.ProfilesActivity;
import eu.geopaparazzi.core.ui.activities.AboutActivity;
import eu.geopaparazzi.core.ui.activities.AddNotesActivity;
import eu.geopaparazzi.core.ui.activities.AdvancedSettingsActivity;
import eu.geopaparazzi.core.ui.activities.ExportActivity;
import eu.geopaparazzi.core.ui.activities.ImportActivity;
import eu.geopaparazzi.core.ui.activities.NotesListActivity;
import eu.geopaparazzi.core.ui.activities.PanicActivity;
import eu.geopaparazzi.core.ui.activities.ProjectMetadataActivity;
import eu.geopaparazzi.core.ui.activities.SettingsActivity;
import eu.geopaparazzi.core.ui.dialogs.GpsInfoDialogFragment;
import eu.geopaparazzi.core.ui.dialogs.NewProjectDialogFragment;
import eu.geopaparazzi.core.utilities.Constants;
import eu.geopaparazzi.core.utilities.IApplicationChangeListener;
import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.DatabaseUtilities;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.GPLogPreferencesHandler;
import eu.geopaparazzi.library.database.TableDescriptions;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.sensors.OrientationSensor;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.CompressionUtilities;
import eu.geopaparazzi.library.util.FileTypes;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TextAndBooleanRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;

import static eu.geopaparazzi.library.util.LibraryConstants.MAPSFORGE_EXTRACTED_DB_NAME;

/**
 * The fragment of the main geopap view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopaparazziActivityFragment extends Fragment implements View.OnLongClickListener, View.OnClickListener, IActivitySupporter {

    private final int RETURNCODE_BROWSE_FOR_NEW_PREOJECT = 665;
    private final int RETURNCODE_PROFILES = 666;

    private ImageButton mNotesButton;
    private ImageButton mMetadataButton;
    private ImageButton mMapviewButton;
    private ImageButton mGpslogButton;
    private ImageButton mExportButton;

    private ImageButton mImportButton;

    private OrientationSensor mOrientationSensor;
    private IApplicationChangeListener appChangeListener;

    private BroadcastReceiver mGpsServiceBroadcastReceiver;
    private static boolean sCheckedGps = false;
    private GpsServiceStatus mLastGpsServiceStatus;
    private GpsLoggingStatus mLastGpsLoggingStatus = GpsLoggingStatus.GPS_DATABASELOGGING_OFF;
    private double[] mLastGpsPosition;
    private FloatingActionButton mPanicFAB;
    private ResourcesManager mResourcesManager;
    private boolean hasProfilesProvider = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_geopaparazzi, container, false);

        // this fragment adds to the menu
        setHasOptionsMenu(true);
        FragmentActivity activity = getActivity();
        if (activity != null)
            for (PackageInfo pack : activity.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        String authority = provider.authority;
                        if (authority != null && authority.equals("eu.geopaparazzi.provider.profiles")) {//NON-NLS
                            hasProfilesProvider = true;
                        }
                    }
                }
            }

        try {
            initializeResourcesManager();

            // start gps service
            GpsServiceUtilities.startGpsService(getActivity());
        } catch (Exception e) {
            e.printStackTrace();

            GPLog.error(this, null, e);
        }
        return v; // return the fragment's view for display
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNotesButton = view.findViewById(R.id.dashboardButtonNotes);
        mNotesButton.setOnClickListener(this);
        mNotesButton.setOnLongClickListener(this);

        mMetadataButton = view.findViewById(R.id.dashboardButtonMetadata);
        mMetadataButton.setOnClickListener(this);
        mMetadataButton.setOnLongClickListener(this);

        mMapviewButton = view.findViewById(R.id.dashboardButtonMapview);
        mMapviewButton.setOnClickListener(this);
        mMapviewButton.setOnLongClickListener(this);

        mGpslogButton = view.findViewById(R.id.dashboardButtonGpslog);
        mGpslogButton.setOnClickListener(this);
        mGpslogButton.setOnLongClickListener(this);

        mImportButton = view.findViewById(R.id.dashboardButtonImport);
        mImportButton.setOnClickListener(this);
        mImportButton.setOnLongClickListener(this);

        mExportButton = view.findViewById(R.id.dashboardButtonExport);
        mExportButton.setOnClickListener(this);
        mExportButton.setOnLongClickListener(this);

        mPanicFAB = view.findViewById(R.id.panicActionButton);
        mPanicFAB.setOnClickListener(this);
        enablePanic(false);

    }


    @Override
    public void onResume() {
        super.onResume();

        Profile activeProfile = ProfilesHandler.INSTANCE.getActiveProfile();
        checkProfileColor(activeProfile);

        GpsServiceUtilities.triggerBroadcast(getActivity());

        View view = getView();
        try {
            int notesCount = DaoNotes.getNotesCount(false);
            int dirtyNotesCount = DaoNotes.getNotesCount(true);
            int logsCount = DaoGpsLog.getGpslogsCount();

            TextView notesTextView = view.findViewById(R.id.dashboardTextNotes);
            TextView logsTextView = view.findViewById(R.id.dashboardTextGpslog);
            TextView metadataTextView = view.findViewById(R.id.dashboardTextMetadata);

//            String notesText = "Notes: " + String.valueOf(notesCount);
            String notesText = String.format(getResources().getString(R.string.dashboard_msg_notes), notesCount);

            if (dirtyNotesCount > 0 && dirtyNotesCount != notesCount) {
                notesText += "(" + dirtyNotesCount + ")";
            }
            notesTextView.setText(notesText);

            String gpsText = String.format(getResources().getString(R.string.dashboard_msg_gps), logsCount);
            logsTextView.setText(gpsText);

            List<Metadata> projectMetadata = DaoMetadata.getProjectMetadata();
            String projectName = null;
            for (final Metadata metadata : projectMetadata) {
                if (metadata.key.equals("name")) {
                    projectName = metadata.value;
                    break;
                }
            }
            // ToDo: This should be handled in the fragment_geopaparazzi.xml (landscape and portrait)
            if (projectName != null) {
                if (projectName.length() > 10) projectName = projectName.substring(0, 10) + "...";
                metadataTextView.setText(projectName);
            }

        } catch (IOException e) {
            GPLog.error(this, null, e);
        }


    }

    private void checkProfileColor(Profile activeProfile) {
        View view = getView();
        if (view != null) {
            View dashboardView = view.findViewById(R.id.dashboardLayout);
            if (dashboardView != null) {
                if (activeProfile != null && activeProfile.profileProject != null && activeProfile.getFile(activeProfile.profileProject.getRelativePath()).exists()) {
                    String color = activeProfile.color;
                    if (color != null) {
                        dashboardView.setBackgroundColor(ColorUtilities.toColor(color));
                    }
                } else {
                    int color = Compat.getColor(getActivity(), R.color.main_background);
                    dashboardView.setBackgroundColor(color);
                }
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof IApplicationChangeListener) {
            appChangeListener = (IApplicationChangeListener) context;
        }

        FragmentActivity activity = getActivity();
        if (activity != null) {
            if (mOrientationSensor == null) {
                SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
                mOrientationSensor = new OrientationSensor(sensorManager, null);
            }
            mOrientationSensor.register(activity, SensorManager.SENSOR_DELAY_NORMAL);

            if (mGpsServiceBroadcastReceiver == null) {
                mGpsServiceBroadcastReceiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        onGpsServiceUpdate(intent);
                        checkFirstTimeGps(context);
                    }
                };
            }
            GpsServiceUtilities.registerForBroadcasts(activity, mGpsServiceBroadcastReceiver);
        }

    }

    // remove SourceUrlsFragmentListener when Fragment detached
    @Override
    public void onDetach() {
        super.onDetach();
        appChangeListener = null;
        mOrientationSensor.unregister();
        try {
            GpsServiceUtilities.unregisterFromBroadcasts(getActivity(), mGpsServiceBroadcastReceiver);
        } catch (Exception e) {
            // TODO how do I check if it is already unregistered
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Profile activeProfile = ProfilesHandler.INSTANCE.getActiveProfile();
        if (activeProfile != null && activeProfile.profileProject != null && activeProfile.getFile(activeProfile.profileProject.getRelativePath()).exists()) {
            // hide new project and open project
            menu.findItem(R.id.action_load).setEnabled(false);
            menu.findItem(R.id.action_new).setEnabled(false);
        } else {
            menu.findItem(R.id.action_load).setEnabled(true);
            menu.findItem(R.id.action_new).setEnabled(true);
        }
        menu.findItem(R.id.action_profiles).setVisible(hasProfilesProvider);

        MenuItem gpsItem = menu.findItem(R.id.action_gps);
        checkGpsItemStatus(gpsItem);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_new) {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                NewProjectDialogFragment newProjectDialogFragment = new NewProjectDialogFragment();
                newProjectDialogFragment.show(fragmentManager, "new project dialog");//NON-NLS
            }
            return true;
        } else if (i == R.id.action_load) {
            try {
                String title = getString(R.string.select_gpap_file);
                AppsUtilities.pickFile(this, RETURNCODE_BROWSE_FOR_NEW_PREOJECT, title, new String[]{FileTypes.GPAP.getExtension()}, null);
            } catch (Exception e) {
                GPLog.error(this, null, e);
            }
            return true;
        } else if (i == R.id.action_gps) {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                GpsInfoDialogFragment gpsInfoDialogFragment = new GpsInfoDialogFragment();
                gpsInfoDialogFragment.show(fragmentManager, "gpsinfo dialog");//NON-NLS
            }
            return true;
        } else if (i == R.id.action_gpsstatus) {
            AppsUtilities.checkAndOpenGpsStatus(getActivity());
            return true;
        } else if (i == R.id.action_settings) {
            Intent preferencesIntent = new Intent(this.getActivity(), SettingsActivity.class);
            startActivity(preferencesIntent);
            return true;
        } else if (i == R.id.action_advanced_settings) {
            Intent advancedSettingsIntent = new Intent(this.getActivity(), AdvancedSettingsActivity.class);
            startActivity(advancedSettingsIntent);
            return true;
        } else if (i == R.id.action_profiles) {
            Intent profilesIntent = new Intent(this.getActivity(), ProfilesActivity.class);
            startActivityForResult(profilesIntent, RETURNCODE_PROFILES);
            return true;
        } else if (i == R.id.action_about) {
            Intent intent = new Intent(getActivity(), AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (i == R.id.action_exit) {
            appChangeListener.onAppIsShuttingDown();
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FragmentActivity activity = getActivity();
        if (activity == null) return;
        switch (requestCode) {
            case (RETURNCODE_BROWSE_FOR_NEW_PREOJECT): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String filePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                        if (filePath == null) return;
                        if (!filePath.endsWith(FileTypes.GPAP.getExtension())) {
                            GPDialogs.warningDialog(activity, activity.getString(R.string.selected_file_is_no_geopap_project), null);
                            return;
                        }
                        File file = new File(filePath);
                        if (file.exists()) {
                            Utilities.setLastFilePath(activity, filePath);
                            try {
                                DatabaseUtilities.setNewDatabase(activity, GeopaparazziApplication.getInstance(), file.getAbsolutePath());

                                if (appChangeListener != null) {
                                    appChangeListener.onApplicationNeedsRestart();
                                }
                            } catch (IOException e) {
                                GPLog.error(this, null, e);
                                GPDialogs.warningDialog(activity, activity.getString(R.string.error_while_setting_project), null);
                            }
                        }
                    } catch (Exception e) {
                        GPDialogs.errorDialog(activity, e, null);
                    }
                }
                break;
            }
            case (RETURNCODE_PROFILES): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        boolean restart = data.getBooleanExtra(LibraryConstants.PREFS_KEY_RESTART_APPLICATION, false);
                        if (restart) {
                            if (activity instanceof GeopaparazziCoreActivity) {
                                GeopaparazziCoreActivity geopaparazziCoreActivity = (GeopaparazziCoreActivity) activity;
                                geopaparazziCoreActivity.onApplicationNeedsRestart();
                            }
                        }

                    } catch (Exception e) {
                        GPDialogs.errorDialog(activity, e, null);
                    }
                }
                break;
            }
//            case (RETURNCODE_NOTES): {
//                if (resultCode == Activity.RESULT_OK) {
//                    String[] noteArray = data.getStringArrayExtra(LibraryConstants.PREFS_KEY_NOTE);
//                    if (noteArray != null) {
//                        try {
//                            double lon = Double.parseDouble(noteArray[0]);
//                            double lat = Double.parseDouble(noteArray[1]);
//                            double elev = Double.parseDouble(noteArray[2]);
//                            DaoNotes.addNote(lon, lat, elev, Long.parseLong(noteArray[3]), noteArray[4], "POI", null,
//                                    null);
//                        } catch (Exception e) {
//                            GPLog.error(this, null, e); //$NON-NLS-1$
//                            Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
//                        }
//                    }
//                }
//                break;
//            }
        }
    }


    @Override
    public boolean onLongClick(View v) {
        if (v instanceof ImageButton) {
            ImageButton imageButton = (ImageButton) v;

            String tooltip = imageButton.getContentDescription().toString();
            if (imageButton == mNotesButton) {
                Intent intent = new Intent(getActivity(), NotesListActivity.class);
                intent.putExtra(LibraryConstants.PREFS_KEY_MAP_ZOOM, false);
                startActivity(intent);
            } else if (imageButton == mMetadataButton) {
                try {
                    String databaseName = ResourcesManager.getInstance(getContext()).getDatabaseFile().getName();
                    tooltip += " (" + databaseName + ")";
                } catch (Exception e) {
                    // ignore
                }
            }


            Snackbar.make(v, tooltip, Snackbar.LENGTH_SHORT).show();
            return true;
        }


        if (v == mNotesButton) {
            StringBuilder tooltip = new StringBuilder("Available providers:");//NON-NLS
            FragmentActivity activity = getActivity();
            if (activity != null) {
                for (PackageInfo pack : activity.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                    ProviderInfo[] providers = pack.providers;
                    if (providers != null) {
                        for (ProviderInfo provider : providers) {
                            tooltip.append("\n").append(provider.authority);
                        }
                    }
                }
                Snackbar.make(v, tooltip.toString(), Snackbar.LENGTH_SHORT).show();
            }
        }


        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == mMetadataButton) {
            try {
                Intent projectMetadataIntent = new Intent(getActivity(), ProjectMetadataActivity.class);
                startActivity(projectMetadataIntent);
            } catch (Exception e) {
                GPLog.error(this, null, e); //$NON-NLS-1$
            }
        } else if (v == mMapviewButton) {
            Intent importIntent = new Intent(getActivity(), MapviewActivity.class);
            startActivity(importIntent);
        } else if (v == mGpslogButton) {
            handleGpsLogAction();
        } else if (v == mImportButton) {
            Intent importIntent = new Intent(getActivity(), ImportActivity.class);
            startActivity(importIntent);
        } else if (v == mNotesButton) {
            try {
                Intent mapTagsIntent = new Intent(getActivity(), AddNotesActivity.class);
                startActivity(mapTagsIntent);
            } catch (Exception e) {
                GPLog.error(this, null, e);
                GPDialogs.errorDialog(getActivity(), e, null);
            }
        } else if (v == mExportButton) {
            Intent exportIntent = new Intent(getActivity(), ExportActivity.class);
            startActivity(exportIntent);
        } else if (v == mPanicFAB) {
            if (mLastGpsPosition == null) {
                return;
            }

            Intent panicIntent = new Intent(getActivity(), PanicActivity.class);
            double lon = mLastGpsPosition[0];
            double lat = mLastGpsPosition[1];
            panicIntent.putExtra(LibraryConstants.LATITUDE, lat);
            panicIntent.putExtra(LibraryConstants.LONGITUDE, lon);
            startActivity(panicIntent);
        }

    }


    private void checkGpsItemStatus(MenuItem gpsItem) {
        if (mLastGpsServiceStatus != GpsServiceStatus.GPS_OFF) {
            if (mLastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
                gpsItem.setIcon(R.drawable.actionbar_gps_logging);
                enablePanic(true);
            } else {
                if (mLastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                    gpsItem.setIcon(R.drawable.actionbar_gps_fix_nologging);
                    enablePanic(true);
                } else {
                    gpsItem.setIcon(R.drawable.actionbar_gps_nofix);
                    enablePanic(false);
                }
            }
        } else {
            gpsItem.setIcon(R.drawable.actionbar_gps_off);
            enablePanic(false);
        }
    }

    private void onGpsServiceUpdate(Intent intent) {
        mLastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        mLastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
        int[] mLastGpsStatusExtras = GpsServiceUtilities.getGpsStatusExtras(intent);
        mLastGpsPosition = GpsServiceUtilities.getPosition(intent);
//        lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
//        lastPositiontime = GpsServiceUtilities.getPositionTime(intent);


        boolean doLog = GPLog.LOG_HEAVY;
        if (doLog && mLastGpsStatusExtras != null) {
            int satCount = mLastGpsStatusExtras[1];
            int satForFixCount = mLastGpsStatusExtras[2];
            GPLog.addLogEntry(this, "satellites: " + satCount + " of which for fix: " + satForFixCount);//NON-NLS
        }
        FragmentActivity activity = getActivity();
        if (activity != null)
            activity.invalidateOptionsMenu();
    }

    private void checkFirstTimeGps(Context context) {
        if (!sCheckedGps) {
            sCheckedGps = true;
            if (mLastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
                String prompt = getResources().getString(R.string.prompt_gpsenable);
                GPDialogs.yesNoMessageDialog(context, prompt, () -> {
                    Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(gpsOptionsIntent);
                }, null);
            }
        }
    }


    private void enablePanic(boolean enable) {
        if (enable) {
            mPanicFAB.show();
        } else {
            mPanicFAB.hide();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        /*
         * avoid oncreate call when rotating device
         * we don't want data to be reloaded
         */
        super.onConfigurationChanged(newConfig);
    }

    private void initializeResourcesManager() throws Exception {
        mResourcesManager = ResourcesManager.getInstance(getContext());

        final FragmentActivity activity = getActivity();
        if (activity != null)
            if (mResourcesManager == null) {
                GPDialogs.yesNoMessageDialog(activity, getString(eu.geopaparazzi.core.R.string.no_sdcard_use_internal_memory),
                        new Runnable() {
                            public void run() {
                                ResourcesManager.setUseInternalMemory(true);
                                try {
                                    mResourcesManager = ResourcesManager.getInstance(getContext());
                                    initIfOk();
                                } catch (Exception e) {
                                    GPLog.error(this, null, e); //$NON-NLS-1$
                                }
                            }
                        }, activity::finish
                );
            } else {
                // create the default mapsforge data extraction db
                File applicationSupporterDir = mResourcesManager.getApplicationSupporterDir();
                File newDbFile = new File(applicationSupporterDir, MAPSFORGE_EXTRACTED_DB_NAME);
                if (!newDbFile.exists()) {
                    AssetManager assetManager = activity.getAssets();
                    InputStream inputStream = assetManager.open(MAPSFORGE_EXTRACTED_DB_NAME);
                    FileUtilities.copyFile(inputStream, new FileOutputStream(newDbFile));
                }
                // initialize rest of resources
                initIfOk();
            }
    }


    private void initIfOk() {
        final FragmentActivity activity = getActivity();
        if (activity == null)
            return;
        if (mResourcesManager == null) {
            GPDialogs.warningDialog(activity, getString(R.string.sdcard_notexist), activity::finish);
            return;
        }


        try {
            File applicationDir = ResourcesManager.getInstance(activity).getApplicationSupporterDir();
            File projFolder = new File(applicationDir, "proj");//NON-NLS
            File projFolderWithDate = new File(applicationDir, "proj20190708"); // to keep versions//NON-NLS
            File projDbFile = new File(projFolderWithDate, "proj.db");//NON-NLS
            if (!projDbFile.exists()) {
                GPLog.addLogEntry("Proj 6 folder doesn't exist: " + projFolderWithDate.getAbsolutePath());//NON-NLS
                File zipFile = new File(applicationDir, "proj.zip");//NON-NLS
                AssetManager assetManager = activity.getAssets();
                InputStream inputStream = assetManager.open("proj.zip");//NON-NLS
                FileUtilities.copyFile(inputStream, new FileOutputStream(zipFile));
                GPLog.addLogEntry("Copied proj defs from asset to: " + zipFile.getAbsolutePath());//NON-NLS
                try {
                    CompressionUtilities.unzipFolder(zipFile.getAbsolutePath(), applicationDir.getAbsolutePath(), false);
                    projFolder.renameTo(projFolderWithDate);
                    GPLog.addLogEntry("Uncompressed and renamed to: " + projFolderWithDate.getAbsolutePath());//NON-NLS
                } finally {
                    zipFile.delete();
                }

            }
            Os.setenv("PROJ_LIB", projFolderWithDate.getAbsolutePath(), true);//NON-NLS
        } catch (Exception e) {
            e.printStackTrace();
        }


        /*
         * check the logging system
         */
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        GPLogPreferencesHandler.checkLog(preferences);
        GPLogPreferencesHandler.checkLogHeavy(preferences);
        GPLogPreferencesHandler.checkLogAbsurd(preferences);

        checkLogButton();

        // check for screen on
        boolean keepScreenOn = preferences.getBoolean(Constants.PREFS_KEY_SCREEN_ON, false);
        if (keepScreenOn) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        try {
            GeopaparazziApplication.getInstance().getDatabase();

            // Set the project name in the metadata, if not already available
            List<Metadata> projectMetadata = DaoMetadata.getProjectMetadata();
            for (Metadata metadata : projectMetadata) {
                if (metadata.key.equals(TableDescriptions.MetadataTableDefaultValues.KEY_NAME.getFieldName())) {
                    String projectName = metadata.value;
                    if (projectName.length() == 0) {
                        File dbFile = mResourcesManager.getDatabaseFile();
                        String dbName = FileUtilities.getNameWithoutExtention(dbFile);
                        DaoMetadata.setValue(metadata.key, dbName);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getLocalizedMessage(), e);
            GPDialogs.toast(activity, R.string.databaseError, Toast.LENGTH_LONG);
        }
    }

    private void checkLogButton() {
        if (mGpslogButton != null) {
            Context context = getContext();
            if (context != null)
                if (mLastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
                    mGpslogButton.setBackgroundColor(ColorUtilities.getAccentColor(context));
                } else {
                    mGpslogButton.setBackgroundColor(ColorUtilities.getPrimaryColor(context));
                }
        }
    }

    private void handleGpsLogAction() {
        final GPApplication appContext = GeopaparazziApplication.getInstance();
        final FragmentActivity activity = getActivity();
        if (activity == null)
            return;
        if (mLastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
            GPDialogs.yesNoMessageDialog(getActivity(), getString(R.string.do_you_want_to_stop_logging),
                    () -> activity.runOnUiThread(() -> {
                        // stop logging
                        GpsServiceUtilities.stopDatabaseLogging(appContext);
                        mGpslogButton.setBackgroundColor(ColorUtilities.getPrimaryColor(activity));
                        GpsServiceUtilities.triggerBroadcast(getActivity());
                    }), null
            );

        } else {
            // start logging
            if (mLastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                final String defaultLogName = "log_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()); //$NON-NLS-1$

                GPDialogs.inputMessageAndCheckboxDialog(getActivity(), getString(R.string.gps_log_name),
                        defaultLogName, getString(R.string.continue_last_log), false, new TextAndBooleanRunnable() {
                            public void run() {
                                activity.runOnUiThread(() -> {
                                    String newName = theTextToRunOn;
                                    if (newName == null || newName.length() < 1) {
                                        newName = defaultLogName;
                                    }

                                    mGpslogButton.setBackgroundColor(ColorUtilities.getAccentColor(activity));
                                    GpsServiceUtilities.startDatabaseLogging(appContext, newName, theBooleanToRunOn,
                                            DefaultHelperClasses.GPSLOG_HELPER_CLASS);
                                    GpsServiceUtilities.triggerBroadcast(getActivity());
                                });
                            }
                        }
                );

            } else {
                GPDialogs.warningDialog(getActivity(), getString(R.string.gpslogging_only), null);
            }
        }
    }

    public BroadcastReceiver getGpsServiceBroadcastReceiver() {
        return mGpsServiceBroadcastReceiver;
    }

    @Override
    public FragmentManager getSupportFragmentManager() {
        return null;
    }
}
