package eu.geopaparazzi.library.sketch.brush;

import android.graphics.Path;

/**
 * Created by IntelliJ IDEA.
 * User: almondmendoza
 * Date: 01/12/2010
 * Time: 10:48 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IBrush {
    /**
     * @param path  path 
     * @param x     x    
     * @param y     y    
     */
    public void mouseDown( Path path, float x, float y );
    /**
     * @param path path 
     * @param x    x    
     * @param y    y    
     */
    public void mouseMove( Path path, float x, float y );
    /**
     * @param path   path 
     * @param x      x    
     * @param y      y    
     */
    public void mouseUp( Path path, float x, float y );
}
