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

import eu.geopaparazzi.library.util.Utilities;

/**
 * A numeric range constraint.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RangeConstraint implements IConstraint {

    private boolean isValid = false;

    private double lowValue;
    private final boolean includeLow;
    private final double highValue;
    private final boolean includeHigh;

    /**
     * @param low low value.
     * @param includeLow if <code>true</code>, include low.
     * @param high high value.
     * @param includeHigh if <code>true</code>, include high.
     */
    public RangeConstraint( Number low, boolean includeLow, Number high, boolean includeHigh ) {
        this.includeLow = includeLow;
        this.includeHigh = includeHigh;
        highValue = high.doubleValue();
        lowValue = low.doubleValue();
    }

    public void applyConstraint( Object value ) {
        if (value instanceof String) {
            String str = (String) value;
            if (str.length() == 0) {
                // empty can be still ok, we just check for ranges if we have a value
                isValid = true;
                return;
            }
        }

        Double adapted = Utilities.adapt(value, Double.class);
        if (adapted != null) {
            double doubleValue = adapted.doubleValue();
            if (//
            ((includeLow && doubleValue >= lowValue) || (!includeLow && doubleValue > lowValue)) && //
                    ((includeHigh && doubleValue <= highValue) || (!includeHigh && doubleValue < highValue)) //
            ) {
                isValid = true;
            } else {
                isValid = false;
            }
        } else {
            isValid = false;
        }
    }

    public boolean isValid() {
        return isValid;
    }

    @SuppressWarnings("nls")
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        if (includeLow) {
            sb.append("[");
        } else {
            sb.append("(");
        }
        sb.append(lowValue);
        sb.append(",");
        sb.append(highValue);
        if (includeHigh) {
            sb.append("]");
        } else {
            sb.append(")");
        }
        return sb.toString();
    }

}
