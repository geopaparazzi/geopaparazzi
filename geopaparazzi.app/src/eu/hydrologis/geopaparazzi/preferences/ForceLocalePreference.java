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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.locale.LocaleUtils;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.activities.DirectoryBrowserActivity;
import eu.hydrologis.geopaparazzi.R;

/**
 * A custom preference to force a particular locale, even if the OS is on another.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ForceLocalePreference extends DialogPreference {
    public static final String PREFS_KEY_FORCELOCALE = "PREFS_KEY_FORCELOCALE";
    private Context context;
    private Spinner localesSpinner;

    /**
     * @param ctxt  the context to use.
     * @param attrs attributes.
     */
    public ForceLocalePreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
        this.context = ctxt;
        setPositiveButtonText(ctxt.getString(android.R.string.ok));
        setNegativeButtonText(ctxt.getString(android.R.string.cancel));
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout mainLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mainLayout.setLayoutParams(layoutParams);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(25, 25, 25, 25);

        localesSpinner = new Spinner(context);
        localesSpinner.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        localesSpinner.setPadding(15, 5, 15, 5);

        final String[] localesArray = context.getResources().getStringArray(R.array.locales);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, localesArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        localesSpinner.setAdapter(adapter);
        final String currentLocale = LocaleUtils.getCurrentLocale(context);
        if (currentLocale != null) {
            for (int i = 0; i < localesArray.length; i++) {
                if (localesArray[i].equals(currentLocale.trim())) {
                    localesSpinner.setSelection(i);
                    break;
                }
            }
        }

        mainLayout.addView(localesSpinner);
        return mainLayout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            String selectedLocale = localesSpinner.getSelectedItem().toString();
            LocaleUtils.changeLang(context, selectedLocale);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
    }


}