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
package eu.geopaparazzi.plugins.profileimport;

import android.content.Intent;
import android.os.IBinder;

import eu.geopaparazzi.library.plugin.PluginService;
import eu.geopaparazzi.library.plugin.types.MenuEntryList;


/**
 * Menu provider for profile imports.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ProfileImportsMenuProvider extends PluginService {
    private static final String NAME = "ProfileImportsMenuProvider";
    private MenuEntryList list = null;

    public ProfileImportsMenuProvider() {
        super(NAME);
    }

    public IBinder onBind(Intent intent) {
        if (list == null) {
            list = new MenuEntryList();
            list.addEntry(new ImportProfilesMenuEntry(getApplicationContext()));
        }
        return list;
    }


}
