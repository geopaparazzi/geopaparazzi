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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Date;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.DatabaseUtilities;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.utilities.IApplicationChangeListener;
import eu.geopaparazzi.core.utilities.Constants;

/**
 * New project creation dialog.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NewProjectDialogFragment extends DialogFragment {
    private EditText projectEditText;
    private AlertDialog alertDialog;
    private TextView errorTextView;
    private IApplicationChangeListener appChangeListener;

    // create an AlertDialog and return it
    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        try {
            final File sdcardDir = ResourcesManager.getInstance(getActivity()).getSdcardDir();
            final String projectExistingString = getString(eu.geopaparazzi.core.R.string.chosen_project_exists);

            final String newGeopaparazziProjectName = Constants.GEOPAPARAZZI
                    + "_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()); //$NON-NLS-1$


            View newProjectDialogView = getActivity().getLayoutInflater().inflate(
                    R.layout.fragment_dialog_newproject, null);
            builder.setView(newProjectDialogView); // add GUI to dialog

            errorTextView = (TextView) newProjectDialogView.findViewById(R.id.newProjectErrorView);

            projectEditText = (EditText) newProjectDialogView.findViewById(R.id.newProjectEditText);
            projectEditText.setText(newGeopaparazziProjectName);
            projectEditText.addTextChangedListener(new TextWatcher() {
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // ignore
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // ignore
                }

                public void afterTextChanged(Editable s) {
                    String newName = s.toString();
                    if (!newName.endsWith(LibraryConstants.GEOPAPARAZZI_DB_EXTENSION)) {
                        newName = newName + LibraryConstants.GEOPAPARAZZI_DB_EXTENSION;
                    }
                    File newProjectFile = new File(sdcardDir, newName);
                    if (newName.length() < 1) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    } else if (newProjectFile.exists()) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        errorTextView.setVisibility(View.VISIBLE);
                        errorTextView.setText(projectExistingString);
                        return;
                    } else {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                    errorTextView.setVisibility(View.GONE);
                }
            });

            builder.setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                Editable value = projectEditText.getText();
                                String newName = value.toString();
                                GeopaparazziApplication.getInstance().closeDatabase();
                                File newGeopaparazziFile = new File(sdcardDir.getAbsolutePath(), newName + LibraryConstants.GEOPAPARAZZI_DB_EXTENSION);
                                DatabaseUtilities.setNewDatabase(getContext(), GeopaparazziApplication.getInstance(), newGeopaparazziFile.getAbsolutePath());

                                if (appChangeListener != null) {
                                    appChangeListener.onApplicationNeedsRestart();
                                }
                            } catch (Exception e) {
                                GPLog.error(this, e.getLocalizedMessage(), e);
                                Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
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

        alertDialog = builder.create();
        return alertDialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof IApplicationChangeListener) {
            appChangeListener = (IApplicationChangeListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        appChangeListener = null;
    }
}
