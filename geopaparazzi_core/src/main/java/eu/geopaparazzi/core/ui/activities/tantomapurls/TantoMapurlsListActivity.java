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

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.TextRunnable;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import eu.geopaparazzi.core.R;

/**
 * Web projects listing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TantoMapurlsListActivity extends ListActivity {
    private ArrayAdapter<TantoMapurl> arrayAdapter;
    private EditText filterText;

    private List<TantoMapurl> mapurlsList = new ArrayList<>();
    private List<TantoMapurl> mapurlsToLoad = new ArrayList<>();
    private StringAsyncTask task;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_tantomapurl_list);

        // avoid keyboard to popup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Bundle extras = getIntent().getExtras();
        String layersJsonKey = extras.getString(TantoMapurlsActivity.RESULT_KEY);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String layersJson = preferences.getString(layersJsonKey, "");
        Editor editor = preferences.edit();
        editor.remove(layersJsonKey);
        editor.apply();
        try {
            mapurlsList.clear();
            JSONArray baseArray = new JSONArray(layersJson);
            for (int i = 0; i < baseArray.length(); i++) {
                JSONObject layerObj = baseArray.getJSONObject(i);
                int id = layerObj.getInt("id");
                String layerTitle = layerObj.getString("title");
                String serviceName = layerObj.getString("service");

                TantoMapurl tantoMapurl = new TantoMapurl();
                tantoMapurl.id = id;
                tantoMapurl.service = serviceName;
                tantoMapurl.title = layerTitle;
                mapurlsList.add(tantoMapurl);
            }
            mapurlsToLoad.addAll(mapurlsList);
        } catch (JSONException e1) {
            GPLog.error(this, null, e1);
        }

        filterText = (EditText) findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);

        refreshList();

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    protected void onDestroy() {
        if (task != null) task.dispose();
        filterText.removeTextChangedListener(filterTextWatcher);
        super.onDestroy();
    }

    private void filterList(String filterText) {
        if (GPLog.LOG)
            GPLog.addLogEntry(this, "filter projects list"); //$NON-NLS-1$

        mapurlsToLoad.clear();
        if (filterText.length() == 0) {
            mapurlsToLoad.addAll(mapurlsList);
        } else {
            for (TantoMapurl project : mapurlsList) {
                if (project.matches(filterText)) {
                    mapurlsToLoad.add(project);
                }
            }
        }
        refreshList();
    }

    private void refreshList() {
        if (GPLog.LOG)
            GPLog.addLogEntry(this, "refreshing projects list"); //$NON-NLS-1$
        arrayAdapter = new ArrayAdapter<TantoMapurl>(this, R.layout.activity_tantomapurl_row, mapurlsToLoad) {
            @Override
            public View getView(int position, View cView, ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.activity_tantomapurl_row, null);

                TextView titleText = (TextView) rowView.findViewById(R.id.tantolayertitletext);
                TextView serviceText = (TextView) rowView.findViewById(R.id.tantoservicetext);
                TextView idText = (TextView) rowView.findViewById(R.id.tantoidtext);

                final TantoMapurl tantoMapurl = mapurlsToLoad.get(position);
                titleText.setText(tantoMapurl.title);
                serviceText.setText(tantoMapurl.service);
                idText.setText(tantoMapurl.id + "");

                ImageButton imageButton =  rowView.findViewById(R.id.downloadproject_image);
                imageButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (!NetworkUtilities.isNetworkAvailable(TantoMapurlsListActivity.this)) {
                            GPDialogs.infoDialog(TantoMapurlsListActivity.this, getString(R.string.available_only_with_network), null);
                            return;
                        }

                        GPDialogs.inputMessageDialog(TantoMapurlsListActivity.this,
                                getString(R.string.service_name_propmt), "", new TextRunnable() {
                                    public void run() {
                                        downloadProject(tantoMapurl, theTextToRunOn);
                                    }
                                });

                    }
                });
                return rowView;
            }

        };

        setListAdapter(arrayAdapter);
    }

    private TextWatcher filterTextWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
            // ignore
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // ignore
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // arrayAdapter.getFilter().filter(s);
            filterList(s.toString());
        }
    };

    private void downloadProject(final TantoMapurl tantoMapurl, final String fileName) {
        // String mapurlFileNameBkp = "tanto_mapurls_" + tantoMapurl.id + ".mapurl";
        task = new StringAsyncTask(this) {

            @Override
            protected String doBackgroundWork() {
                try {
                    String url = TantoMapurlsActivity.BASEURL + tantoMapurl.id + "/download";
                    // String mapurlFileNameBkp = "tanto_mapurls_" + tantoMapurl.id + ".mapurl";
                    String mapurlFileName;
                    if (fileName.trim().length() > 0) {
                        mapurlFileName = fileName.trim() + ".mapurl";
                    } else {
                        mapurlFileName = "tanto_" + tantoMapurl.title + ".mapurl";
                    }

                    File appSupporterDir = ResourcesManager.getInstance(TantoMapurlsListActivity.this).getApplicationSupporterDir();
                    File appsupporterFile = new File(appSupporterDir, mapurlFileName);
                    if (!appsupporterFile.getParentFile().exists()) {
                        appsupporterFile.getParentFile().mkdirs();
                    }
                    File writtenFile = NetworkUtilities.sendGetRequest4File(url, appsupporterFile, null, null, null);

                    BaseMapSourcesManager.INSTANCE.addBaseMapsFromFile(writtenFile);

                    return writtenFile.getName();
                } catch (Exception e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    return "ERROR:" + e.getMessage();
                }
            }

            @Override
            protected void doUiPostWork(String response) {
                if (response.startsWith("ERROR")) {
                    GPDialogs.warningDialog(TantoMapurlsListActivity.this, response, null);
                } else {
                    String okMsg = getString(R.string.mapurl_successfully_downloaded) + " (" + response + ")";
                    GPDialogs.infoDialog(TantoMapurlsListActivity.this, okMsg, null);
                }
            }
        };
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                task.setProgressDialog(getString(R.string.downloading), getString(R.string.downloading_mapurl_to_the_device), false, null);
                task.execute();
            }
        });
    }


}
