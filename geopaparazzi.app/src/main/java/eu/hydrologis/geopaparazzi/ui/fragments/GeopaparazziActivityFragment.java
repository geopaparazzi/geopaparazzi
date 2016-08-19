// MainActivityFragment.java
// Contains the Flag Quiz logic
package eu.hydrologis.geopaparazzi.ui.fragments;

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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.DatabaseUtilities;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.GPLogPreferencesHandler;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.sensors.OrientationSensor;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.FileTypes;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivityStarter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TextAndBooleanRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import eu.geopaparazzi.mapsforge.sourcesview.SourcesTreeListActivity;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoMetadata;
import eu.hydrologis.geopaparazzi.database.TableDescriptions;
import eu.hydrologis.geopaparazzi.database.objects.Metadata;
import eu.hydrologis.geopaparazzi.mapview.MapviewActivity;
import eu.hydrologis.geopaparazzi.ui.activities.AboutActivity;
import eu.hydrologis.geopaparazzi.ui.activities.AddNotesActivity;
import eu.hydrologis.geopaparazzi.ui.activities.AdvancedSettingsActivity;
import eu.hydrologis.geopaparazzi.ui.activities.ExportActivity;
import eu.hydrologis.geopaparazzi.ui.activities.ImportActivity;
import eu.hydrologis.geopaparazzi.ui.activities.PanicActivity;
import eu.hydrologis.geopaparazzi.ui.activities.ProjectMetadataActivity;
import eu.hydrologis.geopaparazzi.ui.activities.SettingsActivity;
import eu.hydrologis.geopaparazzi.ui.dialogs.GpsInfoDialogFragment;
import eu.hydrologis.geopaparazzi.ui.dialogs.NewProjectDialogFragment;
import eu.hydrologis.geopaparazzi.utilities.Constants;
import eu.hydrologis.geopaparazzi.utilities.IApplicationChangeListener;

import static eu.geopaparazzi.library.util.LibraryConstants.MAPSFORGE_EXTRACTED_DB_NAME;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_DATABASE_TO_LOAD;

