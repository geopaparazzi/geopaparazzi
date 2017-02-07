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
import android.widget.Toast;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.utilities.ISimpleChangeListener;
import eu.geopaparazzi.core.database.DaoMetadata;

/**
 * New project creation dialog.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AddMetadataDialogFragment extends DialogFragment implements TextWatcher {
    private AlertDialog alertDialog;
    private ISimpleChangeListener simpleChangeListener;
    private EditText metadataLabelEditText;
    private EditText metadataKeyEditText;
    private EditText metadataValueEditText;

    // create an AlertDialog and return it
    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        try {

            View newProjectDialogView = getActivity().getLayoutInflater().inflate(
                    R.layout.fragment_dialog_addmetadata, null);
            builder.setView(newProjectDialogView); // add GUI to dialog
            builder.setTitle("Add new metadata item");


            metadataKeyEditText = (EditText) newProjectDialogView.findViewById(R.id.metadataKeyEditText);
            metadataKeyEditText.addTextChangedListener(this);
            metadataLabelEditText = (EditText) newProjectDialogView.findViewById(R.id.metadataLabelEditText);
            metadataLabelEditText.addTextChangedListener(this);
            metadataValueEditText = (EditText) newProjectDialogView.findViewById(R.id.metadataValueEditText);
            metadataValueEditText.addTextChangedListener(this);

            builder.setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                String key = metadataKeyEditText.getText().toString();
                                String label = metadataLabelEditText.getText().toString();
                                String value = metadataValueEditText.getText().toString();

                                if (key.trim().length() + label.trim().length() + value.trim().length() == 0) {
                                    return;
                                }

                                DaoMetadata.insertNewItem(key, label, value);
                                if (simpleChangeListener != null) {
                                    simpleChangeListener.changOccurred();
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

        if (activity instanceof ISimpleChangeListener) {
            simpleChangeListener = (ISimpleChangeListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        simpleChangeListener = null;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

        String key = metadataKeyEditText.getText().toString().trim();
        String label = metadataLabelEditText.getText().toString().trim();

        if (key.length() == 0 || label.length() == 0) {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        } else {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        }
    }
}
