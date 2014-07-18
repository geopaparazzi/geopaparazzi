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
 * The different vector layer query modes.
 *
 * @author Mark Johnson
 * @author Andrea Antonello
 */
public enum VectorLayerQueryModes {
    // Mode Types: 0=strict ; 1=tolerant ; 2=corrective ; 3=corrective with CreateSpatialIndex
    // SpatialiteLibraryConstants.PREFS_KEY_SPATIALITE_RECOVERY_MODE
    // read in MapsDirManager.init.
    // - Set to 3 if desired. After compleation of init, turn back to 0
    // - Set SpatialiteLibraryConstants.PREFS_KEY_SPATIALITE_RECOVERY_MODE to false
    //    VECTOR_LAYERS_QUERY_MODE,// = 0;

    STRICT(0),
    TOLERANT(1),
    CORRECTIVE(2),
    CORRECTIVEWITHINDEX(3);

    private int code;

    VectorLayerQueryModes(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * Get the VectorLayerQueryModes for a given code.
     *
     * @param code the code.
     * @return the mode.
     */
    public VectorLayerQueryModes forCode(int code) {
        switch (code) {
            case 0:
                return STRICT;
            case 1:
                return TOLERANT;
            case 2:
                return CORRECTIVE;
            case 3:
                return CORRECTIVEWITHINDEX;
            default:
                break;
        }
        throw new IllegalArgumentException("No mode defined for code: " + code);
    }

}
