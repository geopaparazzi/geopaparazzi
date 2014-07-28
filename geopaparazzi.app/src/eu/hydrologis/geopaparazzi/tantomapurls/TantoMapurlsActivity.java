/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.tantomapurls;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.R;

/**
 * Client for Tanto's mapurls download service written by Giovanni Allegri.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TantoMapurlsActivity extends Activity implements OnClickListener {

    protected static final int CODE = 666;
    protected static final String KEY_DATA = "ARE_MAPURLS_DIRTY";
    /**
     * The result key.
     */
    public static String RESULT_KEY = "KEY_TANTO_RESULT";
    /**
     * The server baseurl.
     */
    public static String BASEURL = "http://mapurls.geopaparazzi.eu/mapurls/";

    private CheckBox useMapcenterCheckbox;
    private CheckBox useLimitCheckbox;
    private boolean oneAdded = false;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.tanto_mapurl_service);

        // avoid keyboard to popup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        useMapcenterCheckbox = (CheckBox) findViewById(R.id.useMapcenterButton);
        useMapcenterCheckbox.setChecked(true);

        useLimitCheckbox = (CheckBox) findViewById(R.id.uselimitButton);
        useLimitCheckbox.setChecked(true);

        Button queryButton = (Button) findViewById(R.id.tantoQueryButton);
        queryButton.setOnClickListener(this);

        TextView titleTextView = (TextView) findViewById(R.id.tanto_title);
        titleTextView.setMovementMethod(LinkMovementMethod.getInstance());
        titleTextView.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                Uri uri = Uri.parse("http://blog.spaziogis.it/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    public void onClick( View v ) {
        if (!NetworkUtilities.isNetworkAvailable(this)) {
            Utilities.messageDialog(this, R.string.available_only_with_network, null);
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean usePosition = useMapcenterCheckbox.isChecked();
        String positionStr = null;
        if (usePosition) {
            double[] position = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
            positionStr = "p=" + position[0] + "," + position[1];
        }

        boolean useLimit = useLimitCheckbox.isChecked();
        String limitStr = null;
        if (useLimit) {
            limitStr = "l=20";
        }

        EditText filterText = (EditText) findViewById(R.id.textfilterText);
        String filterStr = filterText.getText().toString();
        boolean useTextFilter = false;
        if (filterStr.length() > 0) {
            useTextFilter = true;
        }

        String relativeUrl = "";
        if (usePosition) {
            relativeUrl = relativeUrl + "&" + positionStr;
        }
        if (useTextFilter) {
            relativeUrl = relativeUrl + "&" + filterStr;
        }
        if (useLimit) {
            relativeUrl = relativeUrl + "&" + limitStr;
        }

        if (relativeUrl.length() > 0) {
            relativeUrl = "?" + relativeUrl.substring(1);
        }

        final Context context = this;

        final String getUrl = BASEURL + relativeUrl;

        final ProgressDialog importDialog = new ProgressDialog(this);
        importDialog.setCancelable(true);
        importDialog.setTitle("Downloading...");
        importDialog.setMessage("Requesting available services");
        importDialog.setCancelable(false);
        importDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        importDialog.setIndeterminate(true);
        importDialog.show();

        StringAsyncTask task = new StringAsyncTask(this){
            @Override
            protected void doUiPostWork( String response ) {
                Utilities.dismissProgressDialog(importDialog);
                if (response.startsWith("ERROR")) {
                    Utilities.messageDialog(context, response, null);
                } else {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(TantoMapurlsActivity.this);
                    Editor editor = preferences.edit();
                    String key = "MAPURLDATAPREFKEY";
                    editor.putString(key, response);
                    editor.commit();

                    Intent mapurlsIntent = new Intent(context, TantoMapurlsListActivity.class);
                    mapurlsIntent.putExtra(RESULT_KEY, key);
                    startActivityForResult(mapurlsIntent, CODE);
                }
            }

            @Override
            protected String doBackgroundWork() {
                String result;
                try {
                    result = NetworkUtilities.sendGetRequest(getUrl, null, null, null);
                } catch (Exception e) {
                    result = "ERROR: " + e.getLocalizedMessage();
                }
                return result;
            }
        };
        task.execute();
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        switch( requestCode ) {
        case CODE: {
            if (resultCode == Activity.RESULT_OK) {
                if (!oneAdded) {
                    oneAdded = data.getBooleanExtra(KEY_DATA, false);
                }
            }
            break;
        }
        }
    }

    @Override
    public void finish() {
        /*
         * reload mapurls if necessary
         */
        if (oneAdded) {
            new Thread(new Runnable(){
                public void run() {
                    try {
                        MapsDirManager.reset();
                        MapsDirManager.getInstance().init(GeopaparazziApplication.getInstance(), null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }

        TantoMapurlsActivity.super.finish();
    }

}
