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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.Utilities;

/**
 * Utilities for sms handling.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SmsUtilities {

    /**
     * 
     */
    public static String SMSHOST = "gp.eu";

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
     * @param sendMessage if <code>true</code>, a {@link Toast} tells the user that the message was sent.
     */
    public static void sendSMS( Context context, String number, String msg, boolean sendMessage ) {
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
                if (GPLog.LOG)
                    GPLog.addLogEntry("SMSUTILITIES", "Trimming msg to: " + msg);
            }
            mng.sendTextMessage(number, null, msg, dummyEvent, dummyEvent);
            if (sendMessage)
                Utilities.toast(context, R.string.message_sent, Toast.LENGTH_LONG);
        } catch (Exception e) {
            GPLog.error(context, e.getLocalizedMessage(), e);
            Utilities.messageDialog(context, "An error occurred while sending the SMS.", null);
        }
    }

    /**
     * Opens the system sms app to send the message.  
     * 
     * @param context the {@link Context} to use.
     * @param number the number to which to send to or <code>null</code>, in which
     *          case the number will be prompted in the sms app.
     * @param msg the message to send or <code>null</code>.
     */
    public static void sendSMSViaApp( Context context, String number, String msg ) {
        Object systemService = context.getSystemService(Context.TELEPHONY_SERVICE);
        if (systemService instanceof TelephonyManager) {

            TelephonyManager telManager = (TelephonyManager) systemService;
            int phoneType = telManager.getPhoneType();
            if (phoneType == TelephonyManager.PHONE_TYPE_NONE) {
                // no phone
                Utilities.messageDialog(context, "This functionality works only when connected to a GSM network.", null);
                return;
            }
        }

        if (number == null) {
            number = "";
        }
        if (msg == null) {
            msg = "";
        }

        if (Build.VERSION.SDK_INT >= 19) {
            try {
                Class< ? > smsClass = Class.forName("android.provider.Telephony$Sms");
                Method getPackageMethod = smsClass.getMethod("getDefaultSmsPackage", Context.class);
                Object defaultSmsPackageNameObj = getPackageMethod.invoke(null, context);
                if (defaultSmsPackageNameObj instanceof String) {
                    String defaultSmsPackageName = (String) defaultSmsPackageNameObj;
                    // String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(context);
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
                    if (defaultSmsPackageName != null) {
                        sendIntent.setPackage(defaultSmsPackageName);
                    }
                    context.startActivity(sendIntent);
                }
            } catch (Exception e) {
                GPLog.error("SmsUtilities", "Error sending sms in > 4.4.", e);
            }
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + number));
            intent.putExtra("sms_body", msg);
            context.startActivity(intent);
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
                                // double lat = Double.parseDouble(latStr);
                                // double lon = Double.parseDouble(lonStr);
                                StringBuilder sB = new StringBuilder();
                                // sb.append("geo:");
                                // sb.append(lat);
                                // sb.append(",");
                                // sb.append(lon);
                                sB.append("http://maps.google.com/maps?q=");
                                sB.append(latStr);
                                sB.append(",");
                                sB.append(lonStr);
                                sB.append("&GeoSMS");
                                final String geoCoords = sB.toString();

                                NotificationManager notifier = (NotificationManager) context
                                        .getSystemService(Context.NOTIFICATION_SERVICE);
                                int icon = R.drawable.current_position;
                                Notification notification = new Notification(icon, "Incoming GeoSMS", System.currentTimeMillis());
                                final Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(geoCoords));
                                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, myIntent, 0);
                                notification.setLatestEventInfo(context, "GeoSMS",
                                        "Select here to open the GeoSMS with a dedicated application", contentIntent);
                                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                                notifier.notify(0x007, notification);

                            } catch (Exception e) {
                                // ignore the param, it was not a coordinate block
                            }

                        }
                    }
                }
            }

        }

    }

    /**
     * Converts an sms data content to the data.
     * 
     * <p>
     * The format is of the type <b>gp.eu/n:x,y,desc;n:x,y,z,desc;b:x,y,desc;b:x,y,z,desc;...</b>
     * </p>
     * <p>
     * Where n = note, in which case z can be added and is the altitude; and 
     * b = bookmark, in which case z can be added and is the zoom.
     * </p>
     * 
     * @param url the sms data url to convert.
     * @return the list of {@link SmsData} containing notes and bookmarks data.
     * @throws IOException   if something goes wrong.
     */
    public static List<SmsData> sms2Data( String url ) throws IOException {
        List<SmsData> smsDataList = new ArrayList<SmsData>();

        url = url.replaceFirst("http://", "");
        // remove gp://
        url = url.substring(6);

        String[] dataSplit = url.split(";");
        for( String data : dataSplit ) {
            if (data.startsWith("n:") || data.startsWith("b:")) {
                String dataTmp = data.substring(2);
                String[] values = dataTmp.split(",");
                if (values.length < 3) {
                    throw new IOException();
                }

                float x = Float.parseFloat(values[0]);
                float y = Float.parseFloat(values[1]);
                float z = -1;
                String descr = null;
                if (values.length > 3) {
                    z = Float.parseFloat(values[2]);
                    descr = values[3];
                } else {
                    descr = values[2];
                }

                SmsData smsData = new SmsData();

                if (data.startsWith("n:")) {
                    smsData.TYPE = SmsData.NOTE;
                } else if (data.startsWith("b:")) {
                    smsData.TYPE = SmsData.BOOKMARK;
                }

                smsData.x = x;
                smsData.y = y;
                smsData.z = z;
                smsData.text = descr;
                smsDataList.add(smsData);
            }
        }

        return smsDataList;
    }

    /**
     * Checks if the device supports phone. 
     * 
     * @param context  the context to use.
     * @return  if something goes wrong.
     */
    public static boolean hasPhone( Context context ) {
        TelephonyManager telephonyManager1 = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager1.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
            return false;
        } else {
            return true;
        }
    }

}
