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
package eu.hydrologis.geopaparazzi.database;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import jsqlite.Exception;

import eu.geopaparazzi.library.database.spatial.SpatialDatabaseHandler;
import eu.geopaparazzi.library.util.ResourcesManager;
import android.content.Context;

/**
 * The spatial database manager.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialDatabaseManager {

    private List<SpatialDatabaseHandler> sdbHandlers = new ArrayList<SpatialDatabaseHandler>();

    private static SpatialDatabaseManager spatialDbManager = null;

    private SpatialDatabaseManager() {
    }

    public static SpatialDatabaseManager getInstance() {
        if (spatialDbManager == null) {
            spatialDbManager = new SpatialDatabaseManager();
        }
        return spatialDbManager;
    }

    public void init( Context context ) {
        File mapsDir = ResourcesManager.getInstance(context).getMapsDir();
        File[] sqliteFiles = mapsDir.listFiles(new FilenameFilter(){
            public boolean accept( File dir, String filename ) {
                return filename.endsWith(".sqlite");
            }
        });

        for( File sqliteFile : sqliteFiles ) {
            SpatialDatabaseHandler sdb = new SpatialDatabaseHandler(sqliteFile.getAbsolutePath());
            sdbHandlers.add(sdb);
        }
    }

    public List<SpatialDatabaseHandler> getSpatialDatabaseHandlers() {
        return sdbHandlers;
    }

    public void closeDatabases() throws Exception {
        for( SpatialDatabaseHandler sdbHandler : sdbHandlers ) {
            sdbHandler.close();
        }
    }

}
