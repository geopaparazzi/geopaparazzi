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

package eu.hydrologis.geopaparazzi.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.kml.KmlRepresenter;
import eu.geopaparazzi.library.kml.KmzExport;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.webproject.WebProjectManager;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.objects.Bookmark;
import eu.hydrologis.geopaparazzi.database.objects.Line;
import eu.hydrologis.geopaparazzi.database.objects.LogMapItem;
import eu.hydrologis.geopaparazzi.database.objects.Note;


/**
 * Dialog for export to cloud (STAGE).
 *
 * @author Andrea Antonello
 */
public class StageExportDialogFragment extends DialogFragment {
    public static final String NODATA = "NODATA";

    public static final String KEY_URL = "KEY_URL";
    public static final String KEY_USER = "KEY_USER";
    public static final String KEY_PWD = "KEY_PWD";

    private ProgressBar progressBar;

    private AlertDialog alertDialog;
    private Button positiveButton;

    private String serverUrl;
    private String user;
    private String pwd;

    /**
     * Create a dialog instance.
     *
     * @param serverUrl the server url.
     * @param user      the username for the server.
     * @param pwd       the password.
     * @return the instance.
     */
    public static StageExportDialogFragment newInstance(final String serverUrl, final String user, final String pwd) {
        StageExportDialogFragment f = new StageExportDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_URL, serverUrl);
        args.putString(KEY_USER, user);
        args.putString(KEY_PWD, pwd);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serverUrl = getArguments().getString(KEY_URL);
        user = getArguments().getString(KEY_USER);
        pwd = getArguments().getString(KEY_PWD);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        View gpsinfoDialogView = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_dialog_progressbar, null);
        builder.setView(gpsinfoDialogView);
        builder.setMessage(R.string.exporting_data_to_the_cloud);

        progressBar = (ProgressBar) gpsinfoDialogView.findViewById(
                R.id.progressBar);

        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                }
        );

        progressBar.setIndeterminate(true);

        alertDialog = builder.create();
        return alertDialog;
    }

    private void startExport() {
        new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                try {
                    String message = WebProjectManager.INSTANCE.uploadProject(getActivity(), serverUrl, user, pwd);
                    return message;
                } catch (Exception e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    return "ERROR" + e.getLocalizedMessage();
                }
            }

            protected void onPostExecute(String response) {
                alertDialog.setMessage(response);
                positiveButton.setEnabled(true);
            }
        }.execute((String) null);
    }

    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);
        }
        startExport();
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
