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
package eu.geopaparazzi.spatialite.database.spatial.core.enums;

/**
 * Spatial data types with extensions and codes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum SpatialDataType {
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
    MAPURL("mapurl", ".mapurl", 5, false),
    /**
     * A Rasterlite2 Image in a spatialite 4.2.0 database. 
     * - avoids .db being read 2x
     * - real spatialite .atlas files can also be read
     */
    RASTERLITE2("RasterLite2", ".atlas", 6, true);

    private String name;
    // extention must be unique 
    // - otherwise will be read in twice in SpatialDatabasesManager.init
    private String extension;
    private int code;
    private boolean isSpatialiteBased;

    /**
     * @param name a name for the db type. 
     * @param extension the extension used by the db type.
     * @param code a code for the db type.
     */
    private SpatialDataType( String name, String extension, int code, boolean isSpatialiteBased ) {
        this.name = name;
        this.extension = extension;
        this.code = code;
        this.isSpatialiteBased = isSpatialiteBased;
    }

    /**
     * Get the type for a given code.
     * 
     * @param code the code.
     * @return the data type.
     */
    public static SpatialDataType getType4Code( int code ) {
        for( SpatialDataType type : values() ) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("No such type known: " + code);
    }

    /**
     * Get the type for a given name.
     * 
     * @param name the name.
     * @return the data type.
     */
    public static SpatialDataType getType4Name( String name ) {
        for( SpatialDataType type : values() ) {
            if (type.getTypeName().equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No such type known: " + name);
    }

    /**
     * Get the code for a given name.
     * 
     * @param name the name.
     * @return the code.
     */
    public static int getCode4Name( String name ) {
        for( SpatialDataType type : values() ) {
            if (type.getTypeName().equals(name)) {
                return type.getCode();
            }
        }
        throw new IllegalArgumentException("No such type known: " + name);
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
