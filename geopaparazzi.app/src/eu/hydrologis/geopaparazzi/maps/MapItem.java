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
 * Item representing a map entry.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class MapItem implements Serializable {

    private static final long serialVersionUID = 1L;

    protected boolean isDirty = false;
    protected String name;
    protected long id;
    protected float width;
    protected String color;
    protected boolean isVisible;
    protected int type;

    /**
     * @param id id
     * @param text text
     * @param color color
     * @param width width
     * @param isVisible if visible.
     */
    public MapItem( long id, String text, String color, float width, boolean isVisible ) {
        this.id = id;
        name = text;
        this.color = color;
        this.width = width;
        this.isVisible = isVisible;
    }

    /**
     * @return id
     */
    public long getId() {
        return id;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    // public void setName( String name ) {
    // this.name = name;
    // }

    /**
     * @return <code>true</code> if dirty.
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
    * @param isDirty if dirty.
    */
    public void setDirty( boolean isDirty ) {
        this.isDirty = isDirty;
    }

    /**
     * @return width
     */
    public float getWidth() {
        return width;
    }

    // public void setWidth( float width ) {
    // this.width = width;
    // }

    /**
     * @return color
     */
    public String getColor() {
        return color;
    }

    // public void setColor( String color ) {
    // this.color = color;
    // }

    /**
    * @param isVisible if visible.
    */
    public void setVisible( boolean isVisible ) {
        this.isVisible = isVisible;
    }

    /**
     * @return <code>true</code> if visible.
     */
    public boolean isVisible() {
        return isVisible;
    }

    // public void setType( int type ) {
    // this.type = type;
    // }

    /**
     * @return the type.
     */
    public int getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((color == null) ? 0 : color.hashCode());
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + (isDirty ? 1231 : 1237);
        result = prime * result + (isVisible ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + type;
        result = prime * result + Float.floatToIntBits(width);
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MapItem other = (MapItem) obj;
        if (color == null) {
            if (other.color != null)
                return false;
        } else if (!color.equals(other.color))
            return false;
        if (id != other.id)
            return false;
        if (isDirty != other.isDirty)
            return false;
        if (isVisible != other.isVisible)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type != other.type)
            return false;
        if (Float.floatToIntBits(width) != Float.floatToIntBits(other.width))
            return false;
        return true;
    }
}
