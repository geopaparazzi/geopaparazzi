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
package eu.geopaparazzi.library.features;

import android.widget.LinearLayout;

/**
 * The editing layer manager.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum EditManager {
    /**
     * The singleton instance. 
     */
    INSTANCE;

    private ILayer editLayer;
    private Tool activeTool;
    private ToolGroup activeToolGroup;
    private EditingView editingView;
    private LinearLayout toolsLayout;

    /**
     * @return the editing layer.
     */
    public ILayer getEditLayer() {
        return editLayer;
    }

    /**
     * Setter for the editing layer.
     * 
     * @param editLayer the editing layer.
     */
    public void setEditLayer( ILayer editLayer ) {
        this.editLayer = editLayer;
    }

    /**
     * Set the current active {@link Tool}.
     * 
     * <p>Only one tool can be active at the time.</p>
     * <p>Setting the active tool to <code>null</code> has the 
     * result of disabling the current tool.
     * 
     * @param newActiveTool the new active tool to set.
     */
    public void setActiveTool( Tool newActiveTool ) {
        if (this.activeTool != null) {
            // disable current active tool
            this.activeTool.disable();
            this.activeTool = null;
        }
        this.activeTool = newActiveTool;
        if (newActiveTool != null) {
            newActiveTool.activate();
        }
        invalidateEditingView();
    }

    /**
     * @return the current active tool.
     */
    public Tool getActiveTool() {
        return activeTool;
    }

    /**
     * Set the current active {@link ToolGroup}.
     * 
     * <p>The tool group gets initialized inside here. Don't call initUI before.
     * <p>Only one toolgroup can be active at the time.</p>
     * <p>Setting the active toolgroup to <code>null</code> has the 
     * result of disabling the current toolgroup.
     * 
     * @param activeToolGroup the new active tool to set.
     */
    public void setActiveToolGroup( ToolGroup activeToolGroup ) {
        if (this.activeToolGroup != null) {
            // disable current active tool
            this.activeToolGroup.disable();
            this.activeToolGroup = null;
        }
        this.activeToolGroup = activeToolGroup;
        if (activeToolGroup != null) {
            activeToolGroup.activate();
            activeToolGroup.initUI();
        }
        invalidateEditingView();
    }
    /**
     * @return the current active tool.
     */
    public ToolGroup getActiveToolGroup() {
        return activeToolGroup;
    }

    /**
     * Set the editing view.
     * 
     * @param editingView the editing view to set.
     * @param toolsLayout the layout for the tools gui.
     */
    public void setEditingView( EditingView editingView, LinearLayout toolsLayout ) {
        this.editingView = editingView;
        this.toolsLayout = toolsLayout;
    }

    /**
     * @return the current editing view.
     */
    public EditingView getEditingView() {
        return editingView;
    }

    /**
     * Invalidate the editing view if it exists.
     */
    public void invalidateEditingView() {
        if (editingView != null) {
            editingView.invalidate();
        }
    }

    /**
     * @return the tools layout.
     */
    public LinearLayout getToolsLayout() {
        return toolsLayout;
    }

    /**
     * Callback for position updates. 
     * 
     * @param lon longitude.
     * @param lat latitude.
     */
    public void onGpsUpdate( double lon, double lat ) {
        if (this.activeToolGroup != null) {
            this.activeToolGroup.onGpsUpdate(lon, lat);
        }
    }
}
