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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;

/**
 * Client for Tanto's mapurls download service written by Giovanni Allegri.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TantoMapurlsActivity extends Activity implements OnClickListener {

    private String BASEURL = "http://muttley.spaziogis.it:8001/mapurls/";
    private CheckBox useMapcenterCheckbox;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.tanto_mapurl_service);

        // avoid keyboard to popup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        useMapcenterCheckbox = (CheckBox) findViewById(R.id.useMapcenterButton);
        useMapcenterCheckbox.setChecked(true);

        Button queryButton = (Button) findViewById(R.id.tantoQueryButton);
        queryButton.setOnClickListener(this);
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
                    Utilities.messageDialog(context, response, null);
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
}
