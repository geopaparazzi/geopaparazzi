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

import static java.lang.Math.max;

/**
 * A wrapper for a dynamic growing array.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 2.6
 */
public class DynamicDoubleArray {
    private double[] internalArray = null;
    private final int growingSize;
    private int lastIndex = 0;

    /**
     * Create the array of default size.
     */
    public DynamicDoubleArray() {
        this(50, 50);
    }

    /**
     * Create the array with an initial size.
     * 
     * @param initalSize the initial size.
     */
    public DynamicDoubleArray( int initalSize ) {
        this(initalSize, 50);
    }

    /**
     * Create the array with an initial size.
     * 
     * @param initalSize the initial size.
     * @param growingSize the size to grow the array additionally, if the array was too small. 
     */
    public DynamicDoubleArray( int initalSize, int growingSize ) {
        this.growingSize = growingSize;
        internalArray = new double[initalSize];
    }

    /**
     * Safe set the value in a certain position.
     * 
     * <p>If the array is smaller than the position, the array is extended and substituted.</p>
     * 
     * @param position the index in which to set the value.
     * @param value the value to set.
     */
    public void setValue( int position, double value ) {
        if (position >= internalArray.length) {
            double[] newArray = new double[position + growingSize];
            System.arraycopy(internalArray, 0, newArray, 0, internalArray.length);
            internalArray = newArray;
        }
        internalArray[position] = value;
        lastIndex = max(lastIndex, position);
    }

    /**
     * Add a value at the end of the array.
     * 
     * @param value the value to add.
     */
    public void add( double value ) {
        int position = lastIndex;
        setValue(position, value);
        lastIndex = lastIndex + 1;
    }

    /**
     * Get the value in a certain position of the array.
     * 
     * @param position the position.
     * @return the value.
     */
    public double get( int position ) {
        return internalArray[position];
    }

    /**
     * Get the last used index.
     * 
     * @return the last used index.
     */
    public int size() {
        return lastIndex;
    }

    /**
     * Get the internal array. 
     * 
     * @return the array.
     */
    public double[] getInternalArray() {
        return internalArray;
    }

    /**
     * Get a trimmed version of the array, i.e. without ending unset positions. 
     * 
     * @return the trimmed array.
     */
    public double[] getTrimmedInternalArray() {
        if (internalArray.length == lastIndex + 1) {
            return internalArray;
        }
        double[] newArray = new double[lastIndex + 1];
        System.arraycopy(internalArray, 0, newArray, 0, newArray.length);
        return newArray;
    }

}
