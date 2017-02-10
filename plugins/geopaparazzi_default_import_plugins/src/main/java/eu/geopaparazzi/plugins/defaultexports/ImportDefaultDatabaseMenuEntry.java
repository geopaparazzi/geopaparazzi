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
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.plugin.types.MenuEntry;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TextRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImportDefaultDatabaseMenuEntry extends MenuEntry {


    private Context serviceContext;

    public ImportDefaultDatabaseMenuEntry(Context context) {
        this.serviceContext = context;
    }

    @Override
    public String getLabel() {
        return serviceContext.getString(eu.geopaparazzi.core.R.string.default_database);
    }

    @Override
    public void onClick(IActivitySupporter clickActivityStarter) {

        Context context = clickActivityStarter.getContext();
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        }else{
            GPDialogs.warningDialog(context, "Wrong context. This needs to be an activity.", null);
            return;
        }
        final Activity _activity = activity;
        String ts = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date());
        String newName = "spatialite_" + ts + ".sqlite";
        GPDialogs.inputMessageDialog(_activity, context.getString(eu.geopaparazzi.core.R.string.name_new_teample_db), newName, new TextRunnable() {
            @Override
            public void run() {

                try {
                    File sdcardDir = ResourcesManager.getInstance(_activity).getSdcardDir();
                    File newDbFile = new File(sdcardDir, theTextToRunOn);

                    AssetManager assetManager = _activity.getAssets();
                    InputStream inputStream = assetManager.open(LibraryConstants.GEOPAPARAZZI_TEMPLATE_DB_NAME);

                    FileUtilities.copyFile(inputStream, new FileOutputStream(newDbFile));

                    _activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GPDialogs.infoDialog(_activity, _activity.getString(eu.geopaparazzi.core.R.string.new_template_db_create), null);
                        }
                    });
                } catch (final Exception e) {
                    GPLog.error(_activity, null, e);

                    _activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GPDialogs.errorDialog(_activity, e, null);
                        }
                    });
                }

            }
        });

    }


}