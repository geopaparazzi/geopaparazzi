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

import android.graphics.Color;

import eu.geopaparazzi.library.util.ColorUtilities;

/**
 * Singleton that takes care of all the surveyed vector data issues.
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

    /**
     * @return color.
     */
    public int getNotesColor() {
        return notesColor;
    }

    /**
     * @param colorStr color
     */
    public void setNotesColor( String colorStr ) {
        int color = ColorUtilities.toColor(colorStr);
        this.notesColor = color;
    }

    /**
     * @return the width
     */
    public float getNotesWidth() {
        return notesWidth;
    }

    /**
     * @param notesWidth the width
     */
    public void setNotesWidth( float notesWidth ) {
        this.notesWidth = notesWidth;
    }

    private boolean areNotesVisible = true;
    /**
     * @return <code>true</code> if notes visible.
     */
    public boolean areNotesVisible() {
        return areNotesVisible;
    }

    /**
     * @param areNotesVisible if notes are visible.
     */
    public void setNotesVisible( boolean areNotesVisible ) {
        this.areNotesVisible = areNotesVisible;
    }

    private boolean areImagesVisible = true;

    /**
     * @return  <code>true</code>, if images are visible.
     */
    public boolean areImagesVisible() {
        return areImagesVisible;
    }

    /**
     * @param areImagesVisible if images are visible.
     */
    public void setImagesVisible( boolean areImagesVisible ) {
        this.areImagesVisible = areImagesVisible;
    }

    private boolean areLogsVisible = false;
    /**
     * @return <code>true</code> if logs are visible.
     */
    public boolean areLogsVisible() {
        return areLogsVisible;
    }

    /**
     * @param areLogsVisible if logs visible.
     */
    public void setLogsVisible( boolean areLogsVisible ) {
        this.areLogsVisible = areLogsVisible;
    }

    private boolean areMapsVisible = false;
    /**
     * @return <code>true</code> if maps visible.
     */
    public boolean areMapsVisible() {
        return areMapsVisible;
    }

    /**
     * @param areMapsVisible if maps are visible.
     */
    public void setMapsVisible( boolean areMapsVisible ) {
        this.areMapsVisible = areMapsVisible;
    }

}
