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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import eu.hydrologis.geopaparazzi.R;

/**
 * Custom preference to work with the contacts for the phone number retrival.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PhoneNumberPreference extends Preference implements OnSharedPreferenceChangeListener {

    private EditText textView;

    public PhoneNumberPreference( Context context ) {
        super(context);
    }

    public PhoneNumberPreference( Context context, AttributeSet attrs ) {
        super(context, attrs);
    }

    public PhoneNumberPreference( Context context, AttributeSet attrs, int defStyle ) {
        super(context, attrs, defStyle);
    }

    protected View onCreateView( ViewGroup parent ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        preferences.registerOnSharedPreferenceChangeListener(this);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.phonenumberpref, parent, false);

        textView = (EditText) layout.findViewById(R.id.phonenumber_text);
        textView.addTextChangedListener(new TextWatcher(){

            public void onTextChanged( CharSequence s, int start, int before, int count ) {
            }

            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }

            public void afterTextChanged( Editable s ) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                preferences.unregisterOnSharedPreferenceChangeListener(PhoneNumberPreference.this);
                Editor editor = preferences.edit();
                editor.putString(PANICKEY, textView.getText().toString());
                editor.commit();
                preferences.registerOnSharedPreferenceChangeListener(PhoneNumberPreference.this);
            }
        });

        String panicNumbersString = preferences.getString(PANICKEY, "");
        textView.setText(panicNumbersString);

        Button button = (Button) layout.findViewById(R.id.do_phonenumber_picker);
        button.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                getContext().startActivity(new Intent(getContext(), PhoneNumberActivity.class));
            }
        });

        return layout;
    }

    @Override
    protected void onPrepareForRemoval() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPrepareForRemoval();
    }

    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key ) {
        String panicNumbersString = sharedPreferences.getString(PANICKEY, "");
        textView.setText(panicNumbersString);
    }
}
