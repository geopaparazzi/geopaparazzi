// MainActivityFragment.java
// Contains the Flag Quiz logic
package eu.hydrologis.geopaparazzi.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Date;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.sensors.OrientationSensor;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.activities.AboutActivity;
import eu.hydrologis.geopaparazzi.activities.SettingsActivity;
import eu.hydrologis.geopaparazzi.providers.ProviderTestActivity;

public class GeopaparazziActivityFragment extends Fragment implements View.OnLongClickListener, View.OnClickListener {

    private ImageButton notesButton;
    private ImageButton metadataButton;
    private ImageButton mapviewButton;
    private MenuItem gpsMenuItem;
    private ImageButton gpslogButton;
    private ImageButton importButton;
    private OrientationSensor orientationSensor;
    private BroadcastReceiver gpsServiceBroadcastReceiver;

    private static boolean checkedGps = false;
    private GpsServiceStatus lastGpsServiceStatus;
    private int[] lastGpsStatusExtras;
    private GpsLoggingStatus lastGpsLoggingStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_geopaparazzi, container, false);

        // this fragment adds to the menu
        setHasOptionsMenu(true);

        // start gps service
        GpsServiceUtilities.startGpsService(getActivity());

        return v; // return the fragment's view for display
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notesButton = (ImageButton) view.findViewById(R.id.dashboardButtonNotes);
        notesButton.setOnLongClickListener(this);

        metadataButton = (ImageButton) view.findViewById(R.id.dashboardButtonMetadata);
        metadataButton.setOnClickListener(this);

        mapviewButton = (ImageButton) view.findViewById(R.id.dashboardButtonMapview);
        mapviewButton.setOnClickListener(this);

        gpslogButton = (ImageButton) view.findViewById(R.id.dashboardButtonGpslog);
        gpslogButton.setOnClickListener(this);

        importButton = (ImageButton) view.findViewById(R.id.dashboardButtonImport);
        importButton.setOnClickListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();

        GpsServiceUtilities.triggerBroadcast(getActivity());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (orientationSensor == null) {
            SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            orientationSensor = new OrientationSensor(sensorManager, null);
        }
        orientationSensor.register(getActivity(), SensorManager.SENSOR_DELAY_NORMAL);

        if (gpsServiceBroadcastReceiver == null) {
            gpsServiceBroadcastReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    onGpsServiceUpdate(intent);
                    checkFirstTimeGps(context);
                }
            };
        }
        GpsServiceUtilities.registerForBroadcasts(getActivity(), gpsServiceBroadcastReceiver);

    }

    // remove SourceUrlsFragmentListener when Fragment detached
    @Override
    public void onDetach() {
        super.onDetach();

        orientationSensor.unregister();
        GpsServiceUtilities.unregisterFromBroadcasts(getActivity(), gpsServiceBroadcastReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);

        gpsMenuItem = menu.getItem(3);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tilesource: {

            }
            case R.id.action_new: {

            }
            case R.id.action_load: {

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
            case R.id.action_about: {
                Intent intent = new Intent(getActivity(), AboutActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_exit: {
                getActivity().finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onLongClick(View v) {
        String tooltip = "blah";
        if (v == notesButton) {
            tooltip = "Available providers:";
            for (PackageInfo pack : getActivity().getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        Log.d("Example", "provider: " + provider.authority);
                        tooltip = tooltip + "\n" + provider.authority;
                    }
                }
            }


        }

        Snackbar.make(v, tooltip, Snackbar.LENGTH_LONG).show();

        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == metadataButton) {
            LineWidthDialogFragment widthDialog =
                    new LineWidthDialogFragment();
            widthDialog.show(getFragmentManager(), "line width dialog");
        } else if (v == mapviewButton) {
            ColorDialogFragment colorDialog = new ColorDialogFragment();
            colorDialog.show(getFragmentManager(), "color dialog");
        } else if (v == gpslogButton) {
            gpsMenuItem.setIcon(R.drawable.actionbar_gps_nofix);
        } else if (v == importButton) {
            Intent providerIntent = new Intent(getActivity(), ProviderTestActivity.class);
            startActivity(providerIntent);
        }

    }


    private void onGpsServiceUpdate(Intent intent) {
        lastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        lastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
        lastGpsStatusExtras = GpsServiceUtilities.getGpsStatusExtras(intent);
//        lastGpsPosition = GpsServiceUtilities.getPosition(intent);
//        lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
//        lastPositiontime = GpsServiceUtilities.getPositionTime(intent);


        boolean doLog = GPLog.LOG_HEAVY;
        if (doLog && lastGpsStatusExtras != null) {
            int satCount = lastGpsStatusExtras[1];
            int satForFixCount = lastGpsStatusExtras[2];
            GPLog.addLogEntry(this, "satellites: " + satCount + " of which for fix: " + satForFixCount);
        }

        if (gpsMenuItem != null)
            if (lastGpsServiceStatus != GpsServiceStatus.GPS_OFF) {
                if (doLog)
                    GPLog.addLogEntry(this, "GPS seems to be on");
                if (lastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
                    if (doLog)
                        GPLog.addLogEntry(this, "GPS seems to be also logging");
                    gpsMenuItem.setIcon(R.drawable.actionbar_gps_logging);
                } else {
                    if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                        if (doLog) {
                            GPLog.addLogEntry(this, "GPS has fix");
                        }
                        gpsMenuItem.setIcon(R.drawable.actionbar_gps_fix_nologging);
                    } else {
                        if (doLog) {
                            GPLog.addLogEntry(this, "GPS doesn't have a fix");
                        }
                        gpsMenuItem.setIcon(R.drawable.actionbar_gps_nofix);
                    }
                }
            } else {
                if (doLog)
                    GPLog.addLogEntry(this, "GPS seems to be off");
                gpsMenuItem.setIcon(R.drawable.actionbar_gps_off);
            }
    }

    private void checkFirstTimeGps(Context context) {
        if (!checkedGps) {
            checkedGps = true;
            if (lastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
                String prompt = getResources().getString(R.string.prompt_gpsenable);
                Utilities.yesNoMessageDialog(context, prompt, new Runnable() {
                    public void run() {
                        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(gpsOptionsIntent);
                    }
                }, null);
            }
        }
    }


}
