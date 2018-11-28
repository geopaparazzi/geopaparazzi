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

package eu.geopaparazzi.core.ui.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import eu.geopaparazzi.library.sms.SmsUtilities;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.utilities.Constants;

public class PanicActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageButton panicButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panic);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        panicButton = findViewById(R.id.panicButton);
        panicButton.setOnClickListener(this);

        Button sendUpdateButton = findViewById(R.id.sendUpdateButton);
        sendUpdateButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        sendPosition(v == panicButton);
    }

    /**
     * Send the panic or status update message.
     */
    private void sendPosition(boolean isPanic) {
        Context context = this;
        if (isPanic) {
            String message = getString(R.string.help_needed);
            String[] panicNumbers = getPanicNumbers(context);
            if (panicNumbers == null) {
                String positionText = SmsUtilities.createPositionText(context, message);
                SmsUtilities.sendSMSViaApp(context, "", positionText);
            } else {
                for (String number : panicNumbers) {
                    number = number.trim();
                    if (number.length() == 0) {
                        continue;
                    }
                    String positionText = SmsUtilities.createPositionText(context, message);
                    SmsUtilities.sendSMSViaApp(context, number, positionText);
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
}
