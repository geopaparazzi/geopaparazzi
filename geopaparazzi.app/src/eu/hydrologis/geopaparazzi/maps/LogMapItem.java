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
package eu.hydrologis.geopaparazzi.maps;

import java.io.Serializable;

/**
 * Item representing a gps log.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LogMapItem extends MapItem implements Serializable {

    private static final long serialVersionUID = 1L;
    private String startTime = " - "; //$NON-NLS-1$
    private String endTime = " - "; //$NON-NLS-1$

    public LogMapItem( long id, String text, String color, float width, boolean isVisible, String startTime, String endTime ) {
        super(id, text, color, width, isVisible);
        if (startTime != null)
            this.startTime = startTime;
        if (endTime != null)
            this.endTime = endTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime( String startTime ) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime( String endTime ) {
        this.endTime = endTime;
    }

}
