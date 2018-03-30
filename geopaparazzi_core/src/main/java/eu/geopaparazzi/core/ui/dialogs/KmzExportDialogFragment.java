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
import java.util.Map;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.kml.KmlRepresenter;
import eu.geopaparazzi.library.kml.KmzExport;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoBookmarks;
import eu.geopaparazzi.core.database.DaoGpsLog;
import eu.geopaparazzi.core.database.DaoImages;
import eu.geopaparazzi.core.database.DaoNotes;
import eu.geopaparazzi.core.database.objects.Bookmark;
import eu.geopaparazzi.core.database.objects.Line;
import eu.geopaparazzi.core.database.objects.LogMapItem;
import eu.geopaparazzi.core.database.objects.Note;


/**
 * Dialog for kmz files export.
 *
 * @author Andrea Antonello
 */
public class KmzExportDialogFragment extends DialogFragment {
    public static final String NODATA = "NODATA";
    public static final String KMZ_PATH = "exportPath";
    public static final String INTERRUPTED = "INTERRUPTED";
    private ProgressBar progressBar;
    private String exportPath;

    private boolean isInterrupted = false;
    private AlertDialog alertDialog;
    private Button positiveButton;


    /**
     * Create a dialog instance.
     *
     * @param exportPath an optional path to which to export the kmz to. If null, a default path is chosen.
     * @return the instance.
     */
    public static KmzExportDialogFragment newInstance(String exportPath) {
        KmzExportDialogFragment f = new KmzExportDialogFragment();
        Bundle args = new Bundle();
        args.putString(KMZ_PATH, exportPath);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exportPath = getArguments().getString(KMZ_PATH);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        View gpsinfoDialogView = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_dialog_progressbar, null);
        builder.setView(gpsinfoDialogView);
        builder.setMessage(R.string.exporting_data_to_kmz);

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
                File kmlOutputFile = null;
                try {
                    List<KmlRepresenter> kmlRepresenterList = new ArrayList<>();
                    boolean hasAtLeastOne = false;
                    /*
                     * add gps logs
                     */
                    List<LogMapItem> gpslogs = DaoGpsLog.getGpslogs();
                    HashMap<Long, LogMapItem> mapitemsMap = new HashMap<>();
                    for (LogMapItem log : gpslogs) {
                        if (isInterrupted) break;
                        mapitemsMap.put(log.getId(), log);
                        hasAtLeastOne = true;
                    }

                    HashMap<Long, Line> linesMap = DaoGpsLog.getLinesMap();
                    Collection<Map.Entry<Long, Line>> linesSet = linesMap.entrySet();
                    for (Map.Entry<Long, Line> lineEntry : linesSet) {
                        if (isInterrupted) break;
                        Long id = lineEntry.getKey();
                        Line line = lineEntry.getValue();
                        LogMapItem mapItem = mapitemsMap.get(id);
                        if (mapItem == null) continue;
                        float width = mapItem.getWidth();
                        String color = mapItem.getColor();
                        line.setStyle(width, color);
                        line.setName(mapItem.getName());
                        kmlRepresenterList.add(line);
                        hasAtLeastOne = true;
                    }
                    /*
                     * get notes
                     */
                    List<Note> notesList = DaoNotes.getNotesList(null, false);
                    for (Note note : notesList) {
                        if (isInterrupted) break;
                        kmlRepresenterList.add(note);
                        hasAtLeastOne = true;
                    }
                    /*
                     * add pictures
                     */
                    List<Image> imagesList = DaoImages.getImagesList(false, true);
                    for (Image image : imagesList) {
                        kmlRepresenterList.add(image);
                        hasAtLeastOne = true;
                    }

                    /*
                     * add bookmarks
                     */
                    List<Bookmark> bookmarksList = DaoBookmarks.getAllBookmarks();
                    for (Bookmark bookmark : bookmarksList) {
                        if (isInterrupted) break;
                        kmlRepresenterList.add(bookmark);
                        hasAtLeastOne = true;
                    }

                    if (!hasAtLeastOne) {
                        return NODATA;
                    }
                    if (isInterrupted) return INTERRUPTED;

                    File kmlExportDir = ResourcesManager.getInstance(getActivity()).getMainStorageDir();
                    String filename = "geopaparazzi_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()) + ".kmz"; //$NON-NLS-1$ //$NON-NLS-2$
                    kmlOutputFile = new File(kmlExportDir, filename);
                    if (exportPath != null) {
                        kmlOutputFile = new File(exportPath);
                    }
                    KmzExport export = new KmzExport(null, kmlOutputFile);
                    export.export(getActivity(), kmlRepresenterList);

                    return kmlOutputFile.getAbsolutePath();
                } catch (Exception e) {
                    // cleanup as it might be inconsistent
                    if (kmlOutputFile != null && kmlOutputFile.exists()) {
                        kmlOutputFile.delete();
                    }
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    return ""; //$NON-NLS-1$
                }
            }

            protected void onPostExecute(String response) { // on UI thread!
                progressBar.setVisibility(View.GONE);

                if (response.equals(NODATA)) {
                    String msg = context.getString(R.string.no_data_found_in_project_to_export);
                    alertDialog.setMessage(msg);
                } else if (response.equals(INTERRUPTED)) {
                    alertDialog.setMessage(context.getString(R.string.interrupted_by_user));
                } else if (response.length() > 0) {
                    String msg = context.getString(R.string.datasaved) + " " + response;
                    alertDialog.setMessage(msg);
                } else {
                    String msg = context.getString(R.string.data_nonsaved);
                    alertDialog.setMessage(msg);
                }
                if(positiveButton!=null)positiveButton.setEnabled(true);

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
