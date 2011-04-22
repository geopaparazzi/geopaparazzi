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
package eu.hydrologis.geopaparazzi.sms;

import static eu.hydrologis.geopaparazzi.util.Constants.PREFS_KEY_LAT;
import static eu.hydrologis.geopaparazzi.util.Constants.PREFS_KEY_LON;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.debug.Debug;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * Sms reciever and gps coordinates sender.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SmsReceiver extends BroadcastReceiver {
    private static final String SMS_REC_ACTION = "android.provider.Telephony.SMS_RECEIVED"; //$NON-NLS-1$

    @Override
    public void onReceive( Context context, Intent intent ) {
        if (intent.getAction().equals(SmsReceiver.SMS_REC_ACTION)) {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean doCatch = preferences.getBoolean(Constants.SMSCATCHERKEY, false);
            if (!doCatch) {
                return;
            }
            float gpsLon = preferences.getFloat(PREFS_KEY_LON, -9999);
            float gpsLat = preferences.getFloat(PREFS_KEY_LAT, -9999);

            Bundle bundle = intent.getExtras();
            SmsMessage smsMessage = null;
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                for( Object pdu : pdus ) {
                    smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    String body = smsMessage.getDisplayMessageBody();
                    if (Debug.D) Logger.i(this, "Got message: " + body);
                    if (body.toLowerCase().matches(".*geopap.*")) {
                        break;
                    }
                    smsMessage = null;
                }
            }
            if (smsMessage != null) {
                // if (loc == null) {
                // Logger.i("SMSRECEIVER", "Setting a dummy location");
                // loc = new GpsLocation(new Location("dummy"));
                // loc.setLatitude(46.674056);
                // loc.setLongitude(11.132294);
                // loc.setTime(new Date().getTime());
                // }

                StringBuilder sB = new StringBuilder();
                if (gpsLon != -9999) {
                    String lastPosition = context.getString(R.string.last_position);

                    String latString = String.valueOf(gpsLat).replaceAll(",", ".");
                    String lonString = String.valueOf(gpsLon).replaceAll(",", ".");
                    // http://www.openstreetmap.org/?lat=46.068941&lon=11.169849&zoom=18&layers=M&mlat=42.95647&mlon=12.70393
                    sB.append(lastPosition).append(":");
                    sB.append("http://www.openstreetmap.org/?lat=");
                    sB.append(latString);
                    sB.append("&lon=");
                    sB.append(lonString);
                    sB.append("&zoom=14");
                    sB.append("&layers=M&mlat=");
                    sB.append(latString);
                    sB.append("&mlon=");
                    sB.append(lonString);

                    String msg = sB.toString();

                    // Location previousLoc = loc.getPreviousLoc();
                    // if (previousLoc != null) {
                    // Logger.i("SMSRECEIVER", "Has also previous location");
                    // double plat = previousLoc.getLatitude();
                    // double plon = previousLoc.getLongitude();
                    // sB.append("\nPrevious location was:\n");
                    // sB.append("http://www.openstreetmap.org/?lat=");
                    // sB.append(String.valueOf(plat).replaceAll(",", "."));
                    // sB.append("&lon=");
                    // sB.append(String.valueOf(plon).replaceAll(",", "."));
                    // sB.append("&zoom=18");
                    // sB.append("&layers=M&mlat=");
                    // sB.append(latString);
                    // sB.append("&mlon=");
                    // sB.append(lonString);
                    // }

                    if (sB.toString().length() > 160) {
                        // if longer than 160 chars it will not work
                        sB = new StringBuilder(msg);
                    }
                } else {
                    sB.append("Sorry, could not identify the location properly.");
                }

                String msg = sB.toString();
                if (Debug.D) Logger.i(this, msg);
                sendGPSData(context, smsMessage, msg);
            }
        }
    }

    private void sendGPSData( Context context, SmsMessage inMessage, String msg ) {
        SmsManager mng = SmsManager.getDefault();
        PendingIntent dummyEvent = PendingIntent.getBroadcast(context, 0, new Intent("com.devx.SMSExample.IGNORE_ME"), 0);

        String addr = inMessage.getOriginatingAddress();
        // Make sure there's a valid return address.
        if (addr == null) {
            if (Debug.D) Logger.i(this, "Unable to get Address from Sent Message");
        } else {
            if (Debug.D) Logger.i(this, "Sending to: " + addr);
        }
        try {
            if (msg.length() > 160) {
                msg = msg.substring(0, 160);
                if (Debug.D) Logger.i(this, "Trimming msg to: " + msg);
            }
            mng.sendTextMessage(addr, null, msg, dummyEvent, dummyEvent);
        } catch (Exception e) {
            Logger.e(this, "SendException", e);
        }
    }

}