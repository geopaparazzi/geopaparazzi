/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.library.plugin.types;

import android.content.Intent;

import eu.geopaparazzi.library.util.IActivitySupporter;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class MenuEntry implements IMenuEntry {
    private String label = null;
    private byte[] icon = null;
    protected int requestCode = -1;

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public byte[] getIcon() {
        return this.icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    @Override
    public void onClick(IActivitySupporter clickActivityStarter) {
        if (processOnClick(clickActivityStarter)) {
            start(clickActivityStarter);
        }
    }

    /**
     * The start method called if the processOnCLick returns true.
     * @param clickActivityStarter
     */
    protected void start(IActivitySupporter clickActivityStarter){};

    /**
     * This method is invoked when the entry is clicked, before the activity specified
     * by the action is started. The activity execution can be cancelled if this
     * method returns false
     *
     * @return false to cancel the execution of the action, true otherwise
     * @param clickActivityStarter
     */
    protected boolean processOnClick(IActivitySupporter clickActivityStarter) {
        return true;
    }

    @Override
    public int getOrder() {
        return 500;
    }

    public void onActivityResultExecute(int requestCode, int resultCode, Intent data){};

    public void setRequestCode(int requestCode){
        this.requestCode = requestCode;
    }

}
