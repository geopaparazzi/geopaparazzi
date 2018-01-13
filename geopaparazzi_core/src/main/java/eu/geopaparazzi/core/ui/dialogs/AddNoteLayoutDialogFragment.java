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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.ui.activities.AddNotesActivity;


/**
 * Class to change add notes layout
 *
 * @author Andrea Antonello
 */
public class AddNoteLayoutDialogFragment extends DialogFragment {

    private IAddNotesLayoutChangeListener layoutChangeListener;

    public interface IAddNotesLayoutChangeListener {
        void onPropertiesChanged();
    }

    private TextView mSizeTextView;
    private TextView mColumnTextView;

    /**
     * Create a dialog instance.
     *
     * @return the instance.
     */
    public static AddNoteLayoutDialogFragment newInstance() {
        AddNoteLayoutDialogFragment f = new AddNoteLayoutDialogFragment();
        return f;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int gridColumnCount = preferences.getInt(AddNotesActivity.PREFS_KEY_GUICOLUMNCOUNT,
                AddNotesActivity.DEFAULT_GUICOLUMNCOUNT);
        int textsizeFactor = preferences.getInt(AddNotesActivity.PREFS_KEY_GUITEXTSIZEFACTOR,
                AddNotesActivity.DEFAULT_GUITEXTSIZEFACTOR);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        View labelDialogView =
                getActivity().getLayoutInflater().inflate(
                        R.layout.fragment_dialog_addnote_layout, null);
        builder.setView(labelDialogView);


        mSizeTextView = labelDialogView.findViewById(R.id.sizeFactorTextView);
        mSizeTextView.setText(String.valueOf(textsizeFactor));
        SeekBar mSizeSeekBar = labelDialogView.findViewById(R.id.sizeFactorSeekBar);
        mSizeSeekBar.setOnSeekBarChangeListener(sizeChanged);
        mSizeSeekBar.setProgress(textsizeFactor);

        mColumnTextView = labelDialogView.findViewById(R.id.columnTextView);
        mColumnTextView.setText(String.valueOf(gridColumnCount));
        SeekBar mColumnSeekBar = labelDialogView.findViewById(R.id.columnSeekBar);
        mColumnSeekBar.setOnSeekBarChangeListener(columnChanged);
        mColumnSeekBar.setProgress(gridColumnCount);


        builder.setPositiveButton(R.string.set_properties,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences.Editor editor = preferences.edit();
                        int col = 1;
                        try {
                            col = Integer.parseInt(mColumnTextView.getText().toString());
                        } catch (Exception e) {
                            //ignore
                        }
                        editor.putInt(AddNotesActivity.PREFS_KEY_GUICOLUMNCOUNT, col);
                        int sizeFactor = 1;
                        try {
                            sizeFactor = Integer.parseInt(mSizeTextView.getText().toString());
                        } catch (Exception e) {
                            //ignore
                        }
                        editor.putInt(AddNotesActivity.PREFS_KEY_GUITEXTSIZEFACTOR, sizeFactor);
                        editor.commit();

                        if (layoutChangeListener != null) {
                            layoutChangeListener.onPropertiesChanged();
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

        return builder.create(); // return dialog
    }


    private final OnSeekBarChangeListener sizeChanged =
            new OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    mSizeTextView.setText(String.valueOf(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                } // required

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                } // required
            };

    private final OnSeekBarChangeListener columnChanged =
            new OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    mColumnTextView.setText(String.valueOf(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                } // required

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                } // required
            };


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof IAddNotesLayoutChangeListener) {
            layoutChangeListener = (IAddNotesLayoutChangeListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        layoutChangeListener = null;
    }

}
