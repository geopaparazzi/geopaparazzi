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
import java.util.ArrayList;
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
public class ExportBookmarksMenuEntry extends MenuEntry {


    private Context serviceContext;

    public ExportBookmarksMenuEntry(Context context) {
        this.serviceContext = context;
    }

    @Override
    public String getLabel() {
        return serviceContext.getString(eu.geopaparazzi.core.R.string.bookmarks);
    }

    @Override
    public void onClick(IActivitySupporter clickActivityStarter) {
        final Context context = clickActivityStarter.getContext();
        exportBookmarks(context);
    }

    private void exportBookmarks(Context context) {

        try {
            List<Bookmark> allBookmarks = DaoBookmarks.getAllBookmarks();
            TreeSet<String> bookmarksNames = new TreeSet<>();
            for (Bookmark bookmark : allBookmarks) {
                String tmpName = bookmark.getName();
                bookmarksNames.add(tmpName.trim());
            }

            List<String> namesToNOTAdd = new ArrayList<>();
            ResourcesManager resourcesManager = ResourcesManager.getInstance(context);
            File sdcardDir = resourcesManager.getSdcardDir();
            File bookmarksfile = new File(sdcardDir, "bookmarks.csv"); //$NON-NLS-1$
            StringBuilder sb = new StringBuilder();
            if (bookmarksfile.exists()) {
                List<String> bookmarksList = FileUtilities.readfileToList(bookmarksfile);
                for (String bookmarkLine : bookmarksList) {
                    String[] split = bookmarkLine.split(","); //$NON-NLS-1$
                    // bookmarks are of type: Agritur BeB In Valle, 45.46564, 11.58969, 12
                    if (split.length < 3) {
                        continue;
                    }
                    String name = split[0].trim();
                    if (bookmarksNames.contains(name)) {
                        namesToNOTAdd.add(name);
                    }
                }
                for (String string : bookmarksList) {
                    sb.append(string).append("\n");
                }
            }
            int exported = 0;
            for (Bookmark bookmark : allBookmarks) {
                String name = bookmark.getName().trim();
                if (!namesToNOTAdd.contains(name)) {
                    sb.append(name);
                    sb.append(",");
                    sb.append(bookmark.getLat());
                    sb.append(",");
                    sb.append(bookmark.getLon());
                    sb.append(",");
                    sb.append(bookmark.getZoom());
                    sb.append("\n");
                    exported++;
                }
            }

            FileUtilities.writefile(sb.toString(), bookmarksfile);
            if (bookmarksfile.exists()) {
                GPDialogs.infoDialog(context, context.getString(eu.geopaparazzi.core.R.string.bookmarks_exported) + exported, null);
            } else {
                GPDialogs.infoDialog(context, context.getString(eu.geopaparazzi.core.R.string.bookmarks_exported_newfile) + exported, null);
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.warningDialog(context, context.getString(eu.geopaparazzi.core.R.string.bookmarks_exported_error), null);
        }

    }


}