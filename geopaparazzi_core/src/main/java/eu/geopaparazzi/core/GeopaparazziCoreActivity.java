// MainActivity.java
// Hosts the GeopaparazziActivityFragment on a phone and both the
// GeopaparazziActivityFragment and SettingsActivityFragment on a tablet
package eu.geopaparazzi.core;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;

import org.json.JSONException;

import java.io.IOException;

import eu.geopaparazzi.core.database.DaoBookmarks;
import eu.geopaparazzi.core.mapview.MapviewActivity;
import eu.geopaparazzi.core.ui.fragments.GeopaparazziActivityFragment;
import eu.geopaparazzi.core.utilities.IApplicationChangeListener;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.permissions.AChainedPermissionHelper;
import eu.geopaparazzi.library.permissions.PermissionFineLocation;
import eu.geopaparazzi.library.permissions.PermissionGetAccounts;
import eu.geopaparazzi.library.permissions.PermissionRecieveSms;
import eu.geopaparazzi.library.permissions.PermissionSendSms;
import eu.geopaparazzi.library.permissions.PermissionWriteStorage;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.SimplePosition;
import eu.geopaparazzi.library.util.UrlUtilities;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_DATABASE_TO_LOAD;

/**
 * Main activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopaparazziCoreActivity extends AppCompatActivity implements IApplicationChangeListener {
    private AChainedPermissionHelper permissionHelper = new PermissionWriteStorage();
    private GeopaparazziActivityFragment geopaparazziActivityFragment;

    // configure the GeopaparazziCoreActivity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        permissionHelper = new PermissionWriteStorage();
        permissionHelper.add(new PermissionFineLocation()).add(new PermissionSendSms()).add(new PermissionRecieveSms());

        checkIncomingProject();

        if (Build.VERSION.SDK_INT >= 23) {
            // PERMISSIONS START
            if (permissionHelper.hasPermission(this) && permissionHelper.getNextWithoutPermission(this) == null) {
                init();

                checkIncomingUrl();
                checkAvailableProfiles();
            } else {
                if (permissionHelper.hasPermission(this)) {
                    permissionHelper = permissionHelper.getNextWithoutPermission(this);
                }
                permissionHelper.requestPermission(this);
            }
            // PERMISSIONS STOP
        } else {
            init();

            checkIncomingUrl();
            checkAvailableProfiles();
        }

        setContentView(R.layout.activity_geopaparazzi);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    private void init() {

        // set default values in the app's SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        geopaparazziActivityFragment = new GeopaparazziActivityFragment();
        FragmentTransaction transaction =
                getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, geopaparazziActivityFragment);
        transaction.commitAllowingStateLoss();
    }

    private void checkIncomingProject() {
        Uri data = getIntent().getData();
        if (data != null) {
            String path = data.getEncodedPath();
            if (path.endsWith(LibraryConstants.GEOPAPARAZZI_DB_EXTENSION)) {
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PREFS_KEY_DATABASE_TO_LOAD, path);
                editor.apply();
            }
        }
    }

    private void checkAvailableProfiles() {
        try {
            ProfilesHandler.INSTANCE.checkActiveProfile(getContentResolver());

            BaseMapSourcesManager.INSTANCE.forceBasemapsreRead();
            SpatialiteSourcesManager.INSTANCE.forceSpatialitemapsreRead();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void checkIncomingUrl() {
        Uri data = getIntent().getData();
        if (data != null) {
            final String path = data.toString();
            // try osm
            final SimplePosition simplePosition = UrlUtilities.getLatLonTextFromOsmUrl(path);
            if (simplePosition.latitude != null) {
                GPDialogs.yesNoMessageDialog(this, getString(R.string.import_bookmark_prompt), new Runnable() {
                    @Override
                    public void run() {
                        GeopaparazziCoreActivity activity = GeopaparazziCoreActivity.this;
                        try {
                            DaoBookmarks.addBookmark(simplePosition.longitude, simplePosition.latitude, simplePosition.text, simplePosition.zoomLevel, -1, -1, -1, -1);
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                            PositionUtilities.putMapCenterInPreferences(preferences, simplePosition.longitude, simplePosition.latitude, 16);
                            Intent mapIntent = new Intent(activity, MapviewActivity.class);
                            startActivity(mapIntent);
                        } catch (IOException e) {
                            GPLog.error(this, "Error parsing URI: " + path, e); //$NON-NLS-1$
                            GPDialogs
                                    .warningDialog(
                                            activity,
                                            "Unable to parse the url: " + path,
                                            null);
                        }
                    }
                }, null);

            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (permissionHelper.hasGainedPermission(requestCode, grantResults)) {
            AChainedPermissionHelper nextWithoutPermission = permissionHelper.getNextWithoutPermission(this);
            permissionHelper = nextWithoutPermission;
            if (permissionHelper == null) {
                init();
            } else {
                permissionHelper.requestPermission(this);
            }

        } else {
            GPDialogs.infoDialog(this, getString(R.string.premissions_cant_start) + permissionHelper.getDescription(), new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }
    }

    // called after onCreate completes execution
    @Override
    protected void onStart() {
        super.onStart();
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // force to exit through the exit button
        // System.out.println(keyCode + "/" + KeyEvent.KEYCODE_BACK);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onApplicationNeedsRestart() {

        if (geopaparazziActivityFragment != null && geopaparazziActivityFragment.getGpsServiceBroadcastReceiver() != null) {
            GpsServiceUtilities.stopDatabaseLogging(this);
            GpsServiceUtilities.stopGpsService(this);
            GpsServiceUtilities.unregisterFromBroadcasts(this, geopaparazziActivityFragment.getGpsServiceBroadcastReceiver());
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 11) {
                    recreate();
                } else {
                    Intent intent = getIntent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    finish();
                    overridePendingTransition(0, 0);

                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
            }
        }, 10);
    }

    @Override
    public void onAppIsShuttingDown() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putFloat(MapviewActivity.MAPSCALE_X, 1);
        edit.putFloat(MapviewActivity.MAPSCALE_Y, 1);
        edit.apply();


        GpsServiceUtilities.stopDatabaseLogging(this);
        GpsServiceUtilities.stopGpsService(this);

        TagsManager.reset();
        GeopaparazziApplication.reset();
    }

}
