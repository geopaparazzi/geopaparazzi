/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.library.sms;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * Utilities for sms handling.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SmsUtilities {

    /**
     * Create a text containing the OSM link to the current position.
     * 
     * @param context the {@link Context} to use.
     * @param messageText the text to add before the url.
     * @return the position url.
     */
    public static String createPositionText( final Context context, String messageText ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        double[] gpsLocation = PositionUtilities.getGpsLocationFromPreferences(preferences);
        if (gpsLocation == null) {
            gpsLocation = PositionUtilities.getMapCenterFromPreferences(preferences, false, false);
        }
        StringBuilder sB = new StringBuilder();
        if (gpsLocation != null) {
            String latString = LibraryConstants.COORDINATE_FORMATTER.format(gpsLocation[1]).replaceAll(",", ".");
            String lonString = LibraryConstants.COORDINATE_FORMATTER.format(gpsLocation[0]).replaceAll(",", ".");
            // http://www.osm.org/?lat=46.068941&lon=11.169849&zoom=18&layers=M&mlat=42.95647&mlon=12.70393&GeoSMS
            // http://maps.google.com/maps?q=46.068941,11.169849&GeoSMS

            // google maps has the url that is geosms compliant
            sB.append("http://maps.google.com/maps?q=");
            sB.append(latString);
            sB.append(",");
            sB.append(lonString);
            sB.append("&GeoSMS");

            // TODO use OSM again when the coordinates in the url will be geosms compliant
            // sB.append("http://www.osm.org/?lat=");
            // sB.append(latString);
            // sB.append("&lon=");
            // sB.append(lonString);
            // sB.append("&zoom=14");
            // sB.append("&layers=M&mlat=");
            // sB.append(latString);
            // sB.append("&mlon=");
            // sB.append(lonString);
            // sB.append("&GeoSMS");
            if (messageText != null)
                sB.append(" ").append(messageText);

            String msg = sB.toString();
            if (sB.toString().length() > 160) {
                // if longer than 160 chars it will not work
                sB = new StringBuilder(msg);
            }
        } else {
            sB.append(context.getString(R.string.last_position_unknown));
        }

        return sB.toString();
    }

    /**
     * Send an SMS.
     * 
     * @param context the {@link Context} to use.
     * @param number the number to which to send the SMS.
     * @param msg the SMS body text.
     */
    public static void sendSMS( Context context, String number, String msg ) {
        Object systemService = context.getSystemService(Context.TELEPHONY_SERVICE);
        if (systemService instanceof TelephonyManager) {
            TelephonyManager telManager = (TelephonyManager) systemService;
            String networkOperator = telManager.getNetworkOperator();
            if (networkOperator.trim().length() == 0) {
                Utilities.messageDialog(context, "This functionality works only when connected to a GSM network.", null);
                return;
            }
        }

        SmsManager mng = SmsManager.getDefault();
        PendingIntent dummyEvent = PendingIntent.getBroadcast(context, 0, new Intent("com.devx.SMSExample.IGNORE_ME"), 0);
        try {
            if (msg.length() > 160) {
                msg = msg.substring(0, 160);
                if (Debug.D)
                    Logger.i("SMSUTILITIES", "Trimming msg to: " + msg);
            }
            mng.sendTextMessage(number, null, msg, dummyEvent, dummyEvent);
            Utilities.toast(context, R.string.message_sent, Toast.LENGTH_LONG);
        } catch (Exception e) {
            Logger.e(context, e.getLocalizedMessage(), e);
            Utilities.messageDialog(context, "An error occurred while sending the SMS.", null);
        }
    }

    /**
     * Parses a GeoSMS body and tries to extract the coordinates to show them.
     * 
     * @param context the {@link Context} to use.
     * @param smsBody the body of the sms.
     */
    public static void openGeoSms( final Context context, String smsBody ) {
        /*
         * a geosms is supposed to be at least in the form:
         * 
         * http://url?params...?XYZASD=lat,lon?params...?GeoSMS
         */
        String[] split = smsBody.toLowerCase().split("\\?");
        for( String string : split ) {
            if (string.startsWith("http") || string.startsWith("www")) {
                continue;
            }
            if (string.contains("=") && string.contains(",")) {
                String[] coordsParams = string.split("=");
                if (coordsParams.length == 2) {
                    String possibleCoordinates = coordsParams[1];
                    if (possibleCoordinates.contains("&")) {
                        int indexOfAmper = possibleCoordinates.indexOf('&');
                        possibleCoordinates = possibleCoordinates.substring(0, indexOfAmper);
                    }
                    if (possibleCoordinates.contains(",")) {
                        String[] coordsSplit = possibleCoordinates.split(",");
                        if (coordsSplit.length == 2) {
                            String latStr = coordsSplit[0].trim();
                            String lonStr = coordsSplit[1].trim();
                            try {
                                double lat = Double.parseDouble(latStr);
                                double lon = Double.parseDouble(lonStr);
                                StringBuilder sb = new StringBuilder();
                                sb.append("geo:");
                                sb.append(lat);
                                sb.append(",");
                                sb.append(lon);
                                final String geoCoords = sb.toString();

                                // AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                // builder.setMessage("A GeoSMS just arrived, do you want to show the contained position?").setCancelable(false)
                                // .setPositiveButton(context.getString(R.string.ok), new
                                // DialogInterface.OnClickListener(){
                                // public void onClick( DialogInterface dialog, int id ) {
//                                final Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(geoCoords));
//                                context.startActivity(myIntent);

                                NotificationManager notifier = (NotificationManager) context
                                        .getSystemService(Context.NOTIFICATION_SERVICE);
                                int icon = R.drawable.ic_launcher;
                                Notification notification = new Notification(icon, "Simple Notification",
                                        System.currentTimeMillis());
                                final Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(geoCoords));
                                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, myIntent, 0);
                                notification.setLatestEventInfo(context, "Hi!!", "This is a simple notification", contentIntent);
                                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                                notifier.notify(0x007, notification);

                                // }
                                // });
                                // AlertDialog alertDialog = builder.create();
                                // alertDialog.show();

                                // Utilities.messageDialog(context,
                                // "A GeoSMS just arrived, do you want to show the contained position?",
                                // new Runnable(){
                                // public void run() {
                                // final Intent myIntent = new
                                // Intent(android.content.Intent.ACTION_VIEW, Uri
                                // .parse(geoCoords));
                                // context.startActivity(myIntent);
                                // }
                                // });
                            } catch (Exception e) {
                                // ignore the param, it was not a coordinate block
                            }

                        }
                    }
                }
            }

        }

    }

}
