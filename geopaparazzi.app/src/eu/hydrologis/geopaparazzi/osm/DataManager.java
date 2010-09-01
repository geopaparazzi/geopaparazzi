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
package eu.hydrologis.geopaparazzi.osm;

import android.graphics.Color;

/**
 * Singleton that takes care of all the vector data issues.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DataManager {

    private static DataManager dataManager;

    private int notesColor = Color.RED;

    private float notesWidth = 5;

    /**
     * Gets the apps manager singleton. 
     * 
     * @return the {@link DataManager} singleton.
     */
    public static DataManager getInstance() {
        if (dataManager == null) {
            dataManager = new DataManager();
        }
        return dataManager;
    }

    public int getNotesColor() {
        return notesColor;
    }

    public void setNotesColor( String colorStr ) {
        int color = Color.parseColor(colorStr);
        this.notesColor = color;
    }

    public float getNotesWidth() {
        return notesWidth;
    }

    public void setNotesWidth( float notesWidth ) {
        this.notesWidth = notesWidth;
    }

    private boolean areNotesVisible = true;
    public boolean areNotesVisible() {
        return areNotesVisible;
    }
    public void setNotesVisible( boolean areNotesVisible ) {
        this.areNotesVisible = areNotesVisible;
    }

    private boolean areLogsVisible = false;
    public boolean areLogsVisible() {
        return areLogsVisible;
    }

    public void setLogsVisible( boolean areLogsVisible ) {
        this.areLogsVisible = areLogsVisible;
    }

    private boolean areMapsVisible = false;
    public boolean areMapsVisible() {
        return areMapsVisible;
    }

    public void setMapsVisible( boolean areMapsVisible ) {
        this.areMapsVisible = areMapsVisible;
    }

}
