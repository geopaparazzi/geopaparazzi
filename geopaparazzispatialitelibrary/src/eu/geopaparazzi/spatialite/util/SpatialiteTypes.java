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
package eu.geopaparazzi.spatialite.util;

/**
 * Spatialite db types.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum SpatialiteTypes {
    /**
     * 
     */
    MBTILES("mbtiles", ".mbtiles", 0),
    /**
     * 
     */
    DB("db", ".db", 1),
    /**
     * 
     */
    SQLITE("sqlite", ".sqlite", 2),
    /**
     * 
     */
    GPKG("gpkg", ".gpkg", 3);

    private String name;
    private String extension;
    private int code;

    /**
     * @param name a name for the db type. 
     * @param extension the extension used by the db type.
     * @param code a code for the db type.
     */
    private SpatialiteTypes( String name, String extension, int code ) {
        this.name = name;
        this.extension = extension;
        this.code = code;
    }

    /**
     * @return the db type's name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the db type's extension.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * @return the db's type code.
     */
    public int getCode() {
        return code;
    }
}
