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

import java.util.List;
import java.io.File;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.hydrologis.geopaparazzi.R;

/**
 * A custom maps folder chooser.
 * 
 * @author Andrea Antonello (www.hydrologis.com); adapted for folders by Tim Howard (nynhp.org)
 *
 */
public class CustomMapsFolderPreference extends DialogPreference {
    private Context context;
    private EditText editView;
    private String customFolder = ""; //$NON-NLS-1$
    private String sdcardFolder = ""; //$NON-NLS-1$
    private Spinner guessedFolderSpinner;
    private List<String> guessedFoldersList;
    /**
     * @param ctxt  the context to use.
     * @param attrs attributes.
     */
    public CustomMapsFolderPreference( Context ctxt, AttributeSet attrs ) {
        super(ctxt, attrs);
        this.context = ctxt;
        try {
         // mj10777: set start value with existing value
         customFolder=ResourcesManager.getInstance(this.context).getMapsDir().getAbsolutePath();
         sdcardFolder=ResourcesManager.getInstance(this.context).getSdcardDir().getAbsolutePath();
        }
        catch (Exception e) {
         GPLog.error(this, "CustomMapsFolderPreference[invalid getSdcardDir or getMapsDir]", e);
        }
        setPositiveButtonText(ctxt.getString(android.R.string.ok));
        setNegativeButtonText(ctxt.getString(android.R.string.cancel));
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout mainLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        mainLayout.setLayoutParams(layoutParams);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(context);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        textView.setPadding(2, 2, 2, 2);
        textView.setText(R.string.set_maps_folder_manually);
        mainLayout.addView(textView);

        editView = new EditText(context);
        editView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        editView.setPadding(15, 5, 15, 5);
        editView.setText(customFolder);
        mainLayout.addView(editView);

        TextView comboLabelView = new TextView(context);
        comboLabelView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        comboLabelView.setPadding(2, 2, 2, 2);
        comboLabelView.setText(R.string.choose_from_suggested_folders);
        mainLayout.addView(comboLabelView);

        guessedFolderSpinner = new Spinner(context);
        guessedFolderSpinner.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        guessedFolderSpinner.setPadding(15, 5, 15, 5);
        mainLayout.addView(guessedFolderSpinner);

        TextView textView2 = new TextView(context);
        textView2.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        textView2.setPadding(2, 2, 2, 2);
        textView2.setText(R.string.restart_after_setting_maps_folder);
        mainLayout.addView(textView2);
        try {
            guessedFoldersList = FileUtilities.getPossibleMapsFoldersList(sdcardFolder, LibraryConstants.DEFAULT_MAPSDIR); //$NON-NLS-1$
        } catch (java.lang.Exception e) {
            GPLog.error(this, "Error reading a custom tile source.", e); //$NON-NLS-1$
        }
        
        // mj10777: why is an empty string needed?
        // guessedFoldersList.add(0, ""); //$NON-NLS-1$

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, guessedFoldersList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        guessedFolderSpinner.setAdapter(adapter);
        if (customFolder != null) {
            for( int i = 0; i < guessedFoldersList.size(); i++ ) {
                if (guessedFoldersList.get(i).equals(customFolder.trim())) {
                    guessedFolderSpinner.setSelection(i);
                    break;
                }
            }
        }

        return mainLayout;
    }
    @Override
    protected void onBindDialogView( View v ) {
        super.onBindDialogView(v);

        editView.setText(customFolder);
        if (customFolder != null) {
            for( int i = 0; i < guessedFoldersList.size(); i++ ) {
                if (guessedFoldersList.get(i).equals(customFolder.trim())) {
                    guessedFolderSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    @Override
    protected void onDialogClosed( boolean positiveResult ) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            String newFolder = editView.getText().toString().trim();
            if ((newFolder.length() == 0) || (newFolder.equals(customFolder))) {
                // retrieve value from combo
                newFolder = guessedFolderSpinner.getSelectedItem().toString();
            }
            else {
             // mj10777: check if the user, in his/hers wisdom, has given us a valid directory
             File check_Dir=new File(sdcardFolder,newFolder);
             if (!check_Dir.exists())
              // tsk,tsk ...
              newFolder=customFolder;
            }
            if (!newFolder.equals(customFolder)) {
             // mj10777: only if the vlaue has changed
             if (callChangeListener(newFolder)) {
                 persistString(newFolder);
             }
           }
        }
    }

    @Override
    protected Object onGetDefaultValue( TypedArray a, int index ) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue( boolean restoreValue, Object defaultValue ) {

        if (restoreValue) {
            if (defaultValue == null) {
                customFolder = getPersistedString(""); //$NON-NLS-1$
            } else {
                customFolder = getPersistedString(defaultValue.toString());
            }
        } else {
            customFolder = defaultValue.toString();
        }
    }
}
