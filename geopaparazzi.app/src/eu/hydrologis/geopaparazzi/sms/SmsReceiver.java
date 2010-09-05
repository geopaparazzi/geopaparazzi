///*
// * Geopaparazzi - Digital field mapping on Android based devices
// * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package eu.hydrologis.geopaparazzi.sms;
//
//import android.app.PendingIntent;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.location.Location;
//import android.os.Bundle;
//import android.preference.PreferenceManager;
//import android.telephony.SmsManager;
//import android.telephony.SmsMessage;
//import android.util.Log;
//import eu.hydrologis.geopaparazzi.R;
//import eu.hydrologis.geopaparazzi.gps.GpsLocation;
//import eu.hydrologis.geopaparazzi.util.ApplicationManager;
//import eu.hydrologis.geopaparazzi.util.Constants;
//
///**
// * Sms reciever and gps coordinates sender.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class SmsReceiver extends BroadcastReceiver {
//    // public static final String SMSRECEIVED = "SMSR";
//    private static final String SMS_REC_ACTION = "android.provider.Telephony.SMS_RECEIVED"; //$NON-NLS-1$
//
//    private ApplicationManager appsManager;
//
//    @Override
//    public void onReceive( Context context, Intent intent ) {
//        if (intent.getAction().equals(SmsReceiver.SMS_REC_ACTION)) {
//            appsManager = ApplicationManager.getInstance();
//            // SharedPreferences preferences = GeoPaparazziActivity.preferences;
//            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//            boolean doCatch = preferences.getBoolean(Constants.SMSCATCHERKEY, false);
//            if (!doCatch) {
//                return;
//            }
//
//            Bundle bundle = intent.getExtras();
//            SmsMessage smsMessage = null;
//            if (bundle != null) {
//                Object[] pdus = (Object[]) bundle.get("pdus");
//                for( Object pdu : pdus ) {
//                    smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
//                    String body = smsMessage.getDisplayMessageBody();
//                    Log.i("SMSRECEIVER", "Got message: " + body);
//                    if (body.matches(".*GEOPAP.*")) {
//                        break;
//                    }
//                    smsMessage = null;
//                }
//            }
//            if (smsMessage != null) {
//                GpsLocation loc = appsManager.getLoc();
//
//                // if (loc == null) {
//                // Log.i("SMSRECEIVER", "Setting a dummy location");
//                // loc = new GpsLocation(new Location("dummy"));
//                // loc.setLatitude(46.674056);
//                // loc.setLongitude(11.132294);
//                // loc.setTime(new Date().getTime());
//                // }
//
//                StringBuilder sB = new StringBuilder();
//                if (loc != null) {
//                    double lat = loc.getLatitude();
//                    double lon = loc.getLongitude();
//                    String utc = loc.getTimeString();
//                    String lastPosition = appsManager.getResource().getString(R.string.last_position);
//
//                    sB.append(lastPosition).append(" ").append(utc).append("(UTC):\n");
//                    sB.append("http://maps.google.com/maps?q=");
//                    sB.append(String.valueOf(lat).replaceAll(",", "."));
//                    sB.append("+");
//                    sB.append(String.valueOf(lon).replaceAll(",", "."));
//
//                    Location previousLoc = loc.getPreviousLoc();
//                    if (previousLoc != null) {
//                        Log.i("SMSRECEIVER", "Has also previous location");
//                        double plat = previousLoc.getLatitude();
//                        double plon = previousLoc.getLongitude();
//                        sB.append("\nPrevious location was:\n");
//                        sB.append("http://maps.google.com/maps?q=");
//                        sB.append(String.valueOf(plat).replaceAll(",", "."));
//                        sB.append("+");
//                        sB.append(String.valueOf(plon).replaceAll(",", "."));
//                    }
//                } else {
//                    sB.append("Sorry, could not identify the location properly.");
//                }
//
//                String msg = sB.toString();
//                Log.i("SMSRECEIVER", msg);
//                sendGPSData(context, smsMessage, msg);
//            }
//        }
//    }
//
//    private void sendGPSData( Context context, SmsMessage inMessage, String msg ) {
//        SmsManager mng = SmsManager.getDefault();
//        PendingIntent dummyEvent = PendingIntent.getBroadcast(context, 0, new Intent("com.devx.SMSExample.IGNORE_ME"), 0);
//
//        String addr = inMessage.getOriginatingAddress();
//        // Make sure there's a valid return address.
//        if (addr == null) {
//            Log.i("SmsIntent", "Unable to get Address from Sent Message");
//        }
//        try {
//            mng.sendTextMessage(addr, null, msg, dummyEvent, dummyEvent);
//        } catch (Exception e) {
//            Log.e("SmsIntent", "SendException", e);
//        }
//    }
//
//}