/**
 * The fragment of the main geopap view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopaparazziActivityFragment extends Fragment implements View.OnLongClickListener, View.OnClickListener, IActivityStarter {

    private final int RETURNCODE_BROWSE_FOR_NEW_PREOJECT = 665;

    private ImageButton mNotesButton;
    private ImageButton mMetadataButton;
    private ImageButton mMapviewButton;
    private ImageButton mGpslogButton;
    private ImageButton mExportButton;

    private ImageButton mImportButton;

    private MenuItem mGpsMenuItem;
    private OrientationSensor mOrientationSensor;
    private IApplicationChangeListener appChangeListener;

    private BroadcastReceiver mGpsServiceBroadcastReceiver;
    private static boolean sCheckedGps = false;
    private GpsServiceStatus mLastGpsServiceStatus;
    private int[] mLastGpsStatusExtras;
    private GpsLoggingStatus mLastGpsLoggingStatus = GpsLoggingStatus.GPS_DATABASELOGGING_OFF;
    private double[] mLastGpsPosition;
    private FloatingActionButton mPanicFAB;
    private ResourcesManager mResourcesManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_geopaparazzi, container, false);

        // TODO remove after foss4g

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean do_foss4G_screen = preferences.getBoolean("DO_FOSS4G_SCREEN", true);
        final View foss4GLayout = v.findViewById(R.id.foss4gLayout);
        if (do_foss4G_screen) {
            ImageView foss4GImg = (ImageView) v.findViewById(R.id.foss4g);
            foss4GImg.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setData(Uri.parse("http://2016.foss4g.org/ws20.html"));
                    startActivity(intent);
                    foss4GLayout.setVisibility(View.GONE);
                }
            });
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("DO_FOSS4G_SCREEN", false);
            editor.apply();
        } else {
            foss4GLayout.setVisibility(View.GONE);
        }


        // this fragment adds to the menu
        setHasOptionsMenu(true);

        Profile activeProfile = ProfilesHandler.INSTANCE.getActiveProfile();
        if (activeProfile != null) {
            if (activeProfile.projectPath != null && new File(activeProfile.projectPath).exists()) {
                View dashboardView = v.findViewById(R.id.dashboardLayout);
                String color = activeProfile.color;
                if (color != null) {
                    dashboardView.setBackgroundColor(ColorUtilities.toColor(color));
                }
            }
        }

        try {
            initializeResourcesManager();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // start gps service
        GpsServiceUtilities.startGpsService(getActivity());


        // start map reading in background
        new Thread(new Runnable() {
            @Override
            public void run() {
                BaseMapSourcesManager.INSTANCE.getBaseMaps();
            }
        }).start();
        return v; // return the fragment's view for display
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNotesButton = (ImageButton) view.findViewById(R.id.dashboardButtonNotes);
        mNotesButton.setOnClickListener(this);
        mNotesButton.setOnLongClickListener(this);

        mMetadataButton = (ImageButton) view.findViewById(R.id.dashboardButtonMetadata);
        mMetadataButton.setOnClickListener(this);
        mMetadataButton.setOnLongClickListener(this);

        mMapviewButton = (ImageButton) view.findViewById(R.id.dashboardButtonMapview);
        mMapviewButton.setOnClickListener(this);
        mMapviewButton.setOnLongClickListener(this);

        mGpslogButton = (ImageButton) view.findViewById(R.id.dashboardButtonGpslog);
        mGpslogButton.setOnClickListener(this);
        mGpslogButton.setOnLongClickListener(this);

        mImportButton = (ImageButton) view.findViewById(R.id.dashboardButtonImport);
        mImportButton.setOnClickListener(this);
        mImportButton.setOnLongClickListener(this);

        mExportButton = (ImageButton) view.findViewById(R.id.dashboardButtonExport);
        mExportButton.setOnClickListener(this);
        mExportButton.setOnLongClickListener(this);

        mPanicFAB = (FloatingActionButton) view.findViewById(R.id.panicActionButton);
        mPanicFAB.setOnClickListener(this);
        enablePanic(false);


    }


    @Override
    public void onResume() {
        super.onResume();

        GpsServiceUtilities.triggerBroadcast(getActivity());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof IApplicationChangeListener) {
            appChangeListener = (IApplicationChangeListener) context;
        }

        if (mOrientationSensor == null) {
            SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            mOrientationSensor = new OrientationSensor(sensorManager, null);
        }
        mOrientationSensor.register(getActivity(), SensorManager.SENSOR_DELAY_NORMAL);

        if (mGpsServiceBroadcastReceiver == null) {
            mGpsServiceBroadcastReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    onGpsServiceUpdate(intent);
                    checkFirstTimeGps(context);
                }
            };
        }
        GpsServiceUtilities.registerForBroadcasts(getActivity(), mGpsServiceBroadcastReceiver);

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

        mGpsMenuItem = menu.getItem(3);

        Profile activeProfile = ProfilesHandler.INSTANCE.getActiveProfile();
        if (activeProfile != null) {
            if (activeProfile.projectPath != null && new File(activeProfile.projectPath).exists()) {
                // hide new project and open project
                menu.getItem(1).setVisible(false);
                menu.getItem(2).setVisible(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tilesource: {
                Intent preferencesIntent = new Intent(this.getActivity(), SourcesTreeListActivity.class);
                startActivity(preferencesIntent);
                return true;
            }
            case R.id.action_new: {
                NewProjectDialogFragment newProjectDialogFragment = new NewProjectDialogFragment();
                newProjectDialogFragment.show(getFragmentManager(), "new project dialog");
                return true;
            }
            case R.id.action_load: {
                try {
                    String title = getString(R.string.select_gpap_file);
                    AppsUtilities.pickFile(this, RETURNCODE_BROWSE_FOR_NEW_PREOJECT, title, new String[]{FileTypes.GPAP.getExtension()}, null);
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                }
                return true;
            }
            case R.id.action_gps: {
                GpsInfoDialogFragment gpsInfoDialogFragment = new GpsInfoDialogFragment();
                gpsInfoDialogFragment.show(getFragmentManager(), "gpsinfo dialog");
                return true;
            }
            case R.id.action_gpsstatus: {
                // open gps status app
                AppsUtilities.checkAndOpenGpsStatus(getActivity());
                return true;
            }
            case R.id.action_settings: {
                Intent preferencesIntent = new Intent(this.getActivity(), SettingsActivity.class);
                startActivity(preferencesIntent);
                return true;
            }
            case R.id.action_advanced_settings: {
                Intent advancedSettingsIntent = new Intent(this.getActivity(), AdvancedSettingsActivity.class);
                startActivity(advancedSettingsIntent);
                return true;
            }
            case R.id.action_about: {
                Intent intent = new Intent(getActivity(), AboutActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_exit: {

                appChangeListener.onAppIsShuttingDown();
                getActivity().finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (RETURNCODE_BROWSE_FOR_NEW_PREOJECT): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String filePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                        if (filePath == null) return;
                        if (!filePath.endsWith(FileTypes.GPAP.getExtension())) {
                            GPDialogs.warningDialog(getActivity(), getActivity().getString(R.string.selected_file_is_no_geopap_project), null);
                            return;
                        }
                        File file = new File(filePath);
                        if (file.exists()) {
                            Utilities.setLastFilePath(getActivity(), filePath);
                            try {
                                DatabaseUtilities.setNewDatabase(getActivity(), GeopaparazziApplication.getInstance(), file.getAbsolutePath());

                                if (appChangeListener != null) {
                                    appChangeListener.onApplicationNeedsRestart();
                                }
                            } catch (IOException e) {
                                GPLog.error(this, null, e);
                                GPDialogs.warningDialog(getActivity(), getActivity().getString(R.string.error_while_setting_project), null);
                            }
                        }
                    } catch (Exception e) {
                        GPDialogs.errorDialog(getActivity(), e, null);
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
            Snackbar.make(v, tooltip, Snackbar.LENGTH_SHORT).show();
            return true;
        }


        if (v == mNotesButton) {
            String tooltip = "Available providers:";
            for (PackageInfo pack : getActivity().getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        Log.d("Example", "provider: " + provider.authority);
                        tooltip = tooltip + "\n" + provider.authority;
                    }
                }
            }
            Snackbar.make(v, tooltip, Snackbar.LENGTH_SHORT).show();
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

    private void onGpsServiceUpdate(Intent intent) {
        mLastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        mLastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
        mLastGpsStatusExtras = GpsServiceUtilities.getGpsStatusExtras(intent);
        mLastGpsPosition = GpsServiceUtilities.getPosition(intent);
//        lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
//        lastPositiontime = GpsServiceUtilities.getPositionTime(intent);


        boolean doLog = GPLog.LOG_HEAVY;
        if (doLog && mLastGpsStatusExtras != null) {
            int satCount = mLastGpsStatusExtras[1];
            int satForFixCount = mLastGpsStatusExtras[2];
            GPLog.addLogEntry(this, "satellites: " + satCount + " of which for fix: " + satForFixCount);
        }

        if (mGpsMenuItem != null)
            if (mLastGpsServiceStatus != GpsServiceStatus.GPS_OFF) {
                if (doLog)
                    GPLog.addLogEntry(this, "GPS seems to be on");
                if (mLastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
                    if (doLog)
                        GPLog.addLogEntry(this, "GPS seems to be also logging");
                    mGpsMenuItem.setIcon(R.drawable.actionbar_gps_logging);
                    enablePanic(true);
                } else {
                    if (mLastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                        if (doLog) {
                            GPLog.addLogEntry(this, "GPS has fix");
                        }
                        mGpsMenuItem.setIcon(R.drawable.actionbar_gps_fix_nologging);
                        enablePanic(true);
                    } else {
                        if (doLog) {
                            GPLog.addLogEntry(this, "GPS doesn't have a fix");
                        }
                        mGpsMenuItem.setIcon(R.drawable.actionbar_gps_nofix);
                        enablePanic(false);
                    }
                }
            } else {
                if (doLog)
                    GPLog.addLogEntry(this, "GPS seems to be off");
                mGpsMenuItem.setIcon(R.drawable.actionbar_gps_off);
                enablePanic(false);
            }
    }

    private void checkFirstTimeGps(Context context) {
        if (!sCheckedGps) {
            sCheckedGps = true;
            if (mLastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
                String prompt = getResources().getString(R.string.prompt_gpsenable);
                GPDialogs.yesNoMessageDialog(context, prompt, new Runnable() {
                    public void run() {
                        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(gpsOptionsIntent);
                    }
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
        ResourcesManager.resetManager();
        mResourcesManager = ResourcesManager.getInstance(getContext());

        if (mResourcesManager == null) {
            GPDialogs.yesNoMessageDialog(getActivity(), getString(eu.hydrologis.geopaparazzi.R.string.no_sdcard_use_internal_memory),
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
                    }, new Runnable() {
                        public void run() {
                            getActivity().finish();
                        }
                    }
            );
        } else {
            // create the default mapsforge data extraction db
            File applicationSupporterDir = mResourcesManager.getApplicationSupporterDir();
            File newDbFile = new File(applicationSupporterDir, MAPSFORGE_EXTRACTED_DB_NAME);
            if (!newDbFile.exists()) {
                AssetManager assetManager = getActivity().getAssets();
                InputStream inputStream = assetManager.open(MAPSFORGE_EXTRACTED_DB_NAME);
                FileUtilities.copyFile(inputStream, new FileOutputStream(newDbFile));
            }
            // initialize rest of resources
            initIfOk();
        }
    }


    private void initIfOk() {
        if (mResourcesManager == null) {
            GPDialogs.warningDialog(getActivity(), getString(R.string.sdcard_notexist), new Runnable() {
                public void run() {
                    getActivity().finish();
                }
            });
            return;
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
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
            GPDialogs.toast(getActivity(), R.string.databaseError, Toast.LENGTH_LONG);
        }
    }

    private void checkLogButton() {
        if (mGpslogButton != null)
            if (mLastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
                mGpslogButton.setBackgroundColor(ColorUtilities.getAccentColor(getContext()));
            } else {
                mGpslogButton.setBackgroundColor(ColorUtilities.getPrimaryColor(getContext()));
            }
    }

    private void handleGpsLogAction() {
        final GPApplication appContext = GeopaparazziApplication.getInstance();
        if (mLastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
            GPDialogs.yesNoMessageDialog(getActivity(), getString(R.string.do_you_want_to_stop_logging),
                    new Runnable() {
                        public void run() {
                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    // stop logging
                                    GpsServiceUtilities.stopDatabaseLogging(appContext);
                                    mGpslogButton.setBackgroundColor(ColorUtilities.getPrimaryColor(getContext()));
                                    GpsServiceUtilities.triggerBroadcast(getActivity());
                                }
                            });
                        }
                    }, null
            );

        } else {
            // start logging
            if (mLastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                final String defaultLogName = "log_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()); //$NON-NLS-1$

                GPDialogs.inputMessageAndCheckboxDialog(getActivity(), getString(R.string.gps_log_name),
                        defaultLogName, getString(R.string.continue_last_log), false, new TextAndBooleanRunnable() {
                            public void run() {
                                getActivity().runOnUiThread(new Runnable() {
                                    public void run() {
                                        String newName = theTextToRunOn;
                                        if (newName == null || newName.length() < 1) {
                                            newName = defaultLogName;
                                        }

                                        mGpslogButton.setBackgroundColor(ColorUtilities.getAccentColor(getContext()));
                                        GpsServiceUtilities.startDatabaseLogging(appContext, newName, theBooleanToRunOn,
                                                DefaultHelperClasses.GPSLOG_HELPER_CLASS);
                                        GpsServiceUtilities.triggerBroadcast(getActivity());
                                    }
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
}
