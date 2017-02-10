package eu.geopaparazzi.plugins.defaultimports;

import android.app.Activity;
import android.content.Intent;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import java.io.File;

import eu.geopaparazzi.core.ui.dialogs.GpxImportDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.plugin.PluginService;
import eu.geopaparazzi.library.plugin.types.MenuEntry;
import eu.geopaparazzi.library.plugin.types.MenuEntryList;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.FileTypes;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivityStarter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;


/**
 * Menu provider for all default imports.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DefaultImportsMenuProvider extends PluginService {
    private static final String NAME = "DefauktsImportsMenuProvider";
    private MenuEntryList list = null;

    public DefaultImportsMenuProvider() {
        super(NAME);
    }

    public IBinder onBind(Intent intent) {
        if (list == null) {
            list = new MenuEntryList();
            list.addEntry(new ImportGpxMenuEntry());
        }
        return list;
    }


    public class ImportGpxMenuEntry extends MenuEntry {
        @Override
        public String getLabel() {
            return getString(eu.geopaparazzi.core.R.string.gpx);
        }

        @Override
        public void onClick(IActivityStarter clickActivityStarter) {
            String title = getString(eu.geopaparazzi.core.R.string.select_gpx_file);
            try {
                AppsUtilities.pickFile(clickActivityStarter, requestCode, title, new String[]{FileTypes.GPX.getExtension()}, null);
            } catch (Exception e) {
                GPLog.error(this, null, e);
                GPDialogs.errorDialog(clickActivityStarter.getContext(), e, null);
            }
        }

        @Override
        public void onActivityResultExecute(AppCompatActivity callingActivity, int requestCode, int resultCode, Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    String filePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                    if (!filePath.toLowerCase().endsWith(FileTypes.GPX.getExtension())) {
                        GPDialogs.warningDialog(callingActivity, getString(eu.geopaparazzi.core.R.string.no_gpx_selected), null);
                        return;
                    }
                    File file = new File(filePath);
                    if (file.exists()) {
                        Utilities.setLastFilePath(callingActivity, filePath);
                        GpxImportDialogFragment gpxImportDialogFragment = GpxImportDialogFragment.newInstance(file.getAbsolutePath());
                        gpxImportDialogFragment.show(callingActivity.getSupportFragmentManager(), "gpx import");
                    }
                } catch (Exception e) {
                    GPDialogs.errorDialog(callingActivity, e, null);
                }
            }
        }
    }

}
