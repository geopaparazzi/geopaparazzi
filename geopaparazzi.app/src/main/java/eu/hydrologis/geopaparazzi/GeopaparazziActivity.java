// MainActivity.java
// Hosts the GeopaparazziActivityFragment on a phone and both the
// GeopaparazziActivityFragment and SettingsActivityFragment on a tablet
package eu.hydrologis.geopaparazzi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.hydrologis.geopaparazzi.fragments.GeopaparazziActivityFragment;
import eu.hydrologis.geopaparazzi.utilities.IChainedPermissionHelper;
import eu.hydrologis.geopaparazzi.utilities.PermissionWriteStorage;

public class GeopaparazziActivity extends AppCompatActivity {
    private IChainedPermissionHelper permissionHelper = new PermissionWriteStorage();

    // configure the GeopaparazziActivity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_geopaparazzi);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // PERMISSIONS START
        if (permissionHelper.hasPermission(this) && permissionHelper.getNextWithoutPermission(this) == null) {
            init();
        } else {
            if (permissionHelper.hasPermission(this)) {
                permissionHelper = permissionHelper.getNextWithoutPermission(this);
            }
            permissionHelper.requestPermission(this);
        }
        // PERMISSIONS STOP
    }

    private void init() {

        // set default values in the app's SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (permissionHelper.hasGainedPermission(requestCode, grantResults)) {
            IChainedPermissionHelper nextWithoutPermission = permissionHelper.getNextWithoutPermission(this);
            permissionHelper = nextWithoutPermission;
            if (permissionHelper == null) {
                init();
            } else {
                permissionHelper.requestPermission(this);
            }

        } else {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(this);
            builder.setMessage("Geopaparazzi can't be started because the following permission was not granted: " + permissionHelper.getDescription());
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }
            );
            // display the dialog
            builder.create().show();


            Log.i("BLAH", "Exiting");
        }
    }

    // called after onCreate completes execution
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void finish() {
        try {
            GpsServiceUtilities.stopDatabaseLogging(this);
            GpsServiceUtilities.stopGpsService(this);
        } finally {
            super.finish();
        }
    }
}
