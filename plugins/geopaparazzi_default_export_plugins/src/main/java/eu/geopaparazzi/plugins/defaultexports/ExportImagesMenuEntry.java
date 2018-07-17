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

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.database.DaoImages;
import eu.geopaparazzi.core.database.DaoMetadata;
import eu.geopaparazzi.core.database.objects.Metadata;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.plugin.types.MenuEntry;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.TimeUtilities;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ExportImagesMenuEntry extends MenuEntry {


    private Context serviceContext;

    public ExportImagesMenuEntry(Context context) {
        this.serviceContext = context;
    }

    @Override
    public String getLabel() {
        return serviceContext.getString(eu.geopaparazzi.core.R.string.export_images);
    }

    @Override
    public void onClick(IActivitySupporter clickActivityStarter) {
        exportImages(clickActivityStarter.getContext());

    }

    private void exportImages(final Context context) {
        try {
            String projectName = DaoMetadata.getProjectName();
            if (projectName == null) {
                projectName = "geopaparazzi_images_";
            } else {
                projectName += "_images_";
            }
            File exportDir = ResourcesManager.getInstance(GeopaparazziApplication.getInstance()).getApplicationExportDir();
            final File outFolder = new File(exportDir, projectName + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()));
            if (!outFolder.mkdir()) {
                GPDialogs.warningDialog(context, context.getString(eu.geopaparazzi.core.R.string.export_img_unable_to_create_folder) + outFolder, null);
                return;
            }
            final List<Image> imagesList = DaoImages.getImagesList(false, false);
            if (imagesList.size() == 0) {
                GPDialogs.infoDialog(context, context.getString(eu.geopaparazzi.core.R.string.no_images_in_project), null);
                return;
            }


            final DaoImages imageHelper = new DaoImages();
            StringAsyncTask exportImagesTask = new StringAsyncTask(context) {
                protected String doBackgroundWork() {
                    try {
                        for (int i = 0; i < imagesList.size(); i++) {
                            Image image = imagesList.get(i);
                            try {
                                byte[] imageData = imageHelper.getImageData(image.getId());
                                if (imageData != null) {
                                    File imageFile = new File(outFolder, image.getName());

                                    FileOutputStream fos = new FileOutputStream(imageFile);
                                    fos.write(imageData);
                                    fos.close();
                                }
                            } catch (IOException e) {
                                GPLog.error(this, "For file: " + image.getName(), e);
                            } finally {
                                publishProgress(i);
                            }
                        }
                    } catch (Exception e) {
                        return "ERROR: " + e.getLocalizedMessage();
                    }
                    return "";
                }

                protected void doUiPostWork(String response) {
                    if (response == null) response = "";
                    if (response.length() != 0) {
                        GPDialogs.warningDialog(context, response, null);
                    } else {
                        GPDialogs.infoDialog(context, context.getString(eu.geopaparazzi.core.R.string.export_img_ok_exported) + outFolder, null);
                    }
                }
            };
            exportImagesTask.setProgressDialog(context.getString(eu.geopaparazzi.core.R.string.export_uc), context.getString(eu.geopaparazzi.core.R.string.export_img_processing), false, imagesList.size());
            exportImagesTask.execute();


        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(context, e, null);
        }
    }


}