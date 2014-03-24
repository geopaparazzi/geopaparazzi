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
package eu.hydrologis.geopaparazzi.tantomapurls;

import java.util.Locale;

/**
 * The class holding mapurls download info.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TantoMapurl {

    /**
     * The server unique id for the mapurl.
     */
    public long id;
    /**
     * The layer title.
     */
    public String title;
    /**
     * The service name.
     */
    public String service;

    /**
     * Checks if the project info match the supplied string.
     * 
     * @param pattern the pattern to match.
     * @return <code>true</code> if the pattern matches any info.
     */
    public boolean matches( String pattern ) {
        Locale locale = Locale.US;
        pattern = pattern.toLowerCase(locale);
        if (title.toLowerCase(locale).contains(pattern)) {
            return true;
        }
        if (service.toLowerCase(locale).contains(pattern)) {
            return true;
        }
        if (String.valueOf(id).toLowerCase(locale).contains(pattern)) {
            return true;
        }
        return false;
    }
}
