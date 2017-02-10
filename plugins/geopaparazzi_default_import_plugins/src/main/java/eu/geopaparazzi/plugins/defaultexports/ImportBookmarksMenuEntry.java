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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.TreeSet;

import eu.geopaparazzi.core.database.DaoBookmarks;
import eu.geopaparazzi.core.database.objects.Bookmark;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.plugin.types.MenuEntry;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImportBookmarksMenuEntry extends MenuEntry {


    private Context serviceContext;

    public ImportBookmarksMenuEntry(Context context) {
        this.serviceContext = context;
    }

    @Override
    public String getLabel() {
        return serviceContext.getString(eu.geopaparazzi.core.R.string.bookmarks);
    }

    @Override
    public void onClick(IActivitySupporter clickActivityStarter) {
        final Context context = clickActivityStarter.getContext();
        try {
            ResourcesManager resourcesManager = ResourcesManager.getInstance(context);

            final File sdcardDir = resourcesManager.getSdcardDir();
            File[] bookmarksfileList = sdcardDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return filename.startsWith("bookmarks") && filename.endsWith(".csv");
                }
            });
            if (bookmarksfileList.length == 0) {
                GPDialogs.warningDialog(context, context.getString(eu.geopaparazzi.core.R.string.no_bookmarks_csv), null);
                return;
            }

            final String[] items = new String[bookmarksfileList.length];
            for (int i = 0; i < items.length; i++) {
                items[i] = bookmarksfileList[i].getName();
            }

            new AlertDialog.Builder(context).setSingleChoiceItems(items, 0, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            String selectedItem = items[selectedPosition];
                            dialog.dismiss();
                            doImport(context, sdcardDir, selectedItem);
                        }
                    }).show();
        } catch (Exception e1) {
            GPLog.error(this, null, e1);
            GPDialogs.warningDialog(context, context.getString(eu.geopaparazzi.core.R.string.bookmarks_import_error), null);
        }
    }


    private void doImport(final Context context, File sdcardDir, String fileName) {
        File bookmarksfile = new File(sdcardDir, fileName); //$NON-NLS-1$
        if (bookmarksfile.exists()) {
            try {
                // try to load it
                List<Bookmark> allBookmarks = DaoBookmarks.getAllBookmarks();
                TreeSet<String> bookmarksNames = new TreeSet<>();
                for (Bookmark bookmark : allBookmarks) {
                    String tmpName = bookmark.getName();
                    bookmarksNames.add(tmpName.trim());
                }

                List<String> bookmarksList = FileUtilities.readfileToList(bookmarksfile);
                int imported = 0;
                for (String bookmarkLine : bookmarksList) {
                    String[] split = bookmarkLine.split(","); //$NON-NLS-1$
                    // bookmarks are of type: Agritur BeB In Valle, 45.46564, 11.58969, 12
                    if (split.length < 3) {
                        continue;
                    }
                    String name = split[0].trim();
                    if (bookmarksNames.contains(name)) {
                        continue;
                    }
                    try {
                        double zoom = 16.0;
                        if (split.length == 4) {
                            zoom = Double.parseDouble(split[3]);
                        }
                        double lat = Double.parseDouble(split[1]);
                        double lon = Double.parseDouble(split[2]);
                        DaoBookmarks.addBookmark(lon, lat, name, zoom, -1, -1, -1, -1);
                        imported++;
                    } catch (Exception e) {
                        GPLog.error(this, null, e);
                    }
                }

                GPDialogs.infoDialog(context, context.getString(eu.geopaparazzi.core.R.string.successfully_imported_bookmarks) + imported, null);
            } catch (IOException e) {
                GPLog.error(this, null, e);
                GPDialogs.infoDialog(context, context.getString(eu.geopaparazzi.core.R.string.error_bookmarks_import), null);
            }
        } else {
            GPDialogs.warningDialog(context, context.getString(eu.geopaparazzi.core.R.string.no_bookmarks_csv), null);
        }
    }


}