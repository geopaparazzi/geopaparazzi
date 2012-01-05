package eu.hydrologis.geopaparazzi.osm.filters;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of constraints.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Constraints {

    private List<IConstraint> constraints = new ArrayList<IConstraint>();

    public void addConstraint( IConstraint constraint ) {
        if (!constraints.contains(constraint)) {
            constraints.add(constraint);
        }
    }

    public void removeConstraint( IConstraint constraint ) {
        if (constraints.contains(constraint)) {
            constraints.remove(constraint);
        }
    }

    /**
     * Checks if all the {@link IConstraint}s in the current set are valid.
     * @param object 
     * 
     * @param object the object to check.
     * @return <code>true</code> if all the constraints are valid.
     */
    public boolean isValid( Object object ) {
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
