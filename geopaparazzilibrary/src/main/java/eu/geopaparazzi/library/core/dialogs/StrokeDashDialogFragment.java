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
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;

import java.util.Arrays;

import eu.geopaparazzi.library.R;

/**
 * Class to set the dashing of a stroke.
 *
 * @author Andrea Antonello
 */
public class StrokeDashDialogFragment extends DialogFragment implements CompoundButton.OnCheckedChangeListener {


    private EditText unitText;
    private EditText finalDashText;

    /**
     * A simple interface to use to notify color and stroke changes.
     */
    public interface IDashStrokePropertiesChangeListener {

        /**
         * Called when there is the need to notify that a change occurred.
         */
        void onDashChanged(float[] dash);
    }

    private final static String PREFS_KEY_STROKEDASH = "PREFS_KEY_STROKEDASH";

    private float[] mCurrentDash;

    private IDashStrokePropertiesChangeListener iDashStrokePropertiesChangeListener;
    private Switch[] dashSwitches = new Switch[6];
    private LinearLayout[] dashImages = new LinearLayout[6];

    /**
     * Create a dialog instance.
     *
     * @param dash the current dash to show.
     * @return the instance.
     */
    public static StrokeDashDialogFragment newInstance(float[] dash) {
        StrokeDashDialogFragment f = new StrokeDashDialogFragment();
        Bundle args = new Bundle();
        args.putFloatArray(PREFS_KEY_STROKEDASH, dash);
        f.setArguments(args);
        return f;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            float[] mInitialDash = arguments.getFloatArray(PREFS_KEY_STROKEDASH);
            if (mInitialDash != null) {
                mCurrentDash = mInitialDash;
            }
        }
    }

    // create an AlertDialog and return it
    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        // create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dashStrokeDialogView = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_stroke_dash, null);
        builder.setView(dashStrokeDialogView); // add GUI to dialog

        Switch switch1 = (Switch) dashStrokeDialogView.findViewById(R.id.switch1);
        Switch switch2 = (Switch) dashStrokeDialogView.findViewById(R.id.switch2);
        Switch switch3 = (Switch) dashStrokeDialogView.findViewById(R.id.switch3);
        Switch switch4 = (Switch) dashStrokeDialogView.findViewById(R.id.switch4);
        Switch switch5 = (Switch) dashStrokeDialogView.findViewById(R.id.switch5);
        Switch switch6 = (Switch) dashStrokeDialogView.findViewById(R.id.switch6);
        dashSwitches = new Switch[]{switch1, switch2, switch3, switch4, switch5, switch6};
        for (Switch dashSwitch : dashSwitches) {
            dashSwitch.setOnCheckedChangeListener(this);
        }

        LinearLayout image1 = (LinearLayout) dashStrokeDialogView.findViewById(R.id.imageView1);
        LinearLayout image2 = (LinearLayout) dashStrokeDialogView.findViewById(R.id.imageView2);
        LinearLayout image3 = (LinearLayout) dashStrokeDialogView.findViewById(R.id.imageView3);
        LinearLayout image4 = (LinearLayout) dashStrokeDialogView.findViewById(R.id.imageView4);
        LinearLayout image5 = (LinearLayout) dashStrokeDialogView.findViewById(R.id.imageView5);
        LinearLayout image6 = (LinearLayout) dashStrokeDialogView.findViewById(R.id.imageView6);
        dashImages = new LinearLayout[]{image1, image2, image3, image4, image5, image6};


        unitText = (EditText) dashStrokeDialogView.findViewById(R.id.unitText);
        finalDashText = (EditText) dashStrokeDialogView.findViewById(R.id.finalDashText);
        if (mCurrentDash != null) {
            String dashStr = Arrays.toString(mCurrentDash);
            finalDashText.setText(dashStr);
        }

        paintDash(5);

        builder.setPositiveButton(R.string.set_properties,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (iDashStrokePropertiesChangeListener != null) {
                            iDashStrokePropertiesChangeListener.onDashChanged(mCurrentDash);
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

    private void paintDash(int unit) {
        mCurrentDash = new float[dashSwitches.length];
        for (int i = 0; i < dashSwitches.length; i++) {
            if (dashSwitches[i].isChecked()) {
                dashImages[i].setBackgroundColor(Color.BLACK);
                mCurrentDash[i] = unit;
            } else {
                dashImages[i].setBackgroundColor(Color.WHITE);
                mCurrentDash[i] = 0;
            }
        }
        String dashStr = Arrays.toString(mCurrentDash);
        finalDashText.setText(dashStr);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof IDashStrokePropertiesChangeListener) {
            iDashStrokePropertiesChangeListener = (IDashStrokePropertiesChangeListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        iDashStrokePropertiesChangeListener = null;
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String unitString = unitText.getText().toString();
        int unit = 5;
        try {
            unit = Integer.parseInt(unitString);
        } catch (NumberFormatException e) {
            unitText.setText(unit + "");
        }
        paintDash(unit);
    }

}
