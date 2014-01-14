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
package eu.geopaparazzi.library.sms;

/**
 * Data that can be contained in an sms. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SmsData {
    /**
     * 
     */
    public static int NOTE = 0;
    /**
     * 
     */
    public static int BOOKMARK = 1;
    /**
     * 
     */
    public static int TRACK = 2;

    /**
     * 
     */
    public int TYPE = 0;
    /**
     * 
     */
    public float x = 0f;
    /**
     * 
     */
    public float y = 0f;
    /**
     * 
     */
    public float z = Float.NaN;
    /**
     * 
     */
    public String text = ""; //$NON-NLS-1$

    /**
     * Convert data to sms string.
     * 
     * @return the data string.
     */
    @SuppressWarnings("nls")
    public String toSmsDataString() {
        StringBuilder sb = new StringBuilder();
        if (TYPE == NOTE) {
            sb.append("n:");
        } else if (TYPE == BOOKMARK) {
            sb.append("b:");
        }
        sb.append(x).append(",");
        sb.append(y).append(",");
        if (!Float.isNaN(z))
            sb.append(z).append(",");
        sb.append(text);
        return sb.toString();
    }
}
