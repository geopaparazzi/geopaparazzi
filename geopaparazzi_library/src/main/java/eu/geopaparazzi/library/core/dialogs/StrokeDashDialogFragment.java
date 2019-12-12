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
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.style.Style;

/**
 * Class to set the dashing of a stroke.
 *
 * @author Andrea Antonello
 */
public class StrokeDashDialogFragment extends DialogFragment implements CompoundButton.OnCheckedChangeListener {


    private EditText unitText;
    private EditText finalDashText;
    private EditText finalShiftText;
    private float[] mInitialDash;

    /**
     * A simple interface to use to notify color and stroke changes.
     */
    public interface IDashStrokePropertiesChangeListener {

        /**
         * Called when there is the need to notify that a change occurred.
         */
        void onDashChanged(float[] dash, float shift);
    }

    private final static String PREFS_KEY_STROKEDASH = "PREFS_KEY_STROKEDASH";//NON-NLS
    private final static String PREFS_KEY_STROKEDASHSHIFT = "PREFS_KEY_STROKEDASHSHIFT";//NON-NLS

    private float[] mCurrentDash;
    private float mDashShift = 0;

    private IDashStrokePropertiesChangeListener iDashStrokePropertiesChangeListener;
    private Switch[] dashSwitches = new Switch[6];
    private LinearLayout[] dashImages = new LinearLayout[6];

    /**
     * Create a dialog instance.
     *
     * @param dash the current dash to show.
     * @return the instance.
     */
    public static StrokeDashDialogFragment newInstance(float[] dash, float shift) {
        StrokeDashDialogFragment f = new StrokeDashDialogFragment();
        if (dash != null) {
            Bundle args = new Bundle();
            args.putFloatArray(PREFS_KEY_STROKEDASH, dash);
            args.putFloat(PREFS_KEY_STROKEDASHSHIFT, shift);
            f.setArguments(args);
        }
        return f;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mInitialDash = arguments.getFloatArray(PREFS_KEY_STROKEDASH);
            if (mInitialDash != null) {
                mCurrentDash = mInitialDash;
            }
            mDashShift = arguments.getFloat(PREFS_KEY_STROKEDASHSHIFT);
        }
    }

    // create an AlertDialog and return it
    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        // create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dashStrokeDialogView = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_stroke_dash, null);
        builder.setView(dashStrokeDialogView); // add GUI to dialog

        Switch switch1 = dashStrokeDialogView.findViewById(R.id.switch1);
        Switch switch2 = dashStrokeDialogView.findViewById(R.id.switch2);
        Switch switch3 = dashStrokeDialogView.findViewById(R.id.switch3);
        Switch switch4 = dashStrokeDialogView.findViewById(R.id.switch4);
        Switch switch5 = dashStrokeDialogView.findViewById(R.id.switch5);
        Switch switch6 = dashStrokeDialogView.findViewById(R.id.switch6);
        dashSwitches = new Switch[]{switch1, switch2, switch3, switch4, switch5, switch6};
        for (Switch dashSwitch : dashSwitches) {
            dashSwitch.setOnCheckedChangeListener(this);
        }

        LinearLayout image1 = dashStrokeDialogView.findViewById(R.id.imageView1);
        LinearLayout image2 = dashStrokeDialogView.findViewById(R.id.imageView2);
        LinearLayout image3 = dashStrokeDialogView.findViewById(R.id.imageView3);
        LinearLayout image4 = dashStrokeDialogView.findViewById(R.id.imageView4);
        LinearLayout image5 = dashStrokeDialogView.findViewById(R.id.imageView5);
        LinearLayout image6 = dashStrokeDialogView.findViewById(R.id.imageView6);
        dashImages = new LinearLayout[]{image1, image2, image3, image4, image5, image6};


        unitText = dashStrokeDialogView.findViewById(R.id.unitText);
        finalDashText = dashStrokeDialogView.findViewById(R.id.finalDashText);
        finalShiftText = dashStrokeDialogView.findViewById(R.id.finalDashShiftText);
        if (mCurrentDash != null) {
            String dashStr = Style.dashToString(mCurrentDash, null);
            finalDashText.setText(dashStr);
            finalShiftText.setText(mDashShift + "");
        }

        builder.setPositiveButton(R.string.set_dash,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (iDashStrokePropertiesChangeListener != null) {
                            iDashStrokePropertiesChangeListener.onDashChanged(mCurrentDash, mDashShift);
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
        // first check if one is checked
        boolean firstChecked = dashSwitches[0].isChecked();
        if (firstChecked) {
            mDashShift = 0;
        } else {
            mDashShift = unit;
        }

        boolean oneChecked = false;
        for (Switch dashSwitch : dashSwitches) {
            if (dashSwitch.isChecked()) {
                oneChecked = true;
                break;
            }
        }

        List<Float> dashList = new ArrayList<>(dashSwitches.length);
        float count = 1;
        int length = dashSwitches.length - 1;
        for (int i = 0; i < length; i++) {
            if (dashSwitches[i].isChecked() && dashSwitches[i + 1].isChecked()) {
                count++;
            } else if (!dashSwitches[i].isChecked() && !dashSwitches[i + 1].isChecked()) {
                count++;
            } else {
                dashList.add(count * unit);
                count = 1;
            }
        }
        dashList.add(count * unit);

        mCurrentDash = new float[dashList.size()];
        for (int i = 0; i < dashList.size(); i++) {
            mCurrentDash[i] = dashList.get(i);
        }

        for (int i = 0; i < dashSwitches.length; i++) {
            if (dashSwitches[i].isChecked()) {
                dashImages[i].setBackgroundColor(Color.BLACK);
            } else {
                dashImages[i].setBackgroundColor(Color.WHITE);
            }
        }

        if (mCurrentDash == null) mCurrentDash = mInitialDash;
        String dashStr = Style.dashToString(mCurrentDash, null);
        finalDashText.setText(dashStr);
        finalShiftText.setText("" + mDashShift);
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
