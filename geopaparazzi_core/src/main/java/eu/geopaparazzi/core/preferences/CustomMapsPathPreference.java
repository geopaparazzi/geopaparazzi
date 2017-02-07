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
package eu.geopaparazzi.core.preferences;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.activities.DirectoryBrowserActivity;
import eu.geopaparazzi.core.R;

/**
 * A custom sdcard path chooser.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CustomMapsPathPreference extends DialogPreference implements View.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String USER_MAPS_PATHS = "USER_MAPS_PATHS";
    public static final String PREFS_KEY_CUSTOM_MAPSFOLDER = "PREFS_KEY_CUSTOM_MAPSFOLDER";
    private Context context;
    private String customPath = ""; //$NON-NLS-1$
    private Spinner guessedPathsSpinner;
    private List<String> mapsFoldersList = new ArrayList<>();

    /**
     * @param ctxt  the context to use.
     * @param attrs attributes.
     */
    public CustomMapsPathPreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
        this.context = ctxt;
        setPositiveButtonText(ctxt.getString(android.R.string.ok));
        setNegativeButtonText(ctxt.getString(android.R.string.cancel));
    }

    @Override
    protected View onCreateDialogView() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);

        LinearLayout mainLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mainLayout.setLayoutParams(layoutParams);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View preferencesLayout = layoutInflater.inflate(R.layout.preferences_sdcard, mainLayout);

        guessedPathsSpinner = (Spinner) preferencesLayout.findViewById(R.id.customsdcardPathsSpinner);
        Button browseButton = (Button) preferencesLayout.findViewById(R.id.chooseCustomSdcardButton);
        browseButton.setOnClickListener(this);

        refresh();

        return mainLayout;
    }


    private TreeSet<String> toSet(String paths) {
        TreeSet<String> set = new TreeSet<>();
        String[] pathSplit = paths.split(";");
        for (String path : pathSplit) {
            File file = new File(path.trim());
            if (file.exists() && file.isDirectory()) {
                set.add(file.getAbsolutePath());
            }
        }
        return set;
    }

    private String toString(TreeSet<String> set) {
        StringBuilder sb = new StringBuilder();
        for (String path : set) {
            sb.append(";").append(path);
        }
        if (sb.length() < 1) {
            return "";
        }
        return sb.substring(1);
    }

    private void refresh() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String paths = preferences.getString(USER_MAPS_PATHS, "");
        TreeSet<String> pathSet = toSet(paths);
        File file = new File(customPath.trim());
        if (file.exists() && file.isDirectory())
            pathSet.add(file.getAbsolutePath());

        // add to preferences
        String prefString = toString(pathSet);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(USER_MAPS_PATHS, prefString);
        editor.apply();

        mapsFoldersList.clear();
        mapsFoldersList.addAll(pathSet);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, mapsFoldersList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        guessedPathsSpinner.setAdapter(adapter);
        if (customPath != null) {
            file = new File(customPath.trim());
            for (int i = 0; i < mapsFoldersList.size(); i++) {
                if (mapsFoldersList.get(i).equals(file.getAbsolutePath())) {
                    guessedPathsSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        if (customPath != null) {
            File file = new File(customPath.trim());
            for (int i = 0; i < mapsFoldersList.size(); i++) {
                if (mapsFoldersList.get(i).equals(file.getAbsolutePath())) {
                    guessedPathsSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            customPath = guessedPathsSpinner.getSelectedItem().toString();
            if (callChangeListener(customPath)) {
                persistString(customPath);
            }
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.unregisterOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

        if (restoreValue) {
            if (defaultValue == null) {
                customPath = getPersistedString(""); //$NON-NLS-1$
            } else {
                customPath = getPersistedString(defaultValue.toString());
            }
        } else {
            customPath = defaultValue.toString();
        }
    }

    @Override
    public void onClick(View view) {
        try {
            File sdcardDir = ResourcesManager.getInstance(getContext()).getSdcardDir();
            Intent browseIntent = new Intent(getContext(), DirectoryBrowserActivity.class);
            browseIntent.putExtra(DirectoryBrowserActivity.PUT_PATH_PREFERENCE, PREFS_KEY_CUSTOM_MAPSFOLDER);
            browseIntent.putExtra(DirectoryBrowserActivity.DOFOLDER, true);
            browseIntent.putExtra(DirectoryBrowserActivity.STARTFOLDERPATH, sdcardDir.getAbsolutePath());

            context.startActivity(browseIntent);
        } catch (Exception e) {
            GPLog.error(this, null, e); //$NON-NLS-1$
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PREFS_KEY_CUSTOM_MAPSFOLDER)) {
            String path = sharedPreferences.getString(PREFS_KEY_CUSTOM_MAPSFOLDER, "");
            File file = new File(path.trim());
            if (file.exists()) {
                customPath = file.getAbsolutePath();
            }
            refresh();
        }
    }
}