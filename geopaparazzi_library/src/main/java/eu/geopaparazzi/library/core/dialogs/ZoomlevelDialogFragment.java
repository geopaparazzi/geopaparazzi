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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.fragment.app.DialogFragment;

import eu.geopaparazzi.library.R;


/**
 * Class to select min and max zoomlevels
 *
 * @author Andrea Antonello
 */
public class ZoomlevelDialogFragment extends DialogFragment {
    /**
     * A simple interface to use to notify zoomlevel properties changes.
     */
    public interface IZoomlevelPropertiesChangeListener {

        /**
         * Called when there is the need to notify that a change occurred.
         */
        void onPropertiesChanged(int minZoomlevel, int maxZoomlevel);
    }

    private final static String PREFS_KEY_ZOOMLEVELPROPERTIES = "PREFS_KEY_ZOOMLEVELPROPERTIES";//NON-NLS

    private int[] mMinMaxZoomlevels = new int[]{0, 22};
    private String[] allZoomlevels = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22"};

    private IZoomlevelPropertiesChangeListener zoomlevelPropertiesChangeListener;

    /**
     * Create a dialog instance.
     *
     * @param minMaxZoomlevels object holding zoomlevel info.
     * @return the instance.
     */
    public static ZoomlevelDialogFragment newInstance(int[] minMaxZoomlevels) {
        ZoomlevelDialogFragment f = new ZoomlevelDialogFragment();
        if (minMaxZoomlevels != null) {
            Bundle args = new Bundle();
            args.putIntArray(PREFS_KEY_ZOOMLEVELPROPERTIES, minMaxZoomlevels);
            f.setArguments(args);
        }
        return f;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            int[] mInitialZoomlevelsObject = arguments.getIntArray(PREFS_KEY_ZOOMLEVELPROPERTIES);
            if (mInitialZoomlevelsObject != null) {
                mMinMaxZoomlevels = mInitialZoomlevelsObject;
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View labelDialogView = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_zoomlevel, null);
        builder.setView(labelDialogView);

        String minStr = mMinMaxZoomlevels[0] + "";
        String maxStr = mMinMaxZoomlevels[1] + "";
        int minIndex = 0;
        int maxIndex = 0;
        for (int i = 0; i < allZoomlevels.length; i++) {
            if (allZoomlevels[i].equals(minStr)) {
                minIndex = i;
            }
            if (allZoomlevels[i].equals(maxStr)) {
                maxIndex = i;
            }
        }
        final Spinner minSpinner = labelDialogView.findViewById(R.id.minZoomLevelSpinner);
        ArrayAdapter<String> zoomlevelSpinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item,
                allZoomlevels);
        zoomlevelSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        minSpinner.setAdapter(zoomlevelSpinnerAdapter);
        minSpinner.setSelection(minIndex);
        minSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Object selectedItem = minSpinner.getSelectedItem();
                String fieldStr = selectedItem.toString();
                mMinMaxZoomlevels[0] = Integer.parseInt(fieldStr);
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                // ignore
            }
        });

        final Spinner maxSpinner = labelDialogView.findViewById(R.id.maxZoomLevelSpinner);
        zoomlevelSpinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item,
                allZoomlevels);
        zoomlevelSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        maxSpinner.setAdapter(zoomlevelSpinnerAdapter);
        maxSpinner.setSelection(maxIndex);
        maxSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Object selectedItem = maxSpinner.getSelectedItem();
                String fieldStr = selectedItem.toString();
                mMinMaxZoomlevels[1] = Integer.parseInt(fieldStr);
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                // ignore
            }
        });


        builder.setPositiveButton(R.string.set_properties,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (zoomlevelPropertiesChangeListener != null) {
                            zoomlevelPropertiesChangeListener.onPropertiesChanged(mMinMaxZoomlevels[0], mMinMaxZoomlevels[1]);
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


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof IZoomlevelPropertiesChangeListener) {
            zoomlevelPropertiesChangeListener = (IZoomlevelPropertiesChangeListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        zoomlevelPropertiesChangeListener = null;
    }


}
