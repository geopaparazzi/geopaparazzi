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
package eu.geopaparazzi.library.test;

import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import junit.framework.TestCase;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestUtilities extends TestCase {

    public void testExifformats() {
        double decimalDegree = 11.467;
        String exifFormat = Utilities.degreeDecimal2ExifFormat(decimalDegree);
        double degree = Utilities.exifFormat2degreeDecimal(exifFormat);
        assertEquals(decimalDegree, degree, 0.000001);
    }

    public void testNumberFormats() {
        double num = 11.467;
        String numStr = LibraryConstants.COORDINATE_FORMATTER.format(num);

        assertEquals("11.46700000", numStr);
    }

}
