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
package eu.geopaparazzi.library.forms.constraints;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of constraints.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Constraints {

    private List<IConstraint> constraints = new ArrayList<IConstraint>();

    /**
     * Add a constraint.
     * 
     * @param constraint the constraint to add.
     */
    public void addConstraint( IConstraint constraint ) {
        if (!constraints.contains(constraint)) {
            constraints.add(constraint);
        }
    }

    /**
     * Remove a constraint.
     * 
     * @param constraint the constraint to remove.
     */
    public void removeConstraint( IConstraint constraint ) {
        if (constraints.contains(constraint)) {
            constraints.remove(constraint);
        }
    }

    /**
     * Checks if all the {@link IConstraint}s in the current set are valid.
     * 
     * @param object the object to check.
     * @return <code>true</code> if all the constraints are valid.
     */
    public boolean isValid( Object object ) {
        if (object == null) {
            return false;
        }
        boolean isValid = true;
        for( IConstraint constraint : constraints ) {
            constraint.applyConstraint(object);
            isValid = isValid && constraint.isValid();
            if (!isValid) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return description.
     */
    @SuppressWarnings("nls")
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        for( IConstraint constraint : constraints ) {
            sb.append(",");
            sb.append(constraint.getDescription());
        }

        if (sb.length() == 0) {
            return "";
        }
        String description = sb.substring(1);
        description = "( " + description + " )";
        return description;
    }

}
