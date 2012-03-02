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
package eu.hydrologis.geopaparazzi.util;

import static eu.hydrologis.geopaparazzi.util.Constants.PREF_KEY_PWD;
import static eu.hydrologis.geopaparazzi.util.Constants.PREF_KEY_SERVER;
import static eu.hydrologis.geopaparazzi.util.Constants.PREF_KEY_USER;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.geopaparazzi.library.webproject.WebProjectManager;
import eu.geopaparazzi.library.webproject.Webproject;
import eu.hydrologis.geopaparazzi.R;

/**
 * Web projects listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WebProjectsListActivity extends ListActivity {
    private ArrayAdapter<Webproject> arrayAdapter;
    private EditText filterText;

    private List<Webproject> projectList = new ArrayList<Webproject>();
    private List<Webproject> projectListToLoad = new ArrayList<Webproject>();

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.webprojectlist);

        refreshList();

        filterText = (EditText) findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);

        new AsyncTask<String, Void, String>(){

            protected String doInBackground( String... params ) {
                WebProjectsListActivity context = WebProjectsListActivity.this;

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                final String user = preferences.getString(PREF_KEY_USER, ""); //$NON-NLS-1$
                final String pwd = preferences.getString(PREF_KEY_PWD, ""); //$NON-NLS-1$
                final String serverUrl = preferences.getString(PREF_KEY_SERVER, ""); //$NON-NLS-1$

                if (user.length() == 0 || pwd.length() == 0 || serverUrl.length() == 0) {
                    return "settings";
                }

                projectList = WebProjectManager.INSTANCE.downloadProjectList(context, serverUrl, user, pwd);
                projectListToLoad = projectList;
                return "";
            }

            protected void onPostExecute( String response ) { // on UI thread!
                WebProjectsListActivity context = WebProjectsListActivity.this;
                if (response.equals("settings")) {
                    Utilities.messageDialog(context, R.string.error_set_cloud_settings, null);
                } else {
                    refreshList();
                }
            }

        }.execute((String) null);

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    protected void onDestroy() {
        super.onDestroy();
        filterText.removeTextChangedListener(filterTextWatcher);
    }

    private void filterList( String filterText ) {
        if (Debug.D)
            Logger.d(this, "filter bookmarks list"); //$NON-NLS-1$

        projectListToLoad.clear();
        for( Webproject project : projectList ) {
            if (project.matches(filterText)) {
                projectListToLoad.add(project);
            }
        }

        refreshList();
    }

    private void refreshList() {
        if (Debug.D)
            Logger.d(this, "refreshing projects list"); //$NON-NLS-1$
        arrayAdapter = new ArrayAdapter<Webproject>(this, R.layout.bookmark_row, projectListToLoad){
            @Override
            public View getView( int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.webprojectsrow, null);

                TextView titleText = (TextView) rowView.findViewById(R.id.titletext);
                TextView descriptionText = (TextView) rowView.findViewById(R.id.descriptiontext);
                TextView authorText = (TextView) rowView.findViewById(R.id.authortext);
                TextView dateText = (TextView) rowView.findViewById(R.id.datetext);
                TextView sizeText = (TextView) rowView.findViewById(R.id.sizetext);

                Webproject webproject = projectList.get(position);
                titleText.setText(webproject.name);
                descriptionText.setText(webproject.title);
                authorText.setText(webproject.author);
                dateText.setText(webproject.date);
                int kbSize = (int) (webproject.size / 1024.0);
                sizeText.setText(kbSize + "Kb");

                return rowView;
            }

        };

        setListAdapter(arrayAdapter);
    }

    private TextWatcher filterTextWatcher = new TextWatcher(){

        public void afterTextChanged( Editable s ) {
        }

        public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
        }

        public void onTextChanged( CharSequence s, int start, int before, int count ) {
            // arrayAdapter.getFilter().filter(s);
            filterList(s.toString());
        }
    };

}
