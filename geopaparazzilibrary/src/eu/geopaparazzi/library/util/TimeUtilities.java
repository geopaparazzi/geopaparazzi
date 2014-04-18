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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Singleton utility for time management.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum TimeUtilities {
    /**
     * singleton instance.
     */
    INSTANCE;

    private static final String UTC = "UTC"; //$NON-NLS-1$
    /**
     * 
     */
    public final Locale dateLocale = Locale.US;
    /**
     * 
     */
    public final SimpleDateFormat TIMESTAMPFORMATTER_LOCAL = new SimpleDateFormat("yyyyMMdd_HHmmss", dateLocale); //$NON-NLS-1$
    /**
     * 
     */
    public final SimpleDateFormat TIMESTAMPFORMATTER_UTC = new SimpleDateFormat("yyyyMMdd_HHmmss", dateLocale); //$NON-NLS-1$
    /**
     * 
     */
    public final SimpleDateFormat DATEONLY_FORMATTER = new SimpleDateFormat("yyyy-MM-dd", dateLocale); //$NON-NLS-1$
    /**
     * 
     */
    public final SimpleDateFormat TIMEONLY_FORMATTER = new SimpleDateFormat("HH:mm:ss", dateLocale); //$NON-NLS-1$
    /**
     * 
     */
    public final SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", dateLocale); //$NON-NLS-1$

    /**
     * 
     */
    public final SimpleDateFormat TIME_FORMATTER_UTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", dateLocale); //$NON-NLS-1$
    /**
     * 
     */
    public final SimpleDateFormat TIME_FORMATTER_LOCAL = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", dateLocale); //$NON-NLS-1$

    /**
     * 
     */
    public final SimpleDateFormat TIME_FORMATTER_SQLITE_UTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", dateLocale); //$NON-NLS-1$
    /**
     * 
     */
    public final SimpleDateFormat TIME_FORMATTER_SQLITE_LOCAL = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", dateLocale); //$NON-NLS-1$
    /**
     * 
     */
    public final SimpleDateFormat TIME_FORMATTER_GPX_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", dateLocale); //$NON-NLS-1$
    /**
     * 
     */
    public final SimpleDateFormat EXIFFORMATTER = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", dateLocale); //$NON-NLS-1$

    private TimeUtilities() {
        TIME_FORMATTER_SQLITE_UTC.setTimeZone(TimeZone.getTimeZone(UTC));
        TIME_FORMATTER_GPX_UTC.setTimeZone(TimeZone.getTimeZone(UTC));
        TIME_FORMATTER_UTC.setTimeZone(TimeZone.getTimeZone(UTC));
        TIMESTAMPFORMATTER_UTC.setTimeZone(TimeZone.getTimeZone(UTC));
        EXIFFORMATTER.setTimeZone(TimeZone.getTimeZone(UTC));
    }

    /**
     * Converts a utc time string to local time.
     * 
     * @param dateTime the date time string.
     * @return the local time string.
     * @throws Exception if something goes wrong.
     */
    public static String utcToLocalTime( String dateTime ) throws Exception {
        Date date = TimeUtilities.INSTANCE.TIME_FORMATTER_UTC.parse(dateTime);
        return TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(date);
    }
}
