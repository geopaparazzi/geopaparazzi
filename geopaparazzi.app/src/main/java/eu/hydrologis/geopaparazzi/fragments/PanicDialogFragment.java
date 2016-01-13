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

package eu.hydrologis.geopaparazzi.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;

import java.text.DecimalFormat;
import java.util.Date;

import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.sms.SmsUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.utilities.Constants;


/**
 * Dialog to show some gps info and update it.
 *
 * @author hydrologis
 */
public class PanicDialogFragment extends DialogFragment implements View.OnClickListener {

    public static final String KEY_ISPANIC = "KEY_ISPANIC";
    private double mLat = -9999;
    private double mLon = -9999;
    private boolean mIsPanic;

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mLat = arguments.getDouble(LibraryConstants.LATITUDE);
            mLon = arguments.getDouble(LibraryConstants.LONGITUDE);
            mIsPanic = arguments.getBoolean(KEY_ISPANIC);
        }

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        View panicDialogView;
        Button button;
        if (mIsPanic) {
            panicDialogView = getActivity().getLayoutInflater().inflate(
                    R.layout.fragment_dialog_panic, null);
            button = (Button) panicDialogView.findViewById(R.id.panicbutton);
        } else {
            panicDialogView = getActivity().getLayoutInflater().inflate(
                    R.layout.fragment_dialog_sendsms, null);
            button = (Button) panicDialogView.findViewById(R.id.statusupdatebutton);
        }
        builder.setView(panicDialogView);

        button.setOnClickListener(this);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
//            int width = ViewGroup.LayoutParams.MATCH_PARENT;
//            int height = ViewGroup.LayoutParams.MATCH_PARENT;
//            dialog.getWindow().setLayout(width, height);
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

//        gpsServiceBroadcastReceiver = new BroadcastReceiver() {
//            public void onReceive(Context context, Intent intent) {
//                onGpsServiceUpdate(intent);
//            }
//        };
//        GpsServiceUtilities.registerForBroadcasts(getActivity(), gpsServiceBroadcastReceiver);
//        GpsServiceUtilities.triggerBroadcast(getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();

//        GpsServiceUtilities.unregisterFromBroadcasts(getActivity(), gpsServiceBroadcastReceiver);
    }

//    private void onGpsServiceUpdate(Intent intent) {
//        GpsServiceStatus lastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
//        GpsLoggingStatus lastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
//        double[] lastGpsPosition = GpsServiceUtilities.getPosition(intent);
////        float[] lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
//        int[] lastGpsStatusExtras = GpsServiceUtilities.getGpsStatusExtras(intent);
//        long lastPositiontime = GpsServiceUtilities.getPositionTime(intent);
//
//        if (lastGpsPosition != null) {
//            DecimalFormat formatter = new DecimalFormat("0.00000"); //$NON-NLS-1$
//            String lat = formatter.format(lastGpsPosition[1]);
//            String lon = formatter.format(lastGpsPosition[0]);
//            String elev = String.valueOf((int) lastGpsPosition[2]);
//            String time = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(lastPositiontime));
//
//
//        }
//    }


    /**
     * Send the panic or status update message.
     */
    private void sendPosition() {
        Context context = getContext();
        if (mIsPanic) {
            String lastPosition = getString(R.string.help_needed);
            String[] panicNumbers = getPanicNumbers(getContext());
            if (panicNumbers == null) {
                String positionText = SmsUtilities.createPositionText(context, lastPosition);
                SmsUtilities.sendSMSViaApp(context, "", positionText);
            } else {
                for (String number : panicNumbers) {
                    number = number.trim();
                    if (number.length() == 0) {
                        continue;
                    }

                    String positionText = SmsUtilities.createPositionText(context, lastPosition);
                    SmsUtilities.sendSMS(context, number, positionText, true);
                }
            }
        } else {
            // just sending a single geosms
            String positionText = SmsUtilities.createPositionText(context, "");
            SmsUtilities.sendSMSViaApp(context, "", positionText);
        }

    }

    /**
     * Gets the panic numbers from the preferences.
     *
     * @param context the {@link Context} to use.
     * @return the array of numbers or null.
     */
    @SuppressWarnings("nls")
    public static String[] getPanicNumbers(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String panicNumbersString = preferences.getString(Constants.PANICKEY, "");
        // Make sure there's a valid return address.
        if (panicNumbersString.length() == 0 || panicNumbersString.matches(".*[A-Za-z].*")) {
            return null;
        } else {
            String[] numbers = panicNumbersString.split(";");
            return numbers;
        }
    }

    @Override
    public void onClick(View v) {
        sendPosition();
    }
}
