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
 * Spatialite database types with descriptions and codes.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum SpatialiteDatabaseType {
    /**
     * Unknown database.
     */
    UNKNOWN("Unknown Database Type", -1),
    /**
     * spatialite 2.4 and 3 database.
     */
    SPATIALITE3("Spatialite 3", 3),
    /**
     * spatialite 4 database.
     */
    SPATIALITE4("Spatialite 4", 4),
    /**
     * GeoPackage database.
     */
    GEOPACKAGE("GeoPackage", 10);

    private String name;
    private int code;

    /**
     * @param name a name for the db type.
     * @param code a code for the db type.
     */
    private SpatialiteDatabaseType( String name, int code ) {
        this.name = name;
        this.code = code;
    }

    /**
     * Get the type for a given code.
     *
     * @param code the code.
     * @return the data type.
     */
    public static SpatialiteDatabaseType getType4Code( int code ) {
        for( SpatialiteDatabaseType type : values() ) {
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
    public static SpatialiteDatabaseType getType4Name( String name ) {
        for( SpatialiteDatabaseType type : values() ) {
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
        for( SpatialiteDatabaseType type : values() ) {
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
     * @return the db's type code.
     */
    public int getCode() {
        return code;
    }

}
