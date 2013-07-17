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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * Sms reciever and gps coordinates sender.
 * 
 * <p>
 * To use this you will need to add to the manifest:
 * 
 * <pre>
 *      <receiver android:name="eu.geopaparazzi.library.sms.SmsReceiverPositionSender" android:enabled="true">
 *           <intent-filter>
 *               <action android:name="android.provider.Telephony.SMS_RECEIVED" />
 *           </intent-filter>
 *       </receiver>
 * </pre>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SmsReceiverPositionSender extends BroadcastReceiver {
    private static final String SMS_REC_ACTION = "android.provider.Telephony.SMS_RECEIVED"; //$NON-NLS-1$

    @Override
    public void onReceive( final Context context, Intent intent ) {
        if (intent.getAction().equals(SmsReceiverPositionSender.SMS_REC_ACTION)) {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean doCatch = preferences.getBoolean(LibraryConstants.PREFS_KEY_SMSCATCHER, false);
            if (!doCatch) {
                return;
            }

            boolean isGeosms = false;
            boolean isGeopapsms = false;

            Bundle bundle = intent.getExtras();
            SmsMessage smsMessage = null;
            String body = null;
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                for( Object pdu : pdus ) {
                    smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                    body = smsMessage.getDisplayMessageBody();
                    if (body == null) {
                        continue;
                    }
                    if (GPLog.LOG)
                        GPLog.addLogEntry(this, "Got message: " + body);
                    if (body.toLowerCase().matches(".*geopap.*")) {
                        isGeopapsms = true;
                    }
                    if (body.toLowerCase().matches(".*geosms.*")) {
                        isGeosms = true;
                    }
                }
            }
            if (body != null && smsMessage != null && smsMessage.getOriginatingAddress() != null) {
                if (isGeopapsms) {
                    String msg = null;
                    String lastPosition = context.getString(R.string.last_position);
                    msg = SmsUtilities.createPositionText(context, lastPosition);
                    if (msg.length() > 160) {
                        // if longer than 160 chars it will not work, try using only url
                        msg = SmsUtilities.createPositionText(context, null);
                    }

                    if (GPLog.LOG)
                        GPLog.addLogEntry(this, msg);
                    String addr = smsMessage.getOriginatingAddress();
                    SmsUtilities.sendSMS(context, addr, msg, true);
                }
                if (isGeosms) {
                    SmsUtilities.openGeoSms(context, body);
                }
            }
        }
    }

}