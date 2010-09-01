package eu.hydrologis.geopaparazzi.osm;

import java.io.Serializable;

/**
 * Item representing a map entry (gps log or map).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class MapItem implements Comparable<MapItem>, Serializable {
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

    public int compareTo( MapItem another ) {
        if (name.equals(another.name)) {
            return 0;
        } else {
            return name.compareTo(another.name);
        }
    }

}
