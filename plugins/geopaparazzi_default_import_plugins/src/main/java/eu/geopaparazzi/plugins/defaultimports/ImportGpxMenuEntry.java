package eu.geopaparazzi.plugins.defaultimports;

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
import eu.geopaparazzi.library.util.IActivityStupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;

public class ImportGpxMenuEntry extends MenuEntry {

    private final Context serviceContext;
    private IActivityStupporter clickActivityStarter;

    public ImportGpxMenuEntry(Context context) {
        this.serviceContext = context;
    }

    @Override
    public String getLabel() {
        return serviceContext.getString(eu.geopaparazzi.core.R.string.gpx);
    }

    @Override
    public void onClick(IActivityStupporter clickActivityStarter) {
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
