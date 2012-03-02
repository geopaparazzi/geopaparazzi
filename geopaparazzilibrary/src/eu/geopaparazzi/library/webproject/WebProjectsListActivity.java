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
package eu.geopaparazzi.library.webproject;

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_PWD;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_URL;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_USER;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

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

    private String user;
    private String pwd;
    private String url;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.webprojectlist);

        user = icicle.getString(PREFS_KEY_USER);
        pwd = icicle.getString(PREFS_KEY_PWD);
        url = icicle.getString(PREFS_KEY_URL);

        filterText = (EditText) findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);

        new AsyncTask<String, Void, String>(){

            protected String doInBackground( String... params ) {
                WebProjectsListActivity context = WebProjectsListActivity.this;
                try {
                    projectList = WebProjectManager.INSTANCE.downloadProjectList(context, url, user, pwd);
                    projectListToLoad = projectList;
                    return "";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "error";
                }
            }

            protected void onPostExecute( String response ) { // on UI thread!
                WebProjectsListActivity context = WebProjectsListActivity.this;
                if (response.equals("error")) {
                    Utilities.messageDialog(context, "An error occurred while retrieving the projects list.", null);
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
        arrayAdapter = new ArrayAdapter<Webproject>(this, R.layout.webprojectsrow, projectListToLoad){
            @Override
            public View getView( int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.webprojectsrow, null);

                TextView titleText = (TextView) rowView.findViewById(R.id.titletext);
                TextView descriptionText = (TextView) rowView.findViewById(R.id.descriptiontext);
                TextView authorText = (TextView) rowView.findViewById(R.id.authortext);
                TextView dateText = (TextView) rowView.findViewById(R.id.datetext);
                TextView sizeText = (TextView) rowView.findViewById(R.id.sizetext);

                final Webproject webproject = projectList.get(position);
                titleText.setText(webproject.name);
                descriptionText.setText(webproject.title);
                authorText.setText(webproject.author);
                dateText.setText(webproject.date);
                int kbSize = (int) (webproject.size / 1024.0);
                sizeText.setText(kbSize + "Kb");

                ImageView imageText = (ImageView) rowView.findViewById(R.id.downloadproject_image);
                imageText.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        downloadProject(webproject);
                    }
                });
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

    private void downloadProject( final Webproject webproject ) {
        final ProgressDialog cloudProgressDialog = ProgressDialog.show(this, "Downloading project...",
                "Downloading selected project to the device.", true, true);
        new AsyncTask<String, Void, Integer>(){
            protected Integer doInBackground( String... params ) {
                try {
                    url = url + webproject.id;
                    ReturnCodes returnCode = WebProjectManager.INSTANCE.downloadProject(WebProjectsListActivity.this, url, user,
                            pwd);
                    return returnCode.getMsgCode();
                } catch (Exception e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    return ReturnCodes.ERROR.getMsgCode();
                }
            }

            protected void onPostExecute( Integer response ) { // on UI thread!
                cloudProgressDialog.dismiss();
                ReturnCodes code = ReturnCodes.get4Code(response);
                String msg;
                if (code == ReturnCodes.ERROR) {
                    msg = "An error occurred while downloading the project.";
                } else {
                    msg = "The project has been successfully downloaded.";
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(WebProjectsListActivity.this);
                builder.setMessage(msg).setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                            public void onClick( DialogInterface dialog, int id ) {
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        }.execute((String) null);
    }
}
