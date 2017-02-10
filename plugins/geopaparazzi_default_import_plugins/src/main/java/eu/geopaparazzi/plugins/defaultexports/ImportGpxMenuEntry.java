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
package eu.geopaparazzi.plugins.defaultexports;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.io.File;

import eu.geopaparazzi.core.ui.dialogs.GpxImportDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.plugin.types.MenuEntry;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.FileTypes;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImportGpxMenuEntry extends MenuEntry {

    private final Context serviceContext;
    private IActivitySupporter clickActivityStarter;

    public ImportGpxMenuEntry(Context context) {
        this.serviceContext = context;
    }

    @Override
    public String getLabel() {
        return serviceContext.getString(eu.geopaparazzi.core.R.string.gpx);
    }

    @Override
    public void onClick(IActivitySupporter clickActivityStarter) {
        this.clickActivityStarter = clickActivityStarter;
        String title = clickActivityStarter.getContext().getString(eu.geopaparazzi.core.R.string.select_gpx_file);
        try {
            AppsUtilities.pickFile(clickActivityStarter, requestCode, title, new String[]{FileTypes.GPX.getExtension()}, null);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(clickActivityStarter.getContext(), e, null);
        }
    }

    @Override
    public void onActivityResultExecute( int requestCode, int resultCode, Intent data) {
        Context context = clickActivityStarter.getContext();
        if (resultCode == Activity.RESULT_OK) {
            try {

                String filePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                if (!filePath.toLowerCase().endsWith(FileTypes.GPX.getExtension())) {
                    GPDialogs.warningDialog(context, context.getString(eu.geopaparazzi.core.R.string.no_gpx_selected), null);
                    return;
                }
                File file = new File(filePath);
                if (file.exists()) {
                    Utilities.setLastFilePath(context, filePath);
                    GpxImportDialogFragment gpxImportDialogFragment = GpxImportDialogFragment.newInstance(file.getAbsolutePath());
                    gpxImportDialogFragment.show(clickActivityStarter.getSupportFragmentManager(), "gpx import");
                }
            } catch (Exception e) {
                GPDialogs.errorDialog(context, e, null);
            }
        }
    }
}
