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
package eu.geopaparazzi.library.util;

import java.io.File;
import java.util.Comparator;

/**
 * Comparator for file names.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FileNameComparator implements Comparator<File> {
    @Override
    public int compare(File f1, File f2) {
        String name1 = f1.getName();
        String name2 = f2.getName();

        return name1.compareToIgnoreCase(name2);
    }
}
