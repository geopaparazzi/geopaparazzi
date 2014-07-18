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
 * Spatialite database versions with codes.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum SpatialiteVersion {
    /**
     * not a spatialite version
     */
    NO_SPATIALITE("not a spatialite version", 0),
    /**
     * until 2.3.1.
     */
    UNTIL_2_3_1("until 2.3.1", 1),
    /**
     * until 2.4.0
     */
    UNTIL_2_4_0("until 2.4.0", 2),
    /**
     * until 3.1.0-RC2
     */
    UNTIL_3_1_0_RC2("until 3.1.0-RC2", 3),
    /**
     * after 4.0.0-RC1
     */
    AFTER_4_0_0_RC1("after 4.0.0-RC1", 4),
    /**
     * not found, pre 2.4.0
     */
    SRS_WKT__NOTFOUND_PRE_2_4_0("not found, pre 2.4.0", 1000),
    /**
     * from 2.4.0 to 3.1.0
     */
    SRS_WKT__2_4_0_to_3_1_0("from 2.4.0 to 3.1.0", 1001),
    /**
     * starting with 4.0.0
     */
    SRS_WKT__FROM_4_0_0("starting with 4.0.0", 1002);

    private String description;
    private int code;

    /**
     * @param description a description for the db type.
     * @param code        a code for the db type.
     */
    private SpatialiteVersion(String description, int code) {
        this.description = description;
        this.code = code;
    }

    /**
     * @return the db type's description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the db's type code.
     */
    public int getCode() {
        return code;
    }

}
