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
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.GPDialogs;

/**
 * New project creation dialog.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class InsertCoordinatesDialogFragment extends DialogFragment {
    public static final String TITLE = "TITLE";//NON-NLS
    private EditText longitudeText;
    private IInsertCoordinateListener insertCoordinateListener;
    private String title;
    private EditText latitudeText;

    /**
     * Interface to allow adding notes from non library classes.
     */
    public interface IInsertCoordinateListener {
        void onCoordinateInserted(double lon, double lat);
    }

    public static InsertCoordinatesDialogFragment newInstance(String title) {
        InsertCoordinatesDialogFragment f = new InsertCoordinatesDialogFragment();
        if (title != null) {
            Bundle args = new Bundle();
            args.putString(TITLE, title);
            f.setArguments(args);
        }
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null)
            title = arguments.getString(TITLE, getString(R.string.askcoord));
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        try {

            View newProjectDialogView = getActivity().getLayoutInflater().inflate(
                    R.layout.fragment_dialog_insertcoordinate, null);
            builder.setView(newProjectDialogView); // add GUI to dialog
            builder.setTitle(title);

            longitudeText = newProjectDialogView.findViewById(R.id.longitudetext);
            latitudeText = newProjectDialogView.findViewById(R.id.latitudetext);

            builder.setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String lonString = String.valueOf(longitudeText.getText());
                            double lon;
                            try {
                                lon = Double.parseDouble(lonString);
                                if (lon < -180 || lon > 180) {
                                    throw new Exception();
                                }
                            } catch (Exception e1) {
                                GPLog.error(this, e1.getLocalizedMessage(), e1);
                                GPDialogs.toast(getActivity(), R.string.wrongLongitude, Toast.LENGTH_LONG);
                                return;
                            }
                            String latString = String.valueOf(latitudeText.getText());
                            double lat;
                            try {
                                lat = Double.parseDouble(latString);
                                if (lat < -90 || lat > 90) {
                                    throw new Exception();
                                }
                            } catch (Exception e1) {
                                GPLog.error(this, e1.getLocalizedMessage(), e1);
                                GPDialogs.toast(getActivity(), R.string.wrongLatitude, Toast.LENGTH_LONG);
                                return;
                            }

                            if (insertCoordinateListener != null)
                                insertCoordinateListener.onCoordinateInserted(lon, lat);
                        }
                    }
            );

            builder.setNegativeButton(getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
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

        if (activity instanceof IInsertCoordinateListener) {
            insertCoordinateListener = (IInsertCoordinateListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        insertCoordinateListener = null;
    }
}
