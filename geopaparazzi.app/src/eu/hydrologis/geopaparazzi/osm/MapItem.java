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

import java.io.Serializable;

/**
 * Item representing a map entry (gps log or map).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class MapItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean isDirty = false;
    private String name;
    private long id;
    private float width;
    private String color;
    private boolean isVisible;
    private int type;

    public long getId() {
        return id;
    }

    public void setId( long id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty( boolean isDirty ) {
        this.isDirty = isDirty;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth( float width ) {
        this.width = width;
    }

    public String getColor() {
        return color;
    }

    public void setColor( String color ) {
        this.color = color;
    }

    public void setVisible( boolean isVisible ) {
        this.isVisible = isVisible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setType( int type ) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

}
