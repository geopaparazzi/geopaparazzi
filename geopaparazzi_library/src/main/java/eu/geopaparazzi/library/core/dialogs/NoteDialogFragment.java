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

package eu.geopaparazzi.library.core.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.GPDialogs;

import static eu.geopaparazzi.library.util.LibraryConstants.ELEVATION;
import static eu.geopaparazzi.library.util.LibraryConstants.LATITUDE;
import static eu.geopaparazzi.library.util.LibraryConstants.LONGITUDE;

/**
 * New project creation dialog.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NoteDialogFragment extends DialogFragment {
    private EditText noteText;
    private double latitude;
    private double longitude;
    private double elevation;
    private IAddNote iAddNote;
    private CheckBox alsoBookmarkCheck;

    /**
     * Interface to allow adding notes from non library classes.
     */
    public interface IAddNote {
        /**
         * @param lon            the longitude of the point.
         * @param lat            the latitude of the point.
         * @param elev           the elevation of the point.
         * @param timestamp      the timestamp of the point.
         * @param note           the text of the note.
         * @param alsoAsBookmark if true, then the note is also saved to bookmarks.
         */
        void addNote(double lon, double lat, double elev, long timestamp, String note, boolean alsoAsBookmark);
    }

    public static NoteDialogFragment newInstance(double lon, double lat, double elev) {
        NoteDialogFragment f = new NoteDialogFragment();
        Bundle args = new Bundle();
        args.putDouble(LONGITUDE, lon);
        args.putDouble(LATITUDE, lat);
        args.putDouble(ELEVATION, elev);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        latitude = arguments.getDouble(LATITUDE);
        longitude = arguments.getDouble(LONGITUDE);
        elevation = arguments.getDouble(ELEVATION);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        try {

            View newProjectDialogView = getActivity().getLayoutInflater().inflate(
                    R.layout.fragment_dialog_note, null);
            builder.setView(newProjectDialogView); // add GUI to dialog

            noteText = newProjectDialogView.findViewById(R.id.noteentry);
            alsoBookmarkCheck = newProjectDialogView.findViewById(R.id.alsoBookmarksCheck);

            builder.setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                long ts = System.currentTimeMillis();
                                String noteString = noteText.getText().toString();
                                boolean alsoAsBookmark = alsoBookmarkCheck.isChecked();

                                if (iAddNote != null)
                                    iAddNote.addNote(longitude, latitude, elevation, ts, noteString, alsoAsBookmark);

                            } catch (Exception e) {
                                GPLog.error(this, e.getLocalizedMessage(), e);
                                GPDialogs.warningDialog(getActivity(), getString(R.string.notenonsaved), null);
                            }
                        }
                    }
            );

            builder.setNegativeButton(getString(android.R.string.cancel),
                    (dialog, id) -> {
                    }
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof IAddNote) {
            iAddNote = (IAddNote) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        iAddNote = null;
    }
}
