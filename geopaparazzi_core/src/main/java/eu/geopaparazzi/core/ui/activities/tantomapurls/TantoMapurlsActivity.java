/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.core.ui.activities.tantomapurls;

import android.app.Activity;
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

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.core.R;

/**
 * Client for Tanto's mapurls download service written by Giovanni Allegri.
 * <p/>
 * <p/>
 * <p/>
 * <p>Specs:</p>
 * <pre>
 * http://mapurls.geopaparazzi.eu/mapurlshtml/?l=25&o=0&fc=service!!!title&ft=Toscana!!!geodetici
 *
 * l: numero di risultati
 * o: offset da cui cominciare (insieme a "l" ti permette di fare richieste paginate)
 * fc: colonne di filtraggio (separate a !!!). Queste due nell'esempio dovrebbero bastarti,
 * la prima (service) è il nome del servizio, e "title" il nome del layer
 * ft: termini di filtraggio, uno per colonna
 *
 * Il tuo esempio sarebbe
 *
 * http://mapurls.geopaparazzi.eu/mapurls/?l=20&o=0&fc=service&ft=Toscana&p=11.521233,46.498944
 * </pre>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TantoMapurlsActivity extends Activity implements OnClickListener {

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
    private StringAsyncTask task;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_tantomapurl_service);

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
        titleTextView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uri uri = Uri.parse("http://blog.spaziogis.it/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    public void onClick(View v) {
        if (!NetworkUtilities.isNetworkAvailable(this)) {
            GPDialogs.infoDialog(this, getString(R.string.available_only_with_network), null);
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean usePosition = useMapcenterCheckbox.isChecked();
        String positionStr = null;
        if (usePosition) {
            double[] position = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
            if (position != null) {
                positionStr = "p=" + position[0] + "," + position[1];
            } else {
                positionStr = "";
            }
        }

        boolean useLimit = useLimitCheckbox.isChecked();
        String limitStr = null;
        if (useLimit) {
            limitStr = "l=20";
        }

        EditText filterText = (EditText) findViewById(R.id.textfilterText);
        String filterStr = filterText.getText().toString().trim();
        boolean useTextFilter = false;
        if (filterStr.length() > 0) {
            useTextFilter = true;
            filterStr = "fc=service&ft=" + filterStr;
            filterStr=filterStr.replaceAll("\\s+", "%20");
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

        //$NON-NLS-1$
        task = new StringAsyncTask(this) {
            @Override
            protected void doUiPostWork(String response) {
                if (response.startsWith("ERROR")) {
                    GPDialogs.warningDialog(context, response, null);
                } else {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(TantoMapurlsActivity.this);
                    Editor editor = preferences.edit();
                    String key = "MAPURLDATAPREFKEY";
                    editor.putString(key, response);
                    editor.apply();

                    Intent mapurlsIntent = new Intent(context, TantoMapurlsListActivity.class);
                    mapurlsIntent.putExtra(RESULT_KEY, key);
                    startActivity(mapurlsIntent);
                }
            }

            @Override
            protected String doBackgroundWork() {
                String result;
                try {
                    result = NetworkUtilities.sendGetRequest(getUrl, null, null, null);
                } catch (Exception e) {
                    GPLog.error(this, null, e); //$NON-NLS-1$
                    result = "ERROR: " + e.getLocalizedMessage();
                }
                return result;
            }
        };
        task.setProgressDialog(getString(R.string.downloading), getString(R.string.requesting_available_services), false, null);
        task.execute();
    }

    @Override
    protected void onDestroy() {
        if (task != null) task.dispose();
        super.onDestroy();
    }
}
