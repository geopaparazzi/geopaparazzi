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
 * Spatial data types with extensions and codes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum SpatialDataTypes {
    /**
     * Mbtiles based database.
     */
    MBTILES("mbtiles", ".mbtiles", 0, true),
    /**
     * A spatialite/sqlite database.
     */
    DB("db", ".db", 1, true),
    /**
     * A spatialite/sqlite database.
     */
    SQLITE("sqlite", ".sqlite", 2, true),
    /**
     * A geopackage database. 
     */
    GPKG("gpkg", ".gpkg", 3, true),
    /**
     * A mapsforge map file.
     */
    MAP("map", ".map", 4, false),
    /**
     * A mapsurl definition file.
     */
    MAPURL("mapurl", ".mapurl", 5, false);

    private String name;
    private String extension;
    private int code;
    private boolean isSpatialiteBased;

    /**
     * @param name a name for the db type. 
     * @param extension the extension used by the db type.
     * @param code a code for the db type.
     */
    private SpatialDataTypes( String name, String extension, int code, boolean isSpatialiteBased ) {
        this.name = name;
        this.extension = extension;
        this.code = code;
        this.isSpatialiteBased = isSpatialiteBased;
    }

    /**
     * @return the db type's name.
     */
    public String getTypeName() {
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

    /**
     * @return <code>true</code> if the type is spatialite based. 
     */
    public boolean isSpatialiteBased() {
        return isSpatialiteBased;
    }
}
