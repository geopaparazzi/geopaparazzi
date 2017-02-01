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
package eu.geopaparazzi.library.util.types;

/**
 * Spatial data types with extensions and codes.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum ESpatialDataSources {
    /**
     * Mbtiles based database.
     */
    MBTILES("MBTiles", ".mbtiles", 0, true, "MBT"),
    /**
     * A spatialite/sqlite database.
     */
    DB("Db", ".db", 1, true, "Db"),
    /**
     * A spatialite/sqlite database.
     */
    SQLITE("SQLite", ".sqlite", 2, true, "SQLi"),
    /**
     * A geopackage database.
     */
    GPKG("Gpkg", ".gpkg", 3, true, "Gpkg"),
    /**
     * A mapsforge map file.
     */
    MAP("Map", ".map", 4, false, "Map"),
    /**
     * A mapsurl definition file.
     */
    MAPURL("Mapurl", ".mapurl", 5, false, "Mapurl"),
    /**
     * A Rasterlite2  database.
     */
    RASTERLITE2("RasterLite2", ".rl2", 6, true, "RL2");

    private String name;
    private String extension;
    private int code;
    private boolean isSpatialiteBased;
    private String shortName;

    /**
     * @param name      a name for the db type.
     * @param extension the extension used by the db type.
     * @param code      a code for the db type.
     */
    private ESpatialDataSources(String name, String extension, int code, boolean isSpatialiteBased, String shortName) {
        this.name = name;
        this.extension = extension;
        this.code = code;
        this.isSpatialiteBased = isSpatialiteBased;
        this.shortName = shortName;
    }

    /**
     * Get the type for a given code.
     *
     * @param code the code.
     * @return the data type.
     */
    public static ESpatialDataSources getType4Code(int code) {
        for (ESpatialDataSources type : values()) {
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
    public static ESpatialDataSources getType4Name(String name) {
        for (ESpatialDataSources type : values()) {
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
    public static int getCode4Name(String name) {
        for (ESpatialDataSources type : values()) {
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
     * @return the db type's name.
     */
    public static String getTypeName4FileName(String fileName) {
        for (ESpatialDataSources type : values()) {
            if (fileName.toLowerCase().endsWith(type.getExtension())) {
                return type.getTypeName();
            }
        }
        return "unknown";
    }

    /**
     * @return the db type's extension.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * @return list of supported vector file extensions (without dot).
     */
    public static String[] getSupportedVectorExtensions() {
        return new String[]{SQLITE.getExtension().substring(1), GPKG.getExtension().substring(1), DB.getExtension().substring(1)};
    }

    /**
     * @return list of supported tile sources file extensions (without dot).
     */
    public static String[] getSupportedTileSourcesExtensions() {
        return new String[]{MAPURL.getExtension().substring(1), MAP.getExtension().substring(1), SQLITE.getExtension().substring(1), MBTILES.getExtension().substring(1), DB.getExtension().substring(1)};
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

    /*
     * @return a short name for the db type.
     */
    public String getShortTypeName() {
        return shortName;
    }
}
