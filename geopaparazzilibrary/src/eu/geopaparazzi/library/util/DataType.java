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
package eu.geopaparazzi.library.util;

/**
 * Common Data Types.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum DataType {
    /** * */
    TEXT(0, String.class, "''"), //
    /** * */
    DOUBLE(1, Double.class, "-1.0"), //
    /** * */
    PHONE(2, String.class, "''"), //
    /** * */
    DATE(3, String.class, "''"), //
    /** * */
    INTEGER(4, Integer.class, "0"), //
    /** * */
    FLOAT(5, Float.class, "0.0"),
    /** * */
    BLOB(6, Object.class, "''");

    private int code;
    private Class< ? > clazz;
    private String defaultValueForSql;

    private DataType( int code, Class< ? > clazz, String defaultValueForSql ) {
        this.code = code;
        this.clazz = clazz;
        this.defaultValueForSql = defaultValueForSql;
    }

    /**
     * @return the code of this type.
     */
    public int getCode() {
        return code;
    }

    /**
     * @return the class for the type.
     */
    public Class< ? > getClazz() {
        return clazz;
    }

    /**
     * @return the default value for an sql query.
     */
    public String getDefaultValueForSql() {
        return defaultValueForSql;
    }

    /**
     * Get the type from the code.
     * 
     * @param code the code.
     * @return the {@link DataType}.
     */
    public static DataType getType4Code( int code ) {
        DataType[] values = values();
        for( DataType dataType : values ) {
            if (dataType.getCode() == code) {
                return dataType;
            }
        }
        throw new IllegalArgumentException("Unknown datatype for code: " + code); //$NON-NLS-1$
    }

    /**
     * Get the type from the name.
     * 
     * @param name the name.
     * @return the {@link DataType}.
     */
    public static DataType getType4Name( String name ) {
        DataType[] values = values();
        for( DataType dataType : values ) {
            if (dataType.name().equals(name)) {
                return dataType;
            }
        }
        throw new IllegalArgumentException("Unknown datatype for name: " + name); //$NON-NLS-1$
    }

    /**
     * Get the datatype from a given sqlite code.
     * 
     * <p>The codes are the ones defined in jsqlite.Constants.<br>
     * Currently supported are:<br>
     * <pre>
     * SQLITE_INTEGER = 1;
     * SQLITE_FLOAT = 2;
     * SQLITE_BLOB = 4;
     * SQLITE3_TEXT = 3;
     * SQLITE_NUMERIC = -1;
     * SQLITE_TEXT = 3;
     * SQLITE2_TEXT = -2;
     * </pre>
     * 
     * @param sqliteCode the code.
     * @return the {@link DataType}.
     */
    public static DataType getType4SqliteCode( int sqliteCode ) {

        switch( sqliteCode ) {
        case 1:
            return INTEGER;
        case 2:
            return FLOAT;
        case 4:
            return BLOB;
        case 3:
        case -2:
            return TEXT;
        case -1:
            return DOUBLE;
        }
        return null;
    }

}
