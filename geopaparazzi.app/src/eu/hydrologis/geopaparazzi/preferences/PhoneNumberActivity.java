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
package eu.hydrologis.geopaparazzi.preferences;

import static eu.hydrologis.geopaparazzi.util.Constants.PANICKEY;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * The phone number retrieving activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PhoneNumberActivity extends Activity {
    private static final int CONTACT_PICKER_RESULT = 1001;

    @SuppressWarnings("nls")
    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, People.CONTENT_URI);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if (resultCode == RESULT_OK) {
            switch( requestCode ) {
            case CONTACT_PICKER_RESULT:
                Cursor cursor = null;
                String phoneNumber = "";
                try {
                    Uri result = data.getData();
                    Logger.i(this, "Got a contact result: " + result.toString());

                    // get the contact id from the Uri
                    String id = result.getLastPathSegment();

                    // query for everything email
                    cursor = getContentResolver()
                            .query(Phones.CONTENT_URI, null, Phones.PERSON_ID + "=?", new String[]{id}, null);

                    int numberIndex = cursor.getColumnIndex(Phones.NUMBER);

                    if (cursor.moveToFirst()) {
                        phoneNumber = cursor.getString(numberIndex);
                        Logger.i(this, "Got number: " + phoneNumber);
                    } else {
                        Logger.w(this, "No results");
                    }
                } catch (Exception e) {
                    Logger.e(this, "Failed to get email data", e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }

                    // put into preferences
                    if (phoneNumber.length() != 0) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                        String panicNumbersString = preferences.getString(PANICKEY, "");
                        if (!panicNumbersString.equals("")) {
                            panicNumbersString = panicNumbersString + ",";
                        }
                        panicNumbersString = panicNumbersString + phoneNumber;
                        Editor editor = preferences.edit();
                        editor.putString(PANICKEY, panicNumbersString);
                        editor.commit();
                    } else {
                        Toast.makeText(this, "No phone number found for contact.", Toast.LENGTH_LONG).show();
                    }

                }

                break;
            }

        } else {
            Logger.w(this, "Warning: activity result not ok");
        }
        finish();
    }
}
