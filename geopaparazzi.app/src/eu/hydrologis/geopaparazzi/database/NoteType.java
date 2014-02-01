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

/**
 * The supported types of notes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum NoteType {
    /**
     * default type
     */
    POI(0, "POI"),
    /**
     * osm type
     */
    OSM(1, "OSM");

    private final int num;
    private final String def;

    NoteType( int num, String def ) {
        this.num = num;
        this.def = def;
    }

    /**
     * @return the id of teh type.
     */
    public int getTypeNum() {
        return num;
    }

    /**
     * @return the code of the type.
     */
    public String getDef() {
        return def;
    }
}