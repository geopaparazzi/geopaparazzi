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
package eu.geopaparazzi.mapsforge.mapsdirmanager.sourcesview;

import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.List;

import eu.geopaparazzi.library.database.GPLog;

/**
 * A dialog that permits multiple selection.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapTypesChoiceDialog {

    /**
     * Open the dialog.
     *
     * @param title the title for the dialog.
     * @param sourcesTreeListActivity the parent activity.
     * @param items the items to be presented in the dialog.
     * @param checkedValues the selected state of the items.
     */
    public void open(String title, final SourcesTreeListActivity sourcesTreeListActivity,  final List<String> items, final boolean[] checkedValues ) {

        DialogInterface.OnMultiChoiceClickListener dialogListener = new DialogInterface.OnMultiChoiceClickListener(){
            @Override
            public void onClick( DialogInterface dialog, int which, boolean isChecked ) {
                checkedValues[which] = isChecked;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(sourcesTreeListActivity);
        builder.setTitle(title);
        builder.setMultiChoiceItems(items.toArray(new String[items.size()]), checkedValues, dialogListener);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                try {
                    sourcesTreeListActivity.refreshData();
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
