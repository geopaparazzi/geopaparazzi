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

package eu.geopaparazzi.core.profiles.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;


import java.util.Date;

import eu.geopaparazzi.core.R;


/**
 * New profile creation dialog.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NewProfileDialogFragment extends DialogFragment {
    public static final String KEY_PROFILENAME = "KEY_PROFILENAME";
    public static final String KEY_PROFILEDESCR = "KEY_PROFILEDESCR";

    private INewProfileCreatedListener iNewProfileCreatedListener;
    private String name;
    private String description;
    private EditText newProfileNameText;
    private EditText newProfileDescriptionText;

    public interface INewProfileCreatedListener {
        void onNewProfileCreated(String name, String description);
    }

    public static NewProfileDialogFragment newInstance(String name, String description) {
        NewProfileDialogFragment f = new NewProfileDialogFragment();
        if (name != null) {
            Bundle args = new Bundle();
            args.putString(KEY_PROFILENAME, name);
            if (description == null) description = "";
            args.putString(KEY_PROFILEDESCR, description);
            f.setArguments(args);
        }
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            name = arguments.getString(KEY_PROFILENAME);
            if (name == null) name = "";
            description = arguments.getString(KEY_PROFILEDESCR);
            if (description == null) description = "";
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        try {

            View newProjectDialogView = getActivity().getLayoutInflater().inflate(
                    R.layout.fragment_dialog_newprofile, null);
            builder.setView(newProjectDialogView); // add GUI to dialog
            builder.setTitle(name);

            newProfileNameText = (EditText) newProjectDialogView.findViewById(R.id.profileNameEditText);
            if (name != null) newProfileNameText.setText(name);
            newProfileDescriptionText = (EditText) newProjectDialogView.findViewById(R.id.profileDescriptionEditText);
            if (description != null) newProfileDescriptionText.setText(description);

            builder.setPositiveButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String name = newProfileNameText.getText().toString();
                            if (name.trim().length() == 0)
                                name = "no name: " + new Date();
                            String description = newProfileDescriptionText.getText().toString();

                            if (iNewProfileCreatedListener != null)
                                iNewProfileCreatedListener.onNewProfileCreated(name, description);
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

        if (activity instanceof INewProfileCreatedListener) {
            iNewProfileCreatedListener = (INewProfileCreatedListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        iNewProfileCreatedListener = null;
    }
}
