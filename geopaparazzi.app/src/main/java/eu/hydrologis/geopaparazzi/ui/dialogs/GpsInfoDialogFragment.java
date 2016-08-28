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

package eu.hydrologis.geopaparazzi.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Date;

import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.sensors.OrientationSensor;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.hydrologis.geopaparazzi.R;


/**
 * Dialog to show some gps info and update it.
 *
 * @author hydrologis
 */
public class GpsInfoDialogFragment extends DialogFragment {
    private TextView gpsInfoTextview;
    private OrientationSensor orientationSensor;
    private BroadcastReceiver gpsServiceBroadcastReceiver;

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        View gpsinfoDialogView = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_dialog_gpsinfo, null);
        builder.setView(gpsinfoDialogView);
        builder.setTitle(R.string.gps_info_dialog_title);

        gpsInfoTextview = (TextView) gpsinfoDialogView.findViewById(
                R.id.gpsinfoTextview);

        builder.setPositiveButton(R.string.open_gps_settings,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(gpsOptionsIntent);
                    }
                }
        );
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = new OrientationSensor(sensorManager, null);
        orientationSensor.register(getActivity(), SensorManager.SENSOR_DELAY_NORMAL);

        gpsServiceBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                onGpsServiceUpdate(intent);
            }
        };
        GpsServiceUtilities.registerForBroadcasts(getActivity(), gpsServiceBroadcastReceiver);
        GpsServiceUtilities.triggerBroadcast(getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();

        orientationSensor.unregister();
        GpsServiceUtilities.unregisterFromBroadcasts(getActivity(), gpsServiceBroadcastReceiver);
    }

    private void onGpsServiceUpdate(Intent intent) {
        GpsServiceStatus lastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        GpsLoggingStatus lastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
        double[] lastGpsPosition = GpsServiceUtilities.getPosition(intent);
        float[] lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
        int[] lastGpsStatusExtras = GpsServiceUtilities.getGpsStatusExtras(intent);
        long lastPositiontime = GpsServiceUtilities.getPositionTime(intent);

        Context context = getActivity();
        String timeString = context.getString(R.string.utctime);
        String lonString = context.getString(R.string.lon);
        String latString = context.getString(R.string.lat);
        String altimString = context.getString(R.string.altim);
        String azimString = context.getString(R.string.azimuth);
        String loggingString = context.getString(R.string.text_logging);
        String acquirefixString = context.getString(R.string.gps_searching_fix);
        String satellitesString = context.getString(R.string.satellites);
        String gpsAccuracyString = "accuracy:";
        String gpsUnits = " m";
        String gpsStatusString = context.getString(R.string.gps_status);
        String mapString = context.getString(R.string.map);
        String pathString = context.getString(R.string.path_lc);
        String boundsString = context.getString(R.string.bounds);
        String indent = "  ";

        double azimuth = orientationSensor.getAzimuthDegrees();
        StringBuilder sb = new StringBuilder();

        AbstractSpatialTable selectedMapTable = BaseMapSourcesManager.INSTANCE.getSelectedBaseMapTable();
        if (selectedMapTable != null) {
            String path = selectedMapTable.getDatabasePath();
            float[] bounds = selectedMapTable.getTableBounds();
            String mapType = selectedMapTable.getMapType();
            sb.append(mapString).append(":\n");
            sb.append(pathString).append(": ").append(path).append("\n");
            sb.append(boundsString).append(":\n");
            sb.append(indent).append("s = ").append(bounds[1]).append("\n");
            sb.append(indent).append("n = ").append(bounds[0]).append("\n");
            sb.append(indent).append("w = ").append(bounds[3]).append("\n");
            sb.append(indent).append("e = ").append(bounds[2]).append("\n\n");
        }
        if (lastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
            // sb.append(indent).append(nodataString).append("\n");
        } else if (lastGpsServiceStatus == GpsServiceStatus.GPS_LISTENING__NO_FIX) {
            sb.append(gpsStatusString).append(":\n");
            sb.append(indent).append(acquirefixString);
            // if (lastGpsServiceStatus != GpsServiceStatus.GPS_FIX) {
            // } else {
            // sb.append(indent).append(gpsonString);
            //                sb.append(": ").append(lastGpsServiceStatus == GpsServiceStatus.GPS_OFF); //$NON-NLS-1$
            // }
            sb.append("\n");
            addGpsStatusInfo(lastGpsStatusExtras, sb, satellitesString, indent);

        } else {
            sb.append(gpsStatusString).append(":\n");
            String lat;
            String lon;
            String elev;
            String time;
            String acc;

            if (lastGpsPositionExtras != null) {
                acc = String.valueOf(lastGpsPositionExtras[0]);
            } else {
                acc = " - ";
            }

            if (lastGpsPosition != null) {
                DecimalFormat formatter = new DecimalFormat("0.00000"); //$NON-NLS-1$
                lat = formatter.format(lastGpsPosition[1]);
                lon = formatter.format(lastGpsPosition[0]);
                elev = String.valueOf((int) lastGpsPosition[2]);
                time = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(lastPositiontime));
            } else {
                time = " - ";
                lat = " - ";
                lon = " - ";
                elev = " - ";
            }
            sb.append(indent).append(timeString);
            sb.append(" ").append(time); //$NON-NLS-1$
            sb.append("\n");
            sb.append(indent).append(latString);
            sb.append(" ").append(lat); //$NON-NLS-1$
            sb.append("\n");
            sb.append(indent).append(lonString);
            sb.append(" ").append(lon); //$NON-NLS-1$
            sb.append("\n");
            sb.append(indent).append(gpsAccuracyString);
            sb.append(" ").append(acc).append(gpsUnits); //$NON-NLS-1$
            sb.append("\n");
            sb.append(indent).append(altimString);
            sb.append(" ").append(elev).append(gpsUnits); //$NON-NLS-1$
            sb.append("\n");
            sb.append(indent).append(loggingString);
            sb.append(": ").append(lastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON); //$NON-NLS-1$
            sb.append("\n");
            addGpsStatusInfo(lastGpsStatusExtras, sb, satellitesString, indent);
        }
        sb.append(indent).append(azimString);
        sb.append(" ").append((int) azimuth); //$NON-NLS-1$
        sb.append("\n");

        gpsInfoTextview.setText(sb.toString());
    }

    private void addGpsStatusInfo(int[] lastGpsStatusExtras, StringBuilder sb, String satellitesString, String indent) {
        if (lastGpsStatusExtras != null) {
            int satForFixCount = lastGpsStatusExtras[2];
            int satCount = lastGpsStatusExtras[1];
            sb.append(indent).append(satellitesString).append(": ")//
                    .append(satForFixCount).append("/").append(satCount).append("\n");
            // sb.append("used for fix: ").append(satForFixCount).append("\n");
        }
    }


}
