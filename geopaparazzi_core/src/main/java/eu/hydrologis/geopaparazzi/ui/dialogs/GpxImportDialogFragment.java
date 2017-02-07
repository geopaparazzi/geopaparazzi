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

package eu.hydrologis.geopaparazzi.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.util.List;

import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gpx.GpxItem;
import eu.geopaparazzi.library.gpx.parser.GpxParser;
import eu.geopaparazzi.library.gpx.parser.WayPoint;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;


/**
 * Dialog for gpx files import.
 *
 * @author Andrea Antonello
 */
public class GpxImportDialogFragment extends DialogFragment {

    public static final String GPX_PATH = "gpxPath";
    private ProgressBar progressBar;
    private String gpxPath;

    private boolean isInterrupted = false;
    private AlertDialog alertDialog;
    private Button positiveButton;
    private StringAsyncTask task;


    public static GpxImportDialogFragment newInstance(String gpxPath) {
        GpxImportDialogFragment f = new GpxImportDialogFragment();
        Bundle args = new Bundle();
        args.putString(GPX_PATH, gpxPath);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gpxPath = getArguments().getString(GPX_PATH);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        View gpsinfoDialogView = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_dialog_progressbar, null);
        builder.setView(gpsinfoDialogView);
        builder.setMessage(R.string.gpx_import_processing);

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

    private void startImport() {
        task = new StringAsyncTask(getActivity()) {
            protected String doBackgroundWork() {
                try {
                    File file = new File(gpxPath);
                    String fileName = FileUtilities.getNameWithoutExtention(file);
                    GpxParser parser = new GpxParser(gpxPath);
                    if (parser.parse()) {
                        List<WayPoint> wayPoints = parser.getWayPoints();
                        if (wayPoints.size() > 0) {
                            GpxItem item = new GpxItem();
                            item.setName(fileName);
                            item.setWidth("2"); //$NON-NLS-1$
                            item.setVisible(false);
                            item.setColor(ColorUtilities.BLUE.getHex()); //$NON-NLS-1$
                            item.setData(wayPoints);
                            DaoGpsLog.importGpxToMap(item);
                        }
                        List<GpxParser.TrackSegment> tracks = parser.getTracks();
                        if (tracks.size() > 0) {
                            for (GpxParser.TrackSegment trackSegment : tracks) {
                                if (isInterrupted) break;
                                String tName = trackSegment.getName();
                                if (tName == null) {
                                    tName = ""; //$NON-NLS-1$
                                } else {
                                    tName = " - " + tName; //$NON-NLS-1$
                                }
                                String name = fileName + tName;
                                GpxItem item = new GpxItem();
                                item.setName(name);
                                item.setWidth("2"); //$NON-NLS-1$
                                item.setVisible(false);
                                item.setColor("red"); //$NON-NLS-1$
                                item.setData(trackSegment);
                                DaoGpsLog.importGpxToMap(item);
                            }
                        }
                        List<GpxParser.Route> routes = parser.getRoutes();
                        if (routes.size() > 0) {
                            for (GpxParser.Route route : routes) {
                                if (isInterrupted) break;
                                String rName = route.getName();
                                if (rName == null) {
                                    rName = ""; //$NON-NLS-1$
                                } else {
                                    rName = " - " + rName; //$NON-NLS-1$
                                }
                                String name = fileName + rName;
                                GpxItem item = new GpxItem();
                                item.setName(name);
                                item.setWidth("2"); //$NON-NLS-1$
                                item.setVisible(false);
                                item.setColor(ColorUtilities.GREEN.getHex()); //$NON-NLS-1$
                                item.setData(route);
                                DaoGpsLog.importGpxToMap(item);
                            }
                        }
                    } else {
                        if (GPLog.LOG)
                            GPLog.addLogEntry(this, "ERROR"); //$NON-NLS-1$
                    }

                } catch (IOException e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    return "ERROR: " + e.getLocalizedMessage();
                }

                return "";
            }

            protected void doUiPostWork(String response) {
                progressBar.setVisibility(View.GONE);
                if (response.length() != 0) {
                    alertDialog.setMessage(response);
                } else {
                    alertDialog.setMessage(getString(R.string.gpx_file_imported));
                    positiveButton.setEnabled(true);
                }
            }
        };
        task.execute();
    }

    @Override
    public void onDestroy() {
        if (task != null) task.dispose();

        super.onDestroy();
    }

    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);
        }
        startImport();
    }

}
