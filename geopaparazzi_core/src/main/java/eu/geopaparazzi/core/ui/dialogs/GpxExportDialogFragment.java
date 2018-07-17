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

package eu.geopaparazzi.core.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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

import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.database.DaoMetadata;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gpx.GpxExport;
import eu.geopaparazzi.library.gpx.GpxRepresenter;
import eu.geopaparazzi.library.util.FileTypes;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoGpsLog;
import eu.geopaparazzi.core.database.DaoNotes;
import eu.geopaparazzi.core.database.objects.Line;
import eu.geopaparazzi.core.database.objects.Note;


/**
 * Dialog for gpx files export.
 *
 * @author Andrea Antonello
 */
public class GpxExportDialogFragment extends DialogFragment {
    public static final String NODATA = "NODATA";
    public static final String GPX_PATH = "exportPath";
    public static final String INTERRUPTED = "INTERRUPTED";
    private ProgressBar progressBar;
    private String exportPath;

    private boolean isInterrupted = false;
    private AlertDialog alertDialog;
    private Button positiveButton;


    /**
     * Create a dialog instance.
     *
     * @param exportPath an optional path to which to export the gpx to. If null, a default path is chosen.
     * @return the instance.
     */
    public static GpxExportDialogFragment newInstance(String exportPath) {
        GpxExportDialogFragment f = new GpxExportDialogFragment();
        Bundle args = new Bundle();
        args.putString(GPX_PATH, exportPath);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exportPath = getArguments().getString(GPX_PATH);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        View gpsinfoDialogView = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_dialog_progressbar, null);
        builder.setView(gpsinfoDialogView);
        builder.setMessage(R.string.exporting_data_to_gpx);

        progressBar = (ProgressBar) gpsinfoDialogView.findViewById(
                R.id.progressBar);

        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        isInterrupted = true;
                    }
                }
        );
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
        final Context context = getContext();

        new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                try {
                    List<GpxRepresenter> gpxRepresenterList = new ArrayList<>();
                    boolean hasAtLeastOne = false;
                    /*
                     * add gps logs
                     */
                    HashMap<Long, Line> linesMap = DaoGpsLog.getLinesMap();
                    Collection<Line> linesCollection = linesMap.values();
                    for (Line line : linesCollection) {
                        if (isInterrupted) break;
                        gpxRepresenterList.add(line);
                        hasAtLeastOne = true;
                    }
                    /*
                     * get notes
                     */
                    List<Note> notesList = DaoNotes.getNotesList(null, false);
                    for (Note note : notesList) {
                        if (isInterrupted) break;
                        gpxRepresenterList.add(note);
                        hasAtLeastOne = true;
                    }

                    if (!hasAtLeastOne) {
                        return NODATA;
                    }

                    if (isInterrupted) return INTERRUPTED;
                    String projectName = DaoMetadata.getProjectName();
                    if (projectName == null) {
                        projectName = "geopaparazzi_gpx_";
                    } else {
                        projectName += "_gpx_";
                    }
                    File exportDir = ResourcesManager.getInstance(GeopaparazziApplication.getInstance()).getApplicationExportDir();
                    File gpxOutputFile = new File(exportDir, projectName + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()) + ".gpx");
                    if (exportPath != null) {
                        gpxOutputFile = new File(exportPath);
                    }
                    GpxExport export = new GpxExport(null, gpxOutputFile);
                    export.export(getActivity(), gpxRepresenterList);

                    return gpxOutputFile.getAbsolutePath();
                } catch (Exception e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    return null;
                }
            }

            protected void onPostExecute(String response) { // on UI thread!
                progressBar.setVisibility(View.GONE);

                if (response.equals(NODATA)) {
                    String msg = context.getString(R.string.no_data_found_in_project_to_export);
                    alertDialog.setMessage(msg);
                    positiveButton.setEnabled(true);
                } else if (response.equals(INTERRUPTED)) {
                    alertDialog.setMessage("Interrupted by user");
                    positiveButton.setEnabled(true);
                } else if (response.length() > 0) {
                    String msg = context.getString(R.string.datasaved) + response;
                    alertDialog.setMessage(msg);
                    positiveButton.setEnabled(true);
                } else {
                    String msg = context.getString(R.string.data_nonsaved);
                    alertDialog.setMessage(msg);
                    positiveButton.setEnabled(true);
                }

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